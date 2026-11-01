package ess.ll.client.integration;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.culling.Frustum;

public interface LevelRendererAccess {
    Frustum frustum();

    RenderTarget hexRawOutlineTarget();
}
