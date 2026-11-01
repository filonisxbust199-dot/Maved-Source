package ess.ll.client.util;

import ess.ll.client.event.EntityVelocityEvent;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.movement.AirStake;
import ess.ll.client.module.player.FreeCam;
import ess.ll.client.network.PreMovementPacketEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public enum PlayerVelocityTracker implements MinecraftAccess {
   INSTANCE;

   private boolean wasFrozen;
   private boolean frozen;
   private int releaseTicks;
   private Vec3 savedVelocity = Vec3.ZERO;

   public void init() {
      EventBus.register(this);
   }

   public Vec3 getSavedVelocity() {
      return this.savedVelocity;
   }

   public boolean isFrozen() {
      return this.frozen;
   }

   public void setFrozen(boolean frozen) {
      if (this.frozen != frozen) {
         this.frozen = frozen;
         if (!frozen && mc.player != null) {
            mc.player.setDeltaMovement(this.savedVelocity);
         }
      }
   }

   public boolean shouldFreeze() {
      if (mc.player != null && mc.level != null) {
         AirStake airStake = ModuleManager.INSTANCE.airStuck();
         FreeCam freeCam = ModuleManager.INSTANCE.freeCam();
         if (this.frozen) {
            return true;
         } else {
            return airStake.isEnabled() ? true : freeCam.isEnabled() && freeCam.freezePackets.isEnabled();
         }
      } else {
         return false;
      }
   }

   @Subscribe
   public void onVelocity(EntityVelocityEvent entityVelocityEvent) {
      if (this.frozen && entityVelocityEvent.getEntity() instanceof LocalPlayer) {
         entityVelocityEvent.setVelocity(Vec3.ZERO);
         entityVelocityEvent.setCancelled(true);
      }
   }

   @Subscribe
   public void onMovementPacket(PreMovementPacketEvent preMovementPacketEvent) {
      if (this.frozen) {
         preMovementPacketEvent.cancel();
      }
   }

   @Subscribe(4)
   public void onPlayerInput(PlayerInputEvent playerInputEvent) {
      boolean freeze = this.shouldFreeze();
      if (!freeze && mc.player != null) {
         this.savedVelocity = mc.player.getDeltaMovement();
      }

      if (this.wasFrozen && !freeze) {
         this.releaseTicks = 1;
      }

      this.wasFrozen = freeze;
      if (this.frozen) {
         playerInputEvent.setForward(0.0F);
         playerInputEvent.setStrafe(0.0F);
         playerInputEvent.setJump(false);
      }

      if (this.releaseTicks > 0) {
         this.releaseTicks--;
         playerInputEvent.setForward(0.0F);
         playerInputEvent.setStrafe(0.0F);
         playerInputEvent.setJump(false);
         playerInputEvent.setSneak(false);
         playerInputEvent.setSprint(false);
      }
   }
}
