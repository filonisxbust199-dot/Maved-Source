package ess.ll.client.mixin;

import ess.ll.client.module.render.DeathEffect;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skips the body of an entity that DeathEffect has replaced with cubes or fireflies. */
@Mixin(EntityRenderDispatcher.class)
public class DeathEffectMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private <E extends Entity> void ess$hideDead(E entity,Frustum frustum,double x,double y,double z,float partialTicks,CallbackInfoReturnable<Boolean> cir) {
        if(DeathEffect.hidden(entity)) cir.setReturnValue(false);
    }
}
