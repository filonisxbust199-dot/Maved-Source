package ess.ll.client.mixin;
import ess.ll.client.event.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.component.SwingAnimation;
@Mixin(LivingEntity.class)
public abstract class CombatLivingMixin {
 @Inject(method="setSprinting",at=@At("HEAD"),cancellable=true)
 private void ess$sprint(boolean value,CallbackInfo ci){SprintStateEvent.INSTANCE.fire((LivingEntity)(Object)this,value,ci);}
 @Inject(method="die",at=@At("HEAD"))
 private void ess$death(DamageSource source,CallbackInfo ci){EntityDeathEvent.INSTANCE.fire((LivingEntity)(Object)this);}
 @Inject(method="getModifiedSwingDuration",at=@At("HEAD"),cancellable=true)
 private void ess$swingDuration(SwingAnimation animation,CallbackInfoReturnable<Integer> cir){SwingDurationEvent.INSTANCE.fire((LivingEntity)(Object)this,cir);}
}
