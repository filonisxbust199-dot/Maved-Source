package ess.ll.client.render;

import ess.ll.client.command.ClientData;
import ess.ll.client.module.ClientModules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-wide palette and surface styling, edited in the ClickGUI "Темы" tab. Colours live in the Theme
 * module's settings (so they are saved with state.json and profiles); a preset is "current" when every
 * colour matches it.
 */
public final class Theme {
    private Theme() {}
    /** file is the saved theme's file name, null for built-in presets. */
    public record Preset(String name,int accent,int background,int card,int field,int text,int secondary,int hint,int border,String file) {
        Preset(String name,int accent,int background,int card,int field,int text,int secondary,int hint,int border) {
            this(name,accent,background,card,field,text,secondary,hint,border,null);
        }
        public int[] colors() { return new int[]{accent,background,card,field,text,secondary,hint,border}; }
        public static Preset of(String name,int[] c,String file) { return new Preset(name,c[0],c[1],c[2],c[3],c[4],c[5],c[6],c[7],file); }
        public boolean light() { return luminance(background)>.5f; }
    }
    public static final List<Preset> PRESETS=List.of(
            new Preset("Лёд",0xff63c8ff,0xff0b0d12,0xff12151c,0xff1b1f29,0xfff4f8ff,0xffaab3c2,0xff6f7888,0x12ffffff),
            new Preset("Графит",0xfff0f0f0,0xff0e0e10,0xff17171a,0xff212125,0xfffafafa,0xffa1a1aa,0xff68686f,0x10ffffff),
            new Preset("Сирень",0xffb48cff,0xff0e0b16,0xff171223,0xff221b31,0xfff5f0ff,0xffb3a9c9,0xff776d8d,0x12ffffff),
            new Preset("Закат",0xffff8a65,0xff120d0c,0xff1c1514,0xff271e1c,0xfffff5f0,0xffc7b2aa,0xff87726b,0x12ffffff),
            new Preset("Аква",0xff4fe3c1,0xff081110,0xff0f1a18,0xff172422,0xffeffffb,0xff9fb9b3,0xff66807a,0x12ffffff),
            new Preset("Рубин",0xffff5470,0xff12090b,0xff1c1013,0xff28171b,0xfffff0f3,0xffc6a5ad,0xff86686f,0x12ffffff),
            new Preset("Полночь",0xff7c9cff,0xff080b18,0xff0f1425,0xff182036,0xffeef2ff,0xffa3acc9,0xff69728f,0x12ffffff),
            new Preset("Лес",0xff7bd88f,0xff0a100b,0xff111a13,0xff19251b,0xfff0fff2,0xffa6bba9,0xff6b806e,0x12ffffff),
            new Preset("Янтарь",0xffffc44d,0xff100d07,0xff1a160d,0xff252014,0xfffff9ec,0xffc3b796,0xff857a5d,0x12ffffff),
            new Preset("Кофе",0xffd4a373,0xff110d0a,0xff1b1611,0xff262019,0xfffbf3ea,0xffbfae9c,0xff837262,0x12ffffff),
            new Preset("Светлая",0xff2f80ed,0xfff3f5f9,0xffffffff,0xffe9edf3,0xff141821,0xff586174,0xff98a1b1,0x0f000000),
            new Preset("Сакура",0xffe0569b,0xfffbf4f7,0xffffffff,0xfff4e6ed,0xff2a1520,0xff7a5a69,0xffb39aa6,0x10000000),
            new Preset("Мята",0xff12a383,0xfff1f7f5,0xffffffff,0xffe2efea,0xff10201b,0xff4f6a62,0xff93aaa2,0x10000000),
            new Preset("Песок",0xffc27c2c,0xfff7f3ec,0xfffffdf9,0xffefe8dc,0xff231a0f,0xff6f604c,0xffa8987f,0x12000000),
            new Preset("Лаванда",0xff7b5cff,0xfff5f3fd,0xffffffff,0xffebe7fa,0xff1b1530,0xff5f5878,0xffa39db8,0x10000000),
            new Preset("Небо",0xff0ea5e9,0xfff0f7fc,0xffffffff,0xffe1eef7,0xff0c1a24,0xff4d6575,0xff93a8b6,0x10000000),
            new Preset("Персик",0xfff2704b,0xfffdf5f1,0xffffffff,0xfff6e8e1,0xff2b1710,0xff7b5d52,0xffb59f96,0x10000000),
            new Preset("Бумага",0xff1f1f1f,0xfff4f3ef,0xfffbfaf7,0xffe9e7e1,0xff161616,0xff5c5b57,0xff9c9a94,0x14000000));
    private static List<Preset> saved=List.of();

    public static int accent() { return ClientModules.THEME_ACCENT.get(); }
    public static int accentLight() { return mix(accent(),0xffffffff,.55f); }
    public static int background() { return ClientModules.THEME_BACKGROUND.get(); }
    public static int card() { return ClientModules.THEME_CARD.get(); }
    public static int field() { return ClientModules.THEME_FIELD.get(); }
    public static int text() { return ClientModules.THEME_TEXT.get(); }
    public static int secondary() { return ClientModules.THEME_SECONDARY.get(); }
    public static int hint() { return ClientModules.THEME_HINT.get(); }
    public static int border() { return ClientModules.THEME_BORDER.get(); }
    /** Text drawn on an accent fill. */
    public static int onAccent() { return luminance(accent())>.6f ? 0xff0b0d12 : 0xffffffff; }
    public static boolean light() { return luminance(background())>.5f; }
    public static float radius() { return ClientModules.THEME_RADIUS.get(); }
    /** Animation speed multiplier (1 = default). */
    public static float speed() { return ClientModules.THEME_SPEED.get()/100f; }
    public static boolean glass() { return ClientModules.BACKGROUND_STYLE.is("Блюр") && ClientModules.BLUR_POWER.get()>0; }

    public static int[] palette() {
        return new int[]{accent(),background(),card(),field(),text(),secondary(),hint(),border()};
    }
    public static void apply(Preset preset) {
        int[] c=preset.colors();
        ClientModules.THEME_ACCENT.set(c[0]); ClientModules.THEME_BACKGROUND.set(c[1]); ClientModules.THEME_CARD.set(c[2]);
        ClientModules.THEME_FIELD.set(c[3]); ClientModules.THEME_TEXT.set(c[4]); ClientModules.THEME_SECONDARY.set(c[5]);
        ClientModules.THEME_HINT.set(c[6]); ClientModules.THEME_BORDER.set(c[7]);
    }
    /** Ice preset and default shape / effect settings. */
    public static void reset() {
        apply(PRESETS.getFirst());
        ClientModules.THEME_RADIUS.set(10); ClientModules.THEME_OPACITY.set(92); ClientModules.THEME_SPEED.set(100);
        ClientModules.BACKGROUND_STYLE.select("Блюр");
        ClientModules.GLASS_OPACITY.set(.8f); ClientModules.BLUR_POWER.set(19); ClientModules.GLOW_POWER.set(7);
    }
    /** Built-in presets followed by saved themes. */
    public static List<Preset> all() {
        List<Preset> list=new ArrayList<>(PRESETS); list.addAll(saved); return list;
    }
    public static boolean isCurrent(Preset preset) {
        return java.util.Arrays.equals(preset.colors(),palette());
    }
    public static void reloadSaved() {
        try { saved=ClientData.themes(); } catch(Exception e) { System.err.println("[ESS] Cannot read themes: "+e.getMessage()); saved=List.of(); }
    }
    public static Preset saveCurrent() throws java.io.IOException {
        Preset preset=ClientData.saveTheme(palette()); reloadSaved(); return preset;
    }
    public static void delete(Preset preset) throws java.io.IOException {
        if(preset.file()==null) return;
        ClientData.deleteTheme(preset.file()); reloadSaved();
    }

    public static int withAlpha(int color,float alpha) {
        return (Math.clamp(Math.round(alpha*255),0,255)<<24)|(color&0xffffff);
    }
    public static int mix(int a,int b,float t) {
        t=Math.clamp(t,0,1);
        int result=0;
        for(int shift=0;shift<32;shift+=8) {
            int x=(a>>>shift)&255,y=(b>>>shift)&255;
            result|=Math.round(x+(y-x)*t)<<shift;
        }
        return result;
    }
    public static float luminance(int color) {
        return (.299f*((color>>16)&255)+.587f*((color>>8)&255)+.114f*(color&255))/255f;
    }
    /** Background opacity from Темы → Непрозрачность. */
    private static float density() { return ClientModules.THEME_OPACITY.get()/100f; }
    /**
     * Cards and fields follow Непрозрачность too. As they turn see-through they also darken a little,
     * so they stay apart from the window behind them instead of melting into one surface.
     */
    public static int plate(int color) {
        float d=density(),dark=Math.clamp((1-d)/.4f,0,1)*(light() ? .05f : .14f);
        int shaded=mix(color,color&0xff000000,dark);
        return withAlpha(shaded,((color>>>24)/255f)*(float)Math.pow(d,1.6));
    }

    /** HUD widget surface. */
    public static void surface(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius) {
        surface(g,x,y,w,h,radius,1);
    }
    /** Opacity fades the whole surface (shadow and fill) for show / hide animations. */
    public static void surface(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float opacity) {
        fill(g,x,y,w,h,radius,opacity,1);
    }
    /** Large panel surface; its shadow is spread a little wider. */
    public static void panel(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float opacity) {
        fill(g,x,y,w,h,radius,opacity,1.6f);
    }
    /** A surface without its shadow, for panels that sit on another panel. */
    public static void inset(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float opacity) {
        fill(g,x,y,w,h,radius,opacity,0);
    }
    /** Dark HUD plate, its header band and the text on them; they stay the same in every theme. */
    public static final int PLATE=0xff121317, PLATE_HEADER=0x59000000, PLATE_TEXT=0xfff4f5fa, PLATE_HINT=0xffa6a8bb;
    private static final int GLOW=0x2c2d33;
    private static final float GLOW_LAYER=.12f;
    /**
     * The glow around a surface, from Темы → Сила свечения: nested rounded rectangles a window pixel apart, fading
     * outwards. spread widens it for large panels.
     */
    public static void shadow(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float opacity,float spread) {
        float power=ClientModules.GLOW_POWER.get(), rings=power*spread;
        if(rings<1 || opacity<=.004f) return;
        float strength=Math.min(1,power/32+.45f)*255*GLOW_LAYER*opacity, pixel=UiRenderer.pixel(g);
        boolean circular=UiRenderer.circular;
        UiRenderer.circular=true;
        try {
            for(int i=(int)rings;i>0;i--) {
                float t=1-i/rings, grow=i*pixel;
                int layer=(int)(strength*t*t);
                if(layer>0) UiRenderer.rounded(g,x-grow,y-grow,w+grow*2,h+grow*2,(rings+i)*pixel,layer<<24|GLOW);
            }
        } finally { UiRenderer.circular=circular; }
    }
    /** Dark HUD plate: the glow, the blurred frame under it, and the plate colour at Темы → Прозрачность. */
    public static void plate(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float opacity) {
        if(opacity<=.004f) return;
        shadow(g,x,y,w,h,radius,opacity,1);
        int color=withAlpha(PLATE,ClientModules.GLASS_OPACITY.get());
        if(glass()) GlassRenderer.glass(g,x,y,w,h,radius,color,opacity);
        else UiRenderer.rounded(g,x,y,w,h,radius,withAlpha(color,((color>>>24)/255f)*opacity));
    }
    /** The darker band of a plate: limit deep from its top edge, or from its left edge. */
    public static void plateBand(GuiGraphicsExtractor g,float x,float y,float w,float h,boolean top,float limit,float radius,float opacity) {
        GlassRenderer.band(g,x,y,w,h,radius,top,limit,withAlpha(PLATE_HEADER,((PLATE_HEADER>>>24)/255f)*opacity));
    }
    private static void fill(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,float opacity,float spread) {
        if(opacity<=.004f) return;
        if(spread>0) shadow(g,x,y,w,h,radius,opacity,spread);
        int color=background();
        if(glass()) GlassRenderer.glass(g,x,y,w,h,radius,withAlpha(color,ClientModules.GLASS_OPACITY.get()),opacity);
        else UiRenderer.rounded(g,x,y,w,h,radius,withAlpha(color,density()*opacity));
    }
}
