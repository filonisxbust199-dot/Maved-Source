package ess.ll.client.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ColumnHeights {
    public static final double CONST_doubleValue = Double.NEGATIVE_INFINITY;
    private static final int CONST_intValue = 12;
    private static final int CONST_intValue2 = 30000000;
    private int intValue;
    private int intValue2;
    private final int intValue3;
    private final int intValue4;
    private final double[] doubleValues;
    private final double[] doubleValues2;
    private final boolean[] flags;
    private final boolean[] flags2;
    private static long longValue;
    private long longValue2;
    private LevelChunk worldChunk;
    private int intValue5;
    private int intValue6;

    private ColumnHeights(int var1, int var2, int var3) {
        this.longValue2 = ++longValue;
        this.intValue = var1;
        this.intValue2 = var2;
        this.intValue3 = var3;
        this.intValue4 = var3 * 2 + 1;
        this.doubleValues = new double[this.intValue4 * this.intValue4];
        this.doubleValues2 = new double[this.intValue4 * this.intValue4];
        this.flags = new boolean[this.intValue4 * this.intValue4];
        this.flags2 = new boolean[this.intValue4 * this.intValue4];
    }

    public static ColumnHeights capture(Level level, int var1, int var2, int var3) {
        ColumnHeights columnHeights = new ColumnHeights(var1, var2, var3);
        columnHeights.refresh(level, var1, var2);
        return columnHeights;
    }

    public void refresh(Level level, int var2, int var3) {
        boolean var4 = this.intValue != var2 || this.intValue2 != var3;
        this.intValue = var2;
        this.intValue2 = var3;
        this.worldChunk = null;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int var6 = -this.intValue3; var6 <= this.intValue3; var6++) {
            for (int var7 = -this.intValue3; var7 <= this.intValue3; var7++) {
                var4 |= this.updateColumn(level, blockPos, var2 + var6, var3 + var7, this.index(var6, var7));
            }
        }
        this.worldChunk = null;
        if (var4) {
            this.longValue2 = ++longValue;
        }
    }

    public void shiftCenter(Level level, int var2, int var3) {
        int var4 = var2 - this.intValue;
        int var5 = var3 - this.intValue2;
        if (var4 != 0 || var5 != 0) {
            if (Math.abs(var4) < this.intValue4 && Math.abs(var5) < this.intValue4) {
                this.intValue = var2;
                this.intValue2 = var3;
                int var6 = Math.max(0, -var4);
                int var7 = Math.min(this.intValue4 - 1, this.intValue4 - 1 - var4);
                int var8 = Math.max(0, -var5);
                int var9 = Math.min(this.intValue4 - 1, this.intValue4 - 1 - var5);
                int var10 = var9 - var8 + 1;
                int var11 = var8 + var5;
                if (var4 >= 0) {
                    for (int var12 = var6; var12 <= var7; var12++) {
                        this.copyRows(var12 + var4, var11, var12, var8, var10);
                    }
                } else {
                    for (int var16 = var7; var16 >= var6; var16--) {
                        this.copyRows(var16 + var4, var11, var16, var8, var10);
                    }
                }
                this.worldChunk = null;
                BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
                for (int var13 = 0; var13 < this.intValue4; var13++) {
                    boolean var14 = var13 >= var6 && var13 <= var7;
                    for (int var15 = 0; var15 < this.intValue4; var15++) {
                        if (!var14 || var15 < var8 || var15 > var9) {
                            this.updateColumn(level, blockPos, var2 + var13 - this.intValue3, var3 + var15 - this.intValue3, var13 * this.intValue4 + var15);
                        }
                    }
                }
                this.worldChunk = null;
            } else {
                this.refresh(level, var2, var3);
            }
        }
    }

    public void refreshColumn(Level level, int var2, int var3) {
        int var4 = var2 - this.intValue;
        int var5 = var3 - this.intValue2;
        if (var4 >= -this.intValue3 && var4 <= this.intValue3 && var5 >= -this.intValue3 && var5 <= this.intValue3) {
            this.worldChunk = null;
            boolean var6 = this.updateColumn(level, new BlockPos.MutableBlockPos(), var2, var3, this.index(var4, var5));
            this.worldChunk = null;
            if (var6) {
                this.longValue2 = ++longValue;
            }
        }
    }

    private void copyRows(int var1, int var2, int var3, int var4, int var5) {
        int var6 = var1 * this.intValue4 + var2;
        int var7 = var3 * this.intValue4 + var4;
        System.arraycopy(this.doubleValues, var6, this.doubleValues, var7, var5);
        System.arraycopy(this.doubleValues2, var6, this.doubleValues2, var7, var5);
        System.arraycopy(this.flags, var6, this.flags, var7, var5);
        System.arraycopy(this.flags2, var6, this.flags2, var7, var5);
    }

    private boolean updateColumn(Level level, BlockPos.MutableBlockPos blockPos, int var3, int var4, int var5) {
        double var6 = this.doubleValues[var5];
        double var8 = this.doubleValues2[var5];
        boolean var10 = this.flags[var5];
        boolean var11 = this.flags2[var5];
        this.sampleColumn(level, blockPos, var3, var4, var5);
        return this.doubleValues[var5] != var6 || this.doubleValues2[var5] != var8 || this.flags[var5] != var10 || this.flags2[var5] != var11;
    }

    private void sampleColumn(Level level, BlockPos.MutableBlockPos blockPos, int var3, int var4, int var5) {
        this.flags[var5] = false;
        this.flags2[var5] = false;
        int var6 = var3 >> 4;
        int var7 = var4 >> 4;
        if (!level.hasChunk(var6, var7)) {
            this.doubleValues[var5] = Double.NEGATIVE_INFINITY;
            this.doubleValues2[var5] = Double.NEGATIVE_INFINITY;
        } else {
            LevelChunk levelChunk = null;
            int var9;
            if (var3 >= -30000000 && var4 >= -30000000 && var3 < 30000000 && var4 < 30000000) {
                if (this.worldChunk == null || this.intValue5 != var6 || this.intValue6 != var7) {
                    this.worldChunk = level.getChunk(var6, var7);
                    this.intValue5 = var6;
                    this.intValue6 = var7;
                }
                levelChunk = this.worldChunk;
                var9 = levelChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, var3 & 15, var4 & 15) + 1;
            } else {
                var9 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, var3, var4);
            }
            int minY = level.getMinY();
            if (var9 <= minY) {
                this.doubleValues[var5] = Double.NEGATIVE_INFINITY;
                this.doubleValues2[var5] = Double.NEGATIVE_INFINITY;
            } else {
                this.doubleValues2[var5] = var9;
                this.doubleValues[var5] = Double.NEGATIVE_INFINITY;
                int var11 = Math.max(minY, var9 - 12);
                for (int var12 = var9 - 1; var12 >= var11; var12--) {
                    blockPos.set(var3, var12, var4);
                    BlockState blockState = levelChunk != null ? levelChunk.getBlockState(blockPos) : level.getBlockState(blockPos);
                    if (!blockState.getFluidState().isEmpty() || blockState.is(BlockTags.ICE)) {
                        this.doubleValues[var5] = var12 + 1;
                        return;
                    }
                    if (blockState.is(BlockTags.LEAVES)) {
                        this.doubleValues[var5] = var12 + 1;
                        this.flags[var5] = true;
                        this.flags2[var5] = true;
                        return;
                    }
                    if (blockState.isRedstoneConductor(level, blockPos)) {
                        this.doubleValues[var5] = var12 + 1;
                        this.flags[var5] = true;
                        return;
                    }
                }
            }
        }
    }

    public static ColumnHeights createPreview(int var0) {
        ColumnHeights columnHeights = new ColumnHeights(0, 0, var0);
        for (int var2 = -var0; var2 <= var0; var2++) {
            for (int var3 = -var0; var3 <= var0; var3++) {
                int var4 = columnHeights.index(var2, var3);
                columnHeights.doubleValues[var4] = previewHeight(var2, var3);
                columnHeights.doubleValues2[var4] = columnHeights.doubleValues[var4];
                columnHeights.flags[var4] = true;
            }
        }
        return columnHeights;
    }

    private static double previewHeight(int var0, int var1) {
        double var2 = 0.0;
        if (var1 <= -2) {
            var2++;
        }
        if (var1 <= -4 && Math.abs(var0) <= 3) {
            var2++;
        }
        if (var0 >= 3 && var1 >= 1) {
            var2++;
        }
        return var2;
    }

    public int getIntValue() {
        return this.intValue;
    }

    public int getIntValue2() {
        return this.intValue2;
    }

    public int getIntValue3() {
        return this.intValue3;
    }

    public long getLongValue2() {
        return this.longValue2;
    }

    public double surfaceHeightAtOffset(int var1, int var2) {
        return var1 >= -this.intValue3 && var1 <= this.intValue3 && var2 >= -this.intValue3 && var2 <= this.intValue3 ? this.doubleValues[this.index(var1, var2)] : Double.NEGATIVE_INFINITY;
    }

    public boolean isLeafAtOffset(int var1, int var2) {
        return var1 >= -this.intValue3 && var1 <= this.intValue3 && var2 >= -this.intValue3 && var2 <= this.intValue3 ? this.flags2[this.index(var1, var2)] : false;
    }

    public boolean supportsSnowAtOffset(int var1, int var2) {
        return var1 >= -this.intValue3 && var1 <= this.intValue3 && var2 >= -this.intValue3 && var2 <= this.intValue3 ? this.flags[this.index(var1, var2)] : false;
    }

    public double surfaceHeight(double var1, double var3) {
        return this.surfaceHeightAtOffset((int) Math.floor(var1) - this.intValue, (int) Math.floor(var3) - this.intValue2);
    }

    public double heightmapHeight(double var1, double var3) {
        return this.heightmapHeight((int) Math.floor(var1), (int) Math.floor(var3));
    }

    public double heightmapHeight(int var1, int var2) {
        int var3 = var1 - this.intValue;
        int var4 = var2 - this.intValue2;
        return var3 >= -this.intValue3 && var3 <= this.intValue3 && var4 >= -this.intValue3 && var4 <= this.intValue3 ? this.doubleValues2[this.index(var3, var4)] : Double.NEGATIVE_INFINITY;
    }

    private int index(int var1, int var2) {
        return (var1 + this.intValue3) * this.intValue4 + var2 + this.intValue3;
    }
}
