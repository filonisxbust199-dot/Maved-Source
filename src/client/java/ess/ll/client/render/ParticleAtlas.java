package ess.ll.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public enum ParticleAtlas {
    INSTANCE;

    private final ParticleAtlas.AtlasEntry[] cachedAtlases = new ParticleAtlas.AtlasEntry[4];
    private int nextCacheSlot;
    private ParticleAtlas.AtlasEntry activeAtlas;

    public int getSlotCount() {
        return this.activeAtlas == null ? 0 : this.activeAtlas.slotCount;
    }

    public RenderType getRenderType() {
        return this.activeAtlas == null ? null : this.activeAtlas.renderType;
    }

    public float getMinU(int var1) {
        return this.activeAtlas.minU[var1];
    }

    public float getMaxU(int var1) {
        return this.activeAtlas.maxU[var1];
    }

    public void select(int var1) {
        for (ParticleAtlas.AtlasEntry atlasEntry : this.cachedAtlases) {
            if (atlasEntry != null && atlasEntry.mask == var1) {
                this.activeAtlas = atlasEntry;
                return;
            }
        }
        this.activeAtlas = this.cache(buildAtlas(var1));
    }

    private ParticleAtlas.AtlasEntry cache(ParticleAtlas.AtlasEntry atlasEntry) {
        if (atlasEntry == null) {
            return null;
        }
        release(this.cachedAtlases[this.nextCacheSlot]);
        this.cachedAtlases[this.nextCacheSlot] = atlasEntry;
        this.nextCacheSlot = (this.nextCacheSlot + 1) % 4;
        return atlasEntry;
    }

    private static void release(ParticleAtlas.AtlasEntry atlasEntry) {
        if (atlasEntry != null) {
            Minecraft.getInstance().getTextureManager().release(atlasEntry.texture);
        }
    }

    private static List<NativeImage> loadImages(int var0) {
        ArrayList items = new ArrayList();
        for (int var2 = 0; var2 < 11; var2++) {
            if ((var0 & 1 << var2) != 0) {
                NativeImage nativeImage = ClientTextures.INSTANCE.loadParticleImage(var2);
                if (nativeImage != null) {
                    items.add(nativeImage);
                }
            }
        }
        return items;
    }

    private static int mipLevelCount(int var0) {
        return Math.max(0, Integer.numberOfTrailingZeros(var0) - Integer.numberOfTrailingZeros(32));
    }

    private static NativeImage[] generateMipmaps(NativeImage nativeImage, int var1) {
        NativeImage[] nativeImage2 = new NativeImage[var1 + 1];
        nativeImage2[0] = nativeImage;
        for (int var3 = 1; var3 < nativeImage2.length; var3++) {
            nativeImage2[var3] = downsample(nativeImage2[var3 - 1]);
        }
        return nativeImage2;
    }

    private static NativeImage downsample(NativeImage nativeImage) {
        int var1 = nativeImage.getWidth() >> 1;
        int var2 = nativeImage.getHeight() >> 1;
        NativeImage nativeImage2 = new NativeImage(var1, var2, false);
        for (int var4 = 0; var4 < var2; var4++) {
            for (int var5 = 0; var5 < var1; var5++) {
                nativeImage2.setPixel(var5, var4, averagePremultipliedColor(nativeImage, var5 << 1, (var5 << 1) + 2, var4 << 1, (var4 << 1) + 2));
            }
        }
        return nativeImage2;
    }

    private static void copyScaledTile(NativeImage nativeImage, NativeImage nativeImage2, int var2, int var3) {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        for (int var6 = 0; var6 < var3; var6++) {
            int var7 = var6 * height / var3;
            int var8 = Math.max(var7 + 1, (var6 + 1) * height / var3);
            for (int var9 = 0; var9 < var3; var9++) {
                int var10 = var9 * width / var3;
                int var11 = Math.max(var10 + 1, (var9 + 1) * width / var3);
                nativeImage2.setPixel(var2 + var9, var6, averagePremultipliedColor(nativeImage, var10, var11, var7, var8));
            }
        }
    }

    private static int averagePremultipliedColor(NativeImage nativeImage, int var1, int var2, int var3, int var4) {
        long var5 = 0L;
        long var7 = 0L;
        long var9 = 0L;
        long var11 = 0L;
        for (int var13 = var3; var13 < var4; var13++) {
            for (int var14 = var1; var14 < var2; var14++) {
                int pixel = nativeImage.getPixel(var14, var13);
                int var16 = pixel >>> 24 & 0xFF;
                var5 += (pixel >> 16 & 0xFF) * var16;
                var7 += (pixel >> 8 & 0xFF) * var16;
                var9 += (pixel & 0xFF) * var16;
                var11 += var16;
            }
        }
        if (var11 == 0L) {
            return 0;
        }
        int var17 = (int) (var11 / ((long) (var2 - var1) * (var4 - var3)));
        return var17 << 24 | (int) (var5 / var11) << 16 | (int) (var7 / var11) << 8 | (int) (var9 / var11);
    }

    private static ParticleAtlas.AtlasEntry buildAtlas(int var0) {
        var var1 = loadImages(var0);
        if (var1.isEmpty()) {
            return null;
        }
        int count = var1.size();
        int var3 = 0;
        for (NativeImage nativeImage : var1) {
            var3 = Math.max(var3, Math.max(nativeImage.getWidth(), nativeImage.getHeight()));
        }
        if (var3 == 0) {
            return null;
        }
        var3 = Integer.highestOneBit(var3 * 2 - 1);
        int var16 = var3 * count;
        NativeImage nativeImage2 = new NativeImage(var16, var3, false);
        for (int var6 = 0; var6 < count; var6++) {
            copyScaledTile((NativeImage) var1.get(var6), nativeImage2, var6 * var3, var3);
        }
        int var18 = mipLevelCount(var3);
        Identifier identifier = Identifier.fromNamespaceAndPath("Hex".toLowerCase(), "dynamic/particle_atlas_" + Integer.toHexString(var0));
        UploadedTexture uploadedTexture = new UploadedTexture(() -> "hex particle atlas", generateMipmaps(nativeImage2, var18));
        try {
            Minecraft.getInstance().getTextureManager().register(identifier, uploadedTexture);
            float[] var9 = new float[count];
            float[] var10 = new float[count];
            float var11 = 0.5F * (1 << var18) / var16;
            for (int var12 = 0; var12 < count; var12++) {
                var9[var12] = (float) (var12 * var3) / var16 + var11;
                var10[var12] = (float) ((var12 + 1) * var3) / var16 - var11;
            }
            return new ParticleAtlas.AtlasEntry(var0, identifier, EffectRenderPipelines.particles(identifier), count, var9, var10);
        } catch (Exception exception) {
            try {
                uploadedTexture.close();
            } catch (Exception exception2) {
            }
            return null;
        }
    }

    private record AtlasEntry(int mask, Identifier texture, RenderType renderType, int slotCount, float[] minU, float[] maxU) {
    }
}
