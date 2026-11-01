package ess.ll.client.event;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.textures.GpuTextureView;

public class RenderTargetEvent extends Event {
    public static final RenderTargetEvent INSTANCE = new RenderTargetEvent();
    private RenderTarget renderTarget;
    private GpuTextureView textureView;

    public boolean fire(RenderTarget renderTarget, GpuTextureView gpuTextureView) {
        this.renderTarget = renderTarget;
        this.textureView = gpuTextureView;
        super.dispatch();
        return this.consumeCancelled();
    }

    public void setTextureView(GpuTextureView gpuTextureView) {
        this.textureView = gpuTextureView;
    }

    public RenderTarget getRenderTarget() {
        return this.renderTarget;
    }

    public void setRenderTarget(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public GpuTextureView getTextureView() {
        return this.textureView;
    }
}
