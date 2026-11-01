package ess.ll.client.mixin;

import ess.ll.client.features.emote.Emotes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: the playing emote poses the player model over the vanilla animation. */
@Mixin(PlayerModel.class)
public abstract class EmotePlayerModelMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("TAIL"))
    private void ess$emote(AvatarRenderState state,CallbackInfo ci) {
        Emotes.apply(state.id,(HumanoidModel<?>)(Object)this);
    }
}
