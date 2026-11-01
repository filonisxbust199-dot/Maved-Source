package ess.ll.client.module.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Theme;
import ess.ll.client.render.WorldFx;
import ess.ll.client.setting.Settings.*;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/** The 25 ESS sky shaders and their original animated colour palettes. */
public final class ShaderSky {
    public static final ModeSettings MODE=new ModeSettings("\u0428\u0435\u0439\u0434\u0435\u0440","Aurora","Liquid","Void","Meteors","Solar","Bloom",
            "Veil","Halo","Rift","Eclipse","Stars","Aether","Pearl","Caustic","Nebula","Storm","Sakura",
            "Celestial","Sunset","Dreamscape","Emerald","Galaxy","Twilight","Frost","Mirage");
    public static final BooleanSetting THEME_COLORS=new BooleanSetting("\u0426\u0432\u0435\u0442\u0430 \u0442\u0435\u043c\u044b",true);
    public static final SliderSetting SCALE=new SliderSetting("\u041c\u0430\u0441\u0448\u0442\u0430\u0431",.25f,4,.05f,1.15f);
    public static final SliderSetting SPEED=new SliderSetting("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c",.1f,4,.05f,.85f);
    public static final Setting[] SETTINGS={MODE,THEME_COLORS,SCALE,SPEED};
    private static final int[] PALETTE=new int[3];
    private static final Identifier SHADER=Identifier.fromNamespaceAndPath("ess-best","core/sky_shader");
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/sky_shader"))
            .withVertexShader(SHADER).withFragmentShader(SHADER)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER)
                    .withUniform("Projection",UniformType.UNIFORM_BUFFER)
                    .withUniform("SkySettings",UniformType.UNIFORM_BUFFER).build())
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false).build());
    private static long lastNanos;
    private static float time;
    private ShaderSky() {}
    public static void initialize() { /* Register the pipeline before shader reload. */ }

    public static void render() {
        var mc=Minecraft.getInstance();
        if(mc.level==null || !mc.level.dimensionType().hasSkyLight() || !WorldFx.captured()) return;
        var target=mc.gameRenderer.mainRenderTarget();
        if(target==null) return;
        long now=System.nanoTime();
        if(lastNanos!=0 && !mc.isPaused()) time=(time+Math.min(0.1f,(now-lastNanos)/1e9f))%3600f;
        lastNanos=now;
        int mode=MODE.options.indexOf(MODE.get());
        if(mode<0) mode=0;
        float t=time*SPEED.get();
        int[] colors=THEME_COLORS.get() ? buildThemePalette(mode,t) : buildPresetPalette(mode,t);
        int horizon=mix(colors[0],colors[1],.35f);
        var view=WorldFx.view();
        view.m30(0).m31(0).m32(0);
        var transforms=RenderSystem.getDynamicUniforms().writeTransform(view,new Vector4f(1),new Vector3f(),new Matrix4f());
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var projection=WorldFx.upload(Std140Builder.onStack(stack,64).putMat4f(WorldFx.projection()).get());
            var settings=WorldFx.upload(Std140Builder.onStack(stack,80)
                    .putFloat(t).putFloat(SCALE.get()).putInt(mode).align(16)
                    .putVec4(red(colors[0]),green(colors[0]),blue(colors[0]),1)
                    .putVec4(red(colors[1]),green(colors[1]),blue(colors[1]),1)
                    .putVec4(red(colors[2]),green(colors[2]),blue(colors[2]),1)
                    .putVec4(red(horizon),green(horizon),blue(horizon),1).get());
            try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"ESS ShaderSky",
                    target.getColorTextureView(),Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms",transforms);
                pass.setUniform("Projection",projection);
                pass.setUniform("SkySettings",settings);
                pass.draw(6,1,0,0);
            }
        }
    }

    private static int opaque(int color) { return color | 0xff000000; }
    private static int rgb(int r,int g,int b) { return 0xff000000 | r<<16 | g<<8 | b; }
    private static int mix(int a,int b,float f) { return Theme.mix(a,b,Math.clamp(f,0,1)); }
    private static int shade(int color,float f) { return mix(color,0xff000000,f); }
    private static int lift(int color,float f) { return mix(color,0xffffffff,f); }
    private static float pulse(float t,float speed,float phase) { return (float)(Math.sin(t*speed+phase)*.5+.5); }
    private static float red(int c) { return (c>>16&255)/255f; }
    private static float green(int c) { return (c>>8&255)/255f; }
    private static float blue(int c) { return (c&255)/255f; }

    public static int[] buildThemePalette(int mode, float time) {
        int secondary;
        int primary;
        float pulse1 = ShaderSky.pulse(time, 0.31f, 0.0f);
        float pulse2 = ShaderSky.pulse(time, 0.24f, 1.3f);
        float pulse3 = ShaderSky.pulse(time, 0.47f, 0.6f);
        int themePrimary = Theme.accent();
        int themeSecondary = Theme.background();
        int accent = switch (mode) {
            case 6 -> {
                int veilTint = ShaderSky.mix(ShaderSky.lift(themeSecondary, 0.1f), ShaderSky.rgb(213, 225, 241), 0.28f + pulse1 * 0.08f);
                primary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.28f), veilTint, 0.44f + pulse2 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.shade(themePrimary, 0.18f), ShaderSky.shade(themeSecondary, 0.36f), 0.58f + pulse1 * 0.1f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, themeSecondary, 0.6f + pulse3 * 0.1f), 0.42f + pulse3 * 0.08f);
            }
            case 7 -> {
                primary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.24f), themeSecondary, 0.5f + pulse1 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.shade(themePrimary, 0.16f), ShaderSky.shade(themeSecondary, 0.42f), 0.62f + pulse2 * 0.1f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, themeSecondary, 0.34f + pulse3 * 0.18f), 0.56f + pulse2 * 0.08f);
            }
            case 8 -> {
                int stormTint = ShaderSky.mix(themePrimary, themeSecondary, 0.3f + pulse1 * 0.18f);
                primary = ShaderSky.mix(ShaderSky.shade(stormTint, 0.18f), themePrimary, 0.54f + pulse2 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.12f), ShaderSky.shade(stormTint, 0.34f), 0.66f + pulse1 * 0.08f);
                yield ShaderSky.lift(themePrimary, 0.38f + pulse3 * 0.1f);
            }
            case 9 -> {
                int shroudTint = ShaderSky.mix(themePrimary, themeSecondary, 0.42f + pulse1 * 0.1f);
                primary = ShaderSky.mix(ShaderSky.shade(shroudTint, 0.16f), ShaderSky.shade(themeSecondary, 0.06f), 0.62f + pulse2 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.05f), ShaderSky.shade(themePrimary, 0.2f), 0.44f + pulse1 * 0.1f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, shroudTint, 0.4f + pulse3 * 0.1f), 0.66f + pulse3 * 0.06f);
            }
            case 10 -> {
                int cosmicTint = ShaderSky.mix(themePrimary, themeSecondary, 0.62f + pulse1 * 0.12f);
                primary = ShaderSky.mix(ShaderSky.shade(cosmicTint, 0.3f), themePrimary, 0.42f + pulse2 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.42f), ShaderSky.rgb(24, 12, 45), 0.42f + pulse1 * 0.1f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, ShaderSky.rgb(255, 188, 229), 0.28f + pulse3 * 0.1f), 0.52f + pulse2 * 0.08f);
            }
            case 11 -> {
                int skyTint = ShaderSky.mix(themePrimary, ShaderSky.rgb(74, 180, 205), 0.36f + pulse1 * 0.12f);
                int violetTint = ShaderSky.mix(themeSecondary, ShaderSky.rgb(121, 78, 184), 0.34f + pulse2 * 0.1f);
                primary = ShaderSky.mix(ShaderSky.shade(skyTint, 0.22f), skyTint, 0.5f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.shade(violetTint, 0.44f), ShaderSky.rgb(5, 9, 27), 0.42f + pulse2 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(skyTint, ShaderSky.rgb(178, 241, 255), 0.5f + pulse3 * 0.12f), 0.22f + pulse3 * 0.06f);
            }
            case 12 -> {
                int pearlTint = ShaderSky.mix(themeSecondary, ShaderSky.rgb(190, 211, 240), 0.42f + pulse1 * 0.1f);
                int deepBlue = ShaderSky.mix(themePrimary, ShaderSky.rgb(18, 38, 70), 0.44f + pulse2 * 0.1f);
                primary = ShaderSky.mix(ShaderSky.shade(deepBlue, 0.18f), pearlTint, 0.32f + pulse1 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.46f), ShaderSky.rgb(3, 7, 20), 0.46f + pulse2 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(pearlTint, ShaderSky.rgb(255, 247, 232), 0.4f + pulse3 * 0.12f), 0.28f + pulse3 * 0.06f);
            }
            case 13 -> {
                int aquaTint = ShaderSky.mix(themePrimary, ShaderSky.rgb(70, 220, 255), 0.46f + pulse1 * 0.1f);
                int deepOcean = ShaderSky.mix(themeSecondary, ShaderSky.rgb(5, 18, 38), 0.54f + pulse2 * 0.08f);
                primary = ShaderSky.mix(ShaderSky.shade(deepOcean, 0.12f), aquaTint, 0.38f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.34f), ShaderSky.rgb(3, 10, 24), 0.58f + pulse2 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(aquaTint, ShaderSky.rgb(188, 250, 255), 0.42f + pulse3 * 0.12f), 0.18f + pulse3 * 0.06f);
            }
            case 14 -> {
                int cosmicViolet = ShaderSky.mix(themePrimary, ShaderSky.rgb(120, 76, 255), 0.42f + pulse1 * 0.12f);
                int sunsetCore = ShaderSky.mix(themeSecondary, ShaderSky.rgb(255, 128, 72), 0.28f + pulse2 * 0.12f);
                primary = ShaderSky.mix(ShaderSky.shade(cosmicViolet, 0.2f), cosmicViolet, 0.52f + pulse1 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.42f), ShaderSky.rgb(8, 6, 24), 0.52f + pulse2 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(cosmicViolet, sunsetCore, 0.5f + pulse3 * 0.12f), 0.2f + pulse3 * 0.06f);
            }
            case 16 -> {
                int blossom = ShaderSky.mix(themePrimary, ShaderSky.rgb(255, 130, 185), 0.34f + pulse1 * 0.1f);
                primary = ShaderSky.mix(ShaderSky.shade(blossom, 0.18f), blossom, 0.58f + pulse2 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.46f), ShaderSky.rgb(18, 8, 34), 0.44f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(blossom, themeSecondary, 0.28f + pulse3 * 0.1f), 0.48f + pulse3 * 0.06f);
            }
            case 17 -> {
                int galaxy = ShaderSky.mix(themePrimary, themeSecondary, 0.56f + pulse1 * 0.1f);
                primary = ShaderSky.mix(ShaderSky.shade(galaxy, 0.24f), themePrimary, 0.48f + pulse2 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.54f), ShaderSky.rgb(4, 5, 20), 0.48f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, themeSecondary, 0.42f + pulse3 * 0.12f), 0.48f + pulse2 * 0.06f);
            }
            case 18 -> {
                int sunset = ShaderSky.mix(themePrimary, ShaderSky.rgb(255, 117, 67), 0.3f + pulse1 * 0.1f);
                primary = ShaderSky.mix(sunset, themeSecondary, 0.22f + pulse2 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.28f), ShaderSky.rgb(40, 24, 72), 0.34f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, ShaderSky.rgb(255, 194, 92), 0.34f + pulse3 * 0.1f), 0.36f + pulse3 * 0.06f);
            }
            case 19 -> {
                int dream = ShaderSky.mix(ShaderSky.lift(themePrimary, 0.12f), themeSecondary, 0.44f + pulse1 * 0.12f);
                primary = ShaderSky.mix(ShaderSky.shade(dream, 0.14f), dream, 0.58f + pulse2 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.42f), ShaderSky.shade(themePrimary, 0.3f), 0.46f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, themeSecondary, 0.6f + pulse3 * 0.1f), 0.56f + pulse3 * 0.06f);
            }
            case 20 -> {
                int energy = ShaderSky.mix(themePrimary, themeSecondary, 0.28f + pulse1 * 0.12f);
                primary = ShaderSky.mix(ShaderSky.shade(energy, 0.22f), energy, 0.54f + pulse2 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.62f), ShaderSky.rgb(2, 12, 12), 0.5f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, themeSecondary, 0.18f + pulse3 * 0.1f), 0.46f + pulse3 * 0.06f);
            }
            case 21 -> {
                int cosmic = ShaderSky.mix(themePrimary, themeSecondary, 0.52f + pulse1 * 0.12f);
                primary = ShaderSky.mix(ShaderSky.shade(cosmic, 0.26f), themePrimary, 0.46f + pulse2 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.58f), ShaderSky.rgb(4, 6, 18), 0.5f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, ShaderSky.rgb(255, 224, 168), 0.42f + pulse3 * 0.12f), 0.42f + pulse2 * 0.06f);
            }
            case 22 -> {
                int coolSky = ShaderSky.mix(themePrimary, ShaderSky.rgb(96, 122, 196), 0.34f + pulse1 * 0.1f);
                primary = ShaderSky.mix(ShaderSky.shade(coolSky, 0.16f), coolSky, 0.52f + pulse2 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.3f), ShaderSky.rgb(34, 26, 78), 0.4f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, ShaderSky.rgb(255, 150, 78), 0.44f + pulse3 * 0.1f), 0.3f + pulse3 * 0.06f);
            }
            case 23 -> {
                int ice = ShaderSky.lift(themePrimary, 0.34f + pulse1 * 0.1f);
                primary = ShaderSky.mix(ShaderSky.lift(themePrimary, 0.18f), ice, 0.52f + pulse2 * 0.08f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.1f), ShaderSky.lift(themeSecondary, 0.2f), 0.5f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, ShaderSky.rgb(226, 240, 255), 0.56f + pulse3 * 0.1f), 0.46f + pulse3 * 0.06f);
            }
            case 24 -> {
                int bleached = ShaderSky.lift(themePrimary, 0.22f + pulse1 * 0.1f);
                primary = ShaderSky.mix(bleached, themePrimary, 0.42f + pulse2 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.1f), themePrimary, 0.44f + pulse1 * 0.08f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, ShaderSky.rgb(255, 216, 150), 0.48f + pulse3 * 0.12f), 0.4f + pulse3 * 0.06f);
            }
            case 5 -> {
                int bloomTint = ShaderSky.mix(ShaderSky.lift(themePrimary, 0.1f), themeSecondary, 0.38f + pulse1 * 0.18f);
                primary = ShaderSky.mix(ShaderSky.shade(themePrimary, 0.26f), bloomTint, 0.5f + pulse2 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.2f), themeSecondary, 0.38f + pulse1 * 0.12f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, themeSecondary, 0.32f + pulse3 * 0.18f), 0.44f + pulse3 * 0.08f);
            }
            default -> {
                primary = ShaderSky.mix(ShaderSky.shade(themePrimary, 0.26f), ShaderSky.mix(ShaderSky.lift(themePrimary, 0.1f), themeSecondary, 0.38f + pulse1 * 0.18f), 0.5f + pulse2 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.shade(themeSecondary, 0.2f), themeSecondary, 0.38f + pulse1 * 0.12f);
                yield ShaderSky.lift(ShaderSky.mix(themePrimary, themeSecondary, 0.32f + pulse3 * 0.18f), 0.44f + pulse3 * 0.08f);
            }
        };
        ShaderSky.PALETTE[0] = ShaderSky.opaque(primary);
        ShaderSky.PALETTE[1] = ShaderSky.opaque(secondary);
        ShaderSky.PALETTE[2] = ShaderSky.opaque(accent);
        return PALETTE;
    }

    public static int[] buildPresetPalette(int mode, float time) {
        int secondary;
        int primary;
        float pulse1 = ShaderSky.pulse(time, 0.29f, 0.0f);
        float pulse2 = ShaderSky.pulse(time, 0.23f, 1.7f);
        float pulse3 = ShaderSky.pulse(time, 0.41f, 0.5f);
        int accent = switch (mode) {
            case 1 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(16, 42, 84), ShaderSky.rgb(38, 92, 168), 0.5f + pulse1 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.rgb(3, 9, 24), ShaderSky.rgb(12, 30, 62), 0.55f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(96, 210, 255), ShaderSky.rgb(210, 248, 255), 0.35f + pulse3 * 0.15f);
            }
            case 2 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(38, 22, 76), ShaderSky.rgb(88, 52, 150), 0.48f + pulse1 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.rgb(4, 3, 12), ShaderSky.rgb(16, 10, 34), 0.5f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(168, 128, 255), ShaderSky.rgb(255, 214, 240), 0.36f + pulse3 * 0.14f);
            }
            case 3 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(34, 52, 104), ShaderSky.rgb(60, 86, 150), 0.48f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(5, 8, 20), ShaderSky.rgb(12, 18, 38), 0.55f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(190, 214, 255), ShaderSky.rgb(240, 248, 255), 0.4f + pulse3 * 0.12f);
            }
            case 4 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(214, 84, 18), ShaderSky.rgb(255, 132, 36), 0.5f + pulse1 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.rgb(52, 10, 4), ShaderSky.rgb(94, 22, 8), 0.52f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 196, 92), ShaderSky.rgb(255, 236, 170), 0.4f + pulse3 * 0.16f);
            }
            case 6 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(18, 31, 52), ShaderSky.rgb(84, 118, 166), 0.54f + pulse2 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(6, 10, 21), ShaderSky.rgb(38, 55, 86), 0.58f + pulse1 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(176, 202, 234), ShaderSky.rgb(255, 243, 232), 0.42f + pulse3 * 0.14f);
            }
            case 7 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(11, 20, 42), ShaderSky.rgb(56, 96, 188), 0.56f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(4, 8, 20), ShaderSky.rgb(17, 34, 72), 0.68f + pulse2 * 0.08f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 205, 118), ShaderSky.rgb(255, 249, 237), 0.34f + pulse3 * 0.16f);
            }
            case 8 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(31, 5, 11), ShaderSky.rgb(153, 28, 46), 0.62f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(10, 2, 6), ShaderSky.rgb(66, 9, 20), 0.6f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 133, 118), ShaderSky.rgb(255, 214, 194), 0.32f + pulse3 * 0.14f);
            }
            case 9 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(10, 15, 29), ShaderSky.rgb(42, 63, 110), 0.52f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(4, 6, 15), ShaderSky.rgb(18, 24, 42), 0.62f + pulse2 * 0.08f);
                yield ShaderSky.mix(ShaderSky.rgb(205, 219, 255), ShaderSky.rgb(255, 214, 153), 0.28f + pulse3 * 0.14f);
            }
            case 10 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(43, 13, 71), ShaderSky.rgb(132, 67, 154), 0.5f + pulse1 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.rgb(7, 4, 22), ShaderSky.rgb(58, 20, 74), 0.46f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 174, 226), ShaderSky.rgb(207, 228, 255), 0.36f + pulse3 * 0.18f);
            }
            case 11 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(9, 25, 48), ShaderSky.rgb(67, 180, 203), 0.56f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(4, 7, 22), ShaderSky.rgb(48, 24, 82), 0.52f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(146, 235, 255), ShaderSky.rgb(194, 138, 255), 0.34f + pulse3 * 0.16f);
            }
            case 12 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(12, 25, 48), ShaderSky.rgb(90, 125, 172), 0.52f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(3, 7, 20), ShaderSky.rgb(28, 47, 78), 0.5f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(182, 209, 242), ShaderSky.rgb(255, 248, 231), 0.36f + pulse3 * 0.14f);
            }
            case 13 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(8, 28, 52), ShaderSky.rgb(42, 176, 212), 0.54f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(2, 10, 24), ShaderSky.rgb(18, 56, 92), 0.46f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(110, 235, 255), ShaderSky.rgb(220, 252, 255), 0.38f + pulse3 * 0.14f);
            }
            case 14 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(24, 10, 58), ShaderSky.rgb(116, 72, 188), 0.56f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(5, 4, 20), ShaderSky.rgb(34, 18, 72), 0.52f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 144, 92), ShaderSky.rgb(202, 170, 255), 0.42f + pulse3 * 0.16f);
            }
            case 16 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(110, 42, 92), ShaderSky.rgb(225, 111, 164), 0.54f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(13, 8, 31), ShaderSky.rgb(58, 30, 78), 0.5f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 184, 216), ShaderSky.rgb(255, 244, 237), 0.4f + pulse3 * 0.14f);
            }
            case 17 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(38, 22, 105), ShaderSky.rgb(116, 76, 214), 0.52f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(3, 5, 20), ShaderSky.rgb(20, 16, 64), 0.56f + pulse2 * 0.08f);
                yield ShaderSky.mix(ShaderSky.rgb(112, 218, 255), ShaderSky.rgb(239, 205, 255), 0.38f + pulse3 * 0.16f);
            }
            case 18 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(214, 65, 66), ShaderSky.rgb(255, 132, 74), 0.5f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(39, 25, 76), ShaderSky.rgb(103, 49, 98), 0.48f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 183, 72), ShaderSky.rgb(255, 239, 180), 0.38f + pulse3 * 0.14f);
            }
            case 19 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(78, 70, 160), ShaderSky.rgb(205, 132, 220), 0.52f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(10, 15, 48), ShaderSky.rgb(47, 52, 112), 0.5f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(137, 230, 255), ShaderSky.rgb(255, 221, 246), 0.42f + pulse3 * 0.14f);
            }
            case 20 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(4, 72, 54), ShaderSky.rgb(28, 196, 127), 0.52f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(1, 9, 12), ShaderSky.rgb(5, 35, 31), 0.54f + pulse2 * 0.08f);
                yield ShaderSky.mix(ShaderSky.rgb(104, 255, 178), ShaderSky.rgb(222, 255, 213), 0.38f + pulse3 * 0.16f);
            }
            case 21 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(46, 60, 120), ShaderSky.rgb(96, 116, 196), 0.5f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(3, 5, 16), ShaderSky.rgb(12, 16, 40), 0.55f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 214, 150), ShaderSky.rgb(255, 244, 214), 0.4f + pulse3 * 0.14f);
            }
            case 22 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(70, 96, 168), ShaderSky.rgb(126, 142, 206), 0.48f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(28, 30, 74), ShaderSky.rgb(60, 46, 104), 0.5f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 138, 66), ShaderSky.rgb(255, 196, 112), 0.42f + pulse3 * 0.14f);
            }
            case 23 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(150, 186, 224), ShaderSky.rgb(196, 220, 246), 0.5f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(64, 98, 140), ShaderSky.rgb(110, 148, 190), 0.52f + pulse2 * 0.08f);
                yield ShaderSky.mix(ShaderSky.rgb(206, 232, 255), ShaderSky.rgb(245, 251, 255), 0.42f + pulse3 * 0.12f);
            }
            case 24 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(96, 150, 214), ShaderSky.rgb(150, 190, 236), 0.5f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(64, 120, 190), ShaderSky.rgb(110, 160, 214), 0.5f + pulse2 * 0.08f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 206, 128), ShaderSky.rgb(255, 238, 196), 0.42f + pulse3 * 0.14f);
            }
            case 5 -> {
                primary = ShaderSky.mix(ShaderSky.rgb(9, 17, 30), ShaderSky.rgb(58, 178, 168), 0.52f + pulse2 * 0.12f);
                secondary = ShaderSky.mix(ShaderSky.rgb(17, 33, 50), ShaderSky.rgb(114, 87, 152), 0.24f + pulse1 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(255, 206, 189), ShaderSky.rgb(255, 243, 228), 0.4f + pulse3 * 0.16f);
            }
            default -> {
                primary = ShaderSky.mix(ShaderSky.rgb(24, 46, 92), ShaderSky.rgb(58, 96, 158), 0.48f + pulse1 * 0.1f);
                secondary = ShaderSky.mix(ShaderSky.rgb(4, 8, 22), ShaderSky.rgb(14, 26, 52), 0.55f + pulse2 * 0.1f);
                yield ShaderSky.mix(ShaderSky.rgb(64, 224, 160), ShaderSky.rgb(150, 255, 208), 0.4f + pulse3 * 0.16f);
            }
        };
        ShaderSky.PALETTE[0] = ShaderSky.opaque(primary);
        ShaderSky.PALETTE[1] = ShaderSky.opaque(secondary);
        ShaderSky.PALETTE[2] = ShaderSky.opaque(accent);
        return PALETTE;
    }
}
