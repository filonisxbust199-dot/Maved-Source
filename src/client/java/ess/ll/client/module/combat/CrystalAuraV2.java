package ess.ll.client.module.combat;

import com.mojang.datafixers.util.Pair;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PostPlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.CombatTargetUtils;
import ess.ll.client.features.combat.CrystalPearlApproach;
import ess.ll.client.features.crystalv2.Action;
import ess.ll.client.features.crystalv2.CalculateFovUtil;
import ess.ll.client.features.crystalv2.DidRayTraceHitUtil;
import ess.ll.client.features.crystalv2.FaceDefensive;
import ess.ll.client.features.crystalv2.GetExplosionDamageUtil;
import ess.ll.client.features.crystalv2.GetNearestPointUtil;
import ess.ll.client.features.crystalv2.GetSelfExplosionDamageUtil;
import ess.ll.client.features.crystalv2.IsInWebUtil;
import ess.ll.client.features.crystalv2.PlaceData;
import ess.ll.client.features.crystalv2.Rotation;
import ess.ll.client.features.crystalv2.Simulated;
import ess.ll.client.features.crystalv2.ToStringImpl7;
import ess.ll.client.features.crystalv2.V2Packets;
import ess.ll.client.features.crystalv2.V2Visuals;
import ess.ll.client.features.crystalv2.ValuesEnum9;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.module.player.AutoTool;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.RotationManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The maved-26.2 CrystalAura with its target prediction, placement search and fading boxes.
 */
public class CrystalAuraV2 extends ClientModule {
    private static final int ROTATION_PRIORITY = 99999;
    private static final double TARGET_RANGE = 8.0;
    private final V2Packets pc = V2Packets.INSTANCE;
    private final ToggleSetting yakorAura = new ToggleSetting("Якори").withDefault(false).withContainer(this);
    private final ToggleSetting podkop = new ToggleSetting("Копать").withDefault(false).withContainer(this);
    private final NumberSetting<Float> minimumDamage = new NumberSetting<Float>("Минимальный урон").withMin(1.0F).withMax(36.0F).withStep(0.5F).withDefault(15.0F).withContainer(this);
    private final NumberSetting<Float> maximumSelfDamage = new NumberSetting<Float>("Максимальный самоурон").withMin(0.0F).withMax(36.0F).withStep(0.5F).withDefault(12.0F).withContainer(this);
    private final ToggleSetting trade = new ToggleSetting("Размен тотемами").withDefault(false).withContainer(this);
    private final NumberSetting<Float> tradeMaxSelf = new NumberSetting<Float>("Самоурон в размене").withMin(0.0F).withMax(100.0F).withStep(1.0F).withDefault(36.0F).withVisibility(this.trade::isEnabled).withContainer(this);
    private final NumberSetting<Float> tradeMinTotems = new NumberSetting<Float>("Мин. своих тотемов").withMin(1.0F).withMax(20.0F).withStep(1.0F).withDefault(2.0F).withVisibility(this.trade::isEnabled).withContainer(this);
    private final ToggleSetting tradeOnlyTotem = new ToggleSetting("Только если тотем у цели").withDefault(true).withVisibility(this.trade::isEnabled).withContainer(this);
    private final CrystalPearlApproach pearl = new CrystalPearlApproach(this);
    private final V2Visuals visuals = new V2Visuals();
    private Player target;
    private PlaceData placeData;
    private PlaceData prevData;
    private Rotation rotation;
    private boolean tradeActive;
    private double tradeHealth;
    private boolean digging;
    private int digRestoreSlot = -1;

    public CrystalAuraV2() {
        super(ModuleCategory.Combat, ModuleSubCategory.Utility);
        this.setName("CrystalAuraV2");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ModuleManager.INSTANCE.crystalAura().setEnabled(false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.stopDig();
        this.placeData = null;
        this.prevData = null;
        this.rotation = null;
        this.target = null;
        this.pearl.reset();
        this.pc.simulated = null;
        this.pc.simulated2 = null;
        this.pc.simulated3 = null;
    }

    @Subscribe
    private void onInput(PlayerInputEvent event) {
        if (mc.player != null && mc.level != null && mc.gameMode != null) {
            boolean dug = this.digging;
            this.digging = false;
            this.placeData = null;
            this.rotation = null;
            boolean webs = ModuleManager.INSTANCE.webTrap().crystalsWait();
            if (webs) {
                // The aura's look would outrank the one the webs need.
                RotationManager.releasePriority(ROTATION_PRIORITY);
            } else if (ModuleManager.INSTANCE.antiCrystal().getLastData() == null) {
                this.tick();
            }
            if (dug && !this.digging) {
                this.stopDig();
            }
        }
    }

    @Subscribe
    private void onPostInput(PostPlayerInputEvent event) {
        this.pearl.tick(this.target);
        if (this.rotation != null && (this.pc.viaVersionProtocol() >= 768 || this.placeData != null && this.placeData.action().equals(Action.ATTACK_CRYSTAL))) {
            this.rotate(this.rotation);
        }
    }

    private void tick() {
        this.tradeActive = false;
        int obsidianSlot = hotbar(Items.OBSIDIAN);
        int crystalSlot = hotbar(Items.END_CRYSTAL);
        int anchorSlot = hotbar(Items.RESPAWN_ANCHOR);
        int glowStoneSlot = hotbar(Items.GLOWSTONE);
        if ((obsidianSlot == -1 || crystalSlot == -1) && this.yakorAura.isEnabled() && (anchorSlot == -1 || glowStoneSlot == -1)) {
            return;
        }
        this.target = this.findTarget();
        Player target = this.target;
        if (target == null) {
            return;
        }
        this.updateTrade(target);
        this.pc.simulated = Simulated.simulateOtherPlayer(target, 2);
        this.pc.simulated2 = Simulated.simulateOtherPlayer(target, 1);
        this.pc.simulated3 = Simulated.simulateOtherPlayer(target, 1);
        PlaceData finalData = null;
        if (obsidianSlot != -1 && crystalSlot != -1) {
            if (this.podkop.isEnabled()) {
                finalData = this.findBreak(target);
                PlaceData obsidianData = this.findPlaceObsidian(target);
                if (obsidianData.damage() >= this.minDamage()) {
                    finalData = obsidianData;
                }
                PlaceData crystalData = this.findPlaceCrystal(target);
                if (crystalData.damage() >= this.minDamage()) {
                    finalData = crystalData;
                }
                PlaceData crystalAttackData = this.findCrystalAttack(target);
                if (crystalAttackData.damage() - 5.0 > this.minDamage()) {
                    finalData = crystalAttackData;
                }
            } else {
                finalData = this.findPlaceObsidian(target);
                PlaceData crystalData = this.findPlaceCrystal(target);
                if (crystalData.damage() >= finalData.damage() - 2.0) {
                    finalData = crystalData;
                }
                PlaceData crystalAttackData = this.findCrystalAttack(target);
                if (crystalAttackData.damage() >= finalData.damage() || crystalAttackData.damage() - 5.0 > this.minDamage()) {
                    finalData = crystalAttackData;
                }
            }
        }
        if (anchorSlot != -1 && glowStoneSlot != -1 && this.yakorAura.isEnabled()) {
            PlaceData yakorPlaceData = this.findPlaceYakor(target);
            if (finalData == null) {
                finalData = yakorPlaceData;
            }
            if (yakorPlaceData.damage() >= finalData.damage() || yakorPlaceData.damage() > this.minDamage() + 2.0F) {
                finalData = yakorPlaceData;
            }
            PlaceData yakorData = this.findYakor(target);
            if (yakorData.damage() >= finalData.damage() || yakorData.damage() > this.minDamage() + 2.0F) {
                finalData = yakorData;
            }
        }
        if (finalData == null) {
            return;
        }
        this.placeData = finalData;
        this.prevData = finalData;
        if (finalData.action().equals(Action.AWAITING)) {
            return;
        }
        if (finalData.action().equals(Action.ATTACK_CRYSTAL)) {
            if (finalData.rotation() != null && !this.pc.clickedButNotClosedWithPacket && target.hurtTime <= 7 && this.pc.flagTicks > 3) {
                Vec3 look = new Rotation(RotationManager.getServerYaw(), RotationManager.getServerPitch()).toVector();
                if (DidRayTraceHitUtil.rayTrace(look, 3.0, finalData.entity().getBoundingBox())) {
                    mc.gameMode.attack(mc.player, finalData.entity());
                    mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
                }
                this.rotation = finalData.rotation();
                this.rotate(finalData.rotation());
            }
            return;
        }
        if (finalData.pos() == null || finalData.face() == null || finalData.rotation() == null || finalData.damage() == 0.0) {
            return;
        }
        if (this.placedAtThisLook()) {
            finalData.rotation().setYaw(finalData.rotation().getYaw() + 0.005F);
        }
        this.rotate(finalData.rotation());
        this.rotation = finalData.rotation();
        if (finalData.action().equals(Action.BREAK)) {
            this.visuals.add(finalData.pos(), 1.0F, 0.0F, 0.0F);
            this.dig(finalData.pos(), finalData.face());
            return;
        }
        int prev = mc.player.getInventory().getSelectedSlot();
        int slot = obsidianSlot;
        if (finalData.action() == Action.CRYSTAL) {
            slot = crystalSlot;
        }
        if (finalData.action() == Action.CHARGE) {
            slot = glowStoneSlot;
        }
        if (finalData.action() == Action.ANCHOR) {
            slot = anchorSlot;
        }
        if (finalData.action() == Action.ANCHOR_BOOM) {
            slot = 0;
        }
        if (slot == -1) {
            return;
        }
        if (finalData.action() == Action.OBSIDIAN || finalData.action() == Action.ANCHOR) {
            this.visuals.add(finalData.pos().relative(finalData.face()), 0.2F, 0.6F, 1.0F);
        }
        if (finalData.action() == Action.CRYSTAL) {
            this.visuals.add(finalData.pos().above(), 0.68F, 0.58F, 1.0F);
        }
        if (finalData.action() == Action.ANCHOR_BOOM) {
            this.visuals.add(finalData.pos(), 1.0F, 0.95F, 0.0F);
        }
        float look = Math.abs(this.pc.LastYaw - this.pc.PreviousYaw);
        if (Math.abs(look - this.pc.yawPlace) >= 1.0E-4F || this.pc.yawPlace == 0.0 || look == 0.0F || look <= 2.0F) {
            boolean legacy = this.pc.viaVersionProtocol() > 0 && this.pc.viaVersionProtocol() < 768;
            if (legacy) {
                float yaw = mc.player.getYRot() + -Mth.wrapDegrees(mc.player.getYRot() - finalData.rotation().getYaw());
                mc.getConnection().send(new PosRot(this.pc.LastPosX, this.pc.LastPosY, this.pc.LastPosZ, yaw + (Math.abs(yaw - this.pc.yawPlace) < 1.0E-4F ? 5.0E-4F : 0.0F), finalData.rotation().getPitch(), this.pc.LastGround, false));
            }
            mc.player.getInventory().setSelectedSlot(slot);
            BlockHitResult hitResult = new BlockHitResult(DidRayTraceHitUtil.lookToBlock(finalData.rotation(), finalData.pos(), 8.0), finalData.face(), finalData.pos(), false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
            mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            if (slot != prev) {
                mc.player.getInventory().setSelectedSlot(prev);
            }
            if (legacy) {
                mc.getConnection().send(new PosRot(this.pc.LastPosX, this.pc.LastPosY, this.pc.LastPosZ, mc.player.getYRot(), mc.player.getXRot(), this.pc.LastGround, false));
            }
        }
    }

    private boolean placedAtThisLook() {
        float look = Math.abs(this.pc.LastYaw - this.pc.PreviousYaw);
        return Math.abs(look - this.pc.yawPlace) < 1.0E-4F && this.pc.yawPlace != 0.0 && look != 0.0F;
    }

    private Player findTarget() {
        var antiBot = ModuleManager.INSTANCE.antiBot();
        if (this.target != null && this.target.level() == mc.level && CombatTargetUtils.isEnemy(this.target, antiBot) && mc.player.distanceTo(this.target) <= TARGET_RANGE) {
            return this.target;
        }
        Player best = null;
        double bestDistance = TARGET_RANGE;
        for (Player player : mc.level.players()) {
            double distance = mc.player.distanceTo(player);
            if (distance <= bestDistance && CombatTargetUtils.isEnemy(player, antiBot)) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int hotbar(Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private void rotate(Rotation rotation) {
        RotationManager.rotate(new ess.ll.client.util.Rotation(rotation.getYaw(), rotation.getPitch()), 360.0F, ROTATION_PRIORITY, false);
        RotationManager.moveCorrectionRequested = true;
    }

    /**
     * The dig holds the best hotbar tool until it ends, then the old slot comes back.
     */
    private void dig(BlockPos pos, Direction face) {
        int best = AutoTool.bestHotbarSlot(mc.level.getBlockState(pos));
        var inventory = mc.player.getInventory();
        if (best != -1 && inventory.getSelectedSlot() != best) {
            if (this.digRestoreSlot == -1) {
                this.digRestoreSlot = inventory.getSelectedSlot();
            }
            inventory.setSelectedSlot(best);
        }
        this.digging = true;
        mc.gameMode.continueDestroyBlock(pos, face);
        mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
    }

    private void stopDig() {
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        if (this.digRestoreSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(this.digRestoreSlot);
        }
        this.digRestoreSlot = -1;
        this.digging = false;
    }

    private Simulated getSimulated() {
        return this.pc.simulated;
    }

    private Simulated getSimulated2() {
        return this.pc.simulated2;
    }

    private Simulated getSimulated3() {
        return this.pc.simulated3;
    }

    public void updateTrade(Player target) {
        boolean holding = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || mc.player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
        boolean targetHolding = target.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || target.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
        int totems = mc.player.getInventory().countItem(Items.TOTEM_OF_UNDYING);
        this.tradeHealth = target.getHealth() + target.getAbsorptionAmount();
        this.tradeActive = this.trade.isEnabled() && holding && totems >= this.tradeMinTotems.getValue().intValue() && (targetHolding || !this.tradeOnlyTotem.isEnabled());
    }

    public float minDamage() {
        float minimum = this.minimumDamage.getValue();
        return this.tradeActive ? Math.min(minimum, (float) this.tradeHealth) : minimum;
    }

    public boolean selfAllowed(double selfDamage, double damage) {
        return selfDamage <= this.maximumSelfDamage.getValue() || this.tradeActive && damage >= this.tradeHealth && selfDamage <= this.tradeMaxSelf.getValue();
    }

    public PlaceData findPlaceObsidian(Player target) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        Rotation bestRotation = null;
        double bestDamage = 0.0;
        double bestAwaitingDamage = 0.0;
        boolean isBreak = false;
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -5; z < 5; z++) {
                    BlockPos checkPos = new BlockPos(mc.player.blockPosition().getX() + x, mc.player.blockPosition().getY() + y, mc.player.blockPosition().getZ() + z);
                    if (mc.level.getBlockState(checkPos).isSolid()) {
                        ToStringImpl7 result = IsInWebUtil.bestFaceCAura(checkPos, target, this.minDamage(), this.maximumSelfDamage.getValue().floatValue(), true);
                        if (result.awaitingDamage() > bestAwaitingDamage) {
                            bestAwaitingDamage = result.awaitingDamage();
                        }
                        if (result.direction() != null && result.rotation() != null && !(result.damage() < bestDamage)) {
                            if (result.needToBreak()) {
                                bestPos = target.blockPosition();
                                isBreak = true;
                            } else {
                                if (result.defensivePos() != null) {
                                    bestPos = result.defensivePos().immutable();
                                } else {
                                    bestPos = checkPos.immutable();
                                }
                                isBreak = false;
                            }
                            bestFace = result.direction();
                            bestRotation = result.rotation();
                            bestDamage = result.damage();
                        }
                    }
                }
            }
        }
        return new PlaceData(bestPos, bestFace, Math.max(bestDamage, bestAwaitingDamage), bestRotation, isBreak ? Action.BREAK : Action.OBSIDIAN, null, null);
    }

    public PlaceData findPlaceCrystal(Player target) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        Rotation bestRotation = null;
        double bestDamage = 0.0;
        BlockPos defensivePos = null;
        Direction defensiveFace = null;
        Rotation defensiveRotation = null;
        double defensiveDamage = 0.0;
        boolean isBreak = false;
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -5; z < 5; z++) {
                    BlockPos checkPos = new BlockPos(mc.player.blockPosition().getX() + x, mc.player.blockPosition().getY() + y, mc.player.blockPosition().getZ() + z);
                    BlockState state = mc.level.getBlockState(checkPos);
                    if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                        BlockPos placePos = checkPos.above();
                        BlockPos pidorPos = target.blockPosition();
                        BlockState placeState = mc.level.getBlockState(placePos);
                        if (placeState.isAir() && placeState.getBlock() != Blocks.WATER && placeState.getBlock() != Blocks.LAVA) {
                            Pair<Rotation, Direction> face = IsInWebUtil.getBestVisibleFaceCrystal(checkPos, mc.player, true);
                            if (face != null && face.getFirst() != null && face.getSecond() != null && IsInWebUtil.blockedPlace(checkPos).equals(ValuesEnum9.NONE) && IsInWebUtil.blockedPlace(checkPos, this.getSimulated().boundingBox).equals(ValuesEnum9.NONE) && IsInWebUtil.blockedPlace(checkPos, this.getSimulated3().boundingBox).equals(ValuesEnum9.NONE) && IsInWebUtil.blockedPlace(checkPos, this.getSimulated2().boundingBox).equals(ValuesEnum9.NONE) && DidRayTraceHitUtil.canReachBox(new AABB(placePos.getX() - 0.5, placePos.getY(), placePos.getZ() - 0.5, placePos.getX() + 1.5, placePos.getY() + 1.5, placePos.getZ() + 1.5), 3.0)) {
                                float damage = GetSelfExplosionDamageUtil.getDamageIgnoringBlockAt(Vec3.atBottomCenterOf(placePos), target, this.getSimulated().pos, pidorPos, true);
                                if (!(damage < bestDamage) && !(damage < this.minDamage())) {
                                    float selfDamage = GetSelfExplosionDamageUtil.getDamageIgnoringBlock(Vec3.atBottomCenterOf(placePos), mc.player, pidorPos, true);
                                    if (!this.selfAllowed(selfDamage, damage)) {
                                        for (int x2 = -2; x2 < 2; x2++) {
                                            for (int y2 = -2; y2 < 2; y2++) {
                                                for (int z2 = -2; z2 < 2; z2++) {
                                                    BlockPos checkPos2 = new BlockPos(mc.player.blockPosition().getX() + x2, mc.player.blockPosition().getY() + y2, mc.player.blockPosition().getZ() + z2);
                                                    if (mc.level.getBlockState(checkPos2).isSolid()) {
                                                        Pair<Rotation, Direction> face0 = IsInWebUtil.getBestVisibleFaceCrystal(checkPos2, mc.player, true);
                                                        if (face0 != null && face0.getFirst() != null && face0.getSecond() != null) {
                                                            BlockPos place = checkPos2.relative((Direction) face0.getSecond());
                                                            if (mc.level.getBlockState(place).isAir() && !IsInWebUtil.blocked(place).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(place, this.getSimulated().boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(place, this.getSimulated3().boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(place, this.getSimulated2().boundingBox).equals(ValuesEnum9.PLAYER)) {
                                                                float selfDamageNew = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostsAt(Vec3.atBottomCenterOf(placePos), mc.player, mc.player.position(), List.of(), List.of(place), true);
                                                                if (!(selfDamageNew > this.maximumSelfDamage.getValue())) {
                                                                    float damageNew = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostsAt(Vec3.atBottomCenterOf(placePos), target, this.getSimulated().pos, List.of(), List.of(place), true);
                                                                    if (!(damageNew < bestDamage) && !(damageNew < defensiveDamage) && !(damageNew < this.minDamage())) {
                                                                        defensivePos = checkPos2;
                                                                        defensiveFace = (Direction) face0.getSecond();
                                                                        defensiveRotation = (Rotation) face0.getFirst();
                                                                        defensiveDamage = damageNew;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        isBreak = mc.level.getBlockState(pidorPos).isSolid() && GetSelfExplosionDamageUtil.getExplosionDamage(Vec3.atBottomCenterOf(placePos), target, true) <= 1.0F;
                                        if (isBreak) {
                                            Pair<Rotation, Direction> faceInside = IsInWebUtil.getBestVisibleFace(pidorPos, mc.player, true);
                                            if (faceInside != null && faceInside.getFirst() != null && faceInside.getSecond() != null) {
                                                bestPos = pidorPos.immutable();
                                                bestFace = (Direction) faceInside.getSecond();
                                                bestRotation = (Rotation) faceInside.getFirst();
                                                bestDamage = damage;
                                            }
                                        } else {
                                            bestPos = checkPos.immutable();
                                            bestFace = (Direction) face.getSecond();
                                            bestRotation = (Rotation) face.getFirst();
                                            bestDamage = damage;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return defensiveDamage > bestDamage ? new PlaceData(defensivePos, defensiveFace, defensiveDamage, defensiveRotation, Action.OBSIDIAN, null, null) : new PlaceData(bestPos, bestFace, bestDamage, bestRotation, isBreak ? Action.BREAK : Action.CRYSTAL, null, null);
    }

    public PlaceData findBreak(Player target) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        Rotation bestRotation = null;
        double bestDamage = 0.0;
        BlockPos bestPosIncludeAir = null;
        Direction bestFaceIncludeAir = null;
        Rotation bestRotationIncludeAir = null;
        double bestDamageIncludeAir = 0.0;
        BlockPos prevBestPos = null;
        Direction prevBestFace = null;
        Rotation prevBestRotation = null;
        double prevDamage = 0.0;
        BlockPos defensivePos = null;
        Direction defensiveFace = null;
        Rotation defensiveRotation = null;
        double defensiveDamage = 0.0;
        BlockPos check = null;
        Simulated sim = this.getSimulated();
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -5; z < 5; z++) {
                    BlockPos checkPos = new BlockPos(mc.player.blockPosition().getX() + x, mc.player.blockPosition().getY() + y, mc.player.blockPosition().getZ() + z);
                    BlockPos downPos = checkPos.below();
                    BlockPos placePos = checkPos.above();
                    BlockPos pidorPos = target.blockPosition();
                    if (!mc.level.getBlockState(placePos).liquid()) {
                        BlockState checkState = mc.level.getBlockState(checkPos);
                        BlockState downState = mc.level.getBlockState(downPos);
                        if (!checkState.is(Blocks.BEDROCK) && !checkState.is(Blocks.BARRIER) && !downState.is(Blocks.BARRIER) && (checkState.isSolid() || downState.isSolid())) {
                            Pair<Rotation, Direction> face = IsInWebUtil.getBestVisibleFace(checkPos, mc.player, true);
                            if (face != null && face.getFirst() != null && face.getSecond() != null) {
                                double damage = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostAt(Vec3.atBottomCenterOf(checkPos), target, sim.pos, List.of(checkPos, pidorPos), downPos, true);
                                if (this.prevData != null && this.prevData.checkPos() != null && this.prevData.action().equals(Action.BREAK) && this.prevData.checkPos().equals(checkPos) && damage > this.minDamage()) {
                                    damage += 999.0;
                                }
                                if (!(damage < bestDamage) && !(damage < this.minDamage()) && DidRayTraceHitUtil.canReachBox(new AABB(checkPos.getX() - 0.5, checkPos.getY(), checkPos.getZ() - 0.5, checkPos.getX() + 1.5, checkPos.getY() + 1.5, checkPos.getZ() + 1.5), 3.0) && !IsInWebUtil.blockedPlace(checkPos).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blockedPlace(checkPos, sim.boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blockedPlace(checkPos, this.getSimulated2().boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blockedPlace(checkPos, this.getSimulated3().boundingBox).equals(ValuesEnum9.PLAYER)) {
                                    float selfDamage = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostAt(Vec3.atBottomCenterOf(checkPos), mc.player, mc.player.position(), List.of(checkPos, pidorPos), downPos, true);
                                    if (this.selfAllowed(selfDamage, damage)) {
                                        if (mc.level.getBlockState(pidorPos).isSolid() && GetSelfExplosionDamageUtil.getDamageIgnoringBlockAndGhostAt(Vec3.atBottomCenterOf(checkPos), target, sim.pos, checkPos, downPos, true) <= 3.0F) {
                                            Pair<Rotation, Direction> facePidor = IsInWebUtil.getBestVisibleFace(pidorPos, mc.player, true);
                                            if (facePidor != null && facePidor.getFirst() != null && facePidor.getSecond() != null) {
                                                bestPos = pidorPos.immutable();
                                                bestFace = (Direction) facePidor.getSecond();
                                                bestRotation = (Rotation) facePidor.getFirst();
                                                bestDamage = damage;
                                            }
                                        } else {
                                            Pair<Rotation, Direction> faceDown = null;
                                            if (mc.level.getBlockState(downPos).isSolid() && mc.level.getBlockState(downPos).getBlock() != Blocks.BEDROCK && mc.level.getBlockState(downPos).getBlock() != Blocks.OBSIDIAN) {
                                                faceDown = IsInWebUtil.getBestVisibleFace(downPos, mc.player, true);
                                                if (faceDown == null || faceDown.getFirst() == null || faceDown.getSecond() == null) {
                                                    continue;
                                                }
                                            }
                                            if (!mc.level.getBlockState(downPos).isAir() || !IsInWebUtil.blocked(downPos).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(downPos, sim.boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(downPos, this.getSimulated2().boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(downPos, this.getSimulated3().boundingBox).equals(ValuesEnum9.PLAYER)) {
                                                check = checkPos.immutable();
                                                if (bestPosIncludeAir != null && bestFaceIncludeAir != null && bestRotationIncludeAir != null && bestDamageIncludeAir != 0.0) {
                                                    prevBestPos = bestPosIncludeAir;
                                                    prevBestFace = bestFaceIncludeAir;
                                                    prevBestRotation = bestRotationIncludeAir;
                                                    prevDamage = bestDamageIncludeAir;
                                                }
                                                bestPosIncludeAir = checkPos.immutable();
                                                bestFaceIncludeAir = (Direction) face.getSecond();
                                                bestRotationIncludeAir = (Rotation) face.getFirst();
                                                bestDamageIncludeAir = damage;
                                                if (mc.level.getBlockState(checkPos).isSolid()) {
                                                    bestPos = checkPos.immutable();
                                                    bestFace = (Direction) face.getSecond();
                                                    bestRotation = (Rotation) face.getFirst();
                                                    bestDamage = damage;
                                                } else if (faceDown != null && faceDown.getFirst() != null && faceDown.getSecond() != null && !mc.level.getBlockState(checkPos).liquid()) {
                                                    bestPos = downPos.immutable();
                                                    bestFace = (Direction) faceDown.getSecond();
                                                    bestRotation = (Rotation) faceDown.getFirst();
                                                    bestDamage = damage;
                                                }
                                            }
                                        }
                                    } else {
                                        for (int x2 = -2; x2 < 2; x2++) {
                                            for (int y2 = -2; y2 < 2; y2++) {
                                                for (int z2 = -2; z2 < 2; z2++) {
                                                    BlockPos checkPos2 = new BlockPos(mc.player.blockPosition().getX() + x2, mc.player.blockPosition().getY() + y2, mc.player.blockPosition().getZ() + z2);
                                                    if (mc.level.getBlockState(checkPos2).isSolid()) {
                                                        Pair<Rotation, Direction> face0 = IsInWebUtil.getBestVisibleFaceCrystal(checkPos2, mc.player, true);
                                                        if (face0 != null && face0.getFirst() != null && face0.getSecond() != null) {
                                                            BlockPos place = checkPos2.relative((Direction) face0.getSecond());
                                                            if (mc.level.getBlockState(place).isAir() && !IsInWebUtil.blocked(place).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(place, sim.boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(place, this.getSimulated2().boundingBox).equals(ValuesEnum9.PLAYER) && !IsInWebUtil.blocked(place, this.getSimulated3().boundingBox).equals(ValuesEnum9.PLAYER)) {
                                                                float selfDamageNew = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostsAt(Vec3.atBottomCenterOf(checkPos), mc.player, mc.player.position(), List.of(), List.of(downPos, place), true);
                                                                if (!(selfDamageNew > this.maximumSelfDamage.getValue())) {
                                                                    float damageNew = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostsAt(Vec3.atBottomCenterOf(checkPos), target, sim.pos, List.of(), List.of(downPos, place), true);
                                                                    if (!(damageNew < bestDamage) && !(damageNew < defensiveDamage) && !(damageNew < this.minDamage())) {
                                                                        defensivePos = checkPos2;
                                                                        defensiveFace = (Direction) face0.getSecond();
                                                                        defensiveRotation = (Rotation) face0.getFirst();
                                                                        defensiveDamage = damageNew;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (defensiveDamage > bestDamage) {
            return new PlaceData(defensivePos, defensiveFace, defensiveDamage, defensiveRotation, Action.OBSIDIAN, null, check);
        } else {
            return bestPos != null && prevBestPos != null && mc.level.getBlockState(prevBestPos).isSolid() && mc.level.getBlockState(prevBestPos).getBlock() != Blocks.OBSIDIAN && mc.level.getBlockState(prevBestPos).getBlock() != Blocks.BEDROCK && mc.level.getBlockState(prevBestPos).getBlock() != Blocks.OBSIDIAN && mc.level.getBlockState(prevBestPos).getBlock() != Blocks.BARRIER ? new PlaceData(prevBestPos, prevBestFace, prevDamage, prevBestRotation, Action.BREAK, null, check) : new PlaceData(bestPos, bestFace, bestDamage, bestRotation, Action.BREAK, null, check);
        }
    }

    public PlaceData findPlaceYakor(Player target) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        Rotation bestRotation = null;
        double bestDamage = 0.0;
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -5; z < 5; z++) {
                    BlockPos checkPos = new BlockPos(mc.player.blockPosition().getX() + x, mc.player.blockPosition().getY() + y, mc.player.blockPosition().getZ() + z);
                    if (!mc.level.getBlockState(checkPos).isAir()) {
                        ToStringImpl7 result = IsInWebUtil.bestFaceYAura(checkPos, target, this.getSimulated(), this.minDamage(), this.maximumSelfDamage.getValue().floatValue(), true);
                        if (result.direction() != null && result.rotation() != null && !(result.damage() < bestDamage)) {
                            bestPos = checkPos.immutable();
                            bestFace = result.direction();
                            bestRotation = result.rotation();
                            bestDamage = result.damage();
                        }
                    }
                }
            }
        }
        return new PlaceData(bestPos, bestFace, bestDamage, bestRotation, Action.ANCHOR, null, null);
    }

    public PlaceData findYakor(Player target) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        Rotation bestRotation = null;
        double bestDamage = 0.0;
        boolean isCharged = false;
        boolean isDefensive = false;
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -5; z < 5; z++) {
                    BlockPos checkPos = new BlockPos(mc.player.blockPosition().getX() + x, mc.player.blockPosition().getY() + y, mc.player.blockPosition().getZ() + z);
                    BlockState state = mc.level.getBlockState(checkPos);
                    if (state.getBlock() == Blocks.RESPAWN_ANCHOR) {
                        Pair<Rotation, Direction> face = IsInWebUtil.getBestVisibleFace(checkPos, mc.player, true);
                        if (face != null && face.getFirst() != null && face.getSecond() != null) {
                            boolean charged = (Integer) state.getValue(RespawnAnchorBlock.CHARGE) > 0;
                            float selfDamage = GetExplosionDamageUtil.getDamageIgnoringBlock(Vec3.atCenterOf(checkPos), mc.player, checkPos, true);
                            float damage = GetExplosionDamageUtil.getDamageIgnoringBlockAt(Vec3.atCenterOf(checkPos), target, this.getSimulated().pos, checkPos, true);
                            if (!(damage < this.minDamage()) && !(damage < bestDamage)) {
                                if (!this.selfAllowed(selfDamage, damage)) {
                                    Pair<FaceDefensive, FaceDefensive> faceDefensive = IsInWebUtil.getBestVisibleFaceDefensive(checkPos, mc.player, target, this.getSimulated(), Math.max(bestDamage, this.minDamage()), this.maximumSelfDamage.getValue().floatValue(), true);
                                    if (((FaceDefensive) faceDefensive.getFirst()).face() != null && ((FaceDefensive) faceDefensive.getFirst()).rotation() != null) {
                                        bestPos = checkPos;
                                        bestFace = ((FaceDefensive) faceDefensive.getFirst()).face();
                                        bestRotation = ((FaceDefensive) faceDefensive.getFirst()).rotation();
                                        bestDamage = damage;
                                        isCharged = true;
                                        isDefensive = true;
                                    }
                                } else {
                                    bestPos = checkPos;
                                    bestFace = (Direction) face.getSecond();
                                    bestRotation = (Rotation) face.getFirst();
                                    bestDamage = damage;
                                    isCharged = charged;
                                    isDefensive = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return new PlaceData(bestPos, bestFace, bestDamage, bestRotation, !isCharged ? Action.CHARGE : (isDefensive ? Action.OBSIDIAN : Action.ANCHOR_BOOM), null, null);
    }

    public PlaceData findCrystalAttack(Player target) {
        double bestDamage = 0.0;
        Rotation bestRotation = null;
        Entity bestEntity = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal end) {
                Rotation rot = CalculateFovUtil.calculate(GetNearestPointUtil.getNearestPoint(end.getBoundingBox(), 0.0));
                if (DidRayTraceHitUtil.rayTrace(rot, 3.0, end.getBoundingBox())) {
                    float damage = GetSelfExplosionDamageUtil.getExplosionDamageAt(end.position(), target, this.getSimulated().pos, true);
                    if (!(damage < bestDamage)) {
                        float selfDamage = GetSelfExplosionDamageUtil.getExplosionDamage(end.position(), mc.player, true);
                        if (this.selfAllowed(selfDamage, damage)) {
                            bestDamage = damage;
                            bestRotation = rot;
                            bestEntity = end;
                        }
                    }
                }
            }
        }
        return new PlaceData(null, null, bestDamage + 5.0, bestRotation, Action.ATTACK_CRYSTAL, bestEntity, null);
    }
}
