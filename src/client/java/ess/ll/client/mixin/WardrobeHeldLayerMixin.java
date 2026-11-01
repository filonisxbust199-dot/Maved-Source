package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.WardrobeHeld;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wardrobe: your held weapon or item with a worn skin is drawn as that skin in third person and previews. */
@Mixin(ItemInHandLayer.class)
public abstract class WardrobeHeldLayerMixin {
    @WrapOperation(method="submitArmWithItem",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void ess$weaponSkin(ItemStackRenderState itemState,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,
                                Operation<Void> original,@Local(argsOnly=true) ArmedEntityRenderState state,@Local(argsOnly=true) ItemStack stack) {
        if(state instanceof AvatarRenderState avatar && WardrobeHeld.thirdPerson(avatar.id,stack,avatar.isUsingItem,avatar.ticksUsingItem,
                avatar.maxCrossbowChargeDuration,avatar.ageInTicks,itemState,pose,collector,light)) return;
        original.call(itemState,pose,collector,light,overlay,outline);
    }
}
