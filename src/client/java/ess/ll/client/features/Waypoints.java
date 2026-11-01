package ess.ll.client.features;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Waypoints from .way: a name and x/z, kept per server and dimension in config/ess-best/waypoints.json. One of
 * the current place's points is active: the newest one unless .way go picked another; the marker and the HUD
 * arrow lead to it.
 */
public final class Waypoints {
    public record Point(String name,int x,int z,String server,String dimension) {}
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("ess-best").resolve("waypoints.json");
    private static final List<Point> POINTS=new ArrayList<>();
    private static String active;
    private static boolean loaded;

    private Waypoints() {}

    /** Where the player is: the server address (or "singleplayer") and the dimension id. */
    private static String server() {
        var mc=Minecraft.getInstance();
        var data=mc.getCurrentServer();
        return data!=null ? data.ip.toLowerCase(Locale.ROOT) : "singleplayer";
    }
    private static String dimension() {
        var mc=Minecraft.getInstance();
        return mc.level==null ? "" : mc.level.dimension().identifier().toString();
    }

    /** Points of the current server and dimension, oldest first. */
    public static List<Point> here() {
        load();
        String s=server(),d=dimension();
        return POINTS.stream().filter(p->p.server.equals(s) && p.dimension.equals(d)).toList();
    }
    /** The point the marker and the arrow lead to, or null. */
    public static Point active() {
        List<Point> here=here();
        if(here.isEmpty() || "".equals(active)) return null;
        if(active!=null) for(Point p:here) if(p.name.equalsIgnoreCase(active)) return p;
        return here.getLast();
    }

    public static Point add(String name,int x,int z) {
        load();
        if(name==null || name.isBlank()) name="Метка "+(here().size()+1);
        name=name.strip();
        if(name.length()>32) throw new IllegalArgumentException("Имя метки: до 32 символов");
        if(Math.abs(x)>30_000_000 || Math.abs(z)>30_000_000) throw new IllegalArgumentException("Координаты за границей мира");
        String n=name;
        POINTS.removeIf(p->p.name.equalsIgnoreCase(n) && p.server.equals(server()) && p.dimension.equals(dimension()));
        Point point=new Point(name,x,z,server(),dimension());
        POINTS.add(point);
        active=null;
        save();
        return point;
    }
    /** By name or by its number in .way list. */
    public static Point remove(String key) {
        Point p=find(key);
        if(p==null) return null;
        POINTS.remove(p);
        if(active!=null && active.equalsIgnoreCase(p.name)) active=null;
        save();
        return p;
    }
    public static Point go(String key) {
        Point p=find(key);
        if(p!=null) active=p.name;
        return p;
    }
    /** Hides the marker until a point is added or picked again. */
    public static void hide() { active=""; }
    public static int clear() {
        List<Point> here=here();
        POINTS.removeAll(here);
        active=null;
        save();
        return here.size();
    }
    private static Point find(String key) {
        List<Point> here=here();
        try {
            int i=Integer.parseInt(key);
            if(i>=1 && i<=here.size()) return here.get(i-1);
        } catch(NumberFormatException ignored) {}
        for(Point p:here) if(p.name.equalsIgnoreCase(key)) return p;
        return null;
    }

    private static void load() {
        if(loaded) return;
        loaded=true;
        try {
            if(!Files.exists(FILE)) return;
            for(var e:JsonParser.parseString(Files.readString(FILE,StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject o=e.getAsJsonObject();
                String name=o.get("name").getAsString();
                if(name.isBlank() || name.length()>32) continue;
                POINTS.add(new Point(name,o.get("x").getAsInt(),o.get("z").getAsInt(),o.get("server").getAsString(),o.get("dimension").getAsString()));
            }
        } catch(Exception e) { System.err.println("[ESS] waypoints.json is broken: "+e.getMessage()); }
    }
    /** Written to a temporary file first, so a crash mid-write never leaves a broken list. */
    private static void save() {
        JsonArray list=new JsonArray();
        for(Point p:POINTS) {
            JsonObject o=new JsonObject();
            o.addProperty("name",p.name); o.addProperty("x",p.x); o.addProperty("z",p.z);
            o.addProperty("server",p.server); o.addProperty("dimension",p.dimension);
            list.add(o);
        }
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling("waypoints.json.tmp");
            Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(list),StandardCharsets.UTF_8);
            Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException e) { System.err.println("[ESS] waypoints.json not saved: "+e.getMessage()); }
    }
}
