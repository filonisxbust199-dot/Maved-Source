package ess.ll.client.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Current Windows media session (Spotify, browsers, Яндекс Музыка, ...) through a hidden PowerShell process
 * that talks to the system media controls (SMTC). The script prints a JSON state line every 400 ms and
 * reads commands from a file. Windows only; elsewhere it stays empty.
 */
public final class MediaBridge {
    private MediaBridge() {}
    private static final Logger LOG=LoggerFactory.getLogger("ess-best/media");
    private static final Path SCRIPT=temp("ess_media_session.ps1"), ART=temp("ess_media_art.png"), COMMANDS=temp("ess_media_cmd.txt");
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{ Thread t=new Thread(r,"ESS-Media-IO"); t.setDaemon(true); return t; });

    /** One snapshot of the session; position is extrapolated from receivedAt while playing. */
    public record Track(boolean present,String title,String artist,String album,boolean playing,long positionMs,long durationMs,
                        boolean canSeek,long receivedAt) {
        static final Track EMPTY=new Track(false,"","","",false,0,0,false,0);
        public long position() {
            long p=positionMs+(playing ? (System.nanoTime()-receivedAt)/1_000_000 : 0);
            return durationMs>0 ? Math.min(p,durationMs) : p;
        }
        public String key() { return (title+"\u0001"+artist).toLowerCase(Locale.ROOT); }
    }

    private static volatile Track track=Track.EMPTY;
    private static volatile Process process;
    private static volatile boolean running;
    private static volatile Identifier artwork;
    private static volatile String artId="";
    private static int restarts, artCounter;
    private static long lastRaw=-1;
    private static boolean hooked;

    public static Track track() { return track; }
    private static volatile boolean fake;
    /** Development check: a fixed playing track instead of the system session. */
    public static void fake(String title,String artist,String album,long positionMs,long durationMs) {
        stop();
        fake=true;
        track=new Track(true,title,artist,album,true,positionMs,durationMs,true,System.nanoTime());
    }
    public static Identifier artwork() { return artwork; }

    /** Starts the bridge once; safe to call every frame. */
    public static synchronized void ensureRunning() {
        if(running || fake || !System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return;
        running=true; restarts=0;
        // The PowerShell child would outlive the game otherwise.
        if(!hooked) { hooked=true; Runtime.getRuntime().addShutdownHook(new Thread(MediaBridge::stop,"ESS-Media-Stop")); }
        launch();
    }
    public static synchronized void stop() {
        if(fake) return;
        running=false;
        Process p=process; process=null;
        if(p!=null) p.destroy();
        track=Track.EMPTY;
    }

    public static void toggle() { command("toggle",0); flipPlaying(); }
    public static void next() { command("next",0); }
    public static void previous() { command("previous",0); }
    public static void seek(long ms) {
        Track t=track;
        track=new Track(t.present,t.title,t.artist,t.album,t.playing,Math.max(0,ms),t.durationMs,t.canSeek,System.nanoTime());
        command("seek",ms);
    }
    /** Optimistic local flip so the button answers at once; the next state line confirms it. */
    private static void flipPlaying() {
        Track t=track;
        track=new Track(t.present,t.title,t.artist,t.album,!t.playing,t.position(),t.durationMs,t.canSeek,System.nanoTime());
    }

    private static void launch() {
        try {
            try(InputStream in=MediaBridge.class.getResourceAsStream("/assets/ess-best/media/media_session.ps1")) {
                if(in==null) throw new IllegalStateException("media_session.ps1 missing");
                byte[] bytes=in.readAllBytes();
                // Windows PowerShell 5 reads scripts without a BOM as ANSI; the script is UTF-8.
                try(OutputStream out=Files.newOutputStream(SCRIPT)) {
                    if(!(bytes.length>=3 && (bytes[0]&255)==0xEF)) out.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    out.write(bytes);
                }
            }
            Files.deleteIfExists(COMMANDS);
            Process p=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-ExecutionPolicy","Bypass",
                    "-File",SCRIPT.toString(),"-IntervalMs","400","-ArtPath",ART.toString(),"-CommandPath",COMMANDS.toString()).start();
            process=p;
            daemon("ESS-Media",()->read(p));
            daemon("ESS-Media-Errors",()->drain(p));
        } catch(Exception e) {
            LOG.warn("Media bridge did not start: {}",e.toString());
            running=false;
        }
    }
    private static void read(Process p) {
        try(var reader=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))) {
            String line;
            while((line=reader.readLine())!=null && running && process==p) parse(line);
        } catch(Exception ignored) {}
        synchronized(MediaBridge.class) {
            if(running && process==p) {
                track=Track.EMPTY;
                if(++restarts<=3) { sleep(3000); if(running) launch(); }
                else { running=false; LOG.warn("Media bridge stopped after {} restarts",restarts-1); }
            }
        }
    }
    private static void drain(Process p) {
        try(var reader=new BufferedReader(new InputStreamReader(p.getErrorStream(),StandardCharsets.UTF_8))) {
            while(reader.readLine()!=null) {}
        } catch(Exception ignored) {}
    }
    private static void parse(String line) {
        try {
            JsonObject json=JsonParser.parseString(line).getAsJsonObject();
            if(!json.get("ok").getAsBoolean()) { track=Track.EMPTY; return; }
            boolean playing="Playing".equalsIgnoreCase(text(json,"status"));
            long raw=Math.max(0,number(json,"position")), duration=Math.max(0,number(json,"duration"));
            long reported=raw+(playing ? Math.clamp(number(json,"age"),0,Math.max(2000,duration)) : 0);
            Track prev=track, next=new Track(true,text(json,"title"),text(json,"artist"),text(json,"album"),playing,
                    reported,duration,json.has("canSeek") && json.get("canSeek").getAsBoolean(),System.nanoTime());
            // Players refresh the timeline rarely and unevenly, so every state line used to nudge the clock
            // back and forth. Keep the local clock running and only follow the player on a real jump (seek)
            // or when its timeline has actually moved and drifted away from ours.
            if(prev.present && prev.key().equals(next.key()) && prev.playing==playing) {
                long local=prev.position();
                boolean moved=raw!=lastRaw;
                if(!moved || Math.abs(reported-local)<1500) next=new Track(true,next.title,next.artist,next.album,playing,local,duration,next.canSeek,System.nanoTime());
            }
            lastRaw=raw;
            track=next;
            String id=text(json,"thumbId"), thumb=text(json,"thumb");
            if(!id.equals(artId)) {
                artId=id;
                if(thumb.isBlank()) releaseArt(); else loadArt(Path.of(thumb),id);
            }
        } catch(Exception ignored) {}
    }
    private static void loadArt(Path file,String id) {
        IO.execute(()->{
            try {
                NativeImage image=NativeImage.read(Files.readAllBytes(file));
                Minecraft.getInstance().execute(()->{
                    if(!id.equals(artId)) { image.close(); return; }
                    Identifier old=artwork, fresh=Identifier.fromNamespaceAndPath("ess-best","media/art_"+(++artCounter));
                    Minecraft.getInstance().getTextureManager().register(fresh,new DynamicTexture(()->"ess-media-art",image));
                    artwork=fresh;
                    if(old!=null) Minecraft.getInstance().getTextureManager().release(old);
                });
            } catch(Exception e) { LOG.debug("Artwork not read: {}",e.toString()); }
        });
    }
    private static void releaseArt() {
        Identifier old=artwork; artwork=null;
        if(old!=null) Minecraft.getInstance().execute(()->Minecraft.getInstance().getTextureManager().release(old));
    }
    private static void command(String name,long value) {
        if(process==null) return;
        IO.execute(()->{
            try {
                Path tmp=Path.of(COMMANDS+".tmp");
                Files.writeString(tmp,name+" "+value+System.lineSeparator(),StandardCharsets.UTF_8);
                Files.move(tmp,COMMANDS,StandardCopyOption.REPLACE_EXISTING);
            } catch(Exception e) { LOG.debug("Media command {} failed: {}",name,e.toString()); }
        });
    }
    private static String text(JsonObject o,String key) { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : ""; }
    private static long number(JsonObject o,String key) { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : 0; }
    private static Path temp(String name) { return Path.of(System.getProperty("java.io.tmpdir","."),name); }
    private static void daemon(String name,Runnable r) { Thread t=new Thread(r,name); t.setDaemon(true); t.start(); }
    private static void sleep(long ms) { try { Thread.sleep(ms); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } }
}
