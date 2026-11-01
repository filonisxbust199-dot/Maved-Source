package ess.ll.client.mixin;

import ess.ll.client.command.DotCommands;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clickable help: a chat link with a ".command" runs it in the client instead of sending it to the server. */
@Mixin(Screen.class)
public abstract class ScreenClickMixin {
    @Inject(method="clickCommandAction",at=@At("HEAD"),cancellable=true)
    private static void ess$dotCommand(LocalPlayer player,String command,Screen screen,CallbackInfo ci) {
        if(command.startsWith(".")) { DotCommands.execute(command); ci.cancel(); }
    }
}
