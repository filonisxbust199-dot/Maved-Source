package ess.ll.client.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;

/** Public 26.3 render pipeline access for the transferred renderer. */
public final class DimasikRenderPipelinesAccessor {
    private DimasikRenderPipelinesAccessor() {}
    public static RenderPipeline.Snippet getPositionColorSnippet() { return RenderPipelines.DEBUG_FILLED_SNIPPET; }
    public static RenderPipeline.Snippet getOutlineSnippet() { return RenderPipelines.DEBUG_FILLED_SNIPPET; }
    public static RenderPipeline register(RenderPipeline pipeline) { return RenderPipelines.register(pipeline); }
}
