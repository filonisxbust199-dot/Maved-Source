package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.WardrobeWorld;
import ess.ll.client.module.render.CustomCrystal;
import ess.ll.client.module.render.CustomCrystalEffects;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wardrobe: placed end crystals are drawn as the worn crystal skin (the beam stays vanilla). */
@Mixin(EndCrystalRenderer.class)
public abstract class WardrobeCrystalMixin {
    @WrapOperation(method="submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;III)V"))
    private void ess$crystalSkin(SubmitNodeCollector collector,Model<?> model,Object modelState,PoseStack pose,Identifier texture,int light,int overlay,int outline,
                                 Operation<Void> original,EndCrystalRenderState state,PoseStack statePose,SubmitNodeCollector stateCollector,CameraRenderState camera) {
        Identifier custom=CustomCrystal.textureOverride();
        if(CustomCrystalEffects.replaces(state)) return;
        if(custom!=null) original.call(collector,model,modelState,pose,custom,light,overlay,outline);
        else if(!WardrobeWorld.crystal(state,pose,collector)) original.call(collector,model,modelState,pose,texture,light,overlay,outline);
    }
}
