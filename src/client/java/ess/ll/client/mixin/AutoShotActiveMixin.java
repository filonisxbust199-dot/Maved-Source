package ess.ll.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An autoshot window in the background still counts as the active one, as AutoShotWindowMixin explains. */
@Mixin(Minecraft.class)
public abstract class AutoShotActiveMixin {
    @Inject(method="isWindowActive",at=@At("HEAD"),cancellable=true)
    private void ess$active(CallbackInfoReturnable<Boolean> cir) {
        if(Boolean.getBoolean("ess.autoshot") && !Boolean.getBoolean("ess.autoshot.foreground")) cir.setReturnValue(true);
    }
}
