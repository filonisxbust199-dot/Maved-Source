package ess.ll.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ContainerCloseEvent extends Event {
    public static final ContainerCloseEvent INSTANCE = new ContainerCloseEvent();
    private Screen screen;
    private int containerId;

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public void fire(Minecraft mc, LocalPlayer player, CallbackInfo ci) {
        this.screen = mc.gui.screen();
        this.containerId = player.containerMenu.containerId;
        super.dispatch();
        if (this.consumeCancelled()) {
            ci.cancel();
        }
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    public int getContainerId() {
        return this.containerId;
    }
}
