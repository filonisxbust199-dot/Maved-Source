package ess.ll.client.module.render;

import com.google.gson.JsonObject;
import ess.ll.client.event.*;
import ess.ll.client.integration.BaritoneVertexConsumerAccess;
import ess.ll.client.module.*;
import ess.ll.client.render.EffectRenderPipelines;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Incremental, palette-filtered searches; never loads chunks or scans the world during rendering. */
public final class BlockESP extends ClientModule {
    public static final BlockESP INSTANCE=new BlockESP();
    private final Map<Block,Integer> colors=new LinkedHashMap<>();
    private final ToggleSetting minecarts=new ToggleSetting("Вагонетка с сундуком").withDefault(true).withContainer(this);
    private final NumberSetting<Integer> range=new NumberSetting<Integer>("Дальность")
        .withMin(16).withMax(128).withStep(16).withDefault(64).withContainer(this);
    private final NumberSetting<Integer> limit=new NumberSetting<Integer>("Лимит блоков")
        .withMin(128).withMax(8192).withStep(128).withDefault(2048).withContainer(this);
    private record Section(int x,int y,int z) {}
    private final Map<Section,List<BlockPos>> cache=new HashMap<>();
    private final ArrayDeque<Section> pending=new ArrayDeque<>();
    private List<Section> ordered=List.of();
    private ClientLevel world;
    private BlockPos center;
    private int previousRange;
    private BlockESP() { super(ModuleCategory.Render,ModuleSubCategory.World); setName("BlockESP"); }
    public static Block resolve(String name) {
        Identifier id=Identifier.tryParse(name.toLowerCase(Locale.ROOT));
        if(id==null || !BuiltInRegistries.BLOCK.containsKey(id))throw new IllegalArgumentException("Неизвестный блок: "+name);
        return BuiltInRegistries.BLOCK.getValue(id);
    }
    public void add(String name,int color) { colors.put(resolve(name),color|0xff000000); reset(); }
    public boolean remove(String name) { boolean found=colors.remove(resolve(name))!=null; reset(); return found; }
    public void clear() { colors.clear(); reset(); }
    public Map<String,Integer> entries() {
        Map<String,Integer> result=new TreeMap<>();
        colors.forEach((b,c)->result.put(BuiltInRegistries.BLOCK.getKey(b).toString(),c)); return result;
    }
    public JsonObject save() { JsonObject data=new JsonObject(); entries().forEach(data::addProperty); return data; }
    public Runnable prepareLoad(JsonObject data) {
        Map<Block,Integer> next=new LinkedHashMap<>();
        if(data!=null)data.entrySet().forEach(e->next.put(resolve(e.getKey()),e.getValue().getAsInt()|0xff000000));
        return ()->{ colors.clear(); colors.putAll(next); reset(); };
    }
    private void reset() { cache.clear(); pending.clear(); ordered=List.of(); center=null; }
    @Override public void onEnable() { reset(); world=null; super.onEnable(); }
    @Override public void onDisable() { reset(); world=null; super.onDisable(); }
    @Subscribe private void tick(TickEvent event) {
        if(mc.level!=world) { reset(); world=mc.level; }
        if(world==null || mc.player==null || colors.isEmpty())return;
        int radius=range.getValue();
        BlockPos player=mc.player.blockPosition();
        if(center==null || center.distSqr(player)>64 || radius!=previousRange || pending.isEmpty()) {
            center=player; previousRange=radius;
            ArrayList<Section> sections=new ArrayList<>();
            for(int x=(player.getX()-radius)>>4;x<=(player.getX()+radius)>>4;x++)
                for(int z=(player.getZ()-radius)>>4;z<=(player.getZ()+radius)>>4;z++)
                    for(int y=Math.max(world.getMinY(),player.getY()-radius)>>4;y<=Math.min(world.getMaxY(),player.getY()+radius)>>4;y++)
                        sections.add(new Section(x,y,z));
            sections.sort(Comparator.comparingDouble(s->BlockPos.containing(s.x*16+8,s.y*16+8,s.z*16+8).distSqr(player)));
            ordered=sections; cache.keySet().retainAll(new HashSet<>(sections)); pending.clear(); pending.addAll(sections);
        }
        long deadline=System.nanoTime()+1_500_000;
        int stored=cache.values().stream().mapToInt(List::size).sum();
        for(int n=0;n<64 && !pending.isEmpty() && System.nanoTime()<deadline;n++) {
            Section key=pending.removeFirst(); var previous=cache.remove(key);
            if(previous!=null)stored-=previous.size();
            int available=limit.getValue()*2-stored;
            if(available<=0)continue;
            var chunk=world.getChunkSource().getChunk(key.x,key.z,ChunkStatus.FULL,false);
            if(chunk==null)continue;
            int index=key.y-(world.getMinY()>>4);
            if(index<0 || index>=chunk.getSections().length)continue;
            var section=chunk.getSections()[index];
            if(!section.maybeHas(state->colors.containsKey(state.getBlock())))continue;
            ArrayList<BlockPos> hits=new ArrayList<>();
            scan: for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                if(colors.containsKey(section.getBlockState(x,y,z).getBlock()))
                    hits.add(new BlockPos(key.x*16+x,key.y*16+y,key.z*16+z));
                if(hits.size()>=available)break scan;
            }
            if(!hits.isEmpty()) { cache.put(key,hits); stored+=hits.size(); }
        }
    }
    @Subscribe private void render(Render3DEvent event) {
        if(world!=mc.level || world==null || mc.player==null)return;
        double maxDistance=range.getValue()*range.getValue(); int count=0;
        outer: for(Section section:ordered) {
            List<BlockPos> hits=cache.get(section); if(hits==null)continue;
            if(!world.hasChunk(section.x,section.z))continue;
            for(BlockPos pos:hits) {
                if(pos.distToCenterSqr(mc.player.position())>maxDistance)continue;
                var state=world.getBlockState(pos); Integer color=colors.get(state.getBlock());
                if(color==null)continue;
                var shape=state.getShape(world,pos);
                AABB box=shape.isEmpty()?new AABB(pos):shape.bounds().move(pos);
                if(!event.getFrustum().isVisible(box))continue;
                outline(event,box.inflate(.002),color);
                if(++count>=limit.getValue())break outer;
            }
        }
        if(minecarts.isEnabled()) {
            float partial=event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            for(var entity:world.entitiesForRendering())if(entity instanceof MinecartChest && !entity.isRemoved()
                && entity.distanceToSqr(mc.player)<=maxDistance) {
                var offset=entity.getPosition(partial).subtract(entity.position());
                AABB box=entity.getBoundingBox().move(offset);
                if(event.getFrustum().isVisible(box))outline(event,box,0xffffffff);
            }
        }
    }
    private static void outline(Render3DEvent event,AABB box,int color) {
        var camera=event.getCamera().position();
        var b=box.move(-camera.x,-camera.y,-camera.z);
        var consumer=(BaritoneVertexConsumerAccess)event.getVertexCollector().getBuffer(EffectRenderPipelines.circleOutline());
        var matrix=event.getPoseStack().last().pose();
        for(int axis=0;axis<3;axis++)for(int a=0;a<2;a++)for(int c=0;c<2;c++) {
            float x=(float)(axis==0?b.minX:a==0?b.minX:b.maxX);
            float y=(float)(axis==1?b.minY:(axis==0?a:c)==0?b.minY:b.maxY);
            float z=(float)(axis==2?b.minZ:c==0?b.minZ:b.maxZ);
            float nx=axis==0?1:0,ny=axis==1?1:0,nz=axis==2?1:0;
            consumer.lineVertex(matrix,x,y,z,color,nx,ny,nz,1.5f);
            consumer.lineVertex(matrix,axis==0?(float)b.maxX:x,axis==1?(float)b.maxY:y,axis==2?(float)b.maxZ:z,color,nx,ny,nz,1.5f);
        }
    }
}
