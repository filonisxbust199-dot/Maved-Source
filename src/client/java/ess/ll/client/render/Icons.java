package ess.ll.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Resolution-independent stroke icons evaluated as signed distance fields (24-unit Lucide grid). */
public final class Icons {
    public enum Icon {
        CHECK, CHEVRON_DOWN, CHEVRON_RIGHT, HEART_FILLED, HEART, SHIELD, SWORD, CLOCK, PIN, USER,
        BOLT, SIGNAL, COMPASS, FLASK, EYE, GRID, MOVE, CLOSE, CROSSHAIR, SHIELD_FILLED, RING, DOT, ARC, KEYBOARD, PALETTE, CHEVRON_LEFT, SEARCH, STAR,
        PLAY, PAUSE, NEXT, PREVIOUS, SUN, SUNSET, MOON, HELMET, CHESTPLATE, LEGGINGS, BOOTS,
        PLUS, GLOBE, SERVER, CUBE, TREE, VOLUME, MONITOR, LANGUAGE, SNOWFLAKE, POWER, TRASH, PENCIL, MARK, DICE, MUSIC, ROBOT, NAV,
        /** The two peaks of MARK, for the loading screen where they slide together. */
        MARK_LEFT, MARK_RIGHT,
        GEAR
    }
    private Icons() {}
    public static void initialize() { /* Forces pipeline registration before shader reload. */ }

    public static void draw(GuiGraphicsExtractor g,Icon icon,float x,float y,float size,int color) {
        draw(g,icon,x,y,size,2,color);
    }
    /** Stroke is in icon units (Lucide default is 2). */
    public static void draw(GuiGraphicsExtractor g,Icon icon,float x,float y,float size,float stroke,int color) {
        if(size<=0 || (color>>>24)==0) return;
        var pose=new Matrix3x2f(g.pose());
        var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle((int)Math.floor(x),(int)Math.floor(y),(int)Math.ceil(size)+1,(int)Math.ceil(size)+1).transformMaxBounds(pose);
        if(clip!=null) bounds=bounds.intersection(clip);
        if(bounds!=null && UiBatch.ready()) g.guiRenderState.addGuiElement(new Glyph(pose,x,y,size,icon.ordinal(),Math.round(stroke*16),color,UiBatch.textures(),clip,bounds));
    }
    /** Ring filled clockwise from the top up to progress (0..1); stroke in icon units, at most ~7.9. */
    public static void arc(GuiGraphicsExtractor g,float x,float y,float size,float stroke,float progress,int color) {
        if(size<=0 || (color>>>24)==0 || progress<=.002f) return;
        var pose=new Matrix3x2f(g.pose());
        var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle((int)Math.floor(x),(int)Math.floor(y),(int)Math.ceil(size)+1,(int)Math.ceil(size)+1).transformMaxBounds(pose);
        if(clip!=null) bounds=bounds.intersection(clip);
        int packed=Math.round(Math.clamp(progress,0,1)*255)*128+Math.min(127,Math.round(stroke*16));
        if(bounds!=null && UiBatch.ready()) g.guiRenderState.addGuiElement(new Glyph(pose,x,y,size,Icon.ARC.ordinal(),packed,color,UiBatch.textures(),clip,bounds));
    }
    /** A line icon in the shared UI pipeline (drawn by its signed distance in core/ui). */
    private record Glyph(Matrix3x2fc pose,float x,float y,float size,int id,int stroke,int color,TextureSetup textureSetup,
                         ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return UiBatch.PIPELINE; }
        public void buildVertices(VertexConsumer v) {
            UiBatch.vertex(v,pose,x,y,color,0,0,0,0,UiBatch.ICON,id,stroke);
            UiBatch.vertex(v,pose,x,y+size,color,0,24,0,0,UiBatch.ICON,id,stroke);
            UiBatch.vertex(v,pose,x+size,y+size,color,24,24,0,0,UiBatch.ICON,id,stroke);
            UiBatch.vertex(v,pose,x+size,y,color,24,0,0,0,UiBatch.ICON,id,stroke);
        }
    }
}
