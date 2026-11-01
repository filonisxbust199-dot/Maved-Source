package ess.ll.client.weather;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.module.render.WorldTweaks;
import ess.ll.client.render.WorldFx;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/**
 * Full-screen weather passes from Zenith on the 26.3 renderer: the sky (weather_sky) over the frame before terrain,
 * and after the level either the wet gloss (weather_gloss, a two-pass depth-aware blur, weather_gloss_combine with
 * mist and the grade) or just the grade (world_grade). All of them read one uniform block, WeatherFrame.
 */
public final class WeatherFx {
    private static final RenderPipeline SKY = WorldFx.pipeline("weather_sky", Optional.empty(), "WeatherFrame", "InSampler");
    private static final RenderPipeline GLOSS = WorldFx.pipeline("weather_gloss", Optional.empty(), "WeatherFrame", "InSampler", "TerrainDepthSampler");
    private static final RenderPipeline BLUR = WorldFx.pipeline("weather_gloss_blur", Optional.empty(), "WeatherFrame", "InSampler", "DepthSampler");
    private static final RenderPipeline COMBINE = WorldFx.pipeline("weather_gloss_combine", Optional.empty(), "WeatherFrame", "InSampler", "TerrainDepthSampler", "GlossSampler", "DepthSampler");
    private static final RenderPipeline GRADE = WorldFx.pipeline("world_grade", Optional.empty(), "WorldGrade", "SceneSampler", "DepthSampler");
    private static TextureTarget scene, gloss, glossSwap;

    /**
     * The fog of this frame, as the grade needs it to lift the fog off before re-saturating.
     */
    private static float fogEnvStart = Float.MAX_VALUE, fogEnvEnd = Float.MAX_VALUE, fogStart = Float.MAX_VALUE, fogEnd = Float.MAX_VALUE;
    private static final Vector4f fogColor = new Vector4f();
    private static final float[] tint = { 1, 1, 1, 1 };

    private WeatherFx() {
    }

    public static float tanHalfFov() {
        float m11 = WorldFx.projection().m11();
        return m11 == 0 ? .7f : 1 / m11;
    }

    public static float aspect() {
        Matrix4f p = WorldFx.projection();
        return p.m00() == 0 ? 1 : p.m11() / p.m00();
    }

    public static void captureFog(FogData data) {
        if (data == null)
            return;
        fogEnvStart = data.environmentalStart;
        fogEnvEnd = data.environmentalEnd;
        fogStart = data.renderDistanceStart;
        fogEnd = data.renderDistanceEnd;
    }

    public static void captureFogColor(Vector4f color) {
        fogColor.set(color);
    }

    /**
     * Frees the targets while nothing uses them.
     */
    public static void release() {
        if (scene != null) {
            scene.destroyBuffers();
            scene = null;
        }
        if (gloss != null) {
            gloss.destroyBuffers();
            gloss = null;
        }
        if (glossSwap != null) {
            glossSwap.destroyBuffers();
            glossSwap = null;
        }
        SnowMesh.clear();
    }

    /**
     * Before the terrain: the weather sky over the vanilla one.
     */
    public static void sky() {
        if (!Weather.active() || !WorldFx.captured())
            return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        if (!WorldFx.usable(main) || mc.gameRenderer.mainCamera().getFluidInCamera() != FogType.NONE)
            return;
        scene = WorldFx.target(scene, "Weather scene", main.width, main.height);
        WorldFx.copyColor(main, scene);
        GpuBufferSlice frame = frame(main, 0, 0);
        WorldFx.draw("Weather sky", main, SKY, "WeatherFrame", frame, "InSampler", scene.getColorTextureView(), FilterMode.LINEAR);
    }

    /**
     * After the level (before outlines): the wet gloss with the grade, or the grade alone, then the weather itself.
     */
    public static void afterLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !WorldFx.captured())
            return;
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        if (!WorldFx.usable(main))
            return;
        boolean underwater = mc.gameRenderer.mainCamera().getFluidInCamera() != FogType.NONE;
        if (Weather.active() && Weather.glossActive() && !underwater && Weather.wet() * Weather.channel().floatValue23 > .005f)
            gloss(main);
        else
            grade(main);
        Weather.render(WorldFx.view());
    }

    private static float saturation() {
        float world = WorldTweaks.saturation();
        return Weather.active() ? world * Weather.saturation() : world;
    }

    private static void grade(RenderTarget main) {
        if (Weather.active())
            Weather.tint(tint);
        else {
            tint[0] = tint[1] = tint[2] = tint[3] = 1;
        }
        float saturation = saturation();
        if (Math.abs(saturation - 1) < .01f && tint[0] == 1 && tint[1] == 1 && tint[2] == 1 && tint[3] == 1)
            return;
        scene = WorldFx.target(scene, "Weather scene", main.width, main.height);
        WorldFx.copyColor(main, scene);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var data = WorldFx.upload(Std140Builder.onStack(stack, 32).putVec4(saturation, 0, 0, 0).putVec4(tint[0], tint[1], tint[2], tint[3]).get());
            WorldFx.draw("World grade", main, GRADE, "WorldGrade", data, "SceneSampler", scene.getColorTextureView(), FilterMode.NEAREST, "DepthSampler", main.getDepthTextureView(), FilterMode.NEAREST);
        }
    }

    private static void gloss(RenderTarget main) {
        Weather.tint(tint);
        int w = main.width, h = main.height;
        scene = WorldFx.target(scene, "Weather scene", w, h);
        gloss = WorldFx.target(gloss, "Weather gloss", w, h);
        glossSwap = WorldFx.target(glossSwap, "Weather gloss swap", w, h);
        var depth = main.getDepthTextureView();
        WorldFx.draw("Weather gloss", gloss, GLOSS, "WeatherFrame", frame(main, 0, 0), "InSampler", depth, FilterMode.NEAREST, "TerrainDepthSampler", depth, FilterMode.NEAREST);
        WorldFx.draw("Weather gloss blur", glossSwap, BLUR, "WeatherFrame", frame(main, 1, 0), "InSampler", gloss.getColorTextureView(), FilterMode.LINEAR, "DepthSampler", depth, FilterMode.NEAREST);
        WorldFx.draw("Weather gloss blur", gloss, BLUR, "WeatherFrame", frame(main, 0, 1), "InSampler", glossSwap.getColorTextureView(), FilterMode.LINEAR, "DepthSampler", depth, FilterMode.NEAREST);
        WorldFx.copyColor(main, scene);
        WorldFx.draw("Weather combine", main, COMBINE, "WeatherFrame", frame(main, 0, 0), "InSampler", scene.getColorTextureView(), FilterMode.LINEAR, "TerrainDepthSampler", depth, FilterMode.NEAREST, "GlossSampler", gloss.getColorTextureView(), FilterMode.LINEAR, "DepthSampler", depth, FilterMode.NEAREST);
    }

    private static float[] color(int rgb) {
        return new float[] { (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f };
    }

    /**
     * The WeatherFrame block for this frame (blur direction in pixels as given).
     */
    private static GpuBufferSlice frame(RenderTarget main, float blurX, float blurY) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.mainCamera();
        var eye = camera.position();
        WeatherChannel c = Weather.channel();
        Matrix4f p = WorldFx.projection();
        float fade = Weather.fade();
        float far = mc.options.getEffectiveRenderDistance() * 16f * 4f;
        float fadeEnd = c.flag ? Math.min(far, Math.max(c.floatValue14 * 1.6f, 192)) : Math.clamp(c.floatValue14 * 1.1f, 24, Math.min(96, far));
        float coverRadius = c.flag ? (SnowMesh.isFlag() ? SnowCover.maxRenderDistance() : 1e6f) : 0;
        float frost = c.flag ? Math.clamp(c.floatValue23, 0, 1) * Math.clamp(fade, 0, 1) * .55f : 0;
        float[] wetTint = color(c.intValue5), mistTint = color(c.intValue8);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var b = Std140Builder.onStack(stack, 28 * 16);
            b.putVec4(fade, Weather.time(), (float) Math.toRadians(camera.xRot()), (float) (Math.PI - Math.toRadians(camera.yRot())));
            b.putVec4(tanHalfFov(), aspect(), p.m22(), p.m32());
            b.putVec4((float) eye.x, (float) eye.y, (float) eye.z, 0);
            vec(b, c.floatValues);
            vec(b, c.floatValues2);
            vec(b, c.floatValues3);
            vec(b, c.floatValues4);
            vec(b, c.floatValues5);
            b.putVec4(c.floatValue2, c.floatValue3, c.floatValue4, c.floatValue5);
            b.putVec4(c.floatValue6, c.floatValue7, c.floatValue, Weather.flash());
            vec(b, c.floatValues7);
            vec(b, c.floatValues8);
            b.putVec4(c.floatValue8, c.floatValue9, c.floatValue10, c.floatValue11);
            b.putVec4(c.floatValue12, frost, 0, far);
            vec(b, c.floatValues9);
            vec(b, c.floatValues6);
            vec(b, c.floatValues10);
            b.putVec4(Weather.wet() * c.floatValue23, (c.intValue4 >>> 24 & 255) / 255f, c.floatValue24, c.floatValue26);
            b.putVec4(c.floatValue25, c.flag ? 1 : 0, 0, 0);
            b.putVec4(fadeEnd, coverRadius, c.intValue2 == 1 ? c.floatValue18 : 0, c.floatValue27 * fade);
            b.putVec4(wetTint[0], wetTint[1], wetTint[2], 0);
            b.putVec4(mistTint[0], mistTint[1], mistTint[2], 0);
            b.putVec4((float) (eye.y - 1.6), 3.2f, c.floatValue20, c.floatValue21);
            b.putVec4(saturation(), fogEnvStart, fogEnvEnd, fogStart);
            b.putVec4(fogEnd, blurX, blurY, .05f);
            b.putVec4(tint[0], tint[1], tint[2], tint[3]);
            b.putVec4(fogColor.x, fogColor.y, fogColor.z, fogColor.w);
            b.putVec4(main.width, main.height, main.width, main.height);
            return WorldFx.upload(b.get());
        }
    }

    private static void vec(Std140Builder b, float[] v) {
        b.putVec4(v[0], v[1], v[2], 0);
    }
}
