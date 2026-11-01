package ess.ll.client.module.combat;

import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.PearlTrajectoryAimer;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PreMovementPacketEvent;
import ess.ll.client.setting.KeyBindSetting;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.ui.KeyBinding;
import ess.ll.client.ui.KeyboardKey;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.MovementUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangroveRootsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class PearlTarget extends ClientModule {
    private final ModeSetting mode;
    private final KeyBindSetting throwKey;
    private final ModeSetting algorithm;
    private final ModeSetting targetMode;
    private final NumberSetting<Integer> throwCooldown;
    private final NumberSetting<Integer> minimumDistance;
    private final ToggleSetting jumpForDistantTargets;
    private final ToggleSetting maximizeRange;
    private static final float[] PITCH_REFINE_STEPS = new float[] { 0.25F, -0.25F, 0.125F, -0.125F };
    private static final float[] YAW_REFINE_STEPS = new float[] { 1.0F, -1.0F, 0.5F, -0.5F };
    private static final float[] YAW_SEARCH_OFFSETS = new float[] { 0.0F, 3.0F, -3.0F, 6.0F, -6.0F, 9.0F, -9.0F, 12.0F, -12.0F };
    private final PearlTrajectoryAimer aimer;
    private final Map<ThrownEnderpearl, PearlTarget.PearlLanding> landingPredictions;
    private final List<Entity> cachedEntities;
    private final List<Entity> obstructingEntities;
    private final List<Entity> retriedBlockers;
    private Entity blockingEntity;
    private int blockerRetries;
    private long entityCacheTime;
    private Entity lastAuraTarget;
    private long lastAuraTargetTime;
    private long lastThrowTime;
    private int handledPearlId;
    private boolean restorePitchPending;
    private boolean jumpPending;
    private int jumpDelayTicks;
    private boolean wasOnGround;
    private float airbornePitch;
    private ThrownEnderpearl trackedPearl;
    private Vec3 landingPosition;
    private int landingTicks;
    private int yawOffsetIndex;
    private int pitchCandidateIndex;
    private float[] pitchCandidates;
    private boolean searchFinished;
    private double strictRadius;
    private double relaxedRadius;
    private boolean hasStrictSolution;
    private boolean hasRelaxedSolution;
    private boolean hasExtendedSolution;
    private float strictYaw;
    private float strictPitch;
    private float relaxedYaw;
    private float relaxedPitch;
    private float extendedYaw;
    private float extendedPitch;
    private double strictDistance;
    private double relaxedDistance;
    private double extendedDistance;

    private boolean isAuraTargetMode() {
        return this.targetMode.is("Aura");
    }

    private boolean isOwnPearl(ThrownEnderpearl thrownEnderpearl) {
        Entity owner = thrownEnderpearl.getOwner();
        return owner != null && owner.getId() == mc.player.getId();
    }

    private void throwAtTrackedPearl(PreMovementPacketEvent event) {
        if (this.trackedPearl != null) {
            if (this.jumpPending && this.jumpDelayTicks > 0 && mc.player.onGround()) {
                this.jumpDelayTicks--;
            } else if (this.searchFinished) {
                if (!this.hasStrictSolution || !this.tryThrow(event, this.strictYaw, this.strictPitch, "strict err=" + formatDistance(this.strictDistance))) {
                    if (this.searchFinished) {
                        boolean var2 = !this.hasRelaxedSolution || this.relaxedDistance > this.strictRadius + 0.6;
                        if (var2 && this.jumpForDistantTargets.getValue() && mc.player.onGround() && !this.jumpPending) {
                            this.jumpPending = true;
                            double var3 = this.evaluateAirbornePitch();
                            if (var3 <= this.relaxedRadius || this.maximizeRange.getValue() && var3 <= 12.0) {
                                this.restorePitchPending = true;
                                this.jumpDelayTicks = 6;
                                return;
                            }
                        }
                        if (!this.hasRelaxedSolution || !this.tryThrow(event, this.relaxedYaw, this.relaxedPitch, "relaxed err=" + formatDistance(this.relaxedDistance))) {
                            if (this.searchFinished) {
                                if (!this.maximizeRange.getValue() || !this.hasExtendedSolution || !(this.extendedDistance <= 12.0) || !this.tryThrow(event, this.extendedYaw, this.extendedPitch, "chase err=" + formatDistance(this.extendedDistance))) {
                                    if (this.searchFinished) {
                                        this.handledPearlId = this.trackedPearl.getId();
                                        this.clearTrackedPearl();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private long throwCooldownMillis() {
        return this.algorithm.is("RW") ? this.throwCooldown.getValue().intValue() : 1100L;
    }

    private void considerAngle(float var1, float var2) {
        PearlTarget.PearlImpact var3 = this.simulateThrowAt(var1, var2);
        if (var3 != null) {
            if (!this.isTreeImpact(var3)) {
                if (!this.isBlockedByEntity(var1, var2)) {
                    double distance = var3.pos().distanceTo(this.landingPosition);
                    if (!this.hasExtendedSolution || distance < this.extendedDistance) {
                        this.hasExtendedSolution = true;
                        this.extendedDistance = distance;
                        this.extendedYaw = var1;
                        this.extendedPitch = var2;
                    }
                    if (!(distance > this.relaxedRadius)) {
                        if (!this.hasRelaxedSolution || distance < this.relaxedDistance) {
                            this.hasRelaxedSolution = true;
                            this.relaxedDistance = distance;
                            this.relaxedYaw = var1;
                            this.relaxedPitch = var2;
                        }
                        if (distance <= this.strictRadius && this.isBetterStrictSolution(distance, var2)) {
                            this.hasStrictSolution = true;
                            this.strictDistance = distance;
                            this.strictYaw = var1;
                            this.strictPitch = var2;
                        }
                    }
                }
            }
        }
    }

    private float[] pitchCandidatesFor(double var1) {
        return this.expandPitchCandidates(this.solvePitchesToLanding(var1));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.clearTrackedPearl();
        this.landingPredictions.clear();
        this.cachedEntities.clear();
        this.obstructingEntities.clear();
        this.retriedBlockers.clear();
        this.lastAuraTarget = null;
        this.handledPearlId = -1;
        this.aimer.reset();
    }

    private boolean isTreeBlockAt(BlockPos blockPos) {
        return isTreeBlock(mc.level.getBlockState(blockPos));
    }

    private static String formatVector(Vec3 vector) {
        return "(" + formatDistance(vector.x) + ", " + formatDistance(vector.y) + ", " + formatDistance(vector.z) + ")";
    }

    private static String formatDistance(double var0) {
        return var0 >= 8.988465674311579E307 ? "-" : String.valueOf(Math.round(var0 * 100.0) / 100.0);
    }

    private boolean isOwnedBy(ThrownEnderpearl thrownEnderpearl, Entity entity) {
        Entity owner = thrownEnderpearl.getOwner();
        return entity != null && owner != null && owner.getId() == entity.getId();
    }

    private PearlTarget.PearlLanding predictLanding(ThrownEnderpearl thrownEnderpearl) {
        Vec3 position2 = thrownEnderpearl.position();
        Vec3 deltaMovement = thrownEnderpearl.getDeltaMovement();
        if (deltaMovement.lengthSqr() < 1.0E-4) {
            return null;
        }
        double minY = mc.level.getMinY();
        long var6 = System.currentTimeMillis();
        int var8 = Integer.MIN_VALUE;
        for (int var9 = 0; var9 < 180; var9++) {
            deltaMovement = this.stepVelocity(deltaMovement, position2);
            Vec3 vector = position2.add(deltaMovement);
            BlockPos blockPos = BlockPos.containing(vector);
            if (!mc.level.isLoaded(blockPos)) {
                return this.simulateLanding(position2, deltaMovement, var9, var8);
            }
            var8 = mc.level.getHeight(Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());
            BlockHitResult blockHitResult = mc.level.clip(new ClipContext(position2, vector, Block.COLLIDER, Fluid.NONE, mc.player));
            if (blockHitResult.getType() == Type.BLOCK) {
                return new PearlTarget.PearlLanding(blockHitResult.getLocation(), var9 + 1, false, var6);
            }
            position2 = vector;
            if (position2.y < minY) {
                return new PearlTarget.PearlLanding(new Vec3(position2.x, minY, position2.z), var9 + 1, false, var6);
            }
        }
        return null;
    }

    private boolean isInWater(Vec3 vector) {
        BlockPos blockPos = BlockPos.containing(vector.x, vector.y, vector.z);
        return !mc.level.getFluidState(blockPos).isEmpty() || mc.level.getBlockState(blockPos).is(Blocks.WATER);
    }

    private float[] solvePitchesToLanding(double var1) {
        Vec3 vector = this.throwOrigin();
        double var4 = this.landingPosition.x - vector.x;
        double var6 = this.landingPosition.z - vector.z;
        double var8 = Math.sqrt(var4 * var4 + var6 * var6);
        Vec3 deltaMovement = mc.player.getDeltaMovement();
        double var11 = var8 < 1.0E-6 ? 0.0 : (deltaMovement.x * var4 + deltaMovement.z * var6) / var8;
        return solvePitches(var8, this.landingPosition.y - vector.y, var11, var1);
    }

    private float clampPitch(float var1) {
        return Mth.clamp(var1, -80.0F, 70.0F);
    }

    private double findGroundY(Vec3 vector, int var2) {
        int var3 = Mth.floor(vector.x);
        int var4 = Mth.floor(vector.z);
        int var5 = var3 >> 4;
        int var6 = var4 >> 4;
        int blockY = mc.player.getBlockY();
        for (int var8 = 1; var8 <= 8; var8++) {
            for (int var9 = -var8; var9 <= var8; var9++) {
                for (int var10 = -var8; var10 <= var8; var10++) {
                    if (Math.max(Math.abs(var9), Math.abs(var10)) == var8) {
                        int var11 = var5 + var9;
                        int var12 = var6 + var10;
                        int var13 = Mth.clamp(var3, var11 << 4, (var11 << 4) + 15);
                        int var14 = Mth.clamp(var4, var12 << 4, (var12 << 4) + 15);
                        if (mc.level.isLoaded(new BlockPos(var13, blockY, var14))) {
                            return mc.level.getHeight(Types.MOTION_BLOCKING, var13, var14);
                        }
                    }
                }
            }
        }
        return var2 != Integer.MIN_VALUE ? var2 : mc.player.getY();
    }

    private static double clampedLinearRadius(double var0, double var2, double var4, double var6) {
        double var8 = var2 + var0 * var4;
        return var8 < var2 ? var2 : Math.min(var8, var6);
    }

    static double ballisticHeightError(float var0, double var1, double var3, double var5, double var7) {
        double var9 = Math.toRadians(var0);
        double var11 = 1.5 * Math.cos(var9) + var5;
        if (var11 <= 1.0E-6) {
            return Double.NaN;
        }
        double var13 = var1 / var11;
        double var15 = 98.99999999999991;
        if (var13 >= var15) {
            return Double.NaN;
        }
        double var17 = Math.log(1.0 - var13 * 0.010000000000000009 / 0.99) / Math.log(0.99);
        if (var17 > 180.0) {
            return Double.NaN;
        }
        double var19 = (var17 - var13) / 0.010000000000000009;
        double var21 = -1.5 * Math.sin(var9) + var7;
        return var21 * var13 - 0.029699999999999997 * var19 - var3;
    }

    private boolean isThrowRequested() {
        if (!this.mode.is("Bind")) {
            return true;
        } else {
            int key = this.throwKey.getValue().getKey();
            if (key == -1) {
                return false;
            } else {
                return key <= -100 ? KeyboardKey.isMouseDown(key) : KeyboardKey.isKeyDown(key);
            }
        }
    }

    private boolean hasActiveOwnPearl() {
        for (Entity entity : this.nearbyEntities()) {
            if (entity instanceof ThrownEnderpearl var3 && var3.isAlive() && this.isOwnPearl(var3)) {
                return true;
            }
        }
        return false;
    }

    private void collectObstructingEntities() {
        this.obstructingEntities.clear();
        if (this.landingPosition != null) {
            Vec3 vector = this.throwOrigin();
            double var2 = Math.hypot(this.landingPosition.x - vector.x, this.landingPosition.z - vector.z);
            double var4 = 6.5 + var2 * 0.2126;
            AABB bounds = new AABB(vector, this.landingPosition).inflate(var4).expandTowards(0.0, 36.0, 0.0);
            this.obstructingEntities.addAll(mc.level.getEntities(mc.player, bounds, this::isCollidableEntity));
            for (Entity entity : this.retriedBlockers) {
                if (entity.isAlive() && !this.obstructingEntities.contains(entity)) {
                    this.obstructingEntities.add(entity);
                }
            }
        }
    }

    private static boolean isTreeBlock(BlockState blockState) {
        return blockState.getBlock() instanceof LeavesBlock || blockState.getBlock() instanceof MangroveRootsBlock || blockState.getBlock() instanceof HugeMushroomBlock || blockState.is(BlockTags.LEAVES) || blockState.is(BlockTags.LOGS) || blockState.is(BlockTags.WART_BLOCKS);
    }

    private boolean isBetterStrictSolution(double var1, float var3) {
        if (!this.hasStrictSolution) {
            return true;
        } else if (var1 < this.strictDistance - 0.15) {
            return true;
        } else {
            return var1 > this.strictDistance + 0.15 ? false : var3 > this.strictPitch || var3 == this.strictPitch && var1 < this.strictDistance;
        }
    }

    private boolean trajectoryHitsEntity(float var1, float var2, boolean var3) {
        Vec3 vector = this.throwOrigin();
        Vec3 vector2 = this.throwVelocity(var1, var2);
        double minY = mc.level.getMinY();
        for (int var8 = 0; var8 < 180; var8++) {
            vector2 = this.stepVelocity(vector2, vector);
            Vec3 vector3 = vector.add(vector2);
            BlockHitResult blockHitResult = mc.level.clip(new ClipContext(vector, vector3, Block.COLLIDER, Fluid.NONE, mc.player));
            Vec3 vector4 = blockHitResult.getType() == Type.BLOCK ? blockHitResult.getLocation() : vector3;
            if (this.segmentHitsEntity(vector, vector4, var8, var3)) {
                return true;
            }
            if (blockHitResult.getType() == Type.BLOCK) {
                return false;
            }
            vector = vector3;
            if (vector.y < minY) {
                return false;
            }
        }
        return false;
    }

    private void continueAngleSearch() {
        if (!this.searchFinished && this.landingPosition != null) {
            this.collectObstructingEntities();
            float var1 = this.yawToLanding();
            long var2 = System.nanoTime();
            while (this.yawOffsetIndex < YAW_SEARCH_OFFSETS.length) {
                float var4 = var1 + YAW_SEARCH_OFFSETS[this.yawOffsetIndex];
                while (this.pitchCandidateIndex < this.pitchCandidates.length) {
                    float var5 = this.pitchCandidates[this.pitchCandidateIndex];
                    this.pitchCandidateIndex++;
                    this.considerAngle(var4, var5);
                    if (System.nanoTime() - var2 > 2000000L) {
                        return;
                    }
                }
                this.pitchCandidateIndex = 0;
                this.yawOffsetIndex++;
                if (this.hasStrictSolution) {
                    break;
                }
            }
            this.refineStrictSolution();
            this.searchFinished = true;
        }
    }

    private float[] expandPitchCandidates(float[] var1) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        for (float var6 : var1) {
            linkedHashSet.add(this.clampPitch(var6));
            for (float var7 = 0.5F; var7 <= 6.0F; var7 += 0.5F) {
                linkedHashSet.add(this.clampPitch(var6 - var7));
                linkedHashSet.add(this.clampPitch(var6 + var7));
            }
        }
        for (float var8 = 70.0F; var8 >= -80.0F; var8 -= 3.0F) {
            linkedHashSet.add(var8);
        }
        float[] var9 = new float[linkedHashSet.size()];
        int var10 = 0;
        for (float var12 : (Iterable<Float>) (Iterable) linkedHashSet) {
            var9[var10++] = var12;
        }
        return var9;
    }

    private double distanceToLanding() {
        return mc.player != null && this.landingPosition != null ? mc.player.position().distanceTo(this.landingPosition) : Double.MAX_VALUE;
    }

    private double evaluateAirbornePitch() {
        float var1 = this.yawToLanding();
        float NaN2 = Float.NaN;
        double var3 = Double.MAX_VALUE;
        for (float var8 : this.pitchCandidatesFor(0.35)) {
            PearlTarget.PearlImpact var9 = this.simulateThrow(this.throwVelocity(var1, var8, 0.35));
            if (var9 != null && !this.isTreeImpact(var9)) {
                double distance = var9.pos().distanceTo(this.landingPosition);
                if (distance < var3) {
                    var3 = distance;
                    NaN2 = var8;
                }
            }
        }
        if (!Float.isNaN(NaN2)) {
            this.airbornePitch = NaN2;
        }
        return var3;
    }

    private void restorePearlOwners() {
        var var1 = mc.level.players();
        for (ThrownEnderpearl thrownEnderpearl : mc.level.getEntitiesOfClass(ThrownEnderpearl.class, mc.player.getBoundingBox().inflate(160.0), var0 -> true)) {
            if (thrownEnderpearl.getOwner() == null) {
                var1.stream().filter(var1x -> var1x.getEyePosition().distanceTo(thrownEnderpearl.position()) <= 6.0).min(Comparator.comparingDouble(var1x -> var1x.distanceTo(thrownEnderpearl))).ifPresent(thrownEnderpearl::setOwner);
            }
        }
    }

    @Subscribe
    private void onPreMovementPacket(PreMovementPacketEvent event) {
        if (mc.player != null && mc.level != null) {
            if (!mc.player.isFallFlying() && !mc.player.isUnderWater() && !mc.player.isInLava()) {
                this.restorePearlOwners();
                if (System.currentTimeMillis() - this.lastThrowTime >= this.throwCooldownMillis()) {
                    if (this.isThrowRequested()) {
                        Slot slot = InventoryUtils.findSlot(Items.ENDER_PEARL);
                        if (slot != null && !slot.getItem().isEmpty()) {
                            if (!mc.player.getCooldowns().isOnCooldown(slot.getItem())) {
                                if (this.hasActiveOwnPearl()) {
                                    this.clearTrackedPearl();
                                } else if (this.algorithm.is("RW")) {
                                    this.throwAtAimerSolution(event);
                                } else if (this.updateTrackedPearl()) {
                                    this.continueAngleSearch();
                                    this.throwAtTrackedPearl(event);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Vec3 stepVelocity(Vec3 vector, Vec3 vector2) {
        double var3 = this.isInWater(vector2) ? 0.8 : 0.99;
        return vector.subtract(0.0, 0.03, 0.0).scale(var3);
    }

    public static boolean isInsideTree(Vec3 vector) {
        if (isTreeBlock(mc.level.getBlockState(BlockPos.containing(vector.x, vector.y - 0.001, vector.z)))) {
            return true;
        }
        AABB bounds = new AABB(vector.x - 0.3, vector.y, vector.z - 0.3, vector.x + 0.3, vector.y + 1.8, vector.z + 0.3);
        MutableBlockPos blockPos = new MutableBlockPos();
        for (int var3 = Mth.floor(bounds.minX); var3 <= Mth.floor(bounds.maxX); var3++) {
            for (int var4 = Mth.floor(bounds.minY); var4 <= Mth.floor(bounds.maxY); var4++) {
                for (int var5 = Mth.floor(bounds.minZ); var5 <= Mth.floor(bounds.maxZ); var5++) {
                    blockPos.set(var3, var4, var5);
                    if (isTreeBlock(mc.level.getBlockState(blockPos)) && new AABB(blockPos).intersects(bounds)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void resetThrowState() {
        this.resetAngleSearch();
        this.restorePitchPending = false;
        this.jumpPending = false;
        this.jumpDelayTicks = 0;
        this.airbornePitch = Float.NaN;
        this.blockerRetries = 0;
        this.retriedBlockers.clear();
        this.blockingEntity = null;
    }

    private boolean updateTrackedPearl() {
        if (this.handledPearlId != -1) {
            Entity entity = mc.level.getEntity(this.handledPearlId);
            if (entity == null || !entity.isAlive()) {
                this.handledPearlId = -1;
            }
        }
        Entity entity2 = this.resolveAuraTarget();
        if (this.isAuraTargetMode() && entity2 == null) {
            this.clearTrackedPearl();
            return false;
        }
        var var2 = this.nearbyEntities();
        double var3 = this.minimumDistance.getValue().doubleValue();
        Vec3 position2 = mc.player.position();
        long var6 = System.currentTimeMillis();
        ThrownEnderpearl thrownEnderpearl = null;
        PearlTarget.PearlLanding var9 = null;
        double var10 = Double.MAX_VALUE;
        int var12 = 0;
        for (Entity entity3 : var2) {
            if (entity3 instanceof ThrownEnderpearl var15 && var15.isAlive() && var15.getId() != this.handledPearlId && !this.isOwnPearl(var15) && (!this.isAuraTargetMode() || this.isOwnedBy(var15, entity2))) {
                var12++;
                PearlTarget.PearlLanding var16 = this.landingPredictions.get(var15);
                if (var16 == null || var16.estimated() || var6 - var16.time() > 400L) {
                    var16 = this.predictLanding(var15);
                    if (var16 == null) {
                        this.landingPredictions.remove(var15);
                        continue;
                    }
                    this.landingPredictions.put(var15, var16);
                }
                double distance = position2.distanceTo(var16.landing());
                if (!(distance < var3) && !(distance > 160.0) && distance < var10) {
                    var10 = distance;
                    thrownEnderpearl = var15;
                    var9 = var16;
                }
            }
        }
        if (thrownEnderpearl == null) {
            this.clearTrackedPearl();
            return false;
        }
        if (thrownEnderpearl != this.trackedPearl || this.landingPosition == null || this.landingPosition.distanceToSqr(var9.landing()) > 0.36 || this.wasOnGround != mc.player.onGround()) {
            this.trackPearl(thrownEnderpearl, var9);
        }
        return true;
    }

    private void clearTrackedPearl() {
        this.trackedPearl = null;
        this.landingPosition = null;
        this.resetThrowState();
    }

    private void resetAngleSearch() {
        this.yawOffsetIndex = 0;
        this.pitchCandidateIndex = 0;
        this.searchFinished = false;
        this.hasStrictSolution = false;
        this.hasRelaxedSolution = false;
        this.hasExtendedSolution = false;
        this.strictDistance = Double.MAX_VALUE;
        this.relaxedDistance = Double.MAX_VALUE;
        this.extendedDistance = Double.MAX_VALUE;
    }

    private static float bisectPitch(float var0, float var1, double var2, double var4, double var6, double var8, double var10) {
        boolean var12 = var2 <= 0.0;
        for (int var13 = 0; var13 < 14; var13++) {
            float var14 = (var0 + var1) * 0.5F;
            double var15 = ballisticHeightError(var14, var4, var6, var8, var10);
            if (Double.isNaN(var15)) {
                return var1;
            }
            if (var15 <= 0.0 == var12) {
                var0 = var14;
            } else {
                var1 = var14;
            }
        }
        return (var0 + var1) * 0.5F;
    }

    private Vec3 throwVelocity(float var1, float var2) {
        return this.throwVelocity(var1, var2, mc.player.onGround() ? 0.0 : mc.player.getDeltaMovement().y);
    }

    private boolean isCollidableEntity(Entity entity) {
        return entity != mc.player && entity.canBeHitByProjectile();
    }

    private PearlTarget.PearlLanding simulateLanding(Vec3 vector, Vec3 vector2, int var3, int var4) {
        double var5 = this.findGroundY(vector, var4);
        double minY = mc.level.getMinY();
        long var9 = System.currentTimeMillis();
        Vec3 vector3 = vector;
        Vec3 vector4 = vector2;
        for (int var13 = var3; var13 < 180; var13++) {
            vector4 = vector4.subtract(0.0, 0.03, 0.0).scale(0.99);
            vector3 = vector3.add(vector4);
            if (vector3.y <= var5) {
                return new PearlTarget.PearlLanding(new Vec3(vector3.x, var5, vector3.z), var13 + 1, true, var9);
            }
            if (vector3.y < minY) {
                return new PearlTarget.PearlLanding(new Vec3(vector3.x, minY, vector3.z), var13 + 1, true, var9);
            }
        }
        return new PearlTarget.PearlLanding(new Vec3(vector3.x, var5, vector3.z), 180, true, var9);
    }

    private float yawToLanding() {
        Vec3 vector = this.throwOrigin();
        double var2 = this.landingPosition.x - vector.x;
        double var4 = this.landingPosition.z - vector.z;
        return yawFromDelta(var2, var4);
    }

    private PearlTarget.PearlImpact simulateThrow(Vec3 vector) {
        Vec3 vector2 = this.throwOrigin();
        double minY = mc.level.getMinY();
        double var5 = Math.min(vector2.y, this.landingPosition.y) - 6.0;
        double var7 = this.landingPosition.x - vector2.x;
        double var9 = this.landingPosition.z - vector2.z;
        double var11 = Math.sqrt(var7 * var7 + var9 * var9) + 6.0;
        double var13 = var11 * var11;
        for (int var15 = 0; var15 < 180; var15++) {
            vector = this.stepVelocity(vector, vector2);
            Vec3 vector3 = vector2.add(vector);
            if (!mc.level.isLoaded(BlockPos.containing(vector3))) {
                PearlTarget.PearlLanding var22 = this.simulateLanding(vector2, vector, var15, Integer.MIN_VALUE);
                return var22 == null ? null : new PearlTarget.PearlImpact(var22.landing(), null);
            }
            BlockHitResult blockHitResult = mc.level.clip(new ClipContext(vector2, vector3, Block.COLLIDER, Fluid.NONE, mc.player));
            if (blockHitResult.getType() == Type.BLOCK) {
                return new PearlTarget.PearlImpact(blockHitResult.getLocation(), blockHitResult.getBlockPos());
            }
            vector2 = vector3;
            if (vector2.y < minY) {
                return null;
            }
            if (vector2.y < var5) {
                return null;
            }
            double var18 = vector2.x - (this.landingPosition.x - var7);
            double var20 = vector2.z - (this.landingPosition.z - var9);
            if (var18 * var18 + var20 * var20 > var13) {
                return null;
            }
        }
        return null;
    }

    static float[] solvePitches(double var0, double var2, double var4, double var6) {
        var var8 = new ArrayList(2);
        double var9 = Double.MAX_VALUE;
        float NaN2 = Float.NaN;
        double NaN3 = Double.NaN;
        float var14 = 70.0F;
        for (float var15 = 70.0F; var15 >= -80.0F; var14 = var15--) {
            double var16 = ballisticHeightError(var15, var0, var2, var4, var6);
            if (!Double.isNaN(var16)) {
                if (!Double.isNaN(NaN3) && var16 <= 0.0 != NaN3 <= 0.0) {
                    var8.add(bisectPitch(var14, var15, NaN3, var0, var2, var4, var6));
                }
                if (Math.abs(var16) < var9) {
                    var9 = Math.abs(var16);
                    NaN2 = var15;
                }
            }
            NaN3 = var16;
        }
        if (var8.isEmpty() && !Float.isNaN(NaN2)) {
            var8.add(NaN2);
        }
        float[] var18 = new float[var8.size()];
        for (int var19 = 0; var19 < var8.size(); var19++) {
            var18[var19] = (Float) var8.get(var19);
        }
        return var18;
    }

    protected boolean throwPearl(PreMovementPacketEvent event, float var2, float var3) {
        return ModuleManager.INSTANCE.clickPearl().throwPearl(event, var2, var3);
    }

    private void refineStrictSolution() {
        if (this.hasStrictSolution) {
            for (int var1 = 0; var1 < 2; var1++) {
                float var2 = this.strictYaw;
                float var3 = this.strictPitch;
                for (float var7 : PITCH_REFINE_STEPS) {
                    this.considerAngle(var2, this.clampPitch(var3 + var7));
                }
                for (float var11 : YAW_REFINE_STEPS) {
                    this.considerAngle(var2 + var11, var3);
                }
                if (this.strictYaw == var2 && this.strictPitch == var3) {
                    return;
                }
            }
        }
    }

    public PearlTarget() {
        super(ModuleCategory.Combat, ModuleSubCategory.Combat);
        this.mode = new ModeSetting("Mode").addModes("Always", "Bind").withDefault("Always").withContainer(this);
        this.throwKey = new KeyBindSetting("Throw").withDefault(new KeyBinding()).withVisibility(() -> this.mode.is("Bind")).withContainer(this);
        this.algorithm = new ModeSetting("Алгоритм").addModes("Default", "RW").withDefault("Default").withContainer(this);
        this.targetMode = new ModeSetting("Target").addModes("Aura", "All").withDefault("Aura").withContainer(this);
        this.throwCooldown = new NumberSetting<Integer>("Кулдаун броска").withMin(500).withMax(5000).withStep(50).withDefault(2000).withVisibility(() -> this.algorithm.is("RW")).withContainer(this);
        this.minimumDistance = new NumberSetting<Integer>("Мин дистанция").withMin(8).withMax(20).withStep(1).withDefault(10).withContainer(this);
        this.jumpForDistantTargets = new ToggleSetting("Прыжок для дальних").withDefault(true).withVisibility(() -> this.algorithm.is("Default")).withContainer(this);
        this.maximizeRange = new ToggleSetting("Догон (макс дальность)").withDefault(true).withVisibility(() -> this.algorithm.is("Default")).withContainer(this);
        this.aimer = new PearlTrajectoryAimer();
        this.landingPredictions = new HashMap<>();
        this.cachedEntities = new ArrayList<>(64);
        this.obstructingEntities = new ArrayList<>(16);
        this.retriedBlockers = new ArrayList<>(4);
        this.blockingEntity = null;
        this.blockerRetries = 0;
        this.entityCacheTime = 0L;
        this.lastAuraTarget = null;
        this.lastAuraTargetTime = 0L;
        this.lastThrowTime = 0L;
        this.handledPearlId = -1;
        this.restorePitchPending = false;
        this.jumpPending = false;
        this.jumpDelayTicks = 0;
        this.wasOnGround = true;
        this.airbornePitch = Float.NaN;
        this.trackedPearl = null;
        this.landingPosition = null;
        this.landingTicks = 0;
        this.yawOffsetIndex = 0;
        this.pitchCandidateIndex = 0;
        this.pitchCandidates = new float[0];
        this.searchFinished = false;
        this.strictRadius = 0.7;
        this.relaxedRadius = 1.19;
        this.hasStrictSolution = false;
        this.hasRelaxedSolution = false;
        this.hasExtendedSolution = false;
        this.strictDistance = Double.MAX_VALUE;
        this.relaxedDistance = Double.MAX_VALUE;
        this.extendedDistance = Double.MAX_VALUE;
        this.setName("Pearl Target");
    }

    private Vec3 throwOrigin() {
        return new Vec3(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
    }

    private void throwAtAimerSolution(PreMovementPacketEvent event) {
        PearlTrajectoryAimer.ThrowSolution var2 = this.aimer.solve(this.minimumDistance.getValue().doubleValue(), this.isAuraTargetMode());
        if (var2 != null) {
            if (!this.trajectoryHitsEntity(var2.yaw(), var2.pitch(), true)) {
                if (ModuleManager.INSTANCE.clickPearl().throwPearl(event, var2.yaw(), var2.pitch())) {
                    this.lastThrowTime = System.currentTimeMillis();
                    this.aimer.clearState();
                }
            }
        }
    }

    private PearlTarget.PearlImpact simulateThrowAt(float var1, float var2) {
        return this.simulateThrow(this.throwVelocity(var1, var2));
    }

    private boolean isBlockedByEntity(float var1, float var2) {
        return this.obstructingEntities.isEmpty() ? false : this.trajectoryHitsEntity(var1, var2, false);
    }

    private Vec3 throwVelocity(float var1, float var2, double var3) {
        double var5 = Math.toRadians(var1);
        double var7 = Math.toRadians(var2);
        double var9 = Math.cos(var7);
        Vec3 vector = new Vec3(-Math.sin(var5) * var9, -Math.sin(var7), Math.cos(var5) * var9).scale(1.5);
        Vec3 deltaMovement = mc.player.getDeltaMovement();
        return vector.add(deltaMovement.x, var3, deltaMovement.z);
    }

    private List<Entity> nearbyEntities() {
        long var1 = System.currentTimeMillis();
        if (var1 - this.entityCacheTime < 50L && !this.cachedEntities.isEmpty()) {
            return this.cachedEntities;
        }
        this.entityCacheTime = var1;
        this.cachedEntities.clear();
        this.cachedEntities.addAll(mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(160.0)));
        return this.cachedEntities;
    }

    private boolean isTreeImpact(PearlTarget.PearlImpact var1) {
        return var1.block() != null && this.isTreeBlockAt(var1.block()) ? true : isInsideTree(var1.pos());
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (this.restorePitchPending && mc.player != null) {
            if (mc.player.onGround() && !event.isJump()) {
                event.setJump(true);
            }
            this.restorePitchPending = false;
        }
    }

    private boolean segmentHitsEntity(Vec3 vector, Vec3 vector2, int var3, boolean var4) {
        double var5 = this.entityHitboxMargin(var3);
        if (var4) {
            AABB bounds = new AABB(vector, vector2).inflate(var5 + 4.0);
            for (Entity entity : mc.level.getEntities(mc.player, bounds, this::isCollidableEntity)) {
                if (this.expandedEntityBounds(entity, var3, var5).clip(vector, vector2).isPresent()) {
                    this.blockingEntity = entity;
                    return true;
                }
            }
            return false;
        } else {
            for (Entity entity2 : this.obstructingEntities) {
                if (this.expandedEntityBounds(entity2, var3, var5).clip(vector, vector2).isPresent()) {
                    return true;
                }
            }
            return false;
        }
    }

    private double entityHitboxMargin(int var1) {
        return Mth.clamp((var1 - 2.0) / 20.0, 0.0, 0.3);
    }

    private void trackPearl(ThrownEnderpearl thrownEnderpearl, PearlTarget.PearlLanding var2) {
        this.trackedPearl = thrownEnderpearl;
        this.landingPosition = var2.landing();
        this.landingTicks = var2.ticks();
        Vec3 vector = this.throwOrigin();
        double var4 = this.landingPosition.x - vector.x;
        double var6 = this.landingPosition.z - vector.z;
        double var8 = Math.sqrt(var4 * var4 + var6 * var6);
        this.strictRadius = clampedLinearRadius(var8, 0.7, 0.02, 2.2);
        this.relaxedRadius = this.strictRadius * 1.7;
        if (!mc.player.onGround() && !Float.isNaN(this.airbornePitch)) {
            this.pitchCandidates = this.expandPitchCandidates(new float[] { this.airbornePitch });
        } else {
            this.pitchCandidates = this.pitchCandidatesFor(mc.player.onGround() ? 0.0 : mc.player.getDeltaMovement().y);
        }
        this.resetThrowState();
        this.wasOnGround = mc.player.onGround();
    }

    private AABB expandedEntityBounds(Entity entity, int var2, double var3) {
        AABB bounds = entity.getBoundingBox().inflate(var3);
        Vec3 vector = MovementUtils.motionOf(entity);
        if (vector.lengthSqr() < 1.0E-6) {
            return bounds;
        }
        double var7 = Math.min(var2 + 1, 10);
        return bounds.expandTowards(vector.x * var7, vector.y * var7, vector.z * var7);
    }

    private Entity resolveAuraTarget() {
        if (!this.isAuraTargetMode()) {
            return null;
        }
        LivingEntity entity = AttackAura.currentTarget();
        long var2 = System.currentTimeMillis();
        if (entity != null && entity.isAlive()) {
            this.lastAuraTarget = entity;
            this.lastAuraTargetTime = var2;
            return entity;
        }
        if (this.lastAuraTarget != null) {
            if (this.lastAuraTarget.isAlive() && !this.lastAuraTarget.isRemoved() && var2 - this.lastAuraTargetTime < 10000L) {
                return this.lastAuraTarget;
            }
            this.lastAuraTarget = null;
            return null;
        } else {
            return null;
        }
    }

    private boolean tryThrow(PreMovementPacketEvent event, float var2, float var3, String var4) {
        this.blockingEntity = null;
        if (this.trajectoryHitsEntity(var2, var3, true)) {
            if (this.blockingEntity != null && this.blockerRetries < 3) {
                this.blockerRetries++;
                if (!this.retriedBlockers.contains(this.blockingEntity)) {
                    this.retriedBlockers.add(this.blockingEntity);
                }
                this.resetAngleSearch();
            }
            return false;
        } else {
            int var5 = this.trackedPearl != null ? this.trackedPearl.getId() : -1;
            boolean var6 = this.throwPearl(event, var2, var3);
            if (var6) {
                this.lastThrowTime = System.currentTimeMillis();
                this.handledPearlId = var5;
                this.clearTrackedPearl();
                return true;
            } else {
                return false;
            }
        }
    }

    private static float yawFromDelta(double var0, double var2) {
        return (float) (Math.toDegrees(Math.atan2(var2, var0)) - 90.0);
    }

    private record PearlLanding(Vec3 landing, int ticks, boolean estimated, long time) {
    }

    private record PearlImpact(Vec3 pos, BlockPos block) {
    }
}
