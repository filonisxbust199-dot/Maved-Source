package ess.ll.client.event;

import net.minecraft.world.phys.Vec3;

public class CameraPositionEvent extends Event {
    public static final CameraPositionEvent INSTANCE = new CameraPositionEvent();
    private Vec3 position;

    public void setPosition(Vec3 vec) {
        this.position = vec;
    }

    public Vec3 fire(Vec3 vec) {
        this.position = vec;
        super.dispatch();
        return this.position;
    }

    public Vec3 getPosition() {
        return this.position;
    }
}
