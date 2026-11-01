package ess.ll.client.module.combat;

import ess.ll.client.integration.OptionalModules.MaceHelper;
import ess.ll.client.integration.OptionalModules.ElytraTarget;
import ess.ll.client.core.SimulatedPlayerInput;
import ess.ll.client.event.EntityDeathEvent;
import ess.ll.client.event.FrameDeltaEvent;
import ess.ll.client.event.FrameUpdateEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.SprintStateEvent;
import ess.ll.client.event.SprintSyncEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.features.combat.AuraAimPointSelector;
import ess.ll.client.features.combat.AuraAttackTiming;
import ess.ll.client.features.combat.AuraCriticalController;
import ess.ll.client.features.combat.AuraRotationController;
import ess.ll.client.features.combat.AuraTargetPointTracker;
import ess.ll.client.features.combat.AuraUpdateEvent;
import ess.ll.client.features.combat.OnnxRotationModel;
import ess.ll.client.features.combat.OnnxRotationSettingsCodec;
import ess.ll.client.features.combat.RotationModelRepository;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.features.inventory.ItemUtils;
import ess.ll.client.features.social.FriendManager;
import ess.ll.client.features.world.WorldCollisionUtils;
import ess.ll.client.integration.EntityExtensionAccess;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.MultiToggleSetting;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.AttackCooldownUtils;
import ess.ll.client.util.AttackItemUtils;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.MovementUtils;
import ess.ll.client.util.PlayerUtils;
import ess.ll.client.util.PlayerVelocityTracker;
import ess.ll.client.util.RaycastUtils;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import ess.ll.client.util.ServerTickRateTracker;
import ess.ll.client.util.SortableArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import ess.ll.client.mixin.ItemInHandRendererMixin_1191;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public class AttackAura extends ClientModule {
    /**
     * Reuse this Aura's configured rotation strategy for crystal/block aim.
     */
    public boolean rotateCrystalAim(org.joml.Vector2f aim, LivingEntity target, int priority) {
        return this.rotationController.rotateTo(aim, target, priority);
    }

    private final ModeSetting rotationMode;
    private final NumberSetting<Float> attackRange;
    private final NumberSetting<Float> targetingRange;
    private final ModeSetting attackStyle;
    private final NumberSetting<Integer> clicksPerSecond;
    private final MultiToggleSetting targetSortOptions;
    private final MultiToggleSetting targetTypes;
    private final ToggleSetting jumpOnly;
    private final ToggleSetting accelerateAttacks;
    private final MultiToggleSetting attackOptions;
    private final ModeSetting wallCheckMode;
    private final ModeSetting movementCorrectionMode;
    private final NumberSetting<Float> yawSpeed;
    private final NumberSetting<Float> pitchSpeed;
    private final NumberSetting<Float> elytraYawSpeed;
    private final NumberSetting<Float> elytraPitchSpeed;
    private final ToggleSetting aimAssist;
    private final NumberSetting<Float> yawAssist;
    private final NumberSetting<Float> pitchAssist;
    private final NumberSetting<Float> preAttackAssistMillis;
    private final AuraAimPointSelector aimPointSelector;
    public static LivingEntity target;
    public AuraAttackTiming attackTiming;
    public boolean chasingElytraTarget;
    public Vec3 predictedAimPoint;
    private final AuraTargetPointTracker targetPointTracker;
    private final AuraRotationController rotationController;
    private final AuraCriticalController criticalController;
    private final OnnxRotationModel rotationModel;
    private final RotationModelRepository modelRepository;
    public boolean elytraTargetStable;
    private int slowTargetTicks;
    private int sprintResetCheckTick;
    private boolean sprintResetRequired;
    private int attackDelayTicks;
    private int maceWaitTicks;
    private int ticksSinceAttack;
    private boolean sprintStoppedThisTick;
    public int attackCount;
    private int nextMissInterval;
    private boolean previousChaseStability;
    private int criticalPredictionTick;
    private int collisionCheckTick;
    private boolean nearbyCollision;
    private boolean predictedCritical;
    private boolean predictedInWater;
    private int nextCriticalTick;
    private final List<LivingEntity> targets;
    private final SortableArrayList<AttackAura.ScoredTarget> scoredTargets;
    public double targetDistance;

    private void selectTarget(ElytraTarget elytraTarget) {
        this.targets.clear();
        for (Entity entity2 : mc.level.entitiesForRendering()) {
            if (entity2 instanceof LivingEntity selected2 && this.isValidTarget(elytraTarget, selected2)) {
                this.targets.add(selected2);
            }
        }
        if (this.targets.isEmpty()) {
            target = null;
        } else if (this.targets.size() == 1) {
            target = this.targets.getFirst();
        } else {
            this.scoredTargets.clear();
            boolean selected = this.targetSortOptions.isSelected("Дистанции");
            boolean selected2 = this.targetSortOptions.isSelected("Здоровью");
            boolean selected3 = this.targetSortOptions.isSelected("Броня");
            boolean selected4 = this.targetSortOptions.isSelected("Поле зрению");
            boolean selected5 = this.targetSortOptions.isSelected("Надета элитра");
            boolean sxxxxx = selected || selected2 || selected3 || selected4 || selected5;
            boolean enabled = elytraTarget.isEnabled();
            float sxxxxxxx = Math.max(elytraTarget.fallFlyingOffset.getValue(), 0.001F);
            float sxxxxxxxx = Math.max(this.effectiveAttackRange(), 0.001F);
            for (LivingEntity entity3 : this.targets) {
                double sxxxxxxxxxx = this.distanceToTarget(entity3) / (enabled ? sxxxxxxx : sxxxxxxxx);
                double sxxxxxxxxxxx = ItemUtils.weightedHealthRatio(entity3);
                double sxxxxxxxxxxxx = ItemUtils.relativeYaw(entity3);
                double sxxxxxxxxxxxxx = -ItemUtils.armorScore(entity3);
                this.scoredTargets.add(new AttackAura.ScoredTarget(entity3, !ItemUtils.wearsElytra(entity3), sxxxxxxxxxx, sxxxxxxxxxxx, sxxxxxxxxxxxx, sxxxxxxxxxxxxx));
            }
            this.scoredTargets.sortByInt(var0 -> var0.entity().getId());
            if (selected3) {
                this.scoredTargets.sortByDouble(AttackAura.ScoredTarget::armorScore);
            }
            if (selected4) {
                this.scoredTargets.sortByDouble(AttackAura.ScoredTarget::fovScore);
            }
            if (selected2) {
                this.scoredTargets.sortByDouble(AttackAura.ScoredTarget::healthScore);
            }
            if (selected || !sxxxxx) {
                this.scoredTargets.sortByDouble(AttackAura.ScoredTarget::distanceScore);
            }
            if (selected5) {
                this.scoredTargets.sortByInt(var0 -> var0.elytraBias() ? 1 : 0);
            }
            target = this.scoredTargets.getFirst().entity();
        }
    }

    private void tryAttackTarget(ElytraTarget elytraTarget, boolean var2) {
        if (!this.isUsingRangedWeapon()) {
            if (this.attackOptions.isSelected("Отжимать щит") && mc.player.isBlocking()) {
                mc.gameMode.releaseUsingItem(mc.player);
            }
            if (!this.attackOptions.isSelected("Не бить если ешь") || !mc.player.isUsingItem()) {
                if (!this.usesContinuousRotation() && elytraTarget.unusedTarget.is("Легитный") && this.chasingElytraTarget) {
                    Vector2f vector = PlayerUtils.rotationTo(mc.player.getEyePosition(), target.getEyePosition());
                    RotationManager.rotate(new Rotation(vector.x, vector.y), 9999.0F, 100, this.rotationMode.is("Legit"));
                }
                if (this.wallCheckMode.is("Обход стен")) {
                    Vec3 eyePosition = mc.player.getEyePosition();
                    Vec3 eyePosition2 = target.getEyePosition();
                    Vec3 vector2 = eyePosition2.subtract(eyePosition);
                    double count = vector2.length();
                    if (count < 0.001) {
                        return;
                    }
                    Vec3 vector3 = vector2.normalize();
                    for (double var9 = 0.0; var9 < count; var9 += 0.5) {
                        Vec3 vector4 = eyePosition.add(vector3.scale(var9));
                        BlockPos blockPos = BlockPos.containing(vector4);
                        if (!mc.level.isEmptyBlock(blockPos)) {
                            mc.player.connection.send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                            mc.player.connection.send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
                        }
                    }
                }
                this.attack(target);
                if (this.attackOptions.isSelected("Ломать щит") && target instanceof Player var14 && var14.isBlocking()) {
                    this.breakShield(var14);
                }
            }
        }
    }

    public AttackAura() {
        super(ModuleCategory.Combat, ModuleSubCategory.Combat);
        this.rotationMode = new ModeSetting("Тип").addModes("Grim", "Matrix", "MatrixSnap", "Legit", "Smooth", "Universal").withDefault("Matrix").withContainer(this);
        this.attackRange = new NumberSetting<Float>("Дистанция атаки").withMin(3.0F).withMax(6.0F).withStep(0.1F).withDefault(3.0F).withContainer(this);
        this.targetingRange = new NumberSetting<Float>("Дистанция радиуса").withMin(0.0F).withMax(10.0F).withStep(0.5F).withDefault(0.0F).withContainer(this);
        this.attackStyle = new ModeSetting("Стиль атаки").addModes("1.9", "1.8").withDefault("1.9").withContainer(this);
        this.clicksPerSecond = new NumberSetting<Integer>("Частота кликов").withMin(1).withMax(5).withStep(1).withDefault(1).withVisibility(() -> this.attackStyle.is("1.8")).withContainer(this);
        this.targetSortOptions = new MultiToggleSetting("Сортировать по").addOption(NamedOption.of("Здоровью", true)).addOption(NamedOption.of("Дистанции", true)).addOption(NamedOption.of("Броня", false)).addOption(NamedOption.of("Поле зрению", false)).addOption(NamedOption.of("Надета элитра", false)).withMinSelected(1).withContainer(this);
        this.targetTypes = new MultiToggleSetting("Таргеты").addOption(NamedOption.of("Игроки", true)).addOption(NamedOption.of("Голые", true)).addOption(NamedOption.of("Мобы", false)).addOption(NamedOption.of("Животные", false)).addOption(NamedOption.of("Жители", false)).addOption(NamedOption.of("Друзья", false)).addOption(NamedOption.of("Невидимые", true)).withDescription("Настройка выбора цели").withContainer(this);
        this.jumpOnly = new ToggleSetting("Только в прыжке").setValue(true).withContainer(this);
        this.accelerateAttacks = new ToggleSetting("Ускорять удары").setValue(false).withContainer(this);
        this.attackOptions = new MultiToggleSetting("Настройки").addOption(NamedOption.of("Выключать после смерти", true)).addOption(NamedOption.of("Рандомизация ударов", false)).addOption(NamedOption.of("Сбрасывать спринт", true)).addOption(NamedOption.of("Не бить если ешь", false)).addOption(NamedOption.of("Проверка наводки", true)).addOption(NamedOption.of("Криты в воде", false)).addOption(NamedOption.of("Только криты", true)).addOption(NamedOption.of("Отжимать щит", true)).addOption(NamedOption.of("Ломать щит", true)).addOption(NamedOption.of("Тпс синх", false)).withDescription("Настройка выбора цели").withContainer(this);
        this.wallCheckMode = new ModeSetting("Режим проверки стен").addModes("Игнорировать стены", "Не бить через стены", "Обход стен").withDefault("Не бить через стены").withContainer(this);
        this.movementCorrectionMode = new ModeSetting("Тип коррекции").addModes("Незаметный", "Сфокусированный", "Таргетированная").withDefault("Незаметный").withContainer(this);
        this.yawSpeed = new NumberSetting<Float>("Скорость по yaw").withMin(1.0F).withMax(20.0F).withStep(1.0F).withDefault(10.0F).withVisibility(() -> this.rotationMode.is("Legit")).withContainer(this);
        this.pitchSpeed = new NumberSetting<Float>("Скорость по pitch").withMin(1.0F).withMax(20.0F).withStep(1.0F).withDefault(10.0F).withVisibility(() -> this.rotationMode.is("Legit")).withContainer(this);
        this.elytraYawSpeed = new NumberSetting<Float>("Скорость по yaw (Elytra)").withMin(1.0F).withMax(20.0F).withStep(1.0F).withDefault(10.0F).withVisibility(() -> this.rotationMode.is("Legit")).withContainer(this);
        this.elytraPitchSpeed = new NumberSetting<Float>("Скорость по pitch (Elytra)").withMin(1.0F).withMax(20.0F).withStep(1.0F).withDefault(10.0F).withVisibility(() -> this.rotationMode.is("Legit")).withContainer(this);
        this.aimAssist = new ToggleSetting("Аим Ассист").setValue(false);
        this.yawAssist = new NumberSetting<Float>("Помощь по yaw").withMin(0.0F).withMax(100.0F).withStep(1.0F).withDefault(30.0F);
        this.pitchAssist = new NumberSetting<Float>("Помощь по pitch").withMin(0.0F).withMax(100.0F).withStep(1.0F).withDefault(30.0F);
        this.preAttackAssistMillis = new NumberSetting<Float>("Помощь до удара (мс)").withMin(0.0F).withMax(1000.0F).withStep(50.0F).withDefault(350.0F);
        this.aimPointSelector = new AuraAimPointSelector();
        this.attackTiming = new AuraAttackTiming();
        this.predictedAimPoint = Vec3.ZERO;
        this.targetPointTracker = new AuraTargetPointTracker();
        this.rotationController = new AuraRotationController(this);
        this.criticalController = new AuraCriticalController(this);
        this.rotationModel = new OnnxRotationModel();
        this.modelRepository = new RotationModelRepository();
        this.elytraTargetStable = false;
        this.slowTargetTicks = 0;
        this.sprintResetCheckTick = Integer.MIN_VALUE;
        this.previousChaseStability = false;
        this.criticalPredictionTick = Integer.MIN_VALUE;
        this.collisionCheckTick = Integer.MIN_VALUE;
        this.targets = new ArrayList<>();
        this.scoredTargets = new SortableArrayList<>();
        this.targetDistance = 0.0;
        this.setName("Attack Aura");
        this.modelRepository.loadBundledModel();
    }

    public boolean canCriticalHitNow() {
        boolean var1 = mc.player.onGround();
        boolean var2 = mc.options.keyJump.isDown() || !var1 || this.attackedRecently();
        boolean selected = this.attackOptions.isSelected("Только криты");
        boolean var4 = !selected || mc.player.getAbilities().flying || mc.player.isFallFlying() || this.hasCriticalBlockingEffect() || mc.player.onClimbable() || WorldCollisionUtils.containsBlock(mc.player.getBoundingBox(), Blocks.COBWEB);
        if (this.isMovementFrozen()) {
            return true;
        }
        if (mc.player.isInLava()) {
            return true;
        }
        boolean var5 = this.isPlayerInWater() || this.hasCollisionNearPlayer();
        if (!var5 || !mc.player.isUnderWater() && this.shouldAccelerateAttacks()) {
            boolean var6 = WorldCollisionUtils.hasHeadClearance();
            if (!this.jumpOnly.getValue() && !var2) {
                return true;
            }
            if (this.isGroundCollisionBlockingCritical(var6)) {
                return true;
            }
            if (var4) {
                return true;
            }
            boolean var7 = ModuleManager.INSTANCE.criticals().isLegacyModeActive();
            return this.isFallingForCritical(var1, var5) || var7;
        } else {
            return true;
        }
    }

    private void updateCriticalPrediction() {
        int ticks = TickEvent.getTicks();
        if (this.criticalPredictionTick != ticks) {
            this.criticalPredictionTick = ticks;
            this.predictedCritical = false;
            this.predictedInWater = this.isPlayerInWater();
            this.nextCriticalTick = -1;
            if (mc.player != null && mc.player.input != null) {
                SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.ofClientPlayer(SimulatedPlayerInput.SimulatedInput.fromClientPlayer(mc.player.input.keyPresses));
                for (int var3 = 0; var3 < 10; var3++) {
                    simulatedPlayerInput.tick();
                    if (simulatedPlayerInput.inWater) {
                        this.predictedInWater = true;
                    } else if (!simulatedPlayerInput.onGround && simulatedPlayerInput.fallDistance > 0.0F) {
                        this.predictedCritical = true;
                        if (this.nextCriticalTick < 0) {
                            this.nextCriticalTick = var3 + 1;
                        }
                    }
                }
            }
        }
    }

    private boolean isFallingForCritical(boolean var1, boolean var2) {
        Criticals criticals2 = ModuleManager.INSTANCE.criticals();
        return criticals2.isSlowFallingCriticalActive() ? criticals2.canForceCritical() : !var1 && !var2 && mc.player.fallDistance > 0.0;
    }

    public boolean hasNearbyCollision() {
        return this.hasCollisionNearPlayer();
    }

    private void updateTarget() {
        ElytraTarget elytraTarget2 = ModuleManager.INSTANCE.elytraTarget();
        if (target == null || !this.isValidTarget(elytraTarget2, target)) {
            this.selectTarget(elytraTarget2);
        }
        this.updateTargetActions(elytraTarget2);
    }

    private boolean isMaceFalling() {
        return this.isMaceAttack() && AttackItemUtils.isFalling(mc.player);
    }

    private boolean shouldAccelerateAttacks() {
        return this.attackOptions.isSelected("Криты в воде") || this.accelerateAttacks.getValue() || ModuleManager.INSTANCE.criticals().isSlowFallingCriticalActive();
    }

    @Override
    public void toggle() {
        if (mc.player != null && mc.level != null) {
            target = null;
            this.targetPointTracker.reset();
            this.predictedAimPoint = Vec3.ZERO;
            this.attackDelayTicks = 0;
            this.maceWaitTicks = 0;
            this.sprintStoppedThisTick = false;
            this.rotationController.reset();
            this.criticalController.reset();
            super.toggle();
        }
    }

    public boolean shouldSuppressSprint() {
        return this.needsSprintReset();
    }

    @Subscribe
    private void onFrameUpdate(FrameUpdateEvent event) {
        if (mc.player != null && mc.level != null) {
            if (target != null) {
                if (this.usesContinuousRotation()) {
                    return;
                }
                this.updateRotation(false, 50);
            }
        }
    }

    public float getAimPredictionOffset() {
        return 1.5F;
    }

    @Subscribe
    private void onAuraUpdate(AuraUpdateEvent event) {
        this.rotationController.updateRender(false, 50, true);
    }

    public boolean isAimAssistEnabled() {
        return this.rotationMode.is("Neuro") ? this.aimAssist.getValue() : this.rotationSettings().isAimAssist();
    }

    private OnnxRotationSettingsCodec rotationSettings() {
        return OnnxRotationSettingsCodec.INSTANCE;
    }

    private boolean hasPredictedCritical() {
        if (this.criticalController.ticksUntilCritical() >= 0) {
            return true;
        }
        this.updateCriticalPrediction();
        return this.predictedCritical;
    }

    public RotationModelRepository getModelRepository() {
        return this.modelRepository;
    }

    @Subscribe
    private void onSprintSync(SprintSyncEvent event) {
        this.sprintStoppedThisTick = false;
    }

    private boolean isServerLagging() {
        float tps = ServerTickRateTracker.INSTANCE.getTps();
        return Float.isFinite(tps) && tps <= 19.5F;
    }

    @Subscribe
    private void onFrameDelta(FrameDeltaEvent event) {
        if (mc.player != null && mc.level != null && target != null) {
            float delta = event.getDelta();
            if (this.usesContinuousRotation() ? !(delta > 6.0F) : delta == 1.0F) {
                this.rotationController.updateFixedRate(50);
            }
        }
    }

    private boolean canHitPredictedTarget(SimulatedPlayerInput simulatedPlayerInput) {
        Vec3 vector = simulatedPlayerInput.position.add(0.0, mc.player.getEyeHeight(), 0.0);
        AABB bounds = target.getBoundingBox().move(MovementUtils.motionOf(target));
        Vector2f vector2 = PlayerUtils.rotationTo(vector, bounds.getCenter());
        return RaycastUtils.isLookingAt(vector, vector2.x, vector2.y, this.effectiveAttackRange(), this.wallCheckMode.is("Не бить через стены"), bounds);
    }

    public boolean isAttackReady(boolean var1, boolean var2) {
        boolean var3 = Math.abs(this.targetDistance) <= this.effectiveAttackRange();
        boolean selected = this.attackOptions.isSelected("Только криты");
        float var5 = AttackCooldownUtils.attackStrength(mc.player, this.cooldownTickOffset());
        if (!var1 && (var2 || !(var5 < this.requiredAttackStrength()) && (!selected || !(mc.player.getAttackStrengthScale(0.5F) <= 0.9F)))) {
            return !var3 ? false : this.canAttackWithCriticalPolicy();
        } else {
            return false;
        }
    }

    private static float normalizePercent(float var0) {
        return Math.max(0.0F, Math.min(100.0F, var0)) / 100.0F;
    }

    public boolean isAttackAccelerationEnabled() {
        return this.accelerateAttacks.getValue();
    }

    private float cooldownTickOffset() {
        if (mc.player != null && !this.attackStyle.is("1.8")) {
            boolean var1 = this.attackOptions.isSelected("Тпс синх") && this.isServerLagging();
            return serverAdjustedTickOffset(var1, ServerTickRateTracker.INSTANCE.getLagAverage());
        } else {
            return 0.0F;
        }
    }

    private float requiredAttackStrength() {
        return this.attackStyle.is("1.8") ? 0.05F : 0.95F;
    }

    public MultiToggleSetting getAttackOptions() {
        return this.attackOptions;
    }

    private static float predictionDistanceOffset(float var0) {
        return var0 < 4.0F ? 0.0F : var0 - 3.0F;
    }

    public float effectiveAttackRange() {
        float value = this.attackRange.getValue();
        return this.isMovementFrozen() ? 6.0F : value;
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (mc.player != null && mc.level != null) {
            if (target != null && !mc.player.isFallFlying()) {
                switch ((String) this.movementCorrectionMode.getValue()) {
                    case "Таргетированная":
                        Vec3 vector = MovementUtils.motionOf(target).scale(3.0);
                        Vec3 vector2 = target.position().add(vector);
                        MovementUtils.correctMovement(event, Mth.wrapDegrees(mc.player.getYRot()), vector2.x() - mc.player.getX(), vector2.z() - mc.player.getZ());
                        break;
                    case "Незаметный":
                        RotationManager.moveCorrectionRequested = true;
                }
            }
        }
    }

    public float pitchAssistStrength() {
        return this.assistFraction(this.rotationMode.is("Neuro") ? this.pitchAssist.getValue() : this.rotationSettings().getAssistPitch());
    }

    private void updateElytraChaseState(ElytraTarget elytraTarget, LivingEntity entity) {
        float var3 = (float) (entity.getX() - entity.xo);
        float var4 = (float) (entity.getY() - entity.yo);
        float var5 = (float) (entity.getZ() - entity.zo);
        float var6 = blocksPerSecond(var3, var4, var5);
        if (var6 > elytraTarget.unusedInt.getValue().intValue()) {
            this.slowTargetTicks = 0;
            this.previousChaseStability = false;
            this.elytraTargetStable = true;
        } else {
            this.slowTargetTicks++;
            if (this.elytraTargetStable) {
                this.previousChaseStability = true;
            }
            if (this.slowTargetTicks >= 3) {
                this.elytraTargetStable = false;
                this.slowTargetTicks = 0;
                this.previousChaseStability = false;
            } else {
                this.elytraTargetStable = this.previousChaseStability;
            }
        }
    }

    private boolean shouldPrepareSprintReset() {
        if (this.isPlayerInWater()) {
            return this.hasPredictedCritical();
        }
        if (!this.hasSprintResetCharge(true)) {
            return false;
        }
        if (this.attackDelayTicks > 1) {
            return false;
        }
        if (ModuleManager.INSTANCE.criticals().canForceCritical()) {
            return true;
        }
        int var1 = this.predictedCriticalTick();
        return var1 >= 0 && var1 <= 1;
    }

    public boolean canAttack(boolean var1) {
        return this.isAttackReady(var1, false);
    }

    public int attackDelayTicks() {
        int var1 = this.attackStyle.is("1.8") ? 0 : AttackCooldownUtils.ticksUntilCharged(mc.player);
        return Math.max(var1, this.attackDelayTicks);
    }

    public boolean usesContinuousRotation() {
        return this.rotationMode.is("Universal") || this.rotationMode.is("Neuro") || this.rotationMode.is("NeuroNew") || this.rotationMode.is("NeuroV6") || this.rotationMode.is("NeuroV7") || this.rotationMode.is("NeuroV8");
    }

    private boolean isGroundCollisionBlockingCritical(boolean var1) {
        return !var1 && WorldCollisionUtils.isStandingOnSlabOrSnow();
    }

    private boolean isValidTarget(ElytraTarget elytraTarget, LivingEntity entity) {
        if (entity instanceof LocalPlayer) {
            return false;
        }
        if (entity.tickCount >= 3 && !entity.isDeadOrDying()) {
            if (this.distanceToTarget(entity) > this.calculateTargetRange(elytraTarget)) {
                return false;
            }
            if (entity instanceof Player var3) {
                if (ModuleManager.INSTANCE.antiBot().isBot(entity)) {
                    return false;
                }
                if (FriendManager.INSTANCE.isFriend(var3)) {
                    return false;
                }
            }
            if (!this.targetTypes.isSelected("Невидимые") && entity.isInvisible()) {
                return false;
            }
            if (entity.isAlive()) {
                if (this.targetTypes.isSelected("Игроки") && entity instanceof Player && entity.getArmorValue() > 0) {
                    return true;
                }
                if (this.targetTypes.isSelected("Голые") && entity instanceof Player && entity.getArmorValue() == 0) {
                    return true;
                }
                if (this.targetTypes.isSelected("Животные") && entity instanceof Animal) {
                    return true;
                }
                if (this.targetTypes.isSelected("Жители") && (entity instanceof Villager || entity instanceof WanderingTrader)) {
                    return true;
                }
                if (this.targetTypes.isSelected("Мобы") && entity instanceof Mob && !(entity instanceof Animal) && !(entity instanceof Villager) && !(entity instanceof WanderingTrader)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public float targetingRangeExtension() {
        return this.targetingRange.getValue();
    }

    private static float blocksPerSecond(float var0, float var1, float var2) {
        return (float) Math.sqrt(var0 * var0 + var1 * var1 + var2 * var2) * 20.0F;
    }

    public float elytraPitchSpeedPercent() {
        return Mth.clamp(this.elytraPitchSpeed.getValue(), 1.0F, 100.0F);
    }

    public float pitchSpeedPercent() {
        return Mth.clamp(this.pitchSpeed.getValue(), 1.0F, 100.0F);
    }

    public boolean isWithinCrosshair(LivingEntity entity) {
        return mc.player != null && entity != null ? this.isMovementFrozen() || RaycastUtils.isLookingAt(mc.player.getYRot(), mc.player.getXRot(), this.effectiveAttackRange(), entity, this.wallCheckMode.is("Не бить через стены")) || !this.attackOptions.isSelected("Проверка наводки") : false;
    }

    public double distanceToTarget(LivingEntity entity) {
        return ((EntityExtensionAccess) mc.player).getDistanceEyePos(entity);
    }

    private boolean isMovementFrozen() {
        return PlayerVelocityTracker.INSTANCE.shouldFreeze();
    }

    @Subscribe
    private void onSprintState(SprintStateEvent event) {
        if (this.needsSprintReset() || this.sprintStoppedThisTick) {
            event.setAllowed(false);
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (mc.player != null && mc.level != null) {
            if (this.attackDelayTicks > 0) {
                this.attackDelayTicks--;
            }
            this.ticksSinceAttack = mc.player.onGround() ? this.ticksSinceAttack + 1 : 0;
            this.updateTarget();
            this.criticalController.update();
        }
    }

    private void updateAttackCycle(ElytraTarget elytraTarget) {
        if (!this.attackStyle.is("1.9") || !this.isSprintResetEnabled() || this.isPlayerInWater() || !this.sprintStoppedThisTick && this.attackDelayTicks <= 0 && this.hasSprintResetCharge(false)) {
            boolean var2 = false;
            if (this.attackOptions.isSelected("Рандомизация ударов") && this.attackCount % this.nextMissInterval == 0) {
                this.nextMissInterval = 0;
                var2 = true;
            }
            boolean var3 = this.isMaceFalling();
            boolean ready = this.attackTiming.isReady();
            boolean var5 = this.isAttackReady(var2, var3);
            if ((!ready || !var5) && !var2) {
                if (ready) {
                    ;
                }
                if (ready) {
                    ;
                }
            } else {
                boolean var6 = this.canHitCurrentTarget();
                if (var6) {
                    if (!this.shouldWaitForMaceFall()) {
                        this.tryAttackTarget(elytraTarget, var3);
                    }
                }
            }
        } else {
            if (!this.sprintStoppedThisTick && this.attackDelayTicks <= 0) {
                ;
            }
            if (!this.sprintStoppedThisTick) {
                if (this.attackDelayTicks > 0) {
                    String.valueOf("СТОП кд ауры " + this.attackDelayTicks);
                }
            }
        }
    }

    private boolean shouldAttackWithoutWaterCritical() {
        this.updateCriticalPrediction();
        return this.predictedInWater && !this.hasPredictedCritical();
    }

    private boolean isSprintResetEnabled() {
        return this.attackOptions.isSelected("Сбрасывать спринт");
    }

    private void updatePredictedAimPoint(ElytraTarget elytraTarget) {
        float value = elytraTarget.unusedFloat.getValue();
        float var3 = predictionDistanceOffset(value);
        Vec3 vector = switch ((String) elytraTarget.unusedMode.getValue()) {
            case "От скорости" ->
                this.targetPointTracker.aimPoint(target, value, var3);
            default ->
                this.targetPointTracker.predictedAimPoint(target, value + 2.0F, var3);
        };
        this.predictedAimPoint = this.targetPointTracker.getLastAimPoint();
        this.updateTargetDistance(vector);
    }

    public boolean isRaycastRequired() {
        return this.attackOptions.isSelected("Проверка наводки");
    }

    public ModeSetting getRotationMode() {
        return this.rotationMode;
    }

    private boolean hasCollisionNearPlayer() {
        int ticks = TickEvent.getTicks();
        if (this.collisionCheckTick != ticks) {
            this.collisionCheckTick = ticks;
            this.nearbyCollision = WorldCollisionUtils.containsWater(mc.player.getBoundingBox());
        }
        return this.nearbyCollision;
    }

    private float assistFraction(float var1) {
        return normalizePercent(var1);
    }

    public boolean canAttackWithCriticalPolicy() {
        return this.canCriticalHitNow() || this.shouldAttackWithoutWaterCritical() || this.criticalController.shouldAllowFallbackAttack();
    }

    private void updateTargetActions(ElytraTarget elytraTarget) {
        if (this.nextMissInterval == 0) {
            this.nextMissInterval = (int) GuiHitTestUtils.randomBetween(6.0F, 11.0F);
        }
        if (target != null) {
            if (this.needsSprintReset() && mc.player.isSprinting()) {
                mc.player.setSprinting(false);
                this.sprintStoppedThisTick = true;
            }
            this.updateElytraChaseState(elytraTarget, target);
            this.updatePredictedAimPoint(elytraTarget);
            this.aimPointSelector.updateSamples(target);
            this.aimPointSelector.selectMode(elytraTarget, this, target);
            this.rotationController.updateTick(50);
            this.updateAttackCycle(elytraTarget);
        }
    }

    private boolean attackedRecently() {
        return this.ticksSinceAttack <= mc.player.getCurrentItemAttackStrengthDelay() * 0.5F;
    }

    private int cooldownRemainingTicks() {
        long var1 = this.attackTiming.getCooldownMillis() - this.attackTiming.getTimer().elapsed();
        return var1 <= 0L ? 0 : Mth.ceil((float) var1 / 50.0F);
    }

    public boolean shouldUseMaceFall() {
        return target != null && this.isMaceAttack() && AttackItemUtils.isFalling(mc.player);
    }

    public boolean shouldAccelerateCriticals() {
        return this.accelerateAttacks.getValue();
    }

    public int predictedCriticalTick() {
        int var1 = this.criticalController.ticksUntilCritical();
        if (var1 >= 0) {
            return var1;
        }
        this.updateCriticalPrediction();
        return this.nextCriticalTick;
    }

    public boolean checksWalls() {
        return this.wallCheckMode.is("Не бить через стены");
    }

    private void updateTargetDistance(Vec3 vector) {
        float distance = (float) vector.distanceTo(mc.player.getEyePosition());
        float var3 = (float) this.distanceToTarget(target);
        this.targetDistance = this.chasingElytraTarget ? distance : var3;
    }

    private boolean isMaceAttack() {
        if (mc.player == null) {
            return false;
        }
        if (AttackItemUtils.isHoldingMace()) {
            return true;
        }
        MaceHelper maceHelper2 = ModuleManager.INSTANCE.maceHelper();
        return maceHelper2 != null && maceHelper2.isAttacking();
    }

    public float yawAssistStrength() {
        return this.assistFraction(this.rotationMode.is("Neuro") ? this.yawAssist.getValue() : this.rotationSettings().getAssistYaw());
    }

    public OnnxRotationModel getRotationModel() {
        return this.rotationModel;
    }

    private long attackCooldownMillis() {
        return this.attackStyle.is("1.8") ? this.clicksPerSecond.getValue().intValue() * 50L : 473L;
    }

    public float yawSpeedPercent() {
        return Mth.clamp(this.yawSpeed.getValue(), 1.0F, 100.0F);
    }

    public float elytraYawSpeedPercent() {
        return Mth.clamp(this.elytraYawSpeed.getValue(), 1.0F, 100.0F);
    }

    public boolean canHitCurrentTarget() {
        return this.isWithinCrosshair(target) || this.chasingElytraTarget;
    }

    public boolean isWithinAssistWindow() {
        if (mc.player == null) {
            return false;
        } else {
            float currentItemAttackStrengthDelay = mc.player.getCurrentItemAttackStrengthDelay();
            if (Float.isFinite(currentItemAttackStrengthDelay) && !(currentItemAttackStrengthDelay <= 0.0F)) {
                float var2 = this.rotationMode.is("Neuro") ? this.preAttackAssistMillis.getValue() : this.rotationSettings().getAssistWindow();
                return AttackCooldownUtils.ticksUntilCharged(mc.player) * 50.0F <= Mth.clamp(var2, 0.0F, 1000.0F);
            } else {
                return true;
            }
        }
    }

    private boolean isUsingRangedWeapon() {
        if (mc.player != null && mc.player.isUsingItem()) {
            ItemStack activeItem = mc.player.getActiveItem();
            return activeItem.is(Items.BOW) || activeItem.is(Items.CROSSBOW);
        } else {
            return false;
        }
    }

    public Rotation currentRotation() {
        return this.rotationController.currentRotation();
    }

    public float effectiveTargetRange() {
        return this.calculateTargetRange(ModuleManager.INSTANCE.elytraTarget());
    }

    private void updateRotation(boolean var1, int var2) {
        this.rotationController.updateRender(var1, var2, false);
    }

    public static LivingEntity currentTarget() {
        return target;
    }

    private float calculateTargetRange(ElytraTarget elytraTarget) {
        float var2 = mc.player.isFallFlying() && elytraTarget.isEnabled() ? elytraTarget.fallFlyingOffset.getValue() : 0.0F;
        return this.effectiveAttackRange() + this.targetingRangeExtension() + var2;
    }

    private void breakShield(Player player2) {
        Slot slot = InventoryUtils.findAxeSlot();
        if (player2.getActiveItem().getItem() == Items.SHIELD && slot != null) {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            int var4 = InventoryUtils.findAxeIndex(true);
            if (var4 != -1) {
                boolean var5 = selectedSlot != var4;
                if (var5) {
                    InventoryUtils.selectServerSlot(var4);
                }
                this.attack(player2);
                if (var5) {
                    InventoryUtils.selectServerSlot(selectedSlot);
                }
            }
        }
    }

    private static float serverAdjustedTickOffset(boolean var0, float var1) {
        return var0 ? -var1 + 1.5F : 1.5F;
    }

    private void attack(LivingEntity entity) {
        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        long var2 = this.attackCooldownMillis();
        this.attackTiming.startCooldown(var2);
        this.attackDelayTicks = this.attackStyle.is("1.8") ? this.clicksPerSecond.getValue() : 10;
        this.maceWaitTicks = 0;
        this.attackCount++;
    }

    private boolean isPlayerInWater() {
        return mc.player.isInWater();
    }

    private boolean shouldWaitForMaceFall() {
        if (mc.player == null || target == null) {
            return false;
        }
        if (!this.isMaceAttack() || !AttackItemUtils.isFallingForMace()) {
            return false;
        }
        if (mc.player.onGround() || mc.player.isFallFlying() || this.isPlayerInWater() || mc.player.fallDistance <= 0.0) {
            this.maceWaitTicks = 0;
            return false;
        }
        if (this.maceWaitTicks >= 100) {
            return false;
        }
        SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.predictClient(1);
        if (!simulatedPlayerInput.onGround && !simulatedPlayerInput.inWater) {
            if (simulatedPlayerInput.fallDistance <= mc.player.fallDistance) {
                return false;
            }
            if (!this.canHitPredictedTarget(simulatedPlayerInput)) {
                return false;
            }
            this.maceWaitTicks++;
            return true;
        } else {
            return false;
        }
    }

    @Subscribe
    private void onEntityDeath(EntityDeathEvent event) {
        if (this.attackOptions.isSelected("Выключать после смерти")) {
            if (event.getEntity() instanceof LocalPlayer) {
                this.toggle();
            }
        }
    }

    public int ticksUntilAttack() {
        if (mc.player == null) {
            return 0;
        }
        int var1 = Math.max(this.attackDelayTicks, this.cooldownRemainingTicks());
        if (this.attackStyle.is("1.8")) {
            return var1;
        }
        var1 = Math.max(var1, AttackCooldownUtils.ticksUntilCharged(mc.player, this.cooldownTickOffset(), this.requiredAttackStrength()));
        if (this.attackOptions.isSelected("Только криты")) {
            var1 = Math.max(var1, AttackCooldownUtils.ticksUntilFraction(mc.player, 0.5F, 0.9F) - ((LivingEntityMixin_1248) (Object) mc.player).getAttackStrengthTicker());
        }
        return var1;
    }

    public AuraAimPointSelector getAimPointSelector() {
        return this.aimPointSelector;
    }

    public float getCriticalPredictionOffset() {
        return 1.5F;
    }

    @Subscribe(3)
    private void applyCriticalInput(PlayerInputEvent event) {
        this.criticalController.modifyInput(event);
    }

    private boolean needsSprintReset() {
        if (mc.player != null && target != null && this.isEnabled() && this.isSprintResetEnabled()) {
            int ticks = TickEvent.getTicks();
            if (this.sprintResetCheckTick == ticks) {
                return this.sprintResetRequired;
            }
            this.sprintResetCheckTick = ticks;
            this.sprintResetRequired = this.shouldPrepareSprintReset();
            return this.sprintResetRequired;
        } else {
            return false;
        }
    }

    private boolean hasCriticalBlockingEffect() {
        return mc.player.hasEffect(MobEffects.BLINDNESS) || mc.player.hasEffect(MobEffects.LEVITATION) || mc.player.hasEffect(MobEffects.SLOW_FALLING) && !ModuleManager.INSTANCE.criticals().canPrepareSlowFallingCritical();
    }

    private boolean hasSprintResetCharge(boolean var1) {
        float var2 = 0.0F;
        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.isEmpty()) {
            var2 = 0.9F;
        }
        if (mainHandItem.is(ItemTags.SWORDS)) {
            var2 = 0.52F;
        }
        if (var1) {
            var2 -= 0.15F;
        }
        return mc.player.getAttackStrengthScale(0.0F) > var2;
    }

    private record ScoredTarget(LivingEntity entity, boolean elytraBias, double distanceScore, double healthScore, double fovScore, double armorScore) {
    }
}
