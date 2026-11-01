package ess.ll.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

public class ViewModelOffsetEvent extends Event {
   public static final ViewModelOffsetEvent INSTANCE = new ViewModelOffsetEvent();
   private Vec3 rightHandOffset;
   private Vec3 leftHandOffset;

   public Vec3 getRightHandOffset() {
      return this.rightHandOffset;
   }

   public Vec3 getLeftHandOffset() {
      return this.leftHandOffset;
   }

   public void setRightHandOffset(Vec3 vec) {
      this.rightHandOffset = vec;
   }

   public void setLeftHandOffset(Vec3 vec) {
      this.leftHandOffset = vec;
   }

   public void fire(AbstractClientPlayer player, InteractionHand interactionHand, PoseStack poseStack) {
      if (this.hasListeners()) {
         this.setCancelled(false);
         super.dispatch();
         if (this.consumeCancelled()) {
            boolean rightSide = interactionHand == InteractionHand.MAIN_HAND && player.getMainArm() == HumanoidArm.RIGHT
               || interactionHand == InteractionHand.OFF_HAND && player.getMainArm() == HumanoidArm.LEFT;
            Vec3 vec = rightSide ? this.rightHandOffset : this.leftHandOffset;
            if (vec != null) {
               poseStack.translate(vec);
            }
         }
      }
   }
}
