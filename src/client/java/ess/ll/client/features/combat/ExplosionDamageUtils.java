package ess.ll.client.features.combat;

import ess.ll.client.core.CollisionBlockView;
import ess.ll.client.features.world.BlockPlacementManager;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.EnchantmentUtils;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ExplosionDamageUtils implements MinecraftAccess {
    private static final double[] RAY_SAMPLE_OFFSETS = new double[] { 0.5, 0.05, 0.95 };
    private static final CollisionContext EMPTY_CONTEXT = CollisionContext.empty();
    private static final double[] FACE_SAMPLE_OFFSETS = new double[] { 0.5, 0.1, 0.9, 0.3, 0.7 };
    private static final AABB FULL_BLOCK = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final Int2IntOpenHashMap protectionCache = new Int2IntOpenHashMap();
    private static int protectionCacheTick = Integer.MIN_VALUE;
    private static final Map<ExplosionDamageUtils.ExposureKey, Float> exposureCache = new HashMap<>();
    private static int exposureCacheTick = Integer.MIN_VALUE;
    private static boolean ignorePlayerProtection;

    public static Vec3 crystalAimPoint(Entity entity, Vec3 vector, float var2) {
        return crystalAimPoint(entity, vector, var2, false);
    }

    public static Vec3 jitteredCenter(BlockPos blockPos) {
        return Vec3.atCenterOf(blockPos).add(jitter(), jitter(), jitter());
    }

    public static boolean isMineable(BlockState blockState) {
        return isBreakable(blockState) && !blockState.is(Blocks.SWEET_BERRY_BUSH);
    }

    public static boolean isClearOfFluid(BlockPos blockPos) {
        if (mc.level.getFluidState(blockPos).isEmpty() && mc.level.getFluidState(blockPos.above()).isEmpty()) {
            for (Direction direction : Plane.HORIZONTAL) {
                if (!mc.level.getFluidState(blockPos.relative(direction)).isEmpty()) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    public static float damageTo(Vec3 vector, LivingEntity entity) {
        return entity == null ? 0.0F : damageTo(vector, entity, entity.getBoundingBox());
    }

    public static boolean isFaceFull(BlockState blockState, BlockPos blockPos, Direction direction) {
        return Block.isFaceFull(blockState.getCollisionShape(mc.level, blockPos), direction);
    }

    private static float scaleByDifficulty(float var0) {
        return switch (mc.level.getDifficulty()) {
            case PEACEFUL ->
                0.0F;
            case EASY ->
                Math.min(var0 / 2.0F + 1.0F, var0);
            case NORMAL ->
                var0;
            case HARD ->
                var0 * 1.5F;
        };
    }

    public static Vec3 crystalAimPoint(Entity entity, Vec3 vector, float var2, boolean var3) {
        double var4 = var2 * var2;
        Vec3 vector2 = entity.position().add(jitter(), 0.5 + jitter(), jitter());
        if (vector.distanceToSqr(vector2) <= var4 && canSeePoint(vector2, var3)) {
            return vector2;
        }
        AABB boundingBox = entity.getBoundingBox();
        Vec3 vector3 = null;
        double var9 = Double.MAX_VALUE;
        for (double var14 : RAY_SAMPLE_OFFSETS) {
            double var16 = Mth.lerp(var14 + wideJitter(), boundingBox.minX, boundingBox.maxX);
            for (double var21 : RAY_SAMPLE_OFFSETS) {
                double var23 = Mth.lerp(var21 + wideJitter(), boundingBox.minY, boundingBox.maxY);
                for (double var28 : RAY_SAMPLE_OFFSETS) {
                    Vec3 vector4 = new Vec3(var16, var23, Mth.lerp(var28 + wideJitter(), boundingBox.minZ, boundingBox.maxZ));
                    if (!(vector.distanceToSqr(vector4) > var4)) {
                        double distanceSquared = vector4.distanceToSqr(vector2);
                        if (!(distanceSquared >= var9) && canSeePoint(vector4, var3)) {
                            var9 = distanceSquared;
                            vector3 = vector4;
                        }
                    }
                }
            }
        }
        return vector3;
    }

    static {
        protectionCache.defaultReturnValue(-1);
    }

    private static int blastProtectionLevel(LivingEntity entity) {
        int var1 = mc.player == null ? Integer.MIN_VALUE : mc.player.tickCount;
        if (var1 != protectionCacheTick) {
            protectionCache.clear();
            protectionCacheTick = var1;
        }
        int var2 = protectionCache.get(entity.getId());
        if (var2 != -1) {
            return var2;
        }
        int var3 = 0;
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack itemBySlot = entity.getItemBySlot(equipmentSlot);
            if (!itemBySlot.isEmpty()) {
                var3 += EnchantmentUtils.getLevel(itemBySlot, Enchantments.PROTECTION);
                var3 += EnchantmentUtils.getLevel(itemBySlot, Enchantments.BLAST_PROTECTION) * 2;
            }
        }
        var3 = Mth.clamp(var3, 0, 20);
        protectionCache.put(entity.getId(), var3);
        return var3;
    }

    public static ExplosionDamageUtils.PlacementFace findFace(BlockPos blockPos, Vec3 vector, float var2, boolean var3, List<BlockPos> items) {
        return searchFace(blockPos, vector, var2, var3, items).face();
    }

    public static boolean isObstructed(BlockPos blockPos) {
        return !mc.level.isUnobstructed(Blocks.OBSIDIAN.defaultBlockState(), blockPos, CollisionContext.empty());
    }

    private static double wideJitter() {
        return GuiHitTestUtils.randomBetween((-0.04F), 0.04F);
    }

    public static boolean isBreakable(BlockState blockState) {
        if (blockState.isAir()) {
            return false;
        } else {
            return blockState.getBlock() instanceof LiquidBlock ? false : blockState.getBlock().defaultDestroyTime() >= 0.0F;
        }
    }

    public static Vec3 crystalBaseTop(BlockPos blockPos) {
        return new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5);
    }

    public static boolean isBaseVisible(BlockPos blockPos, Vec3 vector, float var2, List<BlockPos> items) {
        return crystalVisibility(blockPos, vector, var2, items).visible();
    }

    private ExplosionDamageUtils() {
    }

    public static BlockPos occluderOf(BlockPos blockPos, Vec3 vector, float var2) {
        ExplosionDamageUtils.BlockAim var3 = findAim(blockPos, vector, var2, List.of());
        return var3.aim() != null ? null : var3.occluder();
    }

    public static boolean isBlockingEntity(Entity entity) {
        return entity.isAlive() && !entity.isSpectator();
    }

    public static double distanceSquaredToBlock(Vec3 vector, BlockPos blockPos) {
        return distanceSquaredTo(vector, new AABB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1.0, blockPos.getY() + 1.0, blockPos.getZ() + 1.0));
    }

    public static boolean canPlaceCrystal(BlockPos blockPos, AABB bounds) {
        return isBlastResistant(mc.level.getBlockState(blockPos.below())) && !bounds.intersects(crystalBounds(blockPos));
    }

    public static Vec3 clipToBlock(Vec3 vector, Vec3 vector2, BlockPos blockPos) {
        Vec3 vector3 = vector2.subtract(vector);
        double POSITIVE_INFINITY2 = Double.POSITIVE_INFINITY;
        POSITIVE_INFINITY2 = slabEntry(POSITIVE_INFINITY2, vector3.x, blockPos.getX() - vector.x, blockPos.getX() + 1.0 - vector.x);
        POSITIVE_INFINITY2 = slabEntry(POSITIVE_INFINITY2, vector3.y, blockPos.getY() - vector.y, blockPos.getY() + 1.0 - vector.y);
        POSITIVE_INFINITY2 = slabEntry(POSITIVE_INFINITY2, vector3.z, blockPos.getZ() - vector.z, blockPos.getZ() + 1.0 - vector.z);
        if (!Double.isFinite(POSITIVE_INFINITY2)) {
            return null;
        }
        double var6 = POSITIVE_INFINITY2 + 1.0E-4;
        return var6 >= 1.0 ? null : vector.add(vector3.scale(var6));
    }

    public static Vec3 aimAtBlock(BlockPos blockPos, Vec3 vector, float var2) {
        return findAim(blockPos, vector, var2, List.of()).aim();
    }

    public static ExplosionDamageUtils.PlacementFace findFace(BlockPos blockPos, Vec3 vector, float var2, boolean var3) {
        return findFace(blockPos, vector, var2, var3, List.of());
    }

    private static ExplosionDamageUtils.FaceSearchResult searchFace(BlockPos blockPos, Vec3 vector, float var2, boolean var3, List<BlockPos> items) {
        ExplosionDamageUtils.PlacementFace var5 = null;
        double var6 = Double.MAX_VALUE;
        BlockPos blockPos2 = null;
        double var9 = Double.MAX_VALUE;
        label71: for (Direction direction : Direction.values()) {
            BlockPos blockPos3 = blockPos.relative(direction.getOpposite());
            if (!items.contains(blockPos3)) {
                BlockState blockState = mc.level.getBlockState(blockPos3);
                if (!blockState.canBeReplaced() && isFaceFull(blockState, blockPos3, direction)) {
                    VoxelShape collisionShape = blockState.getCollisionShape(mc.level, blockPos3);
                    AABB bounds2 = collisionShape.isEmpty() ? FULL_BLOCK : collisionShape.bounds();
                    for (double var22 : FACE_SAMPLE_OFFSETS) {
                        for (double var27 : FACE_SAMPLE_OFFSETS) {
                            Vec3 vector2 = facePoint(bounds2, blockPos3, direction, var22, var27);
                            if (isFacingSide(vector, vector2, direction)) {
                                double distanceSquared = vector.distanceToSqr(vector2);
                                if (!(distanceSquared > var2 * var2) && !(distanceSquared >= var6)) {
                                    BlockPos blockPos4 = var3 ? null : firstOccluder(vector, vector2, blockPos3, items);
                                    if (blockPos4 == null) {
                                        var6 = distanceSquared;
                                        var5 = new ExplosionDamageUtils.PlacementFace(blockPos3, direction, vector2);
                                        continue label71;
                                    }
                                    if (distanceSquared < var9) {
                                        var9 = distanceSquared;
                                        blockPos2 = blockPos4;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new ExplosionDamageUtils.FaceSearchResult(var5, var5 == null ? blockPos2 : null);
    }

    public static BlockPos faceOccluder(BlockPos blockPos, Vec3 vector, float var2, boolean var3) {
        return searchFace(blockPos, vector, var2, var3, List.of()).occluder();
    }

    public static boolean isUnsupportedFalling(BlockPos blockPos, BlockState blockState) {
        return mc.level != null && blockState.getBlock() instanceof FallingBlock ? mc.level.getBlockState(blockPos.below()).canBeReplaced() : false;
    }

    private static BlockPos firstOccluder(Vec3 vector, Vec3 vector2, BlockPos blockPos, List<BlockPos> items) {
        Vec3 vector3 = vector;
        BlockPos blockPos2 = null;
        for (int var6 = 0; var6 <= items.size(); var6++) {
            blockPos2 = CollisionBlockView.INSTANCE.rayBlock(vector3, vector2, EMPTY_CONTEXT, var6 == 0);
            if (blockPos2 == null || blockPos2.equals(blockPos)) {
                return null;
            }
            if (!items.contains(blockPos2)) {
                return blockPos2;
            }
            Vec3 vector4 = clipToBlock(vector3, vector2, blockPos2);
            if (vector4 == null) {
                return null;
            }
            vector3 = vector4;
        }
        return blockPos2;
    }

    public static float explosionDamage(Vec3 vector, LivingEntity entity, AABB bounds, float var3) {
        return explosionDamage(vector, entity, bounds, var3, 12.0);
    }

    private static Vec3 facePoint(AABB bounds, BlockPos blockPos, Direction direction, double var3, double var5) {
        double var7 = blockPos.getX() + Mth.lerp(direction.getAxis() == Axis.X ? 0.5 : var3, bounds.minX, bounds.maxX);
        double var9 = blockPos.getY() + Mth.lerp(direction.getAxis() == Axis.Y ? 0.5 : (direction.getAxis() == Axis.X ? var3 : var5), bounds.minY, bounds.maxY);
        double var11 = blockPos.getZ() + Mth.lerp(direction.getAxis() == Axis.Z ? 0.5 : var5, bounds.minZ, bounds.maxZ);
        switch (direction) {
            case UP:
                var9 = blockPos.getY() + bounds.maxY;
                break;
            case DOWN:
                var9 = blockPos.getY() + bounds.minY;
                break;
            case NORTH:
                var11 = blockPos.getZ() + bounds.minZ;
                break;
            case SOUTH:
                var11 = blockPos.getZ() + bounds.maxZ;
                break;
            case WEST:
                var7 = blockPos.getX() + bounds.minX;
                break;
            case EAST:
                var7 = blockPos.getX() + bounds.maxX;
        }
        return jitterOnFace(new Vec3(var7, var9, var11), direction);
    }

    private static float applyResistance(LivingEntity entity, float var1) {
        MobEffectInstance effect = entity.getEffect(MobEffects.RESISTANCE);
        if (effect == null) {
            return var1;
        }
        float var3 = Mth.clamp((effect.getAmplifier() + 1) * 0.2F, 0.0F, 1.0F);
        return var1 * (1.0F - var3);
    }

    public static boolean hasEntities(BlockPos blockPos) {
        return !mc.level.getEntitiesOfClass(Entity.class, crystalBounds(blockPos), ExplosionDamageUtils::isBlockingEntity).isEmpty();
    }

    public static double distanceSquaredToCrystal(Vec3 vector, Vec3 vector2) {
        return distanceSquaredTo(vector, new AABB(vector2.x - 1.0, vector2.y, vector2.z - 1.0, vector2.x + 1.0, vector2.y + 2.0, vector2.z + 1.0));
    }

    public static Vec3 jitterOnFace(Vec3 vector, Direction direction) {
        Axis axis = direction.getAxis();
        return new Vec3(axis == Axis.X ? vector.x : vector.x + jitter(), axis == Axis.Y ? vector.y : vector.y + jitter(), axis == Axis.Z ? vector.z : vector.z + jitter());
    }

    public static AABB crystalBounds(BlockPos blockPos) {
        return new AABB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1.0, blockPos.getY() + 2.0, blockPos.getZ() + 1.0);
    }

    private static float applyArmor(LivingEntity entity, float var1) {
        float armorValue = entity.getArmorValue();
        if (armorValue <= 0.0F) {
            return var1;
        }
        float attributeValue = (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float var4 = 2.0F + attributeValue / 4.0F;
        float var5 = Mth.clamp(armorValue - var1 / var4, armorValue * 0.2F, 20.0F);
        return var1 * (1.0F - var5 / 25.0F);
    }

    public static ExplosionDamageUtils.Visibility crystalVisibility(BlockPos blockPos, Vec3 vector, float var2, List<BlockPos> items) {
        double var4 = var2 * var2;
        AABB bounds = crystalHitBox(blockPos).deflate(0.05);
        var var7 = BlockPlacementManager.eyePositions();
        BlockPos blockPos2 = null;
        double var9 = Double.MAX_VALUE;
        for (double var14 : RAY_SAMPLE_OFFSETS) {
            double var16 = Mth.lerp(var14, bounds.minX, bounds.maxX);
            for (double var21 : RAY_SAMPLE_OFFSETS) {
                double var23 = Mth.lerp(var21, bounds.minY, bounds.maxY);
                for (double var28 : RAY_SAMPLE_OFFSETS) {
                    Vec3 vector2 = new Vec3(var16, var23, Mth.lerp(var28, bounds.minZ, bounds.maxZ));
                    double distanceSquared = vector.distanceToSqr(vector2);
                    if (!(distanceSquared > var4)) {
                        for (int var33 = 0; var33 < var7.size(); var33++) {
                            BlockPos blockPos3 = firstOccluder((Vec3) var7.get(var33), vector2, null, items);
                            if (blockPos3 == null) {
                                return new ExplosionDamageUtils.Visibility(true, null);
                            }
                            if (distanceSquared < var9) {
                                var9 = distanceSquared;
                                blockPos2 = blockPos3;
                            }
                        }
                    }
                }
            }
        }
        return new ExplosionDamageUtils.Visibility(false, blockPos2);
    }

    public static float maxDamage(Vec3 vector, List<? extends LivingEntity> items) {
        if (items != null && !items.isEmpty()) {
            float var2 = 0.0F;
            for (LivingEntity entity : items) {
                if (entity != null && entity != mc.player) {
                    var2 = Math.max(var2, damageTo(vector, entity, entity.getBoundingBox()));
                }
            }
            return var2;
        } else {
            return 0.0F;
        }
    }

    public static AABB crystalHitBox(BlockPos blockPos) {
        return new AABB(blockPos.getX() - 0.5, blockPos.getY() + 1.0, blockPos.getZ() - 0.5, blockPos.getX() + 1.5, blockPos.getY() + 3.0, blockPos.getZ() + 1.5);
    }

    private static float rawExplosionDamage(Vec3 vector, Entity entity, AABB bounds, float var3, double var4) {
        double distance = bottomCenter(bounds).distanceTo(vector);
        if (distance > var4) {
            return 0.0F;
        }
        double var8 = var3 >= 0.0F ? var3 : cachedExposure(vector, bounds, entity);
        double var10 = (1.0 - distance / var4) * var8;
        return (float) ((var10 * var10 + var10) / 2.0 * 7.0 * var4 + 1.0);
    }

    private static boolean isSegmentClear(Vec3 vector, Vec3 vector2, AABB bounds, List<BlockPos> items) {
        if (bounds != null && bounds.clip(vector, vector2).isPresent()) {
            return false;
        }
        Vec3 vector3 = vector;
        for (int var5 = 0; var5 <= items.size(); var5++) {
            BlockPos blockPos = CollisionBlockView.INSTANCE.rayBlock(vector3, vector2, EMPTY_CONTEXT);
            if (blockPos == null) {
                return true;
            }
            if (!items.contains(blockPos)) {
                return false;
            }
            Vec3 vector4 = clipToBlock(vector3, vector2, blockPos);
            if (vector4 == null) {
                return true;
            }
            vector3 = vector4;
        }
        return false;
    }

    public static float cachedExposure(Vec3 vector, AABB bounds, Entity entity) {
        int var3 = mc.player == null ? Integer.MIN_VALUE : mc.player.tickCount;
        if (var3 != exposureCacheTick) {
            exposureCache.clear();
            exposureCacheTick = var3;
        }
        ExplosionDamageUtils.ExposureKey var4 = new ExplosionDamageUtils.ExposureKey(entity.getId(), (long) Math.floor(vector.x / 1.0), (long) Math.floor(vector.y / 1.0), (long) Math.floor(vector.z / 1.0), bounds);
        Float var5 = exposureCache.get(var4);
        if (var5 != null) {
            return var5;
        }
        float var6 = exposure(vector, bounds, null, List.of());
        exposureCache.put(var4, var6);
        return var6;
    }

    public static Vec3 crystalAimPointOrCenter(Entity entity, Vec3 vector, float var2) {
        Vec3 vector2 = crystalAimPoint(entity, vector, var2);
        return vector2 != null ? vector2 : entity.position().add(jitter(), 0.5 + jitter(), jitter());
    }

    public static float damageWithRemovedBlock(Vec3 vector, Entity entity, BlockPos blockPos) {
        AABB boundingBox = entity.getBoundingBox();
        float var4 = blockPos == null ? -1.0F : exposure(vector, boundingBox, new AABB(blockPos).deflate(1.0E-4), List.of());
        return rawExplosionDamage(vector, entity, boundingBox, var4, 12.0);
    }

    public static boolean isBlastResistant(BlockState blockState) {
        return blockState.is(Blocks.OBSIDIAN) || blockState.is(Blocks.BEDROCK);
    }

    public static boolean hasVisibleAim(BlockPos blockPos, Vec3 vector, float var2, List<BlockPos> items) {
        return findAim(blockPos, vector, var2, items).aim() != null;
    }

    private static float applyBlastProtection(LivingEntity entity, float var1) {
        return var1 * (1.0F - blastProtectionLevel(entity) / 25.0F);
    }

    public static void setIgnorePlayerProtection(boolean var0) {
        ignorePlayerProtection = var0;
    }

    public static Vec3 bottomCenter(AABB bounds) {
        return new Vec3((bounds.minX + bounds.maxX) / 2.0, bounds.minY, (bounds.minZ + bounds.maxZ) / 2.0);
    }

    public static boolean hasVisibleAim(BlockPos blockPos, Vec3 vector, float var2) {
        return hasVisibleAim(blockPos, vector, var2, List.of());
    }

    public static Vec3 clampedAimPoint(BlockPos blockPos, Vec3 vector) {
        return new Vec3(Mth.clamp(vector.x, blockPos.getX() + 0.04, blockPos.getX() + 1.0 - 0.04) + jitter(), Mth.clamp(vector.y, blockPos.getY() + 0.04, blockPos.getY() + 1.0 - 0.04) + jitter(), Mth.clamp(vector.z, blockPos.getZ() + 0.04, blockPos.getZ() + 1.0 - 0.04) + jitter());
    }

    private static double jitter() {
        return GuiHitTestUtils.randomBetween((-0.03F), 0.03F);
    }

    private static boolean canSeePoint(Vec3 vector, boolean var1) {
        return var1 ? BlockPlacementManager.isPointVisible(vector) : GuiHitTestUtils.canSee(vector);
    }

    private static double slabEntry(double var0, double var2, double var4, double var6) {
        if (var2 > 1.0E-8) {
            double var8 = var6 / var2;
            if (var8 >= 0.0 && var8 < var0) {
                return var8;
            }
        } else if (var2 < -1.0E-8) {
            double var10 = var4 / var2;
            if (var10 >= 0.0 && var10 < var0) {
                return var10;
            }
        }
        return var0;
    }

    public static float explosionDamage(Vec3 vector, LivingEntity entity, AABB bounds, float var3, double var4) {
        if (entity == null || bounds == null || mc.level == null) {
            return 0.0F;
        }
        if (entity.isDeadOrDying()) {
            return 0.0F;
        }
        float var6 = rawExplosionDamage(vector, entity, bounds, var3, var4);
        if (var6 <= 0.0F) {
            return 0.0F;
        }
        if (entity instanceof Player && !ignorePlayerProtection) {
            var6 = scaleByDifficulty(var6);
        }
        var6 = applyArmor(entity, var6);
        var6 = applyBlastProtection(entity, var6);
        var6 = applyResistance(entity, var6);
        return Math.max(0.0F, var6);
    }

    private static ExplosionDamageUtils.BlockAim findAim(BlockPos blockPos, Vec3 vector, float var2, List<BlockPos> items) {
        double var4 = var2 * var2;
        BlockPos blockPos2 = null;
        double var7 = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            BlockPos blockPos3 = blockPos.relative(direction);
            if (items.contains(blockPos3) || mc.level.getBlockState(blockPos3).canBeReplaced()) {
                for (double var17 : FACE_SAMPLE_OFFSETS) {
                    for (double var22 : FACE_SAMPLE_OFFSETS) {
                        Vec3 vector2 = facePoint(FULL_BLOCK, blockPos, direction, var17, var22);
                        double distanceSquared = vector.distanceToSqr(vector2);
                        if (!(distanceSquared > var4)) {
                            BlockPos blockPos4 = firstOccluder(vector, vector2, blockPos, items);
                            if (blockPos4 == null) {
                                return new ExplosionDamageUtils.BlockAim(vector2, null);
                            }
                            if (distanceSquared < var7) {
                                var7 = distanceSquared;
                                blockPos2 = blockPos4;
                            }
                        }
                    }
                }
            }
        }
        return new ExplosionDamageUtils.BlockAim(null, blockPos2);
    }

    public static double distanceSquaredTo(Vec3 vector, AABB bounds) {
        return vector.distanceToSqr(Mth.clamp(vector.x, bounds.minX, bounds.maxX), Mth.clamp(vector.y, bounds.minY, bounds.maxY), Mth.clamp(vector.z, bounds.minZ, bounds.maxZ));
    }

    private static float exposure(Vec3 vector, AABB bounds, AABB bounds2, List<BlockPos> items) {
        double var4 = 1.0 / ((bounds.maxX - bounds.minX) * 2.0 + 1.0);
        double var6 = 1.0 / ((bounds.maxY - bounds.minY) * 2.0 + 1.0);
        double var8 = 1.0 / ((bounds.maxZ - bounds.minZ) * 2.0 + 1.0);
        double var10 = (1.0 - Math.floor(1.0 / var4) * var4) / 2.0;
        double var12 = (1.0 - Math.floor(1.0 / var8) * var8) / 2.0;
        if (!(var4 < 0.0) && !(var6 < 0.0) && !(var8 < 0.0)) {
            int var14 = 0;
            int var15 = 0;
            for (double var16 = 0.0; var16 <= 1.0; var16 += var4) {
                for (double var18 = 0.0; var18 <= 1.0; var18 += var6) {
                    for (double var20 = 0.0; var20 <= 1.0; var20 += var8) {
                        Vec3 vector2 = new Vec3(Mth.lerp(var16, bounds.minX, bounds.maxX) + var10, Mth.lerp(var18, bounds.minY, bounds.maxY), Mth.lerp(var20, bounds.minZ, bounds.maxZ) + var12);
                        if (isSegmentClear(vector2, vector, bounds2, items)) {
                            var14++;
                        }
                        var15++;
                    }
                }
            }
            return var15 == 0 ? 0.0F : (float) var14 / var15;
        } else {
            return 0.0F;
        }
    }

    public static ExplosionDamageUtils.PlacementFace findFace(BlockPos blockPos, Vec3 vector, float var2) {
        return findFace(blockPos, vector, var2, false);
    }

    public static float damageTo(Vec3 vector, LivingEntity entity, AABB bounds) {
        return explosionDamage(vector, entity, bounds, -1.0F);
    }

    public static boolean isFacingSide(Vec3 vector, Vec3 vector2, Direction direction) {
        Vec3 vector3 = vector.subtract(vector2);
        double count = vector3.length();
        return count < 1.0E-6 ? false : vector3.dot(new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ())) / count >= 0.2;
    }

    public static float damageWithBlockChanges(Vec3 vector, LivingEntity entity, AABB bounds, BlockPos blockPos, BlockPos blockPos2) {
        if (blockPos == null && blockPos2 == null) {
            return damageTo(vector, entity, bounds);
        }
        AABB bounds2 = blockPos == null ? null : new AABB(blockPos).deflate(1.0E-4);
        List<BlockPos> items = blockPos2 == null ? List.of() : List.of(blockPos2);
        return explosionDamage(vector, entity, bounds, exposure(vector, bounds, bounds2, items));
    }

    private record BlockAim(Vec3 aim, BlockPos occluder) {
    }

    public record Visibility(boolean visible, BlockPos occluder) {
    }

    private record ExposureKey(int entityId, long xBits, long yBits, long zBits, AABB box) {
    }

    public record PlacementFace(BlockPos neighbor, Direction side, Vec3 aim) {
    }

    private record FaceSearchResult(ExplosionDamageUtils.PlacementFace face, BlockPos occluder) {
    }
}
