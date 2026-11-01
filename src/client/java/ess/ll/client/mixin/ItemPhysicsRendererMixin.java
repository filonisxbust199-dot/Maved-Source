package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ess.ll.client.module.render.ItemPhysics;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ItemPhysics: hands vanilla's lift and spin of a dropped item over to the module. */
@Mixin(ItemEntityRenderer.class)
public class ItemPhysicsRendererMixin {
    private static final String SUBMIT="submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
            +"Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",at=@At("TAIL"))
    private void ess$extract(ItemEntity entity,ItemEntityRenderState state,float partial,CallbackInfo ci) {
        if(state instanceof ItemPhysics.State extra) extra.ess$set(entity.onGround(),entity.getId());
    }

    @Inject(method=SUBMIT,at=@At("HEAD"))
    private void ess$begin(ItemEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci) {
        ItemPhysics.begin(state);
    }

    @Redirect(method=SUBMIT,at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void ess$lift(PoseStack pose,float x,float y,float z) { ItemPhysics.translate(pose,x,y,z); }

    @Redirect(method=SUBMIT,at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;rotate(Lcom/mojang/math/Axis;F)V"))
    private void ess$spin(PoseStack pose,Axis axis,float angle) { ItemPhysics.rotate(pose,axis,angle); }
}
