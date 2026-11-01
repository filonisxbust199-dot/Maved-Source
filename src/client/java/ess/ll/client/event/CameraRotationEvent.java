package ess.ll.client.event;

import net.minecraft.client.Camera;
import ess.ll.client.mixin.CameraMixin_1260;

public class CameraRotationEvent extends Event {
   public static final CameraRotationEvent INSTANCE = new CameraRotationEvent();
   private float yaw;
   private float pitch;
   private float roll;

   public void setRoll(float roll) {
      this.roll = roll;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getRoll() {
      return this.roll;
   }

   public void fire(Camera camera, float yaw, float pitch, float roll) {
      if (!this.hasListeners()) {
         ((CameraMixin_1260)(Object)camera).IsetRotation(yaw, pitch);
      } else {
         this.yaw = yaw;
         this.pitch = pitch;
         this.roll = roll;
         this.setCancelled(false);
         super.dispatch();
         this.consumeCancelled();
         ((CameraMixin_1260)(Object)camera).IsetRotation(this.yaw, this.pitch);
      }
   }

   public float getPitch() {
      return this.pitch;
   }
}
