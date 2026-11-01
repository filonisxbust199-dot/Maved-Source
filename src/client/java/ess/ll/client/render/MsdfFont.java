package ess.ll.client.render;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * msdf-atlas-gen JSON fonts and icon sets. Both y origins are accepted and normalised to top-down;
 * sizes are in GUI pixels per em.
 */
public final class MsdfFont {
    public static final MsdfFont UI = new MsdfFont("notosans", false);
    /** SF Rounded Bold atlas carried from test-26.2 for EntityESP plates and BPS. */
    public static final MsdfFont ESP = new MsdfFont("sf_rounded_bold", false);
    /** Text and icon glyphs of the dark HUD plates. */
    public static final MsdfFont PLATE = new MsdfFont("maved_medium", false), PLATE_ICONS = new MsdfFont("maved_icons", false);
    /** Unbounded, the site's typeface (regular, bold, black), built from site/public/fonts for the main menu. */
    public static final MsdfFont SITE = new MsdfFont("unbounded", false);
    public static final MsdfFont SITE_BOLD = new MsdfFont("unbounded_bold", false);
    public static final MsdfFont SITE_BLACK = new MsdfFont("unbounded_black", false);
    private final Identifier texture;
    /** Distance range in atlas pixels *16 for the UI shader, and which texture of the UI pair this atlas is. */
    private final int range16, slot;
    private final float atlasW, atlasH, ascender, lineHeight, capHeight;
    private final Int2ObjectOpenHashMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
    private final Long2FloatOpenHashMap kerning = new Long2FloatOpenHashMap();
    private final Map<String, Integer> names = new HashMap<>();
    private Glyph fallback;

    private MsdfFont(String name, boolean alphaSdf) {
        Data data;
        try (var stream = MsdfFont.class.getResourceAsStream("/assets/ess-best/msdf/" + name + ".json")) {
            if (stream == null) throw new IllegalStateException("Missing MSDF metadata " + name);
            data = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Data.class);
        } catch (Exception e) { throw new IllegalStateException("Cannot load font " + name, e); }
        texture = Identifier.fromNamespaceAndPath("ess-best", "textures/font/" + name + ".png");
        range16 = UiBatch.fixed(data.atlas.distanceRange);
        // The ESP atlas is the second texture of the UI pair; every other font takes the first slot.
        slot = "sf_rounded_bold".equals(name) ? UiBatch.SECOND_FONT : 0;

        boolean bottom = !"top".equals(data.atlas.yOrigin);
        atlasW = data.atlas.width; atlasH = data.atlas.height;
        // Internally y grows downwards: ascender and plane tops are negative.
        ascender = bottom ? -data.metrics.ascender : data.metrics.ascender;
        lineHeight = data.metrics.lineHeight;
        capHeight = data.metrics.capHeight > 0 ? data.metrics.capHeight : .714f;
        for (RawGlyph raw : data.glyphs) {
            Bounds plane = null, atlas = null;
            if (raw.planeBounds != null && raw.atlasBounds != null) {
                Bounds p = raw.planeBounds, a = raw.atlasBounds;
                plane = bottom ? new Bounds(p.left, -p.top, p.right, -p.bottom) : p;
                atlas = bottom ? new Bounds(a.left, atlasH - a.top, a.right, atlasH - a.bottom) : a;
            }
            glyphs.put(raw.unicode, new Glyph(raw.unicode, raw.advance, plane, atlas));
        }
        fallback = glyphs.get('?');
        if (data.kerning != null) for (Kern k : data.kerning) kerning.put(key(k.unicode1, k.unicode2), k.advance);
        if (data.names != null) names.putAll(data.names);
    }
    public static void initialize() { /* Forces pipeline registration before shader reload. */ }

    private static long key(int a, int b) { return ((long) a << 32) | (b & 0xffffffffL); }
    private Glyph glyph(int code) { Glyph glyph = glyphs.get(code); return glyph != null ? glyph : fallback; }
    /** Whether the atlas has this letter (server icon glyphs and rare symbols it does not). */
    public boolean has(int code) { return glyphs.containsKey(code); }
    public float lineHeight(float size) { return lineHeight * size; }

    /** Widths at size 1 (width scales with size), for labels measured and sorted every frame. */
    private final Object2FloatOpenHashMap<String> widths = new Object2FloatOpenHashMap<>();
    public float width(String text, float size) {
        text = ess.ll.client.module.misc.StreamerMode.apply(text);
        float unit = widths.getOrDefault(text, -1f);
        if (unit < 0) {
            if (widths.size() > 4096) widths.clear();
            unit = measure(text);
            widths.put(text, unit);
        }
        return unit * size;
    }
    private float measure(String text) {
        float size = 1;
        float x = 0, max = 0; int prev = -1;
        for (int i = 0; i < text.length(); ) {
            int code = text.codePointAt(i);
            i += Character.charCount(code);
            if (code == '\n') { max = Math.max(max, x); x = 0; prev = -1; continue; }
            Glyph glyph = glyph(code);
            if (glyph == null) continue;
            x += (kerning.get(key(prev, glyph.unicode)) + glyph.advance) * size;
            prev = glyph.unicode;
        }
        return Math.max(max, x);
    }
    public void centered(GuiGraphicsExtractor g, String text, float center, float top, float size, int color) {
        draw(g, text, center - width(text, size) / 2, top, size, color);
    }
    /** Draws text whose cap height is vertically centred on midY; returns the advance. */
    public float mid(GuiGraphicsExtractor g, String text, float x, float midY, float size, int color) {
        draw(g, text, x, topForMid(midY, size), size, Gradient.solid(color));
        return width(text, size);
    }
    public float mid(GuiGraphicsExtractor g, String text, float x, float midY, float size, Gradient paint) {
        draw(g, text, x, topForMid(midY, size), size, paint);
        return width(text, size);
    }
    public void midRight(GuiGraphicsExtractor g, String text, float right, float midY, float size, int color) {
        mid(g, text, right - width(text, size), midY, size, color);
    }
    public void midCenter(GuiGraphicsExtractor g, String text, float center, float midY, float size, int color) {
        mid(g, text, center - width(text, size) / 2, midY, size, color);
    }
    private float topForMid(float midY, float size) { return midY + capHeight * size / 2 + ascender * size; }

    /** Text with a soft drop shadow, for HUD text over bright, busy backdrops. */
    public void shadowed(GuiGraphicsExtractor g, String text, float x, float top, float size, Gradient paint) {
        int alpha = Math.round(0x70 * ((paint.from() >>> 24) / 255f));
        draw(g, text, x + size * .06f, top + size * .08f, size, Gradient.solid(alpha << 24));
        draw(g, text, x, top, size, paint);
    }
    public void shadowed(GuiGraphicsExtractor g, String text, float x, float top, float size, int color) {
        shadowed(g, text, x, top, size, Gradient.solid(color));
    }
    public void draw(GuiGraphicsExtractor g, String text, float x, float top, float size, int color) {
        draw(g, text, x, top, size, Gradient.solid(color));
    }
    /** The whole string is one GUI element: one pose, one clip and one set of bounds instead of one per letter. */
    public void draw(GuiGraphicsExtractor g, String text, float x, float top, float size, Gradient paint) {
        text = ess.ll.client.module.misc.StreamerMode.apply(text);
        if (size <= 0 || text.isEmpty() || (paint.from() >>> 24) == 0 && (paint.to() >>> 24) == 0) return;
        float[] quads = new float[text.length() * 8];
        int count = 0;
        float pen = x, baseline = top - ascender * size; int prev = -1;
        float left = Float.MAX_VALUE, upper = Float.MAX_VALUE, right = -Float.MAX_VALUE, lower = -Float.MAX_VALUE;
        for (int i = 0; i < text.length(); ) {
            int code = text.codePointAt(i);
            i += Character.charCount(code);
            if (code == '\n') { pen = x; baseline += lineHeight(size); prev = -1; continue; }
            Glyph glyph = glyph(code);
            if (glyph == null) continue;
            pen += kerning.get(key(prev, glyph.unicode)) * size;
            if (glyph.plane != null) {
                Bounds p = glyph.plane, a = glyph.atlas;
                float l = pen + p.left * size, t = baseline + p.top * size, r = pen + p.right * size, b = baseline + p.bottom * size;
                int q = count++ * 8;
                quads[q] = l; quads[q + 1] = t; quads[q + 2] = r; quads[q + 3] = b;
                quads[q + 4] = a.left / atlasW; quads[q + 5] = a.top / atlasH; quads[q + 6] = a.right / atlasW; quads[q + 7] = a.bottom / atlasH;
                left = Math.min(left, Math.min(l, r)); upper = Math.min(upper, Math.min(t, b));
                right = Math.max(right, Math.max(l, r)); lower = Math.max(lower, Math.max(t, b));
            }
            pen += glyph.advance * size; prev = glyph.unicode;
        }
        if (count > 0) submit(g, quads, count, left, upper, right, lower, paint);
    }
    /** Draws a named icon (icon fonts only) centred on cx, cy. */
    public void icon(GuiGraphicsExtractor g, String name, float cx, float cy, float size, int color) {
        Integer code = names.get(name);
        Glyph glyph = code == null ? null : glyphs.get(code.intValue());
        if (glyph == null || glyph.plane == null || (color >>> 24) == 0) return;
        Bounds p = glyph.plane, a = glyph.atlas;
        float l = cx + (p.left - .5f) * size, t = cy + (p.top + .5f) * size, r = cx + (p.right - .5f) * size, b = cy + (p.bottom + .5f) * size;
        submit(g, new float[]{l, t, r, b, a.left / atlasW, a.top / atlasH, a.right / atlasW, a.bottom / atlasH}, 1,
                Math.min(l, r), Math.min(t, b), Math.max(l, r), Math.max(t, b), Gradient.solid(color));
    }
    private void submit(GuiGraphicsExtractor g, float[] quads, int count, float l, float t, float r, float b, Gradient paint) {
        var pose = new Matrix3x2f(g.pose());
        var clip = g.scissorStack.peek();
        var bounds = new ScreenRectangle((int) Math.floor(l), (int) Math.floor(t), (int) Math.ceil(r - l) + 1, (int) Math.ceil(b - t) + 1).transformMaxBounds(pose);
        if (clip != null) bounds = bounds.intersection(clip);
        // Text needs core/ui; before it is compiled (the first loading screen) it is skipped, as it always was.
        if (bounds != null && UiBatch.ready()) g.guiRenderState.addGuiElement(new Run(pose, slot == 0 ? UiBatch.textures(texture) : UiBatch.textures(),
                quads, count, UiBatch.TEXT | slot, range16, paint, clip, bounds));
    }
    private record Run(Matrix3x2fc pose, TextureSetup textureSetup, float[] quads, int count, int flags, int range16, Gradient paint,
                       ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return UiBatch.PIPELINE; }
        public void buildVertices(VertexConsumer v) {
            boolean flat = paint.from() == paint.to();
            int solid = paint.from();
            for (int i = 0; i < count; i++) {
                int q = i * 8;
                float l = quads[q], t = quads[q + 1], r = quads[q + 2], b = quads[q + 3];
                float u0 = quads[q + 4], v0 = quads[q + 5], u1 = quads[q + 6], v1 = quads[q + 7];
                UiBatch.vertex(v, pose, l, t, flat ? solid : paint.at(l, t), u0, v0, 0, 0, flags, 0, range16);
                UiBatch.vertex(v, pose, l, b, flat ? solid : paint.at(l, b), u0, v1, 0, 0, flags, 0, range16);
                UiBatch.vertex(v, pose, r, b, flat ? solid : paint.at(r, b), u1, v1, 0, 0, flags, 0, range16);
                UiBatch.vertex(v, pose, r, t, flat ? solid : paint.at(r, t), u1, v0, 0, 0, flags, 0, range16);
            }
        }
    }
    private record Glyph(int unicode, float advance, Bounds plane, Bounds atlas) {}
    private record Data(Atlas atlas, Metrics metrics, RawGlyph[] glyphs, Kern[] kerning, Map<String, Integer> names) {}
    private record Atlas(String type, String yOrigin, float distanceRange, float width, float height) {}
    private record Metrics(float lineHeight, float ascender, float capHeight) {}
    private record RawGlyph(int unicode, float advance, Bounds planeBounds, Bounds atlasBounds) {}
    private record Bounds(float left, float top, float right, float bottom) {}
    private record Kern(int unicode1, int unicode2, float advance) {}
}
