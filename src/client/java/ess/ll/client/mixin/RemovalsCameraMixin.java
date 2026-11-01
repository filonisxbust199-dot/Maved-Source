package ess.ll.client.mixin;

import ess.ll.client.event.VisualRemovalEvent;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class RemovalsCameraMixin {
    @Inject(method="getMaxZoom",at=@At("HEAD"),cancellable=true)
    private void ess$f5Clip(float zoom,CallbackInfoReturnable<Float> cir) {
        VisualRemovalEvent.INSTANCE.overrideIfRemoved(VisualRemovalEvent.Type.F5_XUETA,cir,zoom);
    }
}
