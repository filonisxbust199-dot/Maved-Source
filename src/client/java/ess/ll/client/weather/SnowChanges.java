package ess.ll.client.weather;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SnowChanges {
    private static final long CONST_longValue = 15000L;
    private static final int CONST_intValue = 12;
    public static final long CONST_longValue2 = 900L;
    private static final int CONST_intValue2 = 96;
    private static final int CONST_intValue3 = 4096;
    private static final Long2ObjectOpenHashMap<SnowChanges.Entry> long2ObjectOpenHashMap = new Long2ObjectOpenHashMap();
    private static final List<SnowChanges.Data> items = new ArrayList<>();
    private static final LongArrayList longArrayList = new LongArrayList();
    private static long longValue;

    private SnowChanges() {
    }

    public static void onBlockChanged(Level level, BlockPos blockPos, BlockState blockState, BlockState blockState2) {
        boolean var4 = !blockState.isAir();
        boolean var5 = !blockState2.isAir();
        if (var4 != var5) {
            int x = blockPos.getX();
            int z = blockPos.getZ();
            if (level.hasChunk(x >> 4, z >> 4)) {
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                long var9 = System.currentTimeMillis();
                if (var5) {
                    if (height != blockPos.getY() + 1) {
                        return;
                    }
                    SnowChanges.Entry orCreateColumn = getOrCreateColumn(x, z);
                    orCreateColumn.longValue = var9;
                    orCreateColumn.doubleValue = height;
                    longValue++;
                } else {
                    if (height > blockPos.getY()) {
                        return;
                    }
                    float var12 = SnowCover.heightAt(x + 0.5, z + 0.5);
                    if (var12 <= 0.02F) {
                        long2ObjectOpenHashMap.remove(packColumn(x, z));
                        longArrayList.add(packColumn(x, z));
                        return;
                    }
                    long2ObjectOpenHashMap.remove(packColumn(x, z));
                    if (items.size() >= 96) {
                        items.remove(0);
                    }
                    items.add(new SnowChanges.Data(x, z, blockPos.getY() + 1 + var12, height, var9));
                    longValue++;
                }
                longArrayList.add(packColumn(x, z));
            }
        }
    }

    public static void pruneExpired(long var0) {
        if (!items.isEmpty()) {
            items.removeIf(var2 -> var0 - var2.getLongValue() > 900L);
        }
        if (!long2ObjectOpenHashMap.isEmpty()) {
            long2ObjectOpenHashMap.values().removeIf(var2 -> {
                if (var0 - var2.longValue < 15000L) {
                    return false;
                } else {
                    longValue++;
                    return true;
                }
            });
        }
    }

    public static void refreshChangedColumns(Level level, ColumnHeights columnHeights) {
        if (!longArrayList.isEmpty()) {
            for (int var2 = 0; var2 < longArrayList.size(); var2++) {
                long column = longArrayList.getLong(var2);
                columnHeights.refreshColumn(level, (int) (column >> 32), (int) column);
            }
            longArrayList.clear();
        }
    }

    public static List<SnowChanges.Data> getItems() {
        return items;
    }

    public static long animationRevision(long var0) {
        long var2 = longValue;
        ObjectIterator iterator2 = long2ObjectOpenHashMap.values().iterator();
        while (iterator2.hasNext()) {
            SnowChanges.Entry entry = (SnowChanges.Entry) iterator2.next();
            var2 = var2 * 31L + accumulationStep(entry, var0);
        }
        return var2;
    }

    public static float accumulationFactor(int var0, int var1, double var2, long var4) {
        if (long2ObjectOpenHashMap.isEmpty()) {
            return 1.0F;
        } else {
            SnowChanges.Entry entry = (SnowChanges.Entry) long2ObjectOpenHashMap.get(packColumn(var0, var1));
            if (entry == null) {
                return 1.0F;
            } else {
                return Math.abs(entry.doubleValue - var2) > 0.51 ? 1.0F : Math.clamp(accumulationStep(entry, var4) / 12.0F, 0.0F, 1.0F);
            }
        }
    }

    public static boolean hasAccumulatingSnow() {
        return !long2ObjectOpenHashMap.isEmpty();
    }

    public static void clear() {
        long2ObjectOpenHashMap.clear();
        items.clear();
        longArrayList.clear();
        longValue++;
    }

    private static int accumulationStep(SnowChanges.Entry entry, long var1) {
        long var3 = var1 - entry.longValue;
        if (var3 <= 0L) {
            return 0;
        } else if (var3 >= 15000L) {
            return 12;
        } else {
            float var5 = (float) var3 / 15000.0F;
            return (int) (var5 * var5 * 12.0F);
        }
    }

    private static SnowChanges.Entry getOrCreateColumn(int var0, int var1) {
        long var2 = packColumn(var0, var1);
        SnowChanges.Entry entry = (SnowChanges.Entry) long2ObjectOpenHashMap.get(var2);
        if (entry != null) {
            return entry;
        } else {
            if (long2ObjectOpenHashMap.size() >= 4096) {
                long2ObjectOpenHashMap.clear();
            }
            entry = new SnowChanges.Entry();
            long2ObjectOpenHashMap.put(var2, entry);
            return entry;
        }
    }

    private static long packColumn(int var0, int var1) {
        return (long) var0 << 32 | var1 & 4294967295L;
    }

    public static final class Data {
        private final double doubleValue;
        private final double doubleValue2;
        private final double doubleValue3;
        private final double doubleValue4;
        private final long longValue;

        public double doubleValue() {
            return this.doubleValue;
        }

        public double doubleValue2() {
            return this.doubleValue2;
        }

        public double doubleValue3() {
            return this.doubleValue3;
        }

        public double doubleValue4() {
            return this.doubleValue4;
        }

        public long longValue() {
            return this.longValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Data other))
                return false;
            return java.util.Objects.equals(this.doubleValue, other.doubleValue) && java.util.Objects.equals(this.doubleValue2, other.doubleValue2) && java.util.Objects.equals(this.doubleValue3, other.doubleValue3) && java.util.Objects.equals(this.doubleValue4, other.doubleValue4) && java.util.Objects.equals(this.longValue, other.longValue);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(doubleValue, doubleValue2, doubleValue3, doubleValue4, longValue);
        }

        @Override
        public String toString() {
            return "Data[" + "doubleValue=" + this.doubleValue + ", " + "doubleValue2=" + this.doubleValue2 + ", " + "doubleValue3=" + this.doubleValue3 + ", " + "doubleValue4=" + this.doubleValue4 + ", " + "longValue=" + this.longValue + "]";
        }

        public Data(double var1, double var3, double var5, double var7, long var9) {
            this.doubleValue = var1;
            this.doubleValue2 = var3;
            this.doubleValue3 = var5;
            this.doubleValue4 = var7;
            this.longValue = var9;
        }

        public double getDoubleValue() {
            return this.doubleValue;
        }

        public double getDoubleValue2() {
            return this.doubleValue2;
        }

        public double getDoubleValue3() {
            return this.doubleValue3;
        }

        public double getDoubleValue4() {
            return this.doubleValue4;
        }

        public long getLongValue() {
            return this.longValue;
        }
    }

    private static final class Entry {
        private long longValue;
        private double doubleValue;
    }
}
