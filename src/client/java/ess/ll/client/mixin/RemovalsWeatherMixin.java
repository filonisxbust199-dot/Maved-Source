package ess.ll.client.mixin;

import ess.ll.client.event.WeatherStateEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class RemovalsWeatherMixin {
    @Inject(method="isRaining",at=@At("HEAD"),cancellable=true)
    private void ess$rain(CallbackInfoReturnable<Boolean> cir) {
        if((Object)this instanceof ClientLevel)
            WeatherStateEvent.INSTANCE.overrideActive(WeatherStateEvent.Type.RAIN,cir);
    }
    @Inject(method="isThundering",at=@At("HEAD"),cancellable=true)
    private void ess$thunder(CallbackInfoReturnable<Boolean> cir) {
        if((Object)this instanceof ClientLevel)
            WeatherStateEvent.INSTANCE.overrideActive(WeatherStateEvent.Type.THUNDER,cir);
    }
    @Inject(method="getRainLevel",at=@At("HEAD"),cancellable=true)
    private void ess$rainLevel(float partial,CallbackInfoReturnable<Float> cir) {
        if((Object)this instanceof ClientLevel)
            WeatherStateEvent.INSTANCE.overrideGradient(WeatherStateEvent.Type.RAIN,cir);
    }
    @Inject(method="getThunderLevel",at=@At("HEAD"),cancellable=true)
    private void ess$thunderLevel(float partial,CallbackInfoReturnable<Float> cir) {
        if((Object)this instanceof ClientLevel)
            WeatherStateEvent.INSTANCE.overrideGradient(WeatherStateEvent.Type.THUNDER,cir);
    }
}
