package ess.ll.client.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.IdentityHashMap;
import java.util.Map;

public final class RenderPipelineCache {
    private static final Map<RenderPipeline, Boolean> COMPILED_PIPELINES = new IdentityHashMap<>();

    public static boolean isCompiled(RenderPipeline renderPipeline) {
        if (renderPipeline == null) {
            return false;
        }
        Boolean cached = COMPILED_PIPELINES.get(renderPipeline);
        if (cached != null) {
            return cached;
        }
        boolean sx;
        try {
            sx = RenderSystem.getCompiledPipelineNullable(renderPipeline) != null;
        } catch (Throwable throwable) {
            sx = false;
        }
        COMPILED_PIPELINES.put(renderPipeline, sx);
        return sx;
    }

    public static void clear() {
        COMPILED_PIPELINES.clear();
    }

    private RenderPipelineCache() {
    }
}
