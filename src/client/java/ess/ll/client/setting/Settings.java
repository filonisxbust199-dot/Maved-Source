package ess.ll.client.setting;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Typed module settings. Values are persisted by ClientData (state.json and profiles). */
public final class Settings {
    private Settings() {}
    public sealed interface Setting permits Base {
        String name();
        /** Hidden settings are not shown in the ClickGUI (they keep their value and are still saved). */
        boolean visible();
    }
    /** Name and visibility shared by every setting type. */
    public abstract static sealed class Base implements Setting
            permits BooleanSetting, SliderSetting, ModeSettings, MultiBooleanSettings, ColorSetting, ActionSetting, KeybindSetting {
        private final String name;
        private BooleanSupplier visibility=()->true;
        Base(String name) { this.name=name; }
        public String name() { return name; }
        public boolean visible() { return visibility.getAsBoolean(); }
        void visibility(BooleanSupplier condition) { visibility=condition; }
    }
    public static non-sealed class BooleanSetting extends Base {
        private boolean value;
        public BooleanSetting(String name,boolean value) { super(name); this.value=value; }
        public BooleanSetting visibleWhen(BooleanSupplier condition) { visibility(condition); return this; }
        public boolean get() { return value; }
        public void toggle() { value=!value; }
    }
    /** Captured from the next key or mouse press, like a module bind. */
    public static non-sealed class KeybindSetting extends Base {
        private int key;
        public KeybindSetting(String name,int key){super(name);this.key=key;}
        public KeybindSetting visibleWhen(BooleanSupplier condition) { visibility(condition); return this; }
        public int get(){return key;}
        public void set(int key){this.key=key;}
    }
    public static non-sealed class SliderSetting extends Base {
        public final float min,max,step;
        private float value;
        public SliderSetting(String name,float min,float max,float step,float value) {
            super(name);
            if(max<=min || step<=0) throw new IllegalArgumentException("Invalid slider range");
            this.min=min; this.max=max; this.step=step; set(value);
        }
        public SliderSetting visibleWhen(BooleanSupplier condition) { visibility(condition); return this; }
        public float get() { return value; }
        public void set(float v) { if(Float.isFinite(v)) value=Math.clamp(min+Math.round((v-min)/step)*step,min,max); }
        public float fraction() { return (value-min)/(max-min); }
        public String format(float v) {
            if(step>=1) return String.valueOf(Math.round(v));
            return String.format(Locale.ROOT,step<.1f-1e-4f ? "%.2f" : "%.1f",v);
        }
    }
    public static non-sealed class ModeSettings extends Base {
        public final List<String> options;
        private int selected;
        public ModeSettings(String name,String... options) {
            super(name);
            if(options.length==0) throw new IllegalArgumentException("Empty modes");
            this.options=List.of(options);
        }
        public ModeSettings visibleWhen(BooleanSupplier condition) { visibility(condition); return this; }
        public String get() { return options.get(selected); }
        public boolean is(String option) { return get().equals(option); }
        public void select(int index) { if(index>=0 && index<options.size()) selected=index; }
        public void select(String option) { select(options.indexOf(option)); }
        public boolean selected(int index) { return index==selected; }
    }
    public static non-sealed class MultiBooleanSettings extends Base {
        public final List<String> options;
        private final boolean[] selected;
        public MultiBooleanSettings(String name,String... options) {
            super(name); this.options=List.of(options); selected=new boolean[options.length]; Arrays.fill(selected,true);
        }
        /** Starts with none of the options selected. */
        public MultiBooleanSettings none() { Arrays.fill(selected,false); return this; }
        /** Options added after configs were first saved without the "#known" list; such saves keep their defaults. */
        public final Set<String> addedLater=new HashSet<>();
        public MultiBooleanSettings addedLater(String... names) { addedLater.addAll(List.of(names)); return this; }
        public MultiBooleanSettings visibleWhen(BooleanSupplier condition) { visibility(condition); return this; }
        public boolean selected(int index) { return index>=0 && index<selected.length && selected[index]; }
        public boolean selected(String option) { return selected(options.indexOf(option)); }
        public void toggle(int index) { if(index>=0 && index<selected.length) selected[index]=!selected[index]; }
        public String summary() {
            int count=0; String first="Нет";
            for(int i=0;i<selected.length;i++) if(selected(i)) { if(count++==0) first=options.get(i); }
            return count<=1 ? first : first+" +"+(count-1);
        }
    }
    /** ARGB colour, edited as hue / saturation / brightness (and opacity when alpha is enabled). */
    public static non-sealed class ColorSetting extends Base {
        public final boolean alpha;
        private int value;
        public ColorSetting(String name,int argb) { this(name,argb,false); }
        public ColorSetting(String name,int argb,boolean alpha) { super(name); this.alpha=alpha; set(argb); }
        public ColorSetting visibleWhen(BooleanSupplier condition) { visibility(condition); return this; }
        public int get() { return value; }
        public void set(int argb) { value=alpha ? argb : argb|0xff000000; }
        public float[] hsb() {
            return java.awt.Color.RGBtoHSB((value>>16)&255,(value>>8)&255,value&255,null);
        }
        public void setHsb(float hue,float saturation,float brightness) {
            set((value&0xff000000)|(java.awt.Color.HSBtoRGB(hue,Math.clamp(saturation,0,1),Math.clamp(brightness,0,1))&0xffffff));
        }
        public void setAlpha(float a) { if(alpha) value=(Math.round(Math.clamp(a,0,1)*255)<<24)|(value&0xffffff); }
        public String hex() { return String.format(Locale.ROOT,alpha ? "#%08X" : "#%06X",alpha ? value : value&0xffffff); }
        /** Parses #RRGGBB or #AARRGGBB. */
        public static int parse(String text) {
            String s=text.startsWith("#") ? text.substring(1) : text;
            if(!s.matches("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")) throw new IllegalArgumentException("Цвет: #RRGGBB или #AARRGGBB");
            long v=Long.parseLong(s,16);
            return s.length()==6 ? (int)(v|0xff000000L) : (int)v;
        }
    }
    /** A button in the ClickGUI; it has no value and is not saved. */
    public static final class ActionSetting extends Base {
        private final Runnable action;
        public ActionSetting(String name,Runnable action) { super(name); this.action=action; }
        public ActionSetting visibleWhen(BooleanSupplier condition) { visibility(condition); return this; }
        public void run() { action.run(); }
    }
}
