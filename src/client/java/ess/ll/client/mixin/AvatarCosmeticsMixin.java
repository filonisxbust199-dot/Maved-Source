package ess.ll.client.mixin;

import ess.ll.client.wardrobe.WardrobeLayer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the wardrobe layer to every player renderer (wide and slim). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarCosmeticsMixin extends LivingEntityRenderer<Avatar,AvatarRenderState,PlayerModel> {
    private AvatarCosmeticsMixin(EntityRendererProvider.Context context,PlayerModel model,float shadow) { super(context,model,shadow); }

    @Inject(method="<init>",at=@At("TAIL"))
    private void ess$addCosmetics(EntityRendererProvider.Context context,boolean slim,CallbackInfo ci) {
        addLayer(new WardrobeLayer(this));
    }
}
