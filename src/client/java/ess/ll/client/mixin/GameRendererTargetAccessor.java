package ess.ll.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Bots: the bot camera pass draws the level into its own target in place of the main one. */
@Mixin(GameRenderer.class)
public interface GameRendererTargetAccessor {
    @Mutable @Accessor("mainRenderTarget") void ess$setMainRenderTarget(RenderTarget target);
}
