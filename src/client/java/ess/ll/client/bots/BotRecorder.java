package ess.ll.client.bots;

import ess.ll.client.mixin.BossEventPacketAccessor;
import ess.ll.client.mixin.MoveEntityPacketAccessor;
import ess.ll.client.mixin.RotateHeadPacketAccessor;
import ess.ll.client.mixin.SectionBlocksUpdatePacketAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.ChunkPos;

/**
 * Keeps the packets that describe the current state of the world for one connection: the login, chunks, entities,
 * inventory, player list and the like. Transient packets (sounds, particles, chat, animations) are not kept.
 * When the player switches to this connection, the packets are handed to a fresh vanilla listener in arrival
 * order, so it ends up with the same world the server built for this account, without a relog.
 * Packets come from the netty thread and the replay runs on the main thread, so everything is synchronized.
 */
final class BotRecorder {
    /** Key → packet; re-putting a key moves it to the end, so a newer state replaces the old one in place of order. */
    private final LinkedHashMap<String,Packet<?>> kept=new LinkedHashMap<>();
    private final Map<Long,List<String>> byChunk=new HashMap<>();
    private final Map<Integer,List<String>> byEntity=new HashMap<>();
    private long seq;

    synchronized void record(Packet<?> packet) {
        switch (packet) {
            case ClientboundLoginPacket p -> { kept.clear(); byChunk.clear(); byEntity.clear(); put("login",p); }
            case ClientboundRespawnPacket p -> { dropWorld(); put("respawn",p); }
            case ClientboundLevelChunkWithLightPacket p -> {
                long chunk=ChunkPos.pack(p.x(),p.z());
                dropChunk(chunk);
                putChunk(chunk,"chunk:"+chunk,p);
            }
            case ClientboundForgetLevelChunkPacket p -> dropChunk(p.pos().pack());
            case ClientboundBlockUpdatePacket p -> putChunk(ChunkPos.pack(p.getPos().getX()>>4,p.getPos().getZ()>>4),"block:"+p.getPos().asLong(),p);
            case ClientboundSectionBlocksUpdatePacket p -> {
                var section=((SectionBlocksUpdatePacketAccessor)p).ess$sectionPos();
                putChunk(ChunkPos.pack(section.x(),section.z()),"section:"+(seq++),p);
            }
            case ClientboundBlockEntityDataPacket p -> putChunk(ChunkPos.pack(p.getPos().getX()>>4,p.getPos().getZ()>>4),"be:"+p.getPos().asLong(),p);
            case ClientboundLightUpdatePacket p -> putChunk(ChunkPos.pack(p.x(),p.z()),"light:"+(seq++),p);
            case ClientboundChunksBiomesPacket p -> put("biomes:"+(seq++),p);
            case ClientboundAddEntityPacket p -> { dropEntity(p.getId()); putEntity(p.getId(),"add",p); }
            case ClientboundSetEntityDataPacket p -> putEntity(p.id(),"data:"+(seq++),p);
            case ClientboundSetEquipmentPacket p -> putEntity(p.getEntity(),"equip:"+(seq++),p);
            case ClientboundUpdateAttributesPacket p -> putEntity(p.getEntityId(),"attr:"+(seq++),p);
            case ClientboundTeleportEntityPacket p -> { dropMoves(p.id()); putEntity(p.id(),"pos",p); }
            case ClientboundEntityPositionSyncPacket p -> { dropMoves(p.id()); putEntity(p.id(),"pos",p); }
            case ClientboundMoveEntityPacket p -> putEntity(((MoveEntityPacketAccessor)p).ess$entityId(),"move:"+(seq++),p);
            case ClientboundRotateHeadPacket p -> putEntity(((RotateHeadPacketAccessor)p).ess$entityId(),"head",p);
            case ClientboundSetPassengersPacket p -> putEntity(p.getVehicle(),"passengers",p);
            case ClientboundSetEntityLinkPacket p -> putEntity(p.getSourceId(),"link",p);
            case ClientboundUpdateMobEffectPacket p -> putEntity(p.getEntityId(),"effect:"+p.getEffect().getRegisteredName(),p);
            case ClientboundRemoveMobEffectPacket p -> remove(p.entityId(),"effect:"+p.effect().getRegisteredName());
            case ClientboundRemoveEntitiesPacket p -> p.entityIds().forEach(this::dropEntity);
            case ClientboundPlayerPositionPacket p -> put("position",p);
            case ClientboundSetHealthPacket p -> put("health",p);
            case ClientboundSetExperiencePacket p -> put("experience",p);
            case ClientboundPlayerAbilitiesPacket p -> put("abilities",p);
            case ClientboundSetHeldSlotPacket p -> put("held",p);
            case ClientboundContainerSetContentPacket p when p.containerId()==0 -> { dropPrefix("slot:"); put("inventory",p); }
            case ClientboundContainerSetSlotPacket p when p.getContainerId()==0 -> put("slot:"+p.getSlot(),p);
            case ClientboundSetPlayerInventoryPacket p -> put("pinv:"+p.slot(),p);
            case ClientboundSetCursorItemPacket p -> put("cursor",p);
            case ClientboundSetTimePacket p -> put("time",p);
            case ClientboundGameEventPacket p -> put("event:"+p.getEvent().hashCode(),p);
            case ClientboundChangeDifficultyPacket p -> put("difficulty",p);
            case ClientboundSetDefaultSpawnPositionPacket p -> put("spawn",p);
            case ClientboundSetChunkCacheCenterPacket p -> put("center",p);
            case ClientboundSetChunkCacheRadiusPacket p -> put("radius",p);
            case ClientboundSetSimulationDistancePacket p -> put("simulation",p);
            case ClientboundInitializeBorderPacket p -> { dropPrefix("border"); put("border",p); }
            case ClientboundSetBorderCenterPacket p -> put("border:center",p);
            case ClientboundSetBorderSizePacket p -> put("border:size",p);
            case ClientboundSetBorderLerpSizePacket p -> put("border:size",p);
            case ClientboundSetBorderWarningDelayPacket p -> put("border:delay",p);
            case ClientboundSetBorderWarningDistancePacket p -> put("border:distance",p);
            case ClientboundCommandsPacket p -> put("commands",p);
            case ClientboundUpdateRecipesPacket p -> put("recipes",p);
            case ClientboundRecipeBookSettingsPacket p -> put("recipe:settings",p);
            case ClientboundRecipeBookAddPacket p -> put("recipe:"+(seq++),p);
            case ClientboundRecipeBookRemovePacket p -> put("recipe:"+(seq++),p);
            case ClientboundUpdateAdvancementsPacket p -> put("advancements:"+(seq++),p);
            case ClientboundPlayerInfoUpdatePacket p -> put("info:"+(seq++),p);
            case ClientboundPlayerInfoRemovePacket p -> put("info:"+(seq++),p);
            case ClientboundTabListPacket p -> put("tab",p);
            case ClientboundSetObjectivePacket p -> put("score:"+(seq++),p);
            case ClientboundSetScorePacket p -> put("score:"+(seq++),p);
            case ClientboundResetScorePacket p -> put("score:"+(seq++),p);
            case ClientboundSetDisplayObjectivePacket p -> put("score:"+(seq++),p);
            case ClientboundSetPlayerTeamPacket p -> put("team:"+(seq++),p);
            case ClientboundBossEventPacket p -> put("boss:"+((BossEventPacketAccessor)p).ess$id()+":"+(seq++),p);
            case ClientboundCooldownPacket p -> put("cooldown:"+p.cooldownGroup(),p);
            case ClientboundServerDataPacket p -> put("serverdata",p);
            case ClientboundTickingStatePacket p -> put("ticking",p);
            case ClientboundSetCameraPacket p -> put("camera",p);
            default -> {}
        }
    }

    /** Packets that set up the level and its chunk cache; a newer copy moves to the end, yet they must lead. */
    private static final List<String> SETUP=List.of("login","respawn","radius","simulation","center");

    /**
     * The kept packets for the replay: the level setup first, then the rest in arrival order. The chunk cache
     * keeps only chunks near its centre, so the centre must be known before any chunk arrives; otherwise every
     * chunk away from the origin is dropped and the world stays empty.
     */
    synchronized List<Packet<?>> snapshot() {
        List<Packet<?>> list=new ArrayList<>(kept.size());
        for(String key:SETUP) { var p=kept.get(key); if(p!=null) list.add(p); }
        for(var e:kept.entrySet()) if(!SETUP.contains(e.getKey())) list.add(e.getValue());
        return list;
    }
    synchronized boolean hasLogin() { return kept.containsKey("login"); }
    synchronized void clear() { kept.clear(); byChunk.clear(); byEntity.clear(); }

    private void put(String key,Packet<?> packet) { kept.remove(key); kept.put(key,packet); }
    private void putChunk(long chunk,String key,Packet<?> packet) {
        put(key,packet);
        byChunk.computeIfAbsent(chunk,c->new ArrayList<>()).add(key);
    }
    private void putEntity(int id,String part,Packet<?> packet) {
        String key="entity:"+id+":"+part;
        put(key,packet);
        byEntity.computeIfAbsent(id,e->new ArrayList<>()).add(key);
    }
    private void remove(int id,String part) { kept.remove("entity:"+id+":"+part); }
    private void dropChunk(long chunk) {
        var keys=byChunk.remove(chunk);
        if(keys!=null) for(String key:keys) kept.remove(key);
    }
    private void dropEntity(int id) {
        var keys=byEntity.remove(id);
        if(keys!=null) for(String key:keys) kept.remove(key);
    }
    /** An absolute position makes the relative moves before it unnecessary. */
    private void dropMoves(int id) {
        var keys=byEntity.get(id);
        if(keys==null) return;
        keys.removeIf(key->{
            if(!key.contains(":move:")) return false;
            kept.remove(key);
            return true;
        });
    }
    private void dropPrefix(String prefix) { kept.keySet().removeIf(key->key.startsWith(prefix)); }
    /** A new dimension: chunks and entities of the old one are gone; the player state stays. */
    private void dropWorld() {
        Set<String> keep=Set.of("login");
        kept.keySet().removeIf(key->!keep.contains(key) && (key.startsWith("chunk:") || key.startsWith("block:") || key.startsWith("section:")
                || key.startsWith("be:") || key.startsWith("light:") || key.startsWith("biomes:") || key.startsWith("entity:")
                || key.equals("position") || key.equals("respawn") || key.equals("center")));
        byChunk.clear(); byEntity.clear();
    }
}
