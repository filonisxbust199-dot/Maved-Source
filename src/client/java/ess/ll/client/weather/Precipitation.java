package ess.ll.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;

public final class Precipitation {
    private static final Identifier identifier = Identifier.fromNamespaceAndPath("ess-best", "textures/weather/drop.png");
    private static final Identifier identifier2 = Identifier.fromNamespaceAndPath("ess-best", "textures/weather/flake.png");
    private static final long CONST_longValue = 374227287185L;
    private static final int CONST_intValue = 22;
    private static final float CONST_floatValue = 7.0F;
    private static final float CONST_floatValue2 = 11.0F;
    private static final float CONST_floatValue3 = 5.0F;
    private static final float CONST_floatValue4 = 6.0F;
    private static final Identifier identifier3 = Identifier.fromNamespaceAndPath("ess-best", "textures/weather/ripple.png");
    private static final float CONST_floatValue5 = 0.075F;
    private static final float CONST_floatValue6 = 1.4F;
    private static final float CONST_floatValue7 = 0.42F;
    private static final float CONST_floatValue8 = 9.5F;
    private static final float CONST_floatValue9 = 1.5F;
    private static final float CONST_floatValue10 = 3.2F;
    private static final float CONST_floatValue11 = 3.2F;
    private static final float CONST_floatValue12 = 1.6F;
    private static final float CONST_floatValue13 = 1.1F;
    private static final float CONST_floatValue14 = 0.5F;
    private static final int CONST_intValue2 = 3;
    private static final int CONST_intValue3 = 3;
    private static final float CONST_floatValue15 = 0.085F;
    private static final float CONST_floatValue16 = 0.17F;
    private static final float CONST_floatValue17 = 0.032F;
    private static final float CONST_floatValue18 = 0.046F;
    private static final float CONST_floatValue19 = 2.5F;
    private static final double CONST_doubleValue = Math.toRadians(18.0);
    private static final double CONST_doubleValue2 = 0.25;
    private static final double CONST_doubleValue3 = Math.cos(Math.toRadians(1.0));
    private static boolean flag;
    private static double doubleValue;
    private static double doubleValue2;
    private static double doubleValue3;
    private static double doubleValue4;
    private static double doubleValue5;
    private static double doubleValue6;
    private static double doubleValue7;

    private Precipitation() {
    }

    public static void renderRain(WeatherBatch weatherBatch, ColumnHeights columnHeights, WeatherChannel weatherChannel, float var3, float var4, double var5, double var7, double var9) {
        updateViewCone(weatherBatch);
        renderRainColumn(weatherBatch, columnHeights, weatherChannel, var3, var4, var5, var7, var9, 11.0F, 5.0F, 6.0F, 22, 7.0F);
    }

    public static void renderSnow(WeatherBatch weatherBatch, ColumnHeights columnHeights, WeatherChannel weatherChannel, float var3, float var4, double var5, double var7, double var9) {
        flag = false;
        renderRainColumn(weatherBatch, columnHeights, weatherChannel, var3, var4, var5, var7, var9, 5.5F, 3.4F, 2.0F, 5, 1.0E9F);
    }

    /**
     * The view cone for culling drops behind the camera: the half diagonal of the screen plus a margin. Zenith also
     * read the frustum corners and skipped the cone under nausea (the view wobbles); the corners give the same
     * angle as the field of view here, so only that is kept.
     */
    private static void updateViewCone(WeatherBatch weatherBatch) {
        flag = false;
        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player2 = instance.player;
        if (player2 != null && !player2.hasEffect(MobEffects.NAUSEA)) {
            double var34 = WeatherFx.tanHalfFov();
            double var35 = WeatherFx.aspect();
            double var31 = Math.atan(var34 * Math.sqrt(1.0 + var35 * var35)) + CONST_doubleValue;
            if (var31 < 1.5607963267948965) {
                doubleValue = Math.cos(var31);
                doubleValue2 = Math.sin(var31);
                flag = true;
            }
        }
    }

    private static boolean isInViewCone(WeatherBatch weatherBatch, double var1, double var3, double var5, double var7) {
        if (!flag) {
            return false;
        } else {
            double var9 = var1 - weatherBatch.getDoubleValue();
            double var11 = var3 - weatherBatch.getDoubleValue2();
            double var13 = var5 - weatherBatch.getDoubleValue3();
            double var15 = var9 * weatherBatch.getDoubleValue10() + var11 * weatherBatch.getDoubleValue11() + var13 * weatherBatch.getDoubleValue12();
            double var17 = var7 + 0.25 + var15 * doubleValue2;
            if (var17 < 0.0) {
                return true;
            } else {
                double var19 = var9 * var9 + var11 * var11 + var13 * var13 - var15 * var15;
                return var19 * doubleValue * doubleValue > var17 * var17;
            }
        }
    }

    private static void renderRainColumn(WeatherBatch weatherBatch, ColumnHeights columnHeights, WeatherChannel weatherChannel, float var3, float var4, double var5, double var7, double var9, float var11, float var12, float var13, int var14, float var15) {
        if (weatherChannel.intValue2 != 0) {
            float var16 = weatherChannel.floatValue18 * var3;
            if (!(var16 <= 0.01F)) {
                boolean var17 = weatherChannel.intValue2 == 2;
                weatherBatch.setLineAlpha(0.0F);
                float var18 = 2.0F * WeatherFx.tanHalfFov() / Math.max(1, Minecraft.getInstance().getWindow().getHeight());
                int var19 = (int) Math.floor(var5);
                int var20 = (int) Math.floor(var9);
                float var21 = (var17 ? 3.2F : 3.2F) * var16;
                float var22 = (var17 ? 1.5F : 9.5F) * Math.max(0.05F, weatherChannel.floatValue19);
                float var23 = 0.72F + 0.28F * (float) Math.sin(var4 * 0.53) + 0.18F * (float) Math.sin(var4 * 1.31 + 2.1);
                float var24 = weatherChannel.floatValue20 * var23;
                float var25 = weatherChannel.floatValue21 * var23;
                float var26 = Math.abs(var24) + Math.abs(var25);
                weatherBatch.useTexture(var17 ? identifier2 : identifier);
                if (var17) {
                    updateCameraBasis(weatherBatch);
                }
                float var27 = var17 ? var12 * 1.6F : var12 * 1.8F;
                float var28 = var17 ? var13 : var13 * 1.35F;
                float var29 = var27 + var28;
                double var30 = var7 - var28;
                double var32 = var7 + var27;
                float var34 = var17 ? 1.1F : 1.6F;
                float var35 = var29 / var11;
                int var36 = Math.min(var14, columnHeights.getIntValue3());
                int var37 = var36 * var36;
                for (int var38 = -var36; var38 <= var36; var38++) {
                    for (int var39 = -var36; var39 <= var36; var39++) {
                        int var40 = var38 * var38 + var39 * var39;
                        if (var40 <= var37) {
                            int var41 = var19 + var38;
                            int var42 = var20 + var39;
                            double var43 = var41 + 0.5 - var5;
                            double var45 = var42 + 0.5 - var9;
                            float var47 = (float) Math.sqrt(var43 * var43 + var45 * var45);
                            if (!isFrontFacing(weatherBatch, var43, var45, var47)) {
                                double var48 = columnHeights.heightmapHeight(var41, var42);
                                if (var48 != Double.NEGATIVE_INFINITY && !(var48 > var32)) {
                                    float var50 = Math.min(1.0F, var47 / var34);
                                    float var51 = 1.0F - smoothstep(var36 - 4.0F, var36, var47);
                                    float var52 = var50 * var51;
                                    if (!(var52 <= 0.02F)) {
                                        long var53 = cellSeed(var41, var42);
                                        float var55 = var21 * var35 / (1.0F + var47 / var15);
                                        int var56 = (int) var55;
                                        if (WeatherBatch.hashFloat(var53, 0, 7) < var55 - var56) {
                                            var56++;
                                        }
                                        for (int var57 = 0; var57 < var56; var57++) {
                                            if (var17) {
                                                emitSnowFlake(weatherBatch, weatherChannel, var53, var57, var48, var4, var22, var3 * var52, var41, var42, var24, var25, var26, var29, var30, var32);
                                            } else {
                                                emitRainDrop(weatherBatch, weatherChannel, var53, var57, var48, var4, var22, var3 * var52, var41, var42, var24, var25, var29, var30, var32, var18, var47);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!var17) {
                    renderSnowColumn(weatherBatch, columnHeights, weatherChannel, var3, var4, var5, var7, var9, var14);
                }
            }
        }
    }

    private static void emitRainDrop(WeatherBatch weatherBatch, WeatherChannel weatherChannel, long var2, int var4, double var5, float var7, float var8, float var9, int var10, int var11, float var12, float var13, float var14, double var15, double var17, float var19, float var20) {
        float var21 = WeatherBatch.hashFloat(var2, var4, 0);
        float var22 = WeatherBatch.hashFloat(var2, var4, 1);
        float var23 = WeatherBatch.hashFloat(var2, var4, 2);
        float var24 = WeatherBatch.hashFloat(var2, var4, 4);
        float var25 = var8 * (0.8F + 0.45F * var24);
        double var26 = mod(var7 * var25 + var23 * var14, var14);
        double var28 = wrapToCamera(-var26, var14, var15);
        if (!(var28 > var17)) {
            double var30 = var28 - var5;
            if (!(var30 < 0.0)) {
                float var32 = var25 * 0.075F * (0.62F + 0.76F * WeatherBatch.hashFloat(var2, var4, 11));
                float var33 = var32 * 0.5F;
                float var34 = Math.max(0.014F + 0.016F * var24, var19 * var20 * 1.4F);
                float var35 = 0.05F + 0.04F * WeatherBatch.hashFloat(var2, var4, 6);
                double var36 = var10 + var21 - var12 * 0.55 * var26;
                double var38 = var11 + var22 - var13 * 0.55 * var26;
                if (!isInViewCone(weatherBatch, var36, var28, var38, var35 * 1.17F + var32 + var34)) {
                    double var40 = Math.sin(var28 * 1.7 + var23 * 6.283) * var35;
                    double var42 = var36 + var40;
                    double var44 = var38 + var40 * 0.6;
                    float var46 = Math.min(1.0F, (float) var30 / 0.45F);
                    float var47 = Math.min(1.0F, (float) (var17 - var28) / 2.5F);
                    float var48 = Math.min(1.0F, var20 / 3.2F);
                    float var49 = var9 * var46 * var47 * var48 * (0.36F + 0.48F * var24 * var24);
                    int var50 = WeatherBatch.fade(weatherChannel.intValue3, var49);
                    float var51 = (WeatherBatch.hashFloat(var2, var4, 12) - 0.5F) * 0.11F;
                    double var52 = var12 * 0.55 + var51;
                    double var54 = var13 * 0.55 + var51 * 0.6;
                    double var56 = Math.sqrt(var52 * var52 + 1.0 + var54 * var54);
                    weatherBatch.addCameraQuad(var42 + var52 / var56 * var33, var28 + var33 / var56, var44 + var54 / var56 * var33, var52, 1.0, var54, var33, var34, 0.0F, 0.0F, 1.0F, 1.0F, var50);
                }
            }
        }
    }

    private static void renderSnowColumn(WeatherBatch weatherBatch, ColumnHeights columnHeights, WeatherChannel weatherChannel, float var3, float var4, double var5, double var7, double var9, int var11) {
        float var12 = weatherChannel.floatValue22 * weatherChannel.floatValue18 * var3;
        if (!(var12 <= 0.02F)) {
            weatherBatch.useTexture(identifier3);
            weatherBatch.setLineAlpha(0.0F);
            int var13 = (int) Math.floor(var5);
            int var14 = (int) Math.floor(var9);
            int var15 = Math.min(9, Math.min(var11, columnHeights.getIntValue3()));
            for (int var16 = -var15; var16 <= var15; var16++) {
                for (int var17 = -var15; var17 <= var15; var17++) {
                    int var18 = var13 + var16;
                    int var19 = var14 + var17;
                    double var20 = columnHeights.heightmapHeight(var18, var19);
                    if (var20 != Double.NEGATIVE_INFINITY && !(Math.abs(var20 - var7) > 8.0)) {
                        float var22 = (float) Math.sqrt(var16 * var16 + var17 * var17);
                        if (!(var22 > var15) && !isFrontFacing(weatherBatch, var16, var17, var22)) {
                            float var23 = (1.0F - smoothstep(var15 - 3.0F, var15, var22)) * Math.min(1.0F, var22 / 1.5F);
                            if (!(var23 <= 0.02F)) {
                                long var24 = cellSeed(var18, var19) ^ 93L;
                                int var26 = var12 > 0.6F ? 2 : 1;
                                for (int var27 = 0; var27 < var26; var27++) {
                                    float var28 = 0.9F + 0.7F * WeatherBatch.hashFloat(var24, var27, 1);
                                    float var29 = (float) mod(var4 + WeatherBatch.hashFloat(var24, var27, 2) * var28, var28);
                                    if (!(var29 > 0.42F)) {
                                        float var30 = var29 / 0.42F;
                                        float var31 = 0.06F + 0.2F * (float) Math.sqrt(var30);
                                        int var32 = WeatherBatch.fade(weatherChannel.intValue3, var12 * var23 * (1.0F - var30) * 0.55F);
                                        if (var32 >>> 24 != 0) {
                                            double var33 = var18 + WeatherBatch.hashFloat(var24, var27, 3);
                                            double var35 = var19 + WeatherBatch.hashFloat(var24, var27, 4);
                                            if (!isInViewCone(weatherBatch, var33, var20 + 0.02, var35, var31 * 1.42F)) {
                                                weatherBatch.addOrientedQuad(var33, var20 + 0.02, var35, 1.0, 0.0, var31, var31, 0.0F, 0.0F, 1.0F, 1.0F, var32);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            weatherBatch.useTexture(identifier);
        }
    }

    private static void emitSnowFlake(WeatherBatch weatherBatch, WeatherChannel weatherChannel, long var2, int var4, double var5, float var7, float var8, float var9, int var10, int var11, float var12, float var13, float var14, float var15, double var16, double var18) {
        float var20 = WeatherBatch.hashFloat(var2, var4, 0);
        float var21 = WeatherBatch.hashFloat(var2, var4, 1);
        float var22 = WeatherBatch.hashFloat(var2, var4, 2);
        boolean var23 = WeatherBatch.hashFloat(var2, var4, 9) < 0.34F;
        float var24 = WeatherBatch.hashFloat(var2, var4, 5);
        float var25 = var24 * var24;
        float var26 = var23 ? var8 * (2.1F + 0.7F * var25) : var8 * (1.25F - 0.5F * var25);
        double var27 = mod(var7 * var26 + var22 * var15, var15);
        double var29 = wrapToCamera(-var27, var15, var16);
        if (!(var29 > var18)) {
            float var31 = var23 ? 0.0F : (0.55F + 0.45F * var25) * 0.22F;
            var29 += Math.sin(var7 * 0.83 + var22 * 6.283F) * var31;
            double var32 = var29 - var5;
            if (!(var32 < 0.0)) {
                float var34 = var23 ? 0.032F + 0.046F * var25 : 0.085F + 0.17F * var25;
                float var35 = (0.2F + 0.5F * var14) * (0.55F + 0.75F * var25) * (var23 ? 0.35F : 1.0F);
                double var36 = var10 + var20 - var12 * 0.55 * var27;
                double var38 = var11 + var21 - var13 * 0.55 * var27;
                float var40 = var35 * 1.3F + var34 * 1.42F;
                if (var14 >= 0.7F) {
                    var40 += 0.75F * (Math.abs(var12) + Math.abs(var13) + 0.2F);
                }
                if (!isInViewCone(weatherBatch, var36, var29, var38, var40)) {
                    float var41 = var22 * 6.283F + var7 * (0.55F + 0.45F * WeatherBatch.hashFloat(var2, var4, 4));
                    double var42 = var36 + Math.sin(var41) * var35;
                    double var44 = var38 + Math.cos(var41 * 0.73F) * var35 * 0.8;
                    float var46 = var22 * 6.283F + var7 * (1.1F + 2.3F * WeatherBatch.hashFloat(var2, var4, 10));
                    float var47 = var23 ? 1.0F : (float) Math.abs(Math.sin(var46));
                    float var48 = var23 ? 1.0F : 0.58F + 0.42F * var47;
                    float var49 = Math.min(1.0F, (float) var32 / 0.35F);
                    float var50 = Math.min(1.0F, (float) (var18 - var29) / 2.5F);
                    float var51 = var9 * var49 * var50 * var48 * (var23 ? 0.62F + 0.28F * var25 : 0.74F + 0.26F * WeatherBatch.hashFloat(var2, var4, 6));
                    int var52 = WeatherBatch.fade(weatherChannel.intValue3, var51);
                    int var53 = var23 ? 3 : (int) (WeatherBatch.hashFloat(var2, var4, 13) * 3.0F) % 3;
                    float var54 = var53 % 2 * 0.5F;
                    float var55 = var53 / 2 * 0.5F;
                    double var56;
                    double var58;
                    double var60;
                    if (var23) {
                        double var62 = Math.sin(var41 * 0.6) * 0.45;
                        var56 = var12 * 0.3 + var62;
                        var58 = 1.0;
                        var60 = var13 * 0.3 + var62 * 0.6;
                    } else {
                        double var69 = var46 * 0.53;
                        double var64 = Math.cos(var69);
                        double var66 = Math.sin(var69);
                        var56 = doubleValue5 * var64 + doubleValue3 * var66;
                        var58 = doubleValue6 * var64;
                        var60 = doubleValue7 * var64 + doubleValue4 * var66;
                    }
                    weatherBatch.addCameraQuad(var42, var29, var44, var56, var58, var60, var34, var34 * var48, var54, var55, var54 + 0.5F, var55 + 0.5F, var52);
                    if (!(var14 < 0.7F) && !(WeatherBatch.hashFloat(var2, var4, 8) > 0.3F)) {
                        float var70 = 0.3F + 0.45F * WeatherBatch.hashFloat(var2, var4, 7);
                        weatherBatch.addLine(var42, var29, var44, var42 + var12 * var70, var29 + var70 * 0.2, var44 + var13 * var70, WeatherBatch.fade(weatherChannel.intValue3, var51 * 0.35F));
                    }
                }
            }
        }
    }

    private static void updateCameraBasis(WeatherBatch weatherBatch) {
        double doubleValue10 = weatherBatch.getDoubleValue10();
        double doubleValue11 = weatherBatch.getDoubleValue11();
        double doubleValue12 = weatherBatch.getDoubleValue12();
        double var7 = Math.sqrt(doubleValue10 * doubleValue10 + doubleValue12 * doubleValue12);
        double var9 = var7 < 1.0E-4 ? 1.0 : doubleValue12 / var7;
        double var11 = var7 < 1.0E-4 ? 0.0 : -doubleValue10 / var7;
        doubleValue3 = var9;
        doubleValue4 = var11;
        doubleValue5 = -var11 * doubleValue11;
        doubleValue6 = var11 * doubleValue10 - var9 * doubleValue12;
        doubleValue7 = var9 * doubleValue11;
    }

    private static double wrapToCamera(double var0, float var2, double var3) {
        return var3 + mod(var0 - var3, var2);
    }

    private static float smoothstep(float var0, float var1, float var2) {
        float var3 = Math.clamp((var2 - var0) / Math.max(var1 - var0, 1.0E-4F), 0.0F, 1.0F);
        return var3 * var3 * (3.0F - 2.0F * var3);
    }

    private static double mod(double var0, double var2) {
        double var4 = var0 - Math.floor(var0 / var2) * var2;
        if (var4 >= var2) {
            var4 -= var2;
        } else if (var4 < 0.0) {
            var4 += var2;
        }
        return var4;
    }

    private static boolean isFrontFacing(WeatherBatch weatherBatch, double var1, double var3, float var5) {
        return var5 < 3.0F ? false : var1 * weatherBatch.getDoubleValue10() + var3 * weatherBatch.getDoubleValue12() < -0.25 * var5;
    }

    private static long cellSeed(int var0, int var1) {
        return 374227287185L ^ var0 * -7046029254386353131L ^ var1 * -4417276706812531889L;
    }
}
