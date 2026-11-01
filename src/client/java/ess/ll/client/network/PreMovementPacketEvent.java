package ess.ll.client.network;

import ess.ll.client.event.Event;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ess.ll.client.mixin.LocalPlayerMixin_1170;

public class PreMovementPacketEvent extends Event {
   public static final PreMovementPacketEvent INSTANCE = new PreMovementPacketEvent();
   private double x;
   private double y;
   private double z;
   private double lastX;
   private double lastY;
   private double lastZ;
   private float yaw;
   private float pitch;
   private boolean onGround;
   private boolean modified;

   public void setLastX(double lastX) {
      this.lastX = lastX;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public void fire(LocalPlayer player, CallbackInfo ci) {
      this.modified = false;
      this.x = player.getX();
      this.y = player.getY();
      this.z = player.getZ();
      this.lastX = ((LocalPlayerMixin_1170)(Object)player).lastX();
      this.lastY = ((LocalPlayerMixin_1170)(Object)player).lastY();
      this.lastZ = ((LocalPlayerMixin_1170)(Object)player).lastZ();
      this.yaw = player.getYRot();
      this.pitch = player.getXRot();
      this.onGround = player.onGround();
      super.dispatch();
      if (this.consumeCancelled()) {
         ci.cancel();
      }
   }

   public double getZ() {
      return this.z;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float pitchOr(float vanilla) {
      return this.modified ? this.pitch : vanilla;
   }

   public float getPitch() {
      return this.pitch;
   }

   public double yOr(double vanilla) {
      return this.modified ? this.y : vanilla;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public float yawOr(float vanilla) {
      return this.modified ? this.yaw : vanilla;
   }

   public double xOr(double vanilla) {
      return this.modified ? this.x : vanilla;
   }

   public double getLastY() {
      return this.lastY;
   }

   public double getY() {
      return this.y;
   }

   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setLastY(double lastY) {
      this.lastY = lastY;
   }

   public double zOr(double vanilla) {
      return this.modified ? this.z : vanilla;
   }

   public void setModified(boolean modified) {
      this.modified = modified;
   }

   public double getX() {
      return this.x;
   }

   public double getLastZ() {
      return this.lastZ;
   }

   public void setY(double y) {
      this.y = y;
   }

   public boolean isModified() {
      return this.modified;
   }

   public void setLastZ(double lastZ) {
      this.lastZ = lastZ;
   }

   public boolean onGroundOr(boolean vanilla) {
      return this.onGround;
   }

   public double getLastX() {
      return this.lastX;
   }
}
