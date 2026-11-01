package ess.ll.client.mixin;

import ess.ll.client.module.render.MotionBlur;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MotionBlur goes over the finished level, before the hand and the 3D HUD are drawn. */
@Mixin(GameRenderer.class)
public abstract class MotionBlurMixin {
    @Inject(method="renderLevel",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/GameRenderer;render3dHud(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/OptionsRenderState;Z)V"))
    private void ess$motionBlur(CallbackInfo ci) {
        MotionBlur.render();
    }
}
