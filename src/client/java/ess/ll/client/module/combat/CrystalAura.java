package ess.ll.client.module.combat;

import ess.ll.client.integration.OptionalModules.AutoAnchor;
import ess.ll.client.core.CollisionBlockView;
import ess.ll.client.core.HitResultConstraint;
import ess.ll.client.core.SimulatedPlayerInput;
import ess.ll.client.event.AttackEntityEvent;
import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.HudRenderEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PostPlayerInputEvent;
import ess.ll.client.event.Render3DEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.CombatTargetUtils;
import ess.ll.client.features.combat.CrystalDamageUtils;
import ess.ll.client.features.combat.CrystalPlacementEvaluator;
import ess.ll.client.features.combat.CrystalPlacementRenderer;
import ess.ll.client.features.combat.CrystalTargetMovementTracker;
import ess.ll.client.features.combat.ExplosionDamageUtils;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.features.world.BlockActionQueue;
import ess.ll.client.features.world.BlockBreakManager;
import ess.ll.client.features.world.BlockPlacementManager;
import ess.ll.client.features.world.BlockPlacementTracker;
import ess.ll.client.features.world.WorldCollisionUtils;
import ess.ll.client.integration.EntityExtensionAccess;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.render.ColorUtils;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ElapsedTimer;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.PlayerUtils;
import ess.ll.client.util.RaycastUtils;
import ess.ll.client.util.RotationChangeTracker;
import ess.ll.client.util.RotationManager;
import ess.ll.client.util.ServerLatencyTracker;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class CrystalAura extends ClientModule {
    private final ToggleSetting placeAnchors;
    private final ess.ll.client.features.combat.CrystalPearlApproach pearlApproach = new ess.ll.client.features.combat.CrystalPearlApproach(this);
    private BlockPos anchorPreview;
    private BlockPos anchorAwaitingAck;
    private int anchorSequence = -1;
    private boolean chargingAnchor;

    /**
     * Crystals this client has hit, by entity id, with the tick of the hit. The server removes a hit crystal about a
     * ping later; until then it neither blocks the spot under it nor takes the next hit (which would cost a tick).
     */
    private static final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap HIT = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();

    public static boolean recentlyHit(net.minecraft.world.entity.Entity crystal) {
        if (mc.player == null || !HIT.containsKey(crystal.getId()))
            return false;
        return mc.player.tickCount - HIT.get(crystal.getId()) <= ServerLatencyTracker.INSTANCE.getLatencyTicks() + 3;
    }

    /**
     * Ticks a fresh crystal takes to show up: about one ping, plus the tick it is spawned in.
     */
    private static int spawnTicks() {
        return Math.max(2, ServerLatencyTracker.INSTANCE.getLatencyTicks() + 2);
    }

    /**
     * How far ahead the target's movement is simulated: its position when our packets arrive.
     */
    private static int predictTicks() {
        return Math.clamp(ServerLatencyTracker.INSTANCE.getLatencyTicks() + 1, 1, 4);
    }

    /**
     * A new crystal on the base of the one just hit, in the same tick. The server handles the hit first, so the base is
     * free by the time the placement arrives; vanilla sends the placement even though the client still sees the old
     * crystal there. The spot already passed the damage checks as the hit crystal, and the aim of the hit looks at it.
     */
    private void placeAfterHit(EndCrystal hit) {
        if (this.matrixBypass.isEnabled() || mc.player.tickCount == this.lastInteractTick)
            return;
        if (!this.placeTimer.isOlderThan(this.placementDelay.getValue().intValue() * 50L))
            return;
        this.placeCrystalOn(hit.blockPosition().below());
    }

    /**
     * The crystal on obsidian placed this tick for it: the evaluator chose the obsidian for the crystal that goes on
     * it, so the crystal follows in the same tick instead of waiting for the next one and the placement delay. The
     * client already shows the new block (vanilla predicts placements), and the server handles the obsidian first.
     */
    private void crystalOnNewObsidian(BlockPos base) {
        if (this.matrixBypass.isEnabled())
            return;
        BlockPos spot = base.above();
        if (!mc.level.getBlockState(spot).isAir())
            return;
        // The crystal's box is a block wide and two high; anything standing in it makes the server refuse.
        if (!mc.level.getEntities((Entity) null, new AABB(spot).expandTowards(0.0, 1.0, 0.0), Entity::isAlive).isEmpty())
            return;
        this.placeCrystalOn(base);
    }

    /**
     * Places a crystal on this base; the base and the spot above it were already checked by the caller.
     */
    private void placeCrystalOn(BlockPos base) {
        BlockState state = mc.level.getBlockState(base);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK))
            return;
        if (this.isBreakPending(base) || this.isBlacklisted(base) || risksFriend(Vec3.atBottomCenterOf(base.above())))
            return;
        int slot = InventoryUtils.findItemIndex(Items.END_CRYSTAL, true);
        if (slot == -1)
            return;
        BlockHitResult face = this.placementHit(base, null, Vec3.atCenterOf(base));
        if (face == null)
            return;
        int previous = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        try {
            if (mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, face).consumesAction() && this.showPlacement.isEnabled())
                this.placementRenderer.placed(base);
        } finally {
            mc.player.getInventory().setSelectedSlot(previous);
        }
        mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        RotationChangeTracker.record();
        this.lastInteractTick = mc.player.tickCount;
        this.placeTimer.reset();
        this.lastPlaceBlock = base;
        this.lastPlaceTick = mc.player.tickCount;
        this.expectedCrystalPos = base.above();
        this.expectedCrystalTick = mc.player.tickCount;
    }

    // Refresh friends at execution time as queued actions can outlive .friend add.
    public static boolean risksFriend(Vec3 explosion) {
        if (mc.level == null)
            return false;
        for (Player player : mc.level.players()) {
            if (NoFriendDamage.isFriend(player) && player.isAlive() && player.distanceToSqr(explosion) <= 144.0)
                return true;
        }
        return false;
    }

    private boolean tryAnchor(LivingEntity target) {
        if (!this.placeAnchors.isEnabled() || target.getY() > mc.player.getY() + 0.1)
            return false;
        this.rotationTarget = target;
        BlockPos selected = null;
        double best = Double.MAX_VALUE;
        boolean existing = false;
        double reach = Math.min(this.placeRange.getValue(), mc.player.blockInteractionRange());
        BlockPos feet = target.blockPosition();
        for (BlockPos cursor : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 0, 2))) {
            if (risksFriend(Vec3.atCenterOf(cursor)))
                continue;
            if (this.isBlacklisted(cursor) || this.isBreakPending(cursor))
                continue;
            if (mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(cursor)) > reach * reach)
                continue;
            BlockState state = mc.level.getBlockState(cursor);
            boolean anchor = state.is(Blocks.RESPAWN_ANCHOR);
            if (anchor && state.getValue(RespawnAnchorBlock.CHARGE) < 4 && InventoryUtils.findItemIndex(Items.GLOWSTONE, true) < 0)
                continue;
            if (!anchor && (!state.canBeReplaced() || ExplosionDamageUtils.isObstructed(cursor) || InventoryUtils.findItemIndex(Items.RESPAWN_ANCHOR, true) < 0 || InventoryUtils.findItemIndex(Items.GLOWSTONE, true) < 0 || this.findPlacementAim(cursor) == null))
                continue;
            if (anchor && this.crystalPlaceRotation(cursor) == null)
                continue;
            double score = Vec3.atCenterOf(cursor).distanceToSqr(target.position());
            if ((anchor && !existing) || anchor == existing && score < best) {
                selected = cursor.immutable();
                best = score;
                existing = anchor;
            }
        }
        if (selected == null)
            return false;
        this.anchorPreview = selected;
        this.stopMining();
        this.restoreSlot();
        BlockPlacementTracker.markPlacement();
        BlockPlacementTracker.reserve(this, selected);
        if (existing) {
            Vector2f aim = this.crystalPlaceRotation(selected);
            if (aim != null && BlockActionQueue.requestRotation(this, aim))
                this.deferPlace(selected, Items.GLOWSTONE, null);
        } else if (this.placeTimer.isOlderThan(this.placementDelay.getValue() * 50L)) {
            this.rotateAndPlace(selected, Items.RESPAWN_ANCHOR);
        }
        return true;
    }

    private void chargeAnchor(BlockPos pos) {
        if (risksFriend(Vec3.atCenterOf(pos)))
            return;
        if (mc.player.isSecondaryUseActive())
            return;
        if (pos.equals(this.anchorAwaitingAck))
            return;
        BlockState initial = mc.level.getBlockState(pos);
        if (!initial.is(Blocks.RESPAWN_ANCHOR))
            return;
        if (initial.getValue(RespawnAnchorBlock.CHARGE) == 4) {
            this.explodeAnchor(pos);
            return;
        }
        int slot = InventoryUtils.findItemIndex(Items.GLOWSTONE, true);
        if (slot < 0 || !mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR))
            return;
        BlockHitResult hit = this.placementHit(pos, null, Vec3.atCenterOf(pos));
        if (hit == null)
            return;
        int previous = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        this.chargingAnchor = true;
        try {
            // Send only the missing charges, then wait for vanilla's server reconciliation before retrying/exploding.
            int missing = 4 - initial.getValue(RespawnAnchorBlock.CHARGE);
            for (int i = 0; i < missing; i++) {
                BlockState state = mc.level.getBlockState(pos);
                if (!state.is(Blocks.RESPAWN_ANCHOR) || state.getValue(RespawnAnchorBlock.CHARGE) >= 4 || !mc.player.getMainHandItem().is(Items.GLOWSTONE))
                    break;
                if (!this.interactBlock(hit))
                    break;
            }
        } finally {
            this.chargingAnchor = false;
            mc.player.getInventory().setSelectedSlot(previous);
        }
    }

    private void explodeAnchor(BlockPos pos) {
        if (risksFriend(Vec3.atCenterOf(pos)))
            return;
        if (mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER))
            return;
        // A glowstone offhand redirects main-hand interaction into charging instead of detonating.
        if (mc.player.getOffhandItem().is(Items.GLOWSTONE))
            return;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (!mc.player.getInventory().getItem(i).is(Items.GLOWSTONE)) {
                slot = i;
                break;
            }
        }
        if (slot < 0)
            return;
        BlockHitResult hit = this.placementHit(pos, null, Vec3.atCenterOf(pos));
        if (hit == null)
            return;
        int previous = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        try {
            this.interactBlock(hit);
        } finally {
            mc.player.getInventory().setSelectedSlot(previous);
        }
    }

    private final NumberSetting<Float> targetRange;
    private final NumberSetting<Float> placeRange;
    private final NumberSetting<Float> breakRange;
    private final NumberSetting<Float> minimumTargetDamage;
    private final NumberSetting<Float> maximumSelfDamage;
    private final NumberSetting<Integer> placementDelay;
    private final NumberSetting<Integer> predictionTicks;
    private final ToggleSetting placeObsidian;
    private final ToggleSetting legitimateMode;
    private final ToggleSetting preventSuicide;
    private final ToggleSetting autoMine;
    private final ToggleSetting mineThroughWalls;
    private final ToggleSetting placeThroughWalls;
    private final ToggleSetting breakThroughWalls;
    private final ToggleSetting matrixBypass;
    private final ToggleSetting showPlacement;
    private final ToggleSetting protectSelf;
    private final ToggleSetting simulateDamage;
    private static final int PLACE_BOX_COLOR = ColorUtils.rgb(95, 170, 255);
    private static final int MINE_BOX_COLOR = ColorUtils.rgb(255, 55, 55);
    private final ElapsedTimer placeTimer;
    private final HitResultConstraint hitConstraint;
    private final BlockBreakManager breakManager;
    private final Map<BlockPos, Integer> blacklist;
    private final Map<BlockPos, Integer> keepBlocks;
    private final Map<BlockPos, Integer> blockFirstSeenTicks;
    private int awaitTicks;
    private int idleTicks;
    private final Map<BlockPos, Integer> pendingBreaks;
    private final Map<BlockPos, BlockState> confirmedBlockStates;
    private final Map<BlockPos, PendingBreak> pendingBreakSequences = new HashMap<>();

    /**
     * Obsidian queued as a crystal base; the crystal goes on it as soon as it is placed.
     */
    private BlockPos crystalBase;

    /**
     * The slot held before a dig took the tool, or -1.
     */
    private int digRestoreSlot = -1;

    private record PendingBreak(int sequence, boolean stopped) {
    }

    /**
     * Called on the client thread after vanilla reconciles predicted block changes.
     */
    public void acknowledgeBlockChanges(int sequence) {
        if (!this.isEnabled() || mc.level == null)
            return;
        if (this.anchorSequence >= 0 && sequence >= this.anchorSequence) {
            this.anchorAwaitingAck = null;
            this.anchorSequence = -1;
        }
        var entries = this.pendingBreakSequences.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            if (entry.getValue().sequence() > sequence)
                continue;
            BlockState state = mc.level.getBlockState(entry.getKey());
            // START can be acknowledged while the block is still being mined.
            if (entry.getValue().stopped() || !ExplosionDamageUtils.isMineable(state)) {
                this.confirmedBlockStates.put(entry.getKey(), state);
            }
            entries.remove();
        }
    }

    private BlockPos plannedBase;
    private BlockPos lastFailedBlock;
    private int failedBlockCount;
    private int planTick;
    private BlockPos lastPlaceBlock;
    private int lastPlaceTick;
    private BlockPos deferredPlacePos;
    private Item deferredPlaceItem;
    private ExplosionDamageUtils.PlacementFace deferredPlaceFace;
    private int deferredPlaceTick;
    private int previousSlot;
    private BlockPos expectedCrystalPos;
    private int expectedCrystalTick;
    private int attackChainTicks;
    private int mineCandidateTicks;
    private BlockPos mineCandidate;
    private final CrystalTargetMovementTracker movementTracker;
    private LivingEntity rotationTarget;

    public boolean requestAuraRotation(Vector2f aim, int priority) {
        return rotationTarget != null && ModuleManager.INSTANCE.aura().rotateCrystalAim(aim, rotationTarget, priority);
    }

    private SimulatedPlayerInput simulatedInput;
    private int simulationTick;
    private int lastSimulatedTick;
    private double attackDirectionLength;
    private double attackDirectionX;
    private double attackDirectionZ;
    private Vec3 pendingKnockback;
    private float attackYaw;
    private float attackKnockback;
    private boolean targetHurtOrBlocking;
    private int lastAttackTick;
    private final Deque<CrystalAura.PendingSelfDamage> pendingSelfDamage;
    private volatile int lastSelfDamageTick;
    private int lastInteractTick;
    private final CrystalPlacementRenderer placementRenderer;
    private BlockPos mineBoxPos;
    private BlockState mineBoxState;
    private float mineBoxProgress;
    private long mineBoxTime;

    public static boolean isAnyCrystalAuraEnabled() {
        CrystalAura crystalAura2 = ModuleManager.INSTANCE.crystalAura();
        var var1 = ModuleManager.INSTANCE.crystalAuraV2();
        return crystalAura2 != null && crystalAura2.isEnabled() || var1 != null && var1.isEnabled();
    }

    private boolean shouldProtectSelf(Player player2) {
        if (!this.protectSelf.isEnabled()) {
            return false;
        }
        if (InventoryUtils.findItemIndex(Items.OBSIDIAN, true) == -1) {
            return false;
        }
        double var2 = player2.getX() - mc.player.getX();
        double var4 = player2.getZ() - mc.player.getZ();
        if (Math.sqrt(var2 * var2 + var4 * var4) > this.placeRange.getValue().floatValue() + 1.5) {
            return false;
        }
        if (Math.abs(player2.getY() - mc.player.getY()) > 1.0) {
            return false;
        }
        Direction direction = Math.abs(var2) > Math.abs(var4) ? (var2 > 0.0 ? Direction.EAST : Direction.WEST) : (var4 > 0.0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos blockPos = mc.player.blockPosition().relative(direction);
        if (!mc.level.getBlockState(blockPos).canBeReplaced()) {
            return false;
        }
        if (ExplosionDamageUtils.isObstructed(blockPos)) {
            return false;
        }
        this.restoreSlot();
        this.rotateAndPlace(blockPos, Items.OBSIDIAN);
        return true;
    }

    private BlockHitResult placementHit(BlockPos blockPos, Direction direction, Vec3 vector) {
        if (this.matrixBypass.isEnabled() && !this.placeThroughWalls.isEnabled()) {
            return BlockPlacementManager.hitFromAnyEyePosition(blockPos, direction);
        }
        this.hitConstraint.lockTo(blockPos);
        try {
            double var4 = Math.min(mc.player.getEyePosition().distanceTo(vector) + 1.0, mc.player.blockInteractionRange());
            if (mc.player.pick(var4, 1.0F, false) instanceof BlockHitResult var7 && var7.getBlockPos().equals(blockPos) && (direction == null || var7.getDirection() == direction))
                return var7;
            // The Aura rotation is silent in this client. Keep the source face and
            // reach checks, but raycast the evaluated aim instead of the visible view.
            Vec3 eye = mc.player.getEyePosition();
            if (eye.distanceTo(vector) > mc.player.blockInteractionRange() + 0.25)
                return null;
            BlockHitResult ray = mc.level.clip(new ClipContext(eye, vector, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            if (ray.getBlockPos().equals(blockPos) && (direction == null || ray.getDirection() == direction))
                return new BlockHitResult(vector, ray.getDirection(), blockPos, false);
            // "Сквозь стены": the evaluator already chose this face knowing it is hidden. Requiring a clear ray here
            // failed every such placement, and five failures set the spot aside, so a freshly dug base sat unused
            // while the aura dug elsewhere. The server only checks the reach, which is checked above.
            if (this.placeThroughWalls.isEnabled())
                return new BlockHitResult(vector, direction != null ? direction : Direction.UP, blockPos, false);
            return null;
        } finally {
            this.hitConstraint.clear();
        }
    }

    private void placeBlockAt(BlockPos blockPos, Item item, ExplosionDamageUtils.PlacementFace var3) {
        if (mc.player.tickCount != this.lastInteractTick) {
            int var4 = InventoryUtils.findItemIndex(item, true);
            if (var4 != -1) {
                BlockHitResult blockHitResult = this.placementHit(var3.neighbor(), var3.side(), var3.aim());
                if (blockHitResult != null && blockHitResult.getBlockPos().relative(blockHitResult.getDirection()).equals(blockPos)) {
                    int selectedSlot = mc.player.getInventory().getSelectedSlot();
                    mc.player.getInventory().setSelectedSlot(var4);
                    boolean var7;
                    try {
                        var7 = this.interactBlock(blockHitResult);
                    } finally {
                        mc.player.getInventory().setSelectedSlot(selectedSlot);
                    }
                    this.onPlacementResult(blockPos, var7);
                    if (var7) {
                        CollisionBlockView.INSTANCE.invalidate();
                        this.lastPlaceBlock = blockPos;
                        this.lastPlaceTick = mc.player.tickCount;
                        if (item == Items.OBSIDIAN) {
                            this.keepBlocks.put(blockPos.immutable(), mc.player.tickCount + 200);
                            // A dug spot waits for the server to confirm the break; the obsidian now fills it, so it no
                            // longer has anything to wait for.
                            this.pendingBreaks.remove(blockPos);
                            this.confirmedBlockStates.remove(blockPos);
                            this.blockFirstSeenTicks.remove(blockPos);
                            if (blockPos.equals(this.crystalBase)) {
                                this.crystalBase = null;
                                this.crystalOnNewObsidian(blockPos);
                            }
                        }
                        if (item == Items.RESPAWN_ANCHOR)
                            this.chargeAnchor(blockPos);
                    }
                } else {
                    if (blockHitResult == null) {
                        String.valueOf("блок: луч не дошёл до грани " + var3.neighbor().toShortString());
                    } else {
                        String.valueOf("блок: луч попал в " + blockHitResult.getBlockPos().toShortString() + "/" + blockHitResult.getDirection() + ", не в грань к цели");
                    }
                    this.onPlacementResult(blockPos, false);
                }
            }
        }
    }

    @Subscribe
    private void onAttackEntity(AttackEntityEvent event) {
        if (mc.player != null && event.getEntity() instanceof Player var2) {
            if (event.getPhase() != AttackEntityEvent.Phase.PRE) {
                Vec3 vector = this.pendingKnockback;
                this.pendingKnockback = null;
                if (vector != null && !this.targetHurtOrBlocking && isAuraTarget(var2)) {
                    SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.of(var2, SimulatedPlayerInput.SimulatedInput.guessInput(var2));
                    Vec3 velocity2 = simulatedPlayerInput.velocity;
                    simulatedPlayerInput.velocity = CrystalDamageUtils.predictKnockback(var2, velocity2, simulatedPlayerInput.onGround, vector, this.attackYaw, this.attackKnockback);
                    double var6 = simulatedPlayerInput.velocity.x - velocity2.x / 2.0;
                    double var8 = simulatedPlayerInput.velocity.z - velocity2.z / 2.0;
                    this.attackDirectionLength = Math.sqrt(var6 * var6 + var8 * var8);
                    this.attackDirectionX = this.attackDirectionLength > 0.0 ? var6 / this.attackDirectionLength : 0.0;
                    this.attackDirectionZ = this.attackDirectionLength > 0.0 ? var8 / this.attackDirectionLength : 0.0;
                    this.simulatedInput = simulatedPlayerInput;
                    this.simulationTick = mc.player.tickCount;
                    this.lastSimulatedTick = this.simulationTick;
                }
            } else {
                this.pendingKnockback = mc.player.position();
                this.attackYaw = mc.player.getYRot();
                this.attackKnockback = CrystalDamageUtils.attackKnockback();
                this.targetHurtOrBlocking = var2.hurtTime > 0 || var2.isBlocking();
            }
        }
    }

    private void renderMineBox(Render3DEvent event, long var5) {
        if (this.mineBoxPos != null) {
            long var7 = var5 - this.mineBoxTime;
            float var9;
            if (var7 <= 140L) {
                var9 = 1.0F;
            } else {
                float var10 = (float) (var7 - 140L) / 160.0F;
                if (var10 >= 1.0F) {
                    this.mineBoxPos = null;
                    return;
                }
                var9 = 1.0F - var10;
            }
            float var12 = 0.55F + 0.45F * this.mineBoxProgress;
            int var11 = (int) ((0.45F + 0.4F * this.mineBoxProgress) * var9 * 255.0F);
            CrystalPlacementRenderer.fillBox(event, this.mineBoxBounds(this.mineBoxPos, var12), ColorUtils.withAlpha(MINE_BOX_COLOR, var11));
        }
    }

    private void clearDeferredPlace() {
        this.deferredPlacePos = null;
        this.deferredPlaceItem = null;
        this.deferredPlaceFace = null;
        this.deferredPlaceTick = -100;
    }

    private boolean interactBlock(BlockHitResult blockHitResult) {
        Item held = mc.player.getMainHandItem().getItem();
        if (held == Items.END_CRYSTAL && risksFriend(Vec3.atBottomCenterOf(blockHitResult.getBlockPos().above())))
            return false;
        if ((held == Items.RESPAWN_ANCHOR || mc.level.getBlockState(blockHitResult.getBlockPos()).is(Blocks.RESPAWN_ANCHOR)) && risksFriend(Vec3.atCenterOf(blockHitResult.getBlockPos())))
            return false;
        // Item interactions return a Success carrying the transformed held stack.
        // Comparing it to the singleton SUCCESS incorrectly treats a placement as failed.
        if (!mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult).consumesAction()) {
            return false;
        }
        if (held == Items.END_CRYSTAL && this.showPlacement.isEnabled()) {
            this.placementRenderer.placed(blockHitResult.getBlockPos());
        }
        mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        RotationChangeTracker.record();
        this.lastInteractTick = mc.player.tickCount;
        this.placeTimer.reset();
        return true;
    }

    private boolean applyDigDecision(CrystalPlacementEvaluator.Decision var1, boolean var2) {
        if (!var2) {
            return false;
        }
        BlockPos blockPos = var1.pos();
        if (this.isBreakPending(blockPos)) {
            this.onPlacementResult(blockPos, false);
            return false;
        }
        if (var1.action() == CrystalPlacementEvaluator.Action.PLACE_CRYSTAL && this.isAwaitingCrystalSpawn(blockPos)) {
            return false;
        }
        BlockPlacementTracker.markPlacement();
        BlockPlacementTracker.reserve(this, blockPos);
        BlockPlacementTracker.reserve(this, blockPos.above());
        boolean var4;
        if (var1.action() == CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN) {
            this.restoreSlot();
            var4 = this.rotateAndPlace(blockPos, Items.OBSIDIAN);
            if (var4)
                this.crystalBase = blockPos.immutable();
        } else {
            var4 = this.tryPlaceCrystal(blockPos);
        }
        // Enqueueing is not a successful placement. Keep failures until useItemOn succeeds.
        if (!var4)
            this.onPlacementResult(blockPos, false);
        return var4;
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        ExplosionDamageUtils.setIgnorePlayerProtection(this.simulateDamage.isEnabled());
        if (mc.player != null && mc.level != null && mc.gameMode != null && !mc.player.isSpectator()) {
            if (!this.showPlacement.isEnabled()) {
                this.placementRenderer.clear();
            }
            if (!this.legitimateMode.isEnabled()) {
                this.restoreSlot();
            }
            this.pruneBlockTracking();
            this.handleBrokenBlock();
            this.tickKnockbackSimulation();
            this.rotationTarget = null;
            this.anchorPreview = null;
            if (ModuleManager.INSTANCE.webTrap().crystalsWait()) {
                this.movementTracker.reset();
                this.simulatedInput = null;
                this.resetAttackChain();
                return;
            }
            var var2 = this.findTargets();
            if (var2.isEmpty()) {
                if (this.attackMobWithCrystal())
                    return;
                this.movementTracker.reset();
                this.simulatedInput = null;
                this.resetAttackChain();
            } else {
                if (this.tryAnchor(var2.getFirst()))
                    return;
                Vec3 eyePosition = mc.player.getEyePosition();
                CrystalPlacementEvaluator.Settings var4 = this.buildSettings();
                // The delay only spaces out placements; a crystal that can be hit is hit at once.
                boolean var5 = (this.simulatedInput != null || this.placeTimer.isOlderThan(this.placementDelay.getValue().intValue() * 50L)) && !this.breakManager.sentActionThisTick();
                Player player2 = null;
                AABB bounds = null;
                CrystalPlacementEvaluator.MiningContext var8 = null;
                CrystalPlacementEvaluator.Decision var9 = null;
                CrystalPlacementEvaluator.Decision var10 = null;
                int var11 = Math.min(var2.size(), 3);
                for (int var12 = 0; var12 < var11; var12++) {
                    Player player3 = (Player) var2.get(var12);
                    boolean var14 = this.movementTracker.tracks(player3);
                    if (var14) {
                        this.movementTracker.record(player3);
                    }
                    AABB bounds2 = this.predictedTargetBounds(player3);
                    CrystalPlacementEvaluator.Decision var16 = CrystalPlacementEvaluator.evaluate(player3, bounds2, eyePosition, var4);
                    CrystalPlacementEvaluator.MiningContext var17 = var16 == null ? this.buildMiningContext(var14 ? this.movementTracker.averageSpeed() : 0.0F, !this.isSimulatedTarget(player3)) : null;
                    CrystalPlacementEvaluator.Decision var18 = var17 == null ? null : CrystalPlacementEvaluator.evaluateWithMining(player3, bounds2, eyePosition, var4, var17);
                    boolean var19 = var16 != null || var18 != null;
                    if (var12 == 0 || var19) {
                        player2 = player3;
                        bounds = bounds2;
                        var9 = var16;
                        var10 = var18;
                    }
                    if (var19) {
                        break;
                    }
                }
                if (!this.movementTracker.tracks(player2)) {
                    this.movementTracker.record(player2);
                }
                this.rotationTarget = player2;
                if (var9 != null) {
                    var8 = this.buildMiningContext(this.movementTracker.averageSpeed(), !this.isSimulatedTarget(player2));
                    var10 = CrystalPlacementEvaluator.evaluateWithMining(player2, bounds, eyePosition, var4, var8);
                    if (var5 && this.prefersPlacement(var10, var9, eyePosition)) {
                        this.attackChainTicks = 0;
                        this.stopMining();
                        if (this.applyDigDecision(var10, true)) {
                            return;
                        }
                    }
                    BlockPlacementTracker.markPlacement();
                    this.stopMining();
                    this.reserveBase(var9.pos().below());
                    if (this.isInBreakRange(var9.crystal())) {
                        this.placeAfterHit(var9.crystal());
                    }
                    return;
                }
                if (this.tryExplodeExpected(player2, bounds, var4)) {
                    BlockPlacementTracker.markPlacement();
                    this.stopMining();
                    return;
                }
                if (var10 == null) {
                    this.tickAttackChain(player2);
                } else {
                    if (var10.action() == CrystalPlacementEvaluator.Action.DIG) {
                        CrystalPlacementEvaluator.Decision var22 = this.decideAttackCombo(player2, eyePosition, var4);
                        if (var22 != null) {
                            this.attackChainTicks = 0;
                            this.stopMining();
                            this.restoreSlot();
                            if (var22.action() == CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN) {
                                this.applyDigDecision(var22, var5);
                            }
                            return;
                        }
                    }
                    this.attackChainTicks = 0;
                    if (var10.action() == CrystalPlacementEvaluator.Action.AWAIT) {
                        this.awaitTicks++;
                        this.idleTicks = 0;
                    } else if (++this.idleTicks >= 20) {
                        this.awaitTicks = 0;
                    }
                    this.reserveBase(var10.base() != null ? var10.base() : var10.pos());
                    if (var10.clears() != null) {
                        this.plannedBase = var10.clears();
                        this.planTick = mc.player.tickCount;
                    } else if (var10.base() != null) {
                        this.plannedBase = var10.base();
                        this.planTick = mc.player.tickCount;
                    }
                    if (var10.action() == CrystalPlacementEvaluator.Action.DIG) {
                        this.restoreSlot();
                        if (this.isMineCandidateStable(var10.pos())) {
                            this.startMining(var10.pos());
                        }
                    } else {
                        this.stopMining();
                        if (var10.action() == CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN || var10.action() == CrystalPlacementEvaluator.Action.PLACE_CRYSTAL) {
                            this.applyDigDecision(var10, var5);
                        }
                    }
                }
            }
        } else {
            this.movementTracker.reset();
            this.simulatedInput = null;
            this.resetAttackChain();
        }
    }

    /**
     * Keep the original player path unchanged; use its evaluator and action queue for mobs.
     */
    private boolean attackMobWithCrystal() {
        LivingEntity mob = null;
        double best = this.targetRange.getValue() * this.targetRange.getValue();
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player || living instanceof Player || !living.isAlive() || living.isRemoved())
                continue;
            double distance = mc.player.distanceToSqr(living);
            if (distance < best) {
                best = distance;
                mob = living;
            }
        }
        if (mob == null)
            return false;
        this.rotationTarget = mob;
        if (this.tryAnchor(mob))
            return true;
        this.movementTracker.reset();
        this.simulatedInput = null;
        Vec3 eye = mc.player.getEyePosition();
        AABB box = mob.getBoundingBox();
        CrystalPlacementEvaluator.Settings settings = this.buildSettings();
        CrystalPlacementEvaluator.Decision explode = CrystalPlacementEvaluator.evaluateExplosions(mob, box, eye, settings, null);
        if (explode != null && explode.crystal() != null) {
            BlockPlacementTracker.markPlacement();
            this.stopMining();
            this.reserveBase(explode.pos().below());
            if (this.isInBreakRange(explode.crystal()))
                this.placeAfterHit(explode.crystal());
            return true;
        }
        boolean ready = this.placeTimer.isOlderThan(this.placementDelay.getValue().intValue() * 50L) && !this.breakManager.sentActionThisTick();
        CrystalPlacementEvaluator.Decision place = CrystalPlacementEvaluator.evaluatePlacements(mob, box, eye, settings, this.buildMiningContext(0.0F, true), null);
        if (place == null) {
            this.resetAttackChain();
            return true;
        }
        this.attackChainTicks = 0;
        if (place.action() == CrystalPlacementEvaluator.Action.AWAIT) {
            this.awaitTicks++;
            this.idleTicks = 0;
        } else if (++this.idleTicks >= 20) {
            this.awaitTicks = 0;
        }
        this.reserveBase(place.base() != null ? place.base() : place.pos());
        if (place.clears() != null) {
            this.plannedBase = place.clears();
            this.planTick = mc.player.tickCount;
        } else if (place.base() != null) {
            this.plannedBase = place.base();
            this.planTick = mc.player.tickCount;
        }
        if (place.action() == CrystalPlacementEvaluator.Action.DIG) {
            this.restoreSlot();
            if (this.isMineCandidateStable(place.pos()))
                this.startMining(place.pos());
        } else if (place.action() == CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN || place.action() == CrystalPlacementEvaluator.Action.PLACE_CRYSTAL) {
            this.stopMining();
            this.applyDigDecision(place, ready);
        } else {
            this.stopMining();
        }
        return true;
    }

    private boolean isSimulatedTarget(Player player2) {
        SimulatedPlayerInput simulatedPlayerInput = this.simulatedInput;
        return simulatedPlayerInput != null && simulatedPlayerInput.player == player2;
    }

    private boolean isAwaitingCrystalSpawn(BlockPos blockPos) {
        if (this.expectedCrystalPos != null && this.expectedCrystalPos.equals(blockPos.above())) {
            int var2 = mc.player.tickCount - this.expectedCrystalTick;
            return var2 >= 0 && var2 < spawnTicks() && this.findCrystalAt(this.expectedCrystalPos) == null;
        } else {
            return false;
        }
    }

    private AABB mineBoxBounds(BlockPos blockPos, float var2) {
        double var3 = blockPos.getX() + 0.5;
        double var5 = blockPos.getY() + 0.5;
        double var7 = blockPos.getZ() + 0.5;
        double var9 = 0.5 * Mth.clamp(var2, 0.05F, 1.0F);
        return new AABB(var3 - var9, var5 - var9, var7 - var9, var3 + var9, var5 + var9, var7 + var9);
    }

    private static boolean isAuraTarget(Player player) {
        AttackAura attackAura = ModuleManager.INSTANCE.aura();
        return attackAura != null && attackAura.isEnabled() && AttackAura.target == player;
    }

    private boolean prefersPlacement(CrystalPlacementEvaluator.Decision var1, CrystalPlacementEvaluator.Decision var2, Vec3 vector) {
        if (var1 != null && var1.action() == CrystalPlacementEvaluator.Action.PLACE_CRYSTAL) {
            float var4 = var1.enemyDamage() - var2.enemyDamage();
            if (!(var4 < 2.0F) && !(var1.enemyDamage() < var2.enemyDamage() * 1.5F)) {
                float value = this.breakRange.getValue();
                return ExplosionDamageUtils.distanceSquaredToCrystal(vector, ExplosionDamageUtils.crystalBaseTop(var1.pos())) <= value * value;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean hasBlockPlacementModules() {
        WebTrap webTrap2 = ModuleManager.INSTANCE.webTrap();
        AutoAnchor autoAnchor2 = ModuleManager.INSTANCE.autoAnchor();
        return webTrap2 != null && webTrap2.isEnabled() || autoAnchor2 != null && autoAnchor2.isEnabled();
    }

    private boolean tryExplodeExpected(Player player2, AABB bounds, CrystalPlacementEvaluator.Settings var3) {
        if (this.expectedCrystalPos == null) {
            return false;
        }
        if (mc.player.tickCount - this.expectedCrystalTick > 20) {
            this.expectedCrystalPos = null;
            return false;
        }
        EndCrystal endCrystal = this.findCrystalAt(this.expectedCrystalPos);
        if (endCrystal != null && !endCrystal.isRemoved()) {
            float value = this.breakRange.getValue();
            if (ExplosionDamageUtils.distanceSquaredTo(mc.player.getEyePosition(), endCrystal.getBoundingBox()) > value * value) {
                this.expectedCrystalPos = null;
                return false;
            }
            float var6 = ExplosionDamageUtils.damageTo(endCrystal.position(), mc.player);
            float var7 = ExplosionDamageUtils.damageTo(endCrystal.position(), player2, bounds);
            if (!CrystalPlacementEvaluator.isSelfDamageSafe(var6, var7, var3, this.expectedCrystalPos.below())) {
                this.expectedCrystalPos = null;
                return false;
            }
            boolean var8 = this.canExplode(endCrystal, value);
            if (var8) {
                this.expectedCrystalPos = null;
                this.placeAfterHit(endCrystal);
            }
            return var8;
        } else {
            int var5 = mc.player.tickCount - this.expectedCrystalTick;
            if (var5 < 0 || var5 >= spawnTicks()) {
                this.expectedCrystalPos = null;
            }
            return false;
        }
    }

    private boolean isInBreakRange(EndCrystal endCrystal) {
        return this.canExplode(endCrystal, this.breakRange.getValue());
    }

    public CrystalAura() {
        super(ModuleCategory.Combat, ModuleSubCategory.Utility);
        this.targetRange = new NumberSetting<Float>("Радиус цели").withMin(4.0F).withMax(12.0F).withStep(0.5F).withDefault(8.0F).withContainer(this);
        this.placeRange = new NumberSetting<Float>("Дальность установки").withMin(3.0F).withMax(6.0F).withStep(0.1F).withDefault(4.5F).withContainer(this);
        this.breakRange = new NumberSetting<Float>("Дальность взрыва").withMin(1.0F).withMax(6.0F).withStep(0.1F).withDefault(3.0F).withContainer(this);
        this.minimumTargetDamage = new NumberSetting<Float>("Мин. урон врагу").withMin(1.0F).withMax(20.0F).withStep(0.5F).withDefault(3.0F).withDescription("По Prot 4 незериту кристалл даёт ~4.5 урона — выше 3 аура перестанет находить точки").withContainer(this);
        this.maximumSelfDamage = new NumberSetting<Float>("Макс. урон себе").withMin(0.0F).withMax(20.0F).withStep(0.5F).withDefault(8.0F).withContainer(this);
        this.placementDelay = new NumberSetting<Integer>("Задержка установки").withMin(0).withMax(10).withStep(1).withDefault(0).withDescription("Тики между установками. 0 — каждый тик; взрыв не ждёт задержку никогда").withContainer(this);
        this.predictionTicks = new NumberSetting<Integer>("Предикт тиков").withMin(0).withMax(4).withStep(1).withDefault(2);
        this.placeObsidian = new ToggleSetting("Ставить обсидиан").withDefault(true).withContainer(this);
        this.placeAnchors = new ToggleSetting("Ставить анчорки").withDefault(false).withDescription("Цель на одном уровне или ниже: якорь с четырьмя зарядами. Выше — кристалл.").withContainer(this);
        // Off the menu: legit slot, anti-suicide (always on), self-save and damage emulation keep their defaults,
        // and the three "through walls" toggles are one.
        this.legitimateMode = new ToggleSetting("Легитный").withDefault(false);
        this.preventSuicide = new ToggleSetting("Анти-суицид").withDefault(true);
        this.autoMine = new ToggleSetting("Авто подкоп").withDefault(true).withContainer(this);
        this.mineThroughWalls = new ToggleSetting("Копать сквозь стены").withDefault(true).withDescription("Grim и Matrix видимость ломаемого блока не проверяют; без этого яма глубже одного блока недостижима");
        this.placeThroughWalls = new ToggleSetting("Сквозь стены").withDefault(true).withDescription("Ставить, взрывать и копать то, что не видно. Видимая грань и видимый кристалл всё равно в приоритете").withContainer(this);
        this.breakThroughWalls = new ToggleSetting("Взрывать сквозь стены").withDefault(true).withDescription("Выкл — только кристаллы, видимые реальным лучом. Вкл — без Matrix луч сквозь блоки, под Matrix через высоты глаз 1.62/1.27/0.4");
        this.matrixBypass = new ToggleSetting("Обход Matrix").withDefault(false).withDescription("По исходникам Matrix: взрыв только видимых с его высот глаз кристаллов не дальше 5 блоков, копка сквозь стены с Efficiency IV+, поставка как без обхода, наводка как MatrixRotation").withContainer(this);
        this.showPlacement = new ToggleSetting("Визуализация").withDefault(true).withContainer(this);
        this.protectSelf = new ToggleSetting("Сейвить себя").withDefault(false);
        this.simulateDamage = new ToggleSetting("Эмуляция урона").withDefault(false).withDescription("Для серверов, где сложность мирная или лёгкая, а урон по игрокам идёт как обычно: аура считает урон кристалла как на нормальной сложности");
        this.placeTimer = new ElapsedTimer();
        this.hitConstraint = new HitResultConstraint();
        this.breakManager = BlockBreakManager.INSTANCE;
        this.blacklist = new HashMap<>();
        this.keepBlocks = new HashMap<>();
        this.blockFirstSeenTicks = new HashMap<>();
        this.pendingBreaks = new ConcurrentHashMap<>();
        this.confirmedBlockStates = new ConcurrentHashMap<>();
        this.planTick = -100;
        this.lastPlaceTick = -100;
        this.deferredPlaceTick = -100;
        this.previousSlot = -1;
        this.expectedCrystalTick = -100;
        this.movementTracker = new CrystalTargetMovementTracker();
        this.lastAttackTick = -100;
        this.pendingSelfDamage = new ArrayDeque<>();
        this.lastSelfDamageTick = -100;
        this.lastInteractTick = -100;
        this.placementRenderer = new CrystalPlacementRenderer();
        this.setName("Crystal Aura");
    }

    private boolean continueMining() {
        if (!this.breakManager.isOwnedBy(this)) {
            return false;
        }
        BlockPos blockPos = this.breakManager.targetPosition();
        if (blockPos == null || this.isBlacklisted(blockPos)) {
            return false;
        }
        if (ExplosionDamageUtils.isMineable(mc.level.getBlockState(blockPos)) && !ExplosionDamageUtils.isClearOfFluid(blockPos)) {
            float value = this.placeRange.getValue();
            if (ExplosionDamageUtils.distanceSquaredToBlock(mc.player.getEyePosition(), blockPos) > value * value) {
                return false;
            }
            this.startMining(blockPos);
            return true;
        } else {
            return false;
        }
    }

    private EndCrystal findCrystalAt(BlockPos blockPos) {
        var entitiesOfClass = mc.level.getEntitiesOfClass(EndCrystal.class, ExplosionDamageUtils.crystalBounds(blockPos), var0 -> true);
        return entitiesOfClass.isEmpty() ? null : (EndCrystal) entitiesOfClass.get(0);
    }

    @Subscribe
    private void onPacket(PacketEvent event) {
        if (this.chargingAnchor && event.isSend() && event.getPacket() instanceof ServerboundUseItemOnPacket use) {
            this.anchorAwaitingAck = use.hitResult().getBlockPos().immutable();
            this.anchorSequence = use.sequence();
        }
        if (event.isSend() && event.getPacket() instanceof ServerboundPlayerActionPacket action && this.breakManager.isOwnedBy(this) && action.getPos().equals(this.breakManager.targetPosition())) {
            switch (action.getAction()) {
                case START_DESTROY_BLOCK, STOP_DESTROY_BLOCK ->
                    this.pendingBreakSequences.put(action.getPos().immutable(), new PendingBreak(action.getSequence(), action.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK));
                case ABORT_DESTROY_BLOCK ->
                    this.pendingBreakSequences.remove(action.getPos());
                default ->
                    {
                    }
            }
        }
        if (event.isReceive()) {
            if (event.getPacket() instanceof ClientboundSetHealthPacket) {
                this.lastSelfDamageTick = mc.player == null ? -100 : mc.player.tickCount;
            } else if (!this.pendingBreaks.isEmpty() || this.breakManager.targetPosition() != null) {
                if (event.getPacket() instanceof ClientboundBlockUpdatePacket var2) {
                    this.recordBlockUpdate(var2.getPos(), var2.getBlockState());
                } else if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket var3) {
                    var3.runUpdates(this::recordBlockUpdate);
                }
            }
        }
    }

    private void recordSelfDamage(float var1) {
        if (var1 > 0.0F) {
            this.pendingSelfDamage.add(new CrystalAura.PendingSelfDamage(mc.player.tickCount, var1));
        }
    }

    private boolean tryPlaceCrystal(BlockPos blockPos) {
        if (InventoryUtils.findItemIndex(Items.END_CRYSTAL, true) == -1) {
            return false;
        } else if (this.isPlaceDebounced(blockPos)) {
            return false;
        } else {
            Vector2f vector = this.matrixBypass.isEnabled() ? BlockPlacementManager.blockRotation(blockPos, this.placeRange.getValue().floatValue(), this.placeThroughWalls.isEnabled()) : this.crystalPlaceRotation(blockPos);
            if (vector != null && BlockActionQueue.requestRotation(this, vector)) {
                this.deferPlace(blockPos, Items.END_CRYSTAL, null);
                return true;
            } else {
                return false;
            }
        }
    }

    private Vector2f crystalAimRotation(EndCrystal endCrystal, float var2) {
        Vec3 eyePosition = mc.player.getEyePosition();
        if (this.matrixBypass.isEnabled()) {
            Vec3 vector = ExplosionDamageUtils.crystalAimPoint(endCrystal, eyePosition, var2, true);
            return vector == null ? null : BlockPlacementManager.entityRotation(endCrystal, vector, var2);
        } else {
            Vec3 vector2 = this.placeThroughWalls.isEnabled() ? ExplosionDamageUtils.crystalAimPointOrCenter(endCrystal, eyePosition, var2) : ExplosionDamageUtils.crystalAimPoint(endCrystal, eyePosition, var2);
            return vector2 == null ? null : this.lookRotation(eyePosition, vector2);
        }
    }

    private boolean isPlaceDebounced(BlockPos blockPos) {
        return blockPos.equals(this.lastPlaceBlock) && mc.player.tickCount - this.lastPlaceTick < 1;
    }

    @Subscribe
    private void onPostPlayerInput(PostPlayerInputEvent event) {
        this.pearlApproach.tick(this.rotationTarget);
        if (this.deferredPlaceItem != null) {
            BlockPos blockPos = this.deferredPlacePos;
            Item item = this.deferredPlaceItem;
            ExplosionDamageUtils.PlacementFace var4 = this.deferredPlaceFace;
            int var5 = this.deferredPlaceTick;
            this.clearDeferredPlace();
            if (mc.player != null && mc.level != null && mc.gameMode != null) {
                if (mc.player.tickCount == var5) {
                    if (item == Items.GLOWSTONE) {
                        this.chargeAnchor(blockPos);
                    } else if (var4 == null) {
                        this.placeCrystalAt(blockPos);
                    } else {
                        this.placeBlockAt(blockPos, item, var4);
                    }
                }
            }
        }
    }

    public boolean isMatrixBypassEnabled() {
        return this.matrixBypass.isEnabled();
    }

    @Subscribe
    private void onHudRender(HudRenderEvent event) {
    }

    private CrystalPlacementEvaluator.Settings buildSettings() {
        this.prunePendingSelfDamage();
        float var1 = this.preventSuicide.isEnabled() ? Math.max(0.0F, mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0F - this.totalPendingSelfDamage()) : Float.MAX_VALUE;
        boolean var2 = InventoryUtils.findItemIndex(Items.END_CRYSTAL, true) != -1;
        boolean var3 = this.placeObsidian.isEnabled() && InventoryUtils.findItemIndex(Items.OBSIDIAN, true) != -1;
        return new CrystalPlacementEvaluator.Settings(this.placeRange.getValue(), this.breakRange.getValue(), this.minimumTargetDamage.getValue(), this.maximumSelfDamage.getValue(), var1, var2, var3, this.placeThroughWalls.isEnabled(), this.allowsWallMining(), this.placeThroughWalls.isEnabled(), CombatTargetUtils.findFriendsInRange(this.targetRange.getValue() + 8.0F), this.matrixBypass.isEnabled(), this.activeBase(), this);
    }

    private void handleBrokenBlock() {
        if (this.breakManager.isOwnedBy(this) && this.breakManager.state() == BlockBreakManager.MiningState.BROKEN) {
            BlockPos blockPos = this.breakManager.targetPosition().immutable();
            this.breakManager.release(this);
            this.restoreDigTool();
            if (blockPos.equals(this.mineBoxPos)) {
                this.mineBoxPos = null;
            }
            BlockState blockState = this.confirmedBlockStates.remove(blockPos);
            if (blockState == null || ExplosionDamageUtils.isMineable(blockState)) {
                this.pendingBreaks.put(blockPos, mc.player.tickCount);
                if (blockState != null) {
                    this.confirmedBlockStates.put(blockPos, blockState);
                }
            }
        }
        if (!this.pendingBreaks.isEmpty()) {
            int tickCount2 = mc.player.tickCount;
            var iterator2 = this.pendingBreaks.entrySet().iterator();
            while (iterator2.hasNext()) {
                Entry entry = (Entry) iterator2.next();
                BlockPos key = (BlockPos) entry.getKey();
                BlockState blockState2 = this.confirmedBlockStates.remove(key);
                if (blockState2 == null) {
                    blockState2 = this.trackedBlockState(key, tickCount2);
                }
                int var6 = tickCount2 - (Integer) entry.getValue();
                if (blockState2 != null || var6 < 0 || var6 > 20) {
                    iterator2.remove();
                    this.blockFirstSeenTicks.remove(key);
                    // A block that came back (the server kept it) is set aside for a while. The stored state can be the
                    // stale one acknowledged when the dig started, so the block must also be in the world now.
                    if (blockState2 != null && ExplosionDamageUtils.isMineable(blockState2) && ExplosionDamageUtils.isMineable(mc.level.getBlockState(key))) {
                        this.blacklistBlock(key);
                    }
                }
            }
        }
    }

    private boolean canExplode(EndCrystal endCrystal, float var2) {
        if (endCrystal == null || endCrystal.isRemoved()) {
            return false;
        }
        if (risksFriend(endCrystal.position()) || recentlyHit(endCrystal))
            return false;
        if (mc.player.tickCount == this.lastAttackTick) {
            return false;
        }
        if (this.isTotemSwapRisk(endCrystal)) {
            return false;
        }
        Vector2f vector = this.crystalAimRotation(endCrystal, var2);
        if (vector == null || !BlockActionQueue.requestRotation(this, vector)) {
            return false;
        }
        if (!this.isLookingAtCrystal(endCrystal, var2)) {
            return false;
        }
        if (this.legitimateMode.isEnabled()) {
            this.swapToCrystal();
        }
        mc.gameMode.attack(mc.player, endCrystal);
        mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        this.lastAttackTick = mc.player.tickCount;
        HIT.put(endCrystal.getId(), mc.player.tickCount);
        this.recordSelfDamage(ExplosionDamageUtils.damageTo(endCrystal.position(), mc.player));
        if (this.matrixBypass.isEnabled()) {
            RotationManager.markAttack();
        }
        return true;
    }

    private boolean isBreakPending(BlockPos blockPos) {
        return this.pendingBreaks.containsKey(blockPos) || this.pendingBreaks.containsKey(blockPos.above());
    }

    private CrystalPlacementEvaluator.MiningContext buildMiningContext(float var1, boolean var2) {
        boolean var3 = this.breakManager.isOwnedBy(this);
        return new CrystalPlacementEvaluator.MiningContext(var2 && this.autoMine.isEnabled(), this.blacklist.keySet(), this.keepBlocks.keySet(), var3 ? this.breakManager.targetPosition() : null, var3 ? this.breakManager.breakProgress() : 0.0F, var1, this.activeBase(), this.pendingBreaks.keySet(), this.awaitTicks);
    }

    private boolean allowsWallMining() {
        return this.placeThroughWalls.isEnabled() && (!this.matrixBypass.isEnabled() || BlockPlacementManager.hasFastObsidianTool());
    }

    private boolean isTotemSwapRisk(EndCrystal endCrystal) {
        return AutoTotem.isSwapInProgress() && ExplosionDamageUtils.damageTo(endCrystal.position(), mc.player) > 0.0F;
    }

    private void reserveBase(BlockPos blockPos) {
        if (blockPos != null && hasBlockPlacementModules()) {
            BlockPlacementTracker.reserve(this, blockPos);
            BlockPlacementTracker.reserve(this, blockPos.above());
        }
    }

    private List<Player> findTargets() {
        EntityExtensionAccess player2 = (EntityExtensionAccess) mc.player;
        var var2 = CombatTargetUtils.findEnemiesInRange(this.targetRange.getValue());
        var2.sort((var1x, var2x) -> Double.compare(player2.getDistanceEyePos(var1x), player2.getDistanceEyePos(var2x)));
        return var2;
    }

    private void tickKnockbackSimulation() {
        SimulatedPlayerInput simulatedPlayerInput = this.simulatedInput;
        if (simulatedPlayerInput != null) {
            int tickCount2 = mc.player.tickCount;
            int var3 = tickCount2 - this.simulationTick;
            if (var3 < 0 || var3 > 10 || simulatedPlayerInput.player.isRemoved()) {
                this.simulatedInput = null;
            } else if (tickCount2 != this.lastSimulatedTick) {
                this.lastSimulatedTick = tickCount2;
                Player player2 = simulatedPlayerInput.player;
                double var5 = (player2.getX() - player2.xo) * this.attackDirectionX + (player2.getZ() - player2.zo) * this.attackDirectionZ;
                if (!(var5 >= this.attackDirectionLength * 0.5) && (var3 <= 1 || !simulatedPlayerInput.onGround)) {
                    simulatedPlayerInput.tick();
                } else {
                    this.simulatedInput = null;
                }
            }
        }
    }

    private void blacklistBlock(BlockPos blockPos) {
        if (blockPos != null && mc.player != null) {
            this.blacklist.put(blockPos.immutable(), mc.player.tickCount + 40);
        }
    }

    public boolean isPlacementThroughWallsEnabled() {
        return this.placeThroughWalls.isEnabled();
    }

    private void startMining(BlockPos blockPos) {
        this.selectDigTool(blockPos);
        BlockPlacementTracker.markPlacement();
        BlockPlacementTracker.reserve(this, blockPos);
        if (this.breakManager.requestBreak(this, blockPos)) {
            BlockBreakManager.MiningState var2 = this.breakManager.state();
            if (var2 == BlockBreakManager.MiningState.STUCK) {
                String.valueOf("копка " + blockPos.toShortString() + " STUCK: чёрный список на 40");
                this.blacklistBlock(blockPos);
                this.breakManager.release(this);
            } else {
                if (var2 == BlockBreakManager.MiningState.MINING && this.showPlacement.isEnabled()) {
                    this.mineBoxPos = blockPos;
                    this.mineBoxState = mc.level.getBlockState(blockPos);
                    this.mineBoxProgress = this.breakManager.breakProgress();
                    this.mineBoxTime = System.currentTimeMillis();
                }
            }
        }
    }

    private BlockState trackedBlockState(BlockPos blockPos, int var2) {
        BlockState blockState = mc.level.getBlockState(blockPos);
        if (!ExplosionDamageUtils.isMineable(blockState)) {
            this.blockFirstSeenTicks.remove(blockPos);
            return null;
        } else {
            int var4 = this.blockFirstSeenTicks.computeIfAbsent(blockPos, var1x -> var2);
            return var2 - var4 >= 2 ? blockState : null;
        }
    }

    private void placeCrystalAt(BlockPos blockPos) {
        if (risksFriend(Vec3.atBottomCenterOf(blockPos.above())))
            return;
        if (mc.player.tickCount != this.lastInteractTick) {
            if (InventoryUtils.findItemIndex(Items.END_CRYSTAL, true) != -1) {
                BlockHitResult blockHitResult = this.placementHit(blockPos, null, Vec3.atCenterOf(blockPos));
                if (blockHitResult != null) {
                    boolean enabled = this.legitimateMode.isEnabled();
                    int selectedSlot = mc.player.getInventory().getSelectedSlot();
                    if (enabled) {
                        this.swapToCrystal();
                    } else {
                        mc.player.getInventory().setSelectedSlot(InventoryUtils.findItemIndex(Items.END_CRYSTAL, true));
                    }
                    boolean var5;
                    try {
                        var5 = this.interactBlock(blockHitResult);
                    } finally {
                        if (!enabled) {
                            mc.player.getInventory().setSelectedSlot(selectedSlot);
                        }
                    }
                    this.onPlacementResult(blockPos, var5);
                    if (var5) {
                        this.expectedCrystalPos = blockPos.above();
                        if (blockPos.equals(this.plannedBase)) {
                            this.plannedBase = null;
                        }
                        this.expectedCrystalTick = mc.player.tickCount;
                    }
                } else {
                    this.onPlacementResult(blockPos, false);
                }
            }
        }
    }

    private void restoreSlot() {
        if (this.previousSlot != -1) {
            if (mc.player != null) {
                mc.player.getInventory().setSelectedSlot(this.previousSlot);
            }
            this.previousSlot = -1;
        }
    }

    private int minPredictTicks() {
        return Math.max(predictTicks(), 3);
    }

    private CrystalAura.PlacementAim findPlacementAim(BlockPos blockPos) {
        float value = this.placeRange.getValue();
        if (this.matrixBypass.isEnabled()) {
            BlockPlacementManager.PlacementTarget var5 = BlockPlacementManager.placementTarget(blockPos, value, this.placeThroughWalls.isEnabled());
            return var5 == null ? null : new CrystalAura.PlacementAim(var5.rotation(), new ExplosionDamageUtils.PlacementFace(var5.neighbor(), var5.side(), var5.aim()));
        } else {
            Vec3 eyePosition = mc.player.getEyePosition();
            ExplosionDamageUtils.PlacementFace var4 = ExplosionDamageUtils.findFace(blockPos, eyePosition, value, this.placeThroughWalls.isEnabled());
            return var4 == null ? null : new CrystalAura.PlacementAim(this.lookRotation(eyePosition, var4.aim()), var4);
        }
    }

    private Vector2f lookRotation(Vec3 vector, Vec3 vector2) {
        Vector2f vector3 = PlayerUtils.rotationTo(vector, vector2);
        return this.matrixBypass.isEnabled() ? BlockPlacementManager.quantizeRotation(vector3) : vector3;
    }

    private void recordBlockUpdate(BlockPos blockPos, BlockState blockState) {
        if (this.pendingBreaks.containsKey(blockPos) || blockPos.equals(this.breakManager.targetPosition())) {
            this.confirmedBlockStates.put(blockPos.immutable(), blockState);
        }
    }

    private boolean isBlacklisted(BlockPos blockPos) {
        Integer var2 = this.blacklist.get(blockPos);
        if (var2 == null) {
            return false;
        }
        if (mc.player != null && mc.player.tickCount <= var2) {
            return true;
        }
        this.blacklist.remove(blockPos);
        return false;
    }

    /**
     * The dig is the attack held on the block, so it uses whatever is in hand. The best tool from the hotbar goes in
     * hand for it, AutoTool or not: the dig time is estimated for that tool, and by hand the dig dragged on or stuck.
     */
    private void selectDigTool(BlockPos pos) {
        int best = ess.ll.client.module.player.AutoTool.bestHotbarSlot(mc.level.getBlockState(pos));
        var inventory = mc.player.getInventory();
        if (best == -1 || inventory.getSelectedSlot() == best)
            return;
        if (this.digRestoreSlot == -1)
            this.digRestoreSlot = inventory.getSelectedSlot();
        inventory.setSelectedSlot(best);
    }

    /**
     * Back to the slot held before the dig.
     */
    private void restoreDigTool() {
        if (this.digRestoreSlot == -1)
            return;
        if (mc.player != null)
            mc.player.getInventory().setSelectedSlot(this.digRestoreSlot);
        this.digRestoreSlot = -1;
    }

    private void stopMining() {
        this.restoreDigTool();
        this.mineCandidateTicks = 0;
        this.mineCandidate = null;
        if (this.breakManager.isOwnedBy(this)) {
            boolean var1 = this.breakManager.willBreakNextTick();
            this.breakManager.targetPosition();
            if (var1) {
                this.breakManager.keepAlive(this);
            } else {
                this.breakManager.release(this);
            }
        }
    }

    private CrystalPlacementEvaluator.Decision decideAttackCombo(Player player2, Vec3 vector, CrystalPlacementEvaluator.Settings var3) {
        if (isAuraTarget(player2) && !player2.isBlocking() && !this.isSimulatedTarget(player2)) {
            AttackAura attackAura = ModuleManager.INSTANCE.aura();
            if (((EntityExtensionAccess) mc.player).getDistanceEyePos(player2) > attackAura.effectiveAttackRange()) {
                return null;
            }
            if (WorldCollisionUtils.containsBlock(player2.getBoundingBox(), Blocks.COBWEB)) {
                return null;
            }
            Vec3 position2 = mc.player.position();
            SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.of(player2, SimulatedPlayerInput.SimulatedInput.guessInput(player2));
            simulatedPlayerInput.velocity = CrystalDamageUtils.predictKnockback(player2, simulatedPlayerInput.velocity, simulatedPlayerInput.onGround, position2, CrystalDamageUtils.yawToTarget(position2, player2), CrystalDamageUtils.attackKnockback(mc.player.isSprinting()));
            int var7 = this.minPredictTicks();
            for (int var8 = 0; var8 < var7; var8++) {
                simulatedPlayerInput.tick();
            }
            if (simulatedPlayerInput.boundingBox == null) {
                return null;
            }
            CrystalPlacementEvaluator.Decision var10 = CrystalPlacementEvaluator.evaluateWithMining(player2, simulatedPlayerInput.boundingBox, vector, var3, this.buildMiningContext(0.0F, false));
            if (var10 == null) {
                return null;
            }
            CrystalPlacementEvaluator.Action var9 = var10.action();
            return var9 != CrystalPlacementEvaluator.Action.PLACE_CRYSTAL && var9 != CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN ? null : var10;
        } else {
            return null;
        }
    }

    /**
     * Dev: the placement mark on a block, without a fight.
     */
    public void testMark(BlockPos base) {
        this.placementRenderer.placed(base);
    }

    @Subscribe
    private void onRender3D(Render3DEvent event) {
        if (mc.player != null && mc.level != null && mc.gameMode != null) {
            if (!this.showPlacement.isEnabled()) {
                this.mineBoxPos = null;
            } else {
                this.placementRenderer.render(event, PLACE_BOX_COLOR);
                if (this.anchorPreview != null) {
                    CrystalPlacementRenderer.fillBox(event, new AABB(this.anchorPreview).inflate(0.003), ColorUtils.withAlpha(PLACE_BOX_COLOR, 75));
                }
                this.renderMineBox(event, System.currentTimeMillis());
            }
        }
    }

    private boolean isLookingAtCrystal(EndCrystal endCrystal, float var2) {
        return this.matrixBypass.isEnabled() ? BlockPlacementManager.isLookingAtEntity(endCrystal, var2) : RaycastUtils.isLookingAt(mc.player.getYRot(), mc.player.getXRot(), var2, endCrystal, !this.placeThroughWalls.isEnabled());
    }

    private Vector2f crystalPlaceRotation(BlockPos blockPos) {
        Vec3 eyePosition = mc.player.getEyePosition();
        if (this.placeThroughWalls.isEnabled()) {
            return this.lookRotation(eyePosition, ExplosionDamageUtils.clampedAimPoint(blockPos, eyePosition));
        }
        Vec3 vector = ExplosionDamageUtils.aimAtBlock(blockPos, eyePosition, this.placeRange.getValue());
        return vector == null ? null : this.lookRotation(eyePosition, vector);
    }

    private void resetAttackChain() {
        this.attackChainTicks = 0;
        this.mineCandidateTicks = 0;
        this.restoreSlot();
        this.breakManager.release(this);
    }

    private void prunePendingSelfDamage() {
        if (!this.pendingSelfDamage.isEmpty()) {
            int tickCount2 = mc.player.tickCount;
            int latencyTicks = ServerLatencyTracker.INSTANCE.getLatencyTicks();
            this.pendingSelfDamage.removeIf(var3 -> this.lastSelfDamageTick > var3.tick() + latencyTicks || tickCount2 - var3.tick() > latencyTicks + 5);
        }
    }

    private void pruneBlockTracking() {
        if (mc.player != null) {
            int tickCount2 = mc.player.tickCount;
            this.blacklist.values().removeIf(var1x -> tickCount2 > var1x);
            this.keepBlocks.values().removeIf(var1x -> tickCount2 > var1x);
            int keep = ServerLatencyTracker.INSTANCE.getLatencyTicks() + 3;
            HIT.values().removeIf(var1x -> tickCount2 - var1x > keep || var1x > tickCount2);
        }
    }

    private void onPlacementResult(BlockPos blockPos, boolean var2) {
        if (!var2 && blockPos.equals(this.lastFailedBlock)) {
            if (++this.failedBlockCount >= 5) {
                this.blacklist.put(blockPos.immutable(), mc.player.tickCount + 10);
                if (blockPos.equals(this.plannedBase)) {
                    this.plannedBase = null;
                }
                this.lastFailedBlock = null;
                this.failedBlockCount = 0;
            }
        } else {
            this.lastFailedBlock = var2 ? null : blockPos;
            this.failedBlockCount = var2 ? 0 : 1;
        }
    }

    @Subscribe
    private void onHitResult(HitResultEvent event) {
        this.hitConstraint.apply(event);
    }

    private void tickAttackChain(Player player) {
        this.attackChainTicks++;
        if (this.attackChainTicks <= 6 && !this.isSimulatedTarget(player)) {
            if (this.continueMining()) {
                return;
            }
            if (this.breakManager.isOwnedBy(this) && this.breakManager.targetPosition() != null && ExplosionDamageUtils.isMineable(mc.level.getBlockState(this.breakManager.targetPosition()))) {
                this.breakManager.keepAlive(this);
                return;
            }
        }
        this.resetAttackChain();
    }

    private BlockPos activeBase() {
        return mc.player.tickCount - this.planTick > 40 ? null : this.plannedBase;
    }

    private float totalPendingSelfDamage() {
        float var1 = 0.0F;
        for (CrystalAura.PendingSelfDamage var3 : this.pendingSelfDamage) {
            var1 += var3.damage();
        }
        return var1;
    }

    private void deferPlace(BlockPos blockPos, Item item, ExplosionDamageUtils.PlacementFace var3) {
        this.deferredPlacePos = blockPos;
        this.deferredPlaceItem = item;
        this.deferredPlaceFace = var3;
        this.deferredPlaceTick = mc.player.tickCount;
    }

    private AABB predictedTargetBounds(Player player2) {
        int var2 = predictTicks();
        if (WorldCollisionUtils.containsBlock(player2.getBoundingBox(), Blocks.COBWEB)) {
            return player2.getBoundingBox();
        }
        SimulatedPlayerInput simulatedPlayerInput = this.simulatedInput;
        if (simulatedPlayerInput != null && simulatedPlayerInput.player == player2) {
            SimulatedPlayerInput simulatedPlayerInput2 = simulatedPlayerInput.copy();
            int var5 = this.minPredictTicks();
            for (int var6 = 0; var6 < var5; var6++) {
                simulatedPlayerInput2.tick();
            }
            return simulatedPlayerInput2.boundingBox != null ? simulatedPlayerInput2.boundingBox : player2.getBoundingBox();
        } else {
            if (var2 <= 0) {
                return player2.getBoundingBox();
            }
            SimulatedPlayerInput simulatedPlayerInput3 = SimulatedPlayerInput.predict(player2, var2);
            return simulatedPlayerInput3.boundingBox != null ? simulatedPlayerInput3.boundingBox : player2.getBoundingBox();
        }
    }

    private boolean isMineCandidateStable(BlockPos blockPos) {
        if (this.breakManager.isOwnedBy(this)) {
            return true;
        }
        if (!blockPos.equals(this.mineCandidate)) {
            this.mineCandidate = blockPos;
            this.mineCandidateTicks = 1;
        } else {
            this.mineCandidateTicks++;
        }
        return this.mineCandidateTicks >= 2;
    }

    private boolean rotateAndPlace(BlockPos blockPos, Item item) {
        if (InventoryUtils.findItemIndex(item, true) == -1) {
            return false;
        } else {
            CrystalAura.PlacementAim var3 = this.findPlacementAim(blockPos);
            if (var3 != null && BlockActionQueue.requestRotation(this, var3.rotation())) {
                this.deferPlace(blockPos, item, var3.face());
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ModuleManager.INSTANCE.crystalAuraV2().setEnabled(false);
    }

    @Override
    public void onDisable() {
        this.pearlApproach.reset();
        this.anchorPreview = null;
        this.anchorAwaitingAck = null;
        this.anchorSequence = -1;
        this.chargingAnchor = false;
        this.pendingBreakSequences.clear();
        this.crystalBase = null;
        this.restoreDigTool();
        HIT.clear();
        ExplosionDamageUtils.setIgnorePlayerProtection(false);
        this.restoreSlot();
        this.breakManager.release(this);
        this.hitConstraint.clear();
        BlockActionQueue.release(this);
        this.placementRenderer.clear();
        this.mineBoxPos = null;
        this.mineBoxState = null;
        this.expectedCrystalPos = null;
        this.clearDeferredPlace();
        this.movementTracker.reset();
        this.simulatedInput = null;
        this.pendingKnockback = null;
        this.attackChainTicks = 0;
        this.mineCandidateTicks = 0;
        this.blacklist.clear();
        this.keepBlocks.clear();
        this.blockFirstSeenTicks.clear();
        this.awaitTicks = 0;
        this.pendingBreaks.clear();
        this.confirmedBlockStates.clear();
        this.pendingSelfDamage.clear();
        this.plannedBase = null;
        this.lastFailedBlock = null;
        this.failedBlockCount = 0;
        super.onDisable();
    }

    private void swapToCrystal() {
        int var1 = InventoryUtils.findItemIndex(Items.END_CRYSTAL, true);
        if (var1 != -1) {
            this.breakManager.release(this);
            if (this.previousSlot == -1) {
                this.previousSlot = mc.player.getInventory().getSelectedSlot();
            }
            if (mc.player.getInventory().getSelectedSlot() != var1) {
                mc.player.getInventory().setSelectedSlot(var1);
            }
        }
    }

    private record PendingSelfDamage(int tick, float damage) {
    }

    private record PlacementAim(Vector2f rotation, ExplosionDamageUtils.PlacementFace face) {
    }
}
