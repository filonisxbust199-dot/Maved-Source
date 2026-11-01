package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.event.SwingTransformEvent;
import ess.ll.client.event.ViewModelOffsetEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class CombatHandsMixin {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="submitHandsWithItems",
        at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;xRot:F"))
    private float ess$visibleHandPitch(float original) {
        var mc=Minecraft.getInstance();
        return mc.player!=null && mc.getCameraEntity()==mc.player ? mc.gameRenderer.mainCamera().xRot() : original;
    }

    @Shadow private void applyItemArmAttackTransform(PoseStack pose,HumanoidArm arm,float swing) {}

    /** 26.3 skips swingArm while idle; the source renderer still applied the swing's rest pose. */
    @Inject(method="submitArmWithItem",at=@At(value="INVOKE",
            target="Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
            ordinal=4,shift=Shift.AFTER))
    private void ess$restSwingPose(PlayerRenderState player,FirstPersonHandsAndItemsRenderState items,
            float tickDelta,float equip,InteractionHand hand,float swing,ItemStack stack,float armHeight,
            PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        var avatar=player.avatarRenderState;
        if(avatar==null)return;
        var current=avatar.currentSwing;
        if(current!=null && current.hand()==hand
                && current.animation().type()==net.minecraft.world.item.SwingAnimationType.WHACK)return;
        HumanoidArm arm=hand==InteractionHand.MAIN_HAND ? avatar.mainArm : avatar.mainArm.getOpposite();
        SwingTransformEvent.INSTANCE.fireSwingOffset(pose,arm,0.0f,()->{});
    }

    @Inject(method="submitArmWithItem",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",shift=Shift.AFTER))
    private void ess$viewModel(PlayerRenderState player, FirstPersonHandsAndItemsRenderState items,
            float tickDelta,float equip,InteractionHand hand,float swing,ItemStack stack,float armHeight,
            PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        var local=Minecraft.getInstance().player;
        if(local!=null) ViewModelOffsetEvent.INSTANCE.fire(local,hand,pose);
    }

    @Redirect(method="swingArm",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void ess$swingTranslate(PoseStack pose,float x,float y,float z) {
        SwingTransformEvent.INSTANCE.fireTranslate(pose,x,y,z);
    }

    @Redirect(method="swingArm",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"))
    private void ess$swingTransform(FirstPersonHandsAndItemsRenderer renderer,PoseStack pose,HumanoidArm arm,float swing) {
        SwingTransformEvent.INSTANCE.fireSwingOffset(pose,arm,swing,()->applyItemArmAttackTransform(pose,arm,swing));
    }

    @Redirect(method="applyItemArmTransform",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void ess$equip(PoseStack pose,float x,float y,float z,PoseStack original,HumanoidArm arm,float equip) {
        SwingTransformEvent.INSTANCE.fireEquipOffset(pose,x,y,z,arm==HumanoidArm.RIGHT?1:-1,equip);
    }
}
