package ess.ll.client.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ess.ll.client.integration.ItemStackAccess;

@Mixin(ItemStack.class)
public class ItemStackMixin_1250 implements ItemStackAccess {
   @Unique
   private int transformPlusOne;
   @Unique
   private int swingSpeedDelta;
   @Unique
   private int shouldRenderAsBlockPlusOne;

   @Override
   public void hMI5_0$setTransform(boolean transform) {
      this.hMI5_0$setTransform(transform ? 1 : 0);
   }

   @Override
   public void hMI5_0$setTransform(int transform) {
      this.transformPlusOne = transform + 1;
   }

   @Override
   public int hMI5_0$getTransform() {
      return this.transformPlusOne - 1;
   }

   @Override
   public void hMI5_0$setSwingSpeed(int speed) {
      this.swingSpeedDelta = speed - 10;
   }

   @Override
   public int hMI5_0$getSwingSpeed() {
      return 10 + this.swingSpeedDelta;
   }

   @Override
   public void hMI5_0$setRenderAsBlock(boolean renderAsBlock) {
      this.shouldRenderAsBlockPlusOne = (renderAsBlock ? 1 : 0) + 1;
   }

   @Override
   public int hMI5_0$getRenderAsBlock() {
      return 0;
   }
}
