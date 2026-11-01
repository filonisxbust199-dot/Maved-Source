package ess.ll.client.module;

import ess.ll.client.module.ClientModule;
import ess.ll.client.setting.*;
import ess.ll.client.command.Keys;
import ess.ll.client.setting.Settings;
import java.util.*;

/** Live adapters: GUI, commands and profiles operate on the original module values. */
public final class CombatSettings {
    private CombatSettings() {}
    public static List<Settings.Setting> adapt(ClientModule module) {
        List<Settings.Setting> out=new ArrayList<>();
        for(var s:module.getSettings()) {
            String name=s.getDisplayName();
            if(s instanceof ToggleSetting b) out.add(new Settings.BooleanSetting(name,b.isEnabled()) {
                public boolean visible(){return s.isVisible();}
                public boolean get(){return b.isEnabled();}
                public void toggle(){b.toggle();}
            });
            else if(s instanceof NumberSetting<?> n) {
                float min=n.getMin().floatValue(),max=n.getMax().floatValue(),step=n.getStep().floatValue();
                out.add(new Settings.SliderSetting(name,min,max,step,n.getValue().floatValue()) {
                    public boolean visible(){return s.isVisible();}
                    public float get(){return n.getValue().floatValue();}
                    public void set(float v){super.set(v); n.setFromFloat(super.get());}
                    public float fraction(){return (get()-min)/(max-min);}
                });
            } else if(s instanceof ModeSetting m) out.add(new Settings.ModeSettings(name,m.getModes().toArray(String[]::new)) {
                public boolean visible(){return s.isVisible();}
                public String get(){return m.getValue();}
                public boolean selected(int i){return i>=0 && i<options.size() && options.get(i).equals(get());}
                public void select(int i){if(i>=0 && i<options.size())m.setValue(options.get(i));}
            });
            else if(s instanceof MultiToggleSetting m) out.add(new Settings.MultiBooleanSettings(name,m.getValue().stream().map(NamedOption::getName).toArray(String[]::new)) {
                public boolean visible(){return s.isVisible();}
                public boolean selected(int i){return i>=0 && i<options.size() && m.isSelected(options.get(i));}
                public void toggle(int i){if(i>=0 && i<options.size())m.setOption(options.get(i),!selected(i));}
            });
            else if(s instanceof ColorSetting color) out.add(new Settings.ColorSetting(name,color.getValue().getRGB()) {
                public boolean visible(){return s.isVisible();}
                public int get(){return color.getValue().getRGB();}
                public void set(int argb){super.set(argb);color.setValue(new java.awt.Color(super.get(),true));}
            });
            else if(s instanceof ActionListSetting actions) for(var action:actions.getValue())
                out.add(new Settings.ActionSetting(name+" · "+action.getName(),action.getDefaultValue()).visibleWhen(s::isVisible));
            else if(s instanceof KeyBindSetting k) {
                out.add(new Settings.KeybindSetting(name,k.getValue().getKey()) {
                    public boolean visible(){return s.isVisible();}
                    public int get(){return k.getValue().getKey();}
                    public void set(int key){k.getValue().setKey(key);}
                });
            } else throw new IllegalStateException("Unsupported combat setting: "+s.getClass().getName());
        }
        return List.copyOf(out);
    }
}
