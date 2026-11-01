package ess.ll.client.mixin;

import ess.ll.client.wardrobe.WardrobeLayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wardrobe: under a character skin the vanilla body is hidden and eases into the skin's idle pose. */
@Mixin(PlayerModel.class)
public abstract class WardrobeModelMixin {
    @Unique private boolean ess$hidden;

    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("TAIL"))
    private void ess$characterSkin(AvatarRenderState state,CallbackInfo ci) {
        PlayerModel model=(PlayerModel)(Object)this;
        boolean skin=WardrobeLayer.poseCharacter(model,state);
        if(skin || ess$hidden) {
            for(ModelPart part:new ModelPart[]{model.head,model.body,model.rightArm,model.leftArm,model.rightLeg,model.leftLeg}) part.visible=!skin;
            ess$hidden=skin;
        }
    }
}
