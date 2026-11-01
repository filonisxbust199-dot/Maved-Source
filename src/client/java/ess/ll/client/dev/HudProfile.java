package ess.ll.client.dev;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/**
 * Development-only HUD profiler (-Dess.hudprofile=true): CPU time of each ESS HUD element's extraction, of the
 * GUI renderer (vertex building and draws), the GUI elements by pipeline, how often consecutive elements switch
 * pipeline or texture (each switch ends a batch), the draw count and the frame interval.
 */
public final class HudProfile {
    public static final boolean ON=Boolean.getBoolean("ess.hudprofile");
    private static final Map<String,long[]> ELEMENTS=new TreeMap<>(), PIPELINES=new TreeMap<>();
    private static long frames, guiNs, guiStart, draws, switches, lastFrame, frameNs, elementsTotal;
    private static Object lastKey;

    private HudProfile() {}

    public static HudElement wrap(String name,HudElement element) {
        if(!ON) return element;
        return (g,delta)->{
            long start=System.nanoTime();
            element.extractRenderState(g,delta);
            ELEMENTS.computeIfAbsent(name,k->new long[1])[0]+=System.nanoTime()-start;
        };
    }
    /** A named slice of HUD time (widgets of HudRenderer). */
    public static void time(String name,long nanos) { ELEMENTS.computeIfAbsent(name,k->new long[1])[0]+=nanos; }
    public static void element(GuiElementRenderState state) {
        if(!ON) return;
        String pipeline=String.valueOf(state.pipeline().getLocation());
        PIPELINES.computeIfAbsent(pipeline,k->new long[1])[0]++;
        elementsTotal++;
        Object key=pipeline+"|"+state.textureSetup()+"|"+state.scissorArea();
        if(!key.equals(lastKey)) switches++;
        lastKey=key;
    }
    public static void guiStart() {
        if(!ON) return;
        long now=System.nanoTime();
        if(lastFrame!=0) frameNs+=now-lastFrame;
        lastFrame=now;
        guiStart=now;
        lastKey=null;
    }
    public static void guiEnd(int drawCount) {
        if(!ON) return;
        guiNs+=System.nanoTime()-guiStart;
        frames++;
    }
    public static void drawCount(int count) { if(ON) draws+=count; }
    public static void reset() {
        ELEMENTS.clear(); PIPELINES.clear();
        frames=guiNs=draws=switches=frameNs=elementsTotal=0;
        lastFrame=0;
    }
    public static String report(String label) {
        if(frames==0) return label+": no frames";
        StringBuilder out=new StringBuilder(String.format(Locale.ROOT,"%s: %d frames, frame %.3f ms (%.0f fps), gui render %.3f ms, draws %.1f, elements %.1f, batch breaks %.1f",
                label,frames,frameNs/1e6/frames,frames*1e9/Math.max(1,frameNs),guiNs/1e6/frames,(double)draws/frames,(double)elementsTotal/frames,(double)switches/frames));
        ELEMENTS.forEach((k,v)->out.append(String.format(Locale.ROOT,"\n  hud %-16s %.3f ms",k,v[0]/1e6/frames)));
        PIPELINES.forEach((k,v)->out.append(String.format(Locale.ROOT,"\n  pipeline %-40s %.1f",k,(double)v[0]/frames)));
        return out.toString();
    }
}
