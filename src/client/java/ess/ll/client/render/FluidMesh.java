package ess.ll.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * A liquid layer inside a rounded box, filled from the left up to a wavy edge. Drawn as one horizontal
 * quad per pixel row, so the edge can follow any curve and the rounded corners clip the left side.
 */
public final class FluidMesh {
    private FluidMesh() {}
    /** Edge position (box-local x) for a row centre y. */
    public interface Edge { float at(float y); }

    /**
     * @param from left edge of the layer (box-local), so a caller can fill the body separately
     * @param top colour at the top of the box, bottom colour at its bottom (vertical gradient)
     */
    public static void draw(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float from,Edge edge,int top,int bottom) {
        int rows=Math.max(1,(int)Math.ceil(h*3));
        float step=h/rows;
        var pose=new Matrix3x2f(g.pose());
        // About one screen pixel of soft fringe past the edge, so the wave is anti-aliased instead of stair-stepped.
        float fringe=1/Math.max(.5f,(float)Math.hypot(pose.m00(),pose.m01()));
        float[] xy=new float[rows*16];
        int[] colors=new int[rows*8];
        int n=0;
        for(int i=0;i<rows;i++) {
            float y0=i*step, y1=y0+step;
            float c0r=inset(y0,h,radius), c1r=inset(y1,h,radius);
            float in0=Math.max(from,c0r), in1=Math.max(from,c1r);
            float e0=Math.min(edge.at(y0),w-c0r), e1=Math.min(edge.at(y1),w-c1r);
            if(e0<=in0 && e1<=in1) continue;
            e0=Math.max(e0,in0); e1=Math.max(e1,in1);
            int c0=mix(top,bottom,y0/h), c1=mix(top,bottom,y1/h);
            float[] q={x+in0,y+y0, x+in1,y+y1, x+e1,y+y1, x+e0,y+y0};
            System.arraycopy(q,0,xy,n*8,8);
            colors[n*4]=c0; colors[n*4+1]=c1; colors[n*4+2]=c1; colors[n*4+3]=c0;
            n++;
            float f0=Math.min(e0+fringe,w-c0r), f1=Math.min(e1+fringe,w-c1r);
            if(f0>e0 || f1>e1) {
                float[] s={x+e0,y+y0, x+e1,y+y1, x+Math.max(e1,f1),y+y1, x+Math.max(e0,f0),y+y0};
                System.arraycopy(s,0,xy,n*8,8);
                colors[n*4]=c0; colors[n*4+1]=c1; colors[n*4+2]=c1&0xffffff; colors[n*4+3]=c0&0xffffff;
                n++;
            }
        }
        if(n==0) return;
        var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle((int)Math.floor(x)-1,(int)Math.floor(y)-1,(int)Math.ceil(w)+3,(int)Math.ceil(h)+3).transformMaxBounds(pose);
        if(clip!=null) bounds=bounds.intersection(clip);
        if(bounds!=null) g.guiRenderState.addGuiElement(new Mesh(pose,xy,colors,n,clip,bounds));
    }
    /** A thin line through points {x0,y0,x1,y1,...}: one quad per segment, offset along the segment normal. */
    public static void polyline(GuiGraphicsExtractor g,float[] points,float thickness,int color) {
        int segments=points.length/2-1;
        if(segments<=0 || (color>>>24)==0) return;
        float[] xy=new float[segments*8];
        int[] colors=new int[segments*4];
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE,h=thickness/2;
        for(int i=0;i<segments;i++) {
            float x0=points[i*2],y0=points[i*2+1],x1=points[i*2+2],y1=points[i*2+3];
            float dx=x1-x0,dy=y1-y0,len=Math.max(1e-4f,(float)Math.hypot(dx,dy));
            // Each segment is stretched by a little so neighbours overlap without gaps at the joints.
            float nx=-dy/len*h,ny=dx/len*h,ex=dx/len*h*.5f,ey=dy/len*h*.5f;
            // Same winding as the liquid rows (top-left, bottom-left, bottom-right, top-right); the GUI pipeline culls the other.
            float[] q={x0-ex-nx,y0-ey-ny, x0-ex+nx,y0-ey+ny, x1+ex+nx,y1+ey+ny, x1+ex-nx,y1+ey-ny};
            System.arraycopy(q,0,xy,i*8,8);
            for(int k=0;k<4;k++) {
                colors[i*4+k]=color;
                minX=Math.min(minX,q[k*2]); maxX=Math.max(maxX,q[k*2]); minY=Math.min(minY,q[k*2+1]); maxY=Math.max(maxY,q[k*2+1]);
            }
        }
        var pose=new Matrix3x2f(g.pose());
        var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle((int)Math.floor(minX)-1,(int)Math.floor(minY)-1,(int)Math.ceil(maxX-minX)+3,(int)Math.ceil(maxY-minY)+3).transformMaxBounds(pose);
        if(clip!=null) bounds=bounds.intersection(clip);
        if(bounds!=null) g.guiRenderState.addGuiElement(new Mesh(pose,xy,colors,segments,clip,bounds));
    }
    /**
     * How far a rounded corner cuts into the row at height y. UiRenderer corners are superellipses of degree 4
     * (|x|^4 + |y|^4 = r^4), so the liquid follows the same curve as the card under it.
     */
    private static float inset(float y,float h,float r) {
        r=Math.min(r,h/2);
        float d=y<r ? r-y : y>h-r ? y-(h-r) : 0;
        return d<=0 ? 0 : r-(float)Math.pow(Math.max(0,Math.pow(r,4)-Math.pow(d,4)),.25);
    }
    private static int mix(int a,int b,float t) {
        t=Math.clamp(t,0,1);
        int r=0;
        for(int s=0;s<32;s+=8) {
            int ca=(a>>>s)&255, cb=(b>>>s)&255;
            r|=Math.round(ca+(cb-ca)*t)<<s;
        }
        return r;
    }
    private record Mesh(Matrix3x2fc pose,float[] xy,int[] colors,int count,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        public void buildVertices(VertexConsumer out) {
            for(int i=0;i<count*4;i++) out.addVertexWith2DPose(pose,xy[i*2],xy[i*2+1]).setColor(colors[i]);
        }
    }
}
