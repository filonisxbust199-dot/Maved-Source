package ess.ll.client.bots;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.*;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.*;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * One account on a server. A bot is its own network connection that logs in offline and answers the server
 * itself (keep-alives, teleports, chunk batches), so it stays online without a game window. While the player
 * plays as this account, the vanilla listener drives the connection instead ({@link #foreground}).
 */
public final class Bot {
    public enum State { CONNECTING, ONLINE, OFFLINE }
    public enum Action { NONE, FOLLOW, MIRROR }
    private static final int LOG_LINES=60;
    private static final GameProtocols.Context CONTEXT=new GameProtocols.Context() {
        @Override public boolean hasInfiniteMaterials() { return false; }
        @Override public boolean canUseCommandBlocks() { return false; }
    };

    public final String name,address;
    public final UUID uuid;
    /** The account the player started with; it is never deleted, only kept online in the background. */
    public final boolean main;
    public final int color;
    volatile State state=State.CONNECTING;
    volatile String reason="";
    Connection connection;
    final BotRecorder recorder=new BotRecorder();
    final BotWorld world=new BotWorld();
    volatile RegistryAccess.Frozen registries;
    volatile FeatureFlagSet features=FeatureFlags.DEFAULT_FLAGS;
    volatile String brand="vanilla";
    private RegistryDataCollector collector=new RegistryDataCollector();
    private KnownPacksManager knownPacks;
    /** True while the vanilla listener handles this connection (the player is playing as this account). */
    volatile boolean foreground;
    volatile int entityId=-1;
    volatile double x,y,z;
    volatile float yRot,xRot;
    volatile boolean positioned,loaded;
    volatile float health=20;
    volatile int food=20,ping;
    volatile String dimension="";
    volatile Action action=Action.NONE;
    private final Deque<String> log=new ArrayDeque<>();
    private int signedChats,ticks,jumpTicks;
    private boolean mirrorSwing;
    private volatile boolean teleported;
    private double jumpBase;

    Bot(String name,String address,boolean main,int color) {
        this.name=name; this.address=address; this.main=main; this.color=color;
        this.uuid=UUIDUtil.createOfflinePlayerUUID(name);
    }
    /** The player's own session; its connection is the one the game joined with. */
    Bot(String name,UUID uuid,String address,Connection connection,int color) {
        this.name=name; this.address=address; this.main=true; this.color=color; this.uuid=uuid;
        this.connection=connection; this.foreground=true; this.state=State.ONLINE;
    }

    public State state() { return state; }
    public String reason() { return reason; }
    public boolean foreground() { return foreground; }
    public float health() { return health; }
    public int food() { return food; }
    public int ping() { return ping; }
    public int entityId() { return entityId; }
    public Action action() { return action; }
    public String position() { return positioned ? Math.round(x)+" "+Math.round(y)+" "+Math.round(z) : "—"; }
    public String dimension() { return dimension; }
    public synchronized List<String> log() { return List.copyOf(log); }
    synchronized void line(String text) {
        ess.ll.client.module.misc.AutoRwTikva.heard(this,text);
        log.addLast(text);
        while(log.size()>LOG_LINES) log.removeFirst();
    }

    // ---------------------------------------------------------------- connecting

    void connect() {
        state=State.CONNECTING; reason=""; loaded=positioned=false;
        recorder.clear();
        line("Подключаюсь к "+address+"...");
        Thread thread=new Thread(()->{
            try {
                ServerAddress parsed=ServerAddress.parseString(address);
                Optional<InetSocketAddress> resolved=ServerNameResolver.DEFAULT.resolveAddress(parsed).map(ResolvedServerAddress::asInetSocketAddress);
                if(resolved.isEmpty()) { offline("Сервер не найден"); return; }
                var mc=Minecraft.getInstance();
                Connection c=Connection.connectToServer(resolved.get(),EventLoopGroupHolder.remote(mc.options.useNativeTransport()),mc.getDebugOverlay().getBandwidthLogger());
                connection=c;
                Bots.track(this,c);
                collector=new RegistryDataCollector();
                c.initiateServerboundPlayConnection(parsed.getHost(),parsed.getPort(),listener(ClientLoginPacketListener.class,ConnectionProtocol.LOGIN,this::login));
                c.send(new ServerboundHelloPacket(name,uuid));
            } catch(Exception e) {
                offline("Не удалось подключиться: "+e.getMessage());
            }
        },"ESS bot "+name);
        thread.setDaemon(true);
        thread.start();
    }
    void disconnect(String why) {
        action=Action.NONE;
        if(connection!=null && connection.isConnected()) connection.disconnect(Component.literal(why));
        offline(why);
    }
    void offline(String why) {
        if(state==State.OFFLINE) return;
        state=State.OFFLINE; reason=why; foreground=false;
        line("Отключён: "+why);
        Bots.botOffline(this);
    }

    /**
     * A listener of the given protocol that sends every packet to {@code handler}. The interfaces have a method
     * per packet; a proxy lets the bot handle the few it needs and quietly drop the rest.
     */
    @SuppressWarnings("unchecked")
    <T extends PacketListener> T listener(Class<T> type,ConnectionProtocol protocol,Consumer<Packet<?>> handler) {
        InvocationHandler invoke=(proxy,method,args)->switch (method.getName()) {
            case "protocol" -> protocol;
            case "flow" -> PacketFlow.CLIENTBOUND;
            case "isAcceptingMessages" -> connection!=null && connection.isConnected();
            case "shouldHandleMessage" -> true;
            case "onDisconnect" -> { offline(((DisconnectionDetails)args[0]).reason().getString()); yield null; }
            case "onPacketError" -> { line("Ошибка пакета: "+args[1]); yield null; }
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy==args[0];
            case "toString" -> "ESS bot listener "+name;
            default -> {
                if(args!=null && args.length==1 && args[0] instanceof Packet<?> packet) {
                    try { handler.accept(packet); } catch(Exception e) { line("Ошибка: "+e); }
                    yield null;
                }
                if(method.isDefault()) yield InvocationHandler.invokeDefault(proxy,method,args);
                Class<?> r=method.getReturnType();
                yield r==boolean.class ? false : r==int.class ? 0 : r==long.class ? 0L : r==float.class ? 0f : r==double.class ? 0d : null;
            }
        };
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},invoke);
    }

    void send(Packet<?> packet) {
        if(connection!=null && connection.isConnected()) connection.send(packet);
    }

    private boolean common(Packet<?> packet) {
        switch (packet) {
            case ClientboundKeepAlivePacket p -> send(new ServerboundKeepAlivePacket(p.getId()));
            case ClientboundPingPacket p -> send(new ServerboundPongPacket(p.getId()));
            case ClientboundCookieRequestPacket p -> send(new ServerboundCookieResponsePacket(p.key(),null));
            case ClientboundDisconnectPacket p -> { offline(p.reason().getString()); connection.disconnect(p.reason()); }
            case ClientboundResourcePackPushPacket p -> {
                send(new ServerboundResourcePackPacket(p.id(),ServerboundResourcePackPacket.Action.ACCEPTED));
                send(new ServerboundResourcePackPacket(p.id(),ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            }
            case ClientboundCustomPayloadPacket p -> { if(p.payload() instanceof BrandPayload b) brand=b.brand(); }
            case ClientboundTransferPacket p -> disconnect("Сервер перенаправил на другой адрес");
            default -> { return false; }
        }
        return true;
    }

    private void login(Packet<?> packet) {
        if(common(packet)) return;
        switch (packet) {
            case ClientboundHelloPacket p -> disconnect("Сервер требует лицензионный аккаунт");
            case ClientboundLoginCompressionPacket p -> connection.setupCompression(p.getCompressionThreshold(),false);
            case ClientboundCustomQueryPacket p -> send(new ServerboundCustomQueryAnswerPacket(p.transactionId(),null));
            case ClientboundLoginDisconnectPacket p -> { offline(p.reason().getString()); connection.disconnect(p.reason()); }
            case ClientboundLoginFinishedPacket p -> {
                connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND,configurationListener());
                connection.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
                connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
                connection.send(new ServerboundCustomPayloadPacket(new BrandPayload("vanilla")));
                connection.send(new ServerboundClientInformationPacket(ClientInformation.createDefault()));
            }
            default -> {}
        }
    }
    ClientConfigurationPacketListener configurationListener() {
        return listener(ClientConfigurationPacketListener.class,ConnectionProtocol.CONFIGURATION,this::configuration);
    }

    private void configuration(Packet<?> packet) {
        if(common(packet)) return;
        switch (packet) {
            case ClientboundRegistryDataPacket p -> collector.appendContents(p.registry(),p.entries());
            case ClientboundUpdateTagsPacket p -> collector.appendTags(p.tags());
            case ClientboundUpdateEnabledFeaturesPacket p -> features=FeatureFlags.REGISTRY.fromNames(p.features());
            case ClientboundSelectKnownPacks p -> {
                if(knownPacks==null) knownPacks=new KnownPacksManager();
                send(new ServerboundSelectKnownPacks(knownPacks.trySelectingPacks(p.knownPacks())));
            }
            case ClientboundCodeOfConductPacket p -> send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
            case ClientboundFinishConfigurationPacket p -> {
                RegistryAccess.Frozen base=ClientRegistryLayer.createRegistryAccess().compositeAccess();
                if(knownPacks==null) registries=collector.collectGameRegistries(ResourceProvider.EMPTY,base,false);
                else try(var resources=knownPacks.createResourceManager()) { registries=collector.collectGameRegistries(resources,base,false); }
                collector=new RegistryDataCollector();
                connection.setupInboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries)),gameListener());
                connection.send(ServerboundFinishConfigurationPacket.INSTANCE);
                connection.setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries),CONTEXT));
            }
            default -> {}
        }
    }
    ClientGamePacketListener gameListener() {
        return listener(ClientGamePacketListener.class,ConnectionProtocol.PLAY,this::game);
    }

    /** Headless game handling; the recorder already saw the packet (BotConnectionMixin). */
    void game(Packet<?> packet) {
        if(common(packet)) return;
        switch (packet) {
            case ClientboundBundlePacket p -> { for(var sub:p.subPackets()) game(sub); }
            case ClientboundLoginPacket p -> {
                entityId=p.playerId();
                var info=p.commonPlayerSpawnInfo();
                world.reset(info.dimensionType().value(),registries);
                dimension=dimensionName(info.dimension().identifier().getPath());
                state=State.ONLINE;
                line("Зашёл на сервер");
                Bots.botOnline(this);
            }
            case ClientboundRespawnPacket p -> {
                var info=p.commonPlayerSpawnInfo();
                world.reset(info.dimensionType().value(),registries);
                dimension=dimensionName(info.dimension().identifier().getPath());
                positioned=false;
            }
            case ClientboundPlayerPositionPacket p -> {
                var now=new PositionMoveRotation(new Vec3(x,y,z),Vec3.ZERO,yRot,xRot);
                var next=PositionMoveRotation.calculateAbsolute(now,p.change(),p.relatives());
                x=next.position().x; y=next.position().y; z=next.position().z; yRot=next.yRot(); xRot=next.xRot();
                positioned=true;
                send(new ServerboundAcceptTeleportationPacket(p.id(),x,y,z,yRot,xRot));
                // The confirming position goes out with the next tick: one position per client tick.
                teleported=true;
            }
            case ClientboundSetHealthPacket p -> {
                health=p.getHealth(); food=p.getFood();
                if(health<=0) { line("Умер — возрождаюсь"); send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)); }
            }
            case ClientboundPlayerCombatKillPacket p -> {
                if(p.playerId()==entityId) { health=0; line("Умер — возрождаюсь"); send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)); }
            }
            case ClientboundSystemChatPacket p -> { if(!p.overlay()) line(p.content().getString()); }
            case ClientboundPlayerChatPacket p -> {
                Component body=p.unsignedContent().orElse(Component.literal(p.body().content()));
                line(p.chatType().decorate(body).getString());
                if(p.signature().isPresent() && ++signedChats>=32) { send(new ServerboundChatAckPacket(signedChats)); signedChats=0; }
            }
            case ClientboundDisguisedChatPacket p -> line(p.chatType().decorate(p.message()).getString());
            case ClientboundChunkBatchFinishedPacket p -> send(new ServerboundChunkBatchReceivedPacket(20));
            case ClientboundLevelChunkWithLightPacket p -> world.chunk(p.x(),p.z(),p.chunkData());
            case ClientboundForgetLevelChunkPacket p -> world.forget(p.pos().pack());
            case ClientboundBlockUpdatePacket p -> world.set(p.getPos(),p.getBlockState());
            case ClientboundSectionBlocksUpdatePacket p -> p.runUpdates(world::set);
            case ClientboundAddEntityPacket p -> { if(p.getType()==net.minecraft.world.entity.EntityTypes.PLAYER) world.player(p.getId(),p.getX(),p.getY(),p.getZ()); }
            case ClientboundTeleportEntityPacket p -> {
                var to=p.change().position();
                if(p.relatives().isEmpty()) world.player(p.id(),to.x,to.y,to.z);
            }
            case ClientboundEntityPositionSyncPacket p -> { var to=p.position().endPosition(); world.playerIfKnown(p.id(),to.x,to.y,to.z); }
            case ClientboundMoveEntityPacket p when p.hasPosition() -> {
                world.move(((ess.ll.client.mixin.MoveEntityPacketAccessor)p).ess$entityId(),p.getPositionDelta());
            }
            case ClientboundRemoveEntitiesPacket p -> p.entityIds().forEach(world::remove);
            case ClientboundPlayerInfoUpdatePacket p -> {
                for(var entry:p.entries()) if(entry.profileId().equals(uuid) && p.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY)) ping=entry.latency();
            }
            case ClientboundStartConfigurationPacket p -> {
                connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND,configurationListener());
                connection.send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
                connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
            }
            default -> {}
        }
    }
    private static String dimensionName(String path) {
        return switch (path) { case "overworld" -> "Верхний мир"; case "the_nether" -> "Незер"; case "the_end" -> "Энд"; default -> path; };
    }

    // ---------------------------------------------------------------- acting

    /** Main thread, every client tick, while the bot plays in the background. */
    void tick(Minecraft mc) {
        if(connection==null || foreground) return;
        connection.tick();
        // A dead background bot asks to respawn again now and then, in case the first request came too early.
        if(state==State.ONLINE && health<=0 && ++ticks%40==0) send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        if(state!=State.ONLINE || !positioned) return;
        ticks++;
        var me=mc.player;
        boolean together=me!=null && Bots.sameWorld(this);
        boolean moved=false;
        if(action==Action.FOLLOW && together) {
            double dx=me.getX()-x,dz=me.getZ()-z,dist=Math.sqrt(dx*dx+dz*dz);
            if(dist>2.5) {
                double step=Math.min(.21,dist-2.5);
                x+=dx/dist*step; z+=dz/dist*step;
                y+=Math.clamp(me.getY()-y,-.4,.4);
                yRot=(float)Math.toDegrees(Math.atan2(-dx,dz));
                moved=true;
            }
        } else if(action==Action.MIRROR && together) {
            double dx=me.getX()-me.xo,dy=me.getY()-me.yo,dz=me.getZ()-me.zo;
            if(dx*dx+dy*dy+dz*dz>1e-6 || yRot!=me.getYRot() || xRot!=me.getXRot()) {
                x+=dx; y+=dy; z+=dz; yRot=me.getYRot(); xRot=me.getXRot();
                moved=true;
            }
            boolean swinging=me.isSwinging();
            if(swinging && !mirrorSwing) send(ServerboundPunchPacket.INSTANCE);
            mirrorSwing=swinging;
        }
        if(work()) moved=true;
        if(jumpTicks>0) {
            jumpTicks--;
            int t=12-jumpTicks;
            y=jumpBase+Math.max(0,.42*t-.04*t*t);
            if(jumpTicks==0) y=jumpBase;
            moved=true;
        }
        if(teleported) {
            teleported=false; moved=true;
            if(!loaded) { loaded=true; send(new ServerboundPlayerLoadedPacket()); }
        }
        // A position now and then even when standing, as an idle client would.
        if(moved || ticks%20==0) send(new ServerboundMovePlayerPacket.PosRot(x,y,z,yRot,xRot,jumpTicks==0,false));
        // The server allows one position per client tick and counts ticks by this packet.
        send(ServerboundClientTickEndPacket.INSTANCE);
    }

    /**
     * The console line: "." runs an ESS command and "#" a Baritone command on this client (neither reaches the
     * server), "/" is a server command from this account, anything else is chat from it.
     */
    public void chat(String text) {
        if(text.isBlank()) return;
        line("> "+text);
        if(text.startsWith(".")) { ess.ll.client.command.DotCommands.execute(text,this::line); return; }
        if(text.startsWith("#")) { baritone(text.substring(1).trim()); return; }
        if(foreground) {
            var listener=Minecraft.getInstance().getConnection();
            if(listener!=null) { if(text.startsWith("/")) listener.sendCommand(text.substring(1)); else listener.sendChat(text); }
            return;
        }
        if(state!=State.ONLINE) { line("Бот не в сети"); return; }
        if(text.startsWith("/")) send(new ServerboundChatCommandPacket(text.substring(1)));
        else send(new ServerboundChatPacket(text,Instant.now(),0L,Optional.empty(),new LastSeenMessages.Update(0,new BitSet(),LastSeenMessages.Update.IGNORE_CHECKSUM)));
    }
    /** Baritone drives the player of this client, so it works for the account being played. */
    private void baritone(String command) {
        if(!foreground) { line("Baritone ведёт только аккаунт, за который играешь: нажми «Играть»"); return; }
        if(command.isEmpty()) command="help";
        try {
            boolean done=baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(command);
            line(done ? "Baritone: выполнено, ответ в чате" : "Baritone: неизвестная команда");
        } catch(Throwable e) {
            line("Baritone недоступен: "+e.getMessage());
        }
    }
    public void jump() {
        if(foreground || state!=State.ONLINE || !positioned || jumpTicks>0) return;
        jumpBase=y; jumpTicks=12;
    }
    public void toggle(Action next) { action=action==next ? Action.NONE : next; }

    // ---------------------------------------------------------------- for modules driving a background bot

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public boolean positioned() { return positioned; }
    /** A block this bot was sent, or null when its chunk is not loaded. */
    public net.minecraft.world.level.block.state.BlockState block(net.minecraft.core.BlockPos pos) { return world.blockAt(pos); }
    /** Walks in a straight line to this point (feet height kept), as FOLLOW does; null stops. */
    public void walkTo(Vec3 target) { walkTarget=target; }
    public boolean walking() { return walkTarget!=null; }
    /**
     * Digs a block by hand the way a player does: looks at it, starts, keeps punching, and finishes after the given
     * ticks (the server checks that the dig took long enough). False when the bot is busy, offline or played.
     */
    public boolean dig(net.minecraft.core.BlockPos pos,int ticks) {
        if(foreground || state!=State.ONLINE || !positioned || digPos!=null) return false;
        digPos=pos.immutable(); digTotal=Math.max(1,ticks); digTick=0;
        return true;
    }
    public boolean digging() { return digPos!=null; }
    private Vec3 walkTarget;
    private net.minecraft.core.BlockPos digPos;
    private int digTotal,digTick,sequence;
    /** One step of a walk or a dig; true when the bot moved or turned. */
    private boolean work() {
        boolean moved=false;
        if(walkTarget!=null) {
            double dx=walkTarget.x-x,dz=walkTarget.z-z,dist=Math.sqrt(dx*dx+dz*dz);
            if(dist<.15) walkTarget=null;
            else {
                double step=Math.min(.21,dist);
                x+=dx/dist*step; z+=dz/dist*step;
                yRot=(float)Math.toDegrees(Math.atan2(-dx,dz));
                moved=true;
            }
        }
        if(digPos!=null && walkTarget==null) {
            var centre=Vec3.atCenterOf(digPos);
            double ex=centre.x-x,ey=centre.y-(y+1.62),ez=centre.z-z;
            yRot=(float)Math.toDegrees(Math.atan2(-ex,ez));
            xRot=(float)Math.toDegrees(-Math.atan2(ey,Math.sqrt(ex*ex+ez*ez)));
            var face=net.minecraft.core.Direction.getApproximateNearest(-ex,-ey,-ez);
            if(digTick==0) {
                send(new ServerboundMovePlayerPacket.Rot(yRot,xRot,true,false));
                send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,digPos,face,++sequence));
            }
            if(digTick%4==0) send(ServerboundPunchPacket.INSTANCE);
            if(++digTick>=digTotal) {
                send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,digPos,face,++sequence));
                send(ServerboundPunchPacket.INSTANCE);
                digPos=null;
            }
        }
        return moved;
    }
    /** Hits what the player's crosshair points at, if the bot is close enough to it. */
    public void attack() {
        var mc=Minecraft.getInstance();
        if(foreground || state!=State.ONLINE || !(mc.crosshairPickEntity instanceof net.minecraft.world.entity.Entity target) || !Bots.sameWorld(this)) {
            line("Наведи прицел на цель рядом с ботом"); return;
        }
        if(target.distanceToSqr(x,y+1.6,z)>4.5*4.5) { line("Цель далеко от бота"); return; }
        yRot=(float)Math.toDegrees(Math.atan2(-(target.getX()-x),target.getZ()-z));
        send(new ServerboundMovePlayerPacket.Rot(yRot,xRot,true,false));
        send(new ServerboundAttackPacket(target.getId()));
        send(ServerboundPunchPacket.INSTANCE);
        line("Удар по "+target.getName().getString());
    }

    /** Where the bot's screen is drawn from: its eyes, or the live player when it is being played. */
    double[] eye() {
        var me=Minecraft.getInstance().player;
        if(foreground && me!=null) return new double[]{me.getX(),me.getEyeY(),me.getZ(),me.getYRot(),me.getXRot()};
        return new double[]{x,y+1.62,z,yRot,xRot};
    }
    long chunk() { return ChunkPos.pack((int)Math.floor(x)>>4,(int)Math.floor(z)>>4); }
}
