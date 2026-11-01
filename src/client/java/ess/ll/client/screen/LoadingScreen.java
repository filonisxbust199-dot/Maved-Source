package ess.ll.client.screen;

import ess.ll.client.render.Gradient;
import ess.ll.client.render.Icons;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;

/**
 * ESS resource loading screen: the three bars of the mark assemble (the middle one grows from the centre,
 * the outer ones slide in from both sides), then a tight glow fades in and breathes; the wide wordmark and
 * the progress bar appear underneath. Everything is drawn with plain GUI geometry, because the mod's font
 * and icon shaders are themselves still loading while this screen is up.
 */
public final class LoadingScreen {
    private static final int BG=0xff0a0a0a, FG=0xfff4f4f4, TRACK=0xff1f1f1f, LINE=0x10ffffff;
    private LoadingScreen() {}

    private static float clamp(double t) { return (float)Math.clamp(t,0,1); }
    private static float out(double t) { float x=clamp(t); return 1-(1-x)*(1-x)*(1-x)*(1-x); }
    private static float inOut(double t) { float x=clamp(t); return x<.5f ? 4*x*x*x : 1-(float)Math.pow(-2*x+2,3)/2; }

    private static long timelineStart, timelineLast;
    /**
     * Seconds into the intro for the world loading screens. Screens that follow one another (preparing the world,
     * then loading chunks) continue one animation instead of each replaying the logo.
     */
    public static double timeline() {
        long now=System.currentTimeMillis();
        // A world loads in about a second, so these screens open with the logo nearly assembled, and a gap of
        // a few seconds (between saving and the next load) does not replay it.
        if(now-timelineLast>3000) timelineStart=now-1000;
        timelineLast=now;
        return (now-timelineStart)/1000.0;
    }
    /** World loading: the same screen with a caption under the bar; progress below 0 runs an indeterminate bar. */
    public static void draw(GuiGraphicsExtractor g,double t,float progress,String caption) {
        draw(g,t,progress,1);
        float alpha=out((t-1.4)/.6);
        if(caption==null || alpha<=0) return;
        var window=Minecraft.getInstance().getWindow();
        float s=Math.max(1,window.getHeight()/1080f*1.75f)/(float)window.getGuiScale();
        g.pose().pushMatrix();
        try {
            g.pose().translate(g.guiWidth()/2f,g.guiHeight()/2f);
            g.pose().scale(s,s);
            var font=ess.ll.client.render.MsdfFont.SITE;
            float size=7.5f, spacing=1.5f, w=-spacing;
            for(int code:caption.codePoints().toArray()) w+=font.width(new String(Character.toChars(code)),size)+spacing;
            float x=-w/2;
            for(int code:caption.codePoints().toArray()) {
                String glyph=new String(Character.toChars(code));
                font.mid(g,glyph,x,86,size,Theme.withAlpha(0xff7a7a7a,alpha));
                x+=font.width(glyph,size)+spacing;
            }
        } finally { g.pose().popMatrix(); }
    }

    /** t: seconds since the screen appeared; alpha: the whole screen's opacity while fading in or out. */
    public static void draw(GuiGraphicsExtractor g,double t,float progress,float alpha) {
        if(alpha<=0) return;
        var window=Minecraft.getInstance().getWindow();
        int width=g.guiWidth(), height=g.guiHeight();
        UiRenderer.rect(g,0,0,width,height,Theme.withAlpha(BG,alpha));
        // Same physical size as the main menu: 1.75 px per design unit at 1080p.
        float s=Math.max(1,window.getHeight()/1080f*1.75f)/(float)window.getGuiScale();
        g.pose().pushMatrix();
        try {
            g.pose().translate(width/2f,height/2f);
            g.pose().scale(s,s);
            grid(g,width/s,height/s,alpha*out((t-.2)/1.2));
            logo(g,t,alpha);
            word(g,t,alpha);
            float bar=alpha*out((t-1.3)/.6);
            if(bar>0) {
                float bw=200, by=70;
                UiRenderer.rounded(g,-bw/2,by,bw,3,1.5f,Theme.withAlpha(TRACK,bar));
                if(progress>=0) UiRenderer.rounded(g,-bw/2,by,bw*progress,3,1.5f,Theme.withAlpha(FG,bar));
                else {
                    // Unknown progress: a short segment sweeping across the track.
                    float phase=(float)((t*.7)%1), seg=bw*.28f;
                    float x0=Math.max(0,phase*(bw+seg)-seg), x1=Math.min(bw,phase*(bw+seg));
                    if(x1>x0) UiRenderer.rounded(g,-bw/2+x0,by,x1-x0,3,1.5f,Theme.withAlpha(FG,bar));
                }
            }
        } finally { g.pose().popMatrix(); }
    }

    private static final float MARK=96, MARK_X=-MARK/2, MARK_Y=-30-MARK/2;
    /** Soft rest colour of the M, so the glint (pure white) reads as light passing over it. */
    private static final int MARK_REST=0xffbfc1c7;
    /**
     * The M: its two peaks slide in from the sides and join in the middle, then a glint sweeps across it, and
     * again every few seconds while loading goes on.
     */
    private static void logo(GuiGraphicsExtractor g,double t,float alpha) {
        float left=out((t-.1)/1), right=out((t-.22)/1);
        // Joined, it is one shape: two translucent halves would show their overlap while the screen fades.
        if(left>=1 && right>=1) Icons.draw(g,Icons.Icon.MARK,MARK_X,MARK_Y,MARK,Theme.withAlpha(MARK_REST,alpha));
        else {
            if(left>0) Icons.draw(g,Icons.Icon.MARK_LEFT,MARK_X-70*(1-left),MARK_Y,MARK,Theme.withAlpha(MARK_REST,alpha*left));
            if(right>0) Icons.draw(g,Icons.Icon.MARK_RIGHT,MARK_X+70*(1-right),MARK_Y,MARK,Theme.withAlpha(MARK_REST,alpha*right));
        }
        double sweep=t-1.2;
        if(sweep<0) return;
        float phase=(float)(sweep%3.4/.8);
        if(phase>1) return;
        // A soft band of white strips, widest light in the middle, clipped to thin columns of the M.
        float center=MARK_X-14+(MARK+28)*inOut(phase), fade=(float)Math.sin(Math.PI*phase);
        g.nextStratum();
        for(int i=-5;i<=5;i++) {
            float x0=center+i*2.4f-1.2f, k=1-Math.abs(i)/6f;
            g.enableScissor((int)Math.floor(x0),(int)MARK_Y,(int)Math.ceil(x0+2.4f),(int)(MARK_Y+MARK));
            try { Icons.draw(g,Icons.Icon.MARK,MARK_X,MARK_Y,MARK,Theme.withAlpha(0xffffff,alpha*fade*k*k)); }
            finally { g.disableScissor(); }
        }
    }

    // Wide geometric wordmark in the spirit of Unbounded Black, letter by letter.
    private static final float LH=17, ST=LH*.21f, GAP=LH*.36f;
    private static float letterWidth(char c) {
        return switch (c) { case 'I' -> ST; case 'L' -> LH*.9f; case 'M' -> LH*1.32f; case ' ' -> LH*.55f; default -> LH*1.12f; };
    }
    private static void word(GuiGraphicsExtractor g,double t,float alpha) {
        String text="MAVED CLIENT";
        float total=-GAP;
        for(char c:text.toCharArray()) total+=letterWidth(c)+GAP;
        float x=-total/2, y=30;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            float k=out((t-1.05-i*.035)/.8);
            if(k>0 && c!=' ') letter(g,c,x,y+(1-k)*8,Theme.withAlpha(FG,alpha*k));
            x+=letterWidth(c)+GAP;
        }
    }
    private static void r(GuiGraphicsExtractor g,float x,float y,float w,float h,int color) {
        UiRenderer.rounded(g,x,y,w,h,ST*.32f,color);
    }
    /** A stroke of height h whose bottom is shifted by lean*h (the diagonals of M and V). */
    private static void slant(GuiGraphicsExtractor g,float x,float y,float h,float lean,int color) {
        g.pose().pushMatrix();
        try {
            g.pose().translate(x,y);
            g.pose().mul(new Matrix3x2f(1,0,lean,1,0,0));
            UiRenderer.rounded(g,0,0,ST,h,0,color);
        } finally { g.pose().popMatrix(); }
    }
    private static void letter(GuiGraphicsExtractor g,char c,float x,float y,int color) {
        float w=letterWidth(c), h=LH, mid=y+(h-ST)/2;
        switch (c) {
            case 'E' -> { r(g,x,y,ST,h,color); r(g,x,y,w,ST,color); r(g,x,mid,w*.84f,ST,color); r(g,x,y+h-ST,w,ST,color); }
            case 'S' -> {
                r(g,x,y,w,ST,color); r(g,x,y,ST,h/2+ST/2,color); r(g,x,mid,w,ST,color);
                r(g,x+w-ST,mid,ST,h/2+ST/2,color); r(g,x,y+h-ST,w,ST,color);
            }
            case 'C' -> { r(g,x,y,w,ST,color); r(g,x,y,ST,h,color); r(g,x,y+h-ST,w,ST,color); }
            case 'L' -> { r(g,x,y,ST,h,color); r(g,x,y+h-ST,w,ST,color); }
            case 'I' -> r(g,x,y,ST,h,color);
            case 'N' -> {
                r(g,x,y,ST,h,color); r(g,x+w-ST,y,ST,h,color);
                g.pose().pushMatrix();
                try {
                    g.pose().translate(x,y);
                    g.pose().mul(new Matrix3x2f(1,0,(w-ST)/h,1,0,0));
                    UiRenderer.rounded(g,0,0,ST,h,0,color);
                } finally { g.pose().popMatrix(); }
            }
            case 'T' -> { r(g,x,y,w,ST,color); r(g,x+(w-ST)/2,y,ST,h,color); }
            case 'M' -> {
                r(g,x,y,ST,h,color); r(g,x+w-ST,y,ST,h,color);
                float inner=h*.64f, lean=(w/2-ST/2)/inner;
                slant(g,x,y,inner,lean,color); slant(g,x+w-ST,y,inner,-lean,color);
            }
            case 'A' -> { r(g,x,y,w,ST,color); r(g,x,y,ST,h,color); r(g,x+w-ST,y,ST,h,color); r(g,x,mid,w,ST,color); }
            case 'V' -> { float lean=(w/2-ST/2)/h; slant(g,x,y,h,lean,color); slant(g,x+w-ST,y,h,-lean,color); }
            case 'D' -> { r(g,x,y,ST,h,color); r(g,x,y,w-ST*.6f,ST,color); r(g,x,y+h-ST,w-ST*.6f,ST,color); r(g,x+w-ST,y+ST*.5f,ST,h-ST,color); }
            default -> {}
        }
    }

    /** The site's grid, faded out radially around the logo. */
    private static void grid(GuiGraphicsExtractor g,float w,float h,float alpha) {
        if(alpha<=0) return;
        int line=Theme.withAlpha(LINE,(LINE>>>24)/255f*alpha);
        float cell=28, rx=w*.5f, ry=h*.5f, cy=-30;
        for(float x=-(float)Math.floor(w/2/cell)*cell;x<=w/2;x+=cell) {
            for(int i=0;i<8;i++) {
                float y0=-h/2+h*i/8, y1=-h/2+h*(i+1)/8;
                int c0=mask(line,x/rx,(y0-cy)/ry), c1=mask(line,x/rx,(y1-cy)/ry);
                if((c0>>>24)==0 && (c1>>>24)==0) continue;
                UiRenderer.rounded(g,x,y0,1,y1-y0,0,Gradient.linear(c0,c1,Gradient.Axis.VERTICAL,y0,y1-y0));
            }
        }
        for(float y=cy-(float)Math.floor((h/2+cy)/cell)*cell;y<=h/2;y+=cell) {
            for(int i=0;i<10;i++) {
                float x0=-w/2+w*i/10, x1=-w/2+w*(i+1)/10;
                int c0=mask(line,x0/rx,(y-cy)/ry), c1=mask(line,x1/rx,(y-cy)/ry);
                if((c0>>>24)==0 && (c1>>>24)==0) continue;
                UiRenderer.rounded(g,x0,y,x1-x0,1,0,Gradient.linear(c0,c1,Gradient.Axis.HORIZONTAL,x0,x1-x0));
            }
        }
    }
    private static int mask(int color,float dx,float dy) {
        float k=Math.clamp(1-(float)Math.hypot(dx,dy)/.8f,0,1);
        return Theme.withAlpha(color,(color>>>24)/255f*k);
    }
}
