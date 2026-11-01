package ess.ll.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Player heads (face + hat layer) clipped to a rounded square, with alpha for fades. */
public final class Avatar {
    private static final VertexFormat FORMAT=VertexFormat.builder(0)
            .addAttribute("Position",GpuFormat.RGB32_FLOAT).addAttribute("UV0",GpuFormat.RG32_FLOAT)
            .addAttribute("UV3",GpuFormat.RG32_FLOAT).addAttribute("UV1",GpuFormat.RG16_SINT)
            .addAttribute("Color",GpuFormat.RGBA8_UNORM).build();
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/avatar"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/avatar"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/avatar"))
            .withVertexBinding(0,FORMAT).withCull(false).build());
    private Avatar() {}
    public static void initialize() { /* Forces pipeline registration before shader reload. */ }

    /** Draws the head at x, y; alpha fades both layers. */
    public static void draw(GuiGraphicsExtractor g,PlayerSkin skin,float x,float y,float size,float radius,float alpha) {
        if(skin==null || alpha<=.004f) return;
        var tex=Minecraft.getInstance().getTextureManager().getTexture(skin.body().texturePath());
        var setup=TextureSetup.singleTexture(tex.getTextureView(),tex.getSampler());
        int color=(Math.round(Math.clamp(alpha,0,1)*255)<<24)|0xffffff;
        // 64x64 skin: face at (8,8), hat overlay at (40,8), both 8x8.
        quad(g,setup,x,y,size,radius,8/64f,8/64f,8/64f,color);
        quad(g,setup,x,y,size,radius,40/64f,8/64f,8/64f,color);
    }
    /** Any whole texture (album art and the like) clipped to the same rounded square. */
    public static void image(GuiGraphicsExtractor g,Identifier texture,float x,float y,float size,float radius,float alpha) {
        if(texture==null || alpha<=.004f) return;
        var tex=Minecraft.getInstance().getTextureManager().getTexture(texture);
        int color=(Math.round(Math.clamp(alpha,0,1)*255)<<24)|0xffffff;
        quad(g,TextureSetup.singleTexture(tex.getTextureView(),tex.getSampler()),x,y,size,radius,0,0,1,color);
    }
    private static void quad(GuiGraphicsExtractor g,TextureSetup setup,float x,float y,float size,float radius,float u,float v,float span,int color) {
        var pose=new Matrix3x2f(g.pose());
        var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle((int)Math.floor(x),(int)Math.floor(y),(int)Math.ceil(size)+1,(int)Math.ceil(size)+1).transformMaxBounds(pose);
        if(clip!=null) bounds=bounds.intersection(clip);
        if(bounds!=null) g.guiRenderState.addGuiElement(new Face(pose,setup,x,y,size,u,v,span,Math.round(size/2*16),Math.round(Math.min(radius,size/2)*16)*(UiRenderer.circular ? -1 : 1),color,clip,bounds));
    }
    private record Face(Matrix3x2fc pose,TextureSetup textureSetup,float x,float y,float size,float u,float v,float span,int half,int radius,int color,
                        ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return PIPELINE; }
        public void buildVertices(VertexConsumer c) {
            float d=span,s=size/2;
            c.addVertexWith2DPose(pose,x,y).setUv(u,v).setUv3(-s,-s).setUv1(half,radius).setColor(color);
            c.addVertexWith2DPose(pose,x,y+size).setUv(u,v+d).setUv3(-s,s).setUv1(half,radius).setColor(color);
            c.addVertexWith2DPose(pose,x+size,y+size).setUv(u+d,v+d).setUv3(s,s).setUv1(half,radius).setColor(color);
            c.addVertexWith2DPose(pose,x+size,y).setUv(u+d,v).setUv3(s,-s).setUv1(half,radius).setColor(color);
        }
    }
}
