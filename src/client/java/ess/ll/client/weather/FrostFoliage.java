package ess.ll.client.weather;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

public final class FrostFoliage {
    public static volatile boolean enabled;
    private static final float CONST_floatValue = 3.2F;
    private static final int CONST_intValue = 16777215;
    private static final int CONST_intValue2 = 14213872;
    private static final String[] texts = new String[] { "leaves", "grass", "fern", "bush", "sapling", "vine", "flower", "tulip", "orchid", "dandelion", "poppy", "allium", "bluet", "daisy", "cornflower", "lily", "rose", "peony", "lilac", "sunflower", "azalea", "petals", "mushroom", "wheat", "carrots", "potatoes", "beetroots", "sugar_cane", "bamboo", "moss_carpet", "seagrass", "kelp" };
    private static final String[] texts2 = new String[] { "grass_block", "grass_path", "short_grass_top", "seagrass_", "kelp_plant" };
    private static final float CONST_floatValue2 = 0.8F;
    public static int intValue;

    private FrostFoliage() {
    }

    public static boolean shouldApplyFrost(Identifier identifier) {
        if (enabled && identifier != null && "minecraft".equals(identifier.getNamespace())) {
            String path = identifier.getPath();
            if (!path.startsWith("block/")) {
                return false;
            } else {
                for (String var5 : texts2) {
                    if (path.contains(var5)) {
                        return false;
                    }
                }
                for (String var9 : texts) {
                    if (path.contains(var9)) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    public static void applyFrost(Identifier identifier, NativeImage nativeImage) {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        if (width > 0 && height > 0) {
            intValue++;
            boolean var4 = identifier.getPath().contains("leaves");
            if (var4) {
                for (int var5 = 0; var5 < width; var5++) {
                    for (int var6 = 0; var6 < height; var6++) {
                        int pixel = nativeImage.getPixel(var5, var6);
                        if (pixel >>> 24 > 16) {
                            int var8 = interpolateRgb(14213872, 16777215, pixelNoise(textureSeed(identifier), var5, var6));
                            nativeImage.setPixel(var5, var6, pixel & 0xFF000000 | blendRgb(pixel, var8, 0.8F));
                        }
                    }
                }
            }
            float var18 = width / 16.0F;
            long var19 = textureSeed(identifier);
            for (int var20 = 0; var20 < width; var20++) {
                int var9 = -1;
                for (int var10 = 0; var10 < height; var10++) {
                    if (nativeImage.getPixel(var20, var10) >>> 24 > 128) {
                        var9 = var10;
                        break;
                    }
                }
                if (var9 >= 0) {
                    float var21 = pixelNoise(var19, var20, 0) + 0.5F * pixelNoise(var19, var20, 7);
                    int var11 = Math.max(1, Math.round(3.2F * var18 * (0.55F + 0.75F * var21)));
                    for (int var12 = 0; var12 < var11; var12++) {
                        int var13 = var9 + var12;
                        if (var13 >= height) {
                            break;
                        }
                        int pixel2 = nativeImage.getPixel(var20, var13);
                        int var15 = pixel2 >>> 24;
                        if (var15 <= 128) {
                            break;
                        }
                        int var16 = interpolateRgb(14213872, 16777215, pixelNoise(var19, var20, var13 + 3));
                        float var17 = var12 == var11 - 1 ? 0.45F : 1.0F;
                        nativeImage.setPixel(var20, var13, var15 << 24 | blendRgb(pixel2, var16, var17));
                    }
                }
            }
        }
    }

    private static long textureSeed(Identifier identifier) {
        return identifier.hashCode() * -7046029254386353131L;
    }

    private static int interpolateRgb(int var0, int var1, float var2) {
        int var3 = channel(var0 >> 16, var1 >> 16, var2);
        int var4 = channel(var0 >> 8, var1 >> 8, var2);
        int var5 = channel(var0, var1, var2);
        return var3 << 16 | var4 << 8 | var5;
    }

    private static int blendRgb(int var0, int var1, float var2) {
        int var3 = channel(var0 >> 16, var1 >> 16, var2);
        int var4 = channel(var0 >> 8, var1 >> 8, var2);
        int var5 = channel(var0, var1, var2);
        return var3 << 16 | var4 << 8 | var5;
    }

    private static int channel(int var0, int var1, float var2) {
        int var3 = var0 & 0xFF;
        int var4 = var1 & 0xFF;
        return Math.clamp((long) ((int) (var3 + (var4 - var3) * var2 + 0.5F)), 0, 255);
    }

    private static float pixelNoise(long var0, int var2, int var3) {
        long var4 = var0 ^ var2 * -6752110988234923001L ^ var3 * -6939452855193903323L;
        var4 ^= var4 >>> 29;
        var4 *= -4658895280553007687L;
        var4 ^= var4 >>> 32;
        return (float) (var4 >>> 40) / 1.6777216E7F;
    }
}
