package ess.ll.client.event;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityMoveEvent extends Event {
    public static final EntityMoveEvent INSTANCE = new EntityMoveEvent();
    private Vec3 movement;

    public Vec3 fire(Entity entity, Vec3 vec) {
        if (!(entity instanceof LocalPlayer)) {
            return vec;
        }
        this.movement = vec;
        super.dispatch();
        return this.movement;
    }

    public Vec3 getMovement() {
        return this.movement;
    }

    public void setMovement(Vec3 vec) {
        this.movement = vec;
    }
}
