package ess.ll.client.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import ess.ll.client.render.WorldFx;
import ess.ll.client.module.render.Chams;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World effects (FogAtmosphere) after the opaque and translucent world, before entity outlines. */
@Mixin(value=LevelRenderer.class,priority=900)
public abstract class AtmosphereLevelRendererMixin {
    @Inject(method="submitEntities",at=@At("HEAD"))
    private void ess$beginChamsSubmission(PoseStack pose,LevelRenderState state,SubmitNodeCollector nodes,CallbackInfo ci) {
        Chams.beginWorldSubmission();
    }
    @Inject(method="submitEntities",at=@At("TAIL"))
    private void ess$endChamsSubmission(PoseStack pose,LevelRenderState state,SubmitNodeCollector nodes,CallbackInfo ci) {
        Chams.endWorldSubmission();
    }
    @Inject(method="render",at=@At("HEAD"))
    private void ess$captureCamera(GraphicsResourceAllocator allocator,boolean renderBlockOutline,CameraRenderState cameraState,
        GpuBufferSlice fog,Vector4f clearColor,boolean renderSky,boolean renderClouds,CallbackInfo ci) {
        WorldFx.capture(cameraState);
    }

    @Inject(method="lambda$addMainPass$0",at=@At("HEAD"))
    private void ess$beforeTerrain(CallbackInfo ci) {
        WorldFx.beforeTerrain();
    }

    @Inject(method="lambda$addMainPass$0",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/LevelRenderer;executeOutline(Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V"))
    private void ess$worldEffects(CallbackInfo ci) {
        WorldFx.renderWorld();
    }
}
