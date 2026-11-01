package ess.ll.client.features.inventory;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;

public final class InventoryInputPolicy {
    public static boolean consumesMovementKeys(Screen screen) {
        if (screen == null) {
            return false;
        } else {
            return !(screen instanceof ChatScreen) && !(screen instanceof SignEditScreen) ? screen.getFocused() instanceof EditBox var1 && var1.canConsumeInput() : true;
        }
    }

    private InventoryInputPolicy() {
    }
}
