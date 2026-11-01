package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.WardrobePets;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wardrobe: your pet is drawn with the entities. */
@Mixin(LevelRenderer.class)
public abstract class WardrobePetMixin {
    @Inject(method="submitEntities",at=@At("TAIL"))
    private void ess$pet(PoseStack pose,LevelRenderState state,SubmitNodeCollector collector,CallbackInfo ci) {
        WardrobePets.render(pose,collector);
        ess.ll.client.wardrobe.WardrobeWorld.anchors(pose,collector);
    }
}
