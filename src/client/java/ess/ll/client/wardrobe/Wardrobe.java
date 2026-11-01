package ess.ll.client.wardrobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ess.ll.client.setting.Settings.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The wardrobe (Zenith's full cosmetics): 680 items in 20 slots — pets, clothing, capes, accessories, character
 * skins and skins for weapons and items — from assets/ess-best/wardrobe. What is worn, the saved outfits and the
 * settings live in config/ess-best/wardrobe.json. Only you see your wardrobe.
 */
public final class Wardrobe {
    public enum Category {
        PET, CLOTHING, CAPE, ACCESSORY, CHARACTER, WEAPON, ITEM;
        public String title() { return text("wardrobe.category."+name().toLowerCase(Locale.ROOT),name()); }
    }
    public enum Slot {
        PET(Category.PET,null,null), HEAD(Category.CLOTHING,null,null), BODY(Category.CLOTHING,null,null), LEGS(Category.CLOTHING,null,null),
        BACK(Category.CLOTHING,null,null), CAPE(Category.CAPE,null,null), WATCH(Category.ACCESSORY,null,null), NECK(Category.ACCESSORY,null,null),
        CHARACTER(Category.CHARACTER,null,null),
        SWORD(Category.WEAPON,ItemTags.SWORDS,null), AXE(Category.WEAPON,ItemTags.AXES,null), BOW(Category.WEAPON,null,Items.BOW),
        MACE(Category.WEAPON,null,Items.MACE), SPEAR(Category.WEAPON,ItemTags.SPEARS,null), CROSSBOW(Category.WEAPON,null,Items.CROSSBOW),
        GAPPLE(Category.ITEM,null,Items.GOLDEN_APPLE), ENCHANTED_GAPPLE(Category.ITEM,null,Items.ENCHANTED_GOLDEN_APPLE),
        TOTEM(Category.ITEM,null,Items.TOTEM_OF_UNDYING), ANCHOR(Category.ITEM,null,Items.RESPAWN_ANCHOR), CRYSTAL(Category.ITEM,null,Items.END_CRYSTAL);
        public final Category category;
        private final TagKey<net.minecraft.world.item.Item> tag;
        private final net.minecraft.world.item.Item item;
        Slot(Category category,TagKey<net.minecraft.world.item.Item> tag,net.minecraft.world.item.Item item) { this.category=category; this.tag=tag; this.item=item; }
        public boolean held() { return category==Category.WEAPON || category==Category.ITEM; }
        public boolean matches(ItemStack stack) {
            return stack!=null && !stack.isEmpty() && (tag!=null && stack.is(tag) || item!=null && stack.is(item));
        }
        /** The vanilla item this slot reskins, for previews. */
        public ItemStack sample() {
            return switch (this) {
                case SWORD -> new ItemStack(Items.DIAMOND_SWORD);
                case AXE -> new ItemStack(Items.DIAMOND_AXE);
                case SPEAR -> new ItemStack(Items.IRON_SPEAR);
                default -> item==null ? ItemStack.EMPTY : new ItemStack(item);
            };
        }
        public String title() { return text("wardrobe.slot."+name().toLowerCase(Locale.ROOT),name()); }
        /** The slot that holds held items of this stack, or null. */
        public static Slot of(ItemStack stack) {
            for(Slot s:values()) if(s.held() && s.matches(stack)) return s;
            return null;
        }
    }
    public enum Rarity {
        COMMON(0xff9aa3ad,false), RARE(0xff4fa3ff,false), EPIC(0xffb36bff,true), LEGENDARY(0xffffb33d,true), MYTHIC(0xffff4f7b,true);
        public final int color;
        /** Epic and above have glowing parts. */
        public final boolean glows;
        Rarity(int color,boolean glows) { this.color=color; this.glows=glows; }
        public String title() { return text("wardrobe.rarity."+name().toLowerCase(Locale.ROOT),name()); }
        static Rarity of(String name) {
            try { return name==null ? COMMON : valueOf(name.strip().toUpperCase(Locale.ROOT)); }
            catch(IllegalArgumentException e) { return COMMON; }
        }
    }
    public record Item(String id,Slot slot,Rarity rarity,String set,String model,String theme) {
        public String name() { return text("wardrobe.item."+id,id); }
        public String description() { return text("wardrobe.item."+id+".desc",""); }
        public Category category() { return slot.category; }
        /** A CS-style knife (Оружие → Мечи → CS), held and animated by CsKnife. */
        public boolean cs() { return theme!=null && theme.startsWith("cs:"); }
    }
    public record OutfitSet(String id,List<String> pieces,int color,String particle) {
        public String name() { return text("wardrobe.set."+id,id); }
        public String bonus() { return text("wardrobe.set."+id+".bonus",""); }
    }
    /** A saved outfit. */
    public record Preset(String name,Map<Slot,String> items,long savedAt) {}

    public static final BooleanSetting PARTICLES=new BooleanSetting("Частицы",true);
    public static final BooleanSetting ARMOR_ON_SKINS=new BooleanSetting("Броня на скинах",false);
    public static final BooleanSetting CLOTHES_OVER_ARMOR=new BooleanSetting("Одежда поверх брони",true);
    public static final SliderSetting PET_SCALE=new SliderSetting("Размер питомца",.5f,2,.05f,1);
    public static final SliderSetting PARTICLE_BUDGET=new SliderSetting("Лимит частиц в секунду",10,200,5,60);
    public static final KeybindSetting INSPECT=new KeybindSetting("Осмотр ножа",com.mojang.blaze3d.platform.InputConstants.KEY_C);
    public static final Setting[] SETTINGS={PARTICLES,ARMOR_ON_SKINS,CLOTHES_OVER_ARMOR,PET_SCALE,PARTICLE_BUDGET,INSPECT};
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("ess-best").resolve("wardrobe.json");
    private static final Map<String,String> LANG=new HashMap<>();
    private static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    private static final Map<String,OutfitSet> SETS=new LinkedHashMap<>();
    private static final Map<String,WardrobeModel> MODELS=new ConcurrentHashMap<>();
    private static final Set<String> LOADING=ConcurrentHashMap.newKeySet(),FAILED=ConcurrentHashMap.newKeySet();
    private static final ExecutorService LOADER=Executors.newFixedThreadPool(2,r->{
        Thread t=new Thread(r,"ess-wardrobe-loader");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static final EnumMap<Slot,String> WORN=new EnumMap<>(Slot.class);
    private static final List<Preset> PRESETS=new ArrayList<>();
    /** The item hovered in the wardrobe tab, shown on the preview in place of what is worn there. */
    private static Item hovered;
    private static boolean previewing,loaded;
    private static int version;

    private Wardrobe() {}

    // Catalogue.
    private static synchronized void load() {
        if(loaded) return;
        loaded=true;
        JsonObject lang=resource("/assets/ess-best/wardrobe/lang_ru.json");
        if(lang!=null) lang.entrySet().forEach(e->LANG.put(e.getKey(),e.getValue().getAsString()));
        JsonObject registry=resource("/assets/ess-best/wardrobe/registry.json");
        if(registry==null) { System.err.println("[ESS] wardrobe registry.json not found"); return; }
        for(JsonElement e:WardrobeModel.array(registry,"items")) {
            if(!e.isJsonObject()) continue;
            JsonObject o=e.getAsJsonObject();
            String id=WardrobeModel.string(o,"id",null);
            Slot slot;
            try { slot=Slot.valueOf(WardrobeModel.string(o,"slot","").toUpperCase(Locale.ROOT)); } catch(IllegalArgumentException ex) { continue; }
            if(id==null) continue;
            String model=WardrobeModel.string(o,"model",id);
            ITEMS.put(id,new Item(id,slot,Rarity.of(WardrobeModel.string(o,"rarity",null)),WardrobeModel.string(o,"set",null),model,WardrobeModel.string(o,"theme",null)));
        }
        for(JsonElement e:WardrobeModel.array(registry,"sets")) {
            if(!e.isJsonObject()) continue;
            JsonObject o=e.getAsJsonObject();
            String id=WardrobeModel.string(o,"id",null);
            if(id==null) continue;
            List<String> pieces=new ArrayList<>();
            for(Item item:ITEMS.values()) if(id.equals(item.set)) pieces.add(item.id);
            int color=0xffffff;
            try { color=Integer.parseInt(WardrobeModel.string(o,"color","#ffffff").replace("#",""),16); } catch(RuntimeException ignored) {}
            SETS.put(id,new OutfitSet(id,Collections.unmodifiableList(pieces),color,WardrobeModel.string(o,"particle",null)));
        }
        loadState();
        System.out.println("[ESS] wardrobe: "+ITEMS.size()+" items, "+SETS.size()+" sets");
    }
    private static JsonObject resource(String path) {
        try(InputStream in=Wardrobe.class.getResourceAsStream(path)) {
            if(in==null) return null;
            JsonElement e=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8));
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch(IOException | RuntimeException e) { System.err.println("[ESS] wardrobe "+path+": "+e); return null; }
    }
    static String text(String key,String fallback) {
        load();
        return LANG.getOrDefault(key,fallback);
    }
    public static List<Item> items() { load(); return List.copyOf(ITEMS.values()); }
    public static List<Item> items(Category category) { return items().stream().filter(i->i.category()==category).toList(); }
    public static List<Item> items(Slot slot) { return items().stream().filter(i->i.slot==slot).toList(); }
    public static Item item(String id) { load(); return id==null ? null : ITEMS.get(id); }
    public static List<OutfitSet> sets() { load(); return List.copyOf(SETS.values()); }
    public static OutfitSet set(String id) { load(); return id==null ? null : SETS.get(id); }

    /** The model of an item, loaded in the background on first use (null until it is ready). */
    public static WardrobeModel model(Item item) {
        if(item==null) return null;
        WardrobeModel model=MODELS.get(item.model);
        if(model==null && !FAILED.contains(item.model) && LOADING.add(item.model)) LOADER.execute(()->loadModel(item.model));
        return model;
    }
    private static void loadModel(String path) {
        try(InputStream in=Wardrobe.class.getResourceAsStream("/assets/ess-best/wardrobe/models/"+path+".json")) {
            if(in==null) throw new IOException("no model file");
            JsonObject json=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            MODELS.put(path,WardrobeModel.parse(path,json,Identifier.fromNamespaceAndPath("ess-best","textures/wardrobe/"+path+".png")));
        } catch(Exception | LinkageError e) {
            FAILED.add(path);
            System.err.println("[ESS] wardrobe model "+path+" not loaded: "+e);
        } finally { LOADING.remove(path); }
    }

    // What is worn.
    public static int version() { return version; }
    public static boolean worn(Item item) { load(); return item!=null && item.id.equals(WORN.get(item.slot)); }
    public static Item worn(Slot slot) { load(); return item(WORN.get(slot)); }
    public static int wornCount() { load(); return WORN.size(); }
    public static void wear(Item item) {
        if(item==null) return;
        load();
        WORN.put(item.slot,item.id);
        // A cape and a back item would sit on top of each other.
        if(item.slot==Slot.CAPE) WORN.remove(Slot.BACK);
        else if(item.slot==Slot.BACK) WORN.remove(Slot.CAPE);
        changed();
    }
    public static void takeOff(Slot slot) { load(); if(WORN.remove(slot)!=null) changed(); }
    public static void toggle(Item item) { if(worn(item)) takeOff(item.slot); else wear(item); }
    /** Wears every piece of a set; a character skin comes off so the set shows. */
    public static void wear(OutfitSet set) {
        if(set==null) return;
        load();
        for(String id:set.pieces) {
            Item item=item(id);
            if(item==null) continue;
            WORN.put(item.slot,item.id);
            if(item.slot==Slot.CAPE) WORN.remove(Slot.BACK);
            else if(item.slot==Slot.BACK) WORN.remove(Slot.CAPE);
        }
        WORN.remove(Slot.CHARACTER);
        changed();
    }
    public static void clear() { load(); if(!WORN.isEmpty()) { WORN.clear(); changed(); } }
    public static void hover(Item item,boolean preview) { hovered=item; previewing=preview; }

    /** What to draw on a wearer: the worn items (with the hovered one tried on in previews), cape or back. */
    public static List<Item> outfit(boolean preview) {
        load();
        List<Item> out=new ArrayList<>(8);
        Item tryOn=preview && previewing ? hovered : null;
        for(Slot slot:Slot.values()) {
            Item item=tryOn!=null && tryOn.slot==slot ? tryOn : item(WORN.get(slot));
            if(item!=null) out.add(item);
        }
        Item cape=find(out,Slot.CAPE),back=find(out,Slot.BACK);
        if(cape!=null && back!=null) out.remove(tryOn==back ? cape : back);
        return out;
    }
    public static Item find(List<Item> outfit,Slot slot) {
        for(Item i:outfit) if(i.slot==slot) return i;
        return null;
    }
    /** The set whose every worn clothing piece belongs to it, when all of them do and no skin is worn. */
    public static OutfitSet fullSet(List<Item> outfit) {
        if(find(outfit,Slot.CHARACTER)!=null) return null;
        String set=null;
        int pieces=0;
        for(Item i:outfit) {
            if(i.category()!=Category.CLOTHING) continue;
            if(i.set==null) return null;
            if(set==null) set=i.set; else if(!set.equals(i.set)) return null;
            pieces++;
        }
        OutfitSet s=set(set);
        return s!=null && pieces>=s.pieces.size() ? s : null;
    }

    // Presets.
    public static List<Preset> presets() { load(); return List.copyOf(PRESETS); }
    public static void savePreset(String name) {
        load();
        PRESETS.removeIf(p->p.name.equalsIgnoreCase(name));
        PRESETS.add(new Preset(name,new EnumMap<>(WORN),System.currentTimeMillis()));
        save();
    }
    public static void applyPreset(Preset preset) { load(); WORN.clear(); WORN.putAll(preset.items); changed(); }
    public static void deletePreset(Preset preset) { load(); if(PRESETS.remove(preset)) save(); }

    private static void changed() { version++; save(); }
    private static void loadState() {
        try {
            if(!Files.exists(FILE)) return;
            JsonObject root=JsonParser.parseString(Files.readString(FILE,StandardCharsets.UTF_8)).getAsJsonObject();
            WORN.putAll(slots(WardrobeModel.object(root,"worn")));
            for(JsonElement e:WardrobeModel.array(root,"presets")) {
                if(!e.isJsonObject()) continue;
                JsonObject p=e.getAsJsonObject();
                PRESETS.add(new Preset(WardrobeModel.string(p,"name","Образ"),slots(WardrobeModel.object(p,"items")),p.has("savedAt") ? p.get("savedAt").getAsLong() : 0));
            }
        } catch(Exception e) { System.err.println("[ESS] wardrobe.json is broken: "+e.getMessage()); }
    }
    private static EnumMap<Slot,String> slots(JsonObject json) {
        EnumMap<Slot,String> out=new EnumMap<>(Slot.class);
        if(json==null) return out;
        for(var e:json.entrySet()) {
            try {
                Slot slot=Slot.valueOf(e.getKey());
                Item item=ITEMS.get(e.getValue().getAsString());
                if(item!=null && item.slot==slot) out.put(slot,item.id);
            } catch(RuntimeException ignored) {}
        }
        return out;
    }
    private static JsonObject json(Map<Slot,String> slots) {
        JsonObject o=new JsonObject();
        slots.forEach((slot,id)->o.addProperty(slot.name(),id));
        return o;
    }
    private static void save() {
        JsonObject root=new JsonObject();
        root.add("worn",json(WORN));
        JsonArray presets=new JsonArray();
        for(Preset p:PRESETS) {
            JsonObject o=new JsonObject();
            o.addProperty("name",p.name); o.add("items",json(p.items)); o.addProperty("savedAt",p.savedAt);
            presets.add(o);
        }
        root.add("presets",presets);
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling("wardrobe.json.tmp");
            Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardCharsets.UTF_8);
            Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException e) { System.err.println("[ESS] wardrobe.json not saved: "+e.getMessage()); }
    }
}
