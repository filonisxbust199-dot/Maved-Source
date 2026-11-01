package ess.ll.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;

/**
 * One pipeline for the whole ESS UI (core/ui): rounded shapes, their shadows and outlines as signed-distance quads,
 * MSDF text and line icons. Everything binds the same texture pair (the UI and ESP font atlases), so the GUI
 * renderer batches a whole layer of panels, text and icons into a single draw.
 */
public final class UiBatch {
    static final int FILL=0, SHADOW=1, OUTLINE=2, TEXT=3, ICON=4, CIRCULAR=8, SECOND_FONT=16;
    static final VertexFormat FORMAT=VertexFormat.builder(0)
            .addAttribute("Position",GpuFormat.RGB32_FLOAT).addAttribute("Color",GpuFormat.RGBA8_UNORM)
            .addAttribute("UV0",GpuFormat.RG32_FLOAT).addAttribute("UV3",GpuFormat.RG32_FLOAT)
            .addAttribute("UV1",GpuFormat.RG16_SINT).addAttribute("UV2",GpuFormat.RG16_SINT).build();
    static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/ui"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/ui"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/ui"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Sampler1",UniformType.COMBINED_IMAGE_SAMPLER).build())
            .withVertexBinding(0,FORMAT).withCull(false).build());
    private static final Identifier UI_ATLAS=Identifier.fromNamespaceAndPath("ess-best","textures/font/notosans.png");
    private static final Identifier ESP_ATLAS=Identifier.fromNamespaceAndPath("ess-best","textures/font/sf_rounded_bold.png");
    /** The last texture pair built, reused while the atlases are the same textures (equal setups batch). */
    private static GpuTextureView lastFirst, lastSecond;
    private static TextureSetup last;

    private UiBatch() {}
    public static void initialize() { /* Forces pipeline registration before shader reload. */ }

    /**
     * Whether core/ui is compiled. The game's first shader preload cannot see mod resources, and the ESS loading
     * screen draws before the real load finishes; until then shapes fall back to vanilla geometry.
     */
    static boolean ready() {
        try { return com.mojang.blaze3d.systems.RenderSystem.getCompiledPipelineNullable(PIPELINE)!=null; }
        catch(IllegalStateException e) { return false; }
    }

    /** The UI and ESP atlases: shapes, icons and the two HUD fonts all share this setup. */
    static TextureSetup textures() { return textures(UI_ATLAS); }
    /** A font atlas as the first texture (fonts other than the HUD pair), the ESP atlas second. */
    static TextureSetup textures(Identifier first) {
        var manager=Minecraft.getInstance().getTextureManager();
        var a=manager.getTexture(first);
        var b=manager.getTexture(ESP_ATLAS);
        if(last!=null && a.getTextureView()==lastFirst && b.getTextureView()==lastSecond) return last;
        lastFirst=a.getTextureView(); lastSecond=b.getTextureView();
        last=TextureSetup.doubleTexture(lastFirst,a.getSampler(),lastSecond,b.getSampler());
        return last;
    }

    /** One vertex: x, y in the element's local GUI space. */
    static void vertex(VertexConsumer out,Matrix3x2fc pose,float x,float y,int color,float u,float v,float hw,float hh,int flags,int a,int b) {
        out.addVertexWith2DPose(pose,x,y).setColor(color).setUv(u,v).setUv3(hw,hh).setUv1(flags,a).setUv2(b,0);
    }
    /**
     * A signed-distance quad of a shape centred on cx, cy with half size hw, hh, covering x0..x1, y0..y1 (the shape and
     * its feather or blur). A gradient that is not a straight two-stop ramp over the quad is split into strips.
     */
    static void shapeQuad(VertexConsumer out,Matrix3x2fc pose,float cx,float cy,float hw,float hh,
                          float x0,float y0,float x1,float y1,int flags,int radius16,int extra16,Gradient paint) {
        if(paint.from()==paint.to()) {
            int c=paint.from();
            corner(out,pose,x0,y0,cx,cy,hw,hh,flags,radius16,extra16,c);
            corner(out,pose,x0,y1,cx,cy,hw,hh,flags,radius16,extra16,c);
            corner(out,pose,x1,y1,cx,cy,hw,hh,flags,radius16,extra16,c);
            corner(out,pose,x1,y0,cx,cy,hw,hh,flags,radius16,extra16,c);
            return;
        }
        boolean horizontal=paint.axis()==Gradient.Axis.HORIZONTAL;
        float from=horizontal ? x0 : y0, to=horizontal ? x1 : y1;
        int strips=Math.max(1,Math.min(64,(int)Math.ceil((to-from)/6)));
        for(int i=0;i<strips;i++) {
            float a=from+(to-from)*i/strips, b=from+(to-from)*(i+1)/strips;
            if(horizontal) {
                corner(out,pose,a,y0,cx,cy,hw,hh,flags,radius16,extra16,paint.at(a,y0));
                corner(out,pose,a,y1,cx,cy,hw,hh,flags,radius16,extra16,paint.at(a,y1));
                corner(out,pose,b,y1,cx,cy,hw,hh,flags,radius16,extra16,paint.at(b,y1));
                corner(out,pose,b,y0,cx,cy,hw,hh,flags,radius16,extra16,paint.at(b,y0));
            } else {
                corner(out,pose,x0,a,cx,cy,hw,hh,flags,radius16,extra16,paint.at(x0,a));
                corner(out,pose,x0,b,cx,cy,hw,hh,flags,radius16,extra16,paint.at(x0,b));
                corner(out,pose,x1,b,cx,cy,hw,hh,flags,radius16,extra16,paint.at(x1,b));
                corner(out,pose,x1,a,cx,cy,hw,hh,flags,radius16,extra16,paint.at(x1,a));
            }
        }
    }
    private static void corner(VertexConsumer out,Matrix3x2fc pose,float x,float y,float cx,float cy,float hw,float hh,
                               int flags,int radius16,int extra16,int color) {
        vertex(out,pose,x,y,color,x-cx,y-cy,hw,hh,flags,radius16,extra16);
    }
    static int fixed(float value) { return Math.clamp(Math.round(value*16),0,32767); }
}
