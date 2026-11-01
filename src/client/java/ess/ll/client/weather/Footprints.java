package ess.ll.client.weather;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class Footprints {
    private static final int CONST_intValue = 128;
    private static final double CONST_doubleValue = 0.62;
    private static final double CONST_doubleValue2 = 0.13;
    private static final double CONST_doubleValue3 = 0.012;
    private static final double CONST_doubleValue4 = 0.006;
    private static final int CONST_intValue2 = 14;
    private static final float CONST_floatValue = 3.5F;
    private static final float CONST_floatValue2 = 0.55F;
    private static final float CONST_floatValue3 = 0.014F;
    private static final Identifier identifier = Identifier.fromNamespaceAndPath("ess-best", "textures/weather/print.png");
    private static final Identifier identifier2 = Identifier.fromNamespaceAndPath("ess-best", "textures/weather/flake.png");
    private static final long CONST_longValue = 420L;
    private static final double CONST_doubleValue5 = 0.105;
    private static final double CONST_doubleValue6 = 0.165;
    private final ArrayDeque<Footprints.Data> arrayDeque = new ArrayDeque<>();
    private final Map<UUID, Footprints.Entry> items = new HashMap<>();

    public void update(Level level, Iterable<? extends Player> iterable, WeatherChannel weatherChannel, long var4) {
        if (weatherChannel.intValue7 > 0 && level != null) {
            for (Player player : iterable) {
                if (!player.isSpectator() && player.onGround()) {
                    double x = player.getX();
                    double z = player.getZ();
                    double y = player.getY();
                    UUID uUID = player.getUUID();
                    Footprints.Entry entry = this.items.get(uUID);
                    if (entry == null) {
                        entry = new Footprints.Entry();
                        entry.doubleValue = x;
                        entry.doubleValue2 = z;
                        this.items.put(uUID, entry);
                    }
                    entry.longValue = var4;
                    double var16 = x - entry.doubleValue;
                    double var18 = z - entry.doubleValue2;
                    if (!(var16 * var16 + var18 * var18 < 0.3844)) {
                        entry.doubleValue = x;
                        entry.doubleValue2 = z;
                        entry.left = !entry.left;
                        float yRot = player.getYRot();
                        double var21 = Math.toRadians(yRot);
                        double var23 = Math.cos(var21);
                        double var25 = Math.sin(var21);
                        double var27 = entry.left ? -0.13 : 0.13;
                        double var29 = x + var23 * var27;
                        double var31 = z + var25 * var27;
                        if (isOnSnow(level, var29, y, var31)) {
                            this.addFootprint(new Footprints.Data(var29, y + 0.012, var31, yRot, entry.left, var4));
                        }
                    }
                }
            }
            var iterator2 = this.items.values().iterator();
            while (iterator2.hasNext()) {
                if (var4 - ((Footprints.Entry) iterator2.next()).longValue > 30000L) {
                    iterator2.remove();
                }
            }
            this.pruneExpired(weatherChannel, var4);
        }
    }

    private static boolean isOnSnow(Level level, double var1, double var3, double var5) {
        BlockPos blockPos = BlockPos.containing(var1, var3 - 0.1, var5);
        if (!level.hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
            return false;
        } else {
            BlockState blockState = level.getBlockState(blockPos);
            return !blockState.isAir() && blockState.isCollisionShapeFullBlock(level, blockPos) && blockState.getFluidState().isEmpty();
        }
    }

    private void addFootprint(Footprints.Data data) {
        if (this.arrayDeque.size() >= 128) {
            this.arrayDeque.pollFirst();
        }
        this.arrayDeque.addLast(data);
    }

    private void pruneExpired(WeatherChannel weatherChannel, long var2) {
        var iterator2 = this.arrayDeque.iterator();
        while (iterator2.hasNext() && var2 - ((Footprints.Data) iterator2.next()).getLongValue() > weatherChannel.intValue7) {
            iterator2.remove();
        }
    }

    public void clear() {
        this.arrayDeque.clear();
        this.items.clear();
    }

    public void render(WeatherBatch weatherBatch, WeatherChannel weatherChannel, float var3, long var4, double var6, double var8, double var10) {
        if (weatherChannel.intValue7 > 0 && !this.arrayDeque.isEmpty()) {
            weatherBatch.useTexture(identifier);
            boolean var12 = false;
            for (Footprints.Data data : this.arrayDeque) {
                float var15 = (float) (var4 - data.getLongValue()) / weatherChannel.intValue7;
                if (!(var15 >= 1.0F)) {
                    double var16 = data.getDoubleValue() - var6;
                    double var18 = data.getDoubleValue3() - var10;
                    double var20 = Math.sqrt(var16 * var16 + var18 * var18);
                    if (!(var20 > 14.0) && !(Math.abs(data.getDoubleValue2() - var8) > 12.0) && (!(var20 > 3.0) || !(var16 * weatherBatch.getDoubleValue10() + var18 * weatherBatch.getDoubleValue12() < -0.25 * var20))) {
                        float var22 = (float) Math.min(1.0, (14.0 - var20) / 3.5);
                        double var23 = weatherChannel.flag ? SnowCover.heightAt(data.getDoubleValue(), data.getDoubleValue3()) + 0.006 : 0.0;
                        emitLayer(weatherBatch, weatherChannel, data.getDoubleValue(), data.getDoubleValue2() + var23, data.getDoubleValue3(), data.getFloatValue() + (data.isFlag() ? -8.0F : 8.0F), var3 * fadeCurve(var15) * var22, var15);
                        var12 |= weatherChannel.flag && var4 - data.getLongValue() < 420L;
                    }
                }
            }
            if (var12) {
                weatherBatch.useTexture(identifier2);
                for (Footprints.Data data2 : this.arrayDeque) {
                    long var27 = var4 - data2.getLongValue();
                    if (var27 < 420L) {
                        renderPrint(weatherBatch, data2, (float) var27 / 420.0F, var3, SnowCover.heightAt(data2.getDoubleValue(), data2.getDoubleValue3()));
                    }
                }
            }
        }
    }

    private static void renderPrint(WeatherBatch weatherBatch, Footprints.Data data, float var2, float var3, float var4) {
        float var5 = (1.0F - var2) * (1.0F - var2);
        int var6 = withAlpha(-591105, (int) (150.0F * var5 * Math.clamp(var3, 0.0F, 1.0F)));
        if (var6 >>> 24 != 0) {
            long var7 = Double.doubleToLongBits(data.getDoubleValue()) ^ Long.rotateLeft(data.getLongValue(), 17);
            for (int var9 = 0; var9 < 5; var9++) {
                float var10 = 0.05F + 0.16F * WeatherBatch.hashFloat(var7, var9, 0);
                double var11 = WeatherBatch.hashFloat(var7, var9, 1) * Math.PI * 2.0;
                double var13 = data.getDoubleValue() + Math.cos(var11) * var10 * (0.4 + var2);
                double var15 = data.getDoubleValue3() + Math.sin(var11) * var10 * (0.4 + var2);
                double var17 = 0.2 * (0.5 + 0.5 * WeatherBatch.hashFloat(var7, var9, 2));
                double var19 = data.getDoubleValue2() + var4 + var17 * (var2 * 2.2 - var2 * var2 * 2.4);
                float var21 = 0.035F + 0.03F * WeatherBatch.hashFloat(var7, var9, 3);
                weatherBatch.addCameraQuad(var13, var19, var15, 0.0, 1.0, 0.0, var21, var21, 0.0F, 0.0F, 1.0F, 1.0F, var6);
            }
        }
    }

    public static void renderSurface(WeatherBatch weatherBatch, WeatherChannel weatherChannel, float var2) {
        if (weatherChannel.intValue7 > 0) {
            weatherBatch.useTexture(identifier);
            for (int var3 = 0; var3 < 6; var3++) {
                boolean var4 = (var3 & 1) == 0;
                double var5 = var4 ? -0.13 : 0.13;
                double var7 = 2.4 - var3 * 0.62;
                double var9 = var7 <= -2.0 ? 1.0 : 0.0;
                float var11 = 0.35F + 0.65F * (var3 / 5.0F);
                emitLayer(weatherBatch, weatherChannel, -0.4 + var5, var9 + 0.012, var7, var4 ? -8.0F : 8.0F, var2 * var11, 1.0F - var11);
            }
        }
    }

    private static void emitLayer(WeatherBatch weatherBatch, WeatherChannel weatherChannel, double var2, double var4, double var6, float var8, float var9, float var10) {
        if (!(var9 <= 0.01F)) {
            double var11 = Math.toRadians(var8);
            double var13 = -Math.sin(var11);
            double var15 = Math.cos(var11);
            int var17 = withAlpha(weatherChannel.intValue6, (int) ((weatherChannel.intValue6 >>> 24 & 0xFF) * Math.clamp(var9, 0.0F, 1.0F)));
            if (weatherChannel.flag) {
                float var18 = Math.clamp(1.0F - var10 * 1.8F, 0.0F, 1.0F) * var9;
                if (var18 > 0.02F) {
                    emitLine(weatherBatch, var2, var4, var6, var13, var15, 1.22, withAlpha(-722689, (int) (70.0F * var18)));
                }
                emitLine(weatherBatch, var2, var4 + 0.001, var6, var13, var15, 1.0 - 0.35 * var10, var17);
            } else {
                emitLine(weatherBatch, var2, var4, var6, var13, var15, 1.0, var17);
            }
        }
    }

    private static void emitLine(WeatherBatch weatherBatch, double var1, double var3, double var5, double var7, double var9, double var11, int var13) {
        weatherBatch.addOrientedQuad(var1, var3, var5, var7, var9, 0.105 * var11, 0.165 * var11, 0.0F, 0.0F, 1.0F, 1.0F, var13);
    }

    private static float fadeCurve(float var0) {
        float var1 = smoothstep(Math.clamp(var0 / 0.014F, 0.0F, 1.0F));
        if (var0 <= 0.55F) {
            return var1;
        } else {
            float var2 = (var0 - 0.55F) / 0.45F;
            return var1 * (1.0F - smoothstep(Math.clamp(var2, 0.0F, 1.0F)));
        }
    }

    private static float smoothstep(float var0) {
        return var0 * var0 * (3.0F - 2.0F * var0);
    }

    private static int withAlpha(int var0, int var1) {
        return Math.clamp((long) var1, 0, 255) << 24 | var0 & 16777215;
    }

    private static final class Data {
        private final double doubleValue;
        private final double doubleValue2;
        private final double doubleValue3;
        private final float floatValue;
        private final boolean flag;
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

        public float floatValue() {
            return this.floatValue;
        }

        public boolean flag() {
            return this.flag;
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
            return java.util.Objects.equals(this.doubleValue, other.doubleValue) && java.util.Objects.equals(this.doubleValue2, other.doubleValue2) && java.util.Objects.equals(this.doubleValue3, other.doubleValue3) && java.util.Objects.equals(this.floatValue, other.floatValue) && java.util.Objects.equals(this.flag, other.flag) && java.util.Objects.equals(this.longValue, other.longValue);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(doubleValue, doubleValue2, doubleValue3, floatValue, flag, longValue);
        }

        @Override
        public String toString() {
            return "Data[" + "doubleValue=" + this.doubleValue + ", " + "doubleValue2=" + this.doubleValue2 + ", " + "doubleValue3=" + this.doubleValue3 + ", " + "floatValue=" + this.floatValue + ", " + "flag=" + this.flag + ", " + "longValue=" + this.longValue + "]";
        }

        private Data(double var1, double var3, double var5, float var7, boolean flag2, long var9) {
            this.doubleValue = var1;
            this.doubleValue2 = var3;
            this.doubleValue3 = var5;
            this.floatValue = var7;
            this.flag = flag2;
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

        public float getFloatValue() {
            return this.floatValue;
        }

        public boolean isFlag() {
            return this.flag;
        }

        public long getLongValue() {
            return this.longValue;
        }
    }

    private static final class Entry {
        private double doubleValue;
        private double doubleValue2;
        private boolean left;
        private long longValue;
    }
}
