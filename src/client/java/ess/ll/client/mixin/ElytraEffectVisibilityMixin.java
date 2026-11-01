package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ess.ll.client.event.MobEffectVisibilityEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ElytraEffectVisibilityMixin {
    @ModifyReturnValue(method="hasEffect",at=@At("RETURN"))
    private boolean ess$levitationFlight(boolean original,Holder<MobEffect> effect) {
        return MobEffectVisibilityEvent.INSTANCE.fire((LivingEntity)(Object)this,effect.value(),original);
    }
}
