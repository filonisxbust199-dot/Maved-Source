package ess.ll.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.command.ClientData;
import ess.ll.client.command.Keys;
import ess.ll.client.module.ClientModules;
import ess.ll.client.module.ClientModules.Module;
import ess.ll.client.render.*;
import ess.ll.client.wardrobe.Wardrobe;
import ess.ll.client.wardrobe.WardrobeIcons;
import ess.ll.client.bots.Bot;
import ess.ll.client.bots.Bots;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Window-style ClickGUI: category tiles on the left, modules as cards in two columns with their settings
 * always visible, and a "Темы" tab with presets and every theme colour. All colours come from {@link Theme};
 * nearly every state change is eased through {@link #anim}. The header drags the window.
 */
public final class ClickGuiScreen extends Screen {
    private static final String THEMES="Темы",COSMETICS="Cosmetics",BOTS="Bots",SEARCH="search";
    private static final String[] ORDER={"Combat","Movement","Render","Player","Misc",COSMETICS};
    private static final float H=320,RAIL_NARROW=46,RAIL_WIDE=124,HEADER=34,PAD=12,COL_GAP=8,CARD_GAP=7,TAB=32,TABS_TOP=46;
    /** Floating settings panel, then the sidebar panel, then the module columns, each apart from the next. */
    private static final float SET_W=134,SET_H=112,SIDE_W=128,SIDE_GAP=8,SIDE_X=SET_W+SIDE_GAP,CONTENT_X=SIDE_X+SIDE_W+SIDE_GAP;
    private static final float COL_W=196,FULL_W=COL_W*2+COL_GAP,CP=10,INNER=COL_W-CP*2,VIEW_TOP=0,VIEW_BOTTOM=H;
    /** Card header (name and description), a setting's name and description, a slider track, the colour row. */
    private static final float CARD_HEAD=31,LABEL2=19,TRACK_H=9,COLOR_ROW_H=15,ITEM_H=19,ACTIVE_H=42,PICK_W=136;
    private static final float HEAD_H=16,GAP=6,ROW_H=12,SLIDER_H=17,BUTTON_H=16,SEG_H=13,SEG_PAD=2,CHIP_H=13,BAR_H=10,LABEL_H=11;
    private static final float TEXT=8,SMALL=7.5f;
    private static final float PROFILE_W=78,PROFILE_H=15;
    private static final float SIDE_ROW=22,SIDE_TOP=56;
    /** Sidebar-style controls: pill-thumb slider, drop-down multi select, and the settings preview of a card. */
    private static final float PILL_SLIDER_H=22,DROP_H=17,DROP_ROW=13,PREVIEW_H=14;
    /** Rail width eases between the icon rail ("Окно") and the labelled sidebar ("Сайдбар"); the window follows. */
    private float RAIL=CONTENT_X-PAD,W=RAIL+FULL_W+PAD*2,PROFILE_X=W-PAD-PROFILE_W,PROFILE_Y;
    /** Sidebar style: module cards start collapsed and open their settings on right click. */
    private boolean sidebarLayout;
    /** Set while the fading-out half of the rail draws, so it takes no clicks. */
    private boolean inert;
    private static final float SEARCH_W=96,SEARCH_GROW=60,POP_W=122,PRESET_W=70,PRESET_H=56,PRESET_GAP=6,GROUP_W=104;
    private static final double OPEN_SECONDS=.26,CARD_SECONDS=.3,STAGGER=.035;
    /** Animation slots per key. */
    private static final int HOVER=0,VALUE=1,OPEN=2,SHOWN=3,PRESS=4,X=5,Y=6,WIDTH=7;
    private static float offsetX,offsetY;
    /** The settings panel, shown by the button next to the name; remembered while the game runs. */
    private static boolean settingsOpen;
    /** Cosmetics → Оружие → Мечи: 0 the usual sword skins, 1 the CS knives. */
    private static int swordStyle;
    private static String lastCategory,profile="—";

    private final String[] categories;
    private final Map<String,List<List<Module>>> columns=new HashMap<>();
    private final Map<Module,Integer> order=new HashMap<>();
    /** Per category: scroll, scroll target, max scroll. */
    private final Map<String,float[]> scrolls=new HashMap<>();
    private final Map<Object,Anim> anims=new HashMap<>();
    private final Set<Object> expanded=new HashSet<>();
    private final List<Hit> hits=new ArrayList<>();
    private final List<Setting> colorSettings=List.of(ClientModules.THEME_ACCENT,ClientModules.THEME_TEXT,ClientModules.THEME_SECONDARY,
            ClientModules.THEME_HINT,ClientModules.THEME_BACKGROUND,ClientModules.THEME_CARD,ClientModules.THEME_FIELD,ClientModules.THEME_BORDER);
    private final List<Setting> formSettings=List.of(ClientModules.THEME_RADIUS,ClientModules.THEME_OPACITY,ClientModules.THEME_SPEED,
            ClientModules.BACKGROUND_STYLE,ClientModules.GLASS_OPACITY,ClientModules.BLUR_POWER,ClientModules.GLOW_POWER);
    private final ActionSetting saveTheme,resetTheme;
    private final long openedAt=System.nanoTime();
    private String category;
    private long switchedAt=System.nanoTime(),lastFrame=System.nanoTime(),frame,closingAt,savedAt;
    private boolean closing,opened,movingWindow,profilesOpen,overlayPass,lightGroup;
    private double dt,closeFrom;
    private float alpha=1,mx,my,left,top,scale=1,size=1,grabX,grabY;
    private float clipL=-1e9f,clipT=-1e9f,clipR=1e9f,clipB=1e9f;
    /** Bind popup: its target, the target still fading out, and the pill it hangs from. */
    private Object popupTarget,popupShown;
    private float popupRight,popupTop,popupBottom,popX,popY,popH;
    /** Module or KeybindSetting waiting for a key. */
    private Object listening;
    private Object hovered,frameHover;
    private Drag dragging;
    private List<String> profiles=List.of();
    private float railScroll,railTarget,railMax,railX,railY,railW,searchX,searchW,searchY;
    /** Colour picker: the setting it edits (and the one fading out), where it hangs from, and the hue it keeps. */
    private ColorSetting picker,pickerShown;
    private float pickAnchorX,pickAnchorY,pickX,pickY,pickH,pickHue;
    private boolean hexFocused;
    private String hexText="";
    private String query="";
    private boolean searchFocused;
    private long caretAt;
    private List<List<Module>> results=List.of(List.of(),List.of());
    private final Map<Module,Long> shownAt=new HashMap<>();
    /** Last drawn card heights, used to lay out search results. */
    private final Map<Module,Float> cardHeights=new HashMap<>();

    /** Horizontal drag (set) or, for the colour square, both axes (set2). */
    private record Drag(Object key,float x,float w,Consumer<Float> set,float y,float h,java.util.function.BiConsumer<Float,Float> set2) {
        Drag(Object key,float x,float w,Consumer<Float> set) { this(key,x,w,set,0,1,null); }
        void apply(float px,float py) {
            float fx=Math.clamp((px-x)/w,0,1);
            if(set2!=null) set2.accept(fx,Math.clamp((py-y)/h,0,1)); else set.accept(fx);
        }
    }
    private record Hit(float x,float y,float w,float h,Runnable left,Runnable right,Drag drag,Runnable middle) {}
    private static final class Anim {
        final float[] v=new float[8];
        final long[] frame=new long[8];
        Anim() { Arrays.fill(v,Float.NaN); }
    }

    public ClickGuiScreen() {
        super(Component.literal("Maved GUI"));
        Theme.reloadSaved();
        List<String> list=new ArrayList<>();
        for(String c:ORDER) if(ClientModules.all().stream().anyMatch(m->m.category.equals(c))) list.add(c);
        // Bots has no module cards; it sits with the module categories, before the cosmetics.
        int cosmetics=list.indexOf(COSMETICS);
        list.add(cosmetics>=0 ? cosmetics : list.size(),BOTS);
        list.add(THEMES);
        categories=list.toArray(String[]::new);
        for(String c:categories) scrolls.put(c,new float[3]);
        sidebarLayout=true;
        split();
        scrolls.put(SEARCH,new float[3]);
        category=lastCategory!=null && scrolls.containsKey(lastCategory) ? lastCategory : categories[0];
        lightGroup=Theme.light();
        saveTheme=new ActionSetting("Сохранить тему",()->{
            try { Theme.saveCurrent(); savedAt=System.nanoTime(); UiSounds.bindSet(); }
            catch(Exception e) { System.err.println("[ESS] Theme save failed: "+e.getMessage()); }
        });
        resetTheme=new ActionSetting("Сбросить",()->{ Theme.reset(); lightGroup=false; railTarget=0; });
    }
    /**
     * Greedy split into two columns by card height, fixed while the style stays the same so cards never jump
     * between columns. Collapsed sidebar cards are all one height, so they simply alternate.
     */
    private void split() {
        for(String c:categories) {
            if(c.equals(THEMES) || c.equals(COSMETICS) || c.equals(BOTS)) continue;
            List<List<Module>> cols=List.of(new ArrayList<>(),new ArrayList<>());
            float[] heights=new float[2];
            int index=0;
            for(var m:ClientModules.all()) if(m.category.equals(c)) {
                int col=heights[0]<=heights[1] ? 0 : 1;
                float rows=0;
                if(!sidebarLayout) for(var s:m.settings) if(s.visible()) rows+=rowHeight(s,0);
                float folded=0;
                cols.get(col).add(m); heights[col]+=cardHeight(rows)+folded+CARD_GAP; order.put(m,index++);
            }
            columns.put(c,cols);
        }
    }
    private static boolean sidebar() { return ClientModules.GUI_STYLE.is("Сайдбар"); }
    /** 0 for the icon rail, 1 for the full sidebar. */
    private float sideT() { return Math.clamp((RAIL-RAIL_NARROW)/(RAIL_WIDE-RAIL_NARROW),0,1); }
    private static float cardHeight(float rows) { return CARD_HEAD+(rows>0 ? 8+rows-GAP+CP : 0); }

    // ---------------------------------------------------------------- layout

    private float rowHeight(Setting s,float open) {
        if(s instanceof SliderSetting) return LABEL2+TRACK_H+GAP;
        if(s instanceof ActionSetting) return BUTTON_H+GAP;
        if(s instanceof ColorSetting) return LABEL2+COLOR_ROW_H+GAP;
        if(s instanceof ModeSettings mode) return LABEL2+DROP_H+(2+mode.options.size()*DROP_ROW)*open+GAP;
        if(s instanceof MultiBooleanSettings multi) return LABEL2+chipHeight(multi.options,INNER)+GAP;
        return LABEL2+GAP;
    }
    /** Segmented control cells {x, y, width}: rows of equal cells, as many per row as the widest label allows. */
    private static float[][] segLayout(List<String> options,float w) {
        int n=options.size();
        float widest=0;
        for(var o:options) widest=Math.max(widest,MsdfFont.UI.width(o,SMALL));
        // Two options always share a row; longer lists wrap when the widest label would not fit.
        int perRow=n<=2 ? n : Math.max(1,Math.min(n,(int)((w-2*SEG_PAD+2)/(widest+10))));
        int rows=(n+perRow-1)/perRow;
        perRow=(n+rows-1)/rows;
        float[][] cells=new float[n][];
        for(int i=0;i<n;i++) {
            int row=i/perRow,col=i%perRow,inRow=Math.min(perRow,n-row*perRow);
            float cw=(w-2*SEG_PAD-(inRow-1)*2)/inRow;
            cells[i]=new float[]{SEG_PAD+col*(cw+2),SEG_PAD+row*(SEG_H+2),cw};
        }
        return cells;
    }
    private static float segHeight(List<String> options,float w) {
        float[][] cells=segLayout(options,w);
        return cells[cells.length-1][1]+SEG_H+SEG_PAD;
    }
    /** Chips flow left to right and wrap. */
    private static float[][] chipLayout(List<String> options,float w) {
        float[][] cells=new float[options.size()][];
        float x=0,y=0;
        for(int i=0;i<cells.length;i++) {
            float cw=MsdfFont.UI.width(options.get(i),SMALL)+11;
            if(x>0 && x+cw>w) { x=0; y+=CHIP_H+3; }
            cells[i]=new float[]{x,y,cw};
            x+=cw+3;
        }
        return cells;
    }
    private static float chipHeight(List<String> options,float w) {
        float[][] cells=chipLayout(options,w);
        return cells.length==0 ? 0 : cells[cells.length-1][1]+CHIP_H;
    }

    // ---------------------------------------------------------------- animation and colour helpers

    /** Eases a slot towards target (speed ~ 1/seconds, scaled by the theme); steps once per frame. */
    private float anim(Object key,int slot,float target,float speed) {
        Anim a=anims.computeIfAbsent(key,k->new Anim());
        if(Float.isNaN(a.v[slot])) a.v[slot]=target;
        else if(a.frame[slot]!=frame) {
            a.v[slot]+=(target-a.v[slot])*(float)(1-Math.exp(-dt*speed*Theme.speed()));
            if(Math.abs(a.v[slot]-target)<.002f*Math.max(1,Math.abs(target))) a.v[slot]=target;
        }
        a.frame[slot]=frame;
        return a.v[slot];
    }
    private float peek(Object key,int slot) {
        Anim a=anims.get(key);
        return a==null || Float.isNaN(a.v[slot]) ? 0 : a.v[slot];
    }
    private void kick(Object key,int slot,float value) { anims.computeIfAbsent(key,k->new Anim()).v[slot]=value; }
    /** Applies the current fade to a colour. */
    private int c(int argb) { return Theme.withAlpha(argb,((argb>>>24)/255f)*alpha); }
    /** A card or field fill: follows Непрозрачность like the window, slightly darker so they stay apart. */
    private int p(int argb) { return c(Theme.plate(argb)); }
    /** Plate with a thin edge drawn as an outline, so a see-through fill shows no lighter plate under it. */
    private void framed(GuiGraphicsExtractor g,float x,float y,float w,float h,float r,int edge,int fill) {
        framed(g,x,y,w,h,r,.75f,edge,fill);
    }
    private void framed(GuiGraphicsExtractor g,float x,float y,float w,float h,float r,float width,int edge,int fill) {
        UiRenderer.rounded(g,x,y,w,h,r,p(fill));
        UiRenderer.outline(g,x,y,w,h,r,width,c(edge));
    }
    private static int mix(int a,int b,float t) { return Theme.mix(a,b,t); }
    private static float ease(double t) { t=Math.clamp(t,0,1); return (float)(1-Math.pow(1-t,3)); }
    private static float fieldRadius() { return Math.max(3,Theme.radius()-4); }
    private int shade(float strength) { return Theme.withAlpha(0x000000,(Theme.light() ? .12f : .35f)*strength); }

    private boolean over(float x,float y,float w,float h) {
        return !closing && dragging==null && !movingWindow && mx>=x && mx<x+w && my>=y && my<y+h
                && mx>=clipL && mx<clipR && my>=clipT && my<clipB && (overlayPass || !inOverlay(mx,my));
    }
    /** Click targets are clipped to the current viewport; later hits sit on top. */
    private void hit(float x,float y,float w,float h,Runnable left,Runnable right,Drag drag) { hit(x,y,w,h,left,right,drag,null); }
    private void hit(float x,float y,float w,float h,Runnable left,Runnable right,Drag drag,Runnable middle) {
        float l=Math.max(x,clipL),t=Math.max(y,clipT),r=Math.min(x+w,clipR),b=Math.min(y+h,clipB);
        if(!closing && !inert && r>l && b>t) hits.add(new Hit(l,t,r-l,b-t,left,right,drag,middle));
    }
    private float[] pushClip(GuiGraphicsExtractor g,float l,float t,float r,float b) {
        float[] saved={clipL,clipT,clipR,clipB};
        g.enableScissor((int)Math.floor(l),(int)Math.floor(t),(int)Math.ceil(r),(int)Math.ceil(b));
        clipL=Math.max(clipL,l); clipT=Math.max(clipT,t); clipR=Math.min(clipR,r); clipB=Math.min(clipB,b);
        return saved;
    }
    private void popClip(GuiGraphicsExtractor g,float[] saved) {
        g.disableScissor();
        clipL=saved[0]; clipT=saved[1]; clipR=saved[2]; clipB=saved[3];
    }

    // ---------------------------------------------------------------- screen lifecycle

    private float fit() {
        float fit=Math.min((width-24)/W,(height-24)/H);
        return Math.max(.1f,Math.min(fit,Math.min(1f,fit)*size));
    }
    private double openTime(long now) {
        double length=OPEN_SECONDS/Theme.speed();
        return closing ? Math.max(0,closeFrom-(now-closingAt)/1e9)/length : Math.min(length,(now-openedAt)/1e9)/length;
    }
    @Override protected void init() {
        super.init();
        if(!opened) { opened=true; UiSounds.open(); }
        // SDL delivers characters (charTyped) only while some owner has text input started.
        minecraft.textInputManager().startTextInput(this);
    }
    @Override public void removed() {
        minecraft.textInputManager().stopTextInput(this);
        super.removed();
    }
    @Override public void onClose() {
        if(closing) return;
        UiSounds.close();
        long now=System.nanoTime();
        closeFrom=openTime(now)*OPEN_SECONDS/Theme.speed(); closingAt=now; closing=true;
        dragging=null; listening=null; movingWindow=false; profilesOpen=false; popupTarget=null; picker=null; hexFocused=false;
    }
    @Override public void tick() {
        super.tick();
        if(closing && openTime(System.nanoTime())<=0 && minecraft.gui.screen()==this) super.onClose();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        long now=System.nanoTime(); dt=Math.min(.1,(now-lastFrame)/1e9); lastFrame=now; frame++;
        // Size changes apply on release so the slider does not move under the cursor.
        if(dragging==null) size=ClientModules.GUI_SIZE.get();
        float open=ease(openTime(now));
        alpha=open;
        UiRenderer.rect(g,0,0,width,height,Theme.withAlpha(0x04060b,dim()*open));
        float s=fit()*(.94f+.06f*open);
        float limitX=Math.max(0,(width-W*s)/2),limitY=Math.max(0,(height-H*s)/2);
        offsetX=Math.clamp(offsetX,-limitX,limitX); offsetY=Math.clamp(offsetY,-limitY,limitY);
        // Without the settings panel the sidebar and the cards are centred on their own.
        float panel=anim("settings",OPEN,settingsOpen?1:0,14);
        scale=s; left=width/2f+offsetX-W*s/2-(SET_W+SIDE_GAP)*s*(1-panel)/2; top=height/2f+offsetY-H*s/2+(1-open)*10;
        mx=(mouseX-left)/s; my=(mouseY-top)/s;
        hits.clear();
        g.pose().pushMatrix();
        try {
            g.pose().translate(left,top); g.pose().scale(s,s);
            drawWindow(g,now);
        } finally { g.pose().popMatrix(); }
    }
    private static float dim() { return Theme.light() ? .25f : .42f; }
    private void drawWindow(GuiGraphicsExtractor g,long now) {
        frameHover=null;
        // The GUI's own background: one panel under everything, widening to take in the settings panel.
        float from=SIDE_X-(SET_W+SIDE_GAP)*peek("settings",OPEN)-8,to=W-PAD+8,bgR=Theme.radius()+6;
        Theme.panel(g,from,-8,to-from,H+16,bgR,alpha);
        if(Theme.glass()) UiRenderer.rounded(g,from,-8,to-from,H+16,bgR,Theme.withAlpha(0x04060b,.35f*alpha));
        drawSettingsPanel(g);
        drawSidebar(g);
        drawContent(g,now);
        overlayPass=true;
        try { drawPopup(g); drawProfiles(g); drawPicker(g); }
        finally { overlayPass=false; }
        // Soft tick when the cursor enters a card, tab or theme tile (rate-limited in UiSounds).
        if(frameHover!=null && !frameHover.equals(hovered)) UiSounds.hover();
        hovered=frameHover;
    }
    /** Panel left of the sidebar: sounds, GUI size, the config picker and the glass switch. */
    private void drawSettingsPanel(GuiGraphicsExtractor g) {
        float panel=peek("settings",OPEN);
        if(panel<=.01f) return;
        float saved=alpha;
        alpha*=panel;
        boolean wasInert=inert;
        inert=panel<.5f;
        // Slides in from behind the sidebar.
        g.pose().pushMatrix();
        try {
            g.pose().translate((1-panel)*24,0);
            settingsPanel(g);
        } finally { g.pose().popMatrix(); alpha=saved; inert=wasInert; }
    }
    private void settingsPanel(GuiGraphicsExtractor g) {
        var font=MsdfFont.UI;
        float x=0,y=0,w=SET_W,right=x+w-10;
        Theme.inset(g,x,y,w,SET_H,Theme.radius(),alpha);
        font.mid(g,"Настройки",x+10,y+12,9,c(Theme.text()));
        Icons.draw(g,Icons.Icon.GRID,x+w-19,y+7.5f,9,2,c(Theme.hint()));
        font.mid(g,"Вид, размер и звуки",x+10,y+23,6.6f,c(Theme.hint()));
        font.mid(g,"интерфейса",x+10,y+31,6.6f,c(Theme.hint()));
        float ry=y+47;
        panelToggle(g,ClientModules.GUI_SOUNDS,x,ry,w);
        ry+=16;
        font.mid(g,"Размер",x+10,ry,7.8f,c(Theme.text()));
        miniSlider(g,ClientModules.GUI_SIZE,right-56,ry,56);
        ry+=16;
        font.mid(g,"Конфиги",x+10,ry,7.8f,c(Theme.text()));
        float cw=Math.min(70,font.width(profile,6.8f)+20),cx=right-cw;
        float hover=anim("profile",HOVER,over(cx,ry-6.5f,cw,13) || profilesOpen ? 1 : 0,14);
        anim("profile",OPEN,profilesOpen?1:0,16);
        UiRenderer.rounded(g,cx,ry-6.5f,cw,13,4,c(mix(Theme.field(),Theme.text(),.05f+.06f*hover)));
        font.mid(g,profile,cx+6,ry,6.8f,c(Theme.secondary()));
        Icons.draw(g,Icons.Icon.CHEVRON_RIGHT,right-10,ry-3,6,2.4f,c(Theme.hint()));
        PROFILE_X=right-PROFILE_W; PROFILE_Y=ry-6.5f;
        hit(cx,ry-6.5f,cw,13,this::toggleProfiles,null,null);
        ry+=16;
        boolean glass=ClientModules.BACKGROUND_STYLE.is("Блюр");
        panelToggle(g,ClientModules.BACKGROUND_STYLE,"Стекло",glass,()->{
            ClientModules.BACKGROUND_STYLE.select(glass ? "Обычный" : "Блюр");
            UiSounds.toggle(!glass);
        },x,ry,w);
    }
    private void panelToggle(GuiGraphicsExtractor g,BooleanSetting setting,float x,float mid,float w) {
        panelToggle(g,setting,setting.name(),setting.get(),()->{ setting.toggle(); UiSounds.toggle(setting.get()); },x,mid,w);
    }
    private void panelToggle(GuiGraphicsExtractor g,Setting key,String label,boolean on,Runnable action,float x,float mid,float w) {
        float right=x+w-10;
        MsdfFont.UI.mid(g,label,x+10,mid,7.8f,c(Theme.text()));
        toggle(g,key,right-20,mid-5.5f,20,11,on,over(x+6,mid-7,w-12,14));
        hit(x+6,mid-7,w-12,14,action,null,null);
    }
    /** Thin track with a white knob, for the settings panel. */
    private void miniSlider(GuiGraphicsExtractor g,SliderSetting s,float x,float mid,float w) {
        boolean active=dragging!=null && dragging.key()==s;
        float hover=anim(s,HOVER,active || over(x-3,mid-6,w+6,12) ? 1 : 0,14);
        float shown=anim(s,VALUE,s.get(),active ? 30 : 16),f=Math.clamp((shown-s.min)/(s.max-s.min),0,1);
        UiRenderer.rounded(g,x,mid-1.5f,w,3,1.5f,c(Theme.field()));
        UiRenderer.rounded(g,x,mid-1.5f,Math.max(3,w*f),3,1.5f,c(Theme.accent()));
        float k=7+1.5f*hover;
        UiRenderer.rounded(g,x+w*f-k/2,mid-k/2,k,k,k/2,c(0xffffffff));
        hit(x-3,mid-6,w+6,12,null,null,new Drag(s,x,w,v->{
            float before=s.get();
            s.set(s.min+v*(s.max-s.min));
            if(s.get()!=before) UiSounds.slider(s.fraction());
        }));
    }

    // ---------------------------------------------------------------- rail

    private float tabY(int i) { return TABS_TOP+i*TAB+(categories[i].equals(THEMES) ? 9 : 0); }
    /** Cross-fades the icon rail and the sidebar while the style changes; only the dominant one takes clicks. */
    private void drawTabs(GuiGraphicsExtractor g) {
        float t=sideT(),saved=alpha;
        try {
            if(t<.99f) { alpha=saved*(1-t); inert=t>=.5f; drawRail(g); }
            if(t>.01f) {
                alpha=saved*t; inert=t<.5f;
                float[] clip=pushClip(g,0,0,RAIL,H);
                try { drawSidebar(g); } finally { popClip(g,clip); }
            }
        } finally { alpha=saved; inert=false; }
    }
    private void drawRail(GuiGraphicsExtractor g) {
        float r=Math.max(4,Theme.radius()-2),tile=TAB-4;
        UiRenderer.rounded(g,RAIL/2-13,10,26,26,r,c(Theme.accent()));
        Icons.draw(g,Icons.Icon.MARK,RAIL/2-7,16,14,c(Theme.onAccent()));
        int index=Math.max(0,Arrays.asList(categories).indexOf(category));
        // The highlight tile glides between tabs.
        float y0=anim("tab",VALUE,tabY(index),16),shown=anim("tab",OPEN,searching()?0:1,14);
        UiRenderer.rounded(g,RAIL/2-tile/2,y0+2,tile,tile,r,c(Theme.withAlpha(Theme.accent(),.16f*shown)));
        for(int i=0;i<categories.length;i++) {
            String cat=categories[i];
            float y=tabY(i);
            if(cat.equals(THEMES)) UiRenderer.rect(g,RAIL/2-10,y-6,20,.6f,c(Theme.border()));
            boolean on=!inert && over(RAIL/2-tile/2,y+2,tile,tile);
            if(on) frameHover=cat;
            float hover=anim(cat,HOVER,on?1:0,14),selected=anim(cat,VALUE,cat.equals(category) && !searching()?1:0,14);
            if(hover*(1-selected)>.01f) UiRenderer.rounded(g,RAIL/2-tile/2,y+2,tile,tile,r,c(Theme.withAlpha(Theme.field(),hover*(1-selected))));
            float iconSize=13+1.5f*hover;
            Icons.draw(g,icon(cat),RAIL/2-iconSize/2,y+TAB/2-iconSize/2,iconSize,2.1f,c(mix(mix(Theme.hint(),Theme.secondary(),hover),Theme.accent(),selected)));
            hit(RAIL/2-tile/2,y+2,tile,tile,()->select(cat),null,null);
        }
    }
    private float sideY(int i) {
        int extra=0;
        for(int j=0;j<=i;j++) if(isExtra(categories[j])) { extra=1; break; }
        return SIDE_TOP+i*(SIDE_ROW+2)+extra*18;
    }
    private static boolean isExtra(String category) { return category.equals(THEMES) || category.equals(COSMETICS); }
    /**
     * Sidebar panel: the mark and name, the account, the categories under "Основное" (the selected one grows into an
     * accent block with its description), cosmetics and themes under "Прочее", and the search field at the bottom.
     */
    private void drawSidebar(GuiGraphicsExtractor g) {
        var font=MsdfFont.UI;
        float x0=SIDE_X,w=SIDE_W,r=Theme.radius();
        Theme.inset(g,x0,0,w,H,r,alpha);
        UiRenderer.rounded(g,x0+10,9,17,17,5,c(Theme.accent()));
        Icons.draw(g,Icons.Icon.MARK,x0+12.5f,11.5f,12,c(Theme.onAccent()));
        font.mid(g,"Maved.",x0+32,18,10.5f,c(Theme.text()));
        // Settings button: opens the panel on the left.
        float bx=x0+w-26,by=7.5f,panel=peek("settings",OPEN);
        float bh=anim("settings-button",HOVER,over(bx,by,20,20)?1:0,14);
        if(bh+panel>.01f) UiRenderer.rounded(g,bx,by,20,20,Math.max(4,r-3),c(Theme.withAlpha(mix(Theme.field(),Theme.accent(),.35f*panel),Math.max(bh,panel))));
        g.pose().pushMatrix();
        try {
            g.pose().translate(bx+10,by+10); g.pose().rotate((float)(Math.PI/2*panel));
            Icons.draw(g,Icons.Icon.GRID,-5,-5,10,2,c(mix(mix(Theme.hint(),Theme.text(),bh),Theme.accent(),panel)));
        } finally { g.pose().popMatrix(); }
        hit(bx,by,20,20,()->{ settingsOpen=!settingsOpen; if(!settingsOpen) profilesOpen=false; UiSounds.expand(settingsOpen); },null,null);
        float ay=34,aw=w-16;
        UiRenderer.rounded(g,x0+8,ay,aw,26,Math.max(4,r-2),c(Theme.withAlpha(Theme.field(),.75f)));
        String name=minecraft.getUser().getName();
        UiRenderer.rounded(g,x0+13,ay+5,16,16,4,c(Theme.withAlpha(Theme.accent(),.35f)));
        font.midCenter(g,name.isEmpty() ? "?" : name.substring(0,1).toUpperCase(Locale.ROOT),x0+21,ay+13,8,c(Theme.text()));
        font.mid(g,"Ваш аккаунт",x0+34,ay+9,6.3f,c(Theme.hint()));
        font.mid(g,name,x0+34,ay+18.5f,8,c(Theme.text()));
        Icons.draw(g,Icons.Icon.USER,x0+w-22,ay+8.5f,9,2,c(Theme.hint()));
        float y=ay+26+12;
        section(g,"Основное",x0+12,y);
        y+=8;
        boolean extras=false;
        for(String cat:categories) {
            if(isExtra(cat) && !extras) { extras=true; y+=6; section(g,"Прочее",x0+12,y); y+=8; }
            y+=sideItem(g,cat,x0+8,y,w-16)+2;
        }
        drawSearchField(g);
    }
    private void section(GuiGraphicsExtractor g,String label,float x,float mid) {
        Icons.draw(g,Icons.Icon.DOT,x,mid-2.5f,5,c(Theme.hint()));
        MsdfFont.UI.mid(g,label,x+8,mid,6.5f,c(Theme.hint()));
    }
    /** One category: a quiet row with its icon on the right, or, selected, an accent block with a description. */
    private float sideItem(GuiGraphicsExtractor g,String cat,float x,float y,float w) {
        var font=MsdfFont.UI;
        Object key=List.of(cat,"side");
        float sel=anim(key,VALUE,cat.equals(category) && !searching() ? 1 : 0,14),h=ITEM_H+(ACTIVE_H-ITEM_H)*sel;
        boolean on=over(x,y,w,h);
        if(on) frameHover=key;
        float hover=anim(key,HOVER,on?1:0,14),r=Math.max(4,Theme.radius()-2);
        if(sel>.01f) UiRenderer.rounded(g,x,y,w,h,r,c(Theme.withAlpha(Theme.accent(),sel)));
        if(hover*(1-sel)>.01f) UiRenderer.rounded(g,x,y,w,h,r,c(Theme.withAlpha(Theme.field(),hover*(1-sel))));
        int ink=Theme.onAccent();
        font.mid(g,cat,x+9+11*sel,y+ITEM_H/2+sel,8,c(mix(mix(Theme.secondary(),Theme.text(),hover),ink,sel)));
        Icons.draw(g,icon(cat),x+w-17,y+ITEM_H/2-5,10,2,c(Theme.withAlpha(mix(Theme.hint(),Theme.secondary(),hover),1-sel)));
        if(sel>.01f) {
            float a=alpha; alpha*=sel;
            Icons.draw(g,icon(cat),x+8,y+ITEM_H/2+sel-4,8,2.2f,c(ink));
            Icons.draw(g,Icons.Icon.CHEVRON_RIGHT,x+w-13,y+ITEM_H/2+sel-3.5f,7,2.4f,c(ink));
            String[] lines=describe(cat);
            font.mid(g,lines[0],x+9,y+23,6.4f,c(Theme.withAlpha(ink,.78f)));
            font.mid(g,lines[1],x+9,y+31,6.4f,c(Theme.withAlpha(ink,.78f)));
            alpha=a;
        }
        hit(x,y,w,h,()->select(cat),null,null);
        return h;
    }
    private static String[] describe(String category) {
        return switch (category) {
            case "Combat" -> new String[]{"Усиль свой бой","мощными функциями"};
            case "Movement" -> new String[]{"Скорость, прыжки","и передвижение"};
            case "Render" -> new String[]{"Визуалы мира","и интерфейса"};
            case "Player" -> new String[]{"Помощь игроку","и автоматизация"};
            case BOTS -> new String[]{"Боты на твоих","аккаунтах"};
            case COSMETICS -> new String[]{"Скины, одежда","и эффекты"};
            case THEMES -> new String[]{"Цвета, стекло","и форма GUI"};
            default -> new String[]{"Полезные мелочи","и утилиты"};
        };
    }
    /** Category names slide out of the rail while their tab is hovered. */
    private void drawTooltips(GuiGraphicsExtractor g) {
        float rail=1-sideT();
        if(rail<=.01f) return;
        for(int i=0;i<categories.length;i++) {
            float h=peek(categories[i],HOVER)*rail;
            if(h<=.01f) continue;
            float saved=alpha; alpha*=h;
            float w=MsdfFont.UI.width(categories[i],TEXT)+14,x=RAIL+2+4*h,y=tabY(i)+TAB/2-8;
            UiRenderer.shadow(g,x,y+1,w,16,8,6,c(shade(1)));
            UiRenderer.rounded(g,x,y,w,16,8,c(Theme.card()));
            MsdfFont.UI.mid(g,categories[i],x+7,y+8,TEXT,c(Theme.text()));
            alpha=saved;
        }
    }
    private void select(String cat) {
        boolean wasSearching=searching();
        if(wasSearching) { query=""; searchFocused=false; updateResults(); }
        if(cat.equals(category) && !wasSearching) return;
        category=cat; lastCategory=cat; switchedAt=System.nanoTime(); profilesOpen=false; popupTarget=null; listening=null;
        UiSounds.select();
    }
    private static Icons.Icon icon(String category) {
        return switch (category) {
            case "Combat" -> Icons.Icon.SWORD;
            case "Movement" -> Icons.Icon.MOVE;
            case "Render" -> Icons.Icon.EYE;
            case "Player" -> Icons.Icon.USER;
            case THEMES -> Icons.Icon.PALETTE;
            case COSMETICS -> Icons.Icon.STAR;
            case BOTS -> Icons.Icon.ROBOT;
            default -> Icons.Icon.GRID;
        };
    }

    // ---------------------------------------------------------------- header and profiles

    private void drawHeader(GuiGraphicsExtractor g,long now) {
        var font=MsdfFont.UI;
        float t=ease((now-switchedAt)/1e9*Theme.speed()/.25),x=RAIL+PAD,mid=HEADER/2+1+(1-t)*5;
        float saved=alpha; alpha*=t;
        float w=font.mid(g,searching() ? "Поиск" : category,x,mid,11.5f,c(Theme.text()));
        String info;
        if(searching()) {
            int found=results.get(0).size()+results.get(1).size();
            info=found==0 ? "ничего не найдено" : "найдено: "+found;
        } else if(category.equals(COSMETICS)) info="надето: "+Wardrobe.wornCount()+" · "+Wardrobe.items().size()+" вещей";
        else if(category.equals(BOTS)) {
            int on=0;
            for(var b:Bots.all()) if(b.state()==Bot.State.ONLINE) on++;
            info=on+" из "+Bots.all().size()+" в сети";
        }
        else if(category.equals(THEMES)) info=Theme.all().stream().filter(Theme::isCurrent).map(Theme.Preset::name).findFirst().map(n->"Тема: "+n).orElse("Своя тема");
        else {
            int on=0,total=0;
            for(var m:ClientModules.all()) if(m.category.equals(category)) { total++; if(m.enabled()) on++; }
            info=on+" из "+total+" включено";
        }
        float pw=font.width(info,SMALL)+14;
        UiRenderer.rounded(g,x+w+8,mid-6.5f,pw,13,6.5f,p(Theme.field()));
        font.mid(g,info,x+w+15,mid,SMALL,c(Theme.secondary()));
        alpha=saved;
        float hover=anim("profile",HOVER,over(PROFILE_X,PROFILE_Y,PROFILE_W,PROFILE_H) || profilesOpen ? 1 : 0,14);
        float open=anim("profile",OPEN,profilesOpen?1:0,16);
        pill(g,PROFILE_X,PROFILE_Y,PROFILE_W,PROFILE_H,hover);
        font.mid(g,profile,PROFILE_X+8,PROFILE_Y+PROFILE_H/2,SMALL,c(Theme.secondary()));
        chevron(g,PROFILE_X+PROFILE_W-10,PROFILE_Y+PROFILE_H/2,open,Icons.Icon.CHEVRON_DOWN);
        hit(PROFILE_X,PROFILE_Y,PROFILE_W,PROFILE_H,this::toggleProfiles,null,null);
        drawSearchField(g);
    }

    // ---------------------------------------------------------------- search

    private boolean searching() { return !query.isEmpty(); }
    private String contentKey() { return searching() ? SEARCH : category; }
    /**
     * Capsule left of the profile picker. It widens while focused or filled; the outline and icon turn to the
     * accent, the caret blinks, and a clear button fades in once there is text.
     */
    private void drawSearchField(GuiGraphicsExtractor g) {
        var font=MsdfFont.UI;
        boolean active=searchFocused || searching();
        float open=anim("search",OPEN,active?1:0,14),focus=anim("search",VALUE,searchFocused?1:0,16);
        float x=SIDE_X+8,w=SIDE_W-16,y=H-27,h=18;
        searchX=x; searchW=w; searchY=y;
        boolean over=over(x,y,w,h);
        if(over) frameHover="search";
        float hover=anim("search",HOVER,over?1:0,14);
        framed(g,x,y,w,h,fieldRadius(),mix(mix(Theme.field(),Theme.text(),.08f),Theme.accent(),Math.max(.6f*hover,focus)),Theme.field());
        Icons.draw(g,Icons.Icon.SEARCH,x+6,y+h/2-4,8,2.6f,c(mix(Theme.hint(),Theme.accent(),Math.max(focus,hover*.5f))));
        float clear=anim("search-clear",VALUE,searching()?1:0,16);
        float textX=x+17,room=w-17-8-10*clear;
        float[] clip=pushClip(g,textX-1,y,textX+room,y+h);
        try {
            if(query.isEmpty()) {
                float a=alpha; alpha*=1-.35f*focus;
                font.mid(g,"Поиск...",textX+2*focus,y+h/2,SMALL,c(Theme.hint()));
                alpha=a;
            }
            float textW=font.width(query,SMALL),shift=Math.max(0,textW+2-room);
            if(!query.isEmpty()) ess.ll.client.module.render.TypingAnimation.mid(g,font,"gui:search",query,textX-shift,y+h/2,SMALL,c(Theme.text()));
            if(searchFocused) {
                float blink=(float)(.5+.5*Math.cos((System.nanoTime()-caretAt)/1e9*Math.PI*2.2));
                UiRenderer.rounded(g,textX-shift+textW+.6f,y+3.5f,.9f,h-7,.45f,c(Theme.withAlpha(Theme.accent(),blink)));
            }
        } finally { popClip(g,clip); }
        // Shortcut hint while idle, clear button once there is text.
        float idle=1-open;
        if(idle>.01f) {
            float a=alpha; alpha*=idle;
            float kw=font.width("Ctrl F",6.5f)+8;
            UiRenderer.rounded(g,x+w-kw-4,y+3,kw,h-6,(h-6)/2,c(mix(Theme.field(),Theme.text(),.08f)));
            font.midCenter(g,"Ctrl F",x+w-kw/2-4,y+h/2,6.5f,c(Theme.hint()));
            alpha=a;
        }
        hit(x,y,w,h,()->{ if(!searchFocused) { searchFocused=true; caretAt=System.nanoTime(); UiSounds.select(); } },null,null);
        if(clear>.01f) {
            float cx=x+w-13,cy=y+h/2;
            float clearHover=anim("search-clear",HOVER,over(cx-1,y,13,h)?1:0,16);
            UiRenderer.rounded(g,cx,cy-4.5f,9,9,4.5f,c(Theme.withAlpha(mix(Theme.hint(),Theme.accent(),clearHover),(.25f+.25f*clearHover)*clear)));
            Icons.draw(g,Icons.Icon.CLOSE,cx+1.5f,cy-3,6,3,c(Theme.withAlpha(Theme.text(),clear)));
            if(searching()) hit(cx-1,y,13,h,()->{ setQuery(""); UiSounds.close(); },null,null);
        }
    }
    private void setQuery(String value) {
        boolean was=searching();
        query=value; caretAt=System.nanoTime();
        if(was!=searching()) { switchedAt=System.nanoTime(); popupTarget=null; listening=null; }
        updateResults();
    }
    /** Modules whose name or any setting name contains the query; name matches first. */
    private void updateResults() {
        String q=query.trim().toLowerCase(Locale.ROOT);
        List<Module> byName=new ArrayList<>(),bySetting=new ArrayList<>();
        if(!q.isEmpty()) for(var m:ClientModules.all()) {
            if(m.category.equals(THEMES) || m.category.equals(COSMETICS)) continue;
            String name=m.name.toLowerCase(Locale.ROOT);
            if(name.contains(q)) byName.add(m);
            else if(m.settings.stream().anyMatch(s->s.name().toLowerCase(Locale.ROOT).contains(q))) bySetting.add(m);
        }
        byName.sort(Comparator.comparing((Module m)->!m.name.toLowerCase(Locale.ROOT).startsWith(q)));
        List<Module> found=new ArrayList<>(byName); found.addAll(bySetting);
        long now=System.nanoTime();
        for(var it=shownAt.keySet().iterator();it.hasNext();) {
            Module m=it.next();
            if(!found.contains(m)) { it.remove(); anims.remove(List.of(m,"pos")); }
        }
        List<List<Module>> cols=List.of(new ArrayList<>(),new ArrayList<>());
        float[] heights=new float[2];
        for(var m:found) {
            shownAt.putIfAbsent(m,now);
            int col=heights[0]<=heights[1] ? 0 : 1;
            cols.get(col).add(m); heights[col]+=cardHeights.getOrDefault(m,cardHeight(0))+CARD_GAP;
        }
        results=cols;
        scrolls.get(SEARCH)[1]=0;
    }
    /** Search results: new cards rise in, the rest glide to their new places as the list changes. */
    private float drawResults(GuiGraphicsExtractor g,long now,float scroll) {
        if(results.get(0).isEmpty() && results.get(1).isEmpty()) {
            float t=ease((now-caretAt)/1e9*Theme.speed()/CARD_SECONDS),cx=RAIL+(W-RAIL)/2,cy=VIEW_TOP+(VIEW_BOTTOM-VIEW_TOP)/2-12+(1-t)*6;
            float a=alpha; alpha*=t;
            UiRenderer.rounded(g,cx-16,cy-30,32,32,10,c(Theme.field()));
            Icons.draw(g,Icons.Icon.SEARCH,cx-8,cy-22,16,2.2f,c(Theme.hint()));
            MsdfFont.UI.midCenter(g,"Ничего не найдено",cx,cy+12,9,c(Theme.text()));
            MsdfFont.UI.midCenter(g,"Попробуйте другое название или настройку",cx,cy+25,SMALL,c(Theme.hint()));
            alpha=a;
            return 0;
        }
        float bottom=0;
        for(int col=0;col<2;col++) {
            float x=RAIL+PAD+col*(COL_W+COL_GAP),y=VIEW_TOP+4;
            for(var m:results.get(col)) {
                Object pos=List.of(m,"pos");
                float ax=anim(pos,X,x,14),ay=anim(pos,Y,y,14);
                float a=ease((now-shownAt.getOrDefault(m,now))/1e9*Theme.speed()/CARD_SECONDS);
                float h=moduleCard(g,m,ax,ay-scroll,(1-a)*10,a);
                cardHeights.put(m,h);
                y+=h+CARD_GAP;
            }
            bottom=Math.max(bottom,y-CARD_GAP);
        }
        return bottom;
    }
    /** Module name with the matched part of the search query in the accent colour. */
    private void title(GuiGraphicsExtractor g,String name,float x,float mid,int color) {
        var font=MsdfFont.UI;
        String q=query.trim();
        int at=q.isEmpty() ? -1 : name.toLowerCase(Locale.ROOT).indexOf(q.toLowerCase(Locale.ROOT));
        if(at<0) { font.mid(g,name,x,mid,9,c(color)); return; }
        x+=font.mid(g,name.substring(0,at),x,mid,9,c(color));
        x+=font.mid(g,name.substring(at,at+q.length()),x,mid,9,c(Theme.accent()));
        font.mid(g,name.substring(at+q.length()),x,mid,9,c(color));
    }
    /** Field-coloured capsule whose outline turns towards the accent on hover. */
    private void pill(GuiGraphicsExtractor g,float x,float y,float w,float h,float hover) {
        framed(g,x,y,w,h,h/2,mix(mix(Theme.field(),Theme.text(),.08f),Theme.accent(),.6f*hover),Theme.field());
    }
    private void chevron(GuiGraphicsExtractor g,float cx,float cy,float open,Icons.Icon icon) {
        g.pose().pushMatrix();
        try {
            g.pose().translate(cx,cy);
            g.pose().rotate((float)(Math.PI*open));
            Icons.draw(g,icon,-3.5f,-3.5f,7,2.4f,c(mix(Theme.hint(),Theme.text(),open)));
        } finally { g.pose().popMatrix(); }
    }
    private void toggleProfiles() {
        profilesOpen=!profilesOpen;
        if(profilesOpen) try { profiles=ClientData.profiles(); } catch(java.io.IOException e) { profiles=List.of(); }
        UiSounds.expand(profilesOpen);
    }
    private float profilesHeight() { return (Math.max(1,profiles.size())*13+6)*peek("profile",OPEN); }
    private boolean inProfiles(float x,float y) {
        float listY=PROFILE_Y+PROFILE_H+4;
        return x>=PROFILE_X && x<PROFILE_X+PROFILE_W && y>=listY && y<listY+profilesHeight();
    }
    private boolean inPopup(float x,float y) {
        return popupTarget!=null && x>=popX && x<popX+POP_W && y>=popY && y<popY+popH;
    }
    private boolean inOverlay(float x,float y) { return inProfiles(x,y) || inPopup(x,y) || inPicker(x,y); }
    private void drawProfiles(GuiGraphicsExtractor g) {
        float open=peek("profile",OPEN);
        if(open<=.01f) return;
        var font=MsdfFont.UI;
        float y=PROFILE_Y+PROFILE_H+4,h=profilesHeight(),saved=alpha,r=fieldRadius();
        alpha*=Math.min(1,open*1.5f);
        UiRenderer.shadow(g,PROFILE_X,y+2,PROFILE_W,h,r,8,c(shade(1)));
        UiRenderer.rounded(g,PROFILE_X,y,PROFILE_W,h,r,c(Theme.card()));
        float[] clip=pushClip(g,PROFILE_X,y,PROFILE_X+PROFILE_W,y+h);
        try {
            if(profiles.isEmpty()) font.mid(g,"Нет конфигов",PROFILE_X+8,y+3+6.5f,SMALL,c(Theme.hint()));
            for(int i=0;i<profiles.size();i++) {
                String name=profiles.get(i);
                float oy=y+3+i*13;
                float hover=anim(List.of("profile",name),HOVER,profilesOpen && over(PROFILE_X,oy,PROFILE_W,13)?1:0,16);
                float current=name.equals(profile)?1:0;
                if(hover>.01f) UiRenderer.rounded(g,PROFILE_X+3,oy,PROFILE_W-6,13,r-1,c(Theme.withAlpha(Theme.accent(),.14f*hover)));
                font.mid(g,name,PROFILE_X+8+2*hover,oy+6.5f,SMALL,c(mix(Theme.secondary(),current>0 ? Theme.accent() : Theme.text(),Math.max(hover,current))));
                if(profilesOpen) hit(PROFILE_X,oy,PROFILE_W,13,()->loadProfile(name),null,null);
            }
        } finally { popClip(g,clip); alpha=saved; }
    }
    private void loadProfile(String name) {
        try { ClientData.loadProfile(name); profile=name; UiSounds.bindSet(); }
        catch(Exception e) { System.err.println("[ESS] Profile load failed: "+e.getMessage()); }
        profilesOpen=false;
    }

    // ---------------------------------------------------------------- content

    private void drawContent(GuiGraphicsExtractor g,long now) {
        float[] sc=scrolls.get(contentKey());
        sc[1]=Math.clamp(sc[1],0,sc[2]);
        sc[0]+=(sc[1]-sc[0])*(float)(1-Math.exp(-dt*16*Theme.speed()));
        if(Math.abs(sc[1]-sc[0])<.05f) sc[0]=sc[1];
        double since=(now-switchedAt)/1e9*Theme.speed();
        float bottom;
        float[] clip=pushClip(g,RAIL+1,VIEW_TOP,W-1,VIEW_BOTTOM);
        try {
            bottom=searching() ? drawResults(g,now,sc[0]) : category.equals(THEMES) ? drawThemes(g,since,sc[0])
                    : category.equals(COSMETICS) ? drawCosmetics(g,since,sc[0],now) : category.equals(BOTS) ? drawBots(g,since,sc[0])
                    : drawModules(g,since,sc[0]);
        }
        finally { popClip(g,clip); }
        sc[2]=Math.max(0,bottom-(VIEW_BOTTOM-6));
        if(sc[2]<1) sc[2]=0;
        boolean active=over(RAIL,VIEW_TOP,W-RAIL,VIEW_BOTTOM-VIEW_TOP) || Math.abs(sc[1]-sc[0])>.5f;
        float bar=anim("scrollbar",VALUE,sc[2]>0 ? active ? 1 : .3f : 0,10);
        if(bar>.01f && sc[2]>0) {
            float view=VIEW_BOTTOM-VIEW_TOP-8,thumb=Math.max(20,view*view/(view+sc[2]));
            float thumbY=VIEW_TOP+4+(view-thumb)*Math.clamp(sc[0]/sc[2],0,1);
            UiRenderer.rounded(g,W-5,thumbY,2,thumb,1,c(Theme.withAlpha(Theme.text(),.35f*bar)));
        }
    }
    /** Cards rise in one after another after opening and on every tab switch. */
    private static float appear(double since,int index) { return ease((since-index*STAGGER)/CARD_SECONDS); }
    private float drawModules(GuiGraphicsExtractor g,double since,float scroll) {
        float bottom=0;
        var cols=columns.get(category);
        for(int col=0;col<2;col++) {
            float x=RAIL+PAD+col*(COL_W+COL_GAP),y=VIEW_TOP+4-scroll;
            for(var m:cols.get(col)) {
                float a=appear(since,order.get(m));
                float h=moduleCard(g,m,x,y,(1-a)*10,a);
                cardHeights.put(m,h);
                y+=h+CARD_GAP;
            }
            bottom=Math.max(bottom,y-CARD_GAP+scroll);
        }
        return bottom;
    }
    private void card(GuiGraphicsExtractor g,float x,float y,float w,float h,float hover,float accent) {
        float r=Theme.radius();
        int edge=mix(Theme.border(),Theme.withAlpha(Theme.accent(),.45f+.2f*accent),hover);
        framed(g,x,y,w,h,r,edge,Theme.card());
    }
    /** Measures rows with their eased heights; returns the summed height. */
    private float measure(List<Setting> settings) {
        float rows=0;
        for(var s:settings) rows+=rowHeight(s,anim(s,OPEN,expanded.contains(s)?1:0,16))*anim(s,SHOWN,s.visible()?1:0,12);
        return rows;
    }
    /** Draws rows from y; rows that appear or hide grow from their top edge, clipped to their share. */
    private void rows(GuiGraphicsExtractor g,List<Setting> settings,float x,float y) {
        for(var s:settings) {
            float shown=peek(s,SHOWN),open=peek(s,OPEN),rowH=rowHeight(s,open)*shown;
            if(shown>.02f) {
                boolean clip=shown<.999f;
                float[] saved=clip ? pushClip(g,x-CP,y,x+INNER+CP,y+rowH) : null;
                float rowAlpha=alpha;
                alpha*=shown;
                try { drawSetting(g,s,x,y,open); }
                finally { alpha=rowAlpha; if(saved!=null) popClip(g,saved); }
            }
            y+=rowH;
        }
    }
    /** Draws one module card at y+shift and returns its eased height. */
    private float moduleCard(GuiGraphicsExtractor g,Module m,float x,float y,float shift,float appear) {
        if(sidebarLayout) return compactCard(g,m,x,y,shift,appear);
        var font=MsdfFont.UI;
        float enabled=anim(m,VALUE,m.enabled()?1:0,14);
        float rows=measure(m.settings)*anim(List.of(m,"card"),OPEN,1,14),h=cardHeight(rows);
        y+=shift;
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP-8) return h;
        float saved=alpha; alpha*=appear;
        try {
            boolean over=over(x,y,COL_W,h);
            if(over) frameHover=m;
            float hover=anim(m,HOVER,over?1:0,12);
            card(g,x,y,COL_W,h,hover,enabled);
            float mid=y+CP+HEAD_H/2;
            title(g,m.name,x+CP,mid,mix(Theme.secondary(),Theme.text(),Math.max(enabled,hover*.6f)));
            hit(x,y,COL_W,CP+HEAD_H,()->{ m.toggle(); UiSounds.toggle(m.enabled()); },null,null);
            float tx=x+COL_W-CP-20;
            bindPill(g,m,m.bind,m.hold,tx-6,mid);
            toggle(g,m,tx,mid-6,20,12,m.enabled(),over(tx,mid-6,20,12));
            hit(tx-2,mid-8,24,16,()->{ m.toggle(); UiSounds.toggle(m.enabled()); },null,null);
            settings(g,m,x,y,h,rows,enabled,1);
        } finally { alpha=saved; }
        return h;
    }
    /** Settings rows under the card header, clipped to the card while it grows or shrinks. */
    private void settings(GuiGraphicsExtractor g,Module m,float x,float y,float h,float rows,float enabled,float open) {
        if(rows<=.5f) return;
        // Settings of a disabled module stay usable but fade back.
        float inner=alpha;
        alpha*=(.55f+.45f*enabled)*open;
        float[] clip=pushClip(g,x,y+CARD_HEAD,x+COL_W,y+h-2);
        try {
            UiRenderer.rect(g,x,y+CARD_HEAD,COL_W,.6f,c(Theme.withAlpha(Theme.text(),.07f)));
            rows(g,m.settings,x+CP,y+CARD_HEAD+8);
        }
        finally { popClip(g,clip); alpha=inner; }
    }
    /**
     * Module card: name and a description, then the keycap (on hover or once bound), the switch and a chevron.
     * Left click toggles, right click or the chevron unfolds the settings, middle click opens the bind.
     */
    private float compactCard(GuiGraphicsExtractor g,Module m,float x,float y,float shift,float appear) {
        var font=MsdfFont.UI;
        float enabled=anim(m,VALUE,m.enabled()?1:0,14);
        boolean expandable=!m.settings.isEmpty();
        float open=anim(List.of(m,"card"),OPEN,expanded.contains(m) && expandable?1:0,14);
        float rows=measure(m.settings)*open,h=cardHeight(rows);
        y+=shift;
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP-8) return h;
        float saved=alpha; alpha*=appear;
        try {
            boolean over=over(x,y,COL_W,h);
            if(over) frameHover=m;
            float hover=anim(m,HOVER,over?1:0,12),r=Theme.radius();
            Theme.inset(g,x,y,COL_W,h,r,alpha);
            if(hover>.01f) UiRenderer.outline(g,x,y,COL_W,h,r,.75f,c(Theme.withAlpha(Theme.text(),.09f*hover)));
            title(g,m.name,x+CP,y+11.5f,mix(Theme.secondary(),Theme.text(),Math.max(enabled,.65f+.35f*hover)));
            font.mid(g,describe(m),x+CP,y+22,6.6f,c(Theme.hint()));
            Runnable toggle=()->{ m.toggle(); UiSounds.toggle(m.enabled()); };
            Runnable expand=expandable ? ()->toggle(m) : null;
            hit(x,y,COL_W,CARD_HEAD,toggle,expand,null,()->openBind(m));
            float right=x+COL_W-CP,mid=y+12;
            if(expandable) {
                chevron(g,right-3.5f,mid,open,Icons.Icon.CHEVRON_DOWN);
                hit(right-9,mid-7,12,14,expand,null,null);
                right-=14;
            }
            toggle(g,m,right-20,mid-5.5f,20,11,m.enabled(),over(right-22,mid-7,24,14));
            hit(right-22,mid-7,24,14,toggle,null,null);
            right-=26;
            // Unbound keycaps only show on hover, so the list stays quiet.
            float keycap=m.bind>=0 || popupTarget==m ? 1 : anim(List.of(m,"keycap"),VALUE,over?1:0,14);
            if(keycap>.01f) {
                float a=alpha; alpha*=keycap;
                bindPill(g,m,m.bind,m.hold,right,mid);
                alpha=a;
            }
            settings(g,m,x,y,h,rows,enabled,open);
        } finally { alpha=saved; }
        return h;
    }
    /** The line under a module's name. */
    private static String describe(Module m) {
        String known=DESCRIPTIONS.get(m.name);
        if(known!=null) return known;
        int count=0;
        for(var s:m.settings) if(s.visible()) count++;
        return count==0 ? "Без настроек" : "Настроек: "+count;
    }
    private static final Map<String,String> DESCRIPTIONS=Map.ofEntries(
            Map.entry("Aura","Бьёт врагов в радиусе"),Map.entry("CrystalAura","Кристаллы и анкоры по цели"),
            Map.entry("AutoTotem","Держит тотем в руке"),Map.entry("Criticals","Каждый удар — критический"),
            Map.entry("AntiBot","Не трогает ботов античита"),Map.entry("NoFriendDamage","Друзья не получают урон"),
            Map.entry("WebTrap","Ловит цель в паутину"),Map.entry("AutoAnchor","Взрывает якоря возрождения"),
            Map.entry("Sprint","Всегда бежит"),Map.entry("AutoJump","Сам прыгает на ходу"),
            Map.entry("InventoryWalk","Ходьба с открытым инвентарём"),Map.entry("NoWeb","Паутина не замедляет"),
            Map.entry("AirStuck","Замирает в воздухе"),Map.entry("ElytraBooster","Разгон на элитрах"),
            Map.entry("ElytraMotyaon","Полёт на элитрах"),Map.entry("ElytraHelper","Помощь в полёте"),
            Map.entry("AutoRespawn","Возрождает после смерти"),Map.entry("NoPush","Тебя не толкают"),
            Map.entry("AutoBrewing","Варит зелья сам"),Map.entry("WebIgnore","Проходит сквозь паутину"),
            Map.entry("ClickPearl","Жемчуг по клавише"),Map.entry("FreeCam","Свободная камера"),
            Map.entry("AutoTool","Берёт нужный инструмент"),Map.entry("CustomTime","Своё время суток"),
            Map.entry("Gamma","Яркость без факелов"),Map.entry("Zoom","Приближение как в оптике"),
            Map.entry("HUD","Элементы интерфейса"),Map.entry("FogAtmosphere","Атмосферный туман"),
            Map.entry("DeathEffect","Эффект смерти цели"),Map.entry("JumpCircles","Круги от прыжков"),
            Map.entry("Hands","Вид рук от первого лица"),Map.entry("Trails","След за игроком"),
            Map.entry("HitBubbles","Пузыри при ударе"),Map.entry("BlockOverlay","Обводка блока под прицелом"),
            Map.entry("Crosshair","Свой прицел"),Map.entry("ItemPhysics","Физика выпавших вещей"),
            Map.entry("Arrows","Стрелки к игрокам"),Map.entry("CrystalPosESP","Точки для кристаллов"),
            Map.entry("TypingAnimation","Анимация печати"),Map.entry("HpAlert","Сигнал о низком HP"),
            Map.entry("Wardrobe","Скины и одежда"),Map.entry("ShaderESP","Подсветка шейдером"),
            Map.entry("WorldTweaks","Погода, свет и туман"),Map.entry("SwingAnimation","Анимация взмаха"),
            Map.entry("ViewModel","Положение предмета в руке"),Map.entry("Particles","Частицы при ударе"),
            Map.entry("Predictions","Траектории снарядов"),Map.entry("TargetESP","Метка на цели"),
            Map.entry("EntityESP","Плашки над сущностями"),Map.entry("BlockESP","Подсветка блоков"),
            Map.entry("Removals","Убирает лишние эффекты"),Map.entry("AimingItems","Предметы смотрят на цель"),
            Map.entry("CreamyKeys","Плавные клавиши"),Map.entry("PvpSafe","Защита в PvP-режиме"),
            Map.entry("Optimization","Больше FPS"),Map.entry("NoDelay","Без задержек действий"),
            Map.entry("ServerMenu","Меню сервера"),Map.entry("GriefJoiner","Автовход на гриф"),
            Map.entry("ItemScroller","Быстрый перенос вещей"),Map.entry("BetterChest","Удобные сундуки"),
            Map.entry("ResourcePackSpoof","Обход ресурспаков"),Map.entry("ShulkerPreview","Содержимое шалкеров"),
            Map.entry("Emotes","Эмоции персонажа"),Map.entry("ClickGUI","Это меню"));
    /** Up to three key values of a module (modes, sliders, multi selects) for its collapsed card. */
    private static List<String> preview(Module m) {
        List<String> tags=new ArrayList<>();
        for(var s:m.settings) {
            if(tags.size()==3) break;
            if(!s.visible()) continue;
            if(s instanceof ModeSettings mode) tags.add(mode.get());
            else if(s instanceof SliderSetting slider) {
                // First word of the name, or the whole name when that word is already taken ("Дистанция атаки").
                String word=s.name().split(" ")[0];
                boolean taken=tags.stream().anyMatch(t->t.startsWith(word+" "));
                tags.add((taken ? s.name() : word)+" "+slider.format(slider.get()));
            }
            else if(s instanceof MultiBooleanSettings multi) {
                int on=0;
                for(int i=0;i<multi.options.size();i++) if(multi.selected(i)) on++;
                tags.add(multi.name()+" "+on+"/"+multi.options.size());
            }
        }
        return tags;
    }
    /** Opens the bind popup of a module whose keycap is on screen (it anchors to the keycap). */
    private void openBind(Module m) {
        if(popupTarget==m) { closePopup(); return; }
        popupTarget=m; popupShown=m; listening=m; profilesOpen=false;
        UiSounds.bindListen();
    }
    /** A setting's name and the line under it. */
    private void label(GuiGraphicsExtractor g,Setting s,String description,float x,float y,float hover) {
        MsdfFont.UI.mid(g,s.name(),x,y+5,8,c(mix(Theme.secondary(),Theme.text(),.75f+.25f*hover)));
        MsdfFont.UI.mid(g,description,x,y+14,6.6f,c(Theme.hint()));
    }
    private void drawSetting(GuiGraphicsExtractor g,Setting setting,float x,float y,float open) {
        if(setting instanceof BooleanSetting b) {
            boolean over=over(x,y-2,INNER,LABEL2+2);
            float hover=anim(List.of(b,"row"),HOVER,over?1:0,14);
            label(g,b,b.get() ? "Включено" : "Выключено",x,y,hover);
            toggle(g,b,x+INNER-20,y+4,20,11,b.get(),over);
            hit(x,y-2,INNER,LABEL2+2,()->{ b.toggle(); UiSounds.toggle(b.get()); },null,null);
        } else if(setting instanceof SliderSetting s) slider(g,s,x,y);
        else if(setting instanceof ModeSettings mode) modeDrop(g,mode,x,y,open);
        else if(setting instanceof MultiBooleanSettings multi) {
            int on=0;
            for(int i=0;i<multi.options.size();i++) if(multi.selected(i)) on++;
            label(g,multi,"Выбрано "+on+" из "+multi.options.size(),x,y,0);
            chips(g,multi,x,y+LABEL2);
        } else if(setting instanceof ColorSetting color) colorRow(g,color,x,y,open);
        else if(setting instanceof ActionSetting action) button(g,action,x,y,INNER);
        else if(setting instanceof KeybindSetting bind) {
            float hover=anim(List.of(bind,"row"),HOVER,over(x,y,INNER,LABEL2)?1:0,14);
            label(g,bind,"Клавиша вызова",x,y,hover);
            bindPill(g,bind,bind.get(),false,x+INNER,y+7);
        }
    }

    // ---------------------------------------------------------------- controls

    /** iOS-style switch: the track fills with the accent, the knob slides and stretches mid-way. */
    private void toggle(GuiGraphicsExtractor g,Object key,float x,float y,float w,float h,boolean on,boolean hover) {
        float v=anim(key,VALUE,on?1:0,14),hv=anim(List.of(key,"switch"),HOVER,hover?1:0,14);
        UiRenderer.rounded(g,x-.6f,y-.6f,w+1.2f,h+1.2f,h/2+.6f,c(Theme.withAlpha(mix(Theme.field(),Theme.text(),.1f+.1f*hv),1-v)));
        UiRenderer.rounded(g,x,y,w,h,h/2,c(mix(Theme.field(),Theme.accent(),v)));
        float d=h-4,stretch=2.5f*(float)Math.sin(Math.PI*v)+hv;
        UiRenderer.rounded(g,x+2+(w-4-d)*v-stretch*v,y+2,d+stretch,d,d/2,c(mix(Theme.hint(),Theme.onAccent(),v)));
    }
    /** Name and range, the value on the right ("70 · 100"), and a thin track with a white knob below. */
    private void slider(GuiGraphicsExtractor g,SliderSetting s,float x,float y) {
        var font=MsdfFont.UI;
        boolean active=dragging!=null && dragging.key()==s;
        float hover=anim(s,HOVER,active || over(x,y,INNER,LABEL2+TRACK_H) ? 1 : 0,14);
        float shown=anim(s,VALUE,s.get(),active ? 30 : 16),f=Math.clamp((shown-s.min)/(s.max-s.min),0,1);
        label(g,s,"От "+s.format(s.min)+" до "+s.format(s.max),x,y,hover);
        String max=" · "+s.format(s.max);
        float mw=font.width(max,6.8f);
        font.midRight(g,max,x+INNER,y+5,6.8f,c(Theme.hint()));
        font.midRight(g,s.format(shown),x+INNER-mw,y+5,6.8f,c(Theme.accent()));
        float ty=y+LABEL2+3;
        UiRenderer.rounded(g,x,ty-1.5f,INNER,3,1.5f,c(Theme.field()));
        UiRenderer.rounded(g,x,ty-1.5f,Math.max(3,INNER*f),3,1.5f,c(Theme.accent()));
        float k=7+1.5f*hover;
        UiRenderer.rounded(g,x+INNER*f-k/2,ty-k/2,k,k,k/2,c(0xffffffff));
        hit(x,y,INNER,LABEL2+TRACK_H,null,null,new Drag(s,x,INNER,v->{
            float before=s.get();
            s.set(s.min+v*(s.max-s.min));
            if(s.get()!=before) UiSounds.slider(s.fraction());
        }));
    }
    /** Single choice: a field with the current value as a chip and a chevron; it unfolds a list with a check. */
    private void modeDrop(GuiGraphicsExtractor g,ModeSettings mode,float x,float y,float open) {
        var font=MsdfFont.UI;
        label(g,mode,"Один из "+mode.options.size(),x,y,0);
        float by=y+LABEL2,r=fieldRadius();
        float hover=anim(List.of(mode,"drop"),HOVER,over(x,by,INNER,DROP_H)?1:0,14);
        UiRenderer.rounded(g,x,by,INNER,DROP_H,r,c(mix(Theme.field(),Theme.text(),.03f+.04f*hover)));
        String value=mode.get();
        float vw=font.width(value,6.8f)+10;
        UiRenderer.rounded(g,x+3,by+3,vw,DROP_H-6,Math.max(2,r-1.5f),c(mix(Theme.field(),Theme.text(),.12f)));
        font.midCenter(g,value,x+3+vw/2,by+DROP_H/2,6.8f,c(Theme.text()));
        chevron(g,x+INNER-9,by+DROP_H/2,open,Icons.Icon.CHEVRON_DOWN);
        hit(x,by,INNER,DROP_H,()->toggle(mode),null,null);
        if(open<=.01f) return;
        float listY=by+DROP_H+2,saved=alpha;
        float[] clip=pushClip(g,x,listY,x+INNER,listY+(mode.options.size()*DROP_ROW+2)*open);
        alpha*=open;
        try {
            for(int i=0;i<mode.options.size();i++) {
                final int option=i;
                float ry=listY+i*DROP_ROW,mid=ry+DROP_ROW/2;
                Object k=List.of(mode,i);
                float rowHover=anim(k,HOVER,over(x,ry,INNER,DROP_ROW)?1:0,16),on=anim(k,VALUE,mode.options.get(i).equals(value)?1:0,16);
                if(rowHover+on>.01f) UiRenderer.rounded(g,x,ry,INNER,DROP_ROW,Math.max(2,r-1),c(Theme.withAlpha(Theme.accent(),.1f*rowHover+.16f*on)));
                font.mid(g,mode.options.get(i),x+7+rowHover,mid,6.8f,c(mix(Theme.secondary(),Theme.text(),Math.max(on,rowHover*.6f))));
                if(on>.01f) Icons.draw(g,Icons.Icon.CHECK,x+INNER-12,mid-3.5f,7,3,c(Theme.withAlpha(Theme.accent(),on)));
                hit(x,ry,INNER,DROP_ROW,()->{ if(option!=mode.options.indexOf(mode.get())) { mode.select(option); UiSounds.select(); } },null,null);
            }
        } finally { alpha=saved; popClip(g,clip); }
    }
    /** Sidebar slider: name above a thin track, the value riding in an accent pill that swells while held. */
    private void pillSlider(GuiGraphicsExtractor g,SliderSetting s,float x,float y) {
        var font=MsdfFont.UI;
        boolean active=dragging!=null && dragging.key()==s;
        float hover=anim(s,HOVER,active || over(x,y,INNER,PILL_SLIDER_H) ? 1 : 0,14);
        float shown=anim(s,VALUE,s.get(),active ? 30 : 16);
        float fraction=Math.clamp((shown-s.min)/(s.max-s.min),0,1);
        font.mid(g,s.name(),x,y+4,SMALL,c(mix(Theme.secondary(),Theme.text(),hover)));
        String value=s.format(shown);
        float pw=Math.max(24,font.width(value,6.5f)+10),ph=11+hover,mid=y+15.5f;
        float px=x+(INNER-pw)*fraction,grow=2*hover;
        UiRenderer.rounded(g,x,mid-1.5f,INNER,3,1.5f,c(Theme.field()));
        if(px+pw/2-x>.5f) UiRenderer.rounded(g,x,mid-1.5f,px+pw/2-x,3,1.5f,c(Theme.withAlpha(Theme.accent(),.35f+.15f*hover)));
        if(hover>.01f) UiRenderer.shadow(g,px-grow/2,mid-ph/2+1,pw+grow,ph,ph/2,5,c(Theme.withAlpha(Theme.accent(),.4f*hover)));
        UiRenderer.rounded(g,px-grow/2,mid-ph/2,pw+grow,ph,ph/2,c(mix(Theme.accent(),Theme.accentLight(),.2f*hover)));
        font.midCenter(g,value,px+pw/2,mid,6.5f,c(Theme.onAccent()));
        // The pill's centre follows the cursor.
        hit(x,y,INNER,PILL_SLIDER_H,null,null,new Drag(s,x+pw/2,INNER-pw,f->{
            float before=s.get();
            s.set(s.min+f*(s.max-s.min));
            if(s.get()!=before) UiSounds.slider(s.fraction());
        }));
    }
    /** Sidebar multi select: name, a summary of what is on and a chevron; a click unfolds a check list. */
    private void dropdown(GuiGraphicsExtractor g,MultiBooleanSettings s,float x,float y,float open) {
        var font=MsdfFont.UI;
        boolean over=over(x,y,INNER,DROP_H);
        float hover=anim(List.of(s,"drop"),HOVER,over?1:0,14),r=fieldRadius();
        framed(g,x,y,INNER,DROP_H,r,mix(mix(Theme.field(),Theme.text(),.06f+.06f*hover),Theme.accent(),.6f*open),Theme.field());
        float nameW=font.mid(g,s.name(),x+7,y+DROP_H/2,SMALL,c(mix(Theme.secondary(),Theme.text(),Math.max(hover,open))));
        font.midRight(g,summary(s,INNER-nameW-32),x+INNER-17,y+DROP_H/2,SMALL,c(Theme.text()));
        chevron(g,x+INNER-9,y+DROP_H/2,open,Icons.Icon.CHEVRON_DOWN);
        hit(x,y,INNER,DROP_H,()->toggle(s),null,null);
        if(open<=.01f) return;
        float listY=y+DROP_H+2,saved=alpha;
        float[] clip=pushClip(g,x,listY,x+INNER,listY+s.options.size()*DROP_ROW*open);
        alpha*=open;
        try {
            for(int i=0;i<s.options.size();i++) {
                final int option=i;
                float ry=listY+i*DROP_ROW,mid=ry+DROP_ROW/2;
                Object k=List.of(s,i);
                float rowHover=anim(k,HOVER,over(x,ry,INNER,DROP_ROW)?1:0,16),on=anim(k,VALUE,s.selected(i)?1:0,16);
                if(rowHover>.01f) UiRenderer.rounded(g,x,ry,INNER,DROP_ROW,r-1,c(Theme.withAlpha(Theme.accent(),.1f*rowHover)));
                UiRenderer.rounded(g,x+6,mid-4,8,8,2.5f,c(mix(mix(Theme.field(),Theme.text(),.22f),Theme.accent(),on)));
                UiRenderer.rounded(g,x+7,mid-3,6,6,1.8f,c(mix(Theme.field(),Theme.accent(),on)));
                if(on>.01f) Icons.draw(g,Icons.Icon.CHECK,x+6.5f,mid-3.5f,7,3.2f,c(Theme.withAlpha(Theme.onAccent(),on)));
                font.mid(g,s.options.get(i),x+19+rowHover,mid,SMALL,c(mix(Theme.secondary(),Theme.text(),Math.max(on,rowHover*.6f))));
                hit(x,ry,INNER,DROP_ROW,()->{ s.toggle(option); UiSounds.toggle(s.selected(option)); },null,null);
            }
        } finally { alpha=saved; popClip(g,clip); }
    }
    /** "Игроки, Голые +1"; falls back to a count when the names do not fit. */
    private static String summary(MultiBooleanSettings s,float room) {
        List<String> on=new ArrayList<>();
        for(int i=0;i<s.options.size();i++) if(s.selected(i)) on.add(s.options.get(i));
        if(on.isEmpty()) return "ничего";
        for(int shown=Math.min(2,on.size());shown>=1;shown--) {
            String text=String.join(", ",on.subList(0,shown))+(on.size()>shown ? " +"+(on.size()-shown) : "");
            if(MsdfFont.UI.width(text,SMALL)<=room) return text;
        }
        return on.size()+" из "+s.options.size();
    }
    /**
     * Segmented control: the flat selection glides between cells; an unselected cell under the cursor
     * gets a soft fill, a slight lift of its label, and a short flash when pressed.
     */
    private void segmented(GuiGraphicsExtractor g,Object key,float x,float y,float w,List<String> options,int selected,IntConsumer select) {
        var font=MsdfFont.UI;
        float[][] cells=segLayout(options,w);
        float h=cells[cells.length-1][1]+SEG_H+SEG_PAD,r=fieldRadius(),cellR=Math.max(2,r-1.5f);
        UiRenderer.rounded(g,x,y,w,h,r,p(Theme.field()));
        float[] hover=new float[cells.length],on=new float[cells.length];
        for(int i=0;i<cells.length;i++) {
            float[] cell=cells[i];
            Object k=List.of(key,i);
            hover[i]=anim(k,HOVER,over(x+cell[0],y+cell[1],cell[2],SEG_H)?1:0,16);
            on[i]=anim(k,VALUE,i==selected?1:0,16);
            float press=anim(k,PRESS,0,6),soft=Math.max(hover[i]*(1-on[i]),press);
            if(soft>.01f) UiRenderer.rounded(g,x+cell[0],y+cell[1],cell[2],SEG_H,cellR,
                    c(Theme.withAlpha(mix(Theme.text(),Theme.accent(),press),.07f*hover[i]*(1-on[i])+.18f*press)));
        }
        float[] target=cells[Math.clamp(selected,0,cells.length-1)];
        Object sel=List.of(key,"selection");
        float sx=anim(sel,X,target[0],18),sy=anim(sel,Y,target[1],18),sw=anim(sel,WIDTH,target[2],18);
        UiRenderer.rounded(g,x+sx,y+sy,sw,SEG_H,cellR,c(Theme.light() ? Theme.card() : mix(Theme.field(),Theme.text(),.12f)));
        for(int i=0;i<cells.length;i++) {
            final int option=i;
            float[] cell=cells[i];
            float lift=hover[i]*(1-on[i])*.6f;
            font.midCenter(g,options.get(i),x+cell[0]+cell[2]/2,y+cell[1]+SEG_H/2-lift,SMALL,
                    c(mix(mix(Theme.secondary(),Theme.text(),hover[i]*.75f),Theme.text(),on[i])));
            hit(x+cell[0],y+cell[1],cell[2],SEG_H,()->{
                if(option!=selected) { kick(List.of(key,option),PRESS,1); select.accept(option); UiSounds.select(); }
            },null,null);
        }
    }
    /** Chips: accent-filled when on, a field when off. */
    private void chips(GuiGraphicsExtractor g,MultiBooleanSettings s,float x,float y) {
        var font=MsdfFont.UI;
        float[][] cells=chipLayout(s.options,INNER);
        for(int i=0;i<cells.length;i++) {
            final int option=i;
            float[] cell=cells[i];
            float cx=x+cell[0],cy=y+cell[1];
            Object k=List.of(s,i);
            float hover=anim(k,HOVER,over(cx,cy,cell[2],CHIP_H)?1:0,16),on=anim(k,VALUE,s.selected(i)?1:0,16);
            UiRenderer.rounded(g,cx,cy,cell[2],CHIP_H,4,c(mix(mix(Theme.field(),Theme.text(),.04f+.05f*hover),Theme.accent(),on)));
            font.midCenter(g,s.options.get(i),cx+cell[2]/2,cy+CHIP_H/2,6.8f,c(mix(mix(Theme.secondary(),Theme.text(),hover*.5f),Theme.onAccent(),on)));
            hit(cx,cy,cell[2],CHIP_H,()->{ s.toggle(option); UiSounds.toggle(s.selected(option)); },null,null);
        }
    }
    /** Colour row: the hex, three shades of the colour and the button that opens the picker. */
    private void colorRow(GuiGraphicsExtractor g,ColorSetting color,float x,float y,float open) {
        var font=MsdfFont.UI;
        label(g,color,color.alpha ? "Цвет и прозрачность" : "Цвет эффекта",x,y,0);
        float by=y+LABEL2,h=COLOR_ROW_H-2,r=fieldRadius();
        String hex=color.hex();
        float hw=font.width(hex,6.6f)+10;
        UiRenderer.rounded(g,x,by,hw,h,Math.max(2,r-1),c(mix(Theme.field(),Theme.text(),.04f)));
        font.midCenter(g,hex,x+hw/2,by+h/2,6.6f,c(Theme.secondary()));
        int base=color.get()|0xff000000;
        float sx=x+hw+4,sw=h-3;
        for(int shade:new int[]{base,mix(base,0xffffffff,.3f),mix(base,0xffffffff,.6f)}) {
            UiRenderer.rounded(g,sx,by+1.5f,sw,sw,3,c(shade));
            sx+=sw+3;
        }
        float bx=sx+2,bw=x+INNER-bx;
        boolean shown=picker==color;
        float hover=anim(List.of(color,"open"),HOVER,shown || over(bx,by,bw,h) ? 1 : 0,14);
        UiRenderer.rounded(g,bx,by,bw,h,Math.max(2,r-1),c(mix(Theme.accent(),Theme.accentLight(),.2f*hover)));
        font.mid(g,shown ? "Скрыть палитру" : "Палитра",bx+7,by+h/2,6.8f,c(Theme.onAccent()));
        Icons.draw(g,Icons.Icon.PENCIL,bx+bw-12,by+h/2-3.5f,7,2.2f,c(Theme.onAccent()));
        if(shown) { pickAnchorX=bx+bw; pickAnchorY=by+h; }
        hit(bx,by,bw,h,()->openPicker(color,bx+bw,by+h),null,null);
    }
    private void openPicker(ColorSetting color,float anchorX,float anchorY) {
        if(picker==color) { closePicker(); return; }
        picker=color; pickerShown=color; pickAnchorX=anchorX; pickAnchorY=anchorY;
        pickHue=color.hsb()[0]; hexFocused=false; popupTarget=null; profilesOpen=false;
        UiSounds.expand(true);
    }
    private void closePicker() {
        if(picker==null) return;
        picker=null; hexFocused=false;
        UiSounds.expand(false);
    }
    private boolean inPicker(float x,float y) {
        return picker!=null && x>=pickX && x<pickX+PICK_W && y>=pickY && y<pickY+pickH;
    }
    /**
     * Colour picker: saturation and brightness on a square (white to the hue across, black down), a hue bar, an alpha
     * bar over a checker for colours with alpha, the preview with an editable hex, and preset colours.
     */
    private void drawPicker(GuiGraphicsExtractor g) {
        float open=anim("picker",OPEN,picker!=null?1:0,18);
        if(open<=.01f) { pickerShown=null; return; }
        ColorSetting color=pickerShown;
        if(color==null) return;
        var font=MsdfFont.UI;
        float pad=8,iw=PICK_W-pad*2,svH=78,barH=8,r=Theme.radius();
        float h=pad+13+svH+6+barH+(color.alpha ? 6+barH : 0)+8+15+6+12+pad;
        float x=Math.clamp(pickAnchorX-PICK_W,CONTENT_X,W-PAD-PICK_W),y=pickAnchorY+4;
        if(y+h>H-4) y=Math.max(4,pickAnchorY-COLOR_ROW_H-4-h);
        pickX=x; pickY=y; pickH=h;
        float saved=alpha; alpha*=open;
        g.pose().pushMatrix();
        try {
            g.pose().translate(x+PICK_W,y); g.pose().scale(.92f+.08f*open,.92f+.08f*open); g.pose().translate(-x-PICK_W,-y);
            UiRenderer.shadow(g,x,y+3,PICK_W,h,r,12,c(shade(1.3f)));
            UiRenderer.rounded(g,x,y,PICK_W,h,r,c(mix(Theme.card(),Theme.text(),.1f)));
            UiRenderer.rounded(g,x+.75f,y+.75f,PICK_W-1.5f,h-1.5f,r-.75f,c(Theme.card()));
            float cx=x+pad,cy=y+pad;
            font.mid(g,"Палитра",cx,cy+5,8,c(Theme.text()));
            font.midRight(g,color.name(),x+PICK_W-pad,cy+5,6.6f,c(Theme.hint()));
            cy+=13;
            float[] hsb=color.hsb();
            // Grey and black have no hue of their own: the picker keeps the last one.
            if(hsb[1]>.02f && hsb[2]>.02f) pickHue=hsb[0];
            int hue=java.awt.Color.HSBtoRGB(pickHue,1,1)|0xff000000;
            UiRenderer.rounded(g,cx,cy,iw,svH,4,Gradient.linear(c(0xffffffff),c(hue),Gradient.Axis.HORIZONTAL,cx,iw));
            UiRenderer.rounded(g,cx,cy,iw,svH,4,Gradient.linear(c(0x00000000),c(0xff000000),Gradient.Axis.VERTICAL,cy,svH));
            float kx=cx+iw*hsb[1],ky=cy+svH*(1-hsb[2]);
            UiRenderer.rounded(g,kx-4,ky-4,8,8,4,c(color.get()|0xff000000));
            UiRenderer.outline(g,kx-4,ky-4,8,8,4,1.5f,c(0xffffffff));
            if(picker!=null) hit(cx,cy,iw,svH,null,null,new Drag(List.of(color,"sv"),cx,iw,null,cy,svH,(sx,sy)->color.setHsb(pickHue,sx,1-sy)));
            cy+=svH+6;
            float seg=iw/6;
            for(int i=0;i<6;i++) {
                int from=java.awt.Color.HSBtoRGB(i/6f,1,1)|0xff000000,to=java.awt.Color.HSBtoRGB((i+1)/6f,1,1)|0xff000000;
                float sx=cx+i*seg;
                UiRenderer.rounded(g,sx,cy,seg+.4f,barH,i==0 || i==5 ? barH/2 : 0,Gradient.linear(c(from),c(to),Gradient.Axis.HORIZONTAL,sx,seg));
            }
            UiRenderer.rounded(g,cx+iw*pickHue-2.5f,cy-1.5f,5,barH+3,2.5f,c(0xffffffff));
            if(picker!=null) hit(cx-3,cy-2,iw+6,barH+4,null,null,new Drag(List.of(color,"hue"),cx,iw,v->{
                pickHue=Math.min(v,.999f);
                float[] current=color.hsb();
                color.setHsb(pickHue,current[1],current[2]);
            }));
            cy+=barH+6;
            if(color.alpha) {
                checker(g,cx,cy,iw,barH);
                UiRenderer.rounded(g,cx,cy,iw,barH,barH/2,Gradient.linear(c(color.get()&0xffffff),c(color.get()|0xff000000),Gradient.Axis.HORIZONTAL,cx,iw));
                float a=((color.get()>>>24)&255)/255f;
                UiRenderer.rounded(g,cx+iw*a-2.5f,cy-1.5f,5,barH+3,2.5f,c(0xffffffff));
                if(picker!=null) hit(cx-3,cy-2,iw+6,barH+4,null,null,new Drag(List.of(color,"alpha"),cx,iw,color::setAlpha));
                cy+=barH+6;
            }
            cy+=2;
            if(color.alpha) checker(g,cx,cy,15,15);
            UiRenderer.rounded(g,cx,cy,15,15,4,c(color.get()|(color.alpha ? 0 : 0xff000000)));
            float fx=cx+19,fw=iw-19;
            float focus=anim("hex",VALUE,hexFocused?1:0,16);
            framed(g,fx,cy,fw,15,fieldRadius(),mix(mix(Theme.field(),Theme.text(),.08f),Theme.accent(),focus),Theme.field());
            String text=hexFocused ? "#"+hexText : color.hex();
            font.mid(g,text,fx+6,cy+7.5f,7,c(hexFocused ? Theme.text() : Theme.secondary()));
            if(hexFocused) {
                float blink=(float)(.5+.5*Math.cos((System.nanoTime()-caretAt)/1e9*Math.PI*2.2));
                UiRenderer.rounded(g,fx+6+font.width(text,7)+.6f,cy+3.5f,.9f,8,.45f,c(Theme.withAlpha(Theme.accent(),blink)));
            }
            if(picker!=null) hit(fx,cy,fw,15,()->{
                hexFocused=true; searchFocused=false; caretAt=System.nanoTime();
                hexText=color.hex().substring(1);
            },null,null);
            cy+=15+6;
            int[] presets={Theme.accent(),0xffffffff,0xffff5f57,0xffffa94d,0xffffd43b,0xff51cf66,0xff63c8ff,0xff8b8cf6,0xffda77f2,0xff1b1f29};
            float pw=(iw-(presets.length-1)*3)/presets.length;
            for(int i=0;i<presets.length;i++) {
                int preset=presets[i];
                float px=cx+i*(pw+3);
                float hover=anim(List.of("preset",i),HOVER,picker!=null && over(px,cy,pw,12)?1:0,16);
                UiRenderer.rounded(g,px,cy-hover,pw,12,3,c(preset));
                if(picker!=null) hit(px,cy,pw,12,()->{
                    color.set((color.get()&0xff000000)|(preset&0xffffff));
                    pickHue=color.hsb()[0];
                    UiSounds.select();
                },null,null);
            }
        } finally { g.pose().popMatrix(); alpha=saved; }
    }
    /** Grey checkerboard under see-through colours. */
    private void checker(GuiGraphicsExtractor g,float x,float y,float w,float h) {
        UiRenderer.rounded(g,x,y,w,h,Math.min(4,h/2),c(0xffd0d0d0));
        float cell=h/2;
        for(int row=0;row<2;row++) for(float cx=x+(row%2)*cell;cx<x+w-.1f;cx+=cell*2)
            UiRenderer.rect(g,cx,y+row*cell,Math.min(cell,x+w-cx),cell,c(0xff9a9a9a));
    }
    /** Applies the typed hex (6 or 8 digits) and leaves the field. */
    private void commitHex() {
        if(picker!=null && !hexText.isEmpty()) try {
            int value=ColorSetting.parse(hexText);
            picker.set(picker.alpha || hexText.length()==8 ? value : (picker.get()&0xff000000)|(value&0xffffff));
            pickHue=picker.hsb()[0];
            UiSounds.bindSet();
        } catch(IllegalArgumentException ignored) {}
        hexFocused=false;
    }
    private void toggle(Object key) {
        boolean open=!expanded.remove(key);
        if(open) expanded.add(key);
        UiSounds.expand(open);
    }
    /** Button; "Сохранить тему" is accent-filled. A click flashes it. */
    private void button(GuiGraphicsExtractor g,ActionSetting action,float x,float y,float w) {
        boolean primary=action==saveTheme;
        float hover=anim(action,HOVER,over(x,y,w,BUTTON_H)?1:0,14),press=anim(action,PRESS,0,5);
        float r=fieldRadius();
        int fill=primary ? mix(Theme.accent(),Theme.accentLight(),.18f*hover) : mix(Theme.field(),Theme.text(),.07f*hover);
        fill=mix(fill,primary ? 0xffffffff : Theme.accent(),.35f*press);
        UiRenderer.rounded(g,x,y,w,BUTTON_H,r,primary ? c(fill) : p(fill));
        String label=primary && (System.nanoTime()-savedAt)/1e9<1.5 ? "Сохранено" : action.name();
        MsdfFont.UI.midCenter(g,label,x+w/2,y+BUTTON_H/2,TEXT,c(primary ? Theme.onAccent() : Theme.text()));
        hit(x,y,w,BUTTON_H,()->{ kick(action,PRESS,1); UiSounds.select(); action.run(); },null,null);
    }

    // ---------------------------------------------------------------- binds

    /** Bind capsule with a keyboard icon; opens the bind popup. Right edge at right. */
    private void bindPill(GuiGraphicsExtractor g,Object key,int code,boolean hold,float right,float mid) {
        var font=MsdfFont.UI;
        String label=code<0 ? "—" : Keys.label(code);
        float textW=font.width(label,SMALL),holdW=hold ? font.width("hold",6.5f)+4 : 0;
        float w=8+9+3+textW+holdW+7,h=13,x=right-w,y=mid-h/2;
        boolean open=popupTarget==key;
        float hover=anim(List.of(key,"bind"),HOVER,open || over(x,y,w,h) ? 1 : 0,14);
        pill(g,x,y,w,h,hover);
        Icons.draw(g,Icons.Icon.KEYBOARD,x+7,mid-4.5f,9,2.2f,c(mix(Theme.hint(),Theme.accent(),hover)));
        font.mid(g,label,x+19,mid,SMALL,c(mix(Theme.secondary(),Theme.text(),hover)));
        if(hold) font.mid(g,"hold",x+19+textW+4,mid,6.5f,c(Theme.accent()));
        if(open) { popupRight=x+w; popupTop=y; popupBottom=y+h; }
        hit(x,y,w,h,()->{
            if(popupTarget==key) closePopup();
            else { popupTarget=key; popupShown=key; listening=null; profilesOpen=false; UiSounds.expand(true); }
        },null,null);
    }
    private void closePopup() {
        if(popupTarget==null) return;
        popupTarget=null; listening=null;
        UiSounds.expand(false);
    }
    private void drawPopup(GuiGraphicsExtractor g) {
        float open=anim("popup",OPEN,popupTarget!=null?1:0,18);
        if(open<=.01f) { popupShown=null; return; }
        if(popupShown==null) return;
        var font=MsdfFont.UI;
        Object target=popupShown;
        Module module=target instanceof Module m ? m : null;
        String name=module!=null ? module.name : ((KeybindSetting)target).name();
        int code=module!=null ? module.bind : ((KeybindSetting)target).get();
        float h=module!=null ? 112 : 84,r=Theme.radius();
        float x=Math.clamp(popupRight-POP_W,RAIL+6,W-POP_W-6),y=popupBottom+4;
        if(y+h>H-6) y=popupTop-4-h;
        popX=x; popY=y; popH=h;
        float saved=alpha; alpha*=open;
        // Pops from its top right corner, next to the pill.
        g.pose().pushMatrix();
        try {
            g.pose().translate(x+POP_W,y); g.pose().scale(.92f+.08f*open,.92f+.08f*open); g.pose().translate(-x-POP_W,-y);
            UiRenderer.shadow(g,x,y+3,POP_W,h,r,12,c(shade(1.3f)));
            UiRenderer.rounded(g,x,y,POP_W,h,r,c(mix(Theme.card(),Theme.text(),.1f)));
            UiRenderer.rounded(g,x+.75f,y+.75f,POP_W-1.5f,h-1.5f,r-.75f,c(Theme.card()));
            float ix=x+9,iw=POP_W-18;
            Icons.draw(g,Icons.Icon.KEYBOARD,ix,y+7,10,2.2f,c(Theme.accent()));
            font.mid(g,"Бинд",ix+14,y+12,8.5f,c(Theme.text()));
            font.midRight(g,name,x+POP_W-9,y+12,SMALL,c(Theme.hint()));
            // Key box: click, then press a key; it breathes while waiting.
            boolean waiting=listening==target;
            float ky=y+22,pulse=waiting ? .5f+.5f*(float)Math.sin(System.nanoTime()/1e9*6) : 0;
            float hover=anim("popup-key",HOVER,popupTarget!=null && over(ix,ky,iw,22)?1:0,14);
            float wait=anim("popup-key",VALUE,waiting?1:0,14);
            UiRenderer.rounded(g,ix,ky,iw,22,fieldRadius(),c(mix(mix(Theme.field(),Theme.text(),.1f*hover),Theme.accent(),wait*(.55f+.45f*pulse))));
            UiRenderer.rounded(g,ix+1,ky+1,iw-2,20,fieldRadius()-1,c(Theme.field()));
            if(wait>.01f) {
                float a=alpha; alpha*=wait;
                font.midCenter(g,"Нажмите клавишу...",ix+iw/2,ky+11,SMALL,c(Theme.withAlpha(Theme.accent(),.6f+.4f*pulse)));
                alpha=a;
            }
            if(wait<.99f) {
                float a=alpha; alpha*=1-wait;
                if(code<0) font.midCenter(g,"Не назначен",ix+iw/2,ky+11,TEXT,c(Theme.secondary()));
                else font.midCenter(g,Keys.label(code),ix+iw/2,ky+11,11,c(Theme.text()));
                alpha=a;
            }
            if(popupTarget!=null) hit(ix,ky,iw,22,()->{
                listening=waiting ? null : target;
                if(!waiting) UiSounds.bindListen();
            },null,null);
            float by=ky+28;
            if(module!=null) {
                font.mid(g,"Режим",ix,by+3,SMALL,c(Theme.secondary()));
                segmented(g,List.of(target,"mode"),ix,by+9,iw,List.of("Нажатие","Удержание"),module.hold ? 1 : 0,i->module.hold=i==1);
                by+=9+SEG_H+2*SEG_PAD+6;
            }
            float bw=(iw-4)/2;
            button(g,unbind(target),ix,by,bw);
            doneButton(g,ix+bw+4,by,bw);
            font.midCenter(g,"Esc — отмена, Backspace — снять",x+POP_W/2,by+BUTTON_H+8,6.5f,c(Theme.hint()));
        } finally { g.pose().popMatrix(); alpha=saved; }
    }
    private final Map<Object,ActionSetting> unbindActions=new HashMap<>();
    private ActionSetting unbind(Object target) {
        return unbindActions.computeIfAbsent(target,t->new ActionSetting("Сбросить",()->{
            if(t instanceof Module m) m.bind=-1;
            else if(t instanceof KeybindSetting k) k.set(-1);
            listening=null; UiSounds.bindSet();
        }));
    }
    private void doneButton(GuiGraphicsExtractor g,float x,float y,float w) {
        float hover=anim("popup-done",HOVER,popupTarget!=null && over(x,y,w,BUTTON_H)?1:0,14);
        UiRenderer.rounded(g,x,y,w,BUTTON_H,fieldRadius(),c(mix(Theme.accent(),Theme.accentLight(),.18f*hover)));
        MsdfFont.UI.midCenter(g,"Готово",x+w/2,y+BUTTON_H/2,TEXT,c(Theme.onAccent()));
        if(popupTarget!=null) hit(x,y,w,BUTTON_H,this::closePopup,null,null);
    }

    // ---------------------------------------------------------------- themes tab

    private float drawThemes(GuiGraphicsExtractor g,double since,float scroll) {
        float x=RAIL+PAD,y=VIEW_TOP+4-scroll;
        float a=appear(since,0);
        y+=presetsCard(g,x,y,(1-a)*10,a)+CARD_GAP;
        float left=y,right=y,rx=x+COL_W+COL_GAP;
        a=appear(since,1); left+=settingsCard(g,"Цвета",colorSettings,x,left,(1-a)*10,a)+CARD_GAP;
        a=appear(since,2); right+=settingsCard(g,"Форма и эффекты",formSettings,rx,right,(1-a)*10,a)+CARD_GAP;
        a=appear(since,3); right+=buttonsCard(g,rx,right,(1-a)*10,a)+CARD_GAP;
        return Math.max(left,right)-CARD_GAP+scroll;
    }
    private float settingsCard(GuiGraphicsExtractor g,String title,List<Setting> settings,float x,float y,float shift,float appear) {
        float rows=measure(settings),h=cardHeight(rows);
        y+=shift;
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP-8) return h;
        float saved=alpha; alpha*=appear;
        try {
            boolean over=over(x,y,COL_W,h);
            if(over) frameHover=title;
            card(g,x,y,COL_W,h,anim(title,HOVER,over?1:0,12),0);
            MsdfFont.UI.mid(g,title,x+CP,y+CP+HEAD_H/2,9,c(Theme.text()));
            rows(g,settings,x+CP,y+CP+HEAD_H+6);
        } finally { alpha=saved; }
        return h;
    }
    private float buttonsCard(GuiGraphicsExtractor g,float x,float y,float shift,float appear) {
        float h=CP*2+BUTTON_H;
        y+=shift;
        float saved=alpha; alpha*=appear;
        try {
            card(g,x,y,COL_W,h,anim("buttons",HOVER,over(x,y,COL_W,h)?1:0,12),0);
            float bw=(INNER-4)/2;
            button(g,saveTheme,x+CP,y+CP,bw);
            button(g,resetTheme,x+CP+bw+4,y+CP,bw);
        } finally { alpha=saved; }
        return h;
    }
    /** Ready-made themes: a dark / light switch and a horizontally scrolling strip of preview tiles. */
    private float presetsCard(GuiGraphicsExtractor g,float x,float y,float shift,float appear) {
        float h=CP*2+HEAD_H+6+PRESET_H;
        y+=shift;
        List<Theme.Preset> list=Theme.all().stream().filter(p->p.light()==lightGroup).toList();
        railW=FULL_W-CP*2;
        railMax=Math.max(0,list.size()*(PRESET_W+PRESET_GAP)-PRESET_GAP-railW);
        railTarget=Math.clamp(railTarget,0,railMax);
        railScroll+=(railTarget-railScroll)*(float)(1-Math.exp(-dt*14*Theme.speed()));
        if(Math.abs(railTarget-railScroll)<.05f) railScroll=railTarget;
        railX=x+CP; railY=y+CP+HEAD_H+6;
        float saved=alpha; alpha*=appear;
        try {
            card(g,x,y,FULL_W,h,anim("presets",HOVER,over(x,y,FULL_W,h)?1:0,12),0);
            float mid=y+CP+HEAD_H/2;
            MsdfFont.UI.mid(g,"Готовые темы",x+CP,mid,9,c(Theme.text()));
            float arrowR=x+FULL_W-CP-14,arrowL=arrowR-18;
            arrow(g,"rail-left",Icons.Icon.CHEVRON_LEFT,arrowL,mid-7,railTarget>0,-1);
            arrow(g,"rail-right",Icons.Icon.CHEVRON_RIGHT,arrowR,mid-7,railTarget<railMax,1);
            segmented(g,"group",arrowL-8-GROUP_W,mid-(SEG_H+2*SEG_PAD)/2,GROUP_W,List.of("Тёмные","Светлые"),lightGroup ? 1 : 0,i->selectGroup(i==1));
            float[] clip=pushClip(g,railX,railY-2,railX+railW,railY+PRESET_H+2);
            try {
                for(int i=0;i<list.size();i++) {
                    float px=railX+i*(PRESET_W+PRESET_GAP)-railScroll;
                    if(px+PRESET_W<railX-1 || px>railX+railW+1) continue;
                    presetTile(g,list.get(i),px,railY);
                }
            } finally { popClip(g,clip); }
            // Soft fades at the strip edges while there is more to scroll.
            float fadeL=anim("rail-fade",X,railScroll>1?1:0,10),fadeR=anim("rail-fade",Y,railScroll<railMax-1?1:0,10);
            if(fadeL>.01f) UiRenderer.rounded(g,railX,railY,12,PRESET_H,0,Gradient.linear(c(Theme.withAlpha(Theme.card(),fadeL)),c(Theme.withAlpha(Theme.card(),0)),Gradient.Axis.HORIZONTAL,railX,12));
            if(fadeR>.01f) UiRenderer.rounded(g,railX+railW-12,railY,12,PRESET_H,0,Gradient.linear(c(Theme.withAlpha(Theme.card(),0)),c(Theme.withAlpha(Theme.card(),fadeR)),Gradient.Axis.HORIZONTAL,railX+railW-12,12));
        } finally { alpha=saved; }
        return h;
    }
    private void arrow(GuiGraphicsExtractor g,String key,Icons.Icon icon,float x,float y,boolean enabled,int direction) {
        float hover=anim(key,HOVER,enabled && over(x,y,14,14)?1:0,14),active=anim(key,VALUE,enabled?1:0,12);
        float saved=alpha; alpha*=.4f+.6f*active;
        pill(g,x,y,14,14,hover);
        Icons.draw(g,icon,x+3,y+3,8,2.6f,c(mix(Theme.secondary(),Theme.text(),hover)));
        alpha=saved;
        hit(x,y,14,14,()->{ if(enabled) { railTarget+=direction*2*(PRESET_W+PRESET_GAP); UiSounds.select(); } },null,null);
    }
    private void selectGroup(boolean light) {
        if(light==lightGroup) return;
        lightGroup=light; railTarget=0;
        Theme.all().stream().filter(p->p.light()==light).findFirst().ifPresent(Theme::apply);
    }
    /** Theme tile: a tiny window preview in the theme's own colours; saved themes delete on right click. */
    private void presetTile(GuiGraphicsExtractor g,Theme.Preset p,float x,float y) {
        var font=MsdfFont.UI;
        boolean current=Theme.isCurrent(p);
        boolean over=over(x,y,PRESET_W,PRESET_H);
        if(over) frameHover=p;
        float hover=anim(p,HOVER,over?1:0,14),on=anim(p,VALUE,current?1:0,14);
        float r=fieldRadius()+1;
        y-=hover;
        framed(g,x,y,PRESET_W,PRESET_H,r,1.2f,mix(mix(Theme.field(),Theme.text(),.08f*hover),Theme.accent(),on),mix(Theme.field(),Theme.text(),.04f*hover));
        float px=x+4,py=y+4,pw=PRESET_W-8;
        UiRenderer.rounded(g,px,py,pw,36,r-1.5f,c(p.background()));
        UiRenderer.rounded(g,px+3,py+3,6,30,2.5f,c(p.card()));
        UiRenderer.rounded(g,px+11,py+3,pw-14,30,3,c(p.card()));
        UiRenderer.rounded(g,px+15,py+8,22,2.5f,1.25f,c(p.text()));
        UiRenderer.rounded(g,px+15,py+13,14,2.5f,1.25f,c(p.hint()));
        UiRenderer.rounded(g,px+15,py+25,12,4,2,c(p.accent()));
        UiRenderer.rounded(g,px+pw-15,py+24,9,5,2.5f,c(p.accent()));
        font.mid(g,p.name(),x+5,y+47.5f,SMALL,c(mix(Theme.secondary(),Theme.text(),Math.max(on,hover))));
        if(on>.01f) Icons.draw(g,Icons.Icon.CHECK,x+PRESET_W-13,y+43.5f,8,3,c(Theme.withAlpha(Theme.accent(),on)));
        hit(x,y,PRESET_W,PRESET_H,()->{ Theme.apply(p); UiSounds.select(); },p.file()!=null ? ()->{
            try { Theme.delete(p); UiSounds.close(); } catch(Exception e) { System.err.println("[ESS] Theme delete failed: "+e.getMessage()); }
        } : null,null);
    }

    // ---------------------------------------------------------------- wardrobe tab

    /** Sets are shown as a category of their own, after the item categories. */
    private static final int SETS=Wardrobe.Category.values().length;
    private static int wardrobeCategory=Wardrobe.Category.CLOTHING.ordinal();
    private static Wardrobe.Slot wardrobeSlot;
    private static final float PREVIEW_W=150;
    private long slotChangedAt=System.nanoTime();
    /** Extra preview turn and zoom from dragging and the wheel; both reset when the category changes. */
    private float userYaw,userZoom=1;
    private boolean rotating;
    private float previewX,previewY,previewW,previewH;
    private final Map<Object,Float> spin=new HashMap<>();
    /** Item under the cursor this frame; it is tried on in the preview. */
    private Wardrobe.Item tryOn;

    private static List<String> categoryTitles() {
        List<String> titles=new ArrayList<>();
        for(var c:Wardrobe.Category.values()) titles.add(c.title());
        titles.add("Сеты");
        return titles;
    }
    private static List<Wardrobe.Slot> slotsOf(int category) {
        if(category>=SETS) return List.of();
        var c=Wardrobe.Category.values()[category];
        return Arrays.stream(Wardrobe.Slot.values()).filter(s->s.category==c).toList();
    }

    /** Live player preview on the left; categories, slots and a grid of live item tiles on the right. */
    private float drawCosmetics(GuiGraphicsExtractor g,double since,float scroll,long now) {
        var font=MsdfFont.UI;
        float px=RAIL+PAD,py=VIEW_TOP+4,ph=VIEW_BOTTOM-4-py;
        previewX=px; previewY=py; previewW=PREVIEW_W; previewH=ph;
        float a0=appear(since,0),saved=alpha;
        alpha*=a0;
        try { drawPreview(g,px,py+(1-a0)*10,PREVIEW_W,ph); } finally { alpha=saved; }
        tryOn=null;
        float rx=px+PREVIEW_W+10,rw=W-PAD-rx,y=VIEW_TOP+4-scroll,a1=appear(since,1);
        List<String> titles=categoryTitles();
        List<Wardrobe.Slot> slots=slotsOf(wardrobeCategory);
        if(wardrobeSlot!=null && !slots.contains(wardrobeSlot)) wardrobeSlot=null;
        alpha*=a1;
        try {
            segmented(g,"wardrobe-category",rx,y+(1-a1)*10,rw,titles,wardrobeCategory,this::selectWardrobeCategory);
            y+=segHeight(titles,rw)+6;
            if(slots.size()>1) {
                List<String> slotTitles=new ArrayList<>(List.of("Все"));
                slots.forEach(s->slotTitles.add(s.title()));
                segmented(g,"wardrobe-slot",rx,y,rw,slotTitles,wardrobeSlot==null ? 0 : slots.indexOf(wardrobeSlot)+1,i->{
                    wardrobeSlot=i==0 ? null : slots.get(i-1); slotChangedAt=System.nanoTime(); scrolls.get(COSMETICS)[1]=0;
                });
                y+=segHeight(slotTitles,rw)+6;
            }
            // Swords: the usual skins, or the CS knives with their own hands and animations.
            if(wardrobeSlot==Wardrobe.Slot.SWORD) {
                List<String> styles=List.of("Default","CS");
                segmented(g,"wardrobe-sword",rx,y,rw,styles,swordStyle,i->{ swordStyle=i; slotChangedAt=System.nanoTime(); scrolls.get(COSMETICS)[1]=0; });
                y+=segHeight(styles,rw)+6;
            }
            String worn=wornLine(slots);
            while(font.width(worn,SMALL)>rw-60 && worn.length()>8) worn=worn.substring(0,worn.length()-4)+"...";
            font.mid(g,worn,rx+2,y+7.5f,SMALL,c(worn.startsWith("Ничего") || worn.startsWith("Наденьте") ? Theme.hint() : Theme.secondary()));
            boolean any=wardrobeCategory<SETS && (wardrobeSlot==null ? slots : List.of(wardrobeSlot)).stream().anyMatch(s->Wardrobe.worn(s)!=null);
            float off=anim("cosmetic-off",VALUE,any?1:0,14);
            if(off>.01f) {
                float bw=font.width("Снять",SMALL)+16,bx=rx+rw-bw,a=alpha;
                alpha*=off;
                float hover=anim("cosmetic-off",HOVER,over(bx,y,bw,15)?1:0,14);
                pill(g,bx,y,bw,15,hover);
                font.midCenter(g,"Снять",bx+bw/2,y+7.5f,SMALL,c(mix(Theme.secondary(),Theme.text(),hover)));
                alpha=a;
                if(any) hit(bx,y,bw,15,()->{
                    for(var s:wardrobeSlot==null ? slots : List.of(wardrobeSlot)) Wardrobe.takeOff(s);
                    UiSounds.toggle(false);
                },null,null);
            }
        } finally { alpha=saved; }
        y+=21;
        double sinceSlot=(now-slotChangedAt)/1e9*Theme.speed();
        float gap=6;
        if(wardrobeCategory>=SETS) {
            Wardrobe.hover(null,false);
            // Saved outfits first: save what you wear now, click one to put it on, right click to delete it.
            var font2=MsdfFont.UI;
            font2.mid(g,"Мои образы",rx+2,y+7.5f,8,c(Theme.text()));
            float bw=font2.width("Сохранить текущий",SMALL)+16,bx=rx+rw-bw;
            float hover=anim("wardrobe-save",HOVER,over(bx,y,bw,15)?1:0,14);
            pill(g,bx,y,bw,15,hover);
            font2.midCenter(g,"Сохранить текущий",bx+bw/2,y+7.5f,SMALL,c(mix(Theme.secondary(),Theme.text(),hover)));
            hit(bx,y,bw,15,()->{
                if(Wardrobe.wornCount()==0) return;
                Wardrobe.savePreset("Образ "+(Wardrobe.presets().size()+1));
                UiSounds.toggle(true);
            },null,null);
            y+=21;
            var presets=Wardrobe.presets();
            if(presets.isEmpty()) { font2.mid(g,"Сохраните надетое, чтобы вернуть его в один клик",rx+2,y+5,SMALL,c(Theme.hint())); y+=16; }
            float rowH=26;
            for(int i=0;i<presets.size();i++) {
                var preset=presets.get(i);
                float py2=y+i*(rowH+4);
                boolean o=over(rx,py2,rw,rowH);
                float h=anim(preset,HOVER,o?1:0,12);
                framed(g,rx,py2,rw,rowH,Theme.radius(),1.2f,mix(Theme.field(),Theme.accent(),.4f*h),mix(Theme.card(),Theme.field(),.35f+.45f*h));
                font2.mid(g,preset.name(),rx+10,py2+rowH/2,7.5f,c(Theme.text()));
                font2.midRight(g,preset.items().size()+" вещей · ПКМ — удалить",rx+rw-10,py2+rowH/2,SMALL,c(Theme.hint()));
                hit(rx,py2,rw,rowH,()->{ Wardrobe.applyPreset(preset); UiSounds.toggle(true); },()->{ Wardrobe.deletePreset(preset); UiSounds.toggle(false); },null);
            }
            y+=presets.size()*(rowH+4)+10;
            font2.mid(g,"Сеты",rx+2,y+7.5f,8,c(Theme.text()));
            y+=18;
            var sets=Wardrobe.sets();
            float sh=40;
            for(int i=0;i<sets.size();i++) {
                float a=Math.min(a1,ease((sinceSlot-i*.035)/CARD_SECONDS));
                setRow(g,sets.get(i),rx,y+i*(sh+gap)+(1-a)*12,rw,sh,a);
            }
            return y+sets.size()*(sh+gap)-gap+scroll;
        }
        float tw=(rw-gap*2)/3,th=tw+22;
        var c=Wardrobe.Category.values()[wardrobeCategory];
        var list=wardrobeSlot==null ? Wardrobe.items(c) : Wardrobe.items(wardrobeSlot);
        if(wardrobeSlot==Wardrobe.Slot.SWORD) list=list.stream().filter(i->i.cs()==(swordStyle==1)).toList();
        for(int i=0;i<list.size();i++) {
            float ty=y+(i/3)*(th+gap);
            if(ty>VIEW_BOTTOM+20 || ty+th<VIEW_TOP-20) continue;
            float a=Math.min(a1,ease((sinceSlot-Math.min(i,24)*.03)/CARD_SECONDS));
            cosmeticTile(g,list.get(i),rx+(i%3)*(tw+gap),ty,(1-a)*12,tw,th,a);
        }
        Wardrobe.hover(tryOn,tryOn!=null);
        int rows=(list.size()+2)/3;
        return y+rows*(th+gap)-gap+scroll;
    }
    private String wornLine(List<Wardrobe.Slot> slots) {
        if(wardrobeCategory>=SETS) {
            var set=Wardrobe.fullSet(Wardrobe.outfit(false));
            return set==null ? "Наденьте все вещи сета ради бонуса" : "Полный сет: "+set.name();
        }
        List<String> names=new ArrayList<>();
        for(var s:wardrobeSlot==null ? slots : List.of(wardrobeSlot)) { var w=Wardrobe.worn(s); if(w!=null) names.add(w.name()); }
        return names.isEmpty() ? "Ничего не надето" : "Надето: "+String.join(", ",names);
    }
    /** One set: its colour, name, bonus and how many pieces you wear; a click wears all of it. */
    private void setRow(GuiGraphicsExtractor g,Wardrobe.OutfitSet set,float x,float y,float w,float h,float appear) {
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP) return;
        var font=MsdfFont.UI;
        float saved=alpha;
        alpha*=appear;
        try {
            boolean over=over(x,y,w,h);
            if(over) frameHover=set;
            int worn=0;
            for(String id:set.pieces()) if(Wardrobe.worn(Wardrobe.item(id))) worn++;
            boolean full=worn==set.pieces().size();
            float hover=anim(set,HOVER,over?1:0,12),on=anim(set,VALUE,full?1:0,12);
            int color=0xff000000|set.color();
            framed(g,x,y,w,h,Theme.radius(),1.2f,mix(mix(Theme.field(),color,.35f+.4f*hover),Theme.accent(),on*.5f),mix(Theme.card(),Theme.field(),.35f+.45f*hover));
            UiRenderer.rounded(g,x+8,y+8,3,h-16,1.5f,c(color));
            font.mid(g,set.name(),x+18,y+13,8.5f,c(Theme.text()));
            String bonus=set.bonus();
            while(font.width(bonus,SMALL)>w-60 && bonus.length()>8) bonus=bonus.substring(0,bonus.length()-4)+"...";
            font.mid(g,bonus,x+18,y+27,SMALL,c(Theme.secondary()));
            font.midRight(g,worn+" / "+set.pieces().size(),x+w-10,y+13,SMALL,c(full ? Theme.accent() : Theme.hint()));
            hit(x,y,w,h,()->{ Wardrobe.wear(set); UiSounds.toggle(true); },null,null);
        } finally { alpha=saved; }
    }
    /** The real player model with the hovered item tried on; the camera glides to that body part. */
    private void drawPreview(GuiGraphicsExtractor g,float x,float y,float w,float h) {
        var font=MsdfFont.UI;
        float r=Theme.radius();
        boolean over=over(x,y,w,h);
        float hover=anim("preview",HOVER,over || rotating ? 1 : 0,10);
        card(g,x,y,w,h,hover,0);
        UiRenderer.rounded(g,x+.75f,y+.75f,w-1.5f,h-1.5f,r-.75f,Gradient.linear(c(Theme.withAlpha(Theme.accent(),.12f)),c(Theme.withAlpha(Theme.accent(),0)),Gradient.Axis.VERTICAL,y,h*.75f));
        float[] focus=focus(wardrobeSlot,wardrobeCategory);
        float time=WardrobeIcons.time(),sway=rotating ? 0 : (float)Math.sin(time*.5f)*7;
        float zoom=anim("pv-zoom",VALUE,focus[0]*userZoom,5),focusY=anim("pv-y",VALUE,focus[1],5);
        float yaw=anim("pv-yaw",VALUE,focus[2]+userYaw,rotating ? 20 : 6),shift=anim("pv-shift",VALUE,focus[3],5);
        float pitch=anim("pv-pitch",VALUE,focus[4],5);
        float floorY=y+h/2+(focusY)*h*.8f/1.9f*zoom;
        if(floorY<y+h-4) UiRenderer.rounded(g,x+w/2-26*Math.min(zoom,2),floorY-3,52*Math.min(zoom,2),6,3,c(Theme.withAlpha(0x000000,Theme.light() ? .12f : .3f)));
        var player=minecraft.player;
        if(player!=null) {
            var state=minecraft.getEntityRenderDispatcher().getRenderer(player).createRenderState(player,1);
            state.shadowPieces.clear(); state.outlineColor=0; state.nameTag=null;
            if(state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar) {
                avatar.id=ess.ll.client.wardrobe.WardrobeLayer.PREVIEW_ID;
                ess.ll.client.wardrobe.WardrobeLayer.hideParts(avatar);
                // Weapon and item skins need the item in hand: the preview holds the slot's item, whatever you hold.
                var held=tryOn!=null && tryOn.slot().held() ? tryOn.slot() : wardrobeSlot!=null && wardrobeSlot.held() ? wardrobeSlot : null;
                // "Все" of weapons or items: a worn one of the category, or its first slot.
                if(held==null && wardrobeSlot==null && wardrobeCategory<Wardrobe.Category.values().length) {
                    var shownCategory=Wardrobe.Category.values()[wardrobeCategory];
                    for(var slot:Wardrobe.Slot.values()) if(slot.category==shownCategory && slot.held() && (held==null || Wardrobe.worn(slot)!=null && Wardrobe.worn(held)==null)) held=slot;
                }
                if(held!=null) {
                    var stack=held.sample();
                    avatar.mainArm=net.minecraft.world.entity.HumanoidArm.RIGHT;
                    avatar.rightHandItemStack=stack;
                    avatar.rightArmPose=net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
                    minecraft.getItemModelResolver().updateForLiving(avatar.rightHandItemState,stack,
                            net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,player);
                }
            }
            if(state instanceof LivingEntityRenderState living) {
                living.bodyRot=180+yaw+sway; living.yRot=0; living.xRot=0;
                living.boundingBoxWidth/=living.scale; living.boundingBoxHeight/=living.scale; living.scale=1;
            }
            int ax0=Math.round(left+(x+1)*scale),ay0=Math.round(top+(y+1)*scale),ax1=Math.round(left+(x+w-1)*scale),ay1=Math.round(top+(y+h-1)*scale);
            Quaternionf tilt=new Quaternionf().rotateX((float)Math.toRadians(pitch));
            Quaternionf rotation=new Quaternionf().rotateZ((float)Math.PI).mul(tilt);
            g.entity(state,h*.8f/1.9f*zoom*scale,new Vector3f(shift,focusY,0),rotation,tilt,ax0,ay0,ax1,ay1);
        }
        float st=ease((System.nanoTime()-slotChangedAt)/1e9*Theme.speed()/.35),saved=alpha;
        alpha*=st;
        String title=wardrobeCategory>=SETS ? "Сеты" : wardrobeSlot!=null ? wardrobeSlot.title() : Wardrobe.Category.values()[wardrobeCategory].title();
        font.mid(g,title,x+10,y+13+(1-st)*4,9,c(Theme.text()));
        var shown=tryOn!=null ? tryOn : wardrobeSlot!=null ? Wardrobe.worn(wardrobeSlot) : null;
        if(shown!=null) {
            String name=shown.name();
            while(font.width(name,SMALL)>w-20 && name.length()>4) name=name.substring(0,name.length()-4)+"...";
            font.mid(g,name,x+10,y+24+(1-st)*4,SMALL,c(shown.rarity().color));
            font.mid(g,shown.rarity().title(),x+10,y+33+(1-st)*4,5.5f,c(Theme.hint()));
        } else font.mid(g,"надето: "+Wardrobe.wornCount(),x+10,y+24+(1-st)*4,SMALL,c(Theme.hint()));
        alpha=saved;
        float hint=anim("pv-hint",VALUE,over && !rotating ? 1 : 0,8);
        if(hint>.01f) {
            alpha*=hint;
            font.midCenter(g,"Тяните — вращать, колесо — зум",x+w/2,y+h-9,6.5f,c(Theme.hint()));
            alpha=saved;
        }
    }
    /** Preview camera per slot: zoom, height above the feet (blocks), yaw, horizontal shift, pitch. */
    private static float[] focus(Wardrobe.Slot slot,int category) {
        if(slot==null && category<SETS) slot=switch (Wardrobe.Category.values()[category]) {
            case CAPE -> Wardrobe.Slot.CAPE;
            case PET -> Wardrobe.Slot.PET;
            default -> null;
        };
        if(slot==null) return new float[]{1,1,25,0,0};
        return switch (slot) {
            case HEAD -> new float[]{1.45f,1.8f,25,0,-6};
            case BODY,NECK -> new float[]{1.35f,1.25f,20,0,0};
            case LEGS -> new float[]{1.3f,.45f,28,0,14};
            case BACK,CAPE -> new float[]{.85f,1.1f,200,0,0};
            case WATCH -> new float[]{1.55f,.8f,88,0,0};
            case PET -> new float[]{.75f,1,-25,.2f,0};
            default -> new float[]{1,1,25,0,0};
        };
    }
    /** Tile: the item's live model, its name and rarity, and a check badge when worn. */
    private void cosmeticTile(GuiGraphicsExtractor g,Wardrobe.Item item,float x,float y,float shift,float w,float h,float appear) {
        y+=shift;
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP) return;
        var font=MsdfFont.UI;
        float saved=alpha;
        alpha*=appear;
        try {
            boolean worn=Wardrobe.worn(item),over=over(x,y,w,h);
            if(over) { frameHover=item; tryOn=item; }
            float hover=anim(item,HOVER,over?1:0,12),on=anim(item,VALUE,worn?1:0,12),press=anim(item,PRESS,0,5);
            y-=hover*1.5f;
            float r=Theme.radius();
            framed(g,x,y,w,h,r,1.2f,mix(mix(mix(Theme.field(),Theme.text(),.06f),item.rarity().color,.25f+.35f*hover),Theme.accent(),on),
                    mix(mix(Theme.card(),Theme.field(),.35f+.45f*hover),Theme.accent(),.07f*on+.18f*press));
            float size=w-14,speed=anim(List.of(item,"spin"),VALUE,over ? 150 : 24,4);
            float angle=spin.merge(item,(float)(dt*speed),Float::sum);
            boolean flat=item.slot()==Wardrobe.Slot.CAPE || item.slot()==Wardrobe.Slot.BACK;
            float yaw=(flat ? 180 : 25)+(flat ? 38*(float)Math.sin(Math.toRadians(angle*1.6f)) : angle);
            if(!WardrobeIcons.draw(g,item,x+w/2,y+6+size/2,size*.86f*(1+.08f*hover+.12f*press),yaw,-12,alpha))
                font.midCenter(g,"...",x+w/2,y+6+size/2,9,c(Theme.hint()));
            String name=item.name();
            while(font.width(name,SMALL)>w-8 && name.length()>4) name=name.substring(0,name.length()-4)+"...";
            font.midCenter(g,name,x+w/2,y+h-15,SMALL,c(mix(Theme.secondary(),Theme.text(),Math.max(on,hover))));
            font.midCenter(g,item.rarity().title(),x+w/2,y+h-6.5f,5,c(Theme.withAlpha(item.rarity().color,.9f)));
            if(on>.01f) {
                float b=backEase(on)*11;
                UiRenderer.rounded(g,x+w-5-b,y+5,b,b,b/2,c(Theme.accent()));
                if(on>.5f) Icons.draw(g,Icons.Icon.CHECK,x+w-5-b*.82f,y+5+b*.18f,b*.64f,3.4f,c(Theme.withAlpha(Theme.onAccent(),(on-.5f)*2)));
            }
            hit(x,y,w,h,()->{
                kick(item,PRESS,1);
                Wardrobe.toggle(item);
                UiSounds.toggle(Wardrobe.worn(item));
            },worn ? ()->{ Wardrobe.takeOff(item.slot()); UiSounds.toggle(false); } : null,null);
        } finally { alpha=saved; }
    }
    private static float backEase(float t) {
        float c1=1.9f,c3=c1+1;
        return 1+c3*(float)Math.pow(t-1,3)+c1*(float)Math.pow(t-1,2);
    }
    /** Switches the wardrobe category; the preview glides to it and the tiles rise in again. */
    public void selectWardrobeCategory(int category) {
        if(category==wardrobeCategory) return;
        wardrobeCategory=category; wardrobeSlot=null; slotChangedAt=System.nanoTime(); userYaw=0; userZoom=1;
        scrolls.get(COSMETICS)[1]=0;
    }
    private boolean inPreview(float x,float y) {
        return !searching() && category.equals(COSMETICS) && x>=previewX && x<previewX+previewW && y>=previewY && y<previewY+previewH;
    }

    /** Opens a module's category and its colour editors; used by the dev screenshot run. */
    public void expand(Module module) {
        category=module.category; lastCategory=category; switchedAt=System.nanoTime();
        expanded.add(module);
    }
    /** Opens Оружие → Мечи → CS; used by the dev screenshot run. */
    public void showCsKnives() { wardrobeSlot=Wardrobe.Slot.SWORD; swordStyle=1; }
    /** Shows or hides the settings panel; used by the dev screenshot run. */
    public void toggleSettings() { settingsOpen=!settingsOpen; }
    /** Opens the colour picker of a setting on screen; used by the dev screenshot run. */
    public void showPicker(ColorSetting color) { openPicker(color,W/2,H/2); }
    /** Switches to a tab; used by the dev screenshot run. */
    public void show(String tab) {
        if(scrolls.containsKey(tab)) { category=tab; lastCategory=tab; switchedAt=System.nanoTime(); }
    }
    /** Scrolls the current tab to its end; used by the dev screenshot run. */
    public void scrollToEnd() { float[] sc=scrolls.get(contentKey()); sc[1]=sc[2]; }
    /** Types into the search field; used by the dev screenshot run. */
    public void search(String text) { popupTarget=null; searchFocused=true; setQuery(text); }
    /** Opens the bind popup of a module; used by the dev screenshot run. */
    public void showBind(Module module) {
        category=module.category; lastCategory=category; popupTarget=module; popupShown=module;
    }
    /** Closes the bind popup; used by the dev screenshot run. */
    public void closeBind() { closePopup(); }

    // ---------------------------------------------------------------- bots

    private static final float BOT_CARD_H=CP*2+HEAD_H+11,FIELD_H=16,LOG_H=52,CHAT_GAP=4;
    /** The bot shown on the right; the main session or the first bot when nothing is picked. */
    private static Bot selectedBot;
    private static String botNick="",botAddress="",botChat="";
    private static int botCount=1;
    /** Which bot field takes typing: "nick", "address", "chat", or null. */
    private static String botField;
    private static String botError="";

    private float drawBots(GuiGraphicsExtractor g,double since,float scroll) {
        var bots=Bots.all();
        if(selectedBot!=null && !bots.contains(selectedBot)) selectedBot=null;
        if(selectedBot==null && !bots.isEmpty()) selectedBot=bots.getFirst();
        if(botAddress.isEmpty() && minecraft.getCurrentServer()!=null) botAddress=minecraft.getCurrentServer().ip;
        float x=RAIL+PAD,y=VIEW_TOP+4-scroll,left=y;
        float a=appear(since,0);
        left+=addBotCard(g,x,left,(1-a)*10,a)+CARD_GAP;
        for(int i=0;i<bots.size();i++) {
            a=appear(since,i+1);
            left+=botCard(g,bots.get(i),x,left,(1-a)*10,a)+CARD_GAP;
        }
        float right=y;
        if(selectedBot!=null) { a=appear(since,1); right+=botDetail(g,selectedBot,x+COL_W+COL_GAP,right,(1-a)*10,a)+CARD_GAP; }
        return Math.max(left,right)-CARD_GAP+scroll;
    }

    private float addBotCard(GuiGraphicsExtractor g,float x,float y,float shift,float appear) {
        float seg=SEG_H+SEG_PAD*2,h=CP*2+HEAD_H+6+FIELD_H*2+seg+BUTTON_H+4*3+(botError.isEmpty() ? 0 : 10);
        y+=shift;
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP-8) return h;
        float saved=alpha; alpha*=appear;
        try {
            boolean over=over(x,y,COL_W,h);
            card(g,x,y,COL_W,h,anim("bot-add",HOVER,over?1:0,12),0);
            MsdfFont.UI.mid(g,"Новый бот",x+CP,y+CP+HEAD_H/2,9,c(Theme.text()));
            float fy=y+CP+HEAD_H+6;
            botTextField(g,"nick",botNick,"Ник — пусто, случайный",x+CP,fy,INNER);
            fy+=FIELD_H+4;
            botTextField(g,"address",botAddress,"Адрес сервера",x+CP,fy,INNER);
            fy+=FIELD_H+4;
            segmented(g,"bot-count",x+CP,fy,INNER,List.of("1 бот","3 бота","5 ботов"),botCount==1 ? 0 : botCount==3 ? 1 : 2,i->botCount=i==0 ? 1 : i==1 ? 3 : 5);
            fy+=seg+4;
            if(!botError.isEmpty()) { MsdfFont.UI.mid(g,botError,x+CP,fy+3,6.5f,c(0xffff6b5e)); fy+=10; }
            botButton(g,"bot-connect","Подключить",Icons.Icon.PLUS,x+CP,fy,INNER,true,this::connectBots);
        } finally { alpha=saved; }
        return h;
    }
    private void connectBots() {
        String address=botAddress.trim();
        if(address.isEmpty()) { botError="Укажи адрес сервера"; return; }
        botError="";
        String nick=botNick.trim();
        for(int i=0;i<botCount;i++) {
            String name=nick.isEmpty() ? AltAccounts.randomName() : botCount>1 ? (nick.length()>13 ? nick.substring(0,13) : nick)+"_"+(i+1) : nick;
            Bots.add(name,address);
        }
        selectedBot=Bots.all().getLast();
        botNick=""; botField=null;
        UiSounds.toggle(true);
    }

    private float botCard(GuiGraphicsExtractor g,Bot bot,float x,float y,float shift,float appear) {
        float h=BOT_CARD_H;
        y+=shift;
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP-8) return h;
        float saved=alpha; alpha*=appear;
        try {
            var font=MsdfFont.UI;
            boolean over=over(x,y,COL_W,h),selected=bot==selectedBot;
            card(g,x,y,COL_W,h,Math.max(anim(bot,HOVER,over?1:0,12),selected?1:0),selected?1:0);
            hit(x,y,COL_W,h,()->{ if(selectedBot!=bot) { selectedBot=bot; UiSounds.select(); } },null,null);
            float mid=y+CP+HEAD_H/2;
            UiRenderer.rounded(g,x+CP,mid-5.5f,11,11,3,c(bot.color));
            if(bot.main) Icons.draw(g,Icons.Icon.STAR,x+CP+2,mid-3.5f,7,2.6f,c(0xff3a2a08));
            boolean online=bot.state()==Bot.State.ONLINE;
            float nx=x+CP+16;
            nx+=font.mid(g,bot.name,nx,mid,TEXT,c(online ? Theme.text() : Theme.secondary()))+5;
            if(bot.main) nx+=tag(g,"Главный",nx,mid,0xfff2d14b)+3;
            if(bot.foreground()) tag(g,"Ты здесь",nx,mid,Theme.accent());
            float tx=x+COL_W-CP-20;
            if(!bot.main && !bot.foreground()) {
                boolean hv=over(tx-2,mid-7,24,14);
                toggle(g,List.of(bot,"online"),tx,mid-5.5f,20,11,bot.state()!=Bot.State.OFFLINE,hv);
                hit(tx-2,mid-7,24,14,()->{ Bots.toggleConnection(bot); UiSounds.toggle(bot.state()!=Bot.State.OFFLINE); },null,null);
            }
            if(online && bot.ping()>0) {
                String ping=bot.ping()+" мс";
                float pw=font.width(ping,6.5f)+8,px=(bot.main || bot.foreground() ? x+COL_W-CP : tx-5)-pw;
                UiRenderer.rounded(g,px,mid-5,pw,10,4,c(Theme.field()));
                font.midCenter(g,ping,px+pw/2,mid,6.5f,c(Theme.hint()));
            }
            String state=switch (bot.state()) { case ONLINE -> bot.foreground() ? "Играешь" : bot.main ? "В фоне" : "В сети"; case CONNECTING -> "Подключается"; case OFFLINE -> "Не в сети"; };
            int dot=switch (bot.state()) { case ONLINE -> 0xff4fd18b; case CONNECTING -> 0xffe49a3a; case OFFLINE -> 0xffff6b5e; };
            float sy=y+CP+HEAD_H+3;
            UiRenderer.rounded(g,x+CP+1,sy-1.6f,3.2f,3.2f,1.6f,c(dot));
            String sub=state+" · "+bot.address;
            if(bot.state()==Bot.State.OFFLINE && !bot.reason().isEmpty()) sub=bot.reason();
            float[] clip=pushClip(g,x+CP+6,sy-6,x+COL_W-CP,sy+6);
            try { font.mid(g,sub,x+CP+7,sy,6.5f,c(Theme.hint())); } finally { popClip(g,clip); }
        } finally { alpha=saved; }
        return h;
    }
    private float tag(GuiGraphicsExtractor g,String text,float x,float mid,int color) {
        float w=MsdfFont.UI.width(text,6)+8;
        UiRenderer.rounded(g,x,mid-5,w,10,4,c(Theme.withAlpha(color,.18f)));
        MsdfFont.UI.midCenter(g,text,x+w/2,mid,6,c(color));
        return w;
    }

    private float botDetail(GuiGraphicsExtractor g,Bot bot,float x,float y,float shift,float appear) {
        boolean live=bot.foreground(),online=bot.state()==Bot.State.ONLINE;
        float viewH=live ? 0 : Bots.VIEW_H+6;
        float h=CP*2+HEAD_H+6+viewH+ROW_H*3+4+LOG_H+CHAT_GAP+FIELD_H+CHAT_GAP+CHIP_H+6+BUTTON_H;
        y+=shift;
        if(y>VIEW_BOTTOM || y+h<VIEW_TOP-8) return h;
        float saved=alpha; alpha*=appear;
        try {
            var font=MsdfFont.UI;
            card(g,x,y,COL_W,h,1,1);
            float mid=y+CP+HEAD_H/2;
            UiRenderer.rounded(g,x+CP,mid-5.5f,11,11,3,c(bot.color));
            font.mid(g,bot.name,x+CP+16,mid,9,c(Theme.text()));
            if(!live) {
                boolean can=Bots.canSwitch(bot);
                String label=bot.main ? "Вернуться" : "Играть";
                float bw=font.width(label,SMALL)+22;
                botButton(g,"bot-play",label,Icons.Icon.PLAY,x+COL_W-CP-bw,mid-BUTTON_H/2,bw,can,()->{ if(Bots.canSwitch(bot)) { Bots.switchTo(bot); onClose(); } });
            } else tag(g,"Ты играешь за него",x+COL_W-CP-font.width("Ты играешь за него",6)-8,mid,Theme.accent());
            float cy=y+CP+HEAD_H+6;
            if(!live) {
                var view=Bots.view(bot);
                UiRenderer.rounded(g,x+CP-.5f,cy-.5f,INNER+1,Bots.VIEW_H+1,4,c(Theme.field()));
                if(online) g.blit(view.id(),(int)(x+CP),(int)cy,(int)(x+CP)+Bots.VIEW_W,(int)cy+Bots.VIEW_H,view.u0(),view.u1(),view.v0(),view.v1());
                else font.midCenter(g,"Нет сигнала",x+CP+INNER/2,cy+Bots.VIEW_H/2,SMALL,c(Theme.hint()));
                if(online) {
                    float tw=font.width(bot.position(),6)+8;
                    UiRenderer.rounded(g,x+CP+4,cy+4,tw,10,3,c(Theme.withAlpha(0xff000000,.45f)));
                    font.mid(g,bot.position(),x+CP+8,cy+9,6,c(0xffffffff));
                    // Crosshair in the middle of the view.
                    float mx=x+CP+INNER/2,my=cy+Bots.VIEW_H/2;
                    UiRenderer.rounded(g,mx-3,my-.4f,6,.8f,.4f,c(0xddffffff));
                    UiRenderer.rounded(g,mx-.4f,my-3,.8f,6,.4f,c(0xddffffff));
                }
                cy+=viewH;
            }
            String[][] rows={{"Здоровье",online ? Math.round(bot.health())+" / 20" : "—"},{"Голод",online ? bot.food()+" / 20" : "—"},
                    {"Координаты",bot.position()+(bot.dimension().isEmpty() ? "" : " · "+bot.dimension())}};
            for(String[] row:rows) {
                font.mid(g,row[0],x+CP,cy+ROW_H/2,SMALL,c(Theme.secondary()));
                float vw=font.width(row[1],SMALL);
                font.mid(g,row[1],x+CP+INNER-vw,cy+ROW_H/2,SMALL,c(Theme.text()));
                cy+=ROW_H;
            }
            cy+=4;
            UiRenderer.rounded(g,x+CP,cy,INNER,LOG_H,fieldRadius(),p(mix(Theme.background(),Theme.field(),.4f)));
            float[] clip=pushClip(g,x+CP+4,cy+3,x+CP+INNER-4,cy+LOG_H-3);
            try {
                var log=bot.log();
                float ly=cy+LOG_H-7;
                for(int i=log.size()-1;i>=0 && ly>cy;i--) {
                    String line=log.get(i);
                    int color=line.startsWith(">") ? Theme.accent() : line.startsWith("Отключён") ? 0xffff6b5e : Theme.secondary();
                    font.mid(g,line,x+CP+5,ly,6.5f,c(color));
                    ly-=8;
                }
            } finally { popClip(g,clip); }
            cy+=LOG_H+CHAT_GAP;
            botTextField(g,"chat",botChat,"Чат, /команда, .ess или #baritone",x+CP,cy,INNER-FIELD_H-4);
            botButton(g,"bot-send",null,Icons.Icon.CHEVRON_RIGHT,x+CP+INNER-FIELD_H,cy,FIELD_H,true,()->sendBotChat(bot));
            cy+=FIELD_H+CHAT_GAP;
            // Four actions share the row evenly.
            float cw=(INNER-3*3)/4,chx=x+CP;
            boolean act=online && !live;
            botChip(g,bot,"Ко мне",Icons.Icon.MOVE,chx,cy,cw,act,bot.action()==Bot.Action.FOLLOW,()->bot.toggle(Bot.Action.FOLLOW)); chx+=cw+3;
            botChip(g,bot,"Повтор",Icons.Icon.USER,chx,cy,cw,act,bot.action()==Bot.Action.MIRROR,()->bot.toggle(Bot.Action.MIRROR)); chx+=cw+3;
            botChip(g,bot,"Атака",Icons.Icon.SWORD,chx,cy,cw,act,false,bot::attack); chx+=cw+3;
            botChip(g,bot,"Прыжок",Icons.Icon.CHEVRON_DOWN,chx,cy,cw,act,false,bot::jump);
            cy+=CHIP_H+6;
            if(!bot.main) {
                float bw=(INNER-4)/2;
                botButton(g,"bot-reconnect","Переподключить",Icons.Icon.SIGNAL,x+CP,cy,bw,!live,()->Bots.reconnect(bot));
                botButton(g,"bot-delete","Удалить",Icons.Icon.TRASH,x+CP+bw+4,cy,bw,!live,()->{ Bots.remove(bot); selectedBot=null; });
            } else font.mid(g,live ? "Главный аккаунт: у ботов в мире своя метка" : "Главный аккаунт ждёт в фоне",x+CP,cy+BUTTON_H/2,6.5f,c(Theme.hint()));
        } finally { alpha=saved; }
        return h;
    }
    private void sendBotChat(Bot bot) {
        if(botChat.isBlank()) return;
        bot.chat(botChat.trim());
        botChat="";
        UiSounds.select();
    }
    private void botChip(GuiGraphicsExtractor g,Bot bot,String label,Icons.Icon icon,float x,float y,float w,boolean enabled,boolean on,Runnable action) {
        var font=MsdfFont.UI;
        Object key=List.of(bot,label);
        float hover=anim(key,HOVER,enabled && over(x,y,w,CHIP_H)?1:0,16),v=anim(key,VALUE,on?1:0,14),press=anim(key,PRESS,0,6);
        int fill=mix(mix(Theme.field(),Theme.text(),.07f*hover),Theme.accent(),.22f*v+.3f*press);
        UiRenderer.rounded(g,x,y,w,CHIP_H,5,p(fill));
        int color=!enabled ? Theme.hint() : mix(mix(Theme.secondary(),Theme.text(),hover),Theme.accent(),v);
        float content=font.width(label,6.5f)+9,cx=x+w/2-content/2;
        Icons.draw(g,icon,cx,y+CHIP_H/2-3,6,2.4f,c(color));
        font.mid(g,label,cx+9,y+CHIP_H/2,6.5f,c(color));
        if(enabled) hit(x,y,w,CHIP_H,()->{ kick(key,PRESS,1); UiSounds.select(); action.run(); },null,null);
    }
    private void botButton(GuiGraphicsExtractor g,Object key,String label,Icons.Icon icon,float x,float y,float w,boolean enabled,Runnable action) {
        boolean primary="bot-connect".equals(key) || "bot-play".equals(key);
        float hover=anim(key,HOVER,enabled && over(x,y,w,BUTTON_H)?1:0,14),press=anim(key,PRESS,0,5);
        int fill=primary ? mix(Theme.accent(),Theme.accentLight(),.18f*hover) : mix(Theme.field(),Theme.text(),.07f*hover);
        fill=mix(fill,primary ? 0xffffffff : Theme.accent(),.35f*press);
        if(!enabled) fill=Theme.withAlpha(fill,.45f);
        UiRenderer.rounded(g,x,y,w,BUTTON_H,fieldRadius(),primary ? c(fill) : p(fill));
        int text=primary ? Theme.onAccent() : "bot-delete".equals(key) ? 0xffff6b5e : Theme.text();
        if(!enabled) text=Theme.withAlpha(text,.5f);
        var font=MsdfFont.UI;
        if(label==null) Icons.draw(g,icon,x+w/2-4,y+BUTTON_H/2-4,8,2.6f,c(text));
        else {
            float tw=font.width(label,SMALL),total=tw+11;
            Icons.draw(g,icon,x+w/2-total/2,y+BUTTON_H/2-3.5f,7,2.6f,c(text));
            font.mid(g,label,x+w/2-total/2+11,y+BUTTON_H/2,SMALL,c(text));
        }
        if(enabled) hit(x,y,w,BUTTON_H,()->{ kick(key,PRESS,1); UiSounds.select(); action.run(); },null,null);
    }
    /** Field like the search capsule: placeholder, caret while focused, the text slides left when it is long. */
    private void botTextField(GuiGraphicsExtractor g,String field,String text,String placeholder,float x,float y,float w) {
        var font=MsdfFont.UI;
        boolean focused=field.equals(botField);
        float hover=anim(List.of("bot-field",field),HOVER,over(x,y,w,FIELD_H)?1:0,14),focus=anim(List.of("bot-field",field),VALUE,focused?1:0,16);
        framed(g,x,y,w,FIELD_H,fieldRadius(),mix(mix(Theme.field(),Theme.text(),.08f),Theme.accent(),Math.max(.6f*hover,focus)),Theme.field());
        float tx=x+7,room=w-14;
        float[] clip=pushClip(g,tx-1,y,tx+room,y+FIELD_H);
        try {
            if(text.isEmpty()) font.mid(g,placeholder,tx,y+FIELD_H/2,SMALL,c(Theme.hint()));
            float tw=font.width(text,SMALL),shiftX=Math.max(0,tw+2-room);
            if(!text.isEmpty()) ess.ll.client.module.render.TypingAnimation.mid(g,font,"gui:"+field,text,tx-shiftX,y+FIELD_H/2,SMALL,c(Theme.text()));
            if(focused) {
                float blink=(float)(.5+.5*Math.cos((System.nanoTime()-caretAt)/1e9*Math.PI*2.2));
                UiRenderer.rounded(g,tx-shiftX+tw+.6f,y+3.5f,.9f,FIELD_H-7,.45f,c(Theme.withAlpha(Theme.accent(),blink)));
            }
        } finally { popClip(g,clip); }
        hit(x,y,w,FIELD_H,()->{ if(!field.equals(botField)) { botField=field; searchFocused=false; caretAt=System.nanoTime(); UiSounds.select(); } },null,null);
    }
    /** Typing into the focused bot field; the nick keeps to Minecraft's name rules. */
    private void typeBot(String typed) {
        switch (botField) {
            case "nick" -> {
                StringBuilder b=new StringBuilder(botNick);
                typed.chars().filter(ch->ch<128 && (Character.isLetterOrDigit(ch) || ch=='_')).forEach(ch->b.append((char)ch));
                botNick=b.length()>16 ? b.substring(0,16) : b.toString();
            }
            case "address" -> {
                StringBuilder b=new StringBuilder(botAddress);
                typed.chars().filter(ch->ch>32 && ch<127).forEach(ch->b.append((char)ch));
                botAddress=b.length()>64 ? b.substring(0,64) : b.toString();
                botError="";
            }
            default -> {
                String next=botChat+typed.replace('\n',' ');
                botChat=next.length()>256 ? next.substring(0,256) : next;
            }
        }
    }
    private boolean botKey(KeyEvent event) {
        if(botField==null) return false;
        int key=event.key();
        if(key==InputConstants.KEY_ESCAPE) { botField=null; return true; }
        if(key==InputConstants.KEY_RETURN || key==InputConstants.KEY_NUMPADENTER) {
            if(botField.equals("chat")) { if(selectedBot!=null) sendBotChat(selectedBot); }
            else connectBots();
            return true;
        }
        if(key==InputConstants.KEY_TAB) { botField=botField.equals("nick") ? "address" : botField.equals("address") ? "nick" : "chat"; return true; }
        if(key==InputConstants.KEY_BACKSPACE) {
            String text=switch (botField) { case "nick" -> botNick; case "address" -> botAddress; default -> botChat; };
            text=event.hasControlDown() || text.length()<=1 ? "" : text.substring(0,text.length()-1);
            switch (botField) { case "nick" -> botNick=text; case "address" -> botAddress=text; default -> botChat=text; }
            return true;
        }
        if(event.isPaste()) { typeBot(minecraft.keyboardHandler.getClipboard()); return true; }
        // Other keys stay in the field so module binds do not fire while typing; Right Shift still closes.
        return key!=InputConstants.KEY_RSHIFT;
    }

    // ---------------------------------------------------------------- input

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(closing) return true;
        int button=event.button();
        if(listening!=null) {
            if(button!=InputConstants.MOUSE_BUTTON_LEFT && button!=InputConstants.MOUSE_BUTTON_RIGHT) { bindMouse(button); return true; }
            listening=null; // left / right cancel waiting; the click still goes through
        }
        float x=(float)((event.x()-left)/scale),y=(float)((event.y()-top)/scale);
        // A click anywhere drops the bot field focus; a click on a field takes it back in its hit.
        botField=null;
        if(searchFocused && !(x>=searchX && x<searchX+searchW && y>=searchY && y<searchY+18)) searchFocused=false;
        if(hexFocused && !inPicker(x,y)) commitHex();
        if(picker!=null && !inPicker(x,y)) { closePicker(); return true; }
        boolean onProfiles=x>=PROFILE_X && x<PROFILE_X+PROFILE_W && y>=PROFILE_Y && y<PROFILE_Y+PROFILE_H || inProfiles(x,y);
        if(profilesOpen && !onProfiles) { profilesOpen=false; UiSounds.expand(false); return true; }
        if(popupTarget!=null && !inPopup(x,y)) { closePopup(); return true; }
        for(int i=hits.size()-1;i>=0;i--) {
            Hit h=hits.get(i);
            if(x<h.x() || x>=h.x()+h.w() || y<h.y() || y>=h.y()+h.h()) continue;
            if(button==InputConstants.MOUSE_BUTTON_LEFT) {
                if(h.drag()!=null) { dragging=h.drag(); dragging.apply(x,y); }
                else if(h.left()!=null) h.left().run();
            } else if(button==InputConstants.MOUSE_BUTTON_RIGHT && h.right()!=null) h.right().run();
            else if(button==InputConstants.MOUSE_BUTTON_MIDDLE && h.middle()!=null) h.middle().run();
            return true;
        }
        if(button==InputConstants.MOUSE_BUTTON_LEFT && inPreview(x,y)) { rotating=true; return true; }
        // The top of the sidebar (mark and name) drags the whole GUI.
        if(button==InputConstants.MOUSE_BUTTON_LEFT && x>=SIDE_X && x<SIDE_X+SIDE_W-28 && y>=0 && y<30) {
            movingWindow=true; grabX=(float)event.x()-offsetX; grabY=(float)event.y()-offsetY;
            return true;
        }
        return x>=0 && x<W && y>=0 && y<H || super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(closing) return true;
        if(dragging!=null) { dragging.apply((float)((event.x()-left)/scale),(float)((event.y()-top)/scale)); return true; }
        if(movingWindow) { offsetX=(float)event.x()-grabX; offsetY=(float)event.y()-grabY; return true; }
        if(rotating) { userYaw+=(float)(dx/scale)*1.6f; return true; }
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(dragging!=null || movingWindow || rotating) { dragging=null; movingWindow=false; rotating=false; return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) {
        if(closing) return true;
        float x=(float)((mouseX-left)/scale),y=(float)((mouseY-top)/scale);
        if(inOverlay(x,y)) return true;
        if(inPreview(x,y)) { userZoom=Math.clamp(userZoom*(1+(float)vertical*.12f),.6f,2.6f); return true; }
        if(!searching() && category.equals(THEMES) && x>=railX && x<railX+railW && y>=railY && y<railY+PRESET_H && y>=VIEW_TOP && y<VIEW_BOTTOM) {
            railTarget=Math.clamp(railTarget-(float)(vertical+horizontal)*40,0,railMax);
            return true;
        }
        float[] sc=scrolls.get(contentKey());
        if(x>=RAIL && x<W && y>=VIEW_TOP && y<VIEW_BOTTOM) {
            sc[1]=Math.clamp(sc[1]-(float)vertical*30,0,sc[2]);
            return true;
        }
        return false;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(listening!=null) { bindKey(event.key()); return true; }
        int key=event.key();
        if(hexFocused) {
            if(key==InputConstants.KEY_ESCAPE) hexFocused=false;
            else if(key==InputConstants.KEY_RETURN || key==InputConstants.KEY_NUMPADENTER) commitHex();
            else if(key==InputConstants.KEY_BACKSPACE && !hexText.isEmpty()) hexText=hexText.substring(0,hexText.length()-1);
            else if(event.isPaste()) typeHex(minecraft.keyboardHandler.getClipboard());
            if(key!=InputConstants.KEY_RSHIFT) return true;
        }
        if(picker!=null && key==InputConstants.KEY_ESCAPE) { closePicker(); return true; }
        if(botKey(event)) return true;
        if(key==InputConstants.KEY_F && event.hasControlDown()) {
            searchFocused=true; caretAt=System.nanoTime(); popupTarget=null; profilesOpen=false;
            return true;
        }
        if(searchFocused) {
            if(key==InputConstants.KEY_ESCAPE) { if(searching()) setQuery(""); else searchFocused=false; return true; }
            if(key==InputConstants.KEY_RETURN || key==InputConstants.KEY_NUMPADENTER) { searchFocused=false; return true; }
            if(key==InputConstants.KEY_BACKSPACE) {
                if(event.hasControlDown() || query.length()<=1) setQuery("");
                else setQuery(query.substring(0,query.offsetByCodePoints(query.length(),-1)));
                return true;
            }
            if(event.isPaste()) { type(minecraft.keyboardHandler.getClipboard()); return true; }
            // Other keys stay in the field so module binds do not fire while typing; Right Shift still closes.
            if(key!=InputConstants.KEY_RSHIFT) return true;
        }
        if(searching() && key==InputConstants.KEY_ESCAPE) { setQuery(""); return true; }
        if(popupTarget!=null && event.key()==InputConstants.KEY_ESCAPE) { closePopup(); return true; }
        if(event.key()==InputConstants.KEY_RSHIFT || event.key()==ClientModules.CLICK_GUI.bind) { onClose(); return true; }
        return super.keyPressed(event);
    }
    /** Printable characters go to the search field while it is focused (click it or Ctrl+F). */
    @Override public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if(closing || listening!=null) return true;
        if(botField!=null) { typeBot(event.codepointAsString()); return true; }
        if(hexFocused) { typeHex(event.codepointAsString()); return true; }
        // Only a focused field takes text; otherwise keys pressed in the menu would spill into the search.
        if(!searchFocused || popupTarget!=null || profilesOpen) return false;
        type(event.codepointAsString());
        return true;
    }
    private void typeHex(String text) {
        for(char ch:text.toCharArray())
            if(Character.digit(ch,16)>=0 && hexText.length()<(picker!=null && picker.alpha ? 8 : 6)) hexText+=Character.toUpperCase(ch);
        caretAt=System.nanoTime();
    }
    private void type(String text) {
        StringBuilder accepted=new StringBuilder();
        text.codePoints().filter(cp->Character.isLetterOrDigit(cp) || cp==' ' || cp=='-' || cp=='_' || cp=='.').forEach(accepted::appendCodePoint);
        if(accepted.isEmpty() || accepted.toString().isBlank() && query.isEmpty()) return;
        if(!searchFocused) { searchFocused=true; popupTarget=null; }
        String next=query+accepted;
        if(next.codePointCount(0,next.length())>24) next=next.substring(0,next.offsetByCodePoints(0,24));
        setQuery(next);
    }
    /** Escape cancels, Delete / Backspace unbind. */
    private void bindKey(int key) {
        Object target=listening; listening=null;
        if(key==InputConstants.KEY_ESCAPE) return;
        int code=key==InputConstants.KEY_DELETE || key==InputConstants.KEY_BACKSPACE || !Keys.isValid(key) ? -1 : key;
        if(target instanceof Module m) m.bind=code;
        else if(target instanceof KeybindSetting bind) bind.set(code);
        UiSounds.bindSet();
    }
    /** Mouse buttons other than left / right become binds (MOUSE3+). */
    private void bindMouse(int button) {
        Object target=listening; listening=null;
        if(button<InputConstants.MOUSE_BUTTON_MIDDLE || button>Keys.MOUSE_BUTTONS) return;
        if(target instanceof Module m) m.bind=Keys.MOUSE_BASE+button;
        else if(target instanceof KeybindSetting bind) bind.set(Keys.MOUSE_BASE+button);
        UiSounds.bindSet();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {}
}
