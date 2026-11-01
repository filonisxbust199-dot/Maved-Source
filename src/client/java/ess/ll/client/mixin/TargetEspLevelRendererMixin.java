package ess.ll.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import ess.ll.client.event.RenderTargetEvent;
import ess.ll.client.integration.LevelRendererAccess;
import ess.ll.client.render.TargetScanRenderer;
import ess.ll.client.module.combat.CombatRuntime;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class TargetEspLevelRendererMixin implements LevelRendererAccess {
    @Shadow
    @Final
    private RenderTarget entityOutlineTarget;

    @Unique
    private Frustum ess$frustum;

    @Override
    public Frustum frustum() {
        return ess$frustum;
    }

    @Override
    public RenderTarget hexRawOutlineTarget() {
        return entityOutlineTarget;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ess$scanProjection(GraphicsResourceAllocator allocator, boolean renderBlockOutline, CameraRenderState cameraState, GpuBufferSlice fog, Vector4f clearColor, boolean renderSky, boolean renderClouds, CallbackInfo ci) {
        ess$frustum = cameraState.cullFrustum;
        TargetScanRenderer.setProjectionMatrix(cameraState.projectionMatrix);
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;executeOutline(Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V"))
    private void ess$worldTargetEffect(CallbackInfo ci) {
        // With Chams on, the effect is drawn after the chams pass instead, so the chams do not cover it.
        if (!ess.ll.client.module.render.Chams.enabled())
            CombatRuntime.renderTargetEffects();
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;executeOutline(Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V", shift = At.Shift.AFTER))
    private void ess$scanComposite(CallbackInfo ci) {
        TargetScanRenderer.flush();
    }

    @Inject(method = "blitEntityOutline", at = @At("HEAD"), cancellable = true)
    private void ess$replaceOutline(CallbackInfo ci) {
        if (entityOutlineTarget != null && RenderTargetEvent.INSTANCE.fire(entityOutlineTarget, entityOutlineTarget.getColorTextureView()))
            ci.cancel();
    }
}
