package ess.ll.client.features.world;

import ess.ll.client.integration.MinecraftAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WorldCollisionUtils implements MinecraftAccess {
    public static List<BlockPos> positionsAround(BlockPos blockPos, float var1, float var2, boolean var3) {
        List<BlockPos> items = new ArrayList<>();
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        int sxxxx = var3 ? y - (int) var2 : y;
        for (int sxxxxx = x - (int) var1; sxxxxx <= x + var1; sxxxxx++) {
            for (int sxxxxxx = z - (int) var1; sxxxxxx <= z + var1; sxxxxxx++) {
                for (int sxxxxxxx = sxxxx; sxxxxxxx <= y + var2; sxxxxxxx++) {
                    items.add(new BlockPos(sxxxxx, sxxxxxxx, sxxxxxx));
                }
            }
        }
        return items;
    }

    public static boolean containsEntity(Class<? extends Entity> var0, AABB bounds) {
        return mc.level == null ? false : !mc.level.getEntitiesOfClass(var0, bounds, var0x -> true).isEmpty();
    }

    public static boolean hasEntityNear(Class<? extends Entity> var0, Vec3 vector, double var2) {
        double var4 = var2 * 2.0;
        return containsEntity(var0, AABB.ofSize(vector, var4, var4, var4));
    }

    public static boolean hasGroundBelowWeb(boolean var0, double var1) {
        if (mc.player != null && mc.level != null && var0) {
            AABB boundingBox = mc.player.getBoundingBox();
            double minY2 = boundingBox.minY;
            BlockPos blockPos = BlockPos.containing(mc.player.getX(), minY2, mc.player.getZ());
            int x = blockPos.getX();
            int z = blockPos.getZ();
            for (int var9 = blockPos.getY() - 1; var9 >= mc.level.getMinY(); var9--) {
                BlockState blockState = mc.level.getBlockState(new BlockPos(x, var9, z));
                if (!blockState.isAir()) {
                    if (blockState.is(Blocks.COBWEB)) {
                        return false;
                    }
                    double var11 = minY2 - (var9 + 1.0);
                    return var11 <= var1;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static boolean containsBlock(AABB bounds, Block block) {
        int var2 = Mth.floor(bounds.minX);
        int var3 = Mth.floor(bounds.minY);
        int var4 = Mth.floor(bounds.minZ);
        int var5 = Mth.floor(bounds.maxX);
        int var6 = Mth.floor(bounds.maxY);
        int var7 = Mth.floor(bounds.maxZ);
        for (int var8 = var2; var8 <= var5; var8++) {
            for (int var9 = var3; var9 <= var6; var9++) {
                for (int var10 = var4; var10 <= var7; var10++) {
                    Block block2 = mc.level.getBlockState(new BlockPos(var8, var9, var10)).getBlock();
                    if (block2 == block) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean containsLava(AABB bounds) {
        return containsFluid(bounds, FluidTags.LAVA);
    }

    public static boolean containsFluid(AABB bounds, TagKey<Fluid> tagKey) {
        if (mc.level == null) {
            return false;
        }
        AABB bounds2 = bounds.deflate(0.001);
        MutableBlockPos blockPos = new MutableBlockPos();
        for (int var4 = Mth.floor(bounds2.minX); var4 < Mth.ceil(bounds2.maxX); var4++) {
            for (int var5 = Mth.floor(bounds2.minY); var5 < Mth.ceil(bounds2.maxY); var5++) {
                for (int var6 = Mth.floor(bounds2.minZ); var6 < Mth.ceil(bounds2.maxZ); var6++) {
                    blockPos.set(var4, var5, var6);
                    FluidState fluidState = mc.level.getFluidState(blockPos);
                    if (fluidState.is(tagKey) && var5 + fluidState.getHeight(mc.level, blockPos) >= bounds2.minY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isPlayerInWeb() {
        return mc.player != null && mc.level != null && containsBlock(mc.player.getBoundingBox().deflate(0.005, 0.0, 0.005), Blocks.COBWEB);
    }

    public static List<BlockPos> positionsBetween(BlockPos blockPos, BlockPos blockPos2) {
        ArrayList<BlockPos> items = new ArrayList<>();
        for (int x = blockPos.getX(); x <= blockPos2.getX(); x++) {
            for (int z = blockPos.getZ(); z <= blockPos2.getZ(); z++) {
                for (int y = blockPos.getY(); y <= blockPos2.getY(); y++) {
                    items.add(new BlockPos(x, y, z));
                }
            }
        }
        return items;
    }

    public static boolean hasEntityNearPlayer(Class<? extends Entity> var0, double var1) {
        return mc.player != null && mc.level != null ? containsEntity(var0, mc.player.getBoundingBox().inflate(var1)) : false;
    }

    public static boolean isStandingOnSlabOrSnow() {
        if (mc.player != null && mc.level != null) {
            double var0 = mc.player.getBoundingBox().minY - 0.03;
            BlockPos blockPos = BlockPos.containing(mc.player.getX(), var0, mc.player.getZ());
            BlockState blockState = mc.level.getBlockState(blockPos);
            return blockState.is(BlockTags.SLABS) || blockState.is(Blocks.SNOW);
        } else {
            return false;
        }
    }

    public static List<BlockPos> positionsAround(BlockPos blockPos, float var1) {
        return positionsAround(blockPos, var1, var1, true);
    }

    public static boolean containsWater(AABB bounds) {
        return containsFluid(bounds, FluidTags.WATER);
    }

    private WorldCollisionUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean containsPartialCollision(AABB bounds) {
        MutableBlockPos blockPos = new MutableBlockPos();
        for (int var2 = Mth.floor(bounds.minX); var2 <= Mth.floor(bounds.maxX); var2++) {
            for (int var3 = Mth.floor(bounds.minY); var3 <= Mth.floor(bounds.maxY); var3++) {
                for (int var4 = Mth.floor(bounds.minZ); var4 <= Mth.floor(bounds.maxZ); var4++) {
                    blockPos.set(var2, var3, var4);
                    BlockState blockState = mc.level.getBlockState(blockPos);
                    if (!blockState.getCollisionShape(mc.level, blockPos).isEmpty() && !blockState.isCollisionShapeFullBlock(mc.level, blockPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasHeadClearance() {
        float var0 = mc.player.getBbWidth() / 2.0F;
        AABB bounds = new AABB(mc.player.getX() - var0, mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ() + var0, mc.player.getX() + var0, mc.player.getY() + mc.player.getBbHeight() + 0.2F, mc.player.getZ() - var0);
        return mc.level.noCollision(mc.player, bounds);
    }

    public static List<BlockPos> positionsAround(BlockPos blockPos, float var1, float var2) {
        return positionsAround(blockPos, var1, var2, true);
    }

    public static boolean isAtWaterSurface() {
        if (mc.player != null && mc.level != null) {
            boolean var0 = mc.player.isInWater() || mc.player.getFluidHeight(FluidTags.WATER) > 0.0;
            return var0 && !mc.player.isUnderWater();
        } else {
            return false;
        }
    }

    public static double waterSurfaceHeight(AABB bounds) {
        if (mc.level == null) {
            return Double.NaN;
        }
        AABB bounds2 = bounds.deflate(0.001).expandTowards(0.0, -1.0, 0.0);
        MutableBlockPos blockPos = new MutableBlockPos();
        double NaN2 = Double.NaN;
        for (int var5 = Mth.floor(bounds2.minX); var5 < Mth.ceil(bounds2.maxX); var5++) {
            for (int var6 = Mth.floor(bounds2.minY); var6 < Mth.ceil(bounds2.maxY); var6++) {
                for (int var7 = Mth.floor(bounds2.minZ); var7 < Mth.ceil(bounds2.maxZ); var7++) {
                    blockPos.set(var5, var6, var7);
                    FluidState fluidState = mc.level.getFluidState(blockPos);
                    if (fluidState.is(FluidTags.WATER)) {
                        double var9 = var6 + fluidState.getHeight(mc.level, blockPos);
                        if (Double.isNaN(NaN2) || var9 > NaN2) {
                            NaN2 = var9;
                        }
                    }
                }
            }
        }
        return NaN2;
    }
}
