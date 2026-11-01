package ess.ll.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.module.ClientModules;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * Frosted glass for every blurred surface in the client. The frame behind the GUI is copied once per frame, only
 * when a glass element was extracted, and each surface blurs it in its own shader, under its tint. The radius is
 * in pixels of the frame, from Темы → Сила блюра.
 */
public final class GlassRenderer {
    private static final VertexFormat FORMAT=VertexFormat.builder(0)
            .addAttribute("Position",GpuFormat.RGB32_FLOAT).addAttribute("UV0",GpuFormat.RG32_FLOAT)
            .addAttribute("UV3",GpuFormat.RG32_FLOAT).addAttribute("UV1",GpuFormat.RG16_SINT)
            .addAttribute("Color",GpuFormat.RGBA8_UNORM).build();
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/glass"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/glass"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/glass"))
            .withVertexBinding(0,FORMAT).withCull(false).build());
    private static final RenderPipeline BAND=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/plate_band"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/glass"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/plate_band"))
            .withVertexBinding(0,FORMAT).withCull(false).build());
    /** The frame behind the GUI, which surfaces sample. */
    private static TextureTarget frame;
    private static boolean requested, failed;
    private GlassRenderer() {}

    public record Style(int tint) {
        public static final Style HUD=new Style(0x8c0e0f14);
        public static final Style PANEL=new Style(0x8c0e0f14);
        public static final Style CARD=new Style(0x2affffff);
    }
    public static void initialize() { /* Forces pipeline registration before shader reload. */ }

    public static void glass(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,Style style) {
        glass(g,x,y,w,h,radius,style.tint,1);
    }
    /** Tint alpha is the mix strength over the blurred backdrop; opacity fades the whole surface. */
    public static void glass(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,int tint,float opacity) {
        if(w<=0 || h<=0 || failed || opacity<=.004f) return;
        requested=true;
        // UV1.y packs the blur radius in tenths of a pixel (low 10 bits) and opacity (high 5 bits, /31).
        int packed=Math.round(Math.clamp(opacity,0,1)*31)*1024+Math.round(ClientModules.BLUR_POWER.get()*10);
        if(frame!=null) surface(g,PIPELINE,x,y,w,h,radius,packed,tint);
    }
    /**
     * A band of a plate drawn as a surface: the same rounded shape, cut off limit units from its left edge, or from
     * its top edge when top is set. Its corners and edges land exactly on the plate's, at any scale.
     */
    public static void band(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,boolean top,float limit,int color) {
        if(w<=0 || h<=0 || (color>>>24)==0) return;
        // UV1.y packs the limit in tenths (low 10 bits) and the edge it is measured from (high 5 bits).
        surface(g,BAND,x,y,w,h,radius,(top ? 31 : 0)*1024+Math.clamp(Math.round(limit*10),0,1023),color);
    }
    private static void surface(GuiGraphicsExtractor g,RenderPipeline pipeline,float x,float y,float w,float h,float radius,int packed,int color) {
        var pose=new Matrix3x2f(g.pose());
        var clip=g.scissorStack.peek();
        float pad=1;
        var bounds=new ScreenRectangle((int)Math.floor(x-pad),(int)Math.floor(y-pad),(int)Math.ceil(w+2*pad)+1,(int)Math.ceil(h+2*pad)+1).transformMaxBounds(pose);
        if(clip!=null) bounds=bounds.intersection(clip);
        if(bounds!=null) g.guiRenderState.addGuiElement(new Surface(pipeline,pose,x-pad,y-pad,w+2*pad,h+2*pad,w/2,h/2,
                Math.clamp(radius,0,Math.min(w,h)/2)*(UiRenderer.circular ? -1 : 1),packed,color,clip,bounds));
    }
    /**
     * Called at the head of GuiRenderer.render, before any GUI vertices are built. The backdrop is
     * the world frame, or source when given (the main menu scene, which is drawn inside the GUI pass
     * and so is not in the main target yet). Surfaces sample it in normalised screen space, so its
     * resolution may differ from the window.
     */
    public static void beforeGuiRender(RenderTarget source) {
        if(!requested) return;
        requested=false;
        try {
            RenderTarget from=source!=null ? source : Minecraft.getInstance().gameRenderer.mainRenderTarget();
            frame=WorldFx.target(frame,"ESS glass frame",from.width,from.height);
            WorldFx.copyColor(from,frame);
        } catch(Exception e) {
            failed=true;
            System.err.println("[ESS] Glass disabled: "+e);
        }
    }
    private record Surface(RenderPipeline pipeline,Matrix3x2fc pose,float x,float y,float w,float h,float halfW,float halfH,float radius,int packed,int tint,
                           ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public TextureSetup textureSetup() {
            if(pipeline==BAND) return TextureSetup.noTexture();
            return TextureSetup.singleTexture(frame.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        }
        public void buildVertices(VertexConsumer v) {
            float cx=x+w/2,cy=y+h/2;
            vertex(v,x,y,cx,cy); vertex(v,x,y+h,cx,cy); vertex(v,x+w,y+h,cx,cy); vertex(v,x+w,y,cx,cy);
        }
        private void vertex(VertexConsumer v,float px,float py,float cx,float cy) {
            v.addVertexWith2DPose(pose,px,py).setUv(px-cx,py-cy).setUv3(halfW,halfH)
                    .setUv1(Math.round(radius*16),packed).setColor(tint);
        }
    }
}
