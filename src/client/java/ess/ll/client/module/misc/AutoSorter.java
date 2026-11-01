package ess.ll.client.module.misc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalGetToBlock;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.Render3DEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.BaritoneVertexConsumerAccess;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.EffectRenderPipelines;
import ess.ll.client.setting.Settings.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * AutoSorter: one source chest where everything is thrown in, and target chests, each with its own items. Turning the
 * module on runs the sort: the client takes from the source what has a target, walks (with Baritone) to each target
 * and puts its items in, and goes back for more until the source holds only items without a target or the targets
 * are full; then the module turns itself off. Only what was taken from the source is put away, never your own items.
 *
 * Marking, looking at a chest: ".sort source" makes it the source; ".sort add [item]" adds the held (or named) item to
 * its filter; ".sort learn" or the Отметить сундук key makes it a target that learns its items from what lies in it
 * the next time it is opened. Marks are kept per server and dimension in config/ess-best/sorter.json.
 */
public final class AutoSorter {
    public static final KeybindSetting MARK=new KeybindSetting("Отметить сундук",-1);
    public static final SliderSetting DELAY=new SliderSetting("Задержка кликов (мс)",0,300,5,60);
    public static final BooleanSetting WALK=new BooleanSetting("Ходить к сундукам",true);
    public static final BooleanSetting HIGHLIGHT=new BooleanSetting("Подсветка сундуков",true);
    public static final Setting[] SETTINGS={MARK,DELAY,WALK,HIGHLIGHT};
    private static final double REACH=4.4;
    private static final int OPEN_TIMEOUT=60,WALK_TIMEOUT=20*60,SETTLE=4;
    private static final int SOURCE_COLOR=0xffffa53d,TARGET_COLOR=0xff4fe38a,LEARN_COLOR=0xffffe066;
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("ess-best").resolve("sorter.json");

    /** A target chest: its items, or learn = take them from its contents when it is next opened. */
    public static final class Target {
        final BlockPos pos;
        final Set<String> items=new LinkedHashSet<>();
        boolean learn,full;
        Target(BlockPos pos) { this.pos=pos; }
        boolean accepts(Item item) { return !full && items.contains(id(item)); }
    }
    /** The marks of one server and dimension. */
    private static final class Layout {
        final String server,dimension;
        BlockPos source;
        final List<Target> targets=new ArrayList<>();
        Layout(String server,String dimension) { this.server=server; this.dimension=dimension; }
    }
    private static final List<Layout> LAYOUTS=new ArrayList<>();
    private static boolean loaded,registered,keyDown;

    private enum Stage { IDLE, WALK, OPEN, MENU, CLICKS, SETTLE }
    private static Stage stage=Stage.IDLE;
    private static BlockPos goal;
    private static Target goalTarget;
    private static int timer,menuTicks,moved;
    private static boolean sourceDone,walking;
    private static final Map<Item,Integer> CARRIED=new HashMap<>();
    private record Click(int container,int slot) {}
    private static final Deque<Click> CLICKS=new ArrayDeque<>();
    private record Put(Item item,int count) {}
    private static final Map<Integer,Put> CLICKED=new HashMap<>();
    private static long nextClick;
    private static final AutoSorter LISTENER=new AutoSorter();
    private AutoSorter() {}

    public static boolean enabled() { return ClientModules.AUTO_SORTER!=null && ClientModules.AUTO_SORTER.enabled(); }

    // ---- marks --------------------------------------------------------------------------------------------------
    private static String id(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private static String server() {
        var data=Minecraft.getInstance().getCurrentServer();
        return data!=null ? data.ip.toLowerCase(Locale.ROOT) : "singleplayer";
    }
    private static String dimension() {
        var level=Minecraft.getInstance().level;
        return level==null ? "" : level.dimension().identifier().toString();
    }
    private static Layout layout() {
        load();
        String s=server(),d=dimension();
        for(Layout l:LAYOUTS) if(l.server.equals(s) && l.dimension.equals(d)) return l;
        Layout l=new Layout(s,d);
        LAYOUTS.add(l);
        return l;
    }
    /** One position for both halves of a double chest. */
    private static BlockPos canonical(BlockPos pos) {
        var level=Minecraft.getInstance().level;
        if(level==null) return pos;
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE) {
            BlockPos other=pos.relative(ChestBlock.getConnectedDirection(state));
            return other.asLong()<pos.asLong() ? other : pos;
        }
        return pos;
    }
    private static boolean isContainer(BlockPos pos) {
        var level=Minecraft.getInstance().level;
        return level!=null && level.getBlockEntity(pos) instanceof Container;
    }
    /** The container under the crosshair, or null. */
    private static BlockPos looked() {
        var mc=Minecraft.getInstance();
        if(!(mc.hitResult instanceof BlockHitResult hit) || hit.getType()!=HitResult.Type.BLOCK) return null;
        return isContainer(hit.getBlockPos()) ? canonical(hit.getBlockPos()) : null;
    }
    private static Target target(Layout l,BlockPos pos) {
        for(Target t:l.targets) if(t.pos.equals(pos)) return t;
        return null;
    }

    /** .sort: marks and runs; returns the lines to print. */
    public static List<String> command(List<String> a) {
        List<String> out=new ArrayList<>();
        Layout l=layout();
        String sub=a.size()>1 ? a.get(1).toLowerCase(Locale.ROOT) : "help";
        BlockPos pos=looked();
        switch (sub) {
            case "source", "src", "источник" -> {
                if(pos==null) { out.add("Посмотри на сундук"); break; }
                l.targets.removeIf(t->t.pos.equals(pos));
                l.source=pos;
                out.add("Источник: "+text(pos));
            }
            case "add" -> {
                if(pos==null) { out.add("Посмотри на сундук"); break; }
                if(pos.equals(l.source)) { out.add("Это источник: сначала .sort remove"); break; }
                String item;
                if(a.size()>2) {
                    var parsed=Identifier.tryParse(a.get(2).contains(":") ? a.get(2) : "minecraft:"+a.get(2));
                    if(parsed==null || BuiltInRegistries.ITEM.getOptional(parsed).isEmpty()) { out.add("Нет предмета "+a.get(2)); break; }
                    item=parsed.toString();
                } else {
                    var held=Minecraft.getInstance().player==null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem();
                    if(held.isEmpty()) { out.add("Возьми предмет в руку или напиши .sort add <id>"); break; }
                    item=id(held.getItem());
                }
                Target t=target(l,pos);
                if(t==null) { t=new Target(pos); l.targets.add(t); }
                t.items.add(item);
                out.add("Сундук "+text(pos)+": "+String.join(", ",t.items));
            }
            case "learn" -> {
                if(pos==null) { out.add("Посмотри на сундук"); break; }
                out.add(learnMark(l,pos));
            }
            case "remove", "del" -> {
                if(pos==null) { out.add("Посмотри на сундук"); break; }
                boolean was=l.targets.removeIf(t->t.pos.equals(pos));
                if(pos.equals(l.source)) { l.source=null; was=true; }
                out.add(was ? "Отметка снята" : "Этот сундук не отмечен");
            }
            case "list" -> {
                out.add("Источник: "+(l.source==null ? "не задан" : text(l.source)));
                for(Target t:l.targets) out.add(text(t.pos)+" → "+(t.learn ? "запомнит содержимое при открытии" : String.join(", ",t.items)));
                if(l.targets.isEmpty()) out.add("Сундуков-целей нет");
            }
            case "clear" -> { l.source=null; l.targets.clear(); out.add("Все отметки этого мира сняты"); }
            case "start", "run" -> { ClientModules.AUTO_SORTER.setEnabled(true); out.add("Сортировка запущена"); }
            case "stop" -> { ClientModules.AUTO_SORTER.setEnabled(false); out.add("Сортировка остановлена"); }
            default -> {
                out.add(".sort source — сундук, где всё вперемешку");
                out.add(".sort add [предмет] — добавить предмет в цель (из руки или по id)");
                out.add(".sort learn — цель запомнит свои предметы при открытии");
                out.add(".sort remove / list / clear / start / stop");
            }
        }
        save();
        return out;
    }
    private static String learnMark(Layout l,BlockPos pos) {
        if(pos.equals(l.source)) return "Это источник: сначала .sort remove";
        Target t=target(l,pos);
        if(t==null) { t=new Target(pos); l.targets.add(t); }
        t.learn=true;
        return "Открой сундук "+text(pos)+": он запомнит лежащие в нём предметы";
    }
    private static String text(BlockPos p) { return p.getX()+" "+p.getY()+" "+p.getZ(); }

    // ---- tick ---------------------------------------------------------------------------------------------------
    public static void tick(Minecraft mc) {
        if(!registered) { registered=true; EventBus.register(LISTENER); }
        var player=mc.player;
        if(player==null || mc.level==null) { if(stage!=Stage.IDLE) stop(null); return; }
        // The mark key: a chest becomes a learning target, a marked one loses its mark.
        boolean down=MARK.get()>=0 && mc.gui.screen()==null && ess.ll.client.command.Keys.isDown(MARK.get());
        if(down && !keyDown) markKey(mc);
        keyDown=down;
        learnOpened(mc);
        if(!enabled()) { if(stage!=Stage.IDLE) stop(null); return; }
        if(stage==Stage.IDLE) start(mc);
        else run(mc);
    }
    private static void markKey(Minecraft mc) {
        BlockPos pos=looked();
        if(pos==null) return;
        Layout l=layout();
        String message;
        if(target(l,pos)!=null || pos.equals(l.source)) {
            l.targets.removeIf(t->t.pos.equals(pos));
            if(pos.equals(l.source)) l.source=null;
            message="AutoSorter: отметка снята";
        } else message="AutoSorter: "+learnMark(l,pos);
        save();
        say(message.substring("AutoSorter: ".length()));
    }
    /** A learning target that is open: its items become its filter. */
    private static void learnOpened(Minecraft mc) {
        var menu=mc.player.containerMenu;
        if(menu instanceof InventoryMenu) { menuTicks=0; return; }
        menuTicks++;
        BlockPos pos=stage==Stage.IDLE ? looked() : goal;
        if(pos==null || menuTicks<SETTLE) return;
        Target t=target(layout(),pos);
        if(t==null || !t.learn) return;
        for(Slot slot:menu.slots) if(!(slot.container instanceof Inventory) && slot.hasItem()) t.items.add(id(slot.getItem().getItem()));
        if(t.items.isEmpty()) return;
        t.learn=false;
        save();
        say("Сундук "+text(pos)+" запомнил: "+String.join(", ",t.items));
    }

    private static void start(Minecraft mc) {
        Layout l=layout();
        if(l.source==null || l.targets.isEmpty()) {
            say(l.source==null ? "Нет сундука-источника: посмотри на него и напиши .sort source" : "Нет сундуков-целей: .sort add или .sort learn");
            ClientModules.AUTO_SORTER.setEnabled(false);
            return;
        }
        for(Target t:l.targets) t.full=false;
        CARRIED.clear(); CLICKS.clear();
        sourceDone=false; moved=0;
        next(mc);
    }
    /** Where to go next: a target for what you carry, else the source, else done. */
    private static void next(Minecraft mc) {
        Layout l=layout();
        var player=mc.player;
        CARRIED.entrySet().removeIf(e->e.getValue()<=0);
        Target best=null;
        double bestDist=Double.MAX_VALUE;
        for(Target t:l.targets) {
            boolean wanted=t.learn && t.items.isEmpty() ? false : CARRIED.keySet().stream().anyMatch(t::accepts);
            if(!wanted) continue;
            double d=player.distanceToSqr(Vec3.atCenterOf(t.pos));
            if(d<bestDist) { bestDist=d; best=t; }
        }
        // Carried items whose every target is full stay in your inventory.
        if(best==null && !CARRIED.isEmpty()) {
            say("Нет места для: "+String.join(", ",CARRIED.keySet().stream().map(AutoSorter::id).toList())+" — осталось у тебя");
            CARRIED.clear();
        }
        if(best!=null) go(best.pos,best);
        else if(!sourceDone) go(l.source,null);
        else stop("Готово: разложено предметов — "+moved);
    }
    private static void go(BlockPos pos,Target target) {
        goal=pos; goalTarget=target; timer=0; walking=false;
        stage=Stage.WALK;
    }
    private static void stop(String message) {
        var mc=Minecraft.getInstance();
        if(walking) cancelWalk();
        walking=false;
        if(stage!=Stage.IDLE && mc.player!=null && !(mc.player.containerMenu instanceof InventoryMenu)) mc.player.closeContainer();
        stage=Stage.IDLE; goal=null; goalTarget=null; CLICKS.clear();
        if(message!=null) say(message);
        if(enabled()) ClientModules.AUTO_SORTER.setEnabled(false);
    }

    private static void run(Minecraft mc) {
        var player=mc.player;
        timer++;
        switch (stage) {
            case WALK -> {
                if(!isContainer(goal)) { stop("Сундук "+text(goal)+" пропал"); return; }
                if(inReach(goal)) {
                    if(walking) { cancelWalk(); walking=false; }
                    stage=Stage.OPEN; timer=0;
                } else if(!WALK.get()) stop("Сундук "+text(goal)+" далеко: подойди или включи «Ходить к сундукам»");
                else if(timer>WALK_TIMEOUT) stop("Не дойти до сундука "+text(goal));
                else if(!walking || timer%40==0 && !BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive()) {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(goal));
                    walking=true;
                }
            }
            case OPEN -> {
                if(!(player.containerMenu instanceof InventoryMenu)) player.closeContainer();
                Vec3 centre=Vec3.atCenterOf(goal),eye=player.getEyePosition(),d=centre.subtract(eye);
                player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));
                player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));
                if(player.isShiftKeyDown()) mc.options.keyShift.setDown(false);
                Direction face=Direction.getApproximateNearest(-d.x,-d.y,-d.z);
                mc.gameMode.useItemOn(player,InteractionHand.MAIN_HAND,new BlockHitResult(centre.add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5),face,goal,false));
                player.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
                stage=Stage.MENU; timer=0;
            }
            case MENU -> {
                if(player.containerMenu instanceof InventoryMenu || timer<SETTLE) {
                    if(timer>OPEN_TIMEOUT) stop("Сундук "+text(goal)+" не открылся");
                    return;
                }
                if(goalTarget==null) take(mc); else put(mc);
                stage=Stage.CLICKS; timer=0; nextClick=0;
            }
            case CLICKS -> {
                long now=System.currentTimeMillis();
                long delay=Math.round(DELAY.get());
                while(!CLICKS.isEmpty() && now>=nextClick) {
                    Click c=CLICKS.poll();
                    if(player.containerMenu.containerId!=c.container) { stop("Сундук закрылся во время сортировки"); return; }
                    mc.gameMode.handleContainerInput(c.container,c.slot,0,ContainerInput.QUICK_MOVE,player);
                    if(delay>0) { nextClick=now+delay; break; }
                }
                if(CLICKS.isEmpty()) { stage=Stage.SETTLE; timer=0; }
            }
            case SETTLE -> {
                if(timer<SETTLE) return;
                if(goalTarget!=null) settlePut(mc);
                player.closeContainer();
                stage=Stage.IDLE;
                next(mc);
            }
            default -> {}
        }
    }
    private static boolean inReach(BlockPos pos) {
        var player=Minecraft.getInstance().player;
        return player.getEyePosition().distanceTo(Vec3.atCenterOf(pos))<=REACH;
    }
    private static void cancelWalk() {
        try { BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything(); } catch(Throwable ignored) {}
    }

    /** At the source: as many stacks with a target as your inventory has room for. */
    private static void take(Minecraft mc) {
        var player=mc.player;
        var menu=player.containerMenu;
        Layout l=layout();
        int free=0;
        for(int i=0;i<36;i++) if(player.getInventory().getItem(i).isEmpty()) free++;
        int sortable=0,queued=0;
        for(Slot slot:menu.slots) {
            if(slot.container instanceof Inventory || !slot.hasItem()) continue;
            Item item=slot.getItem().getItem();
            if(l.targets.stream().noneMatch(t->t.accepts(item))) continue;
            sortable++;
            if(queued>=free) continue;
            CLICKS.add(new Click(menu.containerId,slot.index));
            CARRIED.merge(item,slot.getItem().getCount(),Integer::sum);
            queued++;
        }
        // All of it fits, or nothing more can go anywhere: this is the last trip to the source.
        if(queued>=sortable) sourceDone=true;
        if(queued==0 && free==0) { sourceDone=true; say("Инвентарь полон: освободи место"); }
    }
    /** At a target: what you carry from the source and the chest takes. */
    private static void put(Minecraft mc) {
        var player=mc.player;
        var menu=player.containerMenu;
        CLICKED.clear();
        if(goalTarget.learn && goalTarget.items.isEmpty()) return;
        Map<Item,Integer> left=new HashMap<>(CARRIED);
        for(Slot slot:menu.slots) {
            if(!(slot.container instanceof Inventory) || !slot.hasItem()) continue;
            Item item=slot.getItem().getItem();
            int carried=left.getOrDefault(item,0);
            if(carried<=0 || !goalTarget.accepts(item)) continue;
            CLICKS.add(new Click(menu.containerId,slot.index));
            CLICKED.put(slot.index,new Put(item,slot.getItem().getCount()));
            left.put(item,carried-slot.getItem().getCount());
        }
    }
    /** After putting: what stayed in your inventory means the chest is full. */
    private static void settlePut(Minecraft mc) {
        var menu=mc.player.containerMenu;
        for(var e:CLICKED.entrySet()) {
            Slot slot=menu.getSlot(e.getKey());
            Put put=e.getValue();
            int after=slot.hasItem() && slot.getItem().getItem()==put.item ? slot.getItem().getCount() : 0,done=put.count-after;
            if(after>0) goalTarget.full=true;
            if(done>0) { CARRIED.merge(put.item,-done,Integer::sum); moved+=done; }
        }
    }

    private static void say(String message) {
        if(Minecraft.getInstance().player!=null) ess.ll.client.command.DotCommands.say("AutoSorter: "+message);
    }

    // ---- highlight ----------------------------------------------------------------------------------------------
    @Subscribe
    private void render(Render3DEvent event) {
        var mc=Minecraft.getInstance();
        if(!HIGHLIGHT.get() || mc.level==null || mc.player==null) return;
        Layout l=layout();
        if(l.source!=null) outline(event,box(l.source),SOURCE_COLOR);
        for(Target t:l.targets) outline(event,box(t.pos),t.learn ? LEARN_COLOR : TARGET_COLOR);
    }
    private static AABB box(BlockPos pos) {
        var level=Minecraft.getInstance().level;
        AABB box=new AABB(pos);
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE)
            box=box.minmax(new AABB(pos.relative(ChestBlock.getConnectedDirection(state))));
        return box.inflate(.004);
    }
    private static void outline(Render3DEvent event,AABB box,int color) {
        if(!event.getFrustum().isVisible(box)) return;
        var camera=event.getCamera().position();
        var b=box.move(-camera.x,-camera.y,-camera.z);
        var consumer=(BaritoneVertexConsumerAccess)event.getVertexCollector().getBuffer(EffectRenderPipelines.circleOutline());
        var matrix=event.getPoseStack().last().pose();
        for(int axis=0;axis<3;axis++)for(int a=0;a<2;a++)for(int c=0;c<2;c++) {
            float x=(float)(axis==0?b.minX:a==0?b.minX:b.maxX);
            float y=(float)(axis==1?b.minY:(axis==0?a:c)==0?b.minY:b.maxY);
            float z=(float)(axis==2?b.minZ:c==0?b.minZ:b.maxZ);
            float nx=axis==0?1:0,ny=axis==1?1:0,nz=axis==2?1:0;
            consumer.lineVertex(matrix,x,y,z,color,nx,ny,nz,2f);
            consumer.lineVertex(matrix,axis==0?(float)b.maxX:x,axis==1?(float)b.maxY:y,axis==2?(float)b.maxZ:z,color,nx,ny,nz,2f);
        }
    }

    // ---- storage ------------------------------------------------------------------------------------------------
    private static void load() {
        if(loaded) return;
        loaded=true;
        try {
            if(!Files.exists(FILE)) return;
            for(var e:JsonParser.parseString(Files.readString(FILE,StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject o=e.getAsJsonObject();
                Layout l=new Layout(o.get("server").getAsString(),o.get("dimension").getAsString());
                if(o.has("source")) l.source=BlockPos.of(o.get("source").getAsLong());
                for(var te:o.getAsJsonArray("targets")) {
                    JsonObject to=te.getAsJsonObject();
                    Target t=new Target(BlockPos.of(to.get("pos").getAsLong()));
                    t.learn=to.has("learn") && to.get("learn").getAsBoolean();
                    for(var i:to.getAsJsonArray("items")) t.items.add(i.getAsString());
                    l.targets.add(t);
                }
                LAYOUTS.add(l);
            }
        } catch(Exception ex) { System.err.println("[ESS] sorter.json not read: "+ex); }
    }
    private static void save() {
        try {
            JsonArray list=new JsonArray();
            for(Layout l:LAYOUTS) {
                if(l.source==null && l.targets.isEmpty()) continue;
                JsonObject o=new JsonObject();
                o.addProperty("server",l.server); o.addProperty("dimension",l.dimension);
                if(l.source!=null) o.addProperty("source",l.source.asLong());
                JsonArray targets=new JsonArray();
                for(Target t:l.targets) {
                    JsonObject to=new JsonObject();
                    to.addProperty("pos",t.pos.asLong());
                    if(t.learn) to.addProperty("learn",true);
                    JsonArray items=new JsonArray();
                    t.items.forEach(items::add);
                    to.add("items",items);
                    targets.add(to);
                }
                o.add("targets",targets);
                list.add(o);
            }
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling("sorter.json.tmp");
            Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(list),StandardCharsets.UTF_8);
            Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } catch(Exception ex) { System.err.println("[ESS] sorter.json not saved: "+ex); }
    }

    /** Dev check: marks a source (no items) or a target with its items in the current world. */
    public static void devMark(BlockPos pos,String... items) {
        Layout l=layout();
        BlockPos c=canonical(pos);
        l.targets.removeIf(t->t.pos.equals(c));
        if(items.length==0) l.source=c;
        else { Target t=new Target(c); t.items.addAll(List.of(items)); l.targets.add(t); }
    }
    /** Dev check: the stage of the run. */
    public static String stage() { return stage.name(); }
}
