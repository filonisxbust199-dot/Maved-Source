package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ess.ll.client.event.NightVisionEvent;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class RemovalsLightmapMixin {
    @ModifyExpressionValue(method="extract",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float ess$darkness(float strength) {
        return NightVisionEvent.INSTANCE.fire()?0:strength;
    }
}
