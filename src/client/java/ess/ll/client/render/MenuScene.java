package ess.ll.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.Optional;

/**
 * Raymarched scene behind the main menu. It is rendered into its own target right before the GUI
 * is drawn, then stretched over the screen (menu_upscale: bicubic + sharpening).
 *
 * Animated styles render every frame at a resolution that follows the frame rate. Still styles
 * render at full resolution with a sub-pixel jitter and blend each frame into the target with
 * weight 1/n: the running average converges to an anti-aliased, noise-free image, after which the
 * scene is not rendered at all until the window size or style changes.
 *
 * Per-frame values travel in the DynamicTransforms block: ColorModulator = (time, aspect, mouse x,
 * mouse y), ModelOffset = (blend weight, jitter x, jitter y) in target UV units; animated styles have no jitter
 * and get the menu's light theme (0..1) in its place.
 */
public final class MenuScene {
    /** Available backgrounds, in the order the menu cycles through them. */
    public enum Style {
        MOUNTAINS("Горы","menu_mountains",1,true), FIR("Ёлка","menu_scene",.6f,false),
        PLASMA("Плазма","menu_plasma",1,false), TILES("Разбитые плитки","menu_tiles",1,false), AURORA("Аврора","menu_aurora",1,false),
        SILK("Шёлк","menu_silk",1,false), HEX("Гексы","menu_hex",1,false), GRID("Сетка","menu_grid",1,true);
        public final String label;
        final RenderPipeline pipeline;
        /** Initial scene resolution relative to the window; animated styles adjust it by frame rate. */
        final float resolution;
        /** A still image: accumulated over jittered frames instead of re-rendered every frame. */
        public final boolean still;
        Style(String label,String shader,float resolution,boolean still) {
            this.label=label;
            this.resolution=resolution;
            this.still=still;
            pipeline=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/"+shader))
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/"+shader))
                    .withColorTargetState(still ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT)
                    .build());
        }
    }
    public static Style style=Style.MOUNTAINS;
    /** The menu's light theme, 0..1, for the animated styles. */
    public static float light;
    /** Bicubic + sharpening stretch of the scene over the screen. */
    private static final RenderPipeline UPSCALE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/menu_upscale"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/menu_upscale"))
            .build());
    /**
     * Samples averaged into a still image. Each frame renders one quarter of the target (TILES), so a
     * frame costs a quarter of a full render and the image converges after SAMPLES * TILES frames.
     */
    private static final int SAMPLES=48, TILES=4;
    /**
     * Dynamic resolution for animated styles: the scale rises while frames come in at (close to) the
     * frame cap and drops when they do not, judged over windows of FRAME_WINDOW frames.
     */
    private static final float MIN_SCALE=.3f, MAX_SCALE=1, SCALE_STEP=.05f;
    private static final int FRAME_WINDOW=40;
    private static float scale=-1;
    private static Style scaledStyle;
    private static long lastFrame, windowStart;
    private static int windowFrames, accumulated;
    private static RenderTarget target;
    private static boolean requested, failed;
    private static float time, aspect, mouseX, mouseY, intro;
    private MenuScene() {}
    public static void initialize() { Style.values(); /* Forces pipeline registration before shader reload. */ }
    /** Current scene resolution relative to the window (for diagnostics). */
    public static float scale() { return scale; }
    /** Samples blended into the still image so far (for diagnostics). */
    public static int accumulated() { return accumulated/TILES; }

    /**
     * Seconds drive the animation (wrapped to keep float precision), mouse is -1..1 for parallax,
     * intro fades the scene in from black.
     */
    public static void draw(GuiGraphicsExtractor g,int width,int height,double seconds,float mouseX,float mouseY,float intro) {
        if(failed || !ensureTarget()) return;
        requested=true;
        MenuScene.time=(float)(seconds%3600);
        MenuScene.aspect=(float)width/Math.max(1,height);
        MenuScene.mouseX=Math.clamp(mouseX,-1,1);
        MenuScene.mouseY=Math.clamp(mouseY,-1,1);
        MenuScene.intro=Math.clamp(intro,0,1);
        var pose=new Matrix3x2f(g.pose());
        g.guiRenderState.addGuiElement(new Blit(pose,width,height,MenuScene.intro,new ScreenRectangle(0,0,width,height).transformMaxBounds(pose)));
    }
    /**
     * Called at the head of GuiRenderer.render, before any GUI vertices are built. Returns the scene
     * target when the menu shows it this frame, so blurred surfaces can use it as their backdrop.
     */
    public static RenderTarget beforeGuiRender() {
        if(!requested) return null;
        requested=false;
        try {
            adaptScale();
            var window=Minecraft.getInstance().getWindow();
            int w=Math.max(1,Math.round(window.getWidth()*scale)), h=Math.max(1,Math.round(window.getHeight()*scale));
            if(target.width!=w || target.height!=h) { target.resize(w,h); accumulated=0; }
            if(style.still && accumulated>=SAMPLES*TILES) return target;
            float weight=1, jitterX=0, jitterY=0;
            int tile=-1;
            if(style.still) {
                // Running average with a Halton (2, 3) sub-pixel pattern, one quadrant per frame.
                int sample=accumulated/TILES;
                tile=accumulated%TILES;
                weight=1f/(sample+1);
                jitterX=(halton(sample+1,2)-.5f)/w;
                jitterY=(halton(sample+1,3)-.5f)/h;
                accumulated++;
            }
            var transforms=RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(),new Vector4f(time,aspect,mouseX,mouseY),
                    new Vector3f(weight,style.still ? jitterX : light,jitterY),new Matrix4f());
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            try(var pass=encoder.createRenderPass(()->"ESS menu scene",target.getColorTextureView(),Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(style.pipeline));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms",transforms);
                if(tile>=0) {
                    int halfW=(w+1)/2, halfH=(h+1)/2;
                    pass.enableScissor((tile%2)*halfW,(tile/2)*halfH,halfW,halfH);
                }
                pass.draw(3,1,0,0);
            }
            return target;
        } catch(Exception e) {
            failed=true;
            System.err.println("[ESS] Menu scene disabled: "+e);
            return null;
        }
    }
    private static float halton(int index,int base) {
        float result=0, f=1;
        while(index>0) { f/=base; result+=f*(index%base); index/=base; }
        return result;
    }
    private static void adaptScale() {
        long now=System.nanoTime();
        // A new style, or a pause of more than a second (menu was closed), restarts the measurement.
        if(scaledStyle!=style || now-lastFrame>1_000_000_000L) {
            if(scaledStyle!=style) { scale=style.resolution; accumulated=0; }
            scaledStyle=style;
            windowStart=now;
            windowFrames=0;
        }
        lastFrame=now;
        if(style.still) return;
        if(++windowFrames<FRAME_WINDOW) return;
        // An inactive window is throttled by the game; its frame rate says nothing about the GPU.
        if(!Minecraft.getInstance().isWindowActive()) { windowStart=now; windowFrames=0; return; }
        double fps=windowFrames/((now-windowStart)/1e9);
        int cap=Minecraft.getInstance().options.framerateLimit().get();
        // The menu is capped at 60 at most; reaching ~95% of the cap means there is headroom.
        double target=Math.min(60,cap>=260 ? 60 : cap);
        if(fps<target*.82) scale=Math.max(MIN_SCALE,scale-SCALE_STEP);
        else if(fps>target*.95) scale=Math.min(MAX_SCALE,scale+SCALE_STEP);
        windowStart=now;
        windowFrames=0;
    }
    private static boolean ensureTarget() {
        if(target!=null) return true;
        try {
            var window=Minecraft.getInstance().getWindow();
            target=new TextureTarget("ESS menu scene",Math.max(1,Math.round(window.getWidth()*style.resolution)),
                    Math.max(1,Math.round(window.getHeight()*style.resolution)),GpuFormat.RGBA8_UNORM,null);
            return true;
        } catch(Exception e) {
            failed=true;
            System.err.println("[ESS] Menu scene disabled: "+e);
            return false;
        }
    }
    private record Blit(Matrix3x2fc pose,float w,float h,float intro,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return UPSCALE; }
        public TextureSetup textureSetup() {
            return TextureSetup.singleTexture(target.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        }
        public ScreenRectangle scissorArea() { return null; }
        public void buildVertices(VertexConsumer v) {
            // RGB fades the scene in from black; alpha is the sharpening strength for menu_upscale
            // (none at native resolution).
            int grey=Math.round(intro*255);
            int color=(Math.round(Math.clamp((1/Math.max(scale,.01f)-1)*1.2f,0,1)*255)<<24)|(grey<<16)|(grey<<8)|grey;
            // Render targets have v = 0 at the bottom.
            v.addVertexWith2DPose(pose,0,0).setUv(0,1).setColor(color);
            v.addVertexWith2DPose(pose,0,h).setUv(0,0).setColor(color);
            v.addVertexWith2DPose(pose,w,h).setUv(1,0).setColor(color);
            v.addVertexWith2DPose(pose,w,0).setUv(1,1).setColor(color);
        }
    }
}
