package ess.ll.client.module;

import ess.ll.client.module.movement.AutoJumpModule;
import ess.ll.client.module.player.AutoRespawnModule;
import ess.ll.client.module.render.GammaModule;
import net.minecraft.client.Minecraft;

/** Runs the small built-in modules that do not need the event bus. */
public final class ModuleRuntime {
    private ModuleRuntime() {}

    public static void tick(Minecraft mc) {
        boolean world=mc.player!=null && mc.level!=null;
        GammaModule.tick(mc,world);
        AutoJumpModule.tick(mc,world);
        AutoRespawnModule.tick(mc,world);
    }

    public static void restore(Minecraft mc) {
        GammaModule.restore(mc);
        AutoJumpModule.restore(mc);
    }
}
