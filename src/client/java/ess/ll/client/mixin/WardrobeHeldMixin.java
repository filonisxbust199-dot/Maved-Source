package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.WardrobeHeld;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wardrobe: a held weapon or item with a worn skin is drawn as that skin in first person. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class WardrobeHeldMixin {
    @WrapOperation(method="submitArmWithItem",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void ess$weaponSkin(ItemStackRenderState state,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,
                                Operation<Void> original,@Local(argsOnly=true) ItemStack stack) {
        if(!WardrobeHeld.firstPerson(stack,state,pose,collector,light)) original.call(state,pose,collector,light,overlay,outline);
    }
}
