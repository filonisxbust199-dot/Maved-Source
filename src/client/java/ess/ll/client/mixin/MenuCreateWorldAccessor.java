package ess.ll.client.mixin;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the main menu's world form create the world with the vanilla screen's prepared settings. */
@Mixin(CreateWorldScreen.class)
public interface MenuCreateWorldAccessor {
    @Invoker("onCreate") void ess$create();
}