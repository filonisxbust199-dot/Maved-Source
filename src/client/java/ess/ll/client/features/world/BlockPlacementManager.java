package ess.ll.client.features.world;

import ess.ll.client.event.AttackEntityEvent;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.ExplosionDamageUtils;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.CrystalAura;
import ess.ll.client.module.player.AutoTool;
import ess.ll.client.network.PostMovementPacketEvent;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.EnchantmentUtils;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.PlayerUtils;
import ess.ll.client.util.RandomUtils;
import ess.ll.client.util.RaycastUtils;
import ess.ll.client.util.RotationManager;
import ess.ll.client.util.RotationMath;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector2f;

public enum BlockPlacementManager implements MinecraftAccess {
    INSTANCE;

    public static final double[] PLAYER_EYE_HEIGHTS = new double[] { 1.62, 1.27, 0.4 };
    private static final double[] CURRENT_EYE_ONLY = new double[0];
    private static final double[] FACE_SAMPLE_OFFSETS = new double[] { 0.5, 0.1, 0.9, 0.3, 0.7 };
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
    private final Vec3[] placementEyePositions = new Vec3[4];
    private final Vec3[] faceEyePositions = new Vec3[4];
    private final Vec3[] entityEyePositions = new Vec3[4];
    private int rotationModeTick = Integer.MIN_VALUE;
    private boolean matrixRotation;
    private final BlockPlacementManager.RotationPatternTracker rotationPattern = new BlockPlacementManager.RotationPatternTracker();

    public void register() {
        EventBus.register(this);
    }

    public static boolean usesMatrixRotation() {
        return INSTANCE.matrixRotationEnabled();
    }

    @Subscribe
    private void onPostMovementPacket(PostMovementPacketEvent event) {
        if (mc.player != null) {
            this.rotationPattern.recordRotation(mc.player.getYRot(), mc.player.getXRot());
        }
    }

    @Subscribe
    private void onAttackEntity(AttackEntityEvent event) {
        if (event.getPhase() == AttackEntityEvent.Phase.POST) {
            this.rotationPattern.recordAttack();
        }
    }

    public static float yawJitter(float var0) {
        return usesMatrixRotation() ? INSTANCE.rotationPattern.selectYawJitter(var0) : RandomUtils.nextFloat();
    }

    public static float pitchJitter() {
        return usesMatrixRotation() ? 0.0F : RandomUtils.nextFloat();
    }

    public static boolean canAttackWithinRotationBudget() {
        return INSTANCE.rotationPattern.canAttack();
    }

    public static boolean allowsPlacementThroughWalls() {
        CrystalAura crystalAura2 = ModuleManager.INSTANCE.crystalAura();
        return crystalAura2 != null && crystalAura2.isEnabled() && crystalAura2.isPlacementThroughWallsEnabled();
    }

    public static List<Vec3> eyePositions() {
        if (mc.player == null) {
            return List.of();
        }
        int var0 = INSTANCE.collectEyePositions(INSTANCE.entityEyePositions, PLAYER_EYE_HEIGHTS);
        return Arrays.asList(INSTANCE.entityEyePositions).subList(0, var0);
    }

    public static boolean isWithinInteractionRange(Vec3 vector) {
        return mc.player != null && vector != null ? mc.player.position().distanceToSqr(vector) <= 22.090000000000003 : false;
    }

    public static boolean hasFastObsidianTool() {
        return mc.player == null ? false : EnchantmentUtils.getLevel(AutoTool.bestAvailableTool(Blocks.OBSIDIAN.defaultBlockState()), Enchantments.EFFICIENCY) >= 4;
    }

    public static BlockHitResult hitFromAnyEyePosition(BlockPos blockPos, Direction direction) {
        return INSTANCE.raycastFromEyePositions(blockPos, direction);
    }

    public static BlockHitResult currentBlockHit(BlockPos blockPos) {
        return INSTANCE.raycastCurrentBlock(blockPos, null);
    }

    public static BlockHitResult currentBlockHit(BlockPos blockPos, Direction direction) {
        return INSTANCE.raycastCurrentBlock(blockPos, direction);
    }

    public static Vector2f rotationToFacePoint(BlockPos blockPos, Direction direction, Vec3 vector) {
        return INSTANCE.findRotationToFacePoint(blockPos, direction, vector);
    }

    public static Vector2f blockRotation(BlockPos blockPos, double var1) {
        return INSTANCE.findBlockRotation(blockPos, var1, false);
    }

    public static Vector2f blockRotation(BlockPos blockPos, double var1, boolean var3) {
        return INSTANCE.findBlockRotation(blockPos, var1, var3);
    }

    public static BlockPlacementManager.PlacementTarget placementTarget(BlockPos blockPos, double var1) {
        return INSTANCE.findPlacementTarget(blockPos, var1, false);
    }

    public static BlockPlacementManager.PlacementTarget placementTarget(BlockPos blockPos, double var1, boolean var3) {
        return INSTANCE.findPlacementTarget(blockPos, var1, var3);
    }

    public static Vec3 randomizedBlockAim(BlockPos blockPos) {
        return INSTANCE.sampleBlockAim(blockPos);
    }

    public static boolean isPointVisible(Vec3 vector) {
        return mc.player != null && mc.level != null && vector != null ? INSTANCE.visibleFromAnyEyePosition(vector) : false;
    }

    public static Vector2f entityRotation(Entity entity, Vec3 vector, double var2) {
        return INSTANCE.findEntityRotation(entity, vector, var2);
    }

    public static boolean isLookingAtEntity(Entity entity, double var1) {
        return mc.player != null && entity != null ? INSTANCE.raycastEntity(entity, mc.player.getYRot(), mc.player.getXRot(), var1) : false;
    }

    public static Vector2f quantizeRotation(Vector2f vector) {
        if (vector == null) {
            return null;
        } else {
            float var1 = RotationMath.gcd();
            float serverYaw = RotationManager.getServerYaw();
            float serverPitch = RotationManager.getServerPitch();
            if (!(var1 <= 0.0F) && !Float.isNaN(serverYaw) && !Float.isNaN(serverPitch)) {
                vector.x = serverYaw + Math.round(Mth.wrapDegrees(vector.x - serverYaw) / var1) * var1;
                vector.y = Mth.clamp(serverPitch + Math.round((vector.y - serverPitch) / var1) * var1, -90.0F, 90.0F);
                return vector;
            } else {
                return vector;
            }
        }
    }

    private boolean matrixRotationEnabled() {
        this.refreshRotationMode();
        return this.matrixRotation;
    }

    private void refreshRotationMode() {
        if (mc.player == null) {
            this.matrixRotation = false;
        } else {
            int tickCount2 = mc.player.tickCount;
            if (tickCount2 != this.rotationModeTick) {
                this.rotationModeTick = tickCount2;
                CrystalAura crystalAura2 = ModuleManager.INSTANCE.crystalAura();
                this.matrixRotation = crystalAura2 != null && crystalAura2.isEnabled() && crystalAura2.isMatrixBypassEnabled();
            }
        }
    }

    private int collectEyePositions(Vec3[] vector, double[] var2) {
        Vec3 eyePosition = mc.player.getEyePosition();
        vector[0] = eyePosition;
        Vec3 position2 = mc.player.position();
        int var5 = 1;
        for (double var9 : var2) {
            if (!(Math.abs(position2.y + var9 - eyePosition.y) < 1.0E-4)) {
                vector[var5++] = new Vec3(position2.x, position2.y + var9, position2.z);
            }
        }
        return var5;
    }

    private BlockHitResult raycastCurrentBlock(BlockPos blockPos, Direction direction) {
        if (mc.player != null && mc.level != null && blockPos != null) {
            Vec3 eyePosition = mc.player.getEyePosition();
            Vec3 vector = RaycastUtils.directionVector(mc.player.getXRot(), mc.player.getYRot()).scale(6.0);
            BlockHitResult blockHitResult = this.raycastSkippingInsideBlocks(eyePosition, eyePosition.add(vector));
            if (blockHitResult.getType() != Type.BLOCK) {
                return null;
            } else if (!blockHitResult.getBlockPos().equals(blockPos)) {
                return null;
            } else {
                return direction != null && blockHitResult.getDirection() != direction ? null : blockHitResult;
            }
        } else {
            return null;
        }
    }

    public static boolean isLookingAtBlock(BlockPos blockPos) {
        return INSTANCE.raycastCurrentBlock(blockPos, null) != null;
    }

    public static boolean isBlockCenterVisible(BlockPos blockPos, double var1) {
        if (mc.player != null && mc.level != null && blockPos != null) {
            Vec3 eyePosition = mc.player.getEyePosition();
            Vec3 vector = Vec3.atCenterOf(blockPos);
            if (eyePosition.distanceToSqr(vector) > var1 * var1) {
                return false;
            }
            BlockHitResult blockHitResult = INSTANCE.raycastSkippingInsideBlocks(eyePosition, vector);
            return blockHitResult.getType() == Type.BLOCK && blockHitResult.getBlockPos().equals(blockPos);
        } else {
            return false;
        }
    }

    public static boolean isAnyBlockFaceVisible(BlockPos blockPos, double var1) {
        if (mc.player != null && mc.level != null && blockPos != null) {
            Vec3 eyePosition = mc.player.getEyePosition();
            double var4 = var1 * var1;
            for (Direction direction : Direction.values()) {
                Vec3 vector = new Vec3(blockPos.getX() + 0.5 + direction.getStepX() * 0.5, blockPos.getY() + 0.5 + direction.getStepY() * 0.5, blockPos.getZ() + 0.5 + direction.getStepZ() * 0.5);
                if (!(eyePosition.distanceToSqr(vector) > var4)) {
                    BlockHitResult blockHitResult = INSTANCE.raycastSkippingInsideBlocks(eyePosition, vector);
                    if (blockHitResult.getType() == Type.BLOCK && blockHitResult.getBlockPos().equals(blockPos)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static boolean isLookingAtFace(BlockPos blockPos, Direction direction) {
        return INSTANCE.raycastCurrentBlock(blockPos, direction) != null;
    }

    public static String describeBlockRaycast(BlockPos blockPos, Direction direction) {
        if (mc.player != null && mc.level != null) {
            Vec3 eyePosition = mc.player.getEyePosition();
            Vec3 vector = RaycastUtils.directionVector(mc.player.getXRot(), mc.player.getYRot()).scale(6.0);
            BlockHitResult blockHitResult = INSTANCE.raycastSkippingInsideBlocks(eyePosition, eyePosition.add(vector));
            String var5 = blockHitResult.getType() != Type.BLOCK ? "воздух" : blockHitResult.getBlockPos().toShortString() + "/" + blockHitResult.getDirection();
            return "лучВ[" + var5 + "] надо[" + blockPos.toShortString() + "/" + direction + "]";
        } else {
            return "нетИгрока";
        }
    }

    private BlockHitResult raycastFromEyePositions(BlockPos blockPos, Direction direction) {
        if (mc.player != null && mc.level != null && blockPos != null) {
            Vec3 vector = RaycastUtils.directionVector(mc.player.getXRot(), mc.player.getYRot()).scale(6.0);
            int var4 = this.collectEyePositions(this.placementEyePositions, CURRENT_EYE_ONLY);
            for (int var5 = 0; var5 < var4; var5++) {
                Vec3 vector2 = this.placementEyePositions[var5];
                BlockHitResult blockHitResult = this.raycastSkippingInsideBlocks(vector2, vector2.add(vector));
                if (blockHitResult.getType() == Type.BLOCK && blockHitResult.getBlockPos().equals(blockPos) && (direction == null || blockHitResult.getDirection() == direction)) {
                    return blockHitResult;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private Vector2f findRotationToFacePoint(BlockPos blockPos, Direction direction, Vec3 vector) {
        if (mc.player != null && mc.level != null && blockPos != null && vector != null) {
            int var4 = this.collectEyePositions(this.faceEyePositions, CURRENT_EYE_ONLY);
            for (int var5 = 0; var5 < var4; var5++) {
                Vec3 vector2 = this.faceEyePositions[var5];
                Vector2f vector3 = quantizeRotation(PlayerUtils.rotationTo(vector2, vector));
                Vec3 vector4 = RaycastUtils.directionVector(vector3.y, vector3.x);
                BlockHitResult blockHitResult = this.raycastSkippingInsideBlocks(vector2, vector2.add(vector4.scale(6.0)));
                if (blockHitResult.getType() == Type.BLOCK && blockHitResult.getBlockPos().equals(blockPos) && (direction == null || blockHitResult.getDirection() == direction)) {
                    return vector3;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private BlockPlacementManager.PlacementTarget findPlacementTarget(BlockPos blockPos, double var2, boolean var4) {
        if (mc.player != null && mc.level != null && blockPos != null) {
            Vec3 eyePosition = mc.player.getEyePosition();
            int var6 = this.collectEyePositions(this.placementEyePositions, CURRENT_EYE_ONLY);
            BlockPlacementManager.PlacementTarget var7 = null;
            BlockPlacementManager.PlacementTarget var8 = null;
            double MAX_VALUE2 = Double.MAX_VALUE;
            double MAX_VALUE3 = Double.MAX_VALUE;
            for (Direction direction : Direction.values()) {
                BlockPos blockPos2 = blockPos.relative(direction.getOpposite());
                BlockState blockState = mc.level.getBlockState(blockPos2);
                if (!blockState.canBeReplaced() && ExplosionDamageUtils.isFaceFull(blockState, blockPos2, direction)) {
                    Vec3 vector = this.faceCenter(blockPos2, direction);
                    double distanceSquared = eyePosition.distanceToSqr(vector);
                    if (!(distanceSquared > var2 * var2)) {
                        BlockPlacementManager.PlacementTarget var22 = distanceSquared < MAX_VALUE2 ? this.samplePlacementFace(blockPos2, direction, var6, var2) : null;
                        if (var22 != null) {
                            MAX_VALUE2 = distanceSquared;
                            var7 = var22;
                        } else if (distanceSquared < MAX_VALUE3 && var4 && ExplosionDamageUtils.isFacingSide(eyePosition, vector, direction)) {
                            MAX_VALUE3 = distanceSquared;
                            var8 = new BlockPlacementManager.PlacementTarget(quantizeRotation(PlayerUtils.rotationTo(eyePosition, vector)), blockPos2, direction, vector);
                        }
                    }
                }
            }
            return var7 != null ? var7 : var8;
        } else {
            return null;
        }
    }

    private BlockPlacementManager.PlacementTarget samplePlacementFace(BlockPos blockPos, Direction direction, int var3, double var4) {
        Vec3 eyePosition = mc.player.getEyePosition();
        for (double var10 : FACE_SAMPLE_OFFSETS) {
            for (double var15 : FACE_SAMPLE_OFFSETS) {
                Vec3 vector = this.randomizedFacePoint(blockPos, direction, var10, var15);
                if (!(eyePosition.distanceToSqr(vector) > var4 * var4)) {
                    for (int var18 = 0; var18 < var3; var18++) {
                        Vec3 vector2 = this.placementEyePositions[var18];
                        Vector2f vector3 = quantizeRotation(PlayerUtils.rotationTo(vector2, vector));
                        Vec3 vector4 = RaycastUtils.directionVector(vector3.y, vector3.x);
                        BlockHitResult blockHitResult = this.raycastSkippingInsideBlocks(vector2, vector2.add(vector4.scale(6.0)));
                        if (blockHitResult.getType() == Type.BLOCK && blockHitResult.getBlockPos().equals(blockPos) && blockHitResult.getDirection() == direction) {
                            return new BlockPlacementManager.PlacementTarget(vector3, blockPos, direction, blockHitResult.getLocation());
                        }
                    }
                }
            }
        }
        return null;
    }

    private Vector2f sampleFaceRotation(BlockPos blockPos, Direction direction, double var3) {
        Vec3 eyePosition = mc.player.getEyePosition();
        for (double var9 : FACE_SAMPLE_OFFSETS) {
            for (double var14 : FACE_SAMPLE_OFFSETS) {
                Vec3 vector = this.randomizedFacePoint(blockPos, direction, var9, var14);
                if (!(eyePosition.distanceToSqr(vector) > var3 * var3)) {
                    Vector2f vector2 = this.findRotationToFacePoint(blockPos, direction, vector);
                    if (vector2 != null) {
                        return vector2;
                    }
                }
            }
        }
        return null;
    }

    private Vector2f findBlockRotation(BlockPos blockPos, double var2, boolean var4) {
        if (mc.player != null && mc.level != null && blockPos != null) {
            Vec3 eyePosition = mc.player.getEyePosition();
            Vector2f vector = null;
            Vector2f vector2 = null;
            double MAX_VALUE2 = Double.MAX_VALUE;
            double MAX_VALUE3 = Double.MAX_VALUE;
            for (Direction direction : Direction.values()) {
                Vec3 vector3 = this.faceCenter(blockPos, direction);
                double distanceSquared = eyePosition.distanceToSqr(vector3);
                if (!(distanceSquared > var2 * var2)) {
                    Vector2f vector4 = distanceSquared < MAX_VALUE2 ? this.sampleFaceRotation(blockPos, direction, var2) : null;
                    if (vector4 != null) {
                        MAX_VALUE2 = distanceSquared;
                        vector = vector4;
                    } else if (distanceSquared < MAX_VALUE3 && var4 && ExplosionDamageUtils.isFacingSide(eyePosition, vector3, direction)) {
                        MAX_VALUE3 = distanceSquared;
                        vector2 = quantizeRotation(PlayerUtils.rotationTo(eyePosition, vector3));
                    }
                }
            }
            return vector != null ? vector : vector2;
        } else {
            return null;
        }
    }

    private Vec3 faceCenter(BlockPos blockPos, Direction direction) {
        return this.randomizedFacePoint(blockPos, direction, 0.5, 0.5);
    }

    private Vec3 randomizedFacePoint(BlockPos blockPos, Direction direction, double var3, double var5) {
        Axis axis = direction.getAxis();
        double var8 = blockPos.getX() + 0.5 + direction.getStepX() * 0.5;
        double var10 = blockPos.getY() + 0.5 + direction.getStepY() * 0.5;
        double var12 = blockPos.getZ() + 0.5 + direction.getStepZ() * 0.5;
        if (axis != Axis.X) {
            var8 += jitteredFaceOffset(var3);
        }
        if (axis != Axis.Y) {
            var10 += jitteredFaceOffset(axis == Axis.X ? var3 : var5);
        }
        if (axis != Axis.Z) {
            var12 += jitteredFaceOffset(var5);
        }
        return new Vec3(var8, var10, var12);
    }

    private static double jitteredFaceOffset(double var0) {
        float var2 = (float) Math.min(0.2F, 0.5 - Math.abs(var0 - 0.5) - 0.05);
        return var0 - 0.5 + GuiHitTestUtils.randomBetween(-var2, var2);
    }

    private Vec3 sampleBlockAim(BlockPos blockPos) {
        Vec3 eyePosition = mc.player.getEyePosition();
        Vec3 vector = Vec3.atCenterOf(blockPos);
        Vec3 vector2 = vector.subtract(eyePosition);
        double count = vector2.length();
        if (count < 1.0E-4) {
            return vector;
        }
        vector2 = vector2.scale(1.0 / count);
        Vec3 vector3 = vector2.cross(WORLD_UP);
        if (vector3.lengthSqr() < 1.0E-6) {
            vector3 = new Vec3(1.0, 0.0, 0.0);
        }
        vector3 = vector3.normalize();
        Vec3 vector4 = vector3.cross(vector2).normalize();
        double var9 = Math.min(0.36, Math.tan(Math.toRadians(3.0)) * count);
        double var11 = GuiHitTestUtils.randomBetween(0.0F, (float) (Math.PI * 2));
        return vector.add(vector3.scale(Math.cos(var11) * var9)).add(vector4.scale(Math.sin(var11) * var9));
    }

    private boolean visibleFromAnyEyePosition(Vec3 vector) {
        int var2 = this.collectEyePositions(this.entityEyePositions, PLAYER_EYE_HEIGHTS);
        for (int var3 = 0; var3 < var2; var3++) {
            if (this.isRayClear(this.entityEyePositions[var3], vector)) {
                return true;
            }
        }
        return false;
    }

    private boolean raycastEntity(Entity entity, float var2, float var3, double var4) {
        int var6 = this.collectEyePositions(this.entityEyePositions, PLAYER_EYE_HEIGHTS);
        for (int var7 = 0; var7 < var6; var7++) {
            if (RaycastUtils.isLookingAt(this.entityEyePositions[var7], var2, var3, var4, true, entity.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    private Vector2f findEntityRotation(Entity entity, Vec3 vector, double var3) {
        if (mc.player != null && mc.level != null && entity != null && vector != null) {
            int var5 = this.collectEyePositions(this.entityEyePositions, PLAYER_EYE_HEIGHTS);
            for (int var6 = 0; var6 < var5; var6++) {
                Vec3 vector2 = this.entityEyePositions[var6];
                Vector2f vector3 = quantizeRotation(PlayerUtils.rotationTo(vector2, vector));
                if (RaycastUtils.isLookingAt(vector2, vector3.x, vector3.y, var3, true, entity.getBoundingBox())) {
                    return vector3;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private boolean isRayClear(Vec3 vector, Vec3 vector2) {
        return this.raycastSkippingInsideBlocks(vector, vector2).getType() == Type.MISS;
    }

    private BlockHitResult raycastSkippingInsideBlocks(Vec3 vector, Vec3 vector2) {
        BlockHitResult blockHitResult = this.raycastOutline(vector, vector2);
        for (int var4 = 0; var4 < 4 && blockHitResult.getType() == Type.BLOCK && blockHitResult.isInside(); var4++) {
            Vec3 vector3 = ExplosionDamageUtils.clipToBlock(vector, vector2, blockHitResult.getBlockPos());
            if (vector3 == null) {
                break;
            }
            vector = vector3;
            blockHitResult = this.raycastOutline(vector, vector2);
        }
        return blockHitResult;
    }

    private BlockHitResult raycastOutline(Vec3 vector, Vec3 vector2) {
        return mc.level.clip(new ClipContext(vector, vector2, Block.OUTLINE, Fluid.NONE, mc.player));
    }

    private static final class RotationPatternTracker {
        private final float[] yawAccelerationSamples = new float[20];
        private final float[] pitchAccelerationSamples = new float[20];
        private int sampleCount;
        private float previousYaw = Float.NaN;
        private float previousPitch;
        private float previousYawDelta;
        private float previousPitchDelta;
        private int repetitiveRotationCount;
        private int attackTrackingTicks;
        private long lastLargePitchChangeMillis;
        private float attackRotationBudget;

        private static float deviationMagnitude(float[] var0) {
            float var1 = mean(var0);
            float sx = 0.0F;
            for (float sxx : var0) {
                sx += (sxx - var1) * (sxx - var1);
            }
            return (float) Math.sqrt(sx);
        }

        private static float mean(float[] var0) {
            float var1 = 0.0F;
            for (float var5 : var0) {
                var1 += var5;
            }
            return var1 / var0.length;
        }

        private static void appendSample(float[] var0, float var1) {
            System.arraycopy(var0, 1, var0, 0, var0.length - 1);
            var0[var0.length - 1] = var1;
        }

        private RotationPatternTracker() {
            this.lastLargePitchChangeMillis = -4611686018427387904L;
        }

        float selectYawJitter(float var1) {
            if (this.repetitiveRotationCount >= 5 && !Float.isNaN(this.previousYaw)) {
                float var2 = 0.0F;
                float var3 = 0.0F;
                for (float var4 = -12.3F; var4 <= 12.3F; var4 += 24.6F) {
                    float var5 = Math.abs(Mth.wrapDegrees(var1 + var4 - this.previousYaw));
                    float var6 = Math.abs(var5 - this.previousYawDelta);
                    if (var6 > var3) {
                        var3 = var6;
                        var2 = var4;
                    }
                }
                return var3 >= 6.0F ? var2 : 0.0F;
            } else {
                return 0.0F;
            }
        }

        boolean canAttack() {
            return this.attackRotationBudget + 8.0F <= 40.0F;
        }

        void recordAttack() {
            this.attackTrackingTicks = 15;
            long var1 = System.currentTimeMillis() - this.lastLargePitchChangeMillis;
            if (var1 <= 350L) {
                this.attackRotationBudget = this.attackRotationBudget + (8.0F - Math.round((float) var1 / 50.0F));
            } else {
                this.attackRotationBudget = Math.max(0.0F, this.attackRotationBudget - 6.0F);
            }
            if (this.attackRotationBudget > 45.0F) {
                this.attackRotationBudget = Math.max(0.0F, this.attackRotationBudget - 30.0F);
            }
        }

        void recordRotation(float var1, float var2) {
            if (Float.isNaN(this.previousYaw)) {
                this.previousYaw = var1;
                this.previousPitch = var2;
            } else {
                if (this.attackTrackingTicks > 0) {
                    this.attackTrackingTicks--;
                }
                if (var1 != this.previousYaw || var2 != this.previousPitch) {
                    float var3 = Math.abs(Mth.wrapDegrees(var1 - this.previousYaw));
                    float var4 = Math.abs(var2 - this.previousPitch);
                    if (var4 > 20.0F) {
                        this.lastLargePitchChangeMillis = System.currentTimeMillis();
                    }
                    appendSample(this.yawAccelerationSamples, Math.abs(var3 - this.previousYawDelta));
                    appendSample(this.pitchAccelerationSamples, Math.abs(var4 - this.previousPitchDelta));
                    if (this.sampleCount < 20) {
                        this.sampleCount++;
                    }
                    if (this.sampleCount >= 20 && this.attackTrackingTicks > 0) {
                        boolean var5 = var3 < 1.5F;
                        boolean var6 = mean(this.yawAccelerationSamples) < 1.0F || mean(this.pitchAccelerationSamples) < 1.0F && !var5;
                        boolean var7 = deviationMagnitude(this.yawAccelerationSamples) < 5.0F && deviationMagnitude(this.pitchAccelerationSamples) > 5.0F && !var5;
                        this.repetitiveRotationCount = var6 && var7 ? this.repetitiveRotationCount + 1 : Math.max(0, this.repetitiveRotationCount - 1);
                    }
                    this.previousYawDelta = var3;
                    this.previousPitchDelta = var4;
                    this.previousYaw = var1;
                    this.previousPitch = var2;
                }
            }
        }
    }

    public record PlacementTarget(Vector2f rotation, BlockPos neighbor, Direction side, Vec3 aim) {
    }
}
