package ess.ll.client.features.crystalv2;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Rotation implements V2Access {
   public float yaw;
   public float pitch;

   public Rotation(net.minecraft.world.entity.Entity entity) {
      this.yaw = entity.getYRot();
      this.pitch = entity.getXRot();
   }

   public Rotation(Vec3 vec) {
      Rotation calculated = CalculateFovUtil.calculate(vec);
      this.yaw = calculated.getYaw();
      this.pitch = calculated.getPitch();
   }

   public float getDelta(Rotation target) {
      float yawDelta = Mth.wrapDegrees(target.getYaw() - this.yaw);
      float pitchDelta = target.getPitch() - this.pitch;
      return (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
   }

   public double getDeltaDouble(Rotation target) {
      double yawDelta = Mth.wrapDegrees(target.getYaw() - this.yaw);
      double pitchDelta = Mth.wrapDegrees(target.getPitch() - this.pitch);
      return Math.hypot(yawDelta, pitchDelta);
   }

   public static Vec2 camera() {
      return new Vec2(cameraYaw(), cameraPitch());
   }

   public static float cameraYaw() {
      return Mth.wrapDegrees(mc.gameRenderer.mainCamera().yRot() + (mc.options.getCameraType().isMirrored() ? 180 : 0));
   }

   public static float cameraPitch() {
      return (mc.options.getCameraType().isMirrored() ? -1 : 1) * mc.gameRenderer.mainCamera().xRot();
   }

   public final Vec3 toVector() {
      float f = this.pitch * (float) (Math.PI / 180.0);
      float g = -this.yaw * (float) (Math.PI / 180.0);
      float h = Mth.cos(g);
      float i = Mth.sin(g);
      float j = Mth.cos(f);
      float k = Mth.sin(f);
      return new Vec3(i * j, -k, h * j);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Rotation other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         return Float.compare(this.getYaw(), other.getYaw()) != 0 ? false : Float.compare(this.getPitch(), other.getPitch()) == 0;
      }
   }

   public boolean canEqual(Object other) {
      return other instanceof Rotation;
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getYaw());
      return result * 59 + Float.floatToIntBits(this.getPitch());
   }

   @Override
   public String toString() {
      return "Rotation(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   public Rotation() {
   }

   public Rotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }
}
