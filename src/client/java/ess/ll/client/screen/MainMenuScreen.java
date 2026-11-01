package ess.ll.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.media.Lyrics;
import ess.ll.client.media.MediaBridge;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import ess.ll.client.mixin.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main menu in the style of the ESS site: a glass navigation bar with a sliding pill, the hero with the
 * last server and tiles, and the worlds, servers, Realms and settings pages drawn inside the menu itself.
 * Everything is laid out in the units of the 680x382 site mockup and scaled to the window.
 */
public final class MainMenuScreen extends Screen {
    private static final float DESIGN_W=680, DESIGN_H=382.5f;
    private static final ExecutorService PINGS=Executors.newFixedThreadPool(4,r->{ var t=new Thread(r,"ESS menu ping"); t.setDaemon(true); return t; });
    // Site palette as {dark, light}.
    private static final int[] BG={0xff0a0a0a,0xfff3f3f1}, FG={0xfff4f4f4,0xff0b0b0b}, MUT={0xff7a7a7a,0xff85857f}, LINE={0x17ffffff,0x17000000},
            CARD={0xb8161616,0xb3ffffff}, SOLID={0xff121212,0xffffffff}, SEG={0xff1f1f1f,0xffe4e4e0};

    private enum Page { HOME, SINGLE, MULTI, OPTIONS, SERVER_FORM, WORLD_FORM, ACCOUNTS }
    /** One line of the settings page: a vanilla option, a key bind, a language, a skin part, a header or a custom switch. */
    private record Setting(String title,OptionInstance<?> option,KeyMapping key,String language,PlayerModelPart part,Runnable custom,java.util.function.BooleanSupplier state) {
        static Setting header(String title) { return new Setting(title,null,null,null,null,null,null); }
        static Setting of(OptionInstance<?> option) { return new Setting(null,option,null,null,null,null,null); }
        boolean isHeader() { return option==null && key==null && language==null && part==null && custom==null; }
    }
    private record Category(String name,Icons.Icon icon,java.util.function.Supplier<List<Setting>> settings) {}
    /** Clickable area registered while drawing; clicks are tested against the previous frame's list. */
    private record Hot(float x,float y,float w,float h,Runnable click,Runnable twice) {}
    /** Time-based transition between two values with the site's easing curves. */
    private static final class Tween {
        float from, to; double start=-10, length; boolean inOut;
        Tween(float value,double length,boolean inOut) { from=to=value; this.length=length; this.inOut=inOut; }
        void set(float target,double now) { if(target==to) return; from=get(now); to=target; start=now; }
        void jump(float value) { from=to=value; start=-10; }
        float get(double now) {
            double t=Math.clamp((now-start)/length,0,1);
            // cubic-bezier(.65,0,.35,1) is close to ease-in-out cubic; (.2,.8,.2,1) to ease-out quart.
            double e=inOut ? (t<.5 ? 4*t*t*t : 1-Math.pow(-2*t+2,3)/2) : 1-Math.pow(1-t,4);
            return (float)(from+(to-from)*e);
        }
    }

    private final long openedAt=System.nanoTime();
    private long lastFrame=System.nanoTime();
    private double dt;
    private float s=1, W=DESIGN_W, H=DESIGN_H, mx, my, fade=1, lt;
    private float[] clip;
    private Page page=Page.HOME;
    private double pageAt;
    private final Map<String,Float> hovers=new HashMap<>();
    private final Map<String,Float> presses=new HashMap<>();
    private List<Hot> hots=new ArrayList<>(), drawing=new ArrayList<>();
    private String toast="";
    private double toastAt=-10;
    private float scroll, scrollTarget, scrollMax;
    private List<LevelSummary> worlds;
    private String worldsError;
    // init() runs again on resize; a second read while one is running trips over the first one's world locks.
    private boolean loadingWorlds;
    private int selectedWorld;
    private ServerList servers;
    private int selectedServer;
    private final ServerStatusPinger pinger=new ServerStatusPinger();
    private ServerData lastServer;

    // Settings page: selected category, its rows, and the option or key waiting for input.
    private int category;
    private boolean categoryOpen;
    private List<Setting> settings;
    private final Map<OptionInstance<?>,String[]> optionText=new HashMap<>();
    private KeyMapping listening;
    private OptionInstance<?> dragging;
    private float dragX, dragW;
    private double dragValue;
    // Forms: the server being added or edited, and the vanilla create-world screen whose settings the form edits.
    private ServerData formServer, editedServer;
    private CreateWorldScreen createWorld;
    private static MainMenuScreen creatingFor;
    private final Map<String,StringBuilder> fields=new HashMap<>();
    private String focus;
    private long caretAt=System.nanoTime();

    public MainMenuScreen() { super(Component.literal("Maved")); }

    /** Called from MainMenuMixin when the vanilla create-world screen opens: the menu that asked for it takes it. */
    public static Screen claimCreateWorld(CreateWorldScreen screen) {
        var menu=creatingFor;
        if(menu==null) return null;
        creatingFor=null;
        menu.createWorld=screen;
        var state=screen.getUiState();
        menu.fields.put("worldName",new StringBuilder(state.getName()));
        menu.fields.put("seed",new StringBuilder(state.getSeed()));
        menu.focus="worldName";
        menu.page=Page.WORLD_FORM; menu.pageAt=menu.now();
        return menu;
    }

    private double now() { return (System.nanoTime()-openedAt)/1e9; }

    @Override protected void init() {
        if(worlds==null && worldsError==null && !loadingWorlds) loadWorlds();
        if(servers==null) loadServers();
        fields.putIfAbsent("accountName",new StringBuilder());
        // SDL only delivers characters (charTyped) while someone owns text input; vanilla edit boxes take it on
        // focus, and the menu's own fields are drawn rather than widgets, so the menu takes it for itself.
        minecraft.textInputManager().startTextInput(this);
        if(lastServer==null && Prefs.lastType.equals("server")) {
            lastServer=new ServerData(Prefs.lastName,Prefs.lastId,ServerData.Type.OTHER);
            ping(lastServer);
        }
    }

    // ---- Data -------------------------------------------------------------------------------------------------

    private void loadWorlds() {
        worlds=null; worldsError=null;
        // Icons may have changed since (a new screenshot after playing), so they load again.
        worldIcons.values().forEach(texture->{ if(texture!=null) texture.close(); });
        worldIcons.clear();
        try {
            var source=minecraft.getLevelSource();
            loadingWorlds=true;
            source.loadLevelSummaries(source.findLevelCandidates()).whenComplete((list,error)->minecraft.execute(()->{
                loadingWorlds=false;
                if(error!=null) { worldsError="Не удалось прочитать миры"; worlds=List.of(); return; }
                var sorted=new ArrayList<>(list);
                Collections.sort(sorted);
                worlds=sorted;
                selectedWorld=Math.clamp(selectedWorld,0,Math.max(0,sorted.size()-1));
            }));
        } catch(Exception e) { loadingWorlds=false; worldsError="Не удалось прочитать миры"; worlds=List.of(); }
    }
    private void loadServers() {
        pinger.removeAll();
        servers=new ServerList(minecraft);
        servers.load();
        selectedServer=Math.clamp(selectedServer,0,Math.max(0,servers.size()-1));
        for(int i=0;i<servers.size();i++) ping(servers.get(i));
        if(lastServer!=null) { lastServer.setState(ServerData.State.INITIAL); ping(lastServer); }
    }
    /** Same flow as the vanilla server list: ping off-thread, results arrive through the pinger's tick. */
    private void ping(ServerData data) {
        if(data.state()!=ServerData.State.INITIAL) return;
        data.setState(ServerData.State.PINGING);
        data.motd=CommonComponents.EMPTY;
        data.status=CommonComponents.EMPTY;
        PINGS.submit(()->{
            try {
                pinger.pingServer(data,()->minecraft.execute(()->{ if(servers!=null) servers.save(); }),
                        ()->data.setState(ServerData.State.SUCCESSFUL),EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
            } catch(UnknownHostException e) {
                data.setState(ServerData.State.UNREACHABLE);
            } catch(Exception e) {
                data.setState(ServerData.State.UNREACHABLE);
            }
        });
    }

    // ---- Actions ----------------------------------------------------------------------------------------------

    private void go(Page target) {
        if(target==page) return;
        UiSounds.select();
        if(page==Page.WORLD_FORM && createWorld!=null) {
            // Leaving the world form through the navigation discards the prepared world.
            var screen=createWorld;
            createWorld=null;
            screen.popScreen();
        }
        if(page==Page.SERVER_FORM) formServer=editedServer=null;
        listening=null; focus=null; dragging=null; categoryOpen=false; settings=null;
        page=target; pageAt=now();
        scroll=scrollTarget=0;
        if(target==Page.MULTI) for(int i=0;i<servers.size();i++) if(servers.get(i).state()==ServerData.State.UNREACHABLE) {
            servers.get(i).setState(ServerData.State.INITIAL); ping(servers.get(i));
        }
    }
    private void showToast(String text) { toast=text; toastAt=now(); }
    private void back() { minecraft.gui.setScreen(this); }

    private void playWorld(LevelSummary world) {
        if(world.isDisabled() || !world.primaryActionActive()) { showToast(world.getInfo().getString()); return; }
        Prefs.last("world",world.getLevelId(),world.getLevelName());
        // Old, locked or downgraded worlds need the vanilla prompts (backup, conversion), so they go through its list.
        if(world.requiresManualConversion() || world.shouldBackup() || world.isDowngrade() || world.isLocked()) {
            minecraft.gui.setScreen(new SelectWorldScreen(this));
            return;
        }
        minecraft.createWorldOpenFlows().openWorld(world.getLevelId(),this::back);
    }
    /** Vanilla prepares the world settings (data packs, presets); MainMenuMixin then hands its screen back to the form. */
    private void createWorld() {
        creatingFor=this;
        CreateWorldScreen.openFresh(minecraft,()->{ createWorld=null; page=Page.SINGLE; back(); });
    }
    private void finishWorld() {
        var state=createWorld.getUiState();
        String name=fields.get("worldName").toString().trim();
        if(name.isEmpty()) { showToast("Введи название мира"); return; }
        state.setName(name);
        state.setSeed(fields.get("seed").toString());
        Prefs.last("world",state.getTargetFolder(),name);
        ((ess.ll.client.mixin.MenuCreateWorldAccessor)createWorld).ess$create();
    }
    private void cancelWorld() {
        var screen=createWorld;
        createWorld=null;
        page=Page.SINGLE; pageAt=now();
        // popScreen removes the temporary data pack folder and runs the close callback, which returns here.
        if(screen!=null) screen.popScreen();
    }
    /** Game rules and data packs stay on the vanilla screen, which keeps the form's choices. */
    private void advancedWorld() {
        var state=createWorld.getUiState();
        state.setName(fields.get("worldName").toString());
        state.setSeed(fields.get("seed").toString());
        var screen=createWorld;
        createWorld=null;
        page=Page.SINGLE;
        minecraft.gui.setScreen(screen);
    }
    private void editWorld(LevelSummary world) {
        try {
            var access=minecraft.getLevelSource().validateAndCreateAccess(world.getLevelId());
            minecraft.gui.setScreen(EditWorldScreen.create(minecraft,access,changed->{ access.safeClose(); loadWorlds(); back(); }));
        } catch(Exception e) { showToast("Мир сейчас недоступен"); }
    }
    private void deleteWorld(LevelSummary world) {
        minecraft.gui.setScreen(new ConfirmScreen(ok->{
            if(ok) try(var access=minecraft.getLevelSource().createAccess(world.getLevelId())) { access.deleteLevel(); }
            catch(IOException e) { showToast("Не удалось удалить мир"); }
            loadWorlds();
            back();
        },Component.translatable("selectWorld.deleteQuestion"),Component.translatable("selectWorld.deleteWarning",world.getLevelName()),
                Component.translatable("selectWorld.deleteButton"),CommonComponents.GUI_CANCEL));
    }

    private void join(ServerData data) {
        Prefs.last("server",data.ip,data.name);
        lastServer=data;
        ConnectScreen.startConnecting(this,minecraft,ServerAddress.parseString(data.ip),data,false,null);
    }
    private void addServer() { openServerForm(null); }
    private void editServer(ServerData data) { openServerForm(data); }
    private void openServerForm(ServerData original) {
        editedServer=original;
        formServer=new ServerData(original==null ? I18n.get("selectServer.defaultName") : original.name,original==null ? "" : original.ip,ServerData.Type.OTHER);
        if(original!=null) formServer.copyFrom(original);
        fields.put("serverName",new StringBuilder(formServer.name));
        fields.put("serverIp",new StringBuilder(formServer.ip));
        focus=original==null ? "serverIp" : "serverName";
        page=Page.SERVER_FORM; pageAt=now();
        UiSounds.select();
    }
    private void saveServer() {
        String name=fields.get("serverName").toString().trim(), ip=fields.get("serverIp").toString().trim();
        if(!ServerAddress.isValidAddress(ip)) { showToast("Проверь адрес сервера"); return; }
        formServer.name=name.isEmpty() ? I18n.get("selectServer.defaultName") : name;
        formServer.ip=ip;
        if(editedServer!=null) editedServer.copyFrom(formServer);
        else {
            var hidden=servers.unhide(ip);
            if(hidden!=null) { hidden.copyNameIconFrom(formServer); hidden.setResourcePackStatus(formServer.getResourcePackStatus()); }
            else servers.add(formServer,false);
        }
        servers.save();
        loadServers();
        if(editedServer==null) selectedServer=servers.size()-1;
        formServer=editedServer=null;
        go(Page.MULTI);
    }
    private void deleteServer(ServerData data) {
        minecraft.gui.setScreen(new ConfirmScreen(ok->{
            if(ok) { servers.remove(data); servers.save(); loadServers(); }
            back();
        },Component.translatable("selectServer.deleteQuestion"),Component.translatable("selectServer.deleteWarning",data.name),
                Component.translatable("selectServer.deleteButton"),CommonComponents.GUI_CANCEL));
    }
    private void directConnect() {
        var data=new ServerData(I18n.get("selectServer.defaultName"),"",ServerData.Type.OTHER);
        minecraft.gui.setScreen(new DirectJoinServerScreen(this,ok->{ if(ok) join(data); else back(); },data));
    }
    private void continueLast() {
        if(Prefs.lastType.equals("server") && lastServer!=null) { join(lastServer); return; }
        if(Prefs.lastType.equals("world") && worlds!=null)
            for(var world:worlds) if(world.getLevelId().equals(Prefs.lastId)) { playWorld(world); return; }
        go(Page.SINGLE);
    }

    // ---- Drawing primitives -----------------------------------------------------------------------------------

    private int col(int[] pair) { return a(Theme.mix(pair[0],pair[1],lt)); }
    /** Applies the current fade to a colour's own alpha. */
    private int a(int color) { return Theme.withAlpha(color,(color>>>24)/255f*fade); }
    private float ease(String id,boolean on,float speed) {
        float value=hovers.getOrDefault(id,0f);
        value+=((on?1:0)-value)*(float)(1-Math.exp(-dt*speed));
        hovers.put(id,value);
        return value;
    }
    private boolean over(float x,float y,float w,float h) {
        if(clip!=null && (mx<clip[0] || my<clip[1] || mx>=clip[2] || my>=clip[3])) return false;
        return mx>=x && mx<x+w && my>=y && my<y+h;
    }
    /** Eased hover of a rectangle; registers it for clicks when it has an action. */
    private float hot(String id,float x,float y,float w,float h,Runnable click) { return hot(id,x,y,w,h,click,null); }
    private float hot(String id,float x,float y,float w,float h,Runnable click,Runnable twice) {
        boolean on=over(x,y,w,h);
        if(click!=null && (clip==null || y+h>clip[1] && y<clip[3])) {
            float cx=Math.max(x,clip==null ? x : clip[0]), cy=Math.max(y,clip==null ? y : clip[1]);
            float cw=Math.min(x+w,clip==null ? x+w : clip[2])-cx, ch=Math.min(y+h,clip==null ? y+h : clip[3])-cy;
            drawing.add(new Hot(cx,cy,cw,ch,()->{ presses.put(id,1f); click.run(); },twice));
        }
        return ease(id,on,12);
    }
    private float press(String id) {
        float value=Math.max(0,presses.getOrDefault(id,0f)-(float)dt*5);
        presses.put(id,value);
        return value;
    }
    /** Text with letter spacing (labels and the brand), drawn glyph by glyph; returns the width. */
    private float spaced(MsdfFont font,String text,float x,float midY,float size,float spacing,int color) {
        float pen=x;
        for(int code:text.codePoints().toArray()) {
            String glyph=new String(Character.toChars(code));
            if(color!=0) font.mid(g(),glyph,pen,midY,size,color);
            pen+=font.width(glyph,size)+spacing;
        }
        return pen-x-spacing;
    }
    private float spacedWidth(MsdfFont font,String text,float size,float spacing) {
        int n=text.codePointCount(0,text.length());
        return font.width(text,size)+spacing*Math.max(0,n-1);
    }
    private GuiGraphicsExtractor graphics;
    private GuiGraphicsExtractor g() { return graphics; }
    private void card(float x,float y,float w,float h,float r,int fill,int border) {
        UiRenderer.rounded(g(),x,y,w,h,r,fill);
        UiRenderer.outline(g(),x,y,w,h,r,1,border);
    }
    private String fit(MsdfFont font,String text,float size,float max) {
        if(font.width(text,size)<=max) return text;
        while(text.length()>1 && font.width(text+"...",size)>max) text=text.substring(0,text.length()-1);
        return text+"...";
    }

    // ---- Frame ------------------------------------------------------------------------------------------------

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        graphics=g;
        long frame=System.nanoTime(); dt=Math.min(.1,(frame-lastFrame)/1e9); lastFrame=frame;
        double now=now();
        s=scale();
        W=width/s; H=height/s; mx=mouseX/s; my=mouseY/s;
        drawing=new ArrayList<>();
        scroll+=(scrollTarget-scroll)*(float)(1-Math.exp(-dt*16));

        fade=1;
        UiRenderer.rect(g,0,0,width,height,col(BG));
        MenuScene.style=Prefs.background==null ? MenuScene.Style.GRID : Prefs.background;
        MenuScene.light=lt;
        MenuScene.draw(g,width,height,now+20,0,0,(float)Math.clamp(now/1.2,0,1));
        // The shader backdrops are dark already and only need a light veil under the cards.
        if(Prefs.background!=null) UiRenderer.rect(g,0,0,width,height,Theme.withAlpha(Theme.mix(BG[0],BG[1],lt),Prefs.background==MenuScene.Style.MOUNTAINS ? .55f : .25f));
        g.pose().pushMatrix();
        try {
            g.pose().scale(s,s);
            fade=(float)Math.clamp(now/.5,0,1);
            drawPage(now);
            fade=(float)Math.clamp(now/.5,0,1);
            drawPlayer(now);
            drawToast(now);
        } finally { g.pose().popMatrix(); }
        hots=drawing;
    }

    /**
     * GUI units per design unit. Sized by the real window height (1.75 px per unit at 1080p, like the site
     * in a browser) rather than stretched to fill it, and never so large that the mockup no longer fits.
     */
    private float scale() {
        var window=minecraft.getWindow();
        float px=Math.max(1,window.getHeight()/1080f*1.75f);
        float fit=Math.min(width/DESIGN_W,height/DESIGN_H);
        return Math.min(px/(float)window.getGuiScale(),fit);
    }

    private static java.lang.reflect.Constructor<?> accountConstructor;
    private static boolean accountLooked;
    /** IAS is optional and not a build dependency, so its screen is found by name. */
    private java.util.function.Supplier<Screen> accountScreen() {
        if(!accountLooked) {
            accountLooked=true;
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ias")) try {
                accountConstructor=Class.forName("ru.vidtu.ias.screen.AccountScreen").getConstructor(Screen.class);
            } catch(ReflectiveOperationException ignored) {}
        }
        if(accountConstructor==null) return null;
        return ()->{
            try { return (Screen)accountConstructor.newInstance(this); }
            catch(ReflectiveOperationException e) { showToast("Не удалось открыть аккаунты"); return this; }
        };
    }

    /** ViaFabricPlus is optional too: its version picker lets the player join servers of other game versions. */
    private static final String VIA="com.viaversion.viafabricplus.";
    private static Boolean viaLoaded;
    private static java.lang.reflect.Method viaTranslation,viaTarget;
    private static boolean viaLoaded() {
        if(viaLoaded==null) viaLoaded=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("viafabricplus");
        return viaLoaded;
    }
    /** The version ViaFabricPlus will join with, e.g. "1.21.4"; null when it can't be read. */
    private static String viaVersion() {
        try {
            if(viaTarget==null) {
                Class<?> impl=Class.forName(VIA+"ViaFabricPlusImpl");
                viaTranslation=impl.getMethod("protocolTranslation");
                viaTarget=viaTranslation.getReturnType().getMethod("targetVersion");
            }
            Object translation=viaTranslation.invoke(Class.forName(VIA+"ViaFabricPlusImpl").getMethod("impl").invoke(null));
            Object version=viaTarget.invoke(translation);
            return version==null ? null : (String)version.getClass().getMethod("getName").invoke(version);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) { return null; }
    }
    private void openVia() {
        try {
            Object screen=Class.forName(VIA+"screen.impl.ViaFabricPlusScreen").getConstructor().newInstance();
            screen.getClass().getMethod("open",Screen.class).invoke(screen,this);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) { showToast("Не удалось открыть ViaFabricPlus"); }
    }

    private void drawPage(double now) {
        float t=(float)Math.clamp((now-pageAt)/.6,0,1);
        float e=1-(float)Math.pow(1-t,4);
        fade=e*(float)Math.clamp(now/.5,0,1);
        rise=(1-e)*14;
        switch (page) {
            case HOME -> drawHome();
            case SINGLE -> drawWorlds();
            case MULTI -> drawServers();
            case OPTIONS -> drawOptions();
            case SERVER_FORM -> drawServerForm();
            case WORLD_FORM -> drawWorldForm();
            case ACCOUNTS -> drawAccounts();
        }
    }

    /** Home: the name, a greeting, and every way into the game on one plate. */
    private void drawHome() {
        var g=g();
        float content=6*BTN_H+5*GAP, logoH=46;
        float y=(H-(logoH+14+content+2*PAD))/2+rise, cx=W/2;
        spaced(MsdfFont.SITE_BLACK,"MAVED",cx-spacedWidth(MsdfFont.SITE_BLACK,"MAVED",26,6)/2,y+13,26,6,a(Theme.PLATE_TEXT));
        MsdfFont.UI.midCenter(g,"С возвращением, "+minecraft.getUser().getName(),cx,y+36,9,a(Theme.PLATE_HINT));
        float by=plateAt(null,COL_W,y+logoH+14,content);
        String last=Prefs.lastName.isBlank() ? Prefs.lastId : Prefs.lastName;
        String label=Prefs.lastType.isEmpty() || last.isBlank() ? "Продолжить" : "Продолжить · "+shortName(last);
        ServerData server=Prefs.lastType.equals("server") ? lastServer : null;
        var icon=server==null ? null : serverIcon(server);
        by=wide("continue",by,label,Icons.Icon.PLAY,icon==null ? null : (ix,iy,is)->Avatar.image(g,icon,ix,iy,is,is*.22f,fade),PRIMARY,this::continueLast);
        by=wide("worlds",by,"Одиночная игра",Icons.Icon.GLOBE,null,NORMAL,()->go(Page.SINGLE));
        by=wide("servers",by,"Сетевая игра",Icons.Icon.SERVER,null,NORMAL,()->go(Page.MULTI));
        by=wide("options",by,"Настройки",Icons.Icon.GEAR,null,NORMAL,()->go(Page.OPTIONS));
        var me=AltAccounts.current();
        by=wide("accounts",by,"Аккаунты",Icons.Icon.USER,me==null ? null : (ix,iy,is)->Avatar.draw(g,AltAccounts.skin(me),ix,iy,is,3,fade),NORMAL,()->go(Page.ACCOUNTS));
        wide("quit",by,"Выход",Icons.Icon.POWER,null,QUIET,()->minecraft.stop());
    }
    private interface TileArt { void draw(float x,float y,float size); }
    private void serverTile(net.minecraft.resources.Identifier icon,float x,float y,float size) {
        if(icon!=null) { Avatar.image(g(),icon,x,y,size,size*.22f,fade); return; }
        UiRenderer.rounded(g(),x,y,size,size,size*.22f,col(SEG));
        Icons.draw(g(),Icons.Icon.SERVER,x+size*.25f,y+size*.25f,size*.5f,2,col(MUT));
    }
    private void worldTile(LevelSummary world,net.minecraft.resources.Identifier icon,float x,float y,float size) {
        if(icon!=null) { Avatar.image(g(),icon,x,y,size,size*.22f,fade); return; }
        UiRenderer.rounded(g(),x,y,size,size,size*.22f,col(SEG));
        Icons.draw(g(),world==null ? Icons.Icon.PLUS : worldIcon(world),x+size*.25f,y+size*.25f,size*.5f,2,col(MUT));
    }
    private static String shortName(String name) { return name.length()>16 ? name.substring(0,15)+"..." : name; }
    private static String number(long value) { return String.format(Locale.ROOT,"%,d",value).replace(',',' '); }
    private static String plural(long n,String one,String few,String many) {
        long tens=n%100, last=n%10;
        return tens>10 && tens<20 ? many : last==1 ? one : last>=2 && last<=4 ? few : many;
    }

    // ---- Plate: every page is one centred column of buttons ---------------------------------------------------

    private static final float COL_W=236, WIDE_W=344, BTN_H=26, GAP=5, ITEM_H=34, HEAD_H=30, PAD=9, PLATE_R=10;
    private static final int NORMAL=0, PRIMARY=1, QUIET=2;
    // The same see-through dark as the plate's header band, so the blur shows through every row.
    private static final int BTN=Theme.PLATE_HEADER, BTN_ON=0x24ffffff, ITEM=0x40000000, ITEM_ON=0x14ffffff, EDGE=0xff2c2d33, ON_ACCENT=0xff0b2a3c;
    /** How far the page still has to rise as it opens. */
    private float rise;
    /** The inner column of the current plate. */
    private float colX, colW;

    /** The plate centred on the screen around content of this height; returns the y its content starts at. */
    private float plate(String title,float w,float content) {
        float h=(title==null ? 0 : HEAD_H)+2*PAD+content;
        return plateAt(title,w,(H-h)/2+rise,content);
    }
    private float plateAt(String title,float w,float y,float content) {
        var g=g();
        float h=(title==null ? 0 : HEAD_H)+2*PAD+content, x=W/2-w/2;
        Theme.plate(g,x,y,w,h,PLATE_R,fade);
        colX=x+PAD; colW=w-2*PAD;
        if(title==null) return y+PAD;
        Theme.plateBand(g,x,y,w,h,true,HEAD_H,PLATE_R,fade);
        UiRenderer.rounded(g,x,y+HEAD_H-1.5f,w,1.5f,0,a(Theme.accent()));
        MsdfFont.UI.midCenter(g,title,W/2,y+HEAD_H/2-.5f,11,a(Theme.PLATE_TEXT));
        return y+HEAD_H+PAD;
    }
    /** The tallest list that leaves room for the rest of the plate; never under one row. */
    private float listHeight(int count,float rowH,float rest,float limit) {
        float room=H-36-HEAD_H-2*PAD-rest;
        return Math.max(rowH,Math.min(Math.min(room,limit),Math.max(1,count)*rowH));
    }
    /** A full-width button on the column; returns the y of the next one. */
    private float wide(String id,float y,String label,Icons.Icon icon,TileArt art,int kind,Runnable action) {
        button(id,colX,y,colW,label,icon,art,kind,action);
        return y+BTN_H+GAP;
    }
    /** Buttons sharing a row of the column; returns the y of the next row. */
    private float row(float y,List<Action> actions) {
        float w=(colW-(actions.size()-1)*GAP)/actions.size(), x=colX;
        for(var action:actions) { button(action.id(),x,y,w,action.label(),action.icon(),null,action.kind(),action.run()); x+=w+GAP; }
        return y+BTN_H+GAP;
    }
    private record Action(String id,String label,Icons.Icon icon,int kind,Runnable run) {}
    private void button(String id,float x,float y,float w,String label,Icons.Icon icon,TileArt art,int kind,Runnable action) {
        var g=g();
        boolean was=hovers.getOrDefault(id,0f)>.5f;
        float hv=hot(id,x,y,w,BTN_H,action), pr=press(id);
        if(!was && hv>.5f) UiSounds.hover();
        int accent=Theme.accent();
        int fill=switch (kind) {
            case PRIMARY -> Theme.mix(accent,0xffffffff,.2f*hv);
            case QUIET -> Theme.withAlpha(BTN_ON,hv*(BTN_ON>>>24)/255f);
            default -> Theme.mix(BTN,BTN_ON,hv);
        };
        int text=kind==PRIMARY ? ON_ACCENT : kind==QUIET ? Theme.mix(Theme.PLATE_HINT,Theme.PLATE_TEXT,hv) : Theme.PLATE_TEXT;
        String shown=fit(MsdfFont.UI,label,9,w-21);
        float tw=MsdfFont.UI.width(shown,9), total=11+6+tw, ix=x+w/2-total/2;
        g.pose().pushMatrix();
        try {
            float k=1-.03f*(float)Math.sin(Math.PI*pr);
            g.pose().translate(x+w/2,y+BTN_H/2);
            g.pose().scale(k,k);
            g.pose().translate(-x-w/2,-y-BTN_H/2);
            UiRenderer.rounded(g,x,y,w,BTN_H,7,a(fill));
            if(art!=null) art.draw(ix-.5f,y+BTN_H/2-6,12);
            else Icons.draw(g,icon,ix,y+BTN_H/2-5.5f,11,icon==Icons.Icon.PLAY ? 2 : 2.2f,a(text));
            MsdfFont.UI.mid(g,shown,ix+17,y+BTN_H/2-.5f,9,a(text));
        } finally { g.pose().popMatrix(); }
    }
    private interface Right { void draw(float right,float mid,float hover); }
    /** One row of a list: picture on the left, name and detail in the middle, the chosen one outlined in the accent. */
    private void item(String id,float x,float y,float w,boolean selected,Runnable click,Runnable twice,TileArt art,String title,String sub,int subColor,Right right) {
        var g=g();
        boolean was=hovers.getOrDefault(id,0f)>.5f;
        float hv=hot(id,x,y,w,ITEM_H,click,twice);
        if(!was && hv>.5f) UiSounds.hover();
        float sel=ease(id+"sel",selected,10), mid=y+ITEM_H/2, cx=x+w/2, max=w-2*38;
        UiRenderer.rounded(g,x,y,w,ITEM_H,7,a(Theme.mix(ITEM,ITEM_ON,hv)));
        UiRenderer.outline(g,x,y,w,ITEM_H,7,1,a(Theme.withAlpha(Theme.accent(),sel)));
        art.draw(x+6,mid-11,22);
        MsdfFont.UI.midCenter(g,fit(MsdfFont.UI,title,9,max),cx,mid-6,9,a(Theme.PLATE_TEXT));
        MsdfFont.UI.midCenter(g,fit(MsdfFont.UI,sub,7.5f,max),cx,mid+6.5f,7.5f,a(subColor));
        if(right!=null) right.draw(x+w-7,mid,hv);
    }
    private void iconButton(String id,float x,float y,Icons.Icon icon,float alpha,Runnable action) {
        float hv=hot(id,x,y,22,22,action);
        UiRenderer.rounded(g(),x,y,22,22,7,a(Theme.withAlpha(BTN_ON,alpha*hv*(BTN_ON>>>24)/255f)));
        Icons.draw(g(),icon,x+5.5f,y+5.5f,11,2.2f,a(Theme.withAlpha(Theme.mix(Theme.PLATE_HINT,icon==Icons.Icon.TRASH ? 0xffff6b6b : Theme.PLATE_TEXT,hv),alpha)));
    }
    /** Scrolling area of the column; rows are drawn by the callback at their scrolled positions. */
    private void scrolled(float x,float top,float w,float h,int count,float rowH,float gap,java.util.function.IntConsumer drawRow) {
        float content=count*rowH-gap;
        scrollMax=Math.max(0,content-h);
        scrollTarget=Math.clamp(scrollTarget,0,scrollMax);
        clip=new float[]{x,top,x+w,top+h};
        g().enableScissor((int)Math.floor(x),(int)Math.floor(top),(int)Math.ceil(x+w),(int)Math.ceil(top+h));
        try {
            for(int i=0;i<count;i++) {
                float ry=top+i*rowH-scroll;
                if(ry+rowH<top || ry>top+h) continue;
                currentRowY=ry;
                drawRow.accept(i);
            }
        } finally { g().disableScissor(); clip=null; }
        if(scrollMax>0) {
            float thumb=Math.max(18,h*h/content), ty=top+(h-thumb)*(scroll/scrollMax);
            UiRenderer.rounded(g(),x+w+2.5f,ty,2.5f,thumb,1.25f,a(Theme.withAlpha(Theme.PLATE_HINT,.5f)));
        }
    }
    private float currentRowY;
    private void emptyLine(String text,float y,float h) { MsdfFont.UI.midCenter(g(),text,W/2,y+h/2,9,a(Theme.PLATE_HINT)); }

    private void drawWorlds() {
        float rest=3*BTN_H+3*GAP, itemRow=ITEM_H+GAP;
        int count=worlds==null ? 0 : worlds.size();
        float listH=listHeight(count,itemRow,rest,5*itemRow)-GAP;
        float y=plate("Одиночная игра",COL_W,listH+rest);
        if(count==0) emptyLine(worlds==null ? "Загрузка..." : worldsError!=null ? fit(MsdfFont.UI,worldsError,9,colW) : "Миров пока нет",y,listH);
        else scrolled(colX,y,colW,listH,count,itemRow,GAP,i->worldCell(i,colX,currentRowY,colW));
        y+=listH+GAP;
        var world=count==0 ? null : worlds.get(Math.min(selectedWorld,count-1));
        y=wide("play",y,world==null ? "Создать мир" : "Играть · "+shortName(world.getLevelName()),world==null ? Icons.Icon.PLUS : Icons.Icon.PLAY,null,PRIMARY,
                world==null ? this::createWorld : ()->playWorld(world));
        var actions=new ArrayList<Action>();
        actions.add(new Action("create","Создать",Icons.Icon.PLUS,NORMAL,this::createWorld));
        if(world!=null) {
            actions.add(new Action("edit","Изменить",Icons.Icon.PENCIL,NORMAL,()->editWorld(world)));
            actions.add(new Action("delete","Удалить",Icons.Icon.TRASH,NORMAL,()->deleteWorld(world)));
        }
        y=row(y,actions);
        wide("back",y,"Назад",Icons.Icon.CHEVRON_LEFT,null,QUIET,()->go(Page.HOME));
    }
    private void worldCell(int i,float x,float y,float w) {
        var world=worlds.get(i);
        var icon=worldTexture(world);
        item("world"+i,x,y,w,i==selectedWorld,()->selectedWorld=i,()->playWorld(world),(ix,iy,is)->worldTile(world,icon,ix,iy,is),
                world.getLevelName(),worldInfo(world),Theme.PLATE_HINT,null);
    }
    // World icons (the screenshot saved as icon.png), loaded once per world list.
    private final Map<String,net.minecraft.client.gui.screens.FaviconTexture> worldIcons=new HashMap<>();
    private net.minecraft.resources.Identifier worldTexture(LevelSummary world) {
        String id=world.getLevelId();
        if(!worldIcons.containsKey(id)) {
            net.minecraft.client.gui.screens.FaviconTexture texture=null;
            var path=world.getIcon();
            if(path!=null && Files.isRegularFile(path)) try(var in=Files.newInputStream(path)) {
                var image=com.mojang.blaze3d.platform.NativeImage.read(in);
                texture=net.minecraft.client.gui.screens.FaviconTexture.forWorld(minecraft.getTextureManager(),id);
                texture.upload(image);
            } catch(Exception e) { if(texture!=null) texture.close(); texture=null; }
            worldIcons.put(id,texture);
        }
        var texture=worldIcons.get(id);
        return texture==null ? null : texture.textureLocation();
    }
    private static Icons.Icon worldIcon(LevelSummary world) {
        if(world.isHardcore()) return Icons.Icon.HEART;
        return switch (world.getGameMode()) {
            case CREATIVE -> Icons.Icon.CUBE;
            case ADVENTURE -> Icons.Icon.COMPASS;
            case SPECTATOR -> Icons.Icon.EYE;
            default -> Icons.Icon.TREE;
        };
    }
    private static String worldInfo(LevelSummary world) {
        String mode=world.isHardcore() ? "Хардкор" : world.getGameMode()==GameType.SURVIVAL ? "Выживание" : world.getGameMode().getShortDisplayName().getString();
        return ago(world.getLastPlayed())+" · "+mode;
    }
    private static String ago(long millis) {
        if(millis<=0) return "давно";
        long minutes=(System.currentTimeMillis()-millis)/60000;
        if(minutes<1) return "только что";
        if(minutes<60) return minutes+" мин назад";
        long hours=minutes/60;
        if(hours<24) return hours+" ч назад";
        long days=hours/24;
        if(days==1) return "вчера";
        if(days<30) return days+" дн назад";
        return days/30+" мес назад";
    }

    private void drawServers() {
        boolean via=viaLoaded();
        float rest=4*BTN_H+4*GAP, itemRow=ITEM_H+GAP;
        int count=servers.size();
        float listH=listHeight(count,itemRow,rest,5*itemRow)-GAP;
        float y=plate("Сетевая игра",COL_W,listH+rest);
        if(count==0) emptyLine("Серверов пока нет",y,listH);
        else scrolled(colX,y,colW,listH,count,itemRow,GAP,i->serverCell(i,colX,currentRowY,colW));
        y+=listH+GAP;
        var server=count==0 ? null : servers.get(Math.min(selectedServer,count-1));
        y=wide("connect",y,server==null ? "Добавить сервер" : "Подключиться · "+shortName(server.name),server==null ? Icons.Icon.PLUS : Icons.Icon.PLAY,null,PRIMARY,
                server==null ? this::addServer : ()->join(server));
        var actions=new ArrayList<Action>();
        actions.add(new Action("add","Добавить",Icons.Icon.PLUS,NORMAL,this::addServer));
        if(server!=null) {
            actions.add(new Action("edit","Изменить",Icons.Icon.PENCIL,NORMAL,()->editServer(server)));
            actions.add(new Action("delete","Удалить",Icons.Icon.TRASH,NORMAL,()->deleteServer(server)));
        }
        y=row(y,actions);
        actions=new ArrayList<>();
        actions.add(new Action("direct","По адресу",Icons.Icon.SIGNAL,NORMAL,this::directConnect));
        if(via) {
            String version=viaVersion();
            actions.add(new Action("via",version==null ? "Версия" : version,Icons.Icon.COMPASS,NORMAL,this::openVia));
        }
        y=row(y,actions);
        wide("back",y,"Назад",Icons.Icon.CHEVRON_LEFT,null,QUIET,()->go(Page.HOME));
    }
    private void serverCell(int i,float x,float y,float w) {
        var server=servers.get(i);
        var icon=serverIcon(server);
        boolean pinged=server.state()==ServerData.State.SUCCESSFUL;
        String sub=pinged && server.players!=null ? server.ping+" мс · "+number(server.players.online())+" онлайн" : serverInfo(server);
        int subColor=server.state()==ServerData.State.UNREACHABLE ? 0xffff8b80 : Theme.PLATE_HINT;
        item("server"+i,x,y,w,i==selectedServer,()->selectedServer=i,()->join(server),(ix,iy,is)->serverTile(icon,ix,iy,is),server.name,sub,subColor,
                pinged ? (right,mid,hv)->pingBars(right,mid,(int)server.ping) : null);
    }
    /** Five rising bars; returns the left edge. */
    private float pingBars(float right,float mid,int ping) {
        int lit=ping<0 ? 0 : ping<60 ? 5 : ping<150 ? 4 : ping<300 ? 3 : 2;
        int on=ping<150 ? 0xff5fd39a : ping<300 ? 0xfff4d35e : 0xffff6b6b;
        for(int i=0;i<5;i++) {
            float bh=3+i*1.6f;
            UiRenderer.rounded(g(),right-14+i*3,mid+4-bh,2,bh,.8f,i<lit ? a(on) : col(SEG));
        }
        return right-14;
    }
    // Server favicons uploaded once per change of the reported bytes, shared with the accounts page.
    private final Map<String,net.minecraft.client.gui.screens.FaviconTexture> icons=new HashMap<>();
    private final Map<String,byte[]> iconBytes=new HashMap<>();
    private net.minecraft.resources.Identifier serverIcon(ServerData server) {
        byte[] bytes=server.getIconBytes();
        if(bytes==null || server.ip==null) return null;
        var texture=icons.get(server.ip);
        if(iconBytes.get(server.ip)!=bytes) {
            iconBytes.put(server.ip,bytes);
            try {
                var image=com.mojang.blaze3d.platform.NativeImage.read(bytes);
                if(texture==null) { texture=net.minecraft.client.gui.screens.FaviconTexture.forServer(minecraft.getTextureManager(),server.ip); icons.put(server.ip,texture); }
                texture.upload(image);
            } catch(Exception e) { if(texture!=null) texture.clear(); icons.remove(server.ip); return null; }
        }
        return texture==null ? null : texture.textureLocation();
    }
    /** The saved server with this address (it carries the favicon), or a fresh entry for it. */
    private ServerData serverFor(String name,String ip) {
        for(int i=0;i<servers.size();i++) if(servers.get(i).ip.equalsIgnoreCase(ip)) return servers.get(i);
        if(lastServer!=null && lastServer.ip.equalsIgnoreCase(ip)) return lastServer;
        var data=new ServerData(name,ip,ServerData.Type.OTHER);
        ping(data);
        lastServer=data;
        return data;
    }

    // ---- Accounts ---------------------------------------------------------------------------------------------

    private AltAccounts.Account selectedAccount;
    private net.minecraft.client.gui.components.PlayerSkinWidget skinWidget;

    /** Accounts: one plate with the list and the form, and the chosen account's skin beside it on a wide screen. */
    private void drawAccounts() {
        var all=AltAccounts.all();
        if(selectedAccount==null || !all.contains(selectedAccount)) selectedAccount=AltAccounts.current()!=null ? AltAccounts.current() : all.getFirst();
        var account=selectedAccount;
        // Favourites first, otherwise in the order they were added.
        var sorted=new ArrayList<AltAccounts.Account>();
        for(var entry:all) if(entry.favorite) sorted.add(entry);
        for(var entry:all) if(!entry.favorite) sorted.add(entry);
        boolean current=AltAccounts.isCurrent(account), usable=AltAccounts.canUse(account);
        boolean hasServer=account.serverIp!=null && !account.serverIp.isEmpty();
        int rows=4+(hasServer ? 1 : 0);
        float rest=rows*BTN_H+rows*GAP, itemRow=ITEM_H+GAP;
        float listH=listHeight(sorted.size(),itemRow,rest,4*itemRow)-GAP;
        float y=plate("Аккаунты",COL_W,listH+rest);
        float px=colX-PAD;
        drawSkin(px-24,(H/2)+rise);
        scrolled(colX,y,colW,listH,sorted.size(),itemRow,GAP,i->accountCell(sorted.get(i),colX,currentRowY,colW));
        y+=listH+GAP;
        field("accountName",colX,y,colW,BTN_H,"Ник без пароля");
        y+=BTN_H+GAP;
        y=row(y,List.of(new Action("accountDice","Рандом",Icons.Icon.DICE,NORMAL,()->{
                    fields.put("accountName",new StringBuilder(AltAccounts.randomName()));
                    focus="accountName"; caretAt=System.nanoTime(); UiSounds.select();
                }),
                new Action("accountAdd","Добавить",Icons.Icon.PLUS,NORMAL,this::addAccount),
                new Action("accountMicrosoft","Microsoft",Icons.Icon.USER,NORMAL,()->{
                    var screen=accountScreen();
                    if(screen!=null) minecraft.gui.setScreen(screen.get());
                    else showToast("Microsoft-аккаунт входит через лаунчер");
                })));
        y=wide("accountUse",y,current ? "Ты играешь как "+account.name : "Войти как "+account.name,current ? Icons.Icon.CHECK : Icons.Icon.PLAY,null,current || !usable ? NORMAL : PRIMARY,()->useAccount(account));
        if(hasServer) y=wide("accountJoin",y,"Зайти на "+shortName(account.serverName),Icons.Icon.SERVER,null,NORMAL,()->joinAs(account));
        wide("back",y,"Назад",Icons.Icon.CHEVRON_LEFT,null,QUIET,()->go(Page.HOME));
    }
    /** The chosen account's skin, its right edge at right; the preview follows the cursor. */
    private void drawSkin(float right,float mid) {
        float skinH=170, skinW=skinH*.55f, cx=right-skinW/2;
        if(right-skinW<8 || fade<=.6f) return;
        if(skinWidget==null) skinWidget=new net.minecraft.client.gui.components.PlayerSkinWidget(60,120,minecraft.getEntityModels(),
                ()->AltAccounts.skin(selectedAccount!=null ? selectedAccount : AltAccounts.all().getFirst()));
        // The preview is a picture-in-picture render in screen units, outside the menu's scale.
        int sw=Math.round(skinW*s), sh=Math.round(skinH*s);
        skinWidget.setRectangle(sw,sh,Math.round(cx*s)-sw/2,Math.round(mid*s)-sh/2);
        var rotate=(SkinWidgetAccessor)skinWidget;
        rotate.ess$setRotationY(Math.clamp((mx-cx)/3f,-70,70));
        rotate.ess$setRotationX(Math.clamp(-(my-mid)/5f,-25,25));
        skinWidget.extractRenderState(g(),Math.round(mx*s),Math.round(my*s),0);
    }
    private void accountCell(AltAccounts.Account account,float x,float y,float w) {
        var g=g();
        String id="acc"+account.uuid;
        boolean current=AltAccounts.isCurrent(account);
        String sub=current ? "Текущий" : account.microsoft ? "Microsoft" : "Без пароля";
        item(id,x,y,w,account==selectedAccount,()->selectedAccount=account,()->useAccount(account),
                (ix,iy,is)->Avatar.draw(g,AltAccounts.skin(account),ix,iy,is,5,fade),account.name,sub,current ? 0xff6fdc9c : Theme.PLATE_HINT,(right,mid,hv)->{
            float starHv=hot(id+"star",right-16,mid-11,22,22,()->{ AltAccounts.favorite(account); UiSounds.toggle(account.favorite); });
            int star=account.favorite ? a(0xfff4c542) : a(Theme.mix(Theme.withAlpha(Theme.PLATE_HINT,.6f),Theme.PLATE_TEXT,starHv));
            // The star pops when toggled and grows a little under the cursor.
            float pop=(float)Math.sin(Math.PI*press(id+"star")), scale=1+.12f*starHv+.45f*pop;
            g.pose().pushMatrix();
            try {
                g.pose().translate(right-5,mid);
                g.pose().scale(scale,scale);
                g.pose().rotate(pop*.5f);
                Icons.draw(g,Icons.Icon.STAR,-5.5f,-5.5f,11,2.2f,star);
            } finally { g.pose().popMatrix(); }
            if(!current && hv>.01f) iconButton(id+"del",x+w-52,mid-11,Icons.Icon.TRASH,hv,()->{
                AltAccounts.remove(account);
                if(selectedAccount==account) selectedAccount=null;
                showToast("Аккаунт удалён");
            });
        });
    }
    private void addAccount() {
        String name=fields.computeIfAbsent("accountName",k->new StringBuilder()).toString().trim();
        if(!AltAccounts.validName(name)) { showToast("Ник: 3–16 символов, латиница, цифры и _"); return; }
        var account=AltAccounts.addOffline(name);
        if(account==null) { showToast("Такой аккаунт уже есть"); return; }
        fields.get("accountName").setLength(0);
        selectedAccount=account;
        UiSounds.select();
    }
    private void useAccount(AltAccounts.Account account) {
        if(AltAccounts.isCurrent(account)) return;
        if(!AltAccounts.canUse(account)) { showToast("Этот Microsoft-аккаунт входит через лаунчер"); return; }
        AltAccounts.use(account);
        showToast("Вошёл как "+account.name);
        UiSounds.select();
    }
    /** Switches to the account if needed and joins its last server. */
    private void joinAs(AltAccounts.Account account) {
        if(!AltAccounts.isCurrent(account)) {
            if(!AltAccounts.canUse(account)) { showToast("Этот Microsoft-аккаунт входит через лаунчер"); return; }
            AltAccounts.use(account);
        }
        join(serverFor(account.serverName,account.serverIp));
    }
    private static String serverInfo(ServerData server) {
        return switch (server.state()) {
            case SUCCESSFUL -> server.ping+" мс"+(server.players!=null ? " · "+server.players.online()+"/"+server.players.max() : "");
            case UNREACHABLE -> "Нет связи";
            case INCOMPATIBLE -> "Другая версия";
            default -> "Пингую...";
        };
    }

    // ---- Settings ---------------------------------------------------------------------------------------------

    private List<Category> categories() {
        var o=minecraft.options;
        return List.of(
                new Category("Видео",Icons.Icon.MONITOR,()->{
                    var list=new ArrayList<Setting>();
                    list.add(Setting.header("КАЧЕСТВО")); options(list,MenuVideoAccessor.ess$quality(o));
                    list.add(Setting.header("ЭКРАН")); list.add(Setting.of(o.fov())); options(list,MenuVideoAccessor.ess$display(o));
                    list.add(Setting.header("ИНТЕРФЕЙС")); options(list,MenuVideoAccessor.ess$preference(o));
                    return list;
                }),
                new Category("Звук",Icons.Icon.VOLUME,()->{
                    var list=new ArrayList<Setting>();
                    list.add(Setting.header("ГРОМКОСТЬ"));
                    for(var source:SoundSource.values()) list.add(Setting.of(o.getSoundSourceOptionInstance(source)));
                    list.add(Setting.header("ЕЩЁ"));
                    options(list,new OptionInstance<?>[]{o.showSubtitles(),o.directionalAudio(),o.musicFrequency(),o.musicToast(),o.soundDevice()});
                    return list;
                }),
                new Category("Управление",Icons.Icon.KEYBOARD,()->{
                    var list=new ArrayList<Setting>();
                    list.add(Setting.header("МЫШЬ")); options(list,MenuMouseAccessor.ess$options(o));
                    list.add(Setting.header("ДВИЖЕНИЕ")); options(list,MenuControlsAccessor.ess$options(o));
                    var keys=o.keyMappings.clone();
                    Arrays.sort(keys);
                    KeyMapping.Category last=null;
                    for(var key:keys) {
                        if(key.getCategory()!=last) { last=key.getCategory(); list.add(Setting.header(last.label().getString().toUpperCase(Locale.ROOT))); }
                        list.add(new Setting(null,null,key,null,null,null,null));
                    }
                    return list;
                }),
                new Category("Язык",Icons.Icon.LANGUAGE,()->{
                    var list=new ArrayList<Setting>();
                    for(var entry:minecraft.getLanguageManager().getLanguages().entrySet())
                        list.add(new Setting(entry.getValue().toComponent().getString(),null,null,entry.getKey(),null,null,null));
                    return list;
                }),
                new Category("Скин",Icons.Icon.USER,()->{
                    var list=new ArrayList<Setting>();
                    for(var part:PlayerModelPart.values()) list.add(new Setting(part.getName().getString(),null,null,null,part,null,null));
                    list.add(Setting.of(o.mainHand()));
                    return list;
                }),
                new Category("Чат",Icons.Icon.KEYBOARD,()->options(new ArrayList<>(),MenuChatAccessor.ess$options(o))),
                new Category("Доступность",Icons.Icon.EYE,()->{
                    var list=options(new ArrayList<>(),MenuAccessibilityAccessor.ess$options(o));
                    list.add(Setting.header("ШРИФТ"));
                    return options(list,MenuFontAccessor.ess$options(o));
                }),
                new Category("Фон меню",Icons.Icon.SNOWFLAKE,()->{
                    var list=new ArrayList<Setting>();
                    list.add(background("Сетка",null));
                    list.add(background("Новогодний",MenuScene.Style.MOUNTAINS));
                    list.add(Setting.header("ШЕЙДЕРЫ"));
                    for(var style:MenuScene.Style.values())
                        if(style!=MenuScene.Style.MOUNTAINS && style!=MenuScene.Style.FIR && style!=MenuScene.Style.GRID) list.add(background(style.label,style));
                    return list;
                }));
    }
    /** One choice of the menu backdrop; null is the plain grid. */
    private static Setting background(String title,MenuScene.Style style) {
        return new Setting(title,null,null,null,null,()->{ Prefs.background=style; Prefs.save(); },()->Prefs.background==style);
    }
    private static List<Setting> options(List<Setting> list,OptionInstance<?>[] options) {
        for(var option:options) if(option!=null) list.add(Setting.of(option));
        return list;
    }

    /** Settings: the categories first; one of them opens as its own plate of rows. */
    private void drawOptions() {
        var all=categories();
        category=Math.clamp(category,0,all.size()-1);
        if(!categoryOpen) {
            int rows=(all.size()+1)/2;
            float y=plate("Настройки",COL_W,rows*(BTN_H+GAP)+2*BTN_H+GAP);
            float w=(colW-GAP)/2;
            for(int i=0;i<all.size();i++) {
                int index=i;
                button("cat"+i,colX+(i%2)*(w+GAP),y+(i/2)*(BTN_H+GAP),w,all.get(i).name(),all.get(i).icon(),null,NORMAL,()->openCategory(index));
            }
            y+=rows*(BTN_H+GAP);
            y=wide("catAll",y,"Все настройки",Icons.Icon.GRID,null,NORMAL,()->minecraft.gui.setScreen(new OptionsScreen(this,minecraft.options)));
            wide("back",y,"Назад",Icons.Icon.CHEVRON_LEFT,null,QUIET,()->go(Page.HOME));
            return;
        }
        if(settings==null) settings=all.get(category).settings().get();
        float rowH=28, rest=BTN_H+GAP, listH=listHeight(settings.size(),rowH,rest,H-120);
        float y=plate(all.get(category).name(),WIDE_W,listH+rest);
        float x=colX, w=colW;
        scrolled(x,y,w,listH,settings.size(),rowH,0,i->settingRow(i,settings.get(i),x,currentRowY,w,rowH));
        wide("back",y+listH+GAP,"Назад",Icons.Icon.CHEVRON_LEFT,null,QUIET,this::closeCategory);
    }
    private void openCategory(int index) {
        category=index; categoryOpen=true; settings=null; scroll=scrollTarget=0; listening=null; pageAt=now(); UiSounds.select();
    }
    private void closeCategory() {
        categoryOpen=false; settings=null; scroll=scrollTarget=0; listening=null; dragging=null; pageAt=now(); UiSounds.select();
    }

    private void settingRow(int i,Setting setting,float x,float y,float w,float h) {
        var g=g();
        float mid=y+h/2, right=x+w-10;
        if(setting.isHeader()) {
            spaced(MsdfFont.UI,setting.title(),x+10,y+h-9,7.5f,1.5f,col(MUT));
            return;
        }
        if(i>0 && !settings.get(i-1).isHeader()) UiRenderer.rect(g,x+10,y,w-20,1,col(LINE));
        var o=minecraft.options;
        String id="set"+category+"_"+i;
        if(setting.key()!=null) {
            var key=setting.key();
            String name=Component.translatable(key.getName()).getString();
            boolean wait=listening==key;
            String text=wait ? "> ... <" : key.getTranslatedKeyMessage().getString();
            float kw=Math.max(64,MsdfFont.UI.width(text,8)+18);
            float hv=hot(id,x,y,w,h,()->{ listening=wait ? null : key; UiSounds.bindListen(); });
            rowBackground(x,y,w,h,hv);
            MsdfFont.UI.mid(g,fit(MsdfFont.UI,name,8.5f,w-kw-40),x+10,mid,8.5f,col(FG));
            boolean conflict=!key.isUnbound() && Arrays.stream(o.keyMappings).anyMatch(other->other!=key && other.same(key));
            card(right-kw,mid-10,kw,20,6,col(SOLID),wait ? col(FG) : col(LINE));
            UiRenderer.rounded(g,right-kw+1,mid+8,kw-2,1,0,col(LINE));
            MsdfFont.UI.midCenter(g,text,right-kw/2,mid,8,conflict ? a(0xffe5484d) : col(FG));
            if(!key.isDefault()) iconButtonSmall(id+"reset",right-kw-24,mid-9,Icons.Icon.CLOSE,()->{
                key.setKey(key.getDefaultKey()); KeyMapping.resetMapping(); o.save(); });
            return;
        }
        if(setting.language()!=null) {
            boolean selected=setting.language().equals(minecraft.getLanguageManager().getSelected());
            float hv=hot(id,x,y,w,h,()->{
                if(selected) return;
                minecraft.getLanguageManager().setSelected(setting.language());
                o.languageCode=setting.language();
                minecraft.reloadResourcePacks();
                o.save();
                settings=null;
            });
            rowBackground(x,y,w,h,hv);
            MsdfFont.UI.mid(g,setting.title(),x+10,mid,8.5f,col(FG));
            if(selected) Icons.draw(g,Icons.Icon.CHECK,right-12,mid-6,12,2.4f,col(FG));
            return;
        }
        if(setting.part()!=null || setting.custom()!=null) {
            boolean on=setting.part()!=null ? o.isModelPartEnabled(setting.part()) : setting.state().getAsBoolean();
            float hv=hot(id,x,y,w,h,()->{
                if(setting.part()!=null) o.setModelPart(setting.part(),!on); else setting.custom().run();
                UiSounds.toggle(!on); o.save();
            });
            rowBackground(x,y,w,h,hv);
            MsdfFont.UI.mid(g,setting.title(),x+10,mid,8.5f,col(FG));
            switchControl(id+"sw",right-30,mid-8.5f,on);
            return;
        }
        var option=setting.option();
        String[] text=optionText.computeIfAbsent(option,this::describe);
        // A boolean worded other than on/off ("Sneak: Hold / Toggle") reads better as a chip than as a switch.
        boolean onOff=text[1].equals(CommonComponents.OPTION_ON.getString()) || text[1].equals(CommonComponents.OPTION_OFF.getString());
        if(option.values()==OptionInstance.BOOLEAN_VALUES && onOff) {
            @SuppressWarnings("unchecked") var flag=(OptionInstance<Boolean>)option;
            boolean on=flag.get();
            float hv=hot(id,x,y,w,h,()->{ flag.set(!on); changed(option); UiSounds.toggle(!on); });
            rowBackground(x,y,w,h,hv);
            MsdfFont.UI.mid(g,fit(MsdfFont.UI,text[0],8.5f,w-70),x+10,mid,8.5f,col(FG));
            switchControl(id+"sw",right-30,mid-8.5f,on);
        } else if(option.values() instanceof OptionInstance.SliderableValueSet<?>) {
            float trackW=Math.min(130,w*.32f), valueW=58, tx=right-valueW-trackW;
            double value=dragging==option ? dragValue : sliderValue(option);
            float hv=hot(id,tx-6,y,trackW+12,h,()->{ dragging=option; dragX=tx; dragW=trackW; dragTo(clickX); });
            MsdfFont.UI.mid(g,fit(MsdfFont.UI,text[0],8.5f,tx-x-24),x+10,mid,8.5f,col(FG));
            UiRenderer.rounded(g,tx,mid-2,trackW,4,2,col(SEG));
            UiRenderer.rounded(g,tx,mid-2,trackW*(float)value,4,2,a(Theme.accent()));
            float knob=6+1.5f*Math.max(hv,dragging==option ? 1 : 0);
            UiRenderer.rounded(g,tx+trackW*(float)value-knob,mid-knob,knob*2,knob*2,knob,col(FG));
            MsdfFont.UI.midRight(g,fit(MsdfFont.UI,text[1],8,valueW-8),right,mid,8,col(MUT));
        } else if(option.values() instanceof OptionInstance.CycleableValueSet<?>) {
            String value=text[1].isEmpty() ? text[0] : text[1];
            float chipW=Math.min(Math.max(90,MsdfFont.UI.width(value,8)+44),w*.5f), cxl=right-chipW;
            float hv=hot(id,cxl,mid-10,chipW,20,()->{ cycle(option,clickX<cxl+20 ? -1 : 1); changed(option); UiSounds.select(); });
            MsdfFont.UI.mid(g,fit(MsdfFont.UI,text[0],8.5f,cxl-x-20),x+10,mid,8.5f,col(FG));
            card(cxl,mid-10,chipW,20,7,col(SOLID),a(Theme.mix(Theme.mix(LINE[0],LINE[1],lt),Theme.mix(MUT[0],MUT[1],lt),hv)));
            Icons.draw(g,Icons.Icon.CHEVRON_LEFT,cxl+5,mid-5,10,2.2f,col(MUT));
            Icons.draw(g,Icons.Icon.CHEVRON_RIGHT,right-15,mid-5,10,2.2f,col(MUT));
            MsdfFont.UI.midCenter(g,fit(MsdfFont.UI,value,8,chipW-40),cxl+chipW/2,mid,8,col(FG));
        } else {
            MsdfFont.UI.mid(g,fit(MsdfFont.UI,text[0]+(text[1].isEmpty() ? "" : ": "+text[1]),8.5f,w-20),x+10,mid,8.5f,col(MUT));
        }
    }
    private void rowBackground(float x,float y,float w,float h,float hover) {
        UiRenderer.rounded(g(),x,y+1,w,h-2,8,a(Theme.withAlpha(Theme.mix(SEG[0],SEG[1],lt),hover*.7f)));
    }
    private void iconButtonSmall(String id,float x,float y,Icons.Icon icon,Runnable action) {
        float hv=hot(id,x,y,18,18,action);
        UiRenderer.rounded(g(),x,y,18,18,6,a(Theme.withAlpha(Theme.mix(SEG[0],SEG[1],lt),hv)));
        Icons.draw(g(),icon,x+4.5f,y+4.5f,9,2.4f,a(Theme.mix(Theme.mix(MUT[0],MUT[1],lt),Theme.mix(FG[0],FG[1],lt),hv)));
    }
    /** Caption and value as vanilla words them, read from the option's own button ("Графика: Красивая"). */
    private String[] describe(OptionInstance<?> option) {
        String message=option.createButton(minecraft.options).getMessage().getString();
        int split=message.indexOf(": ");
        return split<0 ? new String[]{message,""} : new String[]{message.substring(0,split),message.substring(split+2)};
    }
    private void changed(OptionInstance<?> option) {
        optionText.remove(option);
        minecraft.options.save();
    }
    @SuppressWarnings("unchecked")
    private static <T> void cycle(OptionInstance<T> option,int direction) {
        List<T> values=((OptionInstance.CycleableValueSet<T>)option.values()).valueListSupplier().getSelectedList();
        if(values.isEmpty()) return;
        int index=values.indexOf(option.get());
        option.set(values.get(Math.floorMod(index+direction,values.size())));
    }
    @SuppressWarnings("unchecked")
    private static <T> double sliderValue(OptionInstance<T> option) {
        return ((OptionInstance.SliderableValueSet<T>)option.values()).toSliderValue(option.get());
    }
    @SuppressWarnings("unchecked")
    private static <T> void setSlider(OptionInstance<T> option,double value) {
        option.set(((OptionInstance.SliderableValueSet<T>)option.values()).fromSliderValue(value));
    }
    private void dragTo(float x) {
        dragValue=Math.clamp((x-dragX)/dragW,0,1);
        if(((OptionInstance.SliderableValueSet<?>)dragging.values()).applyValueImmediately()) { setSlider(dragging,dragValue); optionText.remove(dragging); }
    }

    // ---- Forms ------------------------------------------------------------------------------------------------

    /** Site input: hairline that darkens on focus, text clipped to show its end, blinking caret. */
    private void field(String id,float x,float y,float w,float h,String placeholder) {
        var g=g();
        boolean focused=id.equals(focus);
        float hv=hot("field"+id,x,y,w,h,()->{ focus=id; caretAt=System.nanoTime(); });
        float f=ease("focus"+id,focused,12);
        UiRenderer.rounded(g,x,y,w,h,7,a(ITEM));
        UiRenderer.outline(g,x,y,w,h,7,1,a(Theme.mix(Theme.mix(EDGE,Theme.PLATE_HINT,hv*.4f),Theme.accent(),f)));
        String text=fields.get(id).toString();
        float max=w-20, tw=MsdfFont.UI.width(text,9), tx=x+(w-Math.min(tw,max))/2;
        g.enableScissor((int)Math.floor(x+6),(int)Math.floor(y),(int)Math.ceil(x+w-6),(int)Math.ceil(y+h));
        try {
            if(text.isEmpty()) MsdfFont.UI.midCenter(g,placeholder,x+w/2,y+h/2-.5f,9,a(Theme.withAlpha(Theme.PLATE_HINT,.7f)));
            float shift=Math.max(0,tw-max);
            ess.ll.client.module.render.TypingAnimation.mid(g,MsdfFont.UI,"menu:"+id,text,tx-shift,y+h/2-.5f,9,a(Theme.PLATE_TEXT));
            if(focused && (System.nanoTime()-caretAt)/1e9%1<.55) UiRenderer.rect(g,tx-shift+tw+1,y+h/2-6,1,12,a(Theme.PLATE_TEXT));
        } finally { g.disableScissor(); }
    }
    /** Equal parts across the width: the chosen one takes the accent. */
    private void segmented(String id,float x,float y,float w,String[] labels,int selected,boolean enabled,java.util.function.IntConsumer pick) {
        var g=g();
        float pad=2, h=24, part=(w-2*pad)/labels.length, alpha=enabled ? 1 : .45f;
        UiRenderer.rounded(g,x,y,w,h,7,a(Theme.withAlpha(ITEM,alpha*(ITEM>>>24)/255f)));
        for(int i=0;i<labels.length;i++) {
            int index=i;
            float px=x+pad+i*part;
            float hv=hot(id+i,px,y+pad,part,h-2*pad,enabled ? ()->{ pick.accept(index); UiSounds.select(); } : null);
            float on=ease(id+"on"+i,i==selected,10);
            UiRenderer.rounded(g,px,y+pad,part,h-2*pad,5,a(Theme.withAlpha(Theme.accent(),on*alpha)));
            int text=Theme.mix(Theme.mix(Theme.PLATE_HINT,Theme.PLATE_TEXT,hv),ON_ACCENT,on);
            MsdfFont.UI.midCenter(g,fit(MsdfFont.UI,labels[i],8,part-6),px+part/2,y+h/2-.5f,8,a(Theme.withAlpha(text,alpha)));
        }
    }
    private void fieldLabel(String text,float y) { MsdfFont.UI.midCenter(g(),text,W/2,y+4,7.5f,a(Theme.PLATE_HINT)); }

    private void drawServerForm() {
        if(formServer==null) { go(Page.MULTI); return; }
        boolean edit=editedServer!=null;
        float line=12+BTN_H+GAP;
        float y=plate(edit ? "Изменить сервер" : "Новый сервер",COL_W,3*line+2*BTN_H+GAP);
        fieldLabel("Название",y); field("serverName",colX,y+12,colW,BTN_H,I18n.get("selectServer.defaultName"));
        y+=line;
        fieldLabel("Адрес",y); field("serverIp",colX,y+12,colW,BTN_H,"mc.example.net");
        y+=line;
        fieldLabel("Наборы ресурсов",y);
        var statuses=new ServerData.ServerPackStatus[]{ServerData.ServerPackStatus.PROMPT,ServerData.ServerPackStatus.ENABLED,ServerData.ServerPackStatus.DISABLED};
        String[] names=Arrays.stream(statuses).map(st->st.getName().getString()).toArray(String[]::new);
        segmented("pack",colX,y+12,colW,names,Arrays.asList(statuses).indexOf(formServer.getResourcePackStatus()),true,i->formServer.setResourcePackStatus(statuses[i]));
        y+=line;
        y=wide("serverDone",y,edit ? "Сохранить" : "Добавить сервер",Icons.Icon.CHECK,null,PRIMARY,this::saveServer);
        wide("serverCancel",y,"Назад",Icons.Icon.CHEVRON_LEFT,null,QUIET,()->{ formServer=editedServer=null; go(Page.MULTI); });
    }

    private void drawWorldForm() {
        float line=11+24+4, rowH=25;
        if(createWorld==null) { plate("Новый мир",COL_W,BTN_H); emptyLine("Готовлю настройки мира...",H/2+6+rise-BTN_H/2,BTN_H); return; }
        var state=createWorld.getUiState();
        boolean hardcore=state.isHardcore();
        float y=plate("Новый мир",WIDE_W-44,3*line+6*rowH+GAP+BTN_H);
        float x=colX, w=colW, right=x+w-4;
        fieldLabel("Название",y); field("worldName",x,y+11,w,24,"Новый мир");
        y+=line;
        fieldLabel("Режим",y);
        var modes=new WorldCreationUiState.SelectedGameMode[]{WorldCreationUiState.SelectedGameMode.SURVIVAL,
                WorldCreationUiState.SelectedGameMode.HARDCORE,WorldCreationUiState.SelectedGameMode.CREATIVE};
        segmented("mode",x,y+11,w,Arrays.stream(modes).map(m->m.displayName.getString()).toArray(String[]::new),
                Arrays.asList(modes).indexOf(state.getGameMode()),true,i->state.setGameMode(modes[i]));
        y+=line;
        fieldLabel("Сложность",y);
        var levels=Difficulty.values();
        segmented("difficulty",x,y+11,w,Arrays.stream(levels).map(d->d.getDisplayName().getString()).toArray(String[]::new),
                state.getDifficulty().ordinal(),!hardcore,i->state.setDifficulty(levels[i]));
        y+=line;
        MsdfFont.UI.mid(g(),"Сид",x+4,y+rowH/2,9,a(Theme.PLATE_TEXT));
        field("seed",right-130,y+rowH/2-11,130,22,"случайный");
        y+=rowH;
        String type=state.getWorldType().describePreset().getString();
        float chipW=130, chipX=right-chipW, chipY=y+rowH/2-10;
        float th=hot("worldType",chipX,chipY,chipW,20,()->{
            var presets=state.getNormalPresetList();
            if(presets.isEmpty()) return;
            int index=presets.indexOf(state.getWorldType());
            state.setWorldType(presets.get(Math.floorMod(index+(clickX<chipX+20 ? -1 : 1),presets.size())));
            UiSounds.select();
        });
        MsdfFont.UI.mid(g(),"Тип мира",x+4,y+rowH/2,9,a(Theme.PLATE_TEXT));
        UiRenderer.rounded(g(),chipX,chipY,chipW,20,7,a(Theme.mix(BTN,BTN_ON,th)));
        Icons.draw(g(),Icons.Icon.CHEVRON_LEFT,chipX+5,chipY+5,10,2.2f,a(Theme.PLATE_HINT));
        Icons.draw(g(),Icons.Icon.CHEVRON_RIGHT,right-15,chipY+5,10,2.2f,a(Theme.PLATE_HINT));
        MsdfFont.UI.midCenter(g(),fit(MsdfFont.UI,type,8,chipW-40),chipX+chipW/2,chipY+9.5f,8,a(Theme.PLATE_TEXT));
        y+=rowH;
        y=worldSwitch("cheats","Читы",x,y,w,rowH,state.isAllowCommands(),!hardcore,()->state.setAllowCommands(!state.isAllowCommands()));
        y=worldSwitch("structures","Постройки",x,y,w,rowH,state.isGenerateStructures(),!state.isDebug(),()->state.setGenerateStructures(!state.isGenerateStructures()));
        y=worldSwitch("chest","Бонусный сундук",x,y,w,rowH,state.isBonusChest(),!state.isDebug(),()->state.setBonusChest(!state.isBonusChest()));
        float ah=hot("advanced",x,y,w,rowH,this::advancedWorld);
        int color=a(Theme.mix(Theme.PLATE_HINT,Theme.PLATE_TEXT,ah));
        MsdfFont.UI.mid(g(),"Правила игры, датапаки",x+4,y+rowH/2,9,color);
        Icons.draw(g(),Icons.Icon.CHEVRON_RIGHT,right-11+2*ah,y+rowH/2-5.5f,11,2.2f,color);
        y+=rowH+GAP;
        row(y,List.of(new Action("worldCreate","Создать мир",Icons.Icon.PLUS,PRIMARY,this::finishWorld),
                new Action("worldCancel","Назад",Icons.Icon.CHEVRON_LEFT,NORMAL,this::cancelWorld)));
    }
    private float worldSwitch(String id,String label,float x,float y,float w,float h,boolean on,boolean enabled,Runnable flip) {
        hot(id,x,y,w,h,enabled ? ()->{ flip.run(); UiSounds.toggle(!on); } : null);
        MsdfFont.UI.mid(g(),label,x+4,y+h/2,9,a(enabled ? Theme.PLATE_TEXT : Theme.PLATE_HINT));
        float saved=fade;
        if(!enabled) fade*=.45f;
        switchControl(id+"sw",x+w-4-30,y+h/2-8.5f,on);
        fade=saved;
        return y+h;
    }
    /** The knob slides over and the track fills with the accent. */
    private void switchControl(String id,float x,float y,boolean on) {
        float v=ease(id,on,9);
        float k=v<.5f ? 4*v*v*v : 1-(float)Math.pow(-2*v+2,3)/2;
        UiRenderer.rounded(g(),x,y,30,17,8.5f,a(Theme.mix(BTN_ON,Theme.accent(),v)));
        UiRenderer.rounded(g(),x+2+13*k,y+2,13,13,6.5f,a(Theme.mix(Theme.PLATE_HINT,0xffffffff,v)));
    }

    // ---- Media player -----------------------------------------------------------------------------------------

    private boolean playerOpen;
    private final Tween playerTween=new Tween(0,.45,true);
    private float lyricShift;
    private int lyricIndex=-2;

    /** The system's current track in the bottom-right corner of every page; opens upward with synced lyrics. */
    private void drawPlayer(double now) {
        var g=g();
        MediaBridge.ensureRunning();
        var track=MediaBridge.track();
        boolean has=track.present() && !track.title().isBlank();
        if(!has) playerOpen=false;
        playerTween.set(playerOpen ? 1 : 0,now);
        float open=playerTween.get(now);
        // The pill grows to fit the whole "title · artist" line instead of cutting it, up to half the screen.
        String line=track.artist().isBlank() ? track.title() : track.title()+" · "+track.artist();
        float controls=4*20+2, base=has ? 4+18+8+MsdfFont.UI.width(line,8)+8+controls : 150;
        float pw=Math.clamp(base,150,Math.max(150,W*.5f))+44*open, ph=26, px=W-16-pw, py=H-16-ph/2, panel=130;

        if(open>.01f) {
            float eh=panel*open, ey=py-6-eh;
            // Swallows clicks so the page under the panel stays put.
            hot("playerPanel",px,ey,pw,eh,()->{});
            Theme.plate(g,px,ey,pw,eh,12,fade);
            g.enableScissor((int)Math.floor(px),(int)Math.floor(ey),(int)Math.ceil(px+pw),(int)Math.ceil(ey+eh));
            float saved=fade;
            fade*=(float)Math.clamp((open-.3)/.7,0,1);
            try {
                float top=ey+eh-panel;
                artwork(px+12,top+12,40,9);
                float tx=px+12+40+10, max=pw-24-50;
                MsdfFont.UI.mid(g,fit(MsdfFont.UI,track.title(),9.5f,max),tx,top+24,9.5f,col(FG));
                MsdfFont.UI.mid(g,fit(MsdfFont.UI,track.artist(),8,max),tx,top+38,8,col(MUT));
                lyrics(track,px+12,top+66,pw-24,panel-66-8);
            } finally { g.disableScissor(); fade=saved; }
        }

        boolean was=hovers.getOrDefault("player",0f)>.5f;
        float hv=hot("player",px,py,pw,ph,has ? ()->{ playerOpen=!playerOpen; UiSounds.select(); } : null);
        if(has && !was && hv>.5f) UiSounds.hover();
        Theme.plate(g,px,py,pw,ph,10,fade);
        float mid=py+ph/2;
        if(!has) {
            Icons.draw(g,Icons.Icon.MUSIC,px+10,mid-5.5f,11,2,col(MUT));
            MsdfFont.UI.mid(g,"Ничего не играет",px+27,mid,7.5f,col(MUT));
            return;
        }
        artwork(px+4,py+4,18,6);
        float tx=px+4+18+8, max=pw-(tx-px)-controls-6;
        MsdfFont.UI.mid(g,fit(MsdfFont.UI,line,8,max),tx,mid,8,col(FG));
        float cx=px+pw-controls;
        playerButton("playerPrev",cx,mid,Icons.Icon.PREVIOUS,MediaBridge::previous);
        playerButton("playerToggle",cx+20,mid,track.playing() ? Icons.Icon.PAUSE : Icons.Icon.PLAY,MediaBridge::toggle);
        playerButton("playerNext",cx+40,mid,Icons.Icon.NEXT,MediaBridge::next);
        playerButton("playerOpen",cx+60,mid,Icons.Icon.CHEVRON_DOWN,()->{ playerOpen=!playerOpen; UiSounds.select(); });
    }
    private void playerButton(String id,float x,float mid,Icons.Icon icon,Runnable action) {
        var g=g();
        float hv=hot(id,x,mid-10,20,20,action);
        UiRenderer.rounded(g,x,mid-10,20,20,6,a(Theme.withAlpha(Theme.mix(SEG[0],SEG[1],lt),hv)));
        int color=a(Theme.mix(Theme.mix(MUT[0],MUT[1],lt),Theme.mix(FG[0],FG[1],lt),.5f+.5f*hv));
        // The open/close chevron points up while closed: drawn turned over.
        if(id.equals("playerOpen") && !playerOpen) {
            g.pose().pushMatrix();
            try {
                g.pose().translate(x+10,mid);
                g.pose().scale(1,-1);
                Icons.draw(g,icon,-5,-5,10,2.2f,color);
            } finally { g.pose().popMatrix(); }
        } else Icons.draw(g,icon,x+5,mid-5,10,icon==Icons.Icon.CHEVRON_DOWN ? 2.2f : 2,color);
    }
    private void artwork(float x,float y,float size,float radius) {
        var art=MediaBridge.artwork();
        if(art!=null) Avatar.image(g(),art,x,y,size,radius,fade);
        else {
            UiRenderer.rounded(g(),x,y,size,size,radius,Gradient.linear(a(0xffff6a3d),a(0xff8b2fc9),Gradient.Axis.VERTICAL,y,size));
            Icons.draw(g(),Icons.Icon.MUSIC,x+size*.25f,y+size*.25f,size*.5f,2,a(0xffffffff));
        }
    }
    /** Synced lines around the current one; the list glides as lines change. */
    private void lyrics(MediaBridge.Track track,float x,float y,float w,float h) {
        var g=g();
        var lines=Lyrics.of(track);
        UiRenderer.rect(g,x,y-6,w,1,col(LINE));
        if(lines.isEmpty()) {
            MsdfFont.UI.midCenter(g,"Текст песни не найден",x+w/2,y+h/2,7.5f,col(MUT));
            return;
        }
        int index=Lyrics.index(lines,track.position());
        float lineH=15;
        if(lyricIndex==-2) lyricShift=Math.max(0,index);
        lyricIndex=index;
        lyricShift+=(Math.max(0,index)-lyricShift)*(float)(1-Math.exp(-dt*8));
        float center=y+h/2;
        g.enableScissor((int)Math.floor(x),(int)Math.floor(y),(int)Math.ceil(x+w),(int)Math.ceil(y+h));
        try {
            for(int i=Math.max(0,index-3);i<Math.min(lines.size(),index+5);i++) {
                float ly=center+(i-lyricShift)*lineH;
                float near=Math.clamp(1-Math.abs(i-lyricShift),0,1);
                float size=7.5f+1.5f*near;
                float edge=Math.clamp(1-Math.abs(ly-center)/(h/2+4),0,1);
                int color=Theme.mix(Theme.withAlpha(Theme.mix(MUT[0],MUT[1],lt),.55f),Theme.mix(FG[0],FG[1],lt),near);
                var font=near>.5f ? MsdfFont.UI : MsdfFont.SITE;
                font.midCenter(g,fit(font,lines.get(i).text(),size,w-8),x+w/2,ly,size,a(Theme.withAlpha(color,(color>>>24)/255f*edge)));
            }
        } finally { g.disableScissor(); }
    }

    private void drawToast(double now) {
        double age=now-toastAt;
        if(age>1.9) return;
        float in=(float)Math.clamp(age/.5,0,1), outT=(float)Math.clamp((age-1.4)/.35,0,1);
        float e=1-(float)Math.pow(1-in,4);
        fade=Math.min(e,1-outT);
        float w=MsdfFont.UI.width(toast,8.5f)+28, h=28;
        float x=(W-w)/2, y=H-30-h+(1-e)*20;
        Theme.plate(g(),x,y,w,h,10,fade);
        MsdfFont.UI.mid(g(),toast,x+14,y+h/2,8.5f,col(FG));
        fade=1;
    }

    // ---- Input ------------------------------------------------------------------------------------------------

    private float clickX;
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(listening!=null) {
            // Any mouse button binds while a key waits for input.
            listening.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            bound();
            return true;
        }
        if(event.button()!=InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event,doubleClick);
        float x=(float)event.x()/s, y=(float)event.y()/s;
        clickX=x;
        focus=null;
        for(int i=hots.size()-1;i>=0;i--) {
            var hot=hots.get(i);
            if(x>=hot.x() && x<hot.x()+hot.w() && y>=hot.y() && y<hot.y()+hot.h()) {
                hot.click().run();
                if(doubleClick && hot.twice()!=null) hot.twice().run();
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) {
        if(scrollMax>0) { scrollTarget=Math.clamp(scrollTarget-(float)vertical*30,0,scrollMax); return true; }
        return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(dragging!=null) { dragTo((float)event.x()/s); return true; }
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(dragging!=null) {
            setSlider(dragging,dragValue);
            changed(dragging);
            dragging=null;
            return true;
        }
        return super.mouseReleased(event);
    }
    private void bound() {
        KeyMapping.resetMapping();
        minecraft.options.save();
        listening=null;
        UiSounds.bindSet();
    }
    @Override public boolean charTyped(CharacterEvent event) {
        if(focus!=null && event.isAllowedChatCharacter()) {
            var text=fields.get(focus);
            if(focus.equals("accountName")) {
                // Nicks are Latin letters, digits and underscores, up to 16.
                String c=event.codepointAsString();
                if(text.length()<16 && c.matches("[A-Za-z0-9_]")) text.append(c);
                caretAt=System.nanoTime();
                return true;
            }
            if(text.length()<(focus.equals("serverIp") || focus.equals("seed") ? 256 : 48)) text.append(event.codepointAsString());
            caretAt=System.nanoTime();
            return true;
        }
        return super.charTyped(event);
    }
    private static final List<String> SERVER_FIELDS=List.of("serverName","serverIp"), WORLD_FIELDS=List.of("worldName","seed");
    @Override public boolean keyPressed(KeyEvent event) {
        if(listening!=null) {
            listening.setKey(event.key()==InputConstants.KEY_ESCAPE ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            bound();
            return true;
        }
        if(focus!=null) {
            var text=fields.get(focus);
            caretAt=System.nanoTime();
            if(event.key()==InputConstants.KEY_BACKSPACE) {
                if(event.hasControlDown()) text.setLength(0); else if(!text.isEmpty()) text.setLength(text.offsetByCodePoints(text.length(),-1));
                return true;
            }
            if(event.isPaste()) { text.append(minecraft.keyboardHandler.getClipboard().replaceAll("[\\r\\n]","")); return true; }
            if(event.isSelectAll()) return true;
            if(event.key()==InputConstants.KEY_TAB) {
                if(page==Page.ACCOUNTS) return true;
                var order=page==Page.SERVER_FORM ? SERVER_FIELDS : WORLD_FIELDS;
                focus=order.get((order.indexOf(focus)+1)%order.size());
                return true;
            }
            if(event.key()==InputConstants.KEY_ESCAPE) { focus=null; return true; }
        }
        if(event.key()==InputConstants.KEY_RETURN || event.key()==InputConstants.KEY_NUMPADENTER) {
            if(page==Page.SERVER_FORM) { saveServer(); return true; }
            if(page==Page.ACCOUNTS && "accountName".equals(focus)) { addAccount(); return true; }
            if(page==Page.WORLD_FORM && createWorld!=null) { finishWorld(); return true; }
        }
        if(event.key()==InputConstants.KEY_ESCAPE && page==Page.SERVER_FORM) { formServer=editedServer=null; go(Page.MULTI); return true; }
        if(event.key()==InputConstants.KEY_ESCAPE && page==Page.WORLD_FORM) { cancelWorld(); return true; }
        if(event.key()==InputConstants.KEY_ESCAPE && categoryOpen) { closeCategory(); return true; }
        if(event.key()==InputConstants.KEY_ESCAPE && page!=Page.HOME) { go(Page.HOME); return true; }
        if(event.key()==InputConstants.KEY_RETURN || event.key()==InputConstants.KEY_NUMPADENTER) {
            if(page==Page.SINGLE && worlds!=null && !worlds.isEmpty()) { playWorld(worlds.get(Math.min(selectedWorld,worlds.size()-1))); return true; }
            if(page==Page.MULTI && servers.size()>0) { join(servers.get(Math.min(selectedServer,servers.size()-1))); return true; }
        }
        if(event.key()==InputConstants.KEY_UP || event.key()==InputConstants.KEY_DOWN) {
            int step=event.key()==InputConstants.KEY_UP ? -1 : 1;
            if(page==Page.SINGLE && worlds!=null && !worlds.isEmpty()) selectedWorld=Math.clamp(selectedWorld+step,0,worlds.size()-1);
            if(page==Page.MULTI && servers.size()>0) selectedServer=Math.clamp(selectedServer+step,0,servers.size()-1);
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public void tick() { pinger.tick(); }
    @Override public void removed() {
        pinger.removeAll();
        minecraft.textInputManager().stopTextInput(this);
    }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {}

    /** Dev: autoshot switches pages without input. */
    public void showPage(int index) { go(Page.values()[index]); }
    public String pageForTest() { return page.name(); }
    /** Runs the button under a point given as a share of the screen. */
    public boolean clickForTest(float x,float y) {
        for(int i=hots.size()-1;i>=0;i--) {
            var hot=hots.get(i);
            if(W*x>=hot.x() && W*x<hot.x()+hot.w() && H*y>=hot.y() && H*y<hot.y()+hot.h()) { hot.click().run(); return true; }
        }
        return false;
    }
    public void openViaForTest() { openVia(); }
    public void toggleTheme() { Prefs.light=!Prefs.light; }
    public void showCategory(int index) { category=index; categoryOpen=true; settings=null; scroll=scrollTarget=0; }
    public void showCategory(String name) { var all=categories(); for(int i=0;i<all.size();i++) if(all.get(i).name().equals(name)) showCategory(i); }
    /** Dev check: shows a backdrop without saving it; returns the one that was set. */
    public static MenuScene.Style backgroundForTest(MenuScene.Style style) { var old=Prefs.background; Prefs.background=style; return old; }
    public void showServerForm() { addServer(); }
    public void showWorldForm() { createWorld(); }
    public void openPlayer() { playerOpen=true; }
    /** The menu's theme, shared with the pause menu. */
    public static boolean lightTheme() { return Prefs.light; }
    public static void toggleLightForTest() { Prefs.light=!Prefs.light; }
    public void selectAccount(String name) { for(var account:AltAccounts.all()) if(account.name.equals(name)) selectedAccount=account; }

    /** Menu preferences (theme, backdrop, last world or server) in config/ess-menu.properties. */
    private static final class Prefs {
        static boolean light;
        /** The shader backdrop, or null for the grid. */
        static MenuScene.Style background;
        static String lastType="", lastId="", lastName="";
        private static final Path FILE=net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("ess-menu.properties");
        static {
            var p=new Properties();
            try(var in=Files.newBufferedReader(FILE)) { p.load(in); } catch(IOException ignored) {}
            light=Boolean.parseBoolean(p.getProperty("light","false"));
            // Older configs only had the New Year switch.
            String bg=p.getProperty("background",Boolean.parseBoolean(p.getProperty("newYear","false")) ? "MOUNTAINS" : "");
            try { background=bg.isEmpty() ? null : MenuScene.Style.valueOf(bg); } catch(IllegalArgumentException e) { background=null; }
            lastType=p.getProperty("lastType","");
            lastId=p.getProperty("lastId","");
            lastName=p.getProperty("lastName","");
        }
        static void last(String type,String id,String name) { lastType=type; lastId=id; lastName=name; save(); }
        static void save() {
            var p=new Properties();
            p.setProperty("light",String.valueOf(light));
            p.setProperty("background",background==null ? "" : background.name());
            p.setProperty("lastType",lastType);
            p.setProperty("lastId",lastId);
            p.setProperty("lastName",lastName);
            try {
                Files.createDirectories(FILE.getParent());
                try(var out=Files.newBufferedWriter(FILE)) { p.store(out,"ESS main menu"); }
            } catch(IOException ignored) {}
        }
    }
}
