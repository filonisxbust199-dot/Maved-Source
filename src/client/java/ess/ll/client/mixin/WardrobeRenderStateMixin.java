package ess.ll.client.mixin;

import ess.ll.client.wardrobe.WardrobeLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wardrobe: the skin layers (hat, jacket, sleeves, pants, cape) the worn items replace are hidden. */
@Mixin(AvatarRenderer.class)
public abstract class WardrobeRenderStateMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
    private void ess$wardrobeHides(Avatar avatar,AvatarRenderState state,float partial,CallbackInfo ci) {
        WardrobeLayer.hideParts(state);
    }
}
