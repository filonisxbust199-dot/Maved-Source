package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.module.render.CustomCrystal;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndCrystalRenderer.class)
public abstract class CustomCrystalRendererMixin {
    @WrapOperation(method="submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void ess$crystalScale(PoseStack pose, float x, float y, float z, Operation<Void> original) {
        float size=CustomCrystal.size();
        original.call(pose,x*size,y*size,z*size);
    }

    @WrapOperation(method="submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void ess$crystalBaseAnchor(PoseStack pose, float x, float y, float z, Operation<Void> original) {
        original.call(pose,x,y/CustomCrystal.size(),z);
    }

    @ModifyExpressionValue(method="submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at=@At(value="FIELD", target="Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;beamOffset:Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 ess$crystalBeam(Vec3 original) {
        return CustomCrystal.beam() ? original : null;
    }
}
