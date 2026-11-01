package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.event.NauseaStrengthEvent;
import ess.ll.client.event.VisualRemovalEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class RemovalsGameRendererMixin {
    @Inject(method="bobHurt",at=@At("HEAD"),cancellable=true)
    private void ess$hurtCamera(CameraRenderState camera,PoseStack pose,CallbackInfo ci) {
        VisualRemovalEvent.INSTANCE.cancelIfRemoved(VisualRemovalEvent.Type.NOHURTCAM,ci);
    }
    @ModifyExpressionValue(method="renderLevel",at=@At(value="FIELD",
        target="Lnet/minecraft/client/renderer/state/level/PlayerRenderState;nauseaEffectIntensity:F"))
    private float ess$nauseaWorld(float strength) {
        return NauseaStrengthEvent.INSTANCE.fire(strength);
    }
}
