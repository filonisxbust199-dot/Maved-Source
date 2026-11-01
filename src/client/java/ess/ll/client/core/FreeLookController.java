package ess.ll.client.core;

import ess.ll.client.event.CameraRotationEvent;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.MouseTurnEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.ModuleManager;
import net.minecraft.util.Mth;

public class FreeLookController implements MinecraftAccess {
   private static boolean active;
   private static float yaw;
   private static float pitch;
   private static boolean unlockedPitch = false;
   private static final double YAW_SENSITIVITY = 0.15;
   private static final double PITCH_SENSITIVITY = 0.15;
   private static final float MIN_PITCH = -90.0F;
   private static final float MAX_PITCH = 90.0F;

   public static boolean isPitchUnlocked() {
      return unlockedPitch;
   }

   public static void setYaw(float yaw) {
      FreeLookController.yaw = yaw;
   }

   private boolean isFreeCamActive() {
      return mc.player != null && mc.level != null ? ModuleManager.INSTANCE.freeCam().isEnabled() : false;
   }

   public static void setActive(boolean active) {
      setActive(active, true);
   }

   private void turn(double deltaYaw, double deltaPitch) {
      yaw = (float)(yaw + deltaYaw * YAW_SENSITIVITY);
      pitch = (float)(pitch + deltaPitch * PITCH_SENSITIVITY);
      if (!unlockedPitch) {
         pitch = Mth.clamp(pitch, MIN_PITCH, MAX_PITCH);
      }
   }

   public static void setPitch(float pitch) {
      FreeLookController.pitch = pitch;
   }

   public static boolean isActive() {
      return active;
   }

   public static float getYaw() {
      return yaw;
   }

   public static void setPitchUnlocked(boolean unlocked) {
      unlockedPitch = unlocked;
   }

   @Subscribe
   public void onCameraRotation(CameraRotationEvent cameraRotationEvent) {
      if (!active && !this.isFreeCamActive()) {
         yaw = cameraRotationEvent.getYaw();
         pitch = cameraRotationEvent.getPitch();
      } else {
         cameraRotationEvent.setYaw(yaw);
         cameraRotationEvent.setPitch(pitch);
         cameraRotationEvent.setCancelled(true);
      }
   }

   public static float getPitch() {
      return pitch;
   }

   public static void setActive(boolean active, boolean applyToPlayer) {
      if (FreeLookController.active != active) {
         FreeLookController.active = active;
         if (applyToPlayer) {
            applyToPlayer();
         }
      }
   }

   private static void applyToPlayer() {
      if (mc.player != null) {
         float newYaw = mc.player.getYRot() + Mth.wrapDegrees(yaw - mc.player.getYRot());
         float newPitch = Mth.clamp(pitch, -90.0F, 90.0F);
         mc.player.setYRot(newYaw);
         mc.player.setXRot(newPitch);
         mc.player.yRotO = newYaw;
         mc.player.xRotO = newPitch;
      }
   }

   public FreeLookController() {
      EventBus.register(this);
   }

   @Subscribe
   public void onMouseTurn(MouseTurnEvent mouseTurnEvent) {
      if (active || this.isFreeCamActive()) {
         this.turn(mouseTurnEvent.getDeltaYaw(), mouseTurnEvent.getDeltaPitch());
         mouseTurnEvent.setCancelled(true);
      }
   }
}
