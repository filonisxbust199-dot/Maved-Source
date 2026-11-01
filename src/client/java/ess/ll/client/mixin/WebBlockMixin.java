package ess.ll.client.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ess.ll.client.event.CobwebCollisionEvent;

@Mixin(WebBlock.class)
public class WebBlockMixin {
   @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
   public void onEntityCollision(BlockState blockState, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier insideBlockEffectApplier, boolean intersects, CallbackInfo ci) {
      CobwebCollisionEvent.INSTANCE.fire(pos, entity, ci);
   }
}
