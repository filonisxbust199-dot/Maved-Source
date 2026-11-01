package ess.ll.client.integration;

import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;

public interface RenderSubmissionAccess {
    SimpleFeatureRenderPhase hexBeforeClear();
}
