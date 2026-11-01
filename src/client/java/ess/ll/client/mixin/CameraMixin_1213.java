package ess.ll.client.mixin;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ess.ll.client.integration.ModelPartTransformAccess;

@Mixin(Camera.class)
public abstract class CameraMixin_1213 implements ModelPartTransformAccess {
   @Shadow
   @Final
   private static Vector3fc FORWARDS;
   @Shadow
   @Final
   private static Vector3fc UP;
   @Shadow
   @Final
   private static Vector3fc LEFT;
   @Shadow
   @Final
   private Quaternionf rotation;
   @Shadow
   @Final
   private Vector3f forwards;
   @Shadow
   @Final
   private Vector3f up;
   @Shadow
   @Final
   private Vector3f left;
   @Shadow
   private int matrixPropertiesDirty;
   @Unique
   private float pitchM;
   @Unique
   private float yawM;
   @Unique
   private float rollM;
   @Unique
   private float offsetX;
   @Unique
   private float offsetY;
   @Unique
   private float offsetZ;
   private static final float DEG_TO_RAD = 0;
   private static final float YAW_BASE = 0;

   @Shadow
   protected abstract void setPosition(Vec3 vec);

   @Override
   public void hMI5_0$applyRotation() {
      this.rotation.add(Axis.XP.rotationDegrees(this.pitchM));
      this.rotation.add(Axis.YP.rotationDegrees(this.yawM));
      this.rotation.add(Axis.ZP.rotationDegrees(this.rollM));
      this.matrixPropertiesDirty |= 3;
   }

   @Override
   public void hMI5_0$setRotationValues(float pitch, float yaw, float roll) {
      this.pitchM = pitch;
      this.yawM = yaw;
      this.rollM = roll;
   }

   @Override
   public void hMI5_0$setPosValues(float x, float y, float z) {
      this.offsetX = x;
      this.offsetY = y;
      this.offsetZ = z;
   }

   @Inject(method = "setRotation(FF)V", at = @At("TAIL"))
   private void hmiSetRotation(float yaw, float pitch, CallbackInfo ci) {
      if (this.pitchM != 0.0F || this.yawM != 0.0F || this.rollM != 0.0F) {
         float scale = DEG_TO_RAD;
         this.rotation.rotationYXZ(YAW_BASE - (yaw + this.yawM) * scale, (-pitch + this.pitchM) * scale, this.rollM * scale);
         FORWARDS.rotate(this.rotation, this.forwards);
         UP.rotate(this.rotation, this.up);
         LEFT.rotate(this.rotation, this.left);
         this.matrixPropertiesDirty |= 3;
      }
   }

   @Inject(method = "setPosition(DDD)V", at = @At("HEAD"), cancellable = true)
   private void hmiSetPosition(double x, double y, double z, CallbackInfo ci) {
      if (this.offsetX != 0.0F || this.offsetY != 0.0F || this.offsetZ != 0.0F) {
         this.setPosition(new Vec3(x + this.offsetX, y + this.offsetY, z + this.offsetZ));
         ci.cancel();
      }
   }
}
