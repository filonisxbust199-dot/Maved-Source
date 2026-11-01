package ess.ll.client.setting;

import java.awt.Color;

public final class ColorSetting extends Setting<Color> {
    public ColorSetting(String name) { super(name); }
    public int getArgb() { return getValue().getRGB(); }
}
