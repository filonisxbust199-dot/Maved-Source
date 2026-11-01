package ess.ll.client.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The vanilla option list of this settings screen, shown by the main menu's settings page. */
@Mixin(MouseSettingsScreen.class)
public interface MenuMouseAccessor {
    @Invoker("options") static OptionInstance<?>[] ess$options(Options options) { throw new AssertionError(); }
}