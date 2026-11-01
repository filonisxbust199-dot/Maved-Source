package ess.ll.client.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class FireworkBoostEvent extends Event {
    public static final FireworkBoostEvent INSTANCE = new FireworkBoostEvent();
    private Entity entity;
    private float horizontalSpeed;
    private float verticalSpeed;
    private Vec3 direction;
    private double acceleration;
    private static final float DEFAULT_HORIZONTAL_SPEED = 1.5F;
    private static final float DEFAULT_VERTICAL_SPEED = 1.5F;
    private static final double DEFAULT_ACCELERATION = 0.5;
    private static final float VANILLA_HORIZONTAL_SPEED = 1.5F;
    private static final float VANILLA_VERTICAL_SPEED = 1.5F;
    private static final double VANILLA_ACCELERATION = 0.5;
    private static final double BASE_BOOST_X = 0.1;
    private static final double BASE_BOOST_Y = 0.1;
    private static final double BASE_BOOST_Z = 0.1;

    public float getHorizontalSpeed() {
        return this.horizontalSpeed;
    }

    public FireworkBoostEvent() {
        this.horizontalSpeed = DEFAULT_HORIZONTAL_SPEED;
        this.verticalSpeed = DEFAULT_VERTICAL_SPEED;
        this.direction = Vec3.ZERO;
        this.acceleration = DEFAULT_ACCELERATION;
    }

    public void setAcceleration(double acceleration) {
        this.acceleration = acceleration;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void setDirection(Vec3 vec) {
        this.direction = vec;
    }

    public float getVerticalSpeed() {
        return this.verticalSpeed;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public Vec3 getDirection() {
        return this.direction;
    }

    public void setVerticalSpeed(float verticalSpeed) {
        this.verticalSpeed = verticalSpeed;
    }

    public double getAcceleration() {
        return this.acceleration;
    }

    public void setHorizontalSpeed(float horizontalSpeed) {
        this.horizontalSpeed = horizontalSpeed;
    }

    public void fire(Entity entity) {
        this.entity = entity;
        this.horizontalSpeed = VANILLA_HORIZONTAL_SPEED;
        this.verticalSpeed = VANILLA_VERTICAL_SPEED;
        this.acceleration = VANILLA_ACCELERATION;
        Vec3 deltaMovement = entity.getDeltaMovement();
        this.direction = entity.getLookAngle();
        super.dispatch();
        Vec3 vec = this.direction;
        entity.setDeltaMovement(deltaMovement.add(vec.x * BASE_BOOST_X + (vec.x * this.horizontalSpeed - deltaMovement.x) * this.acceleration, vec.y * BASE_BOOST_Y + (vec.y * this.verticalSpeed - deltaMovement.y) * this.acceleration, vec.z * BASE_BOOST_Z + (vec.z * this.horizontalSpeed - deltaMovement.z) * this.acceleration));
    }
}
