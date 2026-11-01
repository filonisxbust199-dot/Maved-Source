package ess.ll.client.mixin;

import ess.ll.client.module.misc.Optimization;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optimization "Дальние сущности": mobs, items and the like past the set range are not drawn. */
@Mixin(EntityRenderDispatcher.class)
public abstract class OptimizationCullingMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private <E extends Entity> void ess$far(E entity,Frustum frustum,double camX,double camY,double camZ,float partial,CallbackInfoReturnable<Boolean> cir) {
        if(Optimization.cullEntity(entity,camX,camY,camZ)) cir.setReturnValue(false);
    }
}
