package ess.ll.client.render;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class UploadedTexture extends AbstractTexture {
    private NativeImage[] mipImages;

    private void releaseImages() {
        if (this.mipImages != null) {
            for (NativeImage nativeImage : this.mipImages) {
                if (nativeImage != null) {
                    nativeImage.close();
                }
            }
            this.mipImages = null;
        }
    }

    @Override
    public void close() {
        super.close();
        this.releaseImages();
    }

    public UploadedTexture(Supplier<String> supplier, NativeImage[] mipImages2) {
        this.mipImages = mipImages2;
        NativeImage nativeImage = mipImages2[0];
        var device = RenderSystem.getDevice();
        this.texture = device.createTexture(supplier, 5, GpuFormat.RGBA8_UNORM, nativeImage.getWidth(), nativeImage.getHeight(), 1, mipImages2.length);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
        this.textureView = device.createTextureView(this.texture);
        for (int var5 = 0; var5 < mipImages2.length; var5++) {
            device.createCommandEncoder().writeToTexture(this.texture, mipImages2[var5], var5, 0, 0, 0);
        }
    }
}
