package ess.ll.client.event;

import net.minecraft.client.player.LocalPlayer;

public class ElytraStartEvent extends Event {
    public static final ElytraStartEvent INSTANCE = new ElytraStartEvent();
    private LocalPlayer player;
    private boolean startFlying;

    public void requestStart() {
        this.startFlying = true;
    }

    public LocalPlayer getPlayer() {
        return this.player;
    }

    public boolean fire(LocalPlayer player) {
        this.player = player;
        this.startFlying = false;
        super.dispatch();
        return this.startFlying;
    }

    public boolean isStartFlying() {
        return this.startFlying;
    }
}
