package ess.ll.client.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The vanilla option list of this settings screen, shown by the main menu's settings page. */
@Mixin(VideoSettingsScreen.class)
public interface MenuVideoAccessor {
    @Invoker("qualityOptions") static OptionInstance<?>[] ess$quality(Options options) { throw new AssertionError(); }
    @Invoker("displayOptions") static OptionInstance<?>[] ess$display(Options options) { throw new AssertionError(); }
    @Invoker("preferenceOptions") static OptionInstance<?>[] ess$preference(Options options) { throw new AssertionError(); }
}