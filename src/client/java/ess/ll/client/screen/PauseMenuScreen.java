package ess.ll.client.screen;

import ess.ll.client.media.Lyrics;
import ess.ll.client.media.MediaBridge;
import ess.ll.client.module.misc.PvpSafe;
import ess.ll.client.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ESS pause menu: a glass panel on the left with where you are and what to do, the world left visible on
 * the right, and the playing track and your coordinates in the corner. Same units and palette as the main menu.
 */
public final class PauseMenuScreen extends Screen {
    private static final int[] FG={0xfff4f4f4,0xff0b0b0b}, MUT={0xff8a8a8a,0xff85857f}, LINE={0x1fffffff,0x1a000000},
            PANEL={0xf40a0a0a,0xf6f6f6f4}, SOLID={0xd8121212,0xe6ffffff}, SEG={0xff1f1f1f,0xffe4e4e0};
    private record Hot(String id,float x,float y,float w,float h,Runnable click) {}
    private record Item(String id,String label,Icons.Icon icon,boolean danger,boolean enabled,Runnable run) {}

    private final long openedAt=System.nanoTime();
    private long lastFrame=System.nanoTime();
    private double dt;
    private float s=1, W, H, mx, my, lt, fade=1;
    private GuiGraphicsExtractor g;
    private final Map<String,Float> hovers=new HashMap<>(), presses=new HashMap<>();
    private List<Hot> hots=new ArrayList<>(), drawing=new ArrayList<>();
    private String toast="";
    private double toastAt=-10;
    private float lyricShift;
    private int lyricIndex=-2;

    public PauseMenuScreen() { super(Component.translatable("menu.paused")); }

    private double now() { return (System.nanoTime()-openedAt)/1e9; }
    private int col(int[] pair) { return a(Theme.mix(pair[0],pair[1],lt)); }
    private int a(int color) { return Theme.withAlpha(color,(color>>>24)/255f*fade); }
    private float ease(String id,boolean on,float speed) {
        float value=hovers.getOrDefault(id,0f);
        value+=((on?1:0)-value)*(float)(1-Math.exp(-dt*speed));
        hovers.put(id,value);
        return value;
    }
    private float hot(String id,float x,float y,float w,float h,Runnable click) {
        if(click!=null) drawing.add(new Hot(id,x,y,w,h,()->{ presses.put(id,1f); click.run(); }));
        return ease(id,mx>=x && mx<x+w && my>=y && my<y+h,12);
    }
    private float press(String id) {
        float value=Math.max(0,presses.getOrDefault(id,0f)-(float)dt*5);
        presses.put(id,value);
        return value;
    }
    private static float out(double t) { float x=(float)Math.clamp(t,0,1); return 1-(1-x)*(1-x)*(1-x)*(1-x); }
    private String fit(MsdfFont font,String text,float size,float max) {
        if(font.width(text,size)<=max) return text;
        while(text.length()>1 && font.width(text+"…",size)>max) text=text.substring(0,text.length()-1);
        return text+"…";
    }
    private float spaced(MsdfFont font,String text,float x,float midY,float size,float spacing,int color) {
        float pen=x;
        for(int code:text.codePoints().toArray()) {
            String glyph=new String(Character.toChars(code));
            font.mid(g,glyph,pen,midY,size,color);
            pen+=font.width(glyph,size)+spacing;
        }
        return pen-x-spacing;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        g=graphics;
        long frame=System.nanoTime(); dt=Math.min(.1,(frame-lastFrame)/1e9); lastFrame=frame;
        double now=now();
        var window=minecraft.getWindow();
        s=Math.min(Math.max(1,window.getHeight()/1080f*1.75f)/(float)window.getGuiScale(),Math.min(width/560f,height/315f));
        W=width/s; H=height/s; mx=mouseX/s; my=mouseY/s;
        lt=MainMenuScreen.lightTheme() ? 1 : 0;
        drawing=new ArrayList<>();
        // The world stays visible: only a shade that deepens towards the panel.
        float open=out(now/.45);
        UiRenderer.rounded(g,0,0,width,height,0,Gradient.linear(Theme.withAlpha(0xff000000,.55f*open),Theme.withAlpha(0xff000000,.12f*open),Gradient.Axis.HORIZONTAL,0,width));
        g.pose().pushMatrix();
        try {
            g.pose().scale(s,s);
            drawPanel(now,open);
            drawCorner(now);
            drawToast(now);
        } finally { g.pose().popMatrix(); }
        hots=drawing;
    }
    /** The world behind is blurred with the game's own menu blur (its strength follows the video setting). */
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        if(minecraft.level!=null) extractBlurredBackground(graphics);
    }

    private void drawPanel(double now,float open) {
        float pw=Math.min(250,W*.4f), px=-(1-open)*(pw+20);
        fade=open;
        UiRenderer.rect(g,px,0,pw,H,col(PANEL));
        UiRenderer.rect(g,px+pw-1,0,1,H,col(LINE));
        float x=px+22, w=pw-44, y=28;
        spaced(MsdfFont.SITE,"MAVED · ПАУЗА",x,y,8,1.8f,col(MUT));
        // Where you are: the server or the world, then ping and players, or the dimension.
        var server=minecraft.getCurrentServer();
        boolean local=minecraft.hasSingleplayerServer();
        String title=local && minecraft.getSingleplayerServer()!=null ? minecraft.getSingleplayerServer().getWorldData().getLevelName()
                : server!=null ? server.name : "Сервер";
        var black=MsdfFont.SITE_BLACK;
        float size=24;
        title=fit(black,title.toUpperCase(Locale.ROOT),size,w);
        spaced(black,title,x-1,y+14+size*.36f,size,-.02f*size,col(FG));
        String sub;
        var connection=minecraft.getConnection();
        if(local) sub="Одиночная игра · "+dimension();
        else {
            int ping=-1;
            if(connection!=null && minecraft.player!=null) {
                var info=connection.getPlayerInfo(minecraft.player.getUUID());
                if(info!=null) ping=info.getLatency();
            }
            int players=connection==null ? 0 : connection.getOnlinePlayers().size();
            sub=(ping>=0 ? ping+" мс · " : "")+players+" "+plural(players,"игрок","игрока","игроков")+(server!=null ? " · "+server.ip : "");
        }
        MsdfFont.SITE.mid(g,fit(MsdfFont.SITE,sub,8,w),x,y+14+size*.72f+12,8,col(MUT));

        var items=new ArrayList<Item>();
        items.add(new Item("resume","Вернуться в игру",Icons.Icon.PLAY,false,true,this::onClose));
        items.add(new Item("options","Настройки",Icons.Icon.MONITOR,false,true,()->open(new OptionsScreen(this,minecraft.options))));
        if(local && minecraft.level!=null) items.add(new Item("world","Настройки мира",Icons.Icon.GLOBE,false,true,()->open(new WorldOptionsScreen(this,minecraft.level))));
        items.add(new Item("modules","Модули",Icons.Icon.GRID,false,true,()->open(new ClickGuiScreen())));
        if(connection!=null) items.add(new Item("advancements","Достижения",Icons.Icon.STAR,false,true,()->open(new AdvancementsScreen(connection.getAdvancements(),this))));
        if(minecraft.player!=null) items.add(new Item("stats","Статистика",Icons.Icon.SIGNAL,false,true,()->open(new StatsScreen(this,minecraft.player.getStats()))));
        float iy=y+14+size*.72f+12+22;
        for(int i=0;i<items.size();i++) {
            float k=out((now-.12-i*.04)/.4);
            fade=open*k;
            item(items.get(i),x-8+(1-k)*-14,iy,w+16,i==0);
            iy+=32;
        }
        // Leaving sits at the bottom, away from the rest; PvpSafe holds it during a fight.
        boolean pvp=PvpSafe.isPvpProtectionActive();
        float k=out((now-.12-items.size()*.04)/.4);
        fade=open*k;
        item(new Item("quit",pvp ? "Нельзя выйти во время боя" : local ? "Сохранить и выйти" : "Отключиться",Icons.Icon.POWER,true,!pvp,this::disconnect),x-8,H-24-28,w+16,false);
        fade=1;
    }
    private void item(Item item,float x,float y,float w,boolean primary) {
        float hv=hot(item.id(),x,y,w,28,item.enabled() ? item.run() : null), pr=press(item.id());
        float k=1-.03f*(float)Math.sin(Math.PI*pr);
        g.pose().pushMatrix();
        try {
            g.pose().translate(x+w/2,y+14);
            g.pose().scale(k,k);
            g.pose().translate(-w/2,-14);
            int fg=Theme.mix(FG[0],FG[1],lt), bg=Theme.mix(PANEL[0],PANEL[1],lt)|0xff000000;
            if(primary) UiRenderer.rounded(g,0,0,w,28,8,a(fg));
            else UiRenderer.rounded(g,0,0,w,28,8,a(Theme.withAlpha(Theme.mix(SEG[0],SEG[1],lt),hv)));
            int color=primary ? a(bg) : item.danger() ? a(item.enabled() ? Theme.mix(0xffff7b7b,fg,hv*.3f) : Theme.mix(MUT[0],MUT[1],lt))
                    : a(Theme.mix(Theme.mix(MUT[0],MUT[1],lt),fg,.55f+.45f*hv));
            // On hover the row's content slides right a little, like a list being pointed at.
            float shift=4*hv;
            Icons.draw(g,item.icon(),10+shift,8.5f,11,item.icon()==Icons.Icon.PLAY ? 2 : 2.1f,color);
            MsdfFont.SITE.mid(g,fit(MsdfFont.SITE,item.label(),8.5f,w-40),10+11+10+shift,14,8.5f,color);
        } finally { g.pose().popMatrix(); }
    }

    /** Bottom right: the playing track with its current line, and the coordinates (click to copy). */
    private void drawCorner(double now) {
        float k=out((now-.2)/.45);
        fade=k;
        float w=Math.min(250,W*.34f), x=W-16-w, y=H-16, oy=(1-k)*10;
        var player=minecraft.player;
        if(player!=null) {
            String coords=String.format(Locale.ROOT,"X %d · Y %d · Z %d",player.getBlockX(),player.getBlockY(),player.getBlockZ());
            y-=30;
            float hv=hot("coords",x,y+oy,w,30,()->{
                minecraft.keyboardHandler.setClipboard(player.getBlockX()+" "+player.getBlockY()+" "+player.getBlockZ());
                showToast("Координаты скопированы");
            });
            card(x,y+oy-2*hv,w,30,hv);
            Icons.draw(g,Icons.Icon.COMPASS,x+12,y+oy-2*hv+9.5f,11,2,col(MUT));
            MsdfFont.SITE.mid(g,fit(MsdfFont.SITE,coords+" · "+dimension(),8,w-44),x+30,y+oy-2*hv+15,8,col(FG));
            y-=8;
        }
        var track=MediaBridge.track();
        if(!track.present() || track.title().isBlank()) { fade=1; return; }
        float h=62;
        y-=h;
        float hv=hot("track",x,y+oy,w,h,null), cy=y+oy-2*hv;
        card(x,cy,w,h,hv);
        var art=MediaBridge.artwork();
        if(art!=null) Avatar.image(g,art,x+10,cy+10,28,7,fade);
        else {
            UiRenderer.rounded(g,x+10,cy+10,28,28,7,Gradient.linear(a(0xffff6a3d),a(0xff8b2fc9),Gradient.Axis.VERTICAL,cy+10,28));
            Icons.draw(g,Icons.Icon.MUSIC,x+17,cy+17,14,2,a(0xffffffff));
        }
        float tx=x+10+28+9, max=w-(tx-x)-3*20-12;
        MsdfFont.SITE_BOLD.mid(g,fit(MsdfFont.SITE_BOLD,track.title(),8.5f,max),tx,cy+18,8.5f,col(FG));
        MsdfFont.SITE.mid(g,fit(MsdfFont.SITE,track.artist(),7.5f,max),tx,cy+30,7.5f,col(MUT));
        float bx=x+w-10-60;
        control("prev",bx,cy+14,Icons.Icon.PREVIOUS,MediaBridge::previous);
        control("toggle",bx+20,cy+14,track.playing() ? Icons.Icon.PAUSE : Icons.Icon.PLAY,MediaBridge::toggle);
        control("next",bx+40,cy+14,Icons.Icon.NEXT,MediaBridge::next);
        lyric(track,x+12,cy+50,w-24);
        fade=1;
    }
    private void control(String id,float x,float y,Icons.Icon icon,Runnable action) {
        float hv=hot(id,x,y,20,20,action);
        UiRenderer.rounded(g,x,y,20,20,6,a(Theme.withAlpha(Theme.mix(SEG[0],SEG[1],lt),hv)));
        Icons.draw(g,icon,x+5,y+5,10,2,a(Theme.mix(Theme.mix(MUT[0],MUT[1],lt),Theme.mix(FG[0],FG[1],lt),.5f+.5f*hv)));
    }
    /** The current lyric line; a new line slides up into place. */
    private void lyric(MediaBridge.Track track,float x,float mid,float w) {
        var lines=Lyrics.of(track);
        if(lines.isEmpty()) return;
        int index=Lyrics.index(lines,track.position());
        if(index!=lyricIndex) { lyricIndex=index; lyricShift=1; }
        lyricShift=Math.max(0,lyricShift-(float)dt*4);
        if(index<0) return;
        float e=1-lyricShift*lyricShift, saved=fade;
        fade=saved*e;
        Icons.draw(g,Icons.Icon.MUSIC,x,mid+lyricShift*6-4,8,2,col(MUT));
        MsdfFont.SITE.mid(g,fit(MsdfFont.SITE,lines.get(index).text(),7.5f,w-13),x+13,mid+lyricShift*6,7.5f,col(MUT));
        fade=saved;
    }
    private void card(float x,float y,float w,float h,float hover) {
        UiRenderer.rounded(g,x,y,w,h,10,col(SOLID));
        UiRenderer.outline(g,x,y,w,h,10,1,a(Theme.mix(Theme.mix(LINE[0],LINE[1],lt),Theme.mix(MUT[0],MUT[1],lt),hover)));
    }
    private void drawToast(double now) {
        double age=now-toastAt;
        if(age>1.8) return;
        float in=out(age/.4), gone=(float)Math.clamp((age-1.4)/.35,0,1);
        fade=Math.min(in,1-gone);
        float w=MsdfFont.SITE.width(toast,8.5f)+28, x=W/2-w/2, y=H-24-28+(1-in)*16;
        card(x,y,w,28,0);
        MsdfFont.SITE.mid(g,toast,x+14,y+14,8.5f,col(FG));
        fade=1;
    }
    private void showToast(String text) { toast=text; toastAt=now(); UiSounds.select(); }

    private String dimension() {
        if(minecraft.level==null) return "";
        var key=minecraft.level.dimension();
        if(key==Level.NETHER) return "Незер";
        if(key==Level.END) return "Энд";
        return key==Level.OVERWORLD ? "Верхний мир" : "Другое измерение";
    }
    private static String plural(long n,String one,String few,String many) {
        long tens=n%100, last=n%10;
        return tens>10 && tens<20 ? many : last==1 ? one : last>=2 && last<=4 ? few : many;
    }
    private void open(Screen screen) { UiSounds.select(); minecraft.gui.setScreen(screen); }
    /** Same as the vanilla button: an unsent report is offered first, then the world is left. */
    private void disconnect() {
        UiSounds.select();
        minecraft.getReportingContext().draftReportHandled(minecraft,this,()->{ ess.ll.client.bots.Bots.quit(); minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE); },true);
    }

    /** Dev: screen position (GUI units) of a clickable area's centre, for simulated clicks. */
    public double[] centerOf(String id) {
        for(var hot:hots) if(hot.id().equals(id)) return new double[]{(hot.x()+hot.w()/2)*s,(hot.y()+hot.h()/2)*s};
        return null;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        // Under SDL the left button is 1, not GLFW's 0.
        if(event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event,doubleClick);
        float x=(float)event.x()/s, y=(float)event.y()/s;
        for(int i=hots.size()-1;i>=0;i--) {
            var hot=hots.get(i);
            if(x>=hot.x() && x<hot.x()+hot.w() && y>=hot.y() && y<hot.y()+hot.h()) { hot.click().run(); return true; }
        }
        return super.mouseClicked(event,doubleClick);
    }
}
