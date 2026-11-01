package ess.ll.client.module.movement;

import ess.ll.client.module.ClientModules;
import net.minecraft.client.Minecraft;

public final class AutoJumpModule {
    private static Boolean original;
    private AutoJumpModule() {}

    public static void tick(Minecraft mc,boolean world) {
        if(world && ClientModules.AUTO_JUMP.enabled()) {
            if(original==null) original=mc.options.autoJump().get();
            if(!mc.options.autoJump().get()) mc.options.autoJump().set(true);
        } else restore(mc);
    }

    public static void restore(Minecraft mc) {
        if(original!=null) {
            mc.options.autoJump().set(original);
            original=null;
        }
    }
}
