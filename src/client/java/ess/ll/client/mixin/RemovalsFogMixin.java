package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ess.ll.client.event.VisualRemovalEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public abstract class RemovalsFogMixin {
    @ModifyExpressionValue(method="getFogType",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/Camera;getFluidInCamera()Lnet/minecraft/world/level/material/FogType;"))
    private FogType ess$fluidFog(FogType type) {
        if(type==FogType.WATER)return VisualRemovalEvent.INSTANCE.filterFog(type,VisualRemovalEvent.Type.WATER_FOG);
        if(type==FogType.LAVA)return VisualRemovalEvent.INSTANCE.filterFog(type,VisualRemovalEvent.Type.LAVA_FOG);
        return type;
    }
    @ModifyExpressionValue(method="computeFogColor",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/GameRenderer;nightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float ess$nightVisionFog(float strength) {
        return VisualRemovalEvent.INSTANCE.isRemoved(VisualRemovalEvent.Type.NIGHT_VISION_FOG)?0:strength;
    }
}
