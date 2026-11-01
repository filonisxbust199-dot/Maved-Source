package ess.ll.client.mixin;

import ess.ll.client.module.render.WorldTweaks;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WorldTweaks light: the lightmap is built with a raised gamma. */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class WorldTweaksLightMixin {
    @Inject(method="extract",at=@At("TAIL"))
    private void ess$light(LightmapRenderState state,float partialTick,CallbackInfo ci) {
        WorldTweaks.lightmap(state);
    }
}
