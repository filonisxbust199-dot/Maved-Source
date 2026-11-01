package ess.ll.client.render.targetfx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

/** World-only context for the source renderer. No preview target or UI is installed. */
public final class TargetEspPreviewContext {
    private TargetEspPreviewContext() {}
    public static Camera camera() { return Minecraft.getInstance().gameRenderer.mainCamera(); }
    public static RenderTarget target() { return Minecraft.getInstance().gameRenderer.mainRenderTarget(); }
}
