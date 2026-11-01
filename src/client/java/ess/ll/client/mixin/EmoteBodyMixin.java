package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.features.emote.Emotes;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: the "body" part of an emote turns the whole player. */
@Mixin(AvatarRenderer.class)
public abstract class EmoteBodyMixin {
    @Inject(method="setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",at=@At("TAIL"))
    private void ess$emoteBody(AvatarRenderState state,PoseStack pose,float bodyRot,float scale,CallbackInfo ci) {
        Emotes.applyBody(state.id,pose);
    }
}
