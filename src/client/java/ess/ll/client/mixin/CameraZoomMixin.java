package ess.ll.client.mixin;

import ess.ll.client.module.render.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraZoomMixin {
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void ess$zoom(float partialTicks,CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ZoomModule.apply(cir.getReturnValue()));
    }
}
