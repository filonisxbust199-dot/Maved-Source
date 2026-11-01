package ess.ll.client.event;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class AttackEntityEvent extends Event {
    public static final AttackEntityEvent INSTANCE = new AttackEntityEvent();
    private Entity entity;
    private AttackEntityEvent.Phase phase;

    public void firePost(Entity entity) {
        this.entity = entity;
        this.phase = AttackEntityEvent.Phase.POST;
        super.dispatch();
    }

    public AttackEntityEvent.Phase getPhase() {
        return this.phase;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void firePre(Entity entity, CallbackInfo ci) {
        this.entity = entity;
        this.phase = AttackEntityEvent.Phase.PRE;
        super.dispatch();
        if (this.consumeCancelled()) {
            ci.cancel();
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    public void setPhase(AttackEntityEvent.Phase phase) {
        this.phase = phase;
    }

    public enum Phase {
        PRE, POST
    }
}
