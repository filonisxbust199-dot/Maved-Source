package ess.ll.client.render;

import ess.ll.client.integration.MinecraftAccess;
import java.awt.Color;
import net.minecraft.util.Mth;

public final class ColorUtils implements MinecraftAccess {
    public static int WHITE = rgb(255, 255, 255);
    public static int BLACK = rgb(0, 0, 0);

    public static int lerpRounded(int var0, int var1, float var2) {
        float var3 = Mth.clamp(var2, 0.0F, 1.0F);
        return packArgb(Math.round(red(var0) + (red(var1) - red(var0)) * var3), Math.round(green(var0) + (green(var1) - green(var0)) * var3), Math.round(blue(var0) + (blue(var1) - blue(var0)) * var3), Math.round(getAlpha(var0) + (getAlpha(var1) - getAlpha(var0)) * var3));
    }

    public static Color reverseLerp(Color color, Color color2, double var2) {
        float var4 = Mth.clamp((float) (1.0 - var2), 0.0F, 1.0F);
        return new Color(Math.round(color.getRed() + (color2.getRed() - color.getRed()) * var4), Math.round(color.getGreen() + (color2.getGreen() - color.getGreen()) * var4), Math.round(color.getBlue() + (color2.getBlue() - color.getBlue()) * var4), Math.round(color.getAlpha() + (color2.getAlpha() - color.getAlpha()) * var4));
    }

    private ColorUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static int animatedGradient(int var0, int var1, int... var2) {
        if (var2 != null && var2.length != 0) {
            if (var2.length == 1) {
                return var2[0];
            } else if (var2.length == 2) {
                int var3 = (int) ((System.currentTimeMillis() / var0 + var1) % 360L);
                var3 = var3 >= 180 ? 360 - var3 : var3;
                return lerpRounded(var2[0], var2[1], var3 / 180.0F);
            } else {
                return animatedMultiGradient(var0, var1, var2);
            }
        } else {
            return WHITE;
        }
    }

    public static int sampleGradient(float var0, int... var1) {
        int length2 = var1.length;
        float var3 = var0 * (length2 - 1);
        int var4 = (int) var3;
        return var4 >= length2 - 1 ? var1[length2 - 1] : reverseLerpClamped(var1[var4 + 1], var1[var4], var3 - var4);
    }

    public static int blue(int var0) {
        return var0 & 0xFF;
    }

    public static int red(int var0) {
        return var0 >> 16 & 0xFF;
    }

    public static int green(int var0) {
        return var0 >> 8 & 0xFF;
    }

    public static int animatedMultiGradient(int var0, int var1, int... var2) {
        int var3 = (int) ((System.currentTimeMillis() / var0 + var1) % 360L);
        var3 = (var3 > 180 ? 360 - var3 : var3) + 180;
        int var4 = (int) (var3 / 360.0F * var2.length);
        if (var4 == var2.length) {
            var4--;
        }
        int var5 = var2[var4];
        int var6 = var2[var4 == var2.length - 1 ? 0 : var4 + 1];
        return reverseLerpClamped(var5, var6, var3 / 360.0F * var2.length - var4);
    }

    public static int withIntegerAlphaFactor(int var0, int var1) {
        return packArgb(getRed(var0), getGreen(var0), getBlue(var0), Math.round(var1 * 255));
    }

    public static int packArgbUnchecked(int var0, int var1, int var2, int var3) {
        return var3 << 24 | var0 << 16 | var1 << 8 | var2;
    }

    public static float[] toNormalizedRgba(int var0) {
        return new float[] { red(var0) / 255.0F, green(var0) / 255.0F, blue(var0) / 255.0F, getAlpha(var0) / 255.0F };
    }

    public static int alpha(int var0) {
        return getAlpha(var0);
    }

    public static int grayscale(int var0) {
        return packArgb(var0, var0, var0, 255);
    }

    public static int multiplyBrightness(int var0, float var1) {
        return packFloatChannels(red(var0) * var1, green(var0) * var1, blue(var0) * var1, getAlpha(var0));
    }

    public static int packArgb(int var0, int var1, int var2, int var3) {
        return clampChannel(var3) << 24 | clampChannel(var0) << 16 | clampChannel(var1) << 8 | clampChannel(var2);
    }

    public static int packRgbWithAlphaFactor(float var0, float var1, float var2, float var3) {
        return packArgb((int) var0, (int) var1, (int) var2, Math.round(var3 * 255.0F));
    }

    public static int blendWithWhite(int var0, float var1) {
        return packFloatChannels(red(var0) + (255 - red(var0)) * var1, green(var0) + (255 - green(var0)) * var1, blue(var0) + (255 - blue(var0)) * var1, getAlpha(var0));
    }

    public static int rainbow(int var0, int var1, float var2, float var3, float var4) {
        int var5 = (int) ((System.currentTimeMillis() / var0 + var1) % 360L);
        float var6 = var5 / 360.0F;
        int var7 = Color.HSBtoRGB(var6, var2, var3);
        return packArgb(red(var7), green(var7), blue(var7), Math.max(0, Math.min(255, (int) (var4 * 255.0F))));
    }

    public static int redGreenGradient(float var0, float var1, boolean var2) {
        float var3 = var0 / var1;
        var3 = Math.max(0.0F, Math.min(var3, 1.0F));
        int var4 = (int) (255.0F * var3);
        int var5 = 255 - var4;
        return rgb(var2 ? var4 : var5, var2 ? var5 : var4, 0);
    }

    public static int lerpClamped(int var0, int var1, double var2) {
        float var4 = Mth.clamp((float) var2, 0.0F, 1.0F);
        return lerpRounded(var0, var1, var4);
    }

    public static int grayscale(int var0, int var1) {
        return packArgb(var0, var0, var0, var1);
    }

    public static int rgb(int var0, int var1, int var2) {
        return packArgb(var0, var1, var2, 255);
    }

    public static int withAlphaFactor(int var0, float var1) {
        return packArgb(getRed(var0), getGreen(var0), getBlue(var0), Math.round(var1 * 255.0F));
    }

    public static Color pulseBrightness(int var0, int var1, Color color) {
        float[] var3 = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int var4 = (int) ((System.currentTimeMillis() / var0 + var1) % 360L);
        var4 = (var4 > 180 ? 360 - var4 : var4) + 180;
        Color color2 = new Color(Color.HSBtoRGB(var3[0], var3[1], var4 / 360.0F));
        return new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), Mth.clamp(0, 255, color.getAlpha()));
    }

    public static int getBlue(int var0) {
        return blue(var0);
    }

    public static int alternateColors(int var0, int var1, int var2, int var3) {
        int var4 = (int) ((System.currentTimeMillis() / var0 + var1) % 360L);
        var4 = (var4 > 180 ? 360 - var4 : var4) + 180;
        float var5 = var4 / 360.0F * 2.0F;
        int var6 = (int) var5;
        if (var6 == 2) {
            var6 = 1;
        }
        int var7 = var6 == 0 ? var2 : var3;
        int var8 = var6 == 1 ? var2 : var3;
        return reverseLerpClamped(var7, var8, var5 - var6);
    }

    public static float alphaFloat(int var0) {
        return getAlpha(var0) / 255.0F;
    }

    public static int gradientAtTime(long var0, int var2, int var3, int var4, int var5) {
        int var6 = (int) ((var0 / var2 + var3) % 360L);
        var6 = var6 >= 180 ? 360 - var6 : var6;
        return lerpRounded(var4, var5, var6 / 180.0F);
    }

    public static int multiplyAlpha(int var0, float var1) {
        return var0 & 16777215 | clampChannel(Math.round(alpha(var0) * var1)) << 24;
    }

    public static float blueFloat(int var0) {
        return blue(var0) / 255.0F;
    }

    public static int animatedTwoColorGradient(int var0, int var1, int var2, int var3) {
        return gradientAtTime(System.currentTimeMillis(), var0, var1, var2, var3);
    }

    public static int lerpUnchecked(int var0, int var1, double var2) {
        float var4 = (float) var2;
        int var5 = (int) ((var0 >> 24 & 0xFF) + ((var1 >> 24 & 0xFF) - (var0 >> 24 & 0xFF)) * var4);
        int var6 = (int) ((var0 >> 16 & 0xFF) + ((var1 >> 16 & 0xFF) - (var0 >> 16 & 0xFF)) * var4);
        int var7 = (int) ((var0 >> 8 & 0xFF) + ((var1 >> 8 & 0xFF) - (var0 >> 8 & 0xFF)) * var4);
        int var8 = (int) ((var0 & 0xFF) + ((var1 & 0xFF) - (var0 & 0xFF)) * var4);
        return var5 << 24 | var6 << 16 | var7 << 8 | var8;
    }

    public static float redFloat(int var0) {
        return red(var0) / 255.0F;
    }

    public static int withAlpha(int var0, int var1) {
        return Mth.clamp(var1, 0, 255) << 24 | var0 & 16777215;
    }

    public static int getRed(int var0) {
        return red(var0);
    }

    public static int packFloatChannels(float var0, float var1, float var2, float var3) {
        return packArgb((int) var0, (int) var1, (int) var2, (int) var3);
    }

    public static int getGreen(int var0) {
        return green(var0);
    }

    public static Color randomPastel() {
        return new Color(Color.HSBtoRGB((float) Math.random(), (float) (0.75 + Math.random() / 4.0), (float) (0.75 + Math.random() / 4.0)));
    }

    public static int animatedAccent(int var0) {
        return animatedTwoColorGradient(4, var0, ColorUtils.Theme.primaryAccent(), ColorUtils.Theme.secondaryAccent());
    }

    public static int parseHexRgb(String var0) {
        int var1 = Integer.parseInt(var0.substring(1), 16);
        return withAlpha(var1, 255);
    }

    public static float[] unpackRgba(int var0) {
        return new float[] { (var0 >> 16 & 0xFF) / 255.0F, (var0 >> 8 & 0xFF) / 255.0F, (var0 & 0xFF) / 255.0F, (var0 >> 24 & 0xFF) / 255.0F };
    }

    public static float[] rgbaComponents(int var0) {
        return unpackRgba(var0);
    }

    public static int getAlpha(int var0) {
        return var0 >> 24 & 0xFF;
    }

    public static int grayscaleWithAlphaFactor(int var0, float var1) {
        return grayscale(var0, Math.round(var1 * 255.0F));
    }

    public static int reverseLerpClamped(int var0, int var1, float var2) {
        float var3 = var2 < 0.0F ? 0.0F : (var2 > 1.0F ? 1.0F : var2);
        int var4 = (int) ((var1 >> 24 & 0xFF) + ((var0 >> 24 & 0xFF) - (var1 >> 24 & 0xFF)) * var3);
        int var5 = (int) ((var1 >> 16 & 0xFF) + ((var0 >> 16 & 0xFF) - (var1 >> 16 & 0xFF)) * var3);
        int var6 = (int) ((var1 >> 8 & 0xFF) + ((var0 >> 8 & 0xFF) - (var1 >> 8 & 0xFF)) * var3);
        int var7 = (int) ((var1 & 0xFF) + ((var0 & 0xFF) - (var1 & 0xFF)) * var3);
        return var4 << 24 | var5 << 16 | var6 << 8 | var7;
    }

    public static int multiplySaturation(int var0, float var1) {
        float[] var2 = Color.RGBtoHSB(getRed(var0), getGreen(var0), getBlue(var0), null);
        var2[1] = Math.max(0.0F, Math.min(1.0F, var2[1] * var1));
        int var3 = Color.HSBtoRGB(var2[0], var2[1], var2[2]);
        return packArgb(var3 >> 16 & 0xFF, var3 >> 8 & 0xFF, var3 & 0xFF, alpha(var0));
    }

    public static float greenFloat(int var0) {
        return green(var0) / 255.0F;
    }

    public static int packRgbUnchecked(int var0, int var1, int var2) {
        return -16777216 | var0 << 16 | var1 << 8 | var2;
    }

    public static int lerpFloatUnchecked(int var0, int var1, float var2) {
        int var3 = (int) ((var0 >> 24 & 0xFF) + ((var1 >> 24 & 0xFF) - (var0 >> 24 & 0xFF)) * var2);
        int var4 = (int) ((var0 >> 16 & 0xFF) + ((var1 >> 16 & 0xFF) - (var0 >> 16 & 0xFF)) * var2);
        int var5 = (int) ((var0 >> 8 & 0xFF) + ((var1 >> 8 & 0xFF) - (var0 >> 8 & 0xFF)) * var2);
        int var6 = (int) ((var0 & 0xFF) + ((var1 & 0xFF) - (var0 & 0xFF)) * var2);
        return var3 << 24 | var4 << 16 | var5 << 8 | var6;
    }

    private static int clampChannel(int var0) {
        return var0 < 0 ? 0 : (var0 > 255 ? 255 : var0);
    }

    public static final class Argb {
        public static <T extends Number> T lerpNumber(T t, T t2, double var2) {
            double var4 = t.doubleValue();
            double sx = t2.doubleValue();
            double sxx = var4 + var2 * (sx - var4);
            if (t instanceof Integer) {
                return (T) (Object) (int) Math.round(sxx);
            } else if (t instanceof Double) {
                return (T) (Object) sxx;
            } else if (t instanceof Float) {
                return (T) (Object) (float) sxx;
            } else if (t instanceof Long) {
                return (T) (Object) Math.round(sxx);
            } else if (t instanceof Short) {
                return (T) (Object) (short) Math.round(sxx);
            } else if (t instanceof Byte) {
                return (T) (Object) (byte) Math.round(sxx);
            } else {
                throw new IllegalArgumentException("Unsupported type: " + t.getClass().getSimpleName());
            }
        }

        private Argb() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    public static class Theme {
        private static final int GRADIENT_PERIOD = Integer.MAX_VALUE;

        public static int accentAtOffset(int var0) {
            return ColorUtils.animatedAccent(var0);
        }

        public static int accentWithOpacity(int var0, float var1) {
            return ColorUtils.multiplyAlpha(accentAtOffset(var0), Math.min(var1, 1.0F));
        }

        public static int primaryAccent() {
            return ess.ll.client.render.Theme.accent();
        }

        public static int secondaryAccent() {
            return ess.ll.client.render.Theme.accentLight();
        }

        public static int animatedThemeGradient(int var0) {
            return ColorUtils.animatedTwoColorGradient(GRADIENT_PERIOD, var0, primaryAccent(), secondaryAccent());
        }
    }
}
