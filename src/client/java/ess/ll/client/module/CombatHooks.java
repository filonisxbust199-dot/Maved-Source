package ess.ll.client.module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Entry points for the enabled combat source set. */
public final class CombatHooks {
    private static Runnable tick=()->{};
    private static java.util.function.BiConsumer<GuiGraphicsExtractor,DeltaTracker> hud=(g,d)->{};
    private CombatHooks() {}
    public static void register(Runnable onTick,java.util.function.BiConsumer<GuiGraphicsExtractor,DeltaTracker> onHud) {
        tick=onTick; hud=onHud;
    }
    public static void tick(){tick.run();}
    public static void renderHud(GuiGraphicsExtractor g,DeltaTracker d){hud.accept(g,d);}
}
