package ess.ll.client.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.setting.Settings.ActionSetting;
import ess.ll.client.setting.Settings.BooleanSetting;
import ess.ll.client.setting.Settings.ModeSettings;
import ess.ll.client.setting.Settings.Setting;
import ess.ll.client.setting.Settings.SliderSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HUD module. Every element is a widget anchored to a screen edge or the centre; while chat is open the
 * widgets can be dragged with snapping, guides and a grid. Positions are saved by ClientData.
 */
public final class HudRenderer {
    private static final float PILL_H=17, ROW_H=14, GAP=3, MARGIN=6, SNAP=4;
    private static final MsdfFont FONT=MsdfFont.UI;
    private static final MsdfFont KB_FONT=MsdfFont.PLATE, KB_ICONS=MsdfFont.PLATE_ICONS;
    private static final Map<ClientModules.Module,Float> SLIDE=new IdentityHashMap<>();
    private static final long START=System.nanoTime();
    private static long lastFrame=System.nanoTime();
    private static LivingEntity target;
    private static float targetShow;
    /** Optional TargetHud target provider (set by CombatModules for Aura); null shows the crosshair entity. */
    public static volatile java.util.function.Supplier<LivingEntity> targetSource;
    /** Screen size in HUD units (GUI pixels divided by the HUD size setting). */
    private static float screenW, screenH, scale=1, mouseX, mouseY;
    /** Multiplies every colour drawn by the widget currently being rendered (show / hide fades). */
    private static float alpha=1;

    // ---- widgets --------------------------------------------------------------------------------
    enum Anchor { START, CENTER, END }
    private static final class Widget {
        final String id, element, label;
        final Anchor defX, defY;
        final float defOx, defOy;
        Anchor ax, ay;
        float ox, oy, x, y, w, h, drawX, drawY, vis, hover, lift;
        boolean placed, present;
        Widget(String id,String element,String label,Anchor ax,Anchor ay,float ox,float oy) {
            this.id=id; this.element=element; this.label=label;
            defX=ax; defY=ay; defOx=ox; defOy=oy;
            reset();
        }
        void reset() { ax=defX; ay=defY; ox=defOx; oy=defOy; }
        void resolve() {
            x=switch (ax) { case START -> ox; case END -> screenW-w-ox; case CENTER -> screenW/2-w/2+ox; };
            y=switch (ay) { case START -> oy; case END -> screenH-h-oy; case CENTER -> screenH/2-h/2+oy; };
        }
        /** Re-anchors to the nearest third of the screen so the widget keeps its place on resize. */
        void moveTo(float nx,float ny) {
            float cx=nx+w/2, cy=ny+h/2;
            ax=cx<screenW/3 ? Anchor.START : cx>screenW*2/3 ? Anchor.END : Anchor.CENTER;
            ay=cy<screenH/3 ? Anchor.START : cy>screenH*2/3 ? Anchor.END : Anchor.CENTER;
            ox=switch (ax) { case START -> nx; case END -> screenW-w-nx; case CENTER -> cx-screenW/2; };
            oy=switch (ay) { case START -> ny; case END -> screenH-h-ny; case CENTER -> cy-screenH/2; };
        }
        boolean contains(float px,float py) { return px>=x-3 && px<=x+w+3 && py>=y-3 && py<=y+h+3; }
    }
    private static final Widget WATERMARK=new Widget("watermark","Watermark","Watermark",Anchor.START,Anchor.START,MARGIN,MARGIN);
    private static final Widget COORDS=new Widget("coordinates","Coordinates","Coordinates",Anchor.START,Anchor.START,MARGIN,MARGIN+PILL_H+GAP);
    private static final Widget LIST=new Widget("arraylist","ArrayList","ArrayList",Anchor.END,Anchor.START,MARGIN,MARGIN);
    private static final Widget POTIONS=new Widget("potions","Potions","Зелья",Anchor.END,Anchor.END,MARGIN,MARGIN);
    private static final Widget TARGET=new Widget("targethud","TargetHud","TargetHud",Anchor.CENTER,Anchor.CENTER,88,41);
    private static final Widget KEYBINDS=new Widget("keybinds","KeyBinds","Бинды",Anchor.START,Anchor.CENTER,MARGIN,0);
    private static final Widget PLAYER=new Widget("player","Player","Плеер",Anchor.CENTER,Anchor.START,0,MARGIN);
    private static final Widget NOTES=new Widget("notifications","Notifications","Уведомления",Anchor.CENTER,Anchor.START,0,MARGIN+1);
    private static final Widget ARMOR_HUD=new Widget("armor","Armor","Броня",Anchor.CENTER,Anchor.END,0,58);
    private static final Widget WAYPOINT=new Widget("waypoint","Waypoint","Метка",Anchor.START,Anchor.START,MARGIN,MARGIN+2*(PILL_H+GAP));
    private static final List<Widget> WIDGETS=List.of(WATERMARK,COORDS,LIST,KEYBINDS,POTIONS,TARGET,PLAYER,NOTES,ARMOR_HUD,WAYPOINT);
    /** Header pill and the separate list panel under it (Бинды, Зелья, settings popup). */
    private static final float HEAD_H=17, HEAD_GAP=2, ROW=15, BODY_PAD=2, PAD=6, TEXT=8;

    // ---- editing ----------------------------------------------------------------------------------
    private static float edit, resetHover, guideX=-1, guideY=-1, guideAlpha;
    /** True as soon as chat is open, independent of the fade-in animation. */
    private static boolean editMode, chatCursor;
    private static Widget dragging;
    /** Settings popup (right click on a widget in chat): target widget, the one still fading out, open amount. */
    private static Widget popupFor, popupShown;
    private static float popup;
    private static SliderSetting popupSlider;
    private static final Map<Object,Float> POPUP_ANIM=new HashMap<>();
    private static final Map<Widget,ActionSetting> RESET_ROWS=new IdentityHashMap<>();
    private static float grabX, grabY;
    private HudRenderer() {}

    public static void initialize() { /* Input arrives through ChatScreenMixin. */ }

    // ---- chat screen input (ChatScreenMixin) ---------------------------------------------------------
    /** Mouse press on the chat screen, GUI coordinates; true when HUD editing used it. */
    public static boolean chatPress(double guiX,double guiY,int button) {
        float x=(float)guiX/scale, y=(float)guiY/scale;
        mouseX=x; mouseY=y; chatCursor=true;
        boolean used=press(button,x,y);
        return used;
    }
    public static boolean chatRelease(int button) { return button==InputConstants.MOUSE_BUTTON_LEFT && release(); }
    /** Cursor position from the chat screen's own render call; preferred over MouseHandler while chat is open. */
    public static void chatCursor(int guiX,int guiY) {
        mouseX=guiX/scale; mouseY=guiY/scale; chatCursor=true;
        if(popupSlider!=null && popupShown!=null) dragSlider(popupSlider,mouseX);
    }

    public static void render(GuiGraphicsExtractor g,DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null) return;
        // The HUD would sit on top of the loading screen during a resource reload.
        // The pause menu shows the track and coordinates itself; the HUD would only show through its panel.
        if(mc.gui.overlay()!=null || mc.gui.screen() instanceof ess.ll.client.screen.PauseMenuScreen) return;
        long now=System.nanoTime();
        double dt=Math.min(.1,(now-lastFrame)/1e9), time=(now-START)/1e9;
        lastFrame=now;
        scale=ClientModules.HUD_SIZE.get();
        screenW=g.guiWidth()/scale; screenH=g.guiHeight()/scale;
        boolean editing=mc.gui.screen() instanceof ChatScreen && ClientModules.HUD.enabled();
        // The chat screen reports the cursor from its own render call (ChatScreenMixin); fall back otherwise.
        if(!editing || !chatCursor) {
            mouseX=(float)(mc.mouseHandler.getScaledXPos(mc.getWindow())/scale);
            mouseY=(float)(mc.mouseHandler.getScaledYPos(mc.getWindow())/scale);
        }
        if(!editing) chatCursor=false;
        editMode=editing;
        if(!editing) dragging=null;
        edit=approach(edit,editing?1:0,dt,12);

        long measured=ess.ll.client.dev.HudProfile.ON ? System.nanoTime() : 0;
        measure(mc,dt,editing);
        if(measured!=0) ess.ll.client.dev.HudProfile.time("measure",System.nanoTime()-measured);
        g.pose().pushMatrix();
        try {
            g.pose().scale(scale,scale);
            if(edit>.01f) drawGrid(g);
            updateDrag();
            for(Widget w:WIDGETS) {
                // Notification lines animate themselves, a second fade over them only slowed them down.
                w.vis=w==NOTES ? (w.present ? 1 : 0) : approach(w.vis,w.present?1:0,dt,w.present?13:darkPlate(w)?15:10);
                w.hover=approach(w.hover,editing && dragging==null && w.present && w.contains(mouseX,mouseY)?1:0,dt,18);
                w.lift=approach(w.lift,dragging==w?1:0,dt,16);
                if(!w.placed) { w.drawX=w.x; w.drawY=w.y; w.placed=true; }
                float follow=dragging==w ? 40 : 14;
                w.drawX=approach(w.drawX,w.x,dt,follow); w.drawY=approach(w.drawY,w.y,dt,follow);
                // The dark plates only fade in, at their place to the pixel, so the header line holds still.
                if(darkPlate(w) && dragging!=w) { w.drawX=w.x; w.drawY=w.y; }
                if(w.vis<=.004f) continue;
                if(edit>.01f) drawEditFrame(g,w);
                long started=ess.ll.client.dev.HudProfile.ON ? System.nanoTime() : 0;
                drawWidget(g,mc,w,time);
                if(started!=0) ess.ll.client.dev.HudProfile.time("widget "+w.id,System.nanoTime()-started);
            }
            if(edit>.01f) { drawGuides(g,dt); drawResetButton(g,dt); }
            drawPopup(g,dt,time);
        } finally { g.pose().popMatrix(); alpha=1; }
    }
    /** Sizes every widget for this frame and resolves its target box. */
    private static void measure(Minecraft mc,double dt,boolean editing) {
        WATERMARK.w=watermarkWidth(mc,dt); WATERMARK.h=KB_HEADER_H*bindsUnit();
        WATERMARK.present=ClientModules.hud("Watermark");
        COORDS.w=coordinatesWidth(mc,dt); COORDS.h=KB_HEADER_H*bindsUnit();
        COORDS.present=ClientModules.hud("Coordinates");
        listEditing=editing;
        float[] list=listSize(dt);
        LIST.w=Math.max(list[0],editing ? 70 : 0); LIST.h=Math.max(list[1],editing ? ROW_H : 0);
        LIST.present=ClientModules.hud("ArrayList") && (list[1]>0 || editing);
        float[] potions=potionsSize(mc,dt,editing);
        POTIONS.w=potions[0]; POTIONS.h=potions[1];
        POTIONS.present=ClientModules.hud("Potions") && (effectsShown || editing);
        float[] binds=bindsSize(dt,editing);
        KEYBINDS.w=binds[0]; KEYBINDS.h=binds[1];
        KEYBINDS.present=ClientModules.hud("KeyBinds") && (bindsShown || editing);
        float[] player=HudPlayer.measure(dt,editing,bindsUnit(),mouseX-PLAYER.drawX,mouseY-PLAYER.drawY);
        PLAYER.present=player!=null;
        if(player!=null) { PLAYER.w=player[0]; PLAYER.h=player[1]; }
        float[] notes=HudNotifications.measure(mc,dt,editing,bindsUnit());
        NOTES.present=notes!=null;
        if(notes!=null) { NOTES.w=notes[0]; NOTES.h=notes[1]; }
        TARGET.present=updateTarget(mc,dt,editing);
        float[] targetSize=targetSize();
        TARGET.w=targetSize[0]; TARGET.h=targetSize[1];
        List<ArmorPiece> armor=armorPieces(mc,editing);
        ARMOR_HUD.present=ClientModules.hud("Armor") && !armor.isEmpty();
        if(!armor.isEmpty()) { ARMOR_HUD.w=armorWidth(armor); ARMOR_HUD.h=PILL_H; }
        var point=ess.ll.client.features.Waypoints.active();
        WAYPOINT.present=ClientModules.hud("Waypoint") && (point!=null || editing);
        WAYPOINT.w=waypointWidth(mc,point); WAYPOINT.h=WP_H;
        for(Widget w:WIDGETS) w.resolve();
        // The plates are as tall as the window pixels say: untouched widgets stack under them at any GUI scale.
        stackBelow(COORDS,WATERMARK);
        stackBelow(WAYPOINT,COORDS);
        // Untouched, the player sits at the top centre under any boss bars (vanilla: from y 12, 19 apart), notifications under it.
        int bars=((ess.ll.client.mixin.BossOverlayAccessor)mc.gui.hud.getBossOverlay()).ess$events().size();
        float guiH=screenH*scale, below=0;
        for(int i=0;i<bars && 12+19*i<guiH/3;i++) below=12+19*i+5+4;
        float top=Math.max(MARGIN,below/scale);
        if(untouched(PLAYER)) PLAYER.y=top;
        if(untouched(NOTES)) NOTES.y=PLAYER.present && untouched(PLAYER) ? PLAYER.y+PLAYER.h+GAP : top;
    }

    private static boolean darkPlate(Widget w) { return w==KEYBINDS || w==POTIONS || w==WATERMARK || w==COORDS || w==PLAYER || w==NOTES; }
    /** A position snapped to the window's pixels, in HUD units. */
    private static float pixelSnap(float v) {
        float pixels=scale*Minecraft.getInstance().getWindow().getGuiScale();
        return Math.round(v*pixels)/pixels;
    }
    private static boolean untouched(Widget w) { return w.ax==w.defX && w.ay==w.defY && w.ox==w.defOx && w.oy==w.defOy; }
    private static void stackBelow(Widget w,Widget above) { if(untouched(w)) w.y=Math.max(w.y,above.y+above.h+GAP); }
    private static void drawWidget(GuiGraphicsExtractor g,Minecraft mc,Widget w,double time) {
        boolean plate=darkPlate(w);
        float lift=w.lift, appear=plate ? 1 : outBack(w.vis), s=(.9f+.1f*appear)*(1+.035f*lift);
        alpha=Math.clamp(w.vis*1.3f,0,1)*(w==TARGET ? Math.clamp(targetShow*1.3f,0,1) : 1);
        g.pose().pushMatrix();
        try {
            if(plate && lift<=.001f) g.pose().translate(pixelSnap(w.drawX),pixelSnap(w.drawY));
            else {
                g.pose().translate(w.drawX+w.w/2,w.drawY+w.h/2-2*lift+(plate ? 0 : (1-outCubic(w.vis))*6));
                g.pose().scale(s,s);
                g.pose().translate(-w.w/2,-w.h/2);
            }
            if(lift>.01f) Theme.shadow(g,0,0,w.w,w.h,8,lift*alpha,2.2f);
            if(w==WATERMARK) watermark(g,mc,time);
            else if(w==COORDS) coordinates(g,mc);
            else if(w==LIST) moduleList(g,w,time);
            else if(w==POTIONS) effects(g,time);
            else if(w==KEYBINDS) keyBinds(g);
            else if(w==PLAYER || w==NOTES) {
                boolean circular=UiRenderer.circular;
                g.pose().pushMatrix();
                UiRenderer.circular=true;
                try {
                    g.pose().scale(bindsUnit(),bindsUnit());
                    if(w==PLAYER) HudPlayer.draw(g,alpha); else HudNotifications.draw(g,alpha);
                } finally { UiRenderer.circular=circular; g.pose().popMatrix(); }
            }
            else if(w==ARMOR_HUD) armor(g,mc);
            else if(w==WAYPOINT) waypoint(g,mc,time);
            else targetHud(g,mc);
        } finally { g.pose().popMatrix(); alpha=1; }
    }
    /** Colour with the current widget fade applied. */
    private static int c(int argb) { return Theme.withAlpha(argb,((argb>>>24)/255f)*alpha); }

    // ---- Watermark ----------------------------------------------------------------------------
    private enum PartKind { FPS, PING, TIME, BPS, NICK }
    /** One info part: value and a small dim unit after it ("144" "fps"); colour is the FPS / ping quality. */
    private record Part(PartKind kind,Icon icon,String value,String unit,int color) {}
    private static final float WM_SIZE=8, WM_LOGO=20, WM_UNIT=11;
    private static final int GOOD=0xff4fd18b, FAIR=0xffe8b04a, BAD=0xffff6b5e;
    private static List<Part> watermarkParts(Minecraft mc) {
        List<Part> parts=new ArrayList<>();
        int fps=mc.getFps();
        if(ClientModules.WM_FPS.get()) parts.add(new Part(PartKind.FPS,Icon.BOLT,String.valueOf(fps),"fps",fps>=60 ? GOOD : fps>=30 ? FAIR : BAD));
        int ping=ping(mc);
        if(ClientModules.WM_PING.get() && ping>=0) parts.add(new Part(PartKind.PING,Icon.SIGNAL,String.valueOf(ping),"ms",ping<80 ? GOOD : ping<150 ? FAIR : BAD));
        if(ClientModules.WM_TIME.get()) parts.add(new Part(PartKind.TIME,Icon.CLOCK,clock(),"",0));
        if(ClientModules.WM_BPS.get()) {
            double bps=Math.hypot(mc.player.getDeltaMovement().x,mc.player.getDeltaMovement().z)*20;
            parts.add(new Part(PartKind.BPS,Icon.MOVE,UiText.oneDecimal(bps),"bps",0));
        }
        if(ClientModules.WM_NICK.get()) parts.add(new Part(PartKind.NICK,Icon.USER,mc.player.getName().getString(),"",0));
        return parts;
    }
    /** Value plus its unit. */
    private static float valueWidth(Part p) {
        return KB_FONT.width(p.value(),KB_TEXT)+(p.unit().isEmpty() ? 0 : 3+KB_FONT.width(p.unit(),WM_UNIT));
    }
    private static float logoWidth() { return KB_PAD+WM_LOGO+7+KB_FONT.width("Maved",14)+KB_PAD; }
    /** Each part's width eases, so digits changing every frame do not shake the dividers after them. */
    private static final float[] WM_WIDTHS=new float[PartKind.values().length];
    private static float wmW;
    private static float watermarkWidth(Minecraft mc,double dt) {
        float w=logoWidth();
        for(Part p:watermarkParts(mc)) {
            int i=p.kind().ordinal();
            float target=2*KB_PAD+valueWidth(p);
            WM_WIDTHS[i]=WM_WIDTHS[i]==0 ? target : approach(WM_WIDTHS[i],target,dt,KB_SPEED);
            w+=WM_WIDTHS[i];
        }
        wmW=wmW==0 ? w : approach(wmW,w,dt,KB_SPEED);
        return wmW*bindsUnit();
    }
    /** One line on the KeyBinds plate: the logo on its dark band, then the parts, a glowing divider after each element. */
    private static void watermark(GuiGraphicsExtractor g,Minecraft mc,double time) {
        List<Part> parts=watermarkParts(mc);
        float h=KB_HEADER_H, band=logoWidth();
        boolean circular=UiRenderer.circular;
        g.pose().pushMatrix();
        UiRenderer.circular=true;
        try {
            g.pose().scale(bindsUnit(),bindsUnit());
            Theme.plate(g,0,0,wmW,h,KB_RADIUS,alpha);
            if(!parts.isEmpty()) Theme.plateBand(g,0,0,wmW,h,false,band,KB_RADIUS,alpha);
            UiRenderer.rounded(g,KB_PAD,h/2-WM_LOGO/2,WM_LOGO,WM_LOGO,6,Gradient.flow(c(Theme.accent()),c(Theme.accentLight()),Gradient.Axis.VERTICAL,0,40,time*.3));
            Icons.draw(g,Icon.MARK,KB_PAD+3,h/2-7,14,c(Theme.onAccent()));
            KB_FONT.mid(g,"Maved",KB_PAD+WM_LOGO+7,h/2,14,c(Theme.PLATE_TEXT));
            float x=band;
            for(Part p:parts) {
                wmDivider(g,x,h);
                float tx=x+KB_PAD;
                KB_FONT.mid(g,p.value(),tx,h/2,KB_TEXT,c(p.color()!=0 ? p.color() : Theme.PLATE_TEXT));
                if(!p.unit().isEmpty()) KB_FONT.mid(g,p.unit(),tx+KB_FONT.width(p.value(),KB_TEXT)+3,h/2+.5f,WM_UNIT,c(Theme.PLATE_HINT));
                x+=WM_WIDTHS[p.kind().ordinal()];
            }
        } finally { UiRenderer.circular=circular; g.pose().popMatrix(); }
    }
    /** The KeyBinds header line stood upright: bright in the middle, gone at both ends. */
    private static void wmDivider(GuiGraphicsExtractor g,float x,float h) { plateDivider(g,x,h,alpha); }
    static void plateDivider(GuiGraphicsExtractor g,float x,float h,float opacity) {
        int line=Theme.withAlpha(plateLine(1),.9f*Math.clamp(opacity,0,1)), edge=Theme.withAlpha(Theme.accent(),0);
        float half=h/2-4;
        UiRenderer.rounded(g,x-.7f,4,1.4f,half,0,Gradient.linear(edge,line,Gradient.Axis.VERTICAL,4,half));
        UiRenderer.rounded(g,x-.7f,4+half,1.4f,half,0,Gradient.linear(line,edge,Gradient.Axis.VERTICAL,4+half,half));
    }
    private static String clock() { return UiText.clock(); }

    // ---- shared look: header pill, glow ------------------------------------------------
    /** Soft halo behind an icon, in the icon's colour; off with the "Свечение иконок" setting. */
    private static void glow(GuiGraphicsExtractor g,float cx,float cy,float size,int color) {
        if(!ClientModules.HUD_GLOW.get() || alpha<=.01f) return;
        // Stacked SDF discs (true circles) fading outward give a round halo instead of a rounded plate.
        for(int i=0;i<6;i++) {
            float d=size*(.7f+.32f*i), box=d/.875f;
            Icons.draw(g,Icon.DOT,cx-box/2,cy-box/2,box,c(Theme.withAlpha(color,.085f*(1-i/7f))));
        }
    }
    /** Row show / hide progress kept per key; finished hides are dropped. */
    private static <K> float slide(Map<K,Float> map,K key,boolean show,double dt) {
        float v=approach(map.getOrDefault(key,0f),show?1:0,dt,show?14:11);
        if(v<=0 && !show) { map.remove(key); return 0; }
        map.put(key,v);
        return v;
    }
    private static float smooth(float t) { return t*t*(3-2*t); }

    // ---- KeyBinds -----------------------------------------------------------------------------
    /** Dark plate: a header with the title and a glowing line under it, then one row per enabled bind. */
    static final float KB_MIN_W=110, KB_RADIUS=12, KB_HEADER_H=33, KB_ROWS_TOP=36, KB_ROW_H=20, KB_PAD=10, KB_TEXT=13, KB_SPEED=30;
    private static final int KB_FADE_OFFSET=-10;
    private static final class BindRow {
        float alpha, offset=KB_FADE_OFFSET, y;
        BindRow(float y) { this.y=y; }
    }
    private static final Map<ClientModules.Module,BindRow> BIND_ROWS=new java.util.LinkedHashMap<>();
    private static final List<ClientModules.Module> BINDS=new ArrayList<>();
    private static float bindsW, bindsH;
    private static String categoryGlyph(String category) {
        return switch (category) {
            case "Movement" -> "c";
            case "Render" -> "d";
            case "Player" -> "o";
            case "Misc" -> "f";
            default -> "b";
        };
    }
    private static String keyLabel(ClientModules.Module m) { return UiText.upper(ess.ll.client.command.Keys.label(m.bind)); }
    /** The plate is laid out in window pixels: its size does not follow the GUI scale. */
    private static float bindsUnit() { return 1f/Minecraft.getInstance().getWindow().getGuiScale(); }
    /** True while there is an enabled bind; the widget hides with the last one. */
    private static boolean bindsShown;
    /** Advances the plate and its rows; returns {width, height}. */
    private static float[] bindsSize(double dt,boolean editing) {
        boolean visible=ClientModules.hud("KeyBinds");
        BINDS.clear();
        if(visible) for(var m:ClientModules.all()) if(m.bind>=0 && m!=ClientModules.CLICK_GUI && m.enabled()) BINDS.add(m);
        bindsShown=!BINDS.isEmpty();
        float name=0, key=0, icon=0;
        for(int i=0;i<BINDS.size();i++) {
            var m=BINDS.get(i);
            name=Math.max(name,KB_FONT.width(m.name,KB_TEXT));
            key=Math.max(key,KB_FONT.width(keyLabel(m),KB_TEXT));
            icon=Math.max(icon,KB_ICONS.width(categoryGlyph(m.category),KB_TEXT));
            float y=i*KB_ROW_H;
            BindRow row=BIND_ROWS.computeIfAbsent(m,x->new BindRow(y));
            row.alpha=approach(row.alpha,1,dt,KB_SPEED);
            row.offset=approach(row.offset,0,dt,KB_SPEED);
            row.y=approach(row.y,y,dt,KB_SPEED);
        }
        var rows=BIND_ROWS.entrySet().iterator();
        while(rows.hasNext()) {
            var entry=rows.next();
            if(BINDS.contains(entry.getKey())) continue;
            // The last rows stay on the plate and fade out with it, so it never empties into a bare grey box.
            if(BINDS.isEmpty() && !editing) { if(KEYBINDS.vis<=.004f) rows.remove(); continue; }
            BindRow row=entry.getValue();
            row.alpha=approach(row.alpha,0,dt,KB_SPEED);
            row.offset=approach(row.offset,KB_FADE_OFFSET,dt,KB_SPEED);
            if(row.alpha<=0) rows.remove();
        }
        float w=Math.max(KB_MIN_W,name+key+icon+50), h=BINDS.isEmpty() ? KB_ROWS_TOP : KB_ROWS_TOP+BINDS.size()*KB_ROW_H+7;
        // The plate shows at its full size and keeps it while it fades out.
        boolean jump=bindsH==0 || KEYBINDS.vis<=.004f, hold=BINDS.isEmpty() && !editing;
        if(!hold || jump) {
            bindsW=jump ? w : approach(bindsW,w,dt,KB_SPEED);
            bindsH=jump ? h : approach(bindsH,h,dt,KB_SPEED);
        }
        return new float[]{bindsW*bindsUnit(),bindsH*bindsUnit()};
    }
    /** The glowing line's colour, breathing between the two accents. */
    private static int plateLine(float strength) {
        int main=Theme.accent();
        return Theme.withAlpha(Theme.mix(main,Theme.mix(main,0xffffffff,.45f),.5f+.5f*(float)Math.sin(System.currentTimeMillis()/700.0)),.9f*strength);
    }
    /** The plate with its dark header, the title and the glowing line under it. */
    private static void plateHeader(GuiGraphicsExtractor g,String title,float w,float h) {
        Theme.plate(g,0,0,w,h,KB_RADIUS,alpha);
        Theme.plateBand(g,0,0,w,h,true,Math.min(h,KB_HEADER_H),KB_RADIUS,alpha);
        float fade=Math.clamp((h-KB_HEADER_H)/10,0,1);
        if(fade>.01f) {
            // The line glows at its middle and fades out to both ends; its colour breathes between the two accents.
            int edge=Theme.withAlpha(Theme.accent(),0), line=plateLine(fade);
            float half=(w-KB_PAD*2)/2;
            UiRenderer.rounded(g,KB_PAD,KB_HEADER_H,half,1.4f,0,Gradient.linear(c(edge),c(line),Gradient.Axis.HORIZONTAL,KB_PAD,half));
            UiRenderer.rounded(g,KB_PAD+half,KB_HEADER_H,half,1.4f,0,Gradient.linear(c(line),c(edge),Gradient.Axis.HORIZONTAL,KB_PAD+half,half));
        }
        KB_FONT.draw(g,title,KB_PAD,9,14,c(Theme.PLATE_TEXT));
    }
    private static void keyBinds(GuiGraphicsExtractor g) {
        float w=bindsW, h=bindsH, saved=alpha;
        boolean circular=UiRenderer.circular;
        g.pose().pushMatrix();
        UiRenderer.circular=true;
        try {
            g.pose().scale(bindsUnit(),bindsUnit());
            plateHeader(g,"Keybinds",w,h);
            KB_ICONS.draw(g,"@",w-26,8,16,c(Theme.accent()));
            for(var entry:BIND_ROWS.entrySet()) {
                var m=entry.getKey();
                BindRow row=entry.getValue();
                alpha=saved*row.alpha;
                float top=KB_ROWS_TOP+row.y+row.offset;
                String key=keyLabel(m);
                float keyX=w-KB_PAD-KB_FONT.width(key,KB_TEXT);
                KB_FONT.draw(g,m.name,KB_PAD,top+2.5f,KB_TEXT,c(Theme.PLATE_TEXT));
                KB_FONT.draw(g,key,keyX,top+2.5f,KB_TEXT,c(Theme.PLATE_HINT));
                KB_ICONS.draw(g,categoryGlyph(m.category),keyX-18,top+3,KB_TEXT,c(Theme.accent()));
            }
        } finally { alpha=saved; UiRenderer.circular=circular; g.pose().popMatrix(); }
    }
    /** Latency in ms, or -1 in singleplayer / before the tab list knows it. */
    private static int ping(Minecraft mc) {
        if(mc.getSingleplayerServer()!=null || mc.getConnection()==null) return -1;
        var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info==null ? -1 : info.getLatency();
    }

    // ---- Coordinates --------------------------------------------------------------------------
    private static String[] coordinateValues(Minecraft mc) {
        var p=mc.player;
        return new String[]{fmt(p.getX()),fmt(p.getY()),fmt(p.getZ())};
    }
    /** The same line as Watermark: the pin on the dark band, then X, Y and Z, a divider after each. */
    private static final String[] AXES={"X","Y","Z"};
    private static final float[] AXIS_WIDTHS=new float[3];
    private static final float COORD_BAND=KB_PAD*2+14;
    private static float coordsW;
    private static float coordinatesWidth(Minecraft mc,double dt) {
        String[] values=coordinateValues(mc);
        float w=COORD_BAND;
        for(int i=0;i<3;i++) {
            float target=2*KB_PAD+KB_FONT.width(AXES[i],WM_UNIT)+4+KB_FONT.width(values[i],KB_TEXT);
            AXIS_WIDTHS[i]=AXIS_WIDTHS[i]==0 ? target : approach(AXIS_WIDTHS[i],target,dt,KB_SPEED);
            w+=AXIS_WIDTHS[i];
        }
        coordsW=w;
        return w*bindsUnit();
    }
    private static void coordinates(GuiGraphicsExtractor g,Minecraft mc) {
        String[] values=coordinateValues(mc);
        float h=KB_HEADER_H;
        boolean circular=UiRenderer.circular;
        g.pose().pushMatrix();
        UiRenderer.circular=true;
        try {
            g.pose().scale(bindsUnit(),bindsUnit());
            Theme.plate(g,0,0,coordsW,h,KB_RADIUS,alpha);
            Theme.plateBand(g,0,0,coordsW,h,false,COORD_BAND,KB_RADIUS,alpha);
            Icons.draw(g,Icon.PIN,KB_PAD,h/2-7,14,2.2f,c(Theme.accent()));
            float x=COORD_BAND;
            for(int i=0;i<3;i++) {
                wmDivider(g,x,h);
                float tx=x+KB_PAD;
                KB_FONT.mid(g,AXES[i],tx,h/2+.5f,WM_UNIT,c(Theme.PLATE_HINT));
                KB_FONT.mid(g,values[i],tx+KB_FONT.width(AXES[i],WM_UNIT)+4,h/2,KB_TEXT,c(Theme.PLATE_TEXT));
                x+=AXIS_WIDTHS[i];
            }
        } finally { UiRenderer.circular=circular; g.pose().popMatrix(); }
    }
    private static String fmt(double v) { return UiText.oneDecimal(v); }

    // ---- Waypoint -----------------------------------------------------------------------------
    /*
     * The active .way point: a round tile with an arrow that turns towards it as you turn, its name and the
     * metres left with its x/z. In the HUD editor with no point it shows a sample.
     */
    private static final float WP_H=30, WP_TILE=22;
    private static float[] wpArrow={0};
    private static String[] waypointText(Minecraft mc,ess.ll.client.features.Waypoints.Point p) {
        if(p==null) return new String[]{"Метка","120 м · 120 64"};
        return new String[]{p.name(),WaypointMarker.distance(mc,p)+" · "+p.x()+" "+p.z()};
    }
    private static float waypointWidth(Minecraft mc,ess.ll.client.features.Waypoints.Point p) {
        String[] t=waypointText(mc,p);
        return 4+WP_TILE+7+Math.max(FONT.width(t[0],8.5f),FONT.width(t[1],7))+9;
    }
    private static void waypoint(GuiGraphicsExtractor g,Minecraft mc,double time) {
        var p=ess.ll.client.features.Waypoints.active();
        String[] t=waypointText(mc,p);
        float h=WP_H;
        Theme.surface(g,0,0,WAYPOINT.w,h,10,alpha);
        float cx=4+WP_TILE/2,cy=h/2;
        // The tile glows with the accent; the arrow eases round instead of snapping, and pulses when you are there.
        float target=p==null ? (float)Math.toRadians(35) : WaypointMarker.bearing(mc,p);
        float diff=(float)Math.atan2(Math.sin(target-wpArrow[0]),Math.cos(target-wpArrow[0]));
        wpArrow[0]+=diff*.25f;
        boolean there=p!=null && WaypointMarker.distanceBlocks(mc,p)<4;
        float pulse=there ? (float)(.5+.5*Math.sin(time*6)) : 0;
        UiRenderer.shadow(g,cx-WP_TILE/2,cy-WP_TILE/2,WP_TILE,WP_TILE,WP_TILE/2,6,c(Theme.withAlpha(Theme.accent(),.35f+.3f*pulse)));
        UiRenderer.rounded(g,cx-WP_TILE/2,cy-WP_TILE/2,WP_TILE,WP_TILE,WP_TILE/2,c(Theme.withAlpha(Theme.accent(),.2f+.15f*pulse)));
        g.pose().pushMatrix();
        try {
            g.pose().translate(cx,cy);
            g.pose().rotate(there ? 0 : wpArrow[0]);
            Icons.draw(g,there ? Icon.CHECK : Icon.NAV,-6,-6,12,c(Theme.mix(Theme.accent(),0xffffffff,.3f)));
        } finally { g.pose().popMatrix(); }
        float tx=4+WP_TILE+7;
        FONT.draw(g,t[0],tx,mid(0,h*.5f+2,8.5f)+2,8.5f,c(Theme.text()));
        FONT.draw(g,there ? "Ты на месте" : t[1],tx,mid(h*.5f-2,h,7)-1,7,c(there ? Theme.accent() : Theme.hint()));
    }

    // ---- Armor --------------------------------------------------------------------------------
    /** One worn piece; durability is -1 for items that cannot break (pumpkin, head). */
    private record ArmorPiece(Icon icon,float durability) {}
    private static final EquipmentSlot[] ARMOR_SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final Icon[] ARMOR_ICONS={Icon.HELMET,Icon.CHESTPLATE,Icon.LEGGINGS,Icon.BOOTS};
    /** Worn pieces head to feet; while editing with nothing worn, a full set as a placeholder. */
    private static List<ArmorPiece> armorPieces(Minecraft mc,boolean editing) {
        List<ArmorPiece> pieces=new ArrayList<>();
        for(int i=0;i<ARMOR_SLOTS.length;i++) {
            ItemStack stack=mc.player.getItemBySlot(ARMOR_SLOTS[i]);
            if(stack.isEmpty()) continue;
            pieces.add(new ArmorPiece(ARMOR_ICONS[i],stack.isDamageableItem() ? 1-stack.getDamageValue()/(float)stack.getMaxDamage() : -1));
        }
        if(pieces.isEmpty() && editing) for(Icon icon:ARMOR_ICONS) pieces.add(new ArmorPiece(icon,1));
        return pieces;
    }
    private static String armorValue(ArmorPiece p) { return p.durability()<0 ? "" : String.valueOf(Math.round(p.durability()*100)); }
    private static float armorCellWidth(ArmorPiece p) {
        String v=armorValue(p);
        return 5+9+(v.isEmpty() ? 0 : 3+FONT.width(v,WM_SIZE))+5;
    }
    private static float armorWidth(List<ArmorPiece> pieces) {
        float w=4;
        for(ArmorPiece p:pieces) w+=armorCellWidth(p);
        return w;
    }
    /**
     * Light pill above the hotbar: per piece an icon tinted by durability (accent, orange under half, red under a
     * quarter) and the percentage, divided by thin lines.
     */
    private static void armor(GuiGraphicsExtractor g,Minecraft mc) {
        List<ArmorPiece> pieces=armorPieces(mc,editMode);
        if(pieces.isEmpty()) return;
        float h=PILL_H, x=2;
        Theme.surface(g,0,0,ARMOR_HUD.w,h,h/2,alpha);
        for(int i=0;i<pieces.size();i++) {
            ArmorPiece p=pieces.get(i);
            float d=p.durability();
            int color=d<0 ? Theme.accent() : healthColor(d);
            if(i>0) UiRenderer.rounded(g,x-.5f,h/2-4,1,8,.5f,c(Theme.withAlpha(Theme.text(),.1f)));
            Icons.draw(g,p.icon(),x+5,h/2-4.5f,9,2.2f,c(color));
            String v=armorValue(p);
            if(!v.isEmpty()) FONT.draw(g,v,x+5+9+3,mid(0,h,WM_SIZE),WM_SIZE,c(d<.25f ? color : Theme.text()));
            x+=armorCellWidth(p);
        }
    }

    // ---- ArrayList ----------------------------------------------------------------------------
    private static final List<ClientModules.Module> LIST_ROWS=new ArrayList<>();
    /** Advances the slide animations and returns {width, height} of the visible rows. */
    private static float[] listSize(double dt) {
        boolean visible=ClientModules.hud("ArrayList");
        LIST_ROWS.clear();
        for(var m:ClientModules.all()) {
            if(m==ClientModules.HUD || m==ClientModules.THEME || m==ClientModules.COSMETICS || m==ClientModules.CLICK_GUI) continue;
            float target=visible && m.enabled() ? 1 : 0;
            float value=SLIDE.getOrDefault(m,0f);
            value+=(target-value)*(float)(1-Math.exp(-dt*14));
            if(Math.abs(value-target)<.01f) value=target;
            if(value<=0) { SLIDE.remove(m); continue; }
            SLIDE.put(m,value); LIST_ROWS.add(m);
        }
        LIST_ROWS.sort(Comparator.comparingDouble((ClientModules.Module m)->-FONT.width(m.name,8.5f)).thenComparing(m->m.name));
        float w=0, h=0;
        for(var m:LIST_ROWS) {
            float t=SLIDE.get(m), ease=t*t*(3-2*t);
            w=Math.max(w,rowWidth(m));
            h+=(ROW_H+2)*ease;
        }
        return new float[]{w,Math.max(0,h-2)};
    }
    /** The empty list only shows its name in the HUD editor, as something to drag. */
    private static boolean listEditing;
    private static float rowWidth(ClientModules.Module m) { return FONT.width(m.name,8.5f)+(ClientModules.LIST_LINE.get()?16:12); }
    private static void moduleList(GuiGraphicsExtractor g,Widget widget,double time) {
        // Rows hug the side of the screen the widget sits on and slide out towards it.
        boolean right=widget.drawX+widget.w/2>screenW/2;
        float size=8.5f, y=0;
        float saved=alpha;
        if(LIST_ROWS.isEmpty() && listEditing) FONT.draw(g,"ArrayList",right ? widget.w-FONT.width("ArrayList",8)-2 : 2,mid(0,ROW_H,8),8,c(0x80ffffff));
        for(var m:LIST_ROWS) {
            float t=SLIDE.get(m), ease=t*t*(3-2*t);
            float w=rowWidth(m), h=ROW_H*ease, slide=(w+MARGIN+4)*(1-ease);
            float x=right ? widget.w-w+slide : -slide;
            int color=Gradient.flow(Theme.accent(),Theme.accentLight(),Gradient.Axis.VERTICAL,0,90,time*.25).at(0,y);
            if(h>1) {
                alpha=saved*ease;
                Theme.surface(g,x,y,w,h,5,alpha);
                if(ClientModules.LIST_LINE.get()) UiRenderer.rounded(g,right ? x+w-4 : x+2.4f,y+3,1.6f,Math.max(0,h-6),.8f,c(color));
                FONT.draw(g,m.name,right ? x+6 : x+10,mid(y,ROW_H,size),size,c(Theme.text()));
                alpha=saved;
            }
            y+=(ROW_H+2)*ease;
        }
    }

    // ---- Potions ------------------------------------------------------------------------------
    /** The same plate as KeyBinds: one row per effect, a removed effect keeps its last state while it fades out. */
    private static final Map<Holder<MobEffect>,BindRow> EFFECT_ROWS=new HashMap<>();
    private static final Map<Holder<MobEffect>,MobEffectInstance> EFFECT_LAST=new HashMap<>();
    private static final List<MobEffectInstance> EFFECTS=new ArrayList<>();
    private static final int LOW_COLOR=0xffff6b5e;
    private static final float EFFECT_ICON=13;
    private static float potionsW, potionsH;
    private static String effectName(MobEffectInstance e) { return e.getEffect().value().getDisplayName().getString(); }
    private static String effectLevel(MobEffectInstance e) { return e.getAmplifier()>0 ? roman(e.getAmplifier()+1) : ""; }
    private static String effectTime(MobEffectInstance e) { return e.isInfiniteDuration() ? "--:--" : formatTicks(e.getDuration()); }
    /** Harmful effects last, longer ones first. */
    private static final Comparator<MobEffectInstance> EFFECT_ORDER=
            Comparator.comparing((MobEffectInstance e)->!e.getEffect().value().isBeneficial()).thenComparingInt(e->-e.getDuration());
    private static boolean effectsShown;
    private static float[] potionsSize(Minecraft mc,double dt,boolean editing) {
        EFFECTS.clear();
        if(ClientModules.hud("Potions")) for(var e:mc.player.getActiveEffects()) if(e.showIcon()) EFFECTS.add(e);
        EFFECTS.sort(EFFECT_ORDER);
        effectsShown=!EFFECTS.isEmpty();
        float name=0, time=0;
        for(int i=0;i<EFFECTS.size();i++) {
            var e=EFFECTS.get(i);
            String level=effectLevel(e);
            name=Math.max(name,KB_FONT.width(effectName(e),KB_TEXT)+(level.isEmpty() ? 0 : 4+KB_FONT.width(level,KB_TEXT)));
            time=Math.max(time,KB_FONT.width(effectTime(e),KB_TEXT));
            EFFECT_LAST.put(e.getEffect(),e);
            float y=i*KB_ROW_H;
            BindRow row=EFFECT_ROWS.computeIfAbsent(e.getEffect(),x->new BindRow(y));
            row.alpha=approach(row.alpha,1,dt,KB_SPEED);
            row.offset=approach(row.offset,0,dt,KB_SPEED);
            row.y=approach(row.y,y,dt,KB_SPEED);
        }
        var rows=EFFECT_ROWS.entrySet().iterator();
        while(rows.hasNext()) {
            var entry=rows.next();
            boolean active=false;
            for(var e:EFFECTS) active|=e.getEffect()==entry.getKey();
            if(active) continue;
            if(EFFECTS.isEmpty() && !editing) { if(POTIONS.vis<=.004f) { EFFECT_LAST.remove(entry.getKey()); rows.remove(); } continue; }
            BindRow row=entry.getValue();
            row.alpha=approach(row.alpha,0,dt,KB_SPEED);
            row.offset=approach(row.offset,KB_FADE_OFFSET,dt,KB_SPEED);
            if(row.alpha<=0) { EFFECT_LAST.remove(entry.getKey()); rows.remove(); }
        }
        float w=Math.max(KB_MIN_W,name+time+EFFECT_ICON+50), h=EFFECTS.isEmpty() ? KB_ROWS_TOP : KB_ROWS_TOP+EFFECTS.size()*KB_ROW_H+7;
        boolean jump=potionsH==0 || POTIONS.vis<=.004f, hold=EFFECTS.isEmpty() && !editing;
        if(!hold || jump) {
            potionsW=jump ? w : approach(potionsW,w,dt,KB_SPEED);
            potionsH=jump ? h : approach(potionsH,h,dt,KB_SPEED);
        }
        return new float[]{potionsW*bindsUnit(),potionsH*bindsUnit()};
    }
    private static void effects(GuiGraphicsExtractor g,double time) {
        float w=potionsW, h=potionsH, saved=alpha;
        boolean circular=UiRenderer.circular;
        g.pose().pushMatrix();
        UiRenderer.circular=true;
        try {
            g.pose().scale(bindsUnit(),bindsUnit());
            plateHeader(g,"Potions",w,h);
            Icons.draw(g,Icon.FLASK,w-25,9,14,2.2f,c(Theme.accent()));
            for(var entry:EFFECT_ROWS.entrySet()) {
                var effect=EFFECT_LAST.get(entry.getKey());
                BindRow row=entry.getValue();
                float top=KB_ROWS_TOP+row.y+row.offset;
                boolean low=!effect.isInfiniteDuration() && effect.getDuration()<ClientModules.POT_THRESHOLD.get()*20;
                float blink=low && ClientModules.POT_BLINK.get() ? .55f+.45f*(float)(.5+.5*Math.cos(time*Math.PI*2*1.2)) : 1;
                alpha=saved*row.alpha;
                String name=effectName(effect), level=effectLevel(effect), duration=effectTime(effect);
                KB_FONT.draw(g,name,KB_PAD,top+2.5f,KB_TEXT,c(Theme.PLATE_TEXT));
                if(!level.isEmpty()) KB_FONT.draw(g,level,KB_PAD+KB_FONT.width(name,KB_TEXT)+4,top+2.5f,KB_TEXT,c(Theme.accent()));
                float timeX=w-KB_PAD-KB_FONT.width(duration,KB_TEXT);
                alpha=saved*row.alpha*blink;
                KB_FONT.draw(g,duration,timeX,top+2.5f,KB_TEXT,c(low ? LOW_COLOR : Theme.PLATE_HINT));
                alpha=saved*row.alpha;
                g.pose().pushMatrix();
                try {
                    g.pose().translate(timeX-18,top+3);
                    g.pose().scale(EFFECT_ICON/16f,EFFECT_ICON/16f);
                    g.blitSprite(RenderPipelines.GUI_TEXTURED,Hud.getMobEffectSprite(effect.getEffect()),0,0,16,16,c(0xffffffff));
                } finally { g.pose().popMatrix(); }
            }
        } finally { alpha=saved; UiRenderer.circular=circular; g.pose().popMatrix(); }
    }

    private static String formatTicks(int ticks) {
        return UiText.duration(ticks/20);
    }
    private static String roman(int n) {
        String[] r={"","I","II","III","IV","V","VI","VII","VIII","IX","X"};
        return n>=0 && n<r.length ? r[n] : String.valueOf(n);
    }

    // ---- TargetHud ----------------------------------------------------------------------------
    /** Picks the target and advances the card (TargetCard); returns whether the card should show. */
    private static boolean updateTarget(Minecraft mc,double dt,boolean editing) {
        LivingEntity next=null;
        if(ClientModules.hud("TargetHud")) {
            var source=targetSource;
            if(source!=null) next=source.get();
            else if(mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive()) next=living;
            // Preview on the player while chat is open so the card can be inspected and moved.
            if(next==null && editing) next=mc.player;
        }
        if(next!=null) target=next;
        targetShow=approach(targetShow,next!=null?1:0,dt,next!=null?11:9);
        // The last target stays referenced until the fade-out has finished.
        if(target==null || targetShow<=.004f) { if(next==null) { target=null; TargetCard.reset(); } return false; }
        TargetCard.update(target,dt);
        return true;
    }
    private static float[] targetSize() { return TargetCard.size(target); }
    private static void targetHud(GuiGraphicsExtractor g,Minecraft mc) {
        if(target!=null) TargetCard.draw(g,target,alpha);
    }
    private static int healthColor(float ratio) { return ratio>.5f ? Theme.accent() : ratio>.25f ? 0xffe89a2e : 0xffe0454f; }
    /** Odometer digits: each place springs to its digit with an overshoot; a leading blank place folds away. */
    static final class Roller {
        final float[] pos=new float[3], vel=new float[3], width=new float[3];
        boolean init;
        void update(int value,double dt) {
            String s=String.valueOf(Math.clamp(value,0,999));
            s=" ".repeat(3-s.length())+s;
            for(int k=0;k<3;k++) {
                char ch=s.charAt(k);
                float target=ch==' ' ? 0 : ch-'0'+1;
                if(!init) { pos[k]=target; width[k]=ch==' '?0:1; continue; }
                // Underdamped spring: digits roll past their target a little and settle.
                for(int i=0;i<2;i++) {
                    float h=(float)Math.min(dt,1/30.0)/2;
                    vel[k]+=((target-pos[k])*170-vel[k]*15)*h;
                    pos[k]+=vel[k]*h;
                }
                width[k]=approach(width[k],ch==' '?0:1,dt,14);
            }
            init=true;
        }
        float width(float digit) { return (width[0]+width[1]+width[2])*digit; }
        /** Draws starting at x; each place is digit wide, the glyph rolls vertically and fades between values. */
        void draw(GuiGraphicsExtractor g,float x,float top,float size,float digit,int color,boolean shadow) {
            // Clipped to the digit line, like a counter window; the glyphs also fade as they pass. The clip is
            // rounded in screen pixels, not HUD units, so small numbers keep their tops and bottoms.
            float travel=size*1.3f;
            var pose=g.pose();
            var a0=pose.transformPosition(x-2,top+size*.02f,new org.joml.Vector2f());
            var a1=pose.transformPosition(x+width(digit)+2,top+size*1.16f,new org.joml.Vector2f());
            int sx=(int)Math.floor(Math.min(a0.x,a1.x)), sy=(int)Math.floor(Math.min(a0.y,a1.y));
            g.scissorStack.push(new net.minecraft.client.gui.navigation.ScreenRectangle(sx,sy,
                    (int)Math.ceil(Math.max(a0.x,a1.x))-sx,(int)Math.ceil(Math.max(a0.y,a1.y))-sy));
            try {
                for(int k=0;k<3;k++) {
                    if(width[k]<.01f) continue;
                    float p=Math.clamp(pos[k],-.5f,10.5f), base=(float)Math.floor(p), f=p-base, a=Math.min(1,width[k]*1.5f);
                    glyph(g,(int)base,x,top-f*travel,size,digit,Theme.withAlpha(color,((color>>>24)/255f)*(1-f)*a),shadow);
                    if(f>.01f) glyph(g,(int)base+1,x,top+(1-f)*travel,size,digit,Theme.withAlpha(color,((color>>>24)/255f)*f*a),shadow);
                    x+=digit*width[k];
                }
            } finally { g.scissorStack.pop(); }
        }
        private static void glyph(GuiGraphicsExtractor g,int index,float x,float top,float size,float digit,int color,boolean shadow) {
            if(index<1 || index>10 || (color>>>24)<3) return;
            String d=String.valueOf((char)('0'+index-1));
            float gx=x+(digit-FONT.width(d,size))/2;
            if(shadow) FONT.shadowed(g,d,gx,top,size,color); else FONT.draw(g,d,gx,top,size,color);
        }
    }
    /** Pushes a clip rectangle given in local coordinates, rounded in screen pixels. */
    static void pushClip(GuiGraphicsExtractor g,float x0,float y0,float x1,float y1) {
        var pose=g.pose();
        var a=pose.transformPosition(x0,y0,new org.joml.Vector2f());
        var b=pose.transformPosition(x1,y1,new org.joml.Vector2f());
        int sx=Math.round(Math.min(a.x,b.x)), sy=Math.round(Math.min(a.y,b.y));
        g.scissorStack.push(new net.minecraft.client.gui.navigation.ScreenRectangle(sx,sy,
                Math.max(0,Math.round(Math.max(a.x,b.x))-sx),Math.max(0,Math.round(Math.max(a.y,b.y))-sy)));
    }
    // ---- settings popup ---------------------------------------------------------------------------
    private static final float POP_W=114, POP_ROW=13, POP_TEXT=7, POP_HEAD=15, POP_CAPTION=10, POP_SEP=5, POP_LABEL=42, POP_PAD=6;
    /** Divider between groups of a popup. */
    private static final Object SEP=new Object();
    /** Rows of a widget's popup: settings, group captions (String) and SEP; null when the widget has none. */
    private static List<Object> popupItems(Widget w) {
        List<Object> list=new ArrayList<>();
        if(w==TARGET) list.addAll(List.of(ClientModules.TARGET_STYLE,SEP,"Частицы",ClientModules.TARGET_PARTICLES,ClientModules.TARGET_PARTICLE_COUNT,
                ClientModules.TARGET_PARTICLE_MODE,SEP,ClientModules.TARGET_FROM_HEAD,ClientModules.TARGET_FROM_BAR));
        else if(w==WATERMARK) list.addAll(List.of(ClientModules.WM_FPS,ClientModules.WM_PING,ClientModules.WM_TIME,ClientModules.WM_BPS,
                ClientModules.WM_NICK,SEP,ClientModules.HUD_GLOW));
        else if(w==PLAYER) list.add(ClientModules.PLAYER_LYRICS);
        else if(w==NOTES) list.addAll(List.of(ClientModules.NOTE_MODULES,ClientModules.NOTE_EFFECTS,ClientModules.NOTE_FRIENDS));
        else if(w==POTIONS) list.addAll(List.of(ClientModules.POT_BLINK,ClientModules.POT_THRESHOLD));
        else if(w!=KEYBINDS) return null;
        if(!list.isEmpty()) list.add(SEP);
        list.add(RESET_ROWS.computeIfAbsent(w,x->new ActionSetting("Сбросить позицию",x::reset)));
        return list;
    }
    private static float itemHeight(Object item) { return item==SEP ? POP_SEP : item instanceof String ? POP_CAPTION : POP_ROW; }
    private static Icon popupIcon(Widget w) {
        return w==TARGET ? Icon.CROSSHAIR : w==WATERMARK ? Icon.STAR : w==KEYBINDS ? Icon.KEYBOARD : w==POTIONS ? Icon.FLASK : Icon.GRID;
    }
    /** {x, y, w, h} beside the widget, flipped to the other side when it would leave the screen. */
    private static float[] popupRect(Widget w) {
        float h=POP_HEAD+HEAD_GAP+BODY_PAD*2;
        for(Object item:popupItems(w)) h+=itemHeight(item);
        float x=w.x+w.w+6, y=w.y;
        if(x+POP_W>screenW-4) x=w.x-POP_W-6;
        // The chat input bar covers the bottom 16 px while editing.
        return new float[]{Math.clamp(x,4,Math.max(4,screenW-POP_W-4)),Math.clamp(y,4,Math.max(4,screenH-h-18)),POP_W,h};
    }
    private static float popupHeaderWidth(Widget w) { return 5+9+4+FONT.width(w.label,7.5f)+6+5+5; }
    private static float sliderX(float[] r) { return r[0]+POP_LABEL; }
    private static float sliderW(float[] r) { return r[2]-POP_LABEL-POP_PAD-16; }
    private static void dragSlider(SliderSetting s,float mouse) {
        float[] r=popupRect(popupShown);
        s.set(s.min+Math.clamp((mouse-sliderX(r))/sliderW(r),0,1)*(s.max-s.min));
    }
    private static float popupAnim(Object key,float target,double dt,float speed) {
        float v=approach(POPUP_ANIM.getOrDefault(key,target),target,dt,speed);
        POPUP_ANIM.put(key,v);
        return v;
    }
    private static void drawPopup(GuiGraphicsExtractor g,double dt,double time) {
        if(popupFor!=null && (!editMode || !popupFor.present)) popupFor=null;
        if(popupFor!=null) popupShown=popupFor;
        popup=approach(popup,popupFor!=null ? 1 : 0,dt,popupFor!=null ? 16 : 22);
        if(popup<=.004f || popupShown==null) { popupShown=null; popupSlider=null; return; }
        Widget w=popupShown;
        List<Object> items=popupItems(w);
        float[] r=popupRect(w);
        float x=r[0], y=r[1], saved=alpha, open=Math.clamp(popup*1.4f,0,1);
        alpha=open;
        g.pose().pushMatrix();
        try {
            // Grows out of the widget's side with a slight rise and an overshoot.
            float s=.9f+.1f*outBack(popup), ox=x<w.x ? x+r[2] : x;
            g.pose().translate(ox,y+(1-outCubic(popup))*4);
            g.pose().scale(s,s);
            g.pose().translate(-ox,-y);
            float hw=popupHeaderWidth(w), by=y+POP_HEAD+HEAD_GAP, bh=r[3]-POP_HEAD-HEAD_GAP;
            Theme.shadow(g,x,by,r[2],bh,7,.55f*alpha,1.4f);
            // Header: icon, widget name and a close cross.
            Theme.surface(g,x,y,hw,POP_HEAD,POP_HEAD/2,alpha);
            glow(g,x+5+4.5f,y+POP_HEAD/2,9,Theme.accent());
            Icons.draw(g,popupIcon(w),x+5,y+POP_HEAD/2-4.5f,9,2.2f,c(Theme.accent()));
            FONT.draw(g,w.label,x+5+9+4,mid(y,POP_HEAD,7.5f),7.5f,c(Theme.text()));
            float closeHover=popupAnim("close",popupFor!=null && inside(mouseX,mouseY,new float[]{x+hw-14,y,14,POP_HEAD})?1:0,dt,18);
            Icons.draw(g,Icon.CLOSE,x+hw-5-5,y+POP_HEAD/2-2.5f,5,3,c(mixColor(Theme.hint(),LOW_COLOR,closeHover)));
            Theme.surface(g,x,by,r[2],bh,7,alpha);
            float ry=by+BODY_PAD;
            for(int i=0;i<items.size();i++) {
                Object item=items.get(i);
                float ih=itemHeight(item);
                // Rows rise in one after another when the popup opens.
                float rowT=Math.clamp(popup*1.7f-i*.06f,0,1), rowY=ry+(1-outCubic(rowT))*3;
                alpha=open*outCubic(rowT);
                if(item==SEP) UiRenderer.rect(g,x+POP_PAD,rowY+ih/2-.3f,r[2]-POP_PAD*2,.6f,c(Theme.withAlpha(Theme.text(),.08f)));
                else if(item instanceof String caption) FONT.draw(g,caption,x+POP_PAD,mid(rowY,ih,6),6,c(Theme.hint()));
                else drawPopupRow(g,(Setting)item,x,rowY,r,dt);
                ry+=ih;
            }
        } finally { g.pose().popMatrix(); alpha=saved; }
    }
    private static void drawPopupRow(GuiGraphicsExtractor g,Setting setting,float x,float y,float[] r,double dt) {
        boolean over=popupFor!=null && inside(mouseX,mouseY,new float[]{x,y,r[2],POP_ROW});
        float hover=popupAnim(setting,over?1:0,dt,18);
        if(hover>.01f) UiRenderer.rounded(g,x+2,y,r[2]-4,POP_ROW,5,c(Theme.withAlpha(Theme.text(),.05f*hover)));
        int label=mixColor(Theme.secondary(),Theme.text(),hover);
        float right=x+r[2]-POP_PAD, ty=mid(y,POP_ROW,POP_TEXT);
        // Short labels for the long ClickGUI names, so rows fit the compact popup.
        String name=switch (setting.name().strip()) {
            case "Количество частиц" -> "Кол-во";
            case "Партиклы цели" -> "Партиклы";
            case "Тип частиц" -> "Режим";
            case "Порог, с" -> "Порог";
            default -> setting.name().strip();
        };
        switch (setting) {
            case BooleanSetting b -> {
                FONT.draw(g,name,x+POP_PAD,ty,POP_TEXT,c(label));
                toggle(g,right-14,y+POP_ROW/2-4,popupAnim(List.of(b,"on"),b.get()?1:0,dt,16));
            }
            case SliderSetting sl -> {
                FONT.draw(g,fit(name,POP_LABEL-POP_PAD-3,POP_TEXT),x+POP_PAD,ty,POP_TEXT,c(label));
                float f=popupAnim(List.of(sl,"v"),sl.fraction(),dt,popupSlider==sl ? 40 : 14);
                float sx=sliderX(r), sw=sliderW(r), cy=y+POP_ROW/2;
                UiRenderer.rounded(g,sx,cy-1,sw,2,1,c(Theme.withAlpha(Theme.text(),.13f)));
                UiRenderer.rounded(g,sx,cy-1,Math.max(2,sw*f),2,1,Gradient.linear(c(Theme.accent()),c(Theme.accentLight()),Gradient.Axis.HORIZONTAL,sx,sw));
                float knob=5.5f+1.5f*Math.max(hover,popupSlider==sl?1:0);
                glow(g,sx+sw*f,cy,knob,Theme.accent());
                UiRenderer.rounded(g,sx+sw*f-knob/2,cy-knob/2,knob,knob,knob/2,c(0xffffffff));
                String value=sl.format(sl.get());
                FONT.draw(g,value,right-FONT.width(value,POP_TEXT),ty,POP_TEXT,c(Theme.accentLight()));
            }
            case ModeSettings m -> {
                FONT.draw(g,name,x+POP_PAD,ty,POP_TEXT,c(label));
                String value=m.get();
                float vw=FONT.width(value,POP_TEXT), vx=right-4-2-vw;
                Icons.draw(g,Icon.CHEVRON_RIGHT,right-4,y+POP_ROW/2-2,4,3,c(Theme.hint()));
                FONT.draw(g,value,vx,ty,POP_TEXT,c(Theme.accentLight()));
                Icons.draw(g,Icon.CHEVRON_LEFT,vx-2-4,y+POP_ROW/2-2,4,3,c(Theme.hint()));
            }
            case ActionSetting act -> {
                Icons.draw(g,Icon.MOVE,x+POP_PAD,y+POP_ROW/2-3.5f,7,2.2f,c(mixColor(Theme.hint(),Theme.accent(),hover)));
                FONT.draw(g,name,x+POP_PAD+7+4,ty,POP_TEXT,c(mixColor(Theme.hint(),Theme.text(),hover)));
            }
            default -> FONT.draw(g,name,x+POP_PAD,ty,POP_TEXT,c(label));
        }
    }
    /** Small switch, 14 x 8; t is the animated on amount. */
    private static void toggle(GuiGraphicsExtractor g,float x,float y,float t) {
        if(t>.01f) glow(g,x+7,y+4,9*t,Theme.accent());
        UiRenderer.rounded(g,x,y,14,8,4,c(mixColor(Theme.withAlpha(Theme.text(),.16f),Theme.accent(),t)));
        float k=5, kx=x+1.5f+6*outBack(t);
        UiRenderer.rounded(g,kx,y+1.5f,k,k,k/2,c(0xffffffff));
    }
    private static int mixColor(int a,int b,float t) {
        t=Math.clamp(t,0,1);
        int r=0;
        for(int s=0;s<32;s+=8) {
            int ca=(a>>>s)&255, cb=(b>>>s)&255;
            r|=Math.round(ca+(cb-ca)*t)<<s;
        }
        return r;
    }
    /** Handles a click inside the open popup. */
    private static void popupPress(int button,float x,float y) {
        Widget w=popupFor;
        float[] r=popupRect(w);
        if(y<r[1]+POP_HEAD) {
            float hw=popupHeaderWidth(w);
            if(x>=r[0]+hw-14 && x<=r[0]+hw) { popupFor=null; UiSounds.expand(false); }
            return;
        }
        float ry=r[1]+POP_HEAD+HEAD_GAP+BODY_PAD;
        for(Object item:popupItems(w)) {
            float ih=itemHeight(item);
            if(y>=ry && y<ry+ih && item instanceof Setting setting) { pressSetting(setting,button,x); return; }
            ry+=ih;
        }
    }
    private static void pressSetting(Setting setting,int button,float x) {
        boolean left=button==InputConstants.MOUSE_BUTTON_LEFT;
        switch (setting) {
            case BooleanSetting b -> { b.toggle(); UiSounds.toggle(b.get()); }
            case SliderSetting s -> { if(left) { popupSlider=s; dragSlider(s,x); } }
            case ModeSettings m -> {
                int n=m.options.size(), i=m.options.indexOf(m.get());
                m.select(Math.floorMod(i+(left ? 1 : -1),n));
                UiSounds.select();
            }
            case ActionSetting a -> { if(left) { a.run(); UiSounds.select(); } }
            default -> {}
        }
    }
    // ---- edit mode ------------------------------------------------------------------------------
    private static void drawGrid(GuiGraphicsExtractor g) {
        int line=Theme.withAlpha(0xffffff,.045f*edit);
        for(float x=0;x<screenW;x+=16) UiRenderer.rect(g,x,0,.5f,screenH,line);
        for(float y=0;y<screenH;y+=16) UiRenderer.rect(g,0,y,screenW,.5f,line);
        int center=Theme.withAlpha(Theme.accent(),.18f*edit);
        UiRenderer.rect(g,screenW/2-.4f,0,.8f,screenH,center);
        UiRenderer.rect(g,0,screenH/2-.4f,screenW,.8f,center);
    }
    private static void drawEditFrame(GuiGraphicsExtractor g,Widget w) {
        float a=edit*Math.clamp(w.vis,0,1), pad=3, x=w.drawX-pad, y=w.drawY-pad, bw=w.w+pad*2, bh=w.h+pad*2;
        float strength=.35f+.45f*Math.max(w.hover,w.lift);
        UiRenderer.rounded(g,x,y,bw,bh,8,Theme.withAlpha(Theme.accent(),(.05f+.07f*Math.max(w.hover,w.lift))*a));
        int edge=Theme.withAlpha(Theme.accent(),strength*a);
        float r=7;
        UiRenderer.rect(g,x+r,y,bw-2*r,.8f,edge); UiRenderer.rect(g,x+r,y+bh-.8f,bw-2*r,.8f,edge);
        UiRenderer.rect(g,x,y+r,.8f,bh-2*r,edge); UiRenderer.rect(g,x+bw-.8f,y+r,.8f,bh-2*r,edge);
        // Name tag only on the hovered / held widget, so tags never cover neighbours.
        float tag=a*Math.max(w.hover,w.lift);
        if(tag<.01f) return;
        float tagW=FONT.width(w.label,7)+8, tagY=y>12 ? y-11 : y+bh+2;
        UiRenderer.rounded(g,x,tagY,tagW,10,4,Theme.withAlpha(Theme.accent(),.9f*tag));
        FONT.draw(g,w.label,x+4,tagY+1.2f,7,Theme.withAlpha(Theme.onAccent(),tag));
    }
    private static void drawGuides(GuiGraphicsExtractor g,double dt) {
        guideAlpha=approach(guideAlpha,dragging!=null && (guideX>=0 || guideY>=0)?1:0,dt,20);
        if(guideAlpha<.01f) return;
        int color=Theme.withAlpha(Theme.accentLight(),.8f*guideAlpha*edit);
        if(guideX>=0) UiRenderer.rect(g,guideX-.4f,0,.8f,screenH,color);
        if(guideY>=0) UiRenderer.rect(g,0,guideY-.4f,screenW,.8f,color);
    }
    private static float[] resetRect() {
        float w=FONT.width("Сбросить позиции",8)+30, h=18;
        // Below the player when it sits at the top centre, so the two never overlap.
        boolean below=PLAYER.present && PLAYER.y<40 && Math.abs(PLAYER.x+PLAYER.w/2-screenW/2)<w;
        return new float[]{screenW/2-w/2,below ? PLAYER.y+PLAYER.h+6 : 8,w,h};
    }
    private static void drawResetButton(GuiGraphicsExtractor g,double dt) {
        float[] r=resetRect();
        resetHover=approach(resetHover,dragging==null && inside(mouseX,mouseY,r) ? 1 : 0,dt,18);
        float a=outCubic(edit), oldAlpha=alpha;
        alpha=a;
        g.pose().pushMatrix();
        try {
            g.pose().translate(0,(1-a)*-12);
            Theme.surface(g,r[0],r[1],r[2],r[3],9,a);
            if(resetHover>.01f) UiRenderer.rounded(g,r[0],r[1],r[2],r[3],9,Theme.withAlpha(Theme.accent(),.18f*resetHover*a));
            Icons.draw(g,Icon.MOVE,r[0]+7,r[1]+4.5f,9,2.2f,c(Theme.accent()));
            FONT.draw(g,"Сбросить позиции",r[0]+21,mid(r[1],r[3],8),8,c(Theme.text()));
            String hint="ЛКМ — перетащить  ·  ПКМ — вернуть элемент";
            FONT.centered(g,hint,screenW/2,r[1]+r[3]+5,7,c(0x99ffffff));
        } finally { g.pose().popMatrix(); alpha=oldAlpha; }
    }
    private static boolean inside(float x,float y,float[] r) { return x>=r[0] && x<=r[0]+r[2] && y>=r[1] && y<=r[1]+r[3]; }

    /** Follows the cursor and snaps the dragged widget to margins, the centre and other widgets. */
    private static void updateDrag() {
        guideX=-1; guideY=-1;
        if(dragging==null) return;
        Widget w=dragging;
        float nx=Math.clamp(mouseX-grabX,0,Math.max(0,screenW-w.w)), ny=Math.clamp(mouseY-grabY,0,Math.max(0,screenH-w.h));
        List<Float> xs=new ArrayList<>(List.of(MARGIN,screenW/2,screenW-MARGIN)), ys=new ArrayList<>(List.of(MARGIN,screenH/2,screenH-MARGIN));
        for(Widget o:WIDGETS) if(o!=w && o.present) {
            xs.add(o.x); xs.add(o.x+o.w/2); xs.add(o.x+o.w); xs.add(o.x-GAP); xs.add(o.x+o.w+GAP);
            ys.add(o.y); ys.add(o.y+o.h/2); ys.add(o.y+o.h); ys.add(o.y-GAP); ys.add(o.y+o.h+GAP);
        }
        float[] snapX=snap(new float[]{nx,nx+w.w/2,nx+w.w},xs), snapY=snap(new float[]{ny,ny+w.h/2,ny+w.h},ys);
        if(snapX!=null) { nx+=snapX[0]; guideX=snapX[1]; }
        if(snapY!=null) { ny+=snapY[0]; guideY=snapY[1]; }
        w.moveTo(nx,ny);
        w.resolve();
    }
    /** Smallest correction (and its guide line) that aligns one of the edges with a candidate line. */
    private static float[] snap(float[] edges,List<Float> lines) {
        float best=SNAP+1, line=-1;
        for(float e:edges) for(float l:lines) if(Math.abs(l-e)<Math.abs(best)) { best=l-e; line=l; }
        return Math.abs(best)<=SNAP ? new float[]{best,line} : null;
    }
    /** @return true when the click was used by HUD editing. */
    private static boolean press(int button,float x,float y) {
        if(!editMode) return false;
        if(popupFor!=null) {
            float[] r=popupRect(popupFor);
            boolean onHeader=x>=r[0] && x<=r[0]+popupHeaderWidth(popupFor) && y>=r[1] && y<=r[1]+POP_HEAD;
            boolean onBody=x>=r[0] && x<=r[0]+r[2] && y>=r[1]+POP_HEAD+HEAD_GAP && y<=r[1]+r[3];
            if(onHeader || onBody) { popupPress(button,x,y); return true; }
            // A click anywhere else closes the popup and still does its normal job.
            popupFor=null;
        }
        if(button==InputConstants.MOUSE_BUTTON_LEFT && inside(x,y,resetRect())) {
            WIDGETS.forEach(Widget::reset);
            ess.ll.client.render.UiSounds.select();
            return true;
        }
        for(int i=WIDGETS.size()-1;i>=0;i--) {
            Widget w=WIDGETS.get(i);
            if(!w.present || !w.contains(x,y)) continue;
            if(button==InputConstants.MOUSE_BUTTON_LEFT && w==PLAYER && HudPlayer.press(x-w.drawX,y-w.drawY,bindsUnit())) return true;
            if(button==InputConstants.MOUSE_BUTTON_LEFT) {
                dragging=w; grabX=x-w.x; grabY=y-w.y; UiSounds.expand(true);
            }
            else if(button==InputConstants.MOUSE_BUTTON_RIGHT) {
                if(popupItems(w)!=null) { popupFor=w; UiSounds.expand(true); }
                else { w.reset(); UiSounds.select(); }
            }
            return true;
        }
        return false;
    }
    private static boolean release() {
        if(popupSlider!=null) { popupSlider=null; return true; }
        if(dragging==null) return false;
        dragging=null;
        UiSounds.expand(false);
        return true;
    }

    public static void finishEditing() {
        release();
        editMode=false;
        popupFor=null;
    }

    /** Dev helper: GUI coordinates of a widget's centre. */
    public static float[] center(String id) {
        for(Widget w:WIDGETS) if(w.id.equals(id)) return new float[]{(w.x+w.w/2)*scale,(w.y+w.h/2)*scale};
        return null;
    }
    /** Development check: GUI point on the n-th setting row of the widget's popup (opens the popup). */
    public static float[] popupPoint(String id,int setting) {
        for(Widget w:WIDGETS) if(w.id.equals(id)) {
            popupFor=w;
            float[] r=popupRect(w);
            float y=r[1]+POP_HEAD+HEAD_GAP+BODY_PAD;
            int n=0;
            for(Object item:popupItems(w)) {
                if(item instanceof Setting && n++==setting) return new float[]{(r[0]+r[2]-10)*scale,(y+POP_ROW/2)*scale};
                y+=itemHeight(item);
            }
        }
        return null;
    }
    /** Saved as {anchorX, anchorY, offsetX, offsetY} per widget id. */
    public static Map<String,float[]> positions() {
        Map<String,float[]> out=new TreeMap<>();
        for(Widget w:WIDGETS) out.put(w.id,new float[]{w.ax.ordinal(),w.ay.ordinal(),w.ox,w.oy});
        return out;
    }
    public static void place(String id,float[] data) {
        for(Widget w:WIDGETS) if(w.id.equals(id)) {
            w.ax=Anchor.values()[Math.clamp((int)data[0],0,2)]; w.ay=Anchor.values()[Math.clamp((int)data[1],0,2)];
            w.ox=data[2]; w.oy=data[3]; w.placed=false;
        }
    }

    // ---- helpers ------------------------------------------------------------------------------
    static float approach(float v,float target,double dt,double speed) {
        float r=v+(target-v)*(float)(1-Math.exp(-dt*speed));
        return Math.abs(r-target)<.005f ? target : r;
    }
    private static float outCubic(float t) { t=Math.clamp(t,0,1); float u=1-t; return 1-u*u*u; }
    private static float outBack(float t) {
        t=Math.clamp(t,0,1);
        float c1=1.70158f, c3=c1+1, u=t-1;
        return 1+c3*u*u*u+c1*u*u;
    }
    private static String fit(String s,float width,float size) {
        if(FONT.width(s,size)<=width) return s;
        while(!s.isEmpty() && FONT.width(s+"..",size)>width) s=s.substring(0,s.length()-1);
        return s+"..";
    }
    /** Top coordinate that vertically centers cap-height text of the given size in a box. */
    static float mid(float y,float h,float size) { return y+h/2-.73f*size; }
}
