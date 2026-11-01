package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.WardrobeLayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wardrobe in first person: a character skin replaces your arm, and clothing and accessories on the arm (sleeves,
 * watches) are drawn over it.
 */
@Mixin(AvatarRenderer.class)
public abstract class WardrobeHandMixin {
    @Inject(method="renderHand",at=@At("HEAD"),cancellable=true)
    private void ess$skinArm(PoseStack pose,SubmitNodeCollector collector,int light,Identifier skin,ModelPart arm,boolean sleeve,CallbackInfo ci) {
        var model=(PlayerModel)((AvatarRenderer)(Object)this).getModel();
        if(WardrobeLayer.firstPersonArm(model,arm,pose,collector,light,true)) ci.cancel();
    }
    @Inject(method="renderHand",at=@At("TAIL"))
    private void ess$clothingArm(PoseStack pose,SubmitNodeCollector collector,int light,Identifier skin,ModelPart arm,boolean sleeve,CallbackInfo ci) {
        var model=(PlayerModel)((AvatarRenderer)(Object)this).getModel();
        WardrobeLayer.firstPersonArm(model,arm,pose,collector,light,false);
    }
}
