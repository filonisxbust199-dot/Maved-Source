package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ess.ll.client.weather.Weather;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** WorldTweaks snow: grass and leaves take on frost (the chunks rebuild when the frost comes or goes). */
@Mixin(BiomeColors.class)
public abstract class WeatherBiomeColorsMixin {
    @ModifyReturnValue(method="getAverageGrassColor",at=@At("RETURN"))
    private static int ess$frostGrass(int color,BlockAndTintGetter level,BlockPos pos) {
        float frost=Weather.frost();
        return frost>.001f ? ess$blend(color,0xd8e8dc,frost) : color;
    }
    @ModifyReturnValue(method="getAverageFoliageColor",at=@At("RETURN"))
    private static int ess$frostFoliage(int color,BlockAndTintGetter level,BlockPos pos) {
        float frost=Weather.frost();
        return frost>.001f ? ess$blend(color,0xd2e4d8,frost) : color;
    }
    private static int ess$blend(int from,int to,float amount) {
        int r=ess$channel(from>>16,to>>16,amount), g=ess$channel(from>>8,to>>8,amount), b=ess$channel(from,to,amount);
        return from&0xff000000|r<<16|g<<8|b;
    }
    private static int ess$channel(int from,int to,float amount) {
        int a=from&255, b=to&255;
        return Math.clamp((int)(a+(b-a)*amount+.5f),0,255);
    }
}
