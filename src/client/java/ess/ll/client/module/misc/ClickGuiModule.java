package ess.ll.client.module.misc;

import ess.ll.client.screen.ClickGuiScreen;
import net.minecraft.client.Minecraft;

public final class ClickGuiModule {
    private ClickGuiModule() {}

    public static boolean isOpen() {
        return Minecraft.getInstance().gui.screen() instanceof ClickGuiScreen;
    }

    public static void toggle() {
        var mc=Minecraft.getInstance();
        if(mc.gui.screen() instanceof ClickGuiScreen screen) screen.onClose();
        else if(mc.level!=null && mc.gui.screen()==null) mc.gui.setScreen(new ClickGuiScreen());
    }
}
