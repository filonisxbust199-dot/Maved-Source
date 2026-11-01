package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.module.render.BlockOverlay;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** BlockOverlay draws its own outline of the aimed block; vanilla's is skipped while it is on. */
@Mixin(LevelRenderer.class)
public class BlockOverlayMixin {
    @Inject(method="submitBlockOutline",at=@At("HEAD"),cancellable=true)
    private void ess$skipOutline(PoseStack pose,SubmitNodeCollector collector,LevelRenderState state,CallbackInfo ci) {
        if(BlockOverlay.enabled()) ci.cancel();
    }
}
