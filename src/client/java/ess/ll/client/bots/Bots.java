package ess.ll.client.bots;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import ess.ll.client.mixin.MinecraftConnectionAccessor;
import ess.ll.client.mixin.SessionAccessor;
import ess.ll.client.render.HudNotifications;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.Theme;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The Bots tab: the player's own session plus any number of offline bots on servers. Any online account can be
 * played: the one being left keeps its connection and is driven by its bot listener in the background, the one
 * being entered gets a fresh vanilla listener and the recorded state of its world, so neither relogs.
 */
public final class Bots {
    public static final int[] COLORS={0xffe49a3a,0xff4fd18b,0xffb884ff,0xffff6b8b,0xff63c8ff,0xfff2d14b};
    private static final List<Bot> BOTS=new CopyOnWriteArrayList<>();
    private static final Map<Connection,Bot> BY_CONNECTION=new java.util.concurrent.ConcurrentHashMap<>();
    private static Bot main;
    private static CommonListenerCookie mainCookie;
    private static User mainUser;
    private static CompletableFuture<ProfileResult> mainProfile;
    private static ProfileKeyPairManager mainKeys;
    private static ReportingContext mainReporting;
    private static boolean creating,quitting;
    private static int colorIndex;
    /** Ticks since the last switch while the loading screen waits for the player's section to be drawn. */
    private static int switchedAt=-1;
    // The screen of the selected bot, drawn off the main thread a few times a second.
    public static final int VIEW_W=178,VIEW_H=96;
    private static final ExecutorService VIEW=Executors.newSingleThreadExecutor(r->{ Thread t=new Thread(r,"ESS bot view"); t.setDaemon(true); return t; });
    private static final AtomicBoolean RENDERING=new AtomicBoolean();
    private static DynamicTexture viewTexture;
    private static final Identifier VIEW_ID=Identifier.fromNamespaceAndPath("ess-best","bots/view");
    private static long viewAt;

    private Bots() {}

    public static List<Bot> all() { return BOTS; }
    public static Bot main() { return main; }
    public static Bot foreground() {
        for(Bot b:BOTS) if(b.foreground) return b;
        return null;
    }
    /** True while the player plays one of the bots and the main account waits in the background. */
    public static boolean playingAsBot() { Bot f=foreground(); return f!=null && !f.main; }

    // ---------------------------------------------------------------- hooks

    /** A vanilla game listener was created: a normal join makes its account the main session. */
    public static void listenerCreated(ClientPacketListener listener,Connection connection,CommonListenerCookie cookie) {
        if(creating || BY_CONNECTION.containsKey(connection)) return;
        var mc=Minecraft.getInstance();
        // Any join gives the cookie the played bots copy (telemetry, server entry, screen to return to).
        mainCookie=cookie;
        mainUser=mc.getUser();
        mainProfile=((SessionAccessor)mc).ess$profileFuture();
        mainKeys=mc.getProfileKeyPairManager();
        mainReporting=mc.getReportingContext();
        // A singleplayer world cannot be left in the background, so it is not a session of its own.
        if(connection.isMemoryConnection()) return;
        if(main!=null) { BOTS.remove(main); BY_CONNECTION.values().remove(main); }
        var data=cookie.serverData();
        var profile=cookie.localGameProfile();
        main=new Bot(profile.name(),profile.id(),data!=null ? data.ip : "",connection,0xfff2d14b);
        BOTS.addFirst(main);
        BY_CONNECTION.put(connection,main);
    }

    /** Netty thread: game packets of tracked connections go to the recorder (bundles are opened). */
    public static void received(Connection connection,Packet<?> packet) {
        Bot bot=BY_CONNECTION.get(connection);
        if(bot==null) return;
        if(packet instanceof ClientboundBundlePacket bundle) { for(var sub:bundle.subPackets()) bot.recorder.record(sub); }
        else bot.recorder.record(packet);
    }

    static void botOnline(Bot bot) {}
    static void botOffline(Bot bot) {
        if(bot.connection!=null) BY_CONNECTION.remove(bot.connection);
    }

    /** Client tick: background connections are ticked here, and a played bot that drops sends the player back. */
    public static void tick(Minecraft mc) {
        for(Bot bot:BOTS) bot.tick(mc);
        // The loading screen waits for the section under the player to be compiled; with the world already
        // there it is closed as soon as that chunk is in, instead of waiting for its timeout.
        if(switchedAt>=0 && ++switchedAt>10) {
            if(mc.gui.screen() instanceof net.minecraft.client.gui.screens.LevelLoadingScreen && mc.player!=null && mc.level!=null
                    && mc.level.isLoaded(mc.player.blockPosition())) mc.gui.setScreen(null);
            if(!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.LevelLoadingScreen) || switchedAt>200) switchedAt=-1;
        }
        Bot f=foreground();
        if(f!=null && f.connection!=null && !f.connection.isConnected()) {
            f.foreground=false;
            var details=f.connection.getDisconnectionDetails();
            f.offline(details!=null ? details.reason().getString() : "Соединение закрыто");
            if(f.main) { BOTS.remove(f); main=null; }
            else if(!quitting && main!=null && main.state==Bot.State.ONLINE) {
                HudNotifications.queue(Icon.CLOSE,0xffff6b5e,f.name+" отключён","Возвращаю на "+main.name);
                mc.execute(()->switchTo(main));
            }
        }
        if(mc.getConnection()==null && !mc.isLocalServer() && main!=null && !main.foreground && quitting) {
            main.disconnect("Выход из игры");
            BOTS.remove(main); main=null;
        }
        if(mc.getConnection()==null) quitting=false;
    }
    /** From the pause menu: leaving while playing a bot leaves with the main account too. */
    public static void quit() {
        quitting=true;
        if(main!=null && !main.foreground) main.disconnect("Выход из игры");
    }

    // ---------------------------------------------------------------- bots

    public static void add(String name,String address) {
        Bot bot=new Bot(name,address,false,COLORS[colorIndex++%COLORS.length]);
        BOTS.add(bot);
        bot.connect();
    }
    static void track(Bot bot,Connection connection) { BY_CONNECTION.put(connection,bot); }
    public static void reconnect(Bot bot) {
        if(bot.main) return;
        if(bot.foreground) return;
        if(bot.connection!=null) { BY_CONNECTION.remove(bot.connection); if(bot.connection.isConnected()) bot.connection.disconnect(Component.literal("Переподключение")); }
        bot.connection=null;
        bot.state=Bot.State.OFFLINE;
        bot.connect();
    }
    public static void remove(Bot bot) {
        if(bot.main || bot.foreground) return;
        bot.disconnect("Удалён");
        BOTS.remove(bot);
    }
    public static void toggleConnection(Bot bot) {
        if(bot.main || bot.foreground) return;
        if(bot.state==Bot.State.OFFLINE) reconnect(bot); else bot.disconnect("Отключён вручную");
    }

    /** The bot stands in the level the player has loaded: the entity with its id is that very account. */
    static boolean sameWorld(Bot bot) {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || bot.foreground || bot.entityId<0) return false;
        var entity=mc.level.getEntity(bot.entityId);
        return entity!=null && entity.getUUID().equals(bot.uuid);
    }
    private static String dimension(Minecraft mc) {
        return switch (mc.level.dimension().identifier().getPath()) { case "overworld" -> "Верхний мир"; case "the_nether" -> "Незер"; case "the_end" -> "Энд"; default -> mc.level.dimension().identifier().getPath(); };
    }

    // ---------------------------------------------------------------- switching

    public static boolean canSwitch(Bot bot) {
        return !bot.foreground && bot.state==Bot.State.ONLINE && bot.registries!=null && bot.recorder.hasLogin() && mainCookie!=null;
    }

    /** Plays as {@code target}: the current account goes to the background, the target takes the game. */
    public static void switchTo(Bot target) {
        var mc=Minecraft.getInstance();
        if(!canSwitch(target)) return;
        Bot from=foreground();
        var listener=mc.getConnection();
        if(from!=null && listener!=null && mc.player!=null) {
            from.registries=listener.registryAccess();
            from.features=listener.enabledFeatures();
            from.brand=listener.serverBrand();
            from.x=mc.player.getX(); from.y=mc.player.getY(); from.z=mc.player.getZ();
            from.yRot=mc.player.getYRot(); from.xRot=mc.player.getXRot();
            from.health=mc.player.getHealth(); from.food=mc.player.getFoodData().getFoodLevel();
            from.entityId=mc.player.getId();
            from.positioned=from.loaded=true;
            from.dimension=dimension(mc);
            rebuild(from);
            from.foreground=false;
            listen(from.connection,from.gameListener());
            from.line("Ушёл в фон");
        }
        session(target);
        var template=mainCookie;
        var cookie=new CommonListenerCookie(new LevelLoadTracker(),new GameProfile(target.uuid,target.name),template.telemetryManager(),
                target.registries,target.features,target.brand,template.serverData(),template.postDisconnectScreen(),Map.of(),null,
                Map.of(),ServerLinks.EMPTY,new HashMap<>(),true);
        ClientPacketListener vanilla;
        creating=true;
        try { vanilla=new ClientPacketListener(mc,target.connection,cookie); }
        finally { creating=false; }
        listen(target.connection,vanilla);
        ((MinecraftConnectionAccessor)mc).ess$setConnection(target.connection);
        target.foreground=true;
        target.action=Bot.Action.NONE;
        // The login creates a player only when there is none; the previous account's player must not be reused.
        mc.player=null;
        replay(target,vanilla);
        switchedAt=0;
        target.line("Ты играешь за этот аккаунт");
        if(target.main) HudNotifications.queue(Icon.USER,Theme.accent(),"Главный аккаунт",target.name);
        else HudNotifications.queue(Icon.ROBOT,Theme.accent(),"Играешь за "+target.name,main!=null ? "Главный "+main.name+" в фоне" : "");
    }

    private static void listen(Connection connection,net.minecraft.network.PacketListener listener) {
        var access=(ess.ll.client.mixin.ConnectionListenerAccessor)connection;
        access.ess$setPacketListener(listener);
        access.ess$setDisconnectListener(listener);
    }

    /** The recorded world goes to the new listener in order; the position comes last with an id the server never uses. */
    @SuppressWarnings("unchecked")
    private static void replay(Bot target,ClientPacketListener vanilla) {
        for(Packet<?> packet:target.recorder.snapshot()) {
            if(packet instanceof ClientboundPlayerPositionPacket) continue;
            try { ((Packet<ClientGamePacketListener>)packet).handle(vanilla); }
            catch(Exception e) { target.line("Пропущен "+packet.type().id()+": "+e.getMessage()); }
        }
        // The server already had the real teleport accepted; a second accept of that id would kick, so -1 is used.
        var at=new PositionMoveRotation(new Vec3(target.x,target.y,target.z),Vec3.ZERO,target.yRot,target.xRot);
        ClientboundPlayerPositionPacket.of(-1,at,Set.of()).handle(vanilla);
    }

    /** Rebuilds the bot's own picture of the world from its recorder (it was not kept while it was played). */
    private static void rebuild(Bot bot) {
        for(Packet<?> packet:bot.recorder.snapshot()) {
            switch (packet) {
                case ClientboundLoginPacket p -> bot.world.reset(p.commonPlayerSpawnInfo().dimensionType().value(),bot.registries);
                case ClientboundRespawnPacket p -> bot.world.reset(p.commonPlayerSpawnInfo().dimensionType().value(),bot.registries);
                case ClientboundLevelChunkWithLightPacket p -> bot.world.chunk(p.x(),p.z(),p.chunkData());
                case ClientboundBlockUpdatePacket p -> bot.world.set(p.getPos(),p.getBlockState());
                case ClientboundSectionBlocksUpdatePacket p -> p.runUpdates(bot.world::set);
                case ClientboundAddEntityPacket p -> { if(p.getType()==net.minecraft.world.entity.EntityTypes.PLAYER) bot.world.player(p.getId(),p.getX(),p.getY(),p.getZ()); }
                default -> {}
            }
        }
        var mc=Minecraft.getInstance();
        if(mc.level!=null) for(var player:mc.level.players()) if(player!=mc.player) bot.world.player(player.getId(),player.getX(),player.getY(),player.getZ());
    }

    /** The game session (name, keys, reporting) of the account being played. */
    private static void session(Bot target) {
        var mc=Minecraft.getInstance();
        var s=(SessionAccessor)mc;
        if(target.main) {
            s.ess$setUser(mainUser); s.ess$setProfileFuture(mainProfile); s.ess$setProfileKeys(mainKeys); s.ess$setReportingContext(mainReporting);
            return;
        }
        s.ess$setUser(new User(target.name,target.uuid,"",Optional.empty(),Optional.empty()));
        s.ess$setProfileFuture(CompletableFuture.completedFuture(null));
        s.ess$setProfileKeys(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
        s.ess$setReportingContext(ReportingContext.create(ReportEnvironment.local(),UserApiService.OFFLINE));
    }

    // ---------------------------------------------------------------- marks and screen

    /** Name tags: the main account gets a gold "Главный" mark, bots a "Бот" mark, while the player is in the world. */
    public static Component nameTag(Entity entity,Component name) {
        if(name==null || !(entity instanceof Player player) || BOTS.size()<2) return name;
        for(Bot bot:BOTS) {
            if(!bot.uuid.equals(player.getUUID())) continue;
            if(bot.main) return Component.literal("★ Главный ").withStyle(ChatFormatting.GOLD).append(name);
            return Component.literal("Бот ").withStyle(ChatFormatting.AQUA).append(name);
        }
        return name;
    }

    /** Dev check: the bot's view written to a PNG at 4× the panel size. */
    public static void dumpView(Bot bot,String file) {
        int w=VIEW_W*4,h=VIEW_H*4;
        int[] pixels=new int[w*h];
        double[] eye=bot.eye();
        bot.world.render(pixels,w,h,eye[0],eye[1],eye[2],(float)eye[3],(float)eye[4],id->0xfff2d14b);
        var image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,w,h,pixels,0,w);
        try { javax.imageio.ImageIO.write(image,"png",new java.io.File(file)); } catch(Exception e) { System.err.println(e); }
    }

    /** What the panel draws: a texture and the part of it to show. */
    public record View(Identifier id,float u0,float u1,float v0,float v1,boolean real) {}

    /**
     * The bot's screen: the real render when the bot is in the player's world, otherwise the bot's own picture
     * of the world from the chunks it received.
     */
    public static View view(Bot bot) {
        if(BotCamera.canShow(bot)) {
            if(BotCamera.request(bot)) {
                float[] uv=BotCamera.uv((float)VIEW_W/VIEW_H);
                return new View(BotCamera.ID,uv[0],uv[1],uv[2],uv[3],true);
            }
        }
        return new View(sketch(bot),0,1,0,1,false);
    }
    private static Identifier sketch(Bot bot) {
        var mc=Minecraft.getInstance();
        if(viewTexture==null) {
            viewTexture=new DynamicTexture(()->"ess-bot-view",new NativeImage(VIEW_W,VIEW_H,false));
            mc.getTextureManager().register(VIEW_ID,viewTexture);
        }
        long now=System.currentTimeMillis();
        if(now-viewAt>160 && RENDERING.compareAndSet(false,true)) {
            viewAt=now;
            double[] eye=bot.eye();
            BotWorld world=bot.foreground ? null : bot.world;
            VIEW.execute(()->{
                int[] pixels=new int[VIEW_W*VIEW_H];
                try {
                    if(world!=null) world.render(pixels,VIEW_W,VIEW_H,eye[0],eye[1],eye[2],(float)eye[3],(float)eye[4],
                            id->main!=null && id==main.entityId && !main.foreground ? 0xfff2d14b : 0xffe6e9ef);
                } catch(Exception ignored) {}
                mc.execute(()->{
                    var image=viewTexture.getPixels();
                    if(image!=null) {
                        for(int y=0;y<VIEW_H;y++) for(int x=0;x<VIEW_W;x++) {
                            int c=pixels[y*VIEW_W+x];
                            image.setPixel(x,y,c);
                        }
                        viewTexture.upload();
                    }
                    RENDERING.set(false);
                });
            });
        }
        return VIEW_ID;
    }
}
