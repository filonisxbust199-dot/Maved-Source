package ess.ll.client.mixin;

import ess.ll.client.weather.Weather;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WorldTweaks weather: the vanilla rain and snow columns give way to the weather's own drops and flakes. */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherVanillaHideMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void ess$hide(ClientLevel level,float partialTicks,Vec3 cameraPos,WeatherRenderState state,CallbackInfo ci) {
        if(Weather.hidesVanilla()) {
            state.reset();
            ci.cancel();
        }
    }
}
