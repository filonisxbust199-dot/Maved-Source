package ess.ll.client.render;

import ess.ll.client.command.ClientData;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Icons.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import java.util.*;

/**
 * Notifications as lines on the HUD plate: the icon on the dark band, the title, the detail and a ring with the
 * time left, a divider after each. Module toggles, ending effects and friends joining raise them on their own;
 * the rest call queue. Toggles share one line: while it is up a new toggle changes its text, not the plate.
 */
public final class HudNotifications {
    private HudNotifications() {}
    private static final MsdfFont FONT=MsdfFont.UI;
    private static final float H=HudRenderer.KB_HEADER_H, PAD=HudRenderer.KB_PAD, TEXT=13, DETAIL=11.5f, ROW_GAP=5, RING=14, RING_W=RING+2*9;
    private static final float TOGGLE_SECONDS=1.8f, NOTE_SECONDS=2.8f, IN_SECONDS=.26f, OUT_SECONDS=.18f, SWAP_SECONDS=.24f;
    private static final float RISE=7, RESIZE_SPEED=16, RING_SPEED=20;
    private static final int MAX_LINES=4;

    private record Content(Icon icon,int color,String title,String detail) {}
    private static final class Note {
        final boolean toggle;
        final float life;
        Content now, before;
        float age, in, swap, ring=1, w, titleW, detailW;
        Note(boolean toggle,float life,Content content) {
            this.toggle=toggle; this.life=life; now=content;
            titleW=FONT.width(content.title,TEXT); detailW=FONT.width(content.detail,DETAIL); w=width(titleW,detailW);
        }
        float width(float title,float detail) { return H+2*PAD+title+(now.detail.isEmpty() ? 0 : 2*PAD+detail)+RING_W; }
        boolean leaving() { return age>life; }
        /** Smooth both ways, so a line never starts or stops moving with a jerk. */
        float appear() { return in*in*(3-2*in); }
        void show(Content content) {
            if(!content.equals(now)) { before=now; now=content; swap=1; }
            age=0;
        }
    }
    private static final List<Note> LINES=new ArrayList<>();
    /** Waiting for a free line, for messages that come as a series. */
    private static final ArrayDeque<Note> QUEUE=new ArrayDeque<>();
    private static final Note SAMPLE=new Note(false,1,new Content(Icon.CHECK,0,"Уведомления","Включён"));
    private static float width;
    private static boolean editing;
    private static final Map<ClientModules.Module,Boolean> MODULES=new IdentityHashMap<>();
    private static final Set<Holder<MobEffect>> WARNED=new HashSet<>();
    private static Set<String> players;

    /** The line of the module toggles: still on screen, even fading out, it takes the new text. */
    private static void toggled(Content content) {
        for(var note:LINES) if(note.toggle) { note.show(content); return; }
        add(new Note(true,TOGGLE_SECONDS,content));
    }
    /** Shown at once; a line with the same title takes the new detail. */
    private static void post(Content content) {
        for(var note:LINES) if(!note.toggle && note.now.title.equals(content.title)) { note.show(content); return; }
        add(new Note(false,NOTE_SECONDS,content));
    }
    private static void add(Note note) {
        LINES.add(note);
        // Over the limit the oldest line leaves on its own animation.
        int shown=0;
        for(var line:LINES) if(!line.leaving()) shown++;
        for(var line:LINES) if(shown>MAX_LINES && !line.leaving()) { line.age=line.life+1; shown--; }
    }
    /** Shown after the ones already waiting, when a line is free. */
    public static void queue(Icon icon,int color,String title,String detail) {
        QUEUE.add(new Note(false,NOTE_SECONDS,new Content(icon,color,title,detail)));
    }

    private static void watchEvents(Minecraft mc) {
        for(var m:ClientModules.all()) {
            if(m==ClientModules.HUD || m==ClientModules.THEME || m==ClientModules.COSMETICS || m==ClientModules.CLICK_GUI) continue;
            boolean on=m.enabled();
            Boolean was=MODULES.put(m,on);
            if(was!=null && was!=on && ClientModules.NOTE_MODULES.get())
                toggled(new Content(on ? Icon.CHECK : Icon.CLOSE,on ? Theme.accent() : 0xffff6b5e,m.name,on ? "Включён" : "Выключен"));
        }
        if(mc.player!=null) {
            Set<Holder<MobEffect>> active=new HashSet<>();
            for(var e:mc.player.getActiveEffects()) {
                active.add(e.getEffect());
                if(e.isInfiniteDuration() || e.getDuration()>100) { WARNED.remove(e.getEffect()); continue; }
                if(WARNED.add(e.getEffect()) && ClientModules.NOTE_EFFECTS.get())
                    post(new Content(Icon.FLASK,0xffe49a3a,e.getEffect().value().getDisplayName().getString(),"Закончится через "+Math.max(1,e.getDuration()/20)+" с"));
            }
            WARNED.retainAll(active);
        }
        if(mc.getConnection()!=null) {
            Set<String> now=new HashSet<>();
            for(var info:mc.getConnection().getOnlinePlayers()) now.add(info.getProfile().name());
            if(players!=null && ClientModules.NOTE_FRIENDS.get())
                for(String name:now) if(!players.contains(name) && ClientData.isFriend(name)) post(new Content(Icon.USER,0xff4fd18b,name,"Друг зашёл на сервер"));
            players=now;
        } else players=null;
    }

    /** Advances the lines; returns {width, height} in HUD units, or null when there is nothing to show. */
    static float[] measure(Minecraft mc,double dt,boolean editing,float unit) {
        boolean visible=ClientModules.hud("Notifications");
        HudNotifications.editing=editing;
        watchEvents(mc);
        if(!visible) { LINES.clear(); QUEUE.clear(); return null; }
        int shown=0;
        for(var note:LINES) if(!note.leaving()) shown++;
        while(!QUEUE.isEmpty() && shown++<MAX_LINES) add(QUEUE.poll());
        float h=0, w=0, step=(float)dt;
        for(var note:LINES) {
            note.age+=step;
            note.in=Math.clamp(note.in+(note.leaving() ? -step/OUT_SECONDS : step/IN_SECONDS),0,1);
            note.swap=Math.max(0,note.swap-step/SWAP_SECONDS);
            note.ring=HudRenderer.approach(note.ring,1-Math.clamp(note.age/note.life,0,1),dt,RING_SPEED);
            note.titleW=HudRenderer.approach(note.titleW,FONT.width(note.now.title,TEXT),dt,RESIZE_SPEED);
            note.detailW=HudRenderer.approach(note.detailW,FONT.width(note.now.detail,DETAIL),dt,RESIZE_SPEED);
            note.w=note.width(note.titleW,note.detailW);
            h+=(H+ROW_GAP)*note.appear();
            w=Math.max(w,note.w);
        }
        LINES.removeIf(note->note.leaving() && note.in<=0);
        if(LINES.isEmpty()) {
            if(!editing) return null;
            w=SAMPLE.w; h=H+ROW_GAP;
        }
        width=w;
        return new float[]{w*unit,Math.max(H,h-ROW_GAP)*unit};
    }

    static void draw(GuiGraphicsExtractor g,float alpha) {
        if(LINES.isEmpty() && editing) { line(g,SAMPLE,0,alpha); return; }
        float y=0;
        for(var note:LINES) {
            float appear=note.appear();
            line(g,note,y-(1-appear)*RISE,alpha*appear);
            y+=(H+ROW_GAP)*appear;
        }
    }
    private static void line(GuiGraphicsExtractor g,Note note,float y,float alpha) {
        float w=note.w;
        g.pose().pushMatrix();
        try {
            g.pose().translate(Math.round(width/2-w/2),y);
            Theme.plate(g,0,0,w,H,HudRenderer.KB_RADIUS,alpha);
            Theme.plateBand(g,0,0,w,H,false,H,HudRenderer.KB_RADIUS,alpha);
            HudRenderer.plateDivider(g,H,H,alpha);
            // The old text slides up and out while the new one comes up from below, over the same plate.
            float swap=note.swap*note.swap*(3-2*note.swap);
            if(swap>0 && note.before!=null) content(g,note,note.before,alpha*swap,-5*(1-swap));
            content(g,note,note.now,alpha*(1-swap),5*swap);
            HudRenderer.plateDivider(g,w-RING_W,H,alpha);
            float rx=w-RING_W+9, ry=H/2-RING/2;
            int color=note.now.color==0 ? Theme.accent() : note.now.color;
            Icons.arc(g,rx,ry,RING,3.4f,1,c(0x24ffffff,alpha));
            Icons.arc(g,rx,ry,RING,3.4f,note.ring,c(color,alpha));
        } finally { g.pose().popMatrix(); }
    }
    private static void content(GuiGraphicsExtractor g,Note note,Content text,float alpha,float dy) {
        if(alpha<=.01f) return;
        int color=text.color==0 ? Theme.accent() : text.color;
        Icons.draw(g,text.icon,H/2-7.5f,H/2-7.5f+dy,15,2.4f,c(color,alpha));
        float x=H+PAD;
        FONT.mid(g,text.title,x,H/2+dy,TEXT,c(Theme.PLATE_TEXT,alpha));
        if(text.detail.isEmpty()) return;
        x+=note.titleW+PAD;
        HudRenderer.plateDivider(g,x,H,alpha);
        FONT.mid(g,text.detail,x+PAD,H/2+dy,DETAIL,c(Theme.PLATE_HINT,alpha));
    }
    private static int c(int argb,float alpha) { return Theme.withAlpha(argb,((argb>>>24)/255f)*Math.clamp(alpha,0,1)); }
}
