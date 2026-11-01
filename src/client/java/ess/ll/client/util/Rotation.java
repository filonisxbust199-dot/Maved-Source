package ess.ll.client.util;

import ess.ll.client.core.FreeLookController;
import ess.ll.client.integration.MinecraftAccess;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Rotation implements MinecraftAccess {
   private float yaw;
   private float pitch;
   private static final double YAW_OFFSET = 90.0;

   public Rotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   @Override
   public int hashCode() {
      byte prime = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getYaw());
      return result * 59 + Float.floatToIntBits(this.getPitch());
   }

   public double distanceTo(Rotation rotation) {
      double deltaYaw = Mth.wrapDegrees(rotation.getYaw() - this.yaw);
      double deltaPitch = Mth.wrapDegrees(rotation.getPitch() - this.pitch);
      return Math.hypot(Math.abs(deltaYaw), Math.abs(deltaPitch));
   }

   public Rotation difference(Rotation rotation) {
      return new Rotation(wrappedDifference(rotation.yaw, this.yaw), wrappedDifference(rotation.pitch, this.pitch));
   }

   public static Rotation fromDirection(Vec3 direction) {
      float x = (float)direction.x;
      float y = (float)direction.y;
      float z = (float)direction.z;
      return new Rotation(
         (float)Mth.wrapDegrees(Math.toDegrees(Mth.atan2(z, x)) - YAW_OFFSET),
         (float)Mth.wrapDegrees(-Math.toDegrees(Mth.atan2(y, Math.hypot(x, z))))
      );
   }

   public Rotation(Entity entity) {
      this.yaw = entity.getYRot();
      this.pitch = entity.getXRot();
   }

   public float getYaw() {
      return this.yaw;
   }

   @Override
   public boolean equals(Object object) {
      if (object == this) {
         return true;
      } else if (!(object instanceof Rotation rotation)) {
         return false;
      } else if (!rotation.canEqual(this)) {
         return false;
      } else {
         return Float.compare(this.getYaw(), rotation.getYaw()) != 0 ? false : Float.compare(this.getPitch(), rotation.getPitch()) == 0;
      }
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public static Rotation freeLook() {
      return new Rotation(FreeLookController.getYaw(), FreeLookController.getPitch());
   }

   public Rotation() {
   }

   public float getPitch() {
      return this.pitch;
   }

   protected boolean canEqual(Object object) {
      return object instanceof Rotation;
   }

   @Override
   public String toString() {
      return "Rotation(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   public static float wrappedDifference(float a, float b) {
      return Mth.wrapDegrees(a - b);
   }
}
