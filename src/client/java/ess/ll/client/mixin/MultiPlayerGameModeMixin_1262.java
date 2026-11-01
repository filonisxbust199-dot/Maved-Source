package ess.ll.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeMixin_1262 {
   @Invoker("startPrediction")
   void sendSequencedPacketAccessor(ClientLevel level, PredictiveAction predictiveAction);

   @Invoker("ensureHasSentCarriedItem")
   void hookEnsureHasSentCarriedItem();

   @Accessor("destroyProgress")
   float getDestroyProgress();

   @Accessor("destroyProgress")
   void setDestroyProgress(float progress);

   @Accessor("destroyBlockPos")
   BlockPos getDestroyBlockPos();

   @Accessor("destroyBlockPos")
   void setDestroyBlockPos(BlockPos pos);

   @Accessor("destroyingItem")
   void setDestroyingItem(ItemStack stack);

   @Accessor("destroyingItem")
   ItemStack getDestroyingItem();

   @Accessor("destroyDelay")
   int getDestroyDelay();

   @Accessor("isDestroying")
   boolean getIsDestroying();
}
