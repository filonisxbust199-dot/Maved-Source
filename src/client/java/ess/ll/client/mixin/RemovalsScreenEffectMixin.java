package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.event.VisualRemovalEvent;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class RemovalsScreenEffectMixin {
    @Inject(method="submitFire",at=@At("HEAD"),cancellable=true)
    private static void ess$fire(PoseStack pose,SubmitNodeCollector collector,TextureAtlasSprite sprite,CallbackInfo ci) {
        VisualRemovalEvent.INSTANCE.cancelIfRemoved(VisualRemovalEvent.Type.FIRE,ci);
    }
}
