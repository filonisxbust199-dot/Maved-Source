package ess.ll.client.mixin;

import ess.ll.client.module.player.NoPush;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NoPush: other entities do not push you, and ladders and vines do not catch you. */
@Mixin(LivingEntity.class)
public abstract class NoPushMixin {
    @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
    private void ess$pushable(CallbackInfoReturnable<Boolean> cir) {
        if(NoPush.blocks((LivingEntity)(Object)this,"Игроки")) cir.setReturnValue(false);
    }
    @Inject(method="onClimbable",at=@At("HEAD"),cancellable=true)
    private void ess$climbable(CallbackInfoReturnable<Boolean> cir) {
        if(NoPush.blocks((LivingEntity)(Object)this,"Лианы")) cir.setReturnValue(false);
    }
}
