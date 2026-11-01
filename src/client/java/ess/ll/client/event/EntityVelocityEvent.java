package ess.ll.client.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityVelocityEvent extends Event {
    public static final EntityVelocityEvent INSTANCE = new EntityVelocityEvent();
    private Entity entity;
    private Vec3 velocity;

    public Entity getEntity() {
        return this.entity;
    }

    public void setVelocity(Vec3 vec) {
        this.velocity = vec;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Vec3 getVelocity() {
        return this.velocity;
    }

    public Vec3 fire(Entity entity, Vec3 vec) {
        if (!this.hasListeners()) {
            return vec;
        }
        this.entity = entity;
        this.velocity = vec;
        super.dispatch();
        return this.consumeCancelled() ? this.velocity : vec;
    }
}
