package ess.ll.client.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time-synced song lyrics from LRCLIB (lrclib.net), looked up by title, artist and duration. A track LRCLIB has
 * no synced lyrics for is remembered for the session; when the service fails the track is asked for again later.
 */
public final class Lyrics {
    private Lyrics() {}
    public record Line(long timeMs,String text) {}
    private static final List<Line> NONE=List.of();
    private static final Map<String,List<Line>> CACHE=new ConcurrentHashMap<>();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final Pattern STAMP=Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");
    /** When a track may be requested again: a request runs, or the last one failed. */
    private static final Map<String,Long> NEXT_TRY=new ConcurrentHashMap<>();
    private static final long RETRY_MS=30_000;
    /** Video-site additions that are not part of a song's title. */
    private static final Pattern NOISE=Pattern.compile("(?iu)\\s*[(\\[][^)\\]]*(official|video|audio|lyric|клип|hd|4k|remaster)[^)\\]]*[)\\]]|\\s+(feat|ft)\\.?\\s.*$|\\s+-\\s+youtube$");

    /** Lines of the track, or an empty list while loading or when none exist. */
    public static List<Line> of(MediaBridge.Track track) {
        if(!track.present() || track.title().isBlank()) return NONE;
        String key=track.key();
        List<Line> cached=CACHE.get(key);
        if(cached!=null) return cached;
        long now=System.currentTimeMillis();
        if(now<NEXT_TRY.getOrDefault(key,0L)) return NONE;
        NEXT_TRY.put(key,now+RETRY_MS);
        fetch(key,track.title(),track.artist(),track.durationMs());
        return NONE;
    }
    /** Development check: preset lines for a track. */
    public static void put(MediaBridge.Track track,List<Line> lines) { CACHE.put(track.key(),List.copyOf(lines)); }
    /** Index of the line playing at position, or -1 before the first one. */
    public static int index(List<Line> lines,long position) {
        int found=-1;
        for(int i=0;i<lines.size();i++) { if(lines.get(i).timeMs()<=position) found=i; else break; }
        return found;
    }

    private static void fetch(String key,String title,String artist,long durationMs) {
        String song=NOISE.matcher(title).replaceAll("").trim(), singer=artist.replaceAll("(?i)\\s*(- topic|vevo)$","").trim();
        String query="track_name="+enc(song)+"&artist_name="+enc(singer)+(durationMs>0 ? "&duration="+Math.round(durationMs/1000.0) : "");
        request("https://lrclib.net/api/get?"+query).thenCompose(body->{
            List<Line> lines=body==null ? NONE : parseObject(body);
            if(!lines.isEmpty()) return java.util.concurrent.CompletableFuture.completedFuture(lines);
            // No exact match: the search endpoint tolerates small title differences.
            return request("https://lrclib.net/api/search?track_name="+enc(song)+"&artist_name="+enc(singer)).thenApply(Lyrics::parseSearch);
        }).thenCompose(lines->{
            if(!lines.isEmpty()) return java.util.concurrent.CompletableFuture.completedFuture(lines);
            // Browsers report the channel as the artist: the free search looks at the title and the name together.
            return request("https://lrclib.net/api/search?q="+enc((song+" "+singer).trim())).thenApply(Lyrics::parseSearch);
        }).thenAccept(lines->CACHE.put(key,lines)).exceptionally(e->null);
    }
    private static java.util.concurrent.CompletableFuture<String> request(String url) {
        HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("User-Agent","ess-best (Minecraft mod; media player lyrics)").GET().build();
        return HTTP.sendAsync(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(r->{
            // A server fault says nothing about the track, so it fails the lookup instead of ending as "no lyrics".
            if(r.statusCode()>=500 || r.statusCode()==429) throw new IllegalStateException("LRCLIB "+r.statusCode());
            return r.statusCode()==200 ? r.body() : null;
        });
    }
    private static List<Line> parseObject(String body) {
        try { return parse(JsonParser.parseString(body).getAsJsonObject()); } catch(Exception e) { return NONE; }
    }
    private static List<Line> parseSearch(String body) {
        if(body==null) return NONE;
        try {
            JsonArray results=JsonParser.parseString(body).getAsJsonArray();
            for(JsonElement e:results) {
                List<Line> lines=parse(e.getAsJsonObject());
                if(!lines.isEmpty()) return lines;
            }
        } catch(Exception ignored) {}
        return NONE;
    }
    private static List<Line> parse(JsonObject o) {
        if(!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return NONE;
        List<Line> lines=new ArrayList<>();
        for(String raw:o.get("syncedLyrics").getAsString().split("\n")) {
            Matcher m=STAMP.matcher(raw);
            List<Long> times=new ArrayList<>();
            int end=0;
            while(m.find() && m.start()==end) {
                times.add(Math.round((Integer.parseInt(m.group(1))*60+Double.parseDouble(m.group(2)))*1000));
                end=m.end();
            }
            String text=raw.substring(end).trim();
            if(text.isEmpty()) text="• • •";
            for(long t:times) lines.add(new Line(t,text));
        }
        lines.sort((a,b)->Long.compare(a.timeMs(),b.timeMs()));
        return List.copyOf(lines);
    }
    private static String enc(String s) { return URLEncoder.encode(s,StandardCharsets.UTF_8); }
}
