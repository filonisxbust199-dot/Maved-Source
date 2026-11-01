package ess.ll.client.mixin;

import ess.ll.client.module.render.ShaderESP;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** ShaderESP: each target's outline is drawn in its ShaderESP colour instead of its team colour. */
@Mixin(EntityRenderer.class)
public abstract class ShaderEspColorMixin {
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int ess$shaderEspColor(Entity entity) {
        return ShaderESP.glows(entity) ? ShaderESP.color(entity) : entity.getTeamColor();
    }
}
