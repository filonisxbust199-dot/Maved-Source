package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.WardrobeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Elytra would cover a wardrobe cape or back item, so your elytra is hidden while one is worn (not while gliding). */
@Mixin(WingsLayer.class)
public abstract class WingsCosmeticsMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",at=@At("HEAD"),cancellable=true)
    private void ess$hideUnderCosmetics(PoseStack pose,SubmitNodeCollector collector,int light,HumanoidRenderState state,float yRot,float xRot,CallbackInfo ci) {
        if(state instanceof AvatarRenderState avatar && WardrobeLayer.hidesElytra(avatar)) ci.cancel();
    }
}
