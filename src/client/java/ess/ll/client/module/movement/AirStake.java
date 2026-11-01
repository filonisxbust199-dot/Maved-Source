package ess.ll.client.module.movement;

import ess.ll.client.event.EntityMoveEvent;
import ess.ll.client.event.EntityVelocityEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.network.PreMovementPacketEvent;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.util.PlayerUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class AirStake extends ClientModule {
    private final ModeSetting mode;
    private int cancelledMovementPackets;
    private Vec3 savedVelocity;

    @Override
    public void onEnable() {
        this.savedVelocity = null;
        this.cancelledMovementPackets = 0;
        super.onEnable();
    }

    @Subscribe
    private void onPreMovementPacket(PreMovementPacketEvent event) {
        if (this.isPositionHoldMode()) {
            this.cancelledMovementPackets++;
            event.cancel();
        }
    }

    @Subscribe(1)
    private void onPlayerInput(PlayerInputEvent event) {
        if (this.isPositionHoldMode()) {
            event.setJump(false);
            event.stopMoving();
        }
    }

    @Subscribe
    private void onEntityVelocity(EntityVelocityEvent event) {
        if (this.isPositionHoldMode()) {
            Entity entity = event.getEntity();
            if (entity instanceof LocalPlayer) {
                if (this.savedVelocity == null) {
                    this.savedVelocity = event.getVelocity();
                }
                event.setVelocity(Vec3.ZERO);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            if (this.savedVelocity != null) {
                mc.player.setDeltaMovement(this.savedVelocity);
            }
            if (this.cancelledMovementPackets > 15) {
                PlayerUtils.sendSilently(new StatusOnly(true, mc.player.horizontalCollision));
            }
        }
        this.cancelledMovementPackets = 0;
        super.onDisable();
    }

    private boolean isPositionHoldMode() {
        return this.mode.is("Обычный");
    }

    @Subscribe
    private void onPacket(PacketEvent event) {
        if (this.mode.is("Обход Funtime") && event.isSend() && event.getPacket() instanceof ServerboundClientTickEndPacket) {
            event.cancel();
        }
    }

    public AirStake() {
        super(ModuleCategory.Movement, ModuleSubCategory.Main);
        this.mode = new ModeSetting("Режим").addModes("Обычный", "Обход Funtime").withDefault("Обычный").withContainer(this);
        this.cancelledMovementPackets = 0;
        this.savedVelocity = null;
        this.setName("Air Stake");
    }

    @Subscribe(4)
    private void onEntityMove(EntityMoveEvent event) {
        if (this.isPositionHoldMode()) {
            event.setMovement(Vec3.ZERO);
        }
    }
}
