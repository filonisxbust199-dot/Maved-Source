package ess.ll.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Rounded shapes as signed-distance quads in the shared UI pipeline; straight-alpha ARGB, local GUI coordinates. */
public final class UiRenderer {
    public enum Shadow {
        SOFT(18,0x38071524), DENSE(6,0x85000000), COMPACT(3,0xa0071524), GLOW(13,0x605ecfff);
        final float blur;
        final int color;
        Shadow(float blur,int color) { this.blur=blur; this.color=color; }
    }
    /**
     * Circular corners instead of the UI's L4 squircle for everything drawn while set (shapes, shadows, glass
     * and player heads); TargetCard turns it on to match the reference's plain rounded rectangles.
     */
    static boolean circular;
    private UiRenderer() {}
    public static void rect(GuiGraphicsExtractor g,float x,float y,float w,float h,int color) {
        rounded(g,x,y,w,h,0,color);
    }
    public static void rounded(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,int color) {
        if((color>>>24)==0) return;
        rounded(g,x,y,w,h,radius,Gradient.solid(color));
    }
    public static void rounded(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,Gradient paint) {
        if(w<=0 || h<=0) return;
        submit(g,x,y,w,h,radius(w,h,radius),paint,0,0,0,feather(g)*2);
    }
    /** A true circle (rounded() draws squircle corners). */
    public static void circle(GuiGraphicsExtractor g,float cx,float cy,float r,int color) {
        circular=true;
        try { rounded(g,cx-r,cy-r,r*2,r*2,r,color); } finally { circular=false; }
    }
    public static void circleOutline(GuiGraphicsExtractor g,float cx,float cy,float r,float width,int color) {
        circular=true;
        try { outline(g,cx-r,cy-r,r*2,r*2,r,width,color); } finally { circular=false; }
    }
    /** Outline only, drawn inside the shape's edge; lets a translucent fill keep its border without a plate behind it. */
    public static void outline(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float width,int color) {
        if(w<=0 || h<=0 || width<=0 || (color>>>24)==0) return;
        submit(g,x,y,w,h,radius(w,h,radius),Gradient.solid(color),width,0,0,feather(g)*2);
    }
    /** One element for both shadow and surface. */
    public static void panel(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,Gradient paint,Shadow style) {
        if(w<=0 || h<=0) return;
        submit(g,x,y,w,h,radius(w,h,radius),paint,0,style.blur,style.color,Math.max(feather(g)*2,style.blur));
    }
    public static void shadow(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float blur,int color) {
        if(w<=0 || h<=0 || (color>>>24)==0) return;
        if(blur<=0) { rounded(g,x,y,w,h,radius,color); return; }
        submit(g,x,y,w,h,radius(w,h,radius),null,0,blur,color,blur);
    }
    public static void flowingLine(GuiGraphicsExtractor g,float x,float y,float width,float height,double phase) {
        if(width<=0 || height<=0) return;
        submit(g,x,y,width,height,0,Gradient.flow(0xff52caff,0xffffffff,Gradient.Axis.HORIZONTAL,x,width,phase),0,0,0,0);
    }
    /** One window pixel in the current pose's units. */
    public static float pixel(GuiGraphicsExtractor g) { return feather(g); }
    private static float radius(float w,float h,float radius) { return Math.clamp(radius,0,Math.min(w,h)/2); }
    private static float feather(GuiGraphicsExtractor g) {
        float scale=(float)Math.hypot(g.pose().m00(),g.pose().m01());
        return 1f/Math.max(1f,scale*Minecraft.getInstance().getWindow().getGuiScale());
    }
    private static ScreenRectangle bounds(Matrix3x2fc pose,ScreenRectangle clip,float x,float y,float w,float h,float pad) {
        var bounds=new ScreenRectangle((int)Math.floor(x-pad),(int)Math.floor(y-pad),
                (int)Math.ceil(w+2*pad)+1,(int)Math.ceil(h+2*pad)+1).transformMaxBounds(pose);
        return clip==null ? bounds : bounds.intersection(clip);
    }
    /**
     * One shape element: an optional shadow (blur > 0) and the surface (paint, or none for a bare shadow), filled or,
     * with an outline width, stroked. pad grows the bounds for the feather or blur.
     */
    private static void submit(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,Gradient paint,
                               float outline,float blur,int shadowColor,float pad) {
        var pose=new Matrix3x2f(g.pose());
        var clip=g.scissorStack.peek();
        var bounds=bounds(pose,clip,x,y,w,h,pad);
        if(bounds==null) return;
        if(!UiBatch.ready()) { legacy(g,pose,clip,bounds,x,y,w,h,radius,paint,outline,blur,shadowColor); return; }
        g.guiRenderState.addGuiElement(new Shape(pose,x,y,w,h,radius,circular,paint,outline,blur,shadowColor,
                feather(g)*2,UiBatch.textures(),clip,bounds));
    }
    /** Before core/ui is compiled (the first loading screen): the same shape as a vanilla-pipeline mesh. */
    private static void legacy(GuiGraphicsExtractor g,Matrix3x2fc pose,ScreenRectangle clip,ScreenRectangle bounds,float x,float y,float w,float h,
                               float radius,Gradient paint,float outline,float blur,int shadowColor) {
        float aa=feather(g);
        float[] shadow=blur>0 ? UiGeometry.mesh(w,h,radius,blur,true,circular) : null;
        float[] surface=paint==null ? null : outline>0 ? UiGeometry.ring(w,h,radius,outline,aa,circular) : UiGeometry.mesh(w,h,radius,aa,false,circular);
        g.guiRenderState.addGuiElement(new LegacyShape(pose,x,y,surface,paint,shadow,shadowColor,clip,bounds));
    }
    private record LegacyShape(Matrix3x2fc pose,float x,float y,float[] mesh,Gradient paint,float[] shadow,int shadowColor,
                               ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return net.minecraft.client.renderer.RenderPipelines.GUI; }
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        public void buildVertices(VertexConsumer out) {
            if(shadow!=null) emit(out,shadow,null,shadowColor);
            if(mesh!=null) emit(out,mesh,paint,paint.from());
        }
        private void emit(VertexConsumer out,float[] data,Gradient gradient,int solid) {
            boolean flat=gradient==null || gradient.from()==gradient.to();
            for(int i=0;i<data.length;i+=3) {
                float px=x+data[i],py=y+data[i+1];
                int color=flat ? solid : gradient.at(px,py);
                int alpha=Math.round((color>>>24)*data[i+2]);
                out.addVertexWith2DPose(pose,px,py).setColor((color&0xffffff)|(alpha<<24));
            }
        }
    }
    private record Shape(Matrix3x2fc pose,float x,float y,float w,float h,float radius,boolean circular,Gradient paint,float outline,
                         float blur,int shadowColor,float aa,TextureSetup textureSetup,ScreenRectangle scissorArea,ScreenRectangle bounds)
            implements GuiElementRenderState {
        public RenderPipeline pipeline() { return UiBatch.PIPELINE; }
        public void buildVertices(VertexConsumer out) {
            float hw=w/2, hh=h/2, cx=x+hw, cy=y+hh;
            int corners=circular ? UiBatch.CIRCULAR : 0, r16=UiBatch.fixed(radius);
            if(blur>0 && (shadowColor>>>24)!=0)
                UiBatch.shapeQuad(out,pose,cx,cy,hw,hh,x-blur,y-blur,x+w+blur,y+h+blur,UiBatch.SHADOW|corners,r16,UiBatch.fixed(blur),Gradient.solid(shadowColor));
            if(paint==null || (paint.from()>>>24)==0 && (paint.to()>>>24)==0) return;
            // Square corners are drawn hard, without a feather, as before.
            float pad=radius>0 || outline>0 ? aa : 0;
            UiBatch.shapeQuad(out,pose,cx,cy,hw,hh,x-pad,y-pad,x+w+pad,y+h+pad,(outline>0 ? UiBatch.OUTLINE : UiBatch.FILL)|corners,r16,
                    UiBatch.fixed(outline),paint);
        }
    }
}
