package ess.ll.client.event;

import net.minecraft.world.entity.Entity;

public class EntityDeathEvent extends Event {
    public static final EntityDeathEvent INSTANCE = new EntityDeathEvent();
    private Entity entity;

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void fire(Entity entity) {
        this.entity = entity;
        super.dispatch();
    }

    public Entity getEntity() {
        return this.entity;
    }
}
