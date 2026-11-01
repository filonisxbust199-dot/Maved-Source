package ess.ll.client.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import ess.ll.client.module.misc.Optimization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optimization "FPS в фоне": the frame limit drops while the game window is not focused. */
@Mixin(FramerateLimitTracker.class)
public abstract class OptimizationFramerateMixin {
    @Inject(method="getFramerateLimit",at=@At("RETURN"),cancellable=true)
    private void ess$background(CallbackInfoReturnable<Integer> cir) {
        int limit=Optimization.framerate(cir.getReturnValueI());
        if(limit!=cir.getReturnValueI()) cir.setReturnValue(limit);
    }
}
