package ess.ll.client.render.targetfx;

import ess.ll.client.render.targetfx.model.ThemeColorRole;
import ess.ll.client.render.Theme;

/** Supplies the existing ESS theme to the transferred world renderer. */
public final class ClickGuiRenderer {
    private ClickGuiRenderer() {}
    public static boolean checkCondition2() { return false; }
    public static int calculateValue3(ThemeColorRole role) {
        return role==ThemeColorRole.MAIN_TWO ? Theme.accentLight() : Theme.accent();
    }
    public static int getThemeColor(ThemeColorRole role) { return calculateValue3(role); }
}
