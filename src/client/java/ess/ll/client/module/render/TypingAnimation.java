package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * TypingAnimation: each typed letter fades in and rises into place instead of popping up, in the chat, in
 * container search fields and in the client's own fields (menu, ClickGUI). Fields are told apart by a key (the
 * vanilla EditBox itself, or a name), and a letter's age is kept per position: on each change the part both
 * texts share at the start and at the end keeps its ages, the new middle starts from zero.
 */
public final class TypingAnimation {
    public static final ModeSettings STYLE=new ModeSettings("Стиль","Подъём","Появление","Масштаб");
    public static final SliderSetting SPEED=new SliderSetting("Длительность (мс)",60,400,10,160);
    public static final Setting[] SETTINGS={STYLE,SPEED};

    private static final class Field { String text=""; long[] born=new long[0]; }
    /** Vanilla boxes are weak keys (they come and go with screens); client fields are named. */
    private static final Map<Object,Field> BOXES=new WeakHashMap<>();
    private static final Map<Object,Field> NAMED=new java.util.HashMap<>();

    private TypingAnimation() {}

    public static boolean enabled() { return ClientModules.TYPING_ANIMATION!=null && ClientModules.TYPING_ANIMATION.enabled(); }

    /** How far letter i of the field's current text has appeared, 0..1. */
    public static float progress(Object key,String text,int i) {
        long[] born=born(key,text);
        if(i<0 || i>=born.length) return 1;
        return ease(Math.clamp((System.currentTimeMillis()-born[i])/SPEED.get(),0,1));
    }

    private static long[] born(Object key,String text) {
        Field f=(key instanceof String ? NAMED : BOXES).computeIfAbsent(key,k->{ Field n=new Field(); n.text=text; n.born=new long[text.length()]; return n; });
        if(!f.text.equals(text)) {
            String old=f.text;
            int start=0,max=Math.min(old.length(),text.length());
            while(start<max && old.charAt(start)==text.charAt(start)) start++;
            int end=0;
            while(end<max-start && old.charAt(old.length()-1-end)==text.charAt(text.length()-1-end)) end++;
            long[] born=new long[text.length()];
            long now=System.currentTimeMillis();
            System.arraycopy(f.born,0,born,0,start);
            for(int i=start;i<text.length()-end;i++) born[i]=now;
            System.arraycopy(f.born,old.length()-end,born,text.length()-end,end);
            f.text=text; f.born=born;
        }
        return f.born;
    }

    private static float ease(float t) { return 1-(1-t)*(1-t)*(1-t); }

    /** Vertical lift of a letter at progress t, in units of its height. */
    public static float lift(float t) { return STYLE.is("Подъём") ? (1-t)*.45f : 0; }
    public static float scale(float t) { return STYLE.is("Масштаб") ? .4f+.6f*t : 1; }

    /**
     * Vanilla EditBox: one piece of its text starting at letter `start`, drawn letter by letter with the
     * shadow and formatting vanilla would use.
     */
    public static void editBox(GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,Object box,String value,int start,
            net.minecraft.util.FormattedCharSequence text,int x,int y,int color,boolean shadow) {
        long[] born=born(box,value);
        long now=System.currentTimeMillis();
        float[] px={x};
        text.accept((index,style,codepoint)->{
            var one=net.minecraft.util.FormattedCharSequence.codepoint(codepoint,style);
            int i=start+index;
            float t=i>=0 && i<born.length ? ease(Math.clamp((now-born[i])/SPEED.get(),0,1)) : 1;
            int w=font.width(one);
            if(t>=1) g.text(font,one,Math.round(px[0]),y,color,shadow);
            else {
                float s=scale(t);
                g.pose().pushMatrix();
                g.pose().translate(px[0]+w/2f,y+4+lift(t)*9);
                g.pose().scale(s,s);
                g.text(font,one,-w/2,-4,Theme.withAlpha(color,((color>>>24)/255f)*t),shadow);
                g.pose().popMatrix();
            }
            px[0]+=w;
            return true;
        });
    }

    /** Client fields: the text drawn letter by letter, vertically centred on y like MsdfFont.mid. */
    public static void mid(GuiGraphicsExtractor g,MsdfFont font,Object key,String text,float x,float y,float size,int color) {
        if(!enabled()) { font.mid(g,text,x,y,size,color); return; }
        // An empty field is still remembered, so its first letter animates too.
        if(text.isEmpty()) { born(key,text); return; }
        long[] born=born(key,text);
        float px=x;
        for(int i=0;i<text.length();i++) {
            String ch=text.substring(i,i+1);
            float w=font.width(ch,size);
            float t=i<born.length ? ease(Math.clamp((System.currentTimeMillis()-born[i])/SPEED.get(),0,1)) : 1;
            if(t>=1) font.mid(g,ch,px,y,size,color);
            else {
                float s=scale(t);
                g.pose().pushMatrix();
                g.pose().translate(px+w/2,y+lift(t)*size);
                g.pose().scale(s,s);
                font.mid(g,ch,-w/2,0,size,Theme.withAlpha(color,((color>>>24)/255f)*t));
                g.pose().popMatrix();
            }
            px+=w;
        }
    }
}
