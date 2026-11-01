package ess.ll.client.module.player;

import ess.ll.client.module.ClientModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

public final class AutoRespawnModule {
    private static long deadSince;
    private static boolean respawnRequested;
    private AutoRespawnModule() {}

    public static void tick(Minecraft mc,boolean world) {
        if(world && ClientModules.AUTO_RESPAWN.enabled() && mc.player.isDeadOrDying()
                && !mc.level.getLevelData().isHardcore()) {
            if(deadSince==0) deadSince=System.nanoTime();
            if(!respawnRequested && mc.gui.screen() instanceof DeathScreen
                    && (System.nanoTime()-deadSince)/1e9>=ClientModules.RESPAWN_DELAY.get()) {
                mc.player.respawn();
                respawnRequested=true;
                mc.gui.setScreen(null);
            }
        } else {
            deadSince=0;
            respawnRequested=false;
        }
    }
}
