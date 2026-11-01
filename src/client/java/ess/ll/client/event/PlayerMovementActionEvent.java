package ess.ll.client.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PlayerMovementActionEvent extends Event {
   public static final PlayerMovementActionEvent INSTANCE = new PlayerMovementActionEvent();
   private PlayerMovementActionEvent.Action action = PlayerMovementActionEvent.Action.VELOCITY;

   public void fireSprint(Player player, boolean sprinting) {
      if (!player.level().isClientSide()) {
         player.setSprinting(sprinting);
      } else {
         this.action = PlayerMovementActionEvent.Action.SPRINT;
         super.dispatch();
         if (this.isCancelled()) {
            this.setCancelled(false);
         } else {
            player.setSprinting(sprinting);
         }
      }
   }

   public void fireVelocity(Player player, Vec3 vec) {
      if (!player.level().isClientSide()) {
         player.setDeltaMovement(vec);
      } else {
         this.action = PlayerMovementActionEvent.Action.VELOCITY;
         super.dispatch();
         if (this.isCancelled()) {
            this.setCancelled(false);
         } else {
            player.setDeltaMovement(vec);
         }
      }
   }

   public PlayerMovementActionEvent.Action getAction() {
      return this.action;
   }

   public enum Action {
      VELOCITY,
      SPRINT;
   }
}
