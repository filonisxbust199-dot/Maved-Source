package ess.ll.client.render;

import ess.ll.client.media.Lyrics;
import ess.ll.client.media.MediaBridge;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Icons.Icon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;

/**
 * The playing track as a line on the HUD plate: artwork on the dark band, then the title, the time and the
 * controls, a divider after each. With lyrics turned on the current line is sung under it, filling as it goes.
 * The controls answer in chat, where the HUD can be clicked.
 */
final class HudPlayer {
    private HudPlayer() {}
    private static final MsdfFont FONT=MsdfFont.UI;
    private static final float H=HudRenderer.KB_HEADER_H, PAD=HudRenderer.KB_PAD, TEXT=13, HINT=11.5f, ART=23, LYRIC_H=26,
            CONTROL=22, TITLE_MIN=80, TITLE_MAX=190, SPEED=HudRenderer.KB_SPEED;
    private static float width, titleW, timeW, lyricH, mouseX=-1, mouseY=-1;
    private static boolean music;

    private static String timeText(MediaBridge.Track track) {
        return track.durationMs()<=0 ? "" : time(track.position())+" / "+time(track.durationMs());
    }
    private static String time(long ms) { return UiText.duration(ms/1000); }
    private static List<Lyrics.Line> lyrics(MediaBridge.Track track) {
        return music && ClientModules.PLAYER_LYRICS.get() ? Lyrics.of(track) : List.of();
    }

    /** Advances the widget; returns {width, height} in HUD units, or null when it is hidden. */
    static float[] measure(double dt,boolean editing,float unit,float localX,float localY) {
        boolean visible=ClientModules.hud("Player");
        if(visible) MediaBridge.ensureRunning(); else MediaBridge.stop();
        var track=MediaBridge.track();
        music=visible && track.present() && !track.title().isBlank();
        if(!music && !(visible && editing)) { lyricH=0; return null; }
        mouseX=editing ? localX/unit : -1; mouseY=editing ? localY/unit : -1;
        String title=music ? track.title() : "Ничего не играет";
        float wantTitle=Math.clamp(FONT.width(title,TEXT),TITLE_MIN,TITLE_MAX), wantTime=FONT.width(timeText(track),HINT);
        titleW=titleW==0 ? wantTitle : HudRenderer.approach(titleW,wantTitle,dt,SPEED);
        timeW=timeW==0 ? wantTime : HudRenderer.approach(timeW,wantTime,dt,SPEED);
        lyricH=HudRenderer.approach(lyricH,lyrics(track).isEmpty() ? 0 : LYRIC_H,dt,SPEED);
        width=H+2*PAD+titleW+(wantTime>0 ? 2*PAD+timeW : 0)+3*CONTROL+2*5;
        return new float[]{width*unit,(H+lyricH)*unit};
    }
    private static float controlX(int i) { return width-5-CONTROL*(3-i)+CONTROL/2; }

    static void draw(GuiGraphicsExtractor g,float alpha) {
        var track=MediaBridge.track();
        float h=H+lyricH, t=(System.nanoTime()%(1L<<42))/1e9f;
        Theme.plate(g,0,0,width,h,HudRenderer.KB_RADIUS,alpha);
        Theme.plateBand(g,0,0,width,h,true,H,HudRenderer.KB_RADIUS,alpha);
        art(g,alpha);
        HudRenderer.plateDivider(g,H,H,alpha);
        float x=H+PAD;
        HudRenderer.pushClip(g,x,0,x+titleW,H);
        try {
            if(music) marquee(g,track.title(),x,titleW,TEXT,c(Theme.PLATE_TEXT,alpha),t);
            else FONT.mid(g,"Ничего не играет",x,H/2,HINT,c(Theme.PLATE_HINT,alpha));
        } finally { g.scissorStack.pop(); }
        x+=titleW+PAD;
        String time=timeText(track);
        if(!time.isEmpty()) {
            HudRenderer.plateDivider(g,x,H,alpha);
            FONT.mid(g,time,x+PAD,H/2,HINT,c(Theme.PLATE_HINT,alpha));
            x+=2*PAD+timeW;
        }
        HudRenderer.plateDivider(g,x,H,alpha);
        for(int i=-1;i<=1;i++) {
            float cx=controlX(i+1), s=i==0 ? 14 : 12;
            boolean over=overControl(i+1);
            Icon icon=i<0 ? Icon.PREVIOUS : i>0 ? Icon.NEXT : track.playing() ? Icon.PAUSE : Icon.PLAY;
            Icons.draw(g,icon,cx-s/2,H/2-s/2,s,c(over || i==0 ? Theme.accent() : Theme.PLATE_TEXT,alpha));
        }
        if(lyricH>1) lyric(g,track,alpha*Math.clamp(lyricH/LYRIC_H,0,1));
    }
    private static void art(GuiGraphicsExtractor g,float alpha) {
        float at=(H-ART)/2;
        var art=MediaBridge.artwork();
        if(art!=null) { Avatar.image(g,art,at,at,ART,6,alpha); return; }
        UiRenderer.rounded(g,at,at,ART,ART,6,c(0x1fffffff,alpha));
        Icons.draw(g,Icon.MUSIC,H/2-7,H/2-7,14,2.2f,c(Theme.accent(),alpha));
    }
    /** The current line: grey, filled white up to where it is being sung; wider than the plate it follows the singer. */
    private static void lyric(GuiGraphicsExtractor g,MediaBridge.Track track,float alpha) {
        var lines=lyrics(track);
        long pos=track.position();
        // The track changed and the row is still closing.
        if(lines.isEmpty()) return;
        // Before the first line it waits, grey, so the row is not empty through the intro.
        int index=Math.max(0,Lyrics.index(lines,pos));
        String text=lines.get(index).text();
        long from=lines.get(index).timeMs(), to=index+1<lines.size() ? lines.get(index+1).timeMs() : from+4000;
        float p=Math.clamp((pos-from)/(float)Math.max(1,to-from),0,1);
        float size=TEXT, tw=FONT.width(text,size), area=width-2*PAD, mid=H+LYRIC_H/2-1;
        float x=tw<=area ? width/2-tw/2 : PAD-Math.clamp(tw*p-area*.6f,0,tw-area);
        HudRenderer.pushClip(g,PAD,H,width-PAD,H+lyricH);
        try {
            FONT.mid(g,text,x,mid,size,c(Theme.withAlpha(Theme.PLATE_TEXT,.4f),alpha));
            HudRenderer.pushClip(g,x,H,Math.min(width-PAD,x+tw*p),H+lyricH);
            try { FONT.mid(g,text,x,mid,size,c(Theme.PLATE_TEXT,alpha)); }
            finally { g.scissorStack.pop(); }
        } finally { g.scissorStack.pop(); }
    }
    /** Text from x; when it does not fit it loops to the left, with a short pause so the start can be read. */
    private static void marquee(GuiGraphicsExtractor g,String text,float x,float w,float size,int color,float t) {
        float tw=FONT.width(text,size);
        if(tw<=w) { FONT.mid(g,text,x,H/2,size,color); return; }
        float gap=36, cycle=tw+gap, off=Math.max(0,(t*24)%(cycle+42)-42);
        FONT.mid(g,text,x-off,H/2,size,color);
        FONT.mid(g,text,x-off+cycle,H/2,size,color);
    }

    private static boolean overControl(int i) {
        float cx=controlX(i);
        return mouseX>=cx-CONTROL/2 && mouseX<=cx+CONTROL/2 && mouseY>=0 && mouseY<=H;
    }
    /** A click at widget-local HUD units; true when a control used it. */
    static boolean press(float x,float y,float unit) {
        if(!music) return false;
        float px=x/unit, py=y/unit;
        for(int i=0;i<3;i++) {
            float cx=controlX(i);
            if(px<cx-CONTROL/2 || px>cx+CONTROL/2 || py<0 || py>H-4) continue;
            if(i==0) MediaBridge.previous(); else if(i==2) MediaBridge.next(); else MediaBridge.toggle();
            UiSounds.select();
            return true;
        }
        return false;
    }
    private static int c(int argb,float alpha) { return Theme.withAlpha(argb,((argb>>>24)/255f)*Math.clamp(alpha,0,1)); }
}
