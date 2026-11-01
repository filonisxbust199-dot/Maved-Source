package ess.ll.client.mixin;

import ess.ll.client.module.render.WorldTweaks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** WorldTweaks weather: rain or snow in every biome of the client world (what the weather renderer and sounds ask). */
@Mixin(ClientLevel.class)
public abstract class WorldTweaksPrecipitationMixin {
    @Inject(method="getPrecipitationAt",at=@At("RETURN"),cancellable=true)
    private void ess$precipitation(BlockPos pos,CallbackInfoReturnable<Biome.Precipitation> cir) {
        cir.setReturnValue(WorldTweaks.precipitation(cir.getReturnValue()));
    }
}
