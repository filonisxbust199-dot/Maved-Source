package ess.ll.client.event;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class SwingDurationEvent extends Event {
    public static final SwingDurationEvent INSTANCE = new SwingDurationEvent();
    private LivingEntity entity;
    private CallbackInfoReturnable<Integer> callback;

    public void fire(LivingEntity livingEntity, CallbackInfoReturnable<Integer> cir) {
        this.setCancelled(false);
        this.entity = livingEntity;
        this.callback = cir;
        super.dispatch();
    }

    public void setCallback(CallbackInfoReturnable<Integer> cir) {
        this.callback = cir;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public void setEntity(LivingEntity livingEntity) {
        this.entity = livingEntity;
    }

    public CallbackInfoReturnable<Integer> getCallback() {
        return this.callback;
    }
}
