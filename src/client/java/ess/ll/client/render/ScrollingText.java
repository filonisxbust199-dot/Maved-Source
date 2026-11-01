package ess.ll.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.IdentityHashMap;

/** Clips overflowing text and pans it while its own hit region is hovered. */
public final class ScrollingText {
    private static final IdentityHashMap<Object,State> STATES=new IdentityHashMap<>();
    private static final class State { double offset; }
    private ScrollingText() {}
    public static void draw(GuiGraphicsExtractor g,Object key,String value,float x,float y,float width,float size,int color,boolean hover) {
        if(width<=0 || value==null)return;
        MsdfFont font=MsdfFont.UI;
        float overflow=Math.max(0,font.width(value,size)-width);
        State state=STATES.computeIfAbsent(key,k->new State());
        double target=hover && overflow>0 ? overflow*(.5-.5*Math.cos(System.nanoTime()/1e9*1.6)) : 0;
        state.offset+=(target-state.offset)*.18;
        if(overflow<=0){font.draw(g,value,x,y,size,color);return;}
        if(!hover) {
            String clipped=value;
            while(!clipped.isEmpty() && font.width(clipped+"..",size)>width)clipped=clipped.substring(0,clipped.length()-1);
            font.draw(g,clipped+"..",x,y,size,color);
            return;
        }
        g.enableScissor((int)Math.floor(x),(int)Math.floor(y-2),(int)Math.ceil(x+width),(int)Math.ceil(y+size+4));
        try{font.draw(g,value,x-(float)state.offset,y,size,color);}
        finally{g.disableScissor();}
    }
}
