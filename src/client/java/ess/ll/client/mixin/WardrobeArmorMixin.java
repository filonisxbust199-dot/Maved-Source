package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.WardrobeLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wardrobe: armour is not drawn over a character skin (unless "Броня на скинах" is on). */
@Mixin(HumanoidArmorLayer.class)
public abstract class WardrobeArmorMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",at=@At("HEAD"),cancellable=true)
    private void ess$hideArmorOnSkins(PoseStack pose,SubmitNodeCollector collector,int light,HumanoidRenderState state,float yRot,float xRot,CallbackInfo ci) {
        if(state instanceof AvatarRenderState avatar && WardrobeLayer.hidesArmor(avatar)) ci.cancel();
    }
}
