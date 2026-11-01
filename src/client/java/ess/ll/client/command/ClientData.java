package ess.ll.client.command;

import com.google.gson.*;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.HudRenderer;
import ess.ll.client.setting.Settings.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Local JSON storage with bounded input, validation before apply, and atomic replacement. */
public final class ClientData {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT=FabricLoader.getInstance().getConfigDir().resolve("ess-best");
    private static final Path PROFILES=ROOT.resolve("profiles"),THEMES=ROOT.resolve("themes"),STATE=ROOT.resolve("state.json");
    /** Colour order of Theme.Preset.colors(). */
    private static final String[] THEME_KEYS={"accent","background","card","field","text","secondary","hint","border"};
    private static final Map<String,String> FRIENDS=new TreeMap<>();
    private static final Map<String,Macro> MACROS=new TreeMap<>();
    private static String lastSaved="";
    private static int ticks;
    private static boolean ready;
    public record Macro(String name,int key,String text) {}
    private ClientData() {}
    public static boolean isFriend(String name) { return FRIENDS.containsKey(name.toLowerCase(Locale.ROOT)); }
    public static Collection<String> friends() { return List.copyOf(FRIENDS.values()); }
    public static Collection<Macro> macros() { return List.copyOf(MACROS.values()); }
    public static void addFriend(String name) {
        if(!name.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Неверный ник");
        FRIENDS.put(name.toLowerCase(Locale.ROOT),name);
    }
    public static void removeFriend(String name) { FRIENDS.remove(name.toLowerCase(Locale.ROOT)); }
    public static void clearFriends() { FRIENDS.clear(); }
    public static void putMacro(String name,int key,String text) {
        validName(name);
        if(key<0 || text.isBlank() || text.length()>256 || text.indexOf('\n')>=0 || text.indexOf('\r')>=0) throw new IllegalArgumentException("Макрос: до 256 символов, одна строка");
        MACROS.put(name.toLowerCase(Locale.ROOT),new Macro(name,key,text));
    }
    public static void removeMacro(String name) { MACROS.remove(name.toLowerCase(Locale.ROOT)); }
    public static void clearMacros() { MACROS.clear(); }
    public static void initialize() {
        try {
            Files.createDirectories(PROFILES);
            if(Files.exists(STATE)) {
                JsonObject root=read(STATE);
                List<Runnable> changes=prepareModules(root.getAsJsonObject("modules"));
                Map<String,String> friends=new TreeMap<>(); Map<String,Macro> macros=new TreeMap<>();
                if(root.has("friends")) for(var e:root.getAsJsonArray("friends")) {
                    String name=e.getAsString();
                    if(!name.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Неверный ник в state.json");
                    friends.put(name.toLowerCase(Locale.ROOT),name);
                }
                if(root.has("macros")) for(var e:root.getAsJsonArray("macros")) {
                    JsonObject m=e.getAsJsonObject(); String name=m.get("name").getAsString(),text=m.get("text").getAsString();
                    int key=m.get("key").getAsInt(); validName(name);
                    if(!Keys.isValid(key) || text.isBlank() || text.length()>256 || text.contains("\n") || text.contains("\r")) throw new IllegalArgumentException("Неверный макрос");
                    macros.put(name.toLowerCase(Locale.ROOT),new Macro(name,key,text));
                }
                if(root.has("hud")) for(var e:root.getAsJsonObject("hud").entrySet()) {
                    var array=e.getValue().getAsJsonArray();
                    if(array.size()!=4) continue; // positions from older HUD layouts are ignored
                    float[] data=new float[4];
                    for(int i=0;i<4;i++) { data[i]=array.get(i).getAsFloat(); if(!Float.isFinite(data[i])) throw new IllegalArgumentException("Неверная позиция HUD"); }
                    changes.add(()->HudRenderer.place(e.getKey(),data));
                }
                changes.forEach(Runnable::run); FRIENDS.putAll(friends); MACROS.putAll(macros);
            }
        } catch(Exception e) {
            System.err.println("[ESS] Cannot load state: "+e.getMessage());
            try { if(Files.exists(STATE)) Files.copy(STATE,ROOT.resolve("state-broken-"+System.currentTimeMillis()+".json")); }
            catch(IOException backupFailure) { System.err.println("[ESS] Backup failed; autosave disabled: "+backupFailure.getMessage()); return; }
        }
        ready=true;
        lastSaved=GSON.toJson(snapshot());
    }
    public static void tick() {
        if(ready && ++ticks%40==0) try { flush(); } catch(IOException e) { System.err.println("[ESS] Autosave failed: "+e.getMessage()); }
    }
    public static void flush() throws IOException {
        if(!ready) return;
        String json=GSON.toJson(snapshot());
        if(!json.equals(lastSaved) || !Files.exists(STATE)) { write(STATE,json); lastSaved=json; }
    }
    public static void saveProfile(String name) throws IOException {
        JsonObject root=new JsonObject(); root.addProperty("version",1); root.add("modules",modules()); write(profile(name),GSON.toJson(root));
    }
    public static void loadProfile(String name) throws IOException {
        var changes=prepareModules(read(profile(name)).getAsJsonObject("modules")); changes.forEach(Runnable::run); flush();
    }
    public static void deleteProfile(String name) throws IOException {
        if(!Files.deleteIfExists(profile(name))) throw new IllegalArgumentException("Конфиг не найден");
    }
    public static List<String> profiles() throws IOException {
        Files.createDirectories(PROFILES);
        try(var files=Files.list(PROFILES)) {
            return files.filter(Files::isRegularFile).map(p->p.getFileName().toString()).filter(s->s.endsWith(".json")).map(s->s.substring(0,s.length()-5)).sorted().toList();
        }
    }
    /** Saved ClickGUI themes, config/ess-best/themes/custom-N.json, named "Моя N". */
    public static List<ess.ll.client.render.Theme.Preset> themes() throws IOException {
        Files.createDirectories(THEMES);
        List<ess.ll.client.render.Theme.Preset> list=new ArrayList<>();
        try(var files=Files.list(THEMES)) {
            for(Path path:files.filter(p->p.getFileName().toString().matches("custom-\\d{1,4}\\.json")).sorted(Comparator.comparingInt(ClientData::themeNumber)).toList()) {
                try {
                    JsonObject root=read(path),colors=root.getAsJsonObject("colors");
                    String name=root.get("name").getAsString();
                    if(name.isBlank() || name.length()>24) throw new IllegalArgumentException("Неверное имя темы");
                    int[] c=new int[THEME_KEYS.length];
                    for(int i=0;i<c.length;i++) c[i]=ColorSetting.parse(colors.get(THEME_KEYS[i]).getAsString());
                    list.add(ess.ll.client.render.Theme.Preset.of(name,c,path.getFileName().toString()));
                } catch(Exception e) { System.err.println("[ESS] Skipping theme "+path.getFileName()+": "+e.getMessage()); }
            }
        }
        return list;
    }
    public static ess.ll.client.render.Theme.Preset saveTheme(int[] colors) throws IOException {
        Files.createDirectories(THEMES);
        int n=1;
        while(Files.exists(THEMES.resolve("custom-"+n+".json"))) n++;
        String file="custom-"+n+".json",name="Моя "+n;
        JsonObject root=new JsonObject(),data=new JsonObject();
        root.addProperty("version",1); root.addProperty("name",name);
        for(int i=0;i<THEME_KEYS.length;i++) data.addProperty(THEME_KEYS[i],String.format(Locale.ROOT,"#%08X",colors[i]));
        root.add("colors",data);
        write(THEMES.resolve(file),GSON.toJson(root));
        return ess.ll.client.render.Theme.Preset.of(name,colors,file);
    }
    public static void deleteTheme(String file) throws IOException {
        if(!file.matches("custom-\\d{1,4}\\.json")) throw new IllegalArgumentException("Неверный файл темы");
        Files.deleteIfExists(THEMES.resolve(file));
    }
    private static int themeNumber(Path path) {
        String name=path.getFileName().toString();
        return Integer.parseInt(name.substring(7,name.length()-5));
    }
    private static void validName(String name) {
        if(!name.matches("[A-Za-z0-9_-]{1,48}") || name.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])")) throw new IllegalArgumentException("Имя: 1–48 латинских букв, цифр, _ или -");
    }
    private static Path profile(String name) { validName(name); return PROFILES.resolve(name+".json"); }
    private static JsonObject read(Path path) throws IOException {
        if(Files.size(path)>1_048_576) throw new IOException("JSON слишком большой");
        JsonObject root=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();
        if(!root.has("version") || root.get("version").getAsInt()!=1) throw new IllegalArgumentException("Неизвестная версия конфига");
        return root;
    }
    private static void write(Path target,String json) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),"ess-",".tmp");
        try {
            Files.writeString(temp,json,StandardCharsets.UTF_8);
            try { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private static JsonObject modules() {
        JsonObject modules=new JsonObject();
        for(var m:ClientModules.all()) {
            JsonObject data=new JsonObject(),settings=new JsonObject();
            data.addProperty("enabled",m.enabled()); data.addProperty("bind",m.bind); data.addProperty("hold",m.hold);
            for(var s:m.settings) {
                if(s instanceof BooleanSetting b) settings.addProperty(s.name(),b.get());
                else if(s instanceof SliderSetting slider) settings.addProperty(s.name(),slider.get());
                else if(s instanceof ModeSettings mode) settings.addProperty(s.name(),mode.get());
                else if(s instanceof KeybindSetting keybind) settings.addProperty(s.name(),keybind.get());
                else if(s instanceof ColorSetting color) settings.addProperty(s.name(),color.get());
                else if(s instanceof MultiBooleanSettings multi) {
                    JsonArray values=new JsonArray();
                    for(int i=0;i<multi.options.size();i++) if(multi.selected(i)) values.add(multi.options.get(i));
                    settings.add(s.name(),values);
                    // Every option known at save time, so options added in later versions keep their default.
                    JsonArray known=new JsonArray();
                    multi.options.forEach(known::add);
                    settings.add(s.name()+"#known",known);
                }
            }
            data.add("settings",settings); modules.add(m.name,data);
            if(m.name.equals("BlockESP"))data.add("blocks",ess.ll.client.module.render.BlockESP.INSTANCE.save());
        }
        return modules;
    }
    private static JsonObject snapshot() {
        JsonObject root=new JsonObject(); root.addProperty("version",1); root.add("modules",modules());
        root.add("friends",GSON.toJsonTree(FRIENDS.values())); root.add("macros",GSON.toJsonTree(MACROS.values()));
        JsonObject hud=new JsonObject();
        HudRenderer.positions().forEach((id,pos)->{ JsonArray a=new JsonArray(); for(float v:pos) a.add(v); hud.add(id,a); });
        root.add("hud",hud);
        return root;
    }
    private static boolean bool(JsonElement value) {
        if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Ожидался boolean");
        return value.getAsBoolean();
    }
    private static List<Runnable> prepareModules(JsonObject source) {
        if(source==null) throw new IllegalArgumentException("Нет секции modules");
        List<Runnable> changes=new ArrayList<>();
        for(var m:ClientModules.all()) {
            if(!source.has(m.name)) continue;
            JsonObject data=source.getAsJsonObject(m.name);
            if(m.name.equals("BlockESP"))changes.add(ess.ll.client.module.render.BlockESP.INSTANCE.prepareLoad(data.getAsJsonObject("blocks")));
            boolean enabled=bool(data.get("enabled"));
            int bind=data.has("bind")?data.get("bind").getAsInt():-1;
            if(bind!=-1 && !Keys.isValid(bind)) throw new IllegalArgumentException("Неизвестная клавиша");
            boolean hold=data.has("hold") ? bool(data.get("hold")) : m.hold;
            JsonObject settings=data.getAsJsonObject("settings");
            if(settings!=null) for(var s:m.settings) if(settings.has(s.name())) {
                JsonElement value=settings.get(s.name());
                if(s instanceof BooleanSetting b) { boolean v=bool(value); changes.add(()->{if(b.get()!=v)b.toggle();}); }
                else if(s instanceof SliderSetting slider) {
                    float v=value.getAsFloat(); if(!Float.isFinite(v) || v<slider.min || v>slider.max) throw new IllegalArgumentException("Значение вне диапазона: "+s.name());
                    changes.add(()->slider.set(v));
                } else if(s instanceof ModeSettings mode) {
                    int index=mode.options.indexOf(value.getAsString()); if(index<0) throw new IllegalArgumentException("Неизвестный режим");
                    changes.add(()->mode.select(index));
                } else if(s instanceof KeybindSetting keybind) {
                    int key=value.getAsJsonPrimitive().isString()?Keys.parse(value.getAsString()):value.getAsInt();
                    if(key!=-1 && !Keys.isValid(key))throw new IllegalArgumentException("Неизвестная клавиша");
                    changes.add(()->keybind.set(key));
                } else if(s instanceof ColorSetting color) {
                    int argb=value.getAsJsonPrimitive().isString()?ColorSetting.parse(value.getAsString()):value.getAsInt();
                    changes.add(()->color.set(argb));
                } else if(s instanceof MultiBooleanSettings multi) {
                    Set<String> selected=new HashSet<>();
                    for(var item:value.getAsJsonArray()) { String option=item.getAsString(); if(!multi.options.contains(option)) throw new IllegalArgumentException("Неизвестный пункт"); selected.add(option); }
                    // Options the save did not know yet (newer than it) keep their default instead of turning off.
                    Set<String> known=new HashSet<>(multi.options);
                    JsonElement knownValue=settings.get(s.name()+"#known");
                    if(knownValue!=null && knownValue.isJsonArray()) { known.clear(); for(var item:knownValue.getAsJsonArray()) known.add(item.getAsString()); }
                    else known.removeAll(multi.addedLater);
                    changes.add(()->{for(int i=0;i<multi.options.size();i++) {
                        String option=multi.options.get(i);
                        if(known.contains(option) && multi.selected(i)!=selected.contains(option)) multi.toggle(i);
                    }});
                }
            }
            // Hold modules always start released; their state follows the key.
            changes.add(()->{m.bind=bind; m.hold=hold; if(!hold) m.setEnabled(enabled);});
        }
        return changes;
    }
}
