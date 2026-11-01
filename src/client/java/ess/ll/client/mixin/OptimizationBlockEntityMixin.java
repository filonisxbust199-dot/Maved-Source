package ess.ll.client.mixin;

import ess.ll.client.module.misc.Optimization;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optimization "Дальние блок-сущности": chests, signs, banners and the like past the set range are not drawn. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class OptimizationBlockEntityMixin {
    @Unique private Vec3 ess$camera=Vec3.ZERO;

    @Inject(method="prepare",at=@At("HEAD"))
    private void ess$camera(Vec3 camera,CallbackInfo ci) { ess$camera=camera; }

    @Inject(method="tryExtractRenderState",at=@At("HEAD"),cancellable=true)
    private <E extends BlockEntity,S extends BlockEntityRenderState> void ess$far(E blockEntity,float partial,
            ModelFeatureRenderer.CrumblingOverlay crumbling,boolean outline,CallbackInfoReturnable<S> cir) {
        if(Optimization.cullBlockEntity(Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(ess$camera))) cir.setReturnValue(null);
    }
}
