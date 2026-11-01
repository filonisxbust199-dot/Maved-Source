package ess.ll.client.storage;

import net.minecraft.util.Mth;

public final class RotationDerivativeHistory {
   private float lastYaw;
   private float lastPitch;
   private boolean hasRotation;
   private float lastVelYaw;
   private float lastVelPitch;
   private boolean hasVelocity;
   private float lastAccelYaw;
   private float lastAccelPitch;
   private boolean hasAcceleration;
   private float lastErrorYaw;
   private float lastErrorPitch;
   private boolean hasError;

   public boolean hasRotation() {
      return this.hasRotation;
   }

   public void reset() {
      this.hasRotation = false;
      this.hasVelocity = false;
      this.hasAcceleration = false;
      this.hasError = false;
      this.lastVelYaw = 0.0F;
      this.lastVelPitch = 0.0F;
      this.lastAccelYaw = 0.0F;
      this.lastAccelPitch = 0.0F;
   }

   public RotationDerivativeHistory.Derivatives derive(float yaw, float pitch, float errorYaw, float errorPitch) {
      float velYaw = this.hasRotation ? Mth.wrapDegrees(yaw - this.lastYaw) : 0.0F;
      float velPitch = this.hasRotation ? pitch - this.lastPitch : 0.0F;
      float accelYaw = this.hasVelocity ? velYaw - this.lastVelYaw : 0.0F;
      float accelPitch = this.hasVelocity ? velPitch - this.lastVelPitch : 0.0F;
      float jerkYaw = this.hasAcceleration ? accelYaw - this.lastAccelYaw : 0.0F;
      float jerkPitch = this.hasAcceleration ? accelPitch - this.lastAccelPitch : 0.0F;
      float errVelYaw = this.hasError ? Mth.wrapDegrees(errorYaw - this.lastErrorYaw) : 0.0F;
      float errVelPitch = this.hasError ? errorPitch - this.lastErrorPitch : 0.0F;
      return new RotationDerivativeHistory.Derivatives(velYaw, velPitch, accelYaw, accelPitch, jerkYaw, jerkPitch, errVelYaw, errVelPitch);
   }

   public void push(RotationDerivativeHistory.Derivatives derivatives, float yaw, float pitch, float errorYaw, float errorPitch) {
      if (this.hasVelocity) {
         this.hasAcceleration = true;
      }

      if (this.hasRotation) {
         this.hasVelocity = true;
      }

      this.lastVelYaw = derivatives.velYaw();
      this.lastVelPitch = derivatives.velPitch();
      this.lastAccelYaw = derivatives.accelYaw();
      this.lastAccelPitch = derivatives.accelPitch();
      this.lastYaw = yaw;
      this.lastPitch = pitch;
      this.hasRotation = true;
      this.lastErrorYaw = errorYaw;
      this.lastErrorPitch = errorPitch;
      this.hasError = true;
   }

   public record Derivatives(float velYaw, float velPitch, float accelYaw, float accelPitch, float jerkYaw, float jerkPitch, float errVelYaw, float errVelPitch) {
   }
}
