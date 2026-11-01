package ess.ll.client.mixin;

import ess.ll.client.module.render.ShaderESP;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ShaderESP: its targets glow (are drawn into the entity outline buffer). */
@Mixin(Minecraft.class)
public abstract class ShaderEspMixin {
    @Inject(method="shouldEntityAppearGlowing",at=@At("HEAD"),cancellable=true)
    private void ess$shaderEspGlowing(Entity entity,CallbackInfoReturnable<Boolean> cir) {
        if(ShaderESP.glows(entity)) cir.setReturnValue(true);
    }
}
