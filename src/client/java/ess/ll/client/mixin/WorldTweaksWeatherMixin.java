package ess.ll.client.mixin;

import ess.ll.client.module.render.WorldTweaks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** WorldTweaks weather: the client world rains, snows or thunders as the module says. */
@Mixin(Level.class)
public abstract class WorldTweaksWeatherMixin {
    @Inject(method="getRainLevel",at=@At("RETURN"),cancellable=true)
    private void ess$rain(float partialTicks,CallbackInfoReturnable<Float> cir) {
        if((Object)this instanceof ClientLevel) cir.setReturnValue(WorldTweaks.rainLevel(cir.getReturnValue()));
    }
    @Inject(method="getThunderLevel",at=@At("RETURN"),cancellable=true)
    private void ess$thunder(float partialTicks,CallbackInfoReturnable<Float> cir) {
        if((Object)this instanceof ClientLevel) cir.setReturnValue(WorldTweaks.thunderLevel(cir.getReturnValue()));
    }
}
