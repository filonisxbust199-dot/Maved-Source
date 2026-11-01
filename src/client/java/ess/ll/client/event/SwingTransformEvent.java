package ess.ll.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;

public class SwingTransformEvent extends Event {
   public static final SwingTransformEvent INSTANCE = new SwingTransformEvent();
   private SwingTransformEvent.Kind kind;
   private PoseStack poseStack;
   private HumanoidArm arm;
   private float swingProgress;
   private float x;
   private float y;
   private float z;
   private int side;
   private float equipProgress;

   public float getY() {
      return this.y;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public void setY(float y) {
      this.y = y;
   }

   public void fireSwingOffset(PoseStack poseStack, HumanoidArm humanoidArm, float swingProgress, Runnable task) {
      this.setCancelled(false);
      this.kind = SwingTransformEvent.Kind.SWING_OFFSET;
      this.poseStack = poseStack;
      this.arm = humanoidArm;
      this.swingProgress = swingProgress;
      super.dispatch();
      if (!this.consumeCancelled()) {
         task.run();
      }
   }

   public void fireEquipOffset(PoseStack poseStack, float x, float y, float z, int side, float equipProgress) {
      this.setCancelled(false);
      this.kind = SwingTransformEvent.Kind.EQUIP_OFFSET;
      this.poseStack = poseStack;
      this.x = x;
      this.y = y;
      this.z = z;
      this.side = side;
      this.equipProgress = equipProgress;
      super.dispatch();
      if (!this.consumeCancelled()) {
         poseStack.translate(x, y, z);
      }
   }

   public void fireTranslate(PoseStack poseStack, float x, float y, float z) {
      this.setCancelled(false);
      this.kind = SwingTransformEvent.Kind.TRANSLATE;
      this.poseStack = poseStack;
      this.x = x;
      this.y = y;
      this.z = z;
      super.dispatch();
      if (!this.consumeCancelled()) {
         poseStack.translate(x, y, z);
      }
   }

   public void setPoseStack(PoseStack poseStack) {
      this.poseStack = poseStack;
   }

   public SwingTransformEvent.Kind getKind() {
      return this.kind;
   }

   public void setSwingProgress(float swingProgress) {
      this.swingProgress = swingProgress;
   }

   public float getX() {
      return this.x;
   }

   public int getSide() {
      return this.side;
   }

   public void setKind(SwingTransformEvent.Kind kind) {
      this.kind = kind;
   }

   public HumanoidArm getArm() {
      return this.arm;
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setSide(int side) {
      this.side = side;
   }

   public void setZ(float z) {
      this.z = z;
   }

   public float getZ() {
      return this.z;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public void setEquipProgress(float equipProgress) {
      this.equipProgress = equipProgress;
   }

   public void setArm(HumanoidArm humanoidArm) {
      this.arm = humanoidArm;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }

   public enum Kind {
      TRANSLATE,
      SWING_OFFSET,
      EQUIP_OFFSET;
   }
}
