package ess.ll.client.mixin;

import ess.ll.client.module.render.FogAtmosphere;
import ess.ll.client.module.render.WorldTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FogAtmosphere replacing the vanilla distance fog; WorldTweaks fog and mist over it. */
@Mixin(FogRenderer.class)
public abstract class AtmosphereFogMixin {
    @Inject(method="computeFogColor",at=@At("RETURN"))
    private void ess$fogColor(Camera camera,float partial,ClientLevel level,int renderDistance,float darkness,Vector4f color,CallbackInfo ci) {
        WorldTweaks.fogColor(camera,color);
    }

    @Inject(method="setupFog",at=@At("RETURN"))
    private void ess$atmosphereFog(Camera camera,int renderDistance,DeltaTracker delta,float darkness,ClientLevel level,CallbackInfoReturnable<FogData> cir) {
        FogAtmosphere.fog(camera,cir.getReturnValue());
        WorldTweaks.fog(camera,cir.getReturnValue());
    }
}
