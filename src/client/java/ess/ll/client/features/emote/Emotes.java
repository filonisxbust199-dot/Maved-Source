package ess.ll.client.features.emote;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import ess.ll.client.features.emote.EmoteAnimation.Axis;
import ess.ll.client.features.emote.EmoteAnimation.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;

/**
 * Emotes (from Zenith): the 146 built-in Emotecraft animations from assets/ess-best/emotes, which emote each player
 * is playing, and the 120 wheel slots (15 pages of 8) saved in config/ess-best/emotes.json. Playback is purely
 * client side: other players see your emote only if they run the client too and play it themselves.
 */
public final class Emotes {
    public record Emote(String id,String name,String author,Identifier icon,EmoteAnimation animation) {}
    private record Playing(Emote emote,long startNanos) {
        float tick() { return (System.nanoTime()-startNanos)/50_000_000f; }
    }

    public static final int PAGES=15,PER_PAGE=8,SLOTS=PAGES*PER_PAGE;
    /** Render state id used by the picker's preview model, so its emote never touches a real player. */
    public static final int PREVIEW_ID=Integer.MIN_VALUE+7;
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("ess-best").resolve("emotes.json");
    private static final CompletableFuture<Map<String,Emote>> ALL=CompletableFuture.supplyAsync(Emotes::load);
    /** Playing emotes by entity id. */
    private static final Map<Integer,Playing> PLAYING=new ConcurrentHashMap<>();
    private static final String[] SLOT=new String[SLOTS];
    private static boolean slotsLoaded;

    private Emotes() {}

    private static Map<String,Emote> load() {
        Map<String,Emote> out=new LinkedHashMap<>();
        for(String id:resourceLines("/assets/ess-best/emotes/index.txt")) {
            try(InputStream in=Emotes.class.getResourceAsStream("/assets/ess-best/emotes/"+id+".json")) {
                if(in==null) continue;
                JsonObject json=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
                out.put(id,new Emote(id,text(json,"name",title(id)),text(json,"author","Unknown"),
                        Identifier.fromNamespaceAndPath("ess-best","textures/emote/"+id+".png"),EmoteAnimation.parse(json)));
            } catch(IOException | RuntimeException e) { System.err.println("[ESS] emote "+id+" not loaded: "+e); }
        }
        System.out.println("[ESS] loaded "+out.size()+" emotes");
        return out;
    }
    private static List<String> resourceLines(String path) {
        try(InputStream in=Emotes.class.getResourceAsStream(path)) {
            if(in==null) return List.of();
            return new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().map(String::trim).filter(l->!l.isEmpty() && !l.startsWith("#")).toList();
        } catch(IOException e) { return List.of(); }
    }
    /** Names may be plain or a json text with a fallback. */
    private static String text(JsonObject json,String key,String fallback) {
        if(!json.has(key) || !json.get(key).isJsonPrimitive()) return fallback;
        String value=json.get(key).getAsString();
        if(value.isBlank()) return fallback;
        if(value.startsWith("{")) try {
            JsonObject o=JsonParser.parseString(value).getAsJsonObject();
            for(String k:List.of("fallback","text")) if(o.has(k)) return o.get(k).getAsString();
        } catch(RuntimeException ignored) {}
        return value;
    }
    private static String title(String id) {
        List<String> words=new ArrayList<>();
        for(String w:id.split("_")) words.add(w.isEmpty() ? w : Character.toUpperCase(w.charAt(0))+w.substring(1));
        return String.join(" ",words);
    }

    public static Collection<Emote> all() { return ALL.join().values(); }
    public static Emote find(String id) {
        if(id==null || id.isBlank()) return null;
        return ALL.join().get(id.trim().toLowerCase(Locale.ROOT).replace(' ','_'));
    }

    // Playback.
    public static boolean play(String id) {
        var mc=Minecraft.getInstance();
        Emote emote=find(id);
        if(emote==null || mc.player==null) return false;
        PLAYING.put(mc.player.getId(),new Playing(emote,System.nanoTime()));
        return true;
    }
    public static void stop() {
        var mc=Minecraft.getInstance();
        if(mc.player!=null) PLAYING.remove(mc.player.getId());
    }
    public static Emote playing() {
        var mc=Minecraft.getInstance();
        Playing p=mc.player==null ? null : PLAYING.get(mc.player.getId());
        return p==null ? null : p.emote;
    }
    /** The picker's preview model plays this emote from the start; null clears it. */
    public static void preview(Emote emote) {
        if(emote==null) PLAYING.remove(PREVIEW_ID);
        else PLAYING.put(PREVIEW_ID,new Playing(emote,System.nanoTime()));
    }
    /** Client tick: finished emotes end, and the world changing drops everything. */
    public static void tick(Minecraft mc) {
        if(mc.level==null) { PLAYING.keySet().removeIf(id->id!=PREVIEW_ID); return; }
        PLAYING.entrySet().removeIf(e->{
            Playing p=e.getValue();
            if(p.emote.animation().time(p.tick())==null) return e.getKey()!=PREVIEW_ID || restartPreview(e);
            return e.getKey()!=PREVIEW_ID && mc.level.getEntity(e.getKey())==null;
        });
    }
    /** The preview loops: it starts over a moment after it ends. */
    private static boolean restartPreview(Map.Entry<Integer,Playing> e) {
        e.setValue(new Playing(e.getValue().emote,System.nanoTime()));
        return false;
    }

    /** Applies the emote of this render state's entity to the model, after vanilla posed it. */
    public static void apply(int entityId,HumanoidModel<?> model) {
        Playing p=PLAYING.get(entityId);
        if(p==null) return;
        EmoteAnimation a=p.emote.animation();
        EmoteAnimation.Time time=a.time(p.tick());
        if(time==null) return;
        part(a,Part.HEAD,time,model.head);
        part(a,Part.RIGHT_ARM,time,model.rightArm);
        part(a,Part.LEFT_ARM,time,model.leftArm);
        part(a,Part.RIGHT_LEG,time,model.rightLeg);
        part(a,Part.LEFT_LEG,time,model.leftLeg);
        part(a,Part.TORSO,time,model.body);
    }
    private static void part(EmoteAnimation a,Part part,EmoteAnimation.Time time,ModelPart model) {
        if(!a.animates(part)) return;
        model.x=a.sample(part,Axis.X,time,model.x);
        model.y=a.sample(part,Axis.Y,time,model.y);
        model.z=a.sample(part,Axis.Z,time,model.z);
        model.xRot=a.sample(part,Axis.PITCH,time,model.xRot);
        model.yRot=a.sample(part,Axis.YAW,time,model.yRot);
        model.zRot=a.sample(part,Axis.ROLL,time,model.zRot);
    }
    /** The whole-body move of the emote (playerAnimator's "body"), applied after the renderer's own rotations. */
    public static void applyBody(int entityId,PoseStack pose) {
        Playing p=PLAYING.get(entityId);
        if(p==null || !p.emote.animation().animates(Part.BODY)) return;
        EmoteAnimation a=p.emote.animation();
        EmoteAnimation.Time time=a.time(p.tick());
        if(time==null) return;
        pose.translate(a.sample(Part.BODY,Axis.X,time,0),a.sample(Part.BODY,Axis.Y,time,0)+.7f,a.sample(Part.BODY,Axis.Z,time,0));
        pose.rotate(com.mojang.math.Axis.ZP.rotation(a.sample(Part.BODY,Axis.ROLL,time,0)));
        pose.rotate(com.mojang.math.Axis.YP.rotation(a.sample(Part.BODY,Axis.YAW,time,0)));
        pose.rotate(com.mojang.math.Axis.XP.rotation(a.sample(Part.BODY,Axis.PITCH,time,0)));
        pose.translate(0,-.7f,0);
    }

    // Wheel slots.
    public static String slot(int index) {
        loadSlots();
        return index>=0 && index<SLOTS ? SLOT[index] : null;
    }
    /** Puts an emote into a slot; it leaves any other slot it was in. */
    public static void assign(int index,String id) {
        loadSlots();
        if(index<0 || index>=SLOTS || find(id)==null) return;
        for(int i=0;i<SLOTS;i++) if(id.equals(SLOT[i])) SLOT[i]=null;
        SLOT[index]=id;
        saveSlots();
    }
    public static void clear(int index) {
        loadSlots();
        if(index<0 || index>=SLOTS) return;
        SLOT[index]=null;
        saveSlots();
    }
    private static void loadSlots() {
        if(slotsLoaded) return;
        slotsLoaded=true;
        try {
            if(Files.exists(FILE)) {
                JsonArray list=JsonParser.parseString(Files.readString(FILE,StandardCharsets.UTF_8)).getAsJsonArray();
                for(int i=0;i<list.size() && i<SLOTS;i++) {
                    JsonElement e=list.get(i);
                    if(e.isJsonPrimitive() && find(e.getAsString())!=null) SLOT[i]=e.getAsString();
                }
                return;
            }
        } catch(Exception e) { System.err.println("[ESS] emotes.json is broken: "+e.getMessage()); }
        // First start: the first page holds Zenith's own emotes.
        List<String> first=List.of("waving","clap","point","here","backflip","twerk","kazotsky_kick","club_penguin_dance");
        for(int i=0;i<first.size();i++) if(find(first.get(i))!=null) SLOT[i]=first.get(i);
    }
    private static void saveSlots() {
        JsonArray list=new JsonArray();
        for(String id:SLOT) list.add(id==null ? JsonNull.INSTANCE : new JsonPrimitive(id));
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling("emotes.json.tmp");
            Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(list),StandardCharsets.UTF_8);
            Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException e) { System.err.println("[ESS] emotes.json not saved: "+e.getMessage()); }
    }
}
