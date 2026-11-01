package ess.ll.client.mixin;

import ess.ll.client.module.render.ClientClockAccess;
import ess.ll.client.module.ClientModules;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads are overridden; server updates continue writing the original backing fields. */
@Mixin(ClientClockManager.ClientClockInstance.class)
public abstract class ClientClockInstanceMixin implements ClientClockAccess {
    @Shadow private long totalTicks;
    @Unique private boolean ess$overworld;
    @Override public void ess$setOverworld(boolean overworld) { ess$overworld=overworld; }
    /** Time of day to show from WorldTweaks or CustomTime; -1 keeps the server's clock. */
    @Unique private long ess$override() {
        if(!ess$overworld) return -1;
        long tweak=ess.ll.client.module.render.WorldTweaks.time();
        if(tweak>=0) return tweak;
        return ClientModules.CUSTOM_TIME.enabled() ? (long)ClientModules.TIME.get() : -1;
    }
    @Inject(method="totalTicks",at=@At("HEAD"),cancellable=true)
    private void ess$time(CallbackInfoReturnable<Long> cir) {
        long time=ess$override();
        if(time>=0) cir.setReturnValue(Math.floorDiv(totalTicks,24000)*24000+time);
    }
    @Inject(method="partialTick",at=@At("HEAD"),cancellable=true)
    private void ess$partial(CallbackInfoReturnable<Float> cir) {
        if(ess$override()>=0) cir.setReturnValue(0f);
    }
    @Inject(method="rate",at=@At("HEAD"),cancellable=true)
    private void ess$rate(CallbackInfoReturnable<Float> cir) {
        if(ess$override()>=0) cir.setReturnValue(0f);
    }
    @Inject(method="isPaused",at=@At("HEAD"),cancellable=true)
    private void ess$paused(CallbackInfoReturnable<Boolean> cir) {
        if(ess$override()>=0) cir.setReturnValue(true);
    }
}
