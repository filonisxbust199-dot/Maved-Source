package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import net.minecraft.client.Minecraft;

public final class GammaModule {
    private static Double original;
    private GammaModule() {}

    public static void tick(Minecraft mc,boolean world) {
        if(world && ClientModules.GAMMA.enabled()) {
            if(original==null) original=mc.options.gamma().get();
            double brightness=ClientModules.BRIGHTNESS.get();
            if(mc.options.gamma().get()!=brightness) mc.options.gamma().set(brightness);
        } else restore(mc);
    }

    public static void restore(Minecraft mc) {
        if(original!=null) {
            mc.options.gamma().set(original);
            original=null;
        }
    }
}
