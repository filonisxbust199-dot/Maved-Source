package ess.ll.client.render;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.CollisionUtils;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class ClientParticle implements MinecraftAccess {
    private static final AABB EMPTY_BOUNDS = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final double MAX_COLLISION_DISTANCE_SQUARED = Mth.square(100.0);
    private double previousX;
    private double previousY;
    private double previousZ;
    private double x;
    private double y;
    private double z;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private AABB bounds = EMPTY_BOUNDS;
    private boolean onGround;
    private boolean collisionEnabled = true;
    private boolean stopped;
    private boolean removed;
    private float width;
    private float height;
    private int age;
    private int lifetime;
    private int motionDelay;
    private float gravity;
    private float friction;
    private float initialRestitution;
    private float restitution;
    private double peakY;
    private boolean sliding;
    private boolean accelerateWhenBlocked;
    private float rotation;
    private float previousRotation;
    private float angularVelocity;
    private float startScale;
    private float peakScale;
    private float endScale;
    private float scalePeakTime;
    private ParticleCollisionMode collisionMode;
    private ParticleInterpolationMode interpolationMode;
    private double maxRenderDistanceSquared;
    private static final double PI = Math.PI;

    public void setStopped(boolean stopped2) {
        this.stopped = stopped2;
    }

    public float getEndScale() {
        return this.endScale;
    }

    public float getGravity() {
        return this.gravity;
    }

    private void tickSliding() {
        this.velocityY = -0.04 * this.gravity;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        if (this.onGround) {
            this.velocityY = 0.0;
        }
        this.velocityX = this.velocityX * 0.986F;
        this.velocityZ = this.velocityZ * 0.986F;
        double var1 = this.velocityX * this.velocityX + this.velocityZ * this.velocityZ;
        if (var1 < 4.0E-5) {
            this.stopped = true;
            this.stopMotion();
        }
    }

    public float getInitialRestitution() {
        return this.initialRestitution;
    }

    public void setPreviousZ(double previousZ2) {
        this.previousZ = previousZ2;
    }

    public void setSliding(boolean sliding2) {
        this.sliding = sliding2;
    }

    public void setMotionDelay(int motionDelay2) {
        this.motionDelay = motionDelay2;
    }

    private void tickSweptCollision() {
        if (this.sliding) {
            this.tickSliding();
        } else {
            this.velocityY = this.velocityY - 0.04 * this.gravity;
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            if (!this.onGround && this.y > this.peakY) {
                this.peakY = this.y;
            }
            if (this.accelerateWhenBlocked && this.y == this.previousY) {
                this.velocityX = this.velocityX * 1.1;
                this.velocityZ = this.velocityZ * 1.1;
            }
            this.velocityX = this.velocityX * this.friction;
            this.velocityY = this.velocityY * this.friction;
            this.velocityZ = this.velocityZ * this.friction;
            if (this.onGround) {
                this.velocityX = this.velocityX * 0.7;
                this.velocityZ = this.velocityZ * 0.7;
            }
        }
    }

    public double getPreviousZ() {
        return this.previousZ;
    }

    public boolean isSliding() {
        return this.sliding;
    }

    private static float smoothstep(float var0) {
        float var1 = Math.clamp(var0, 0.0F, 1.0F);
        return var1 * var1 * (3.0F - 2.0F * var1);
    }

    public void setPeakScale(float peakScale2) {
        this.peakScale = peakScale2;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public void setVelocityX(double velocityX2) {
        this.velocityX = velocityX2;
    }

    public double interpolateZ(float var1) {
        return this.previousZ + (this.z - this.previousZ) * var1;
    }

    public float interpolateRotation(float var1) {
        return this.previousRotation + (this.rotation - this.previousRotation) * var1;
    }

    public void remove() {
        this.removed = true;
    }

    public void setStartScale(float startScale2) {
        this.startScale = startScale2;
    }

    public void setPreviousRotation(float previousRotation2) {
        this.previousRotation = previousRotation2;
    }

    public void setVelocity(double velocityX2, double velocityY2, double velocityZ2) {
        this.velocityX = velocityX2;
        this.velocityY = velocityY2;
        this.velocityZ = velocityZ2;
    }

    public ParticleCollisionMode getCollisionMode() {
        return this.collisionMode;
    }

    private void stopMotion() {
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
    }

    public double getVelocityZ() {
        return this.velocityZ;
    }

    public ClientParticle(ClientParticle.SpawnOptions spawnOptions) {
        this.width = 0.6F;
        this.height = 1.8F;
        this.friction = 0.98F;
        this.startScale = 1.0F;
        this.peakScale = 1.0F;
        this.endScale = 1.0F;
        this.scalePeakTime = 0.2F;
        this.collisionMode = ParticleCollisionMode.SWEPT;
        this.interpolationMode = ParticleInterpolationMode.SMOOTH;
        this.reset(spawnOptions);
    }

    public void setPosition(double x2, double y2, double z2) {
        this.x = x2;
        this.y = y2;
        this.z = z2;
        float var7 = this.width / 2.0F;
        float height2 = this.height;
        this.setBounds(new AABB(x2 - var7, y2, z2 - var7, x2 + var7, y2 + height2, z2 + var7));
    }

    public void move(double var1, double var3, double var5) {
        if (!this.stopped) {
            double var7 = var1;
            double var9 = var3;
            double var11 = var5;
            ClientLevel level2 = mc.level;
            if (this.collisionEnabled && level2 != null && (var1 != 0.0 || var3 != 0.0 || var5 != 0.0)) {
                double var14 = var1 * var1 + var3 * var3 + var5 * var5;
                if (var14 < MAX_COLLISION_DISTANCE_SQUARED) {
                    Vec3 vector = new Vec3(var1, var3, var5);
                    AABB bounds = this.getBounds();
                    if (!CollisionUtils.noCollision(level2, bounds, vector)) {
                        Vec3 vector2 = Entity.collideBoundingBox((Entity) null, vector, bounds, level2, Collections.emptyList());
                        var1 = vector2.x;
                        var3 = vector2.y;
                        var5 = vector2.z;
                    }
                }
            }
            if (var1 != 0.0 || var3 != 0.0 || var5 != 0.0) {
                this.setBounds(this.getBounds().move(var1, var3, var5));
                this.updatePositionFromBounds();
            }
            boolean var24 = var9 != var3;
            boolean var15 = var7 != var1 || var11 != var5;
            boolean var25 = var24 && var9 > 0.0;
            this.onGround = var24 && var9 < 0.0;
            if (var7 != var1) {
                if (this.restitution > 0.0F) {
                    this.velocityX = -var7 * this.restitution;
                    this.restitution = this.restitution * 0.9F;
                    ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
                    double var30 = Math.abs(var7) * 0.1F;
                    this.velocityZ = this.velocityZ + (threadLocalRandom.nextDouble() * 2.0 - 1.0) * var30;
                    this.velocityY = this.velocityY + threadLocalRandom.nextDouble() * var30 * 0.5;
                } else {
                    this.velocityX = 0.0;
                }
            }
            if (var11 != var5) {
                if (this.restitution > 0.0F) {
                    this.velocityZ = -var11 * this.restitution;
                    this.restitution = this.restitution * 0.9F;
                    ThreadLocalRandom threadLocalRandom2 = ThreadLocalRandom.current();
                    double var31 = Math.abs(var11) * 0.1F;
                    this.velocityX = this.velocityX + (threadLocalRandom2.nextDouble() * 2.0 - 1.0) * var31;
                    this.velocityY = this.velocityY + threadLocalRandom2.nextDouble() * var31 * 0.5;
                } else {
                    this.velocityZ = 0.0;
                }
            }
            if (var24) {
                if (this.restitution <= 0.0F) {
                    this.velocityY = 0.0;
                } else if (this.onGround) {
                    double var28 = Math.max(0.0, this.peakY - this.y);
                    double var19 = Math.sqrt(Math.max(var28, 0.01)) * 0.2 * this.restitution;
                    this.restitution = this.restitution * 0.9F;
                    this.peakY = this.y;
                    if (var19 > 0.055) {
                        this.velocityY = var19;
                        ThreadLocalRandom threadLocalRandom3 = ThreadLocalRandom.current();
                        double var22 = var19 * 0.1F;
                        this.velocityX = this.velocityX + (threadLocalRandom3.nextDouble() * 2.0 - 1.0) * var22;
                        this.velocityZ = this.velocityZ + (threadLocalRandom3.nextDouble() * 2.0 - 1.0) * var22;
                    } else {
                        this.velocityY = 0.0;
                        if (var19 > 0.01) {
                            ThreadLocalRandom threadLocalRandom4 = ThreadLocalRandom.current();
                            double var33 = threadLocalRandom4.nextDouble() * PI * 2.0;
                            this.velocityX = this.velocityX + Math.cos(var33) * var19 * 0.5;
                            this.velocityZ = this.velocityZ + Math.sin(var33) * var19 * 0.5;
                        }
                        this.sliding = true;
                    }
                } else if (var25) {
                    this.velocityY = -Math.abs(var9) * this.restitution;
                    this.restitution = this.restitution * 0.9F;
                }
            }
            if (!this.sliding) {
                double var29 = this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ;
                if (this.onGround && !var15 && Math.abs(var9) >= 1.0E-5 && Math.abs(var3) < 1.0E-5 && var29 <= 2.25E-4) {
                    this.stopped = true;
                    this.velocityX = 0.0;
                    this.velocityY = 0.0;
                    this.velocityZ = 0.0;
                } else if (var29 > 2.25E-4) {
                    this.stopped = false;
                }
            }
        }
    }

    public float getRotation() {
        return this.rotation;
    }

    public double getPreviousX() {
        return this.previousX;
    }

    public float getWidth() {
        return this.width;
    }

    public boolean isAccelerateWhenBlocked() {
        return this.accelerateWhenBlocked;
    }

    public void setInterpolationMode(ParticleInterpolationMode interpolationMode2) {
        this.interpolationMode = interpolationMode2;
    }

    private void tickRicochet() {
        this.reflectVelocityAtBlock();
        this.velocityX = this.velocityX * 0.999F;
        this.velocityY = this.velocityY * 0.999F;
        this.velocityZ = this.velocityZ * 0.999F;
        this.setPosition(this.x + this.velocityX, this.y + this.velocityY, this.z + this.velocityZ);
        this.velocityX = this.velocityX * 0.9998F;
        this.velocityY = this.velocityY - (double) 0.015F * this.gravity;
        this.velocityZ = this.velocityZ * 0.9998F;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public float getStartScale() {
        return this.startScale;
    }

    public void setLifetime(int lifetime2) {
        this.lifetime = lifetime2;
    }

    public float getHeight() {
        return this.height;
    }

    public ClientParticle(double var1, double var3, double var5, double var7, double var9, double var11) {
        this(new ClientParticle.SpawnOptions(var1, var3, var5).withVelocity(var7, var9, var11));
    }

    public float getRestitution() {
        return this.restitution;
    }

    private static int randomLifetime() {
        return (int) (4.0F / (ThreadLocalRandom.current().nextFloat() * 0.9F + 0.1F));
    }

    public void reset(ClientParticle.SpawnOptions spawnOptions) {
        Objects.requireNonNull(spawnOptions, "args");
        this.removed = false;
        this.age = 0;
        this.stopped = false;
        this.onGround = false;
        this.collisionEnabled = spawnOptions.collisionEnabled;
        this.gravity = spawnOptions.gravity;
        this.friction = spawnOptions.friction;
        this.initialRestitution = Mth.clamp(spawnOptions.restitution, 0.0F, 1.95F);
        this.restitution = this.initialRestitution;
        this.peakY = spawnOptions.y;
        this.sliding = false;
        this.accelerateWhenBlocked = spawnOptions.accelerateWhenBlocked;
        this.rotation = 0.0F;
        this.previousRotation = 0.0F;
        this.angularVelocity = spawnOptions.angularVelocity;
        this.startScale = spawnOptions.startScale;
        this.peakScale = spawnOptions.peakScale;
        this.endScale = spawnOptions.endScale;
        this.scalePeakTime = spawnOptions.scalePeakTime;
        this.motionDelay = Math.max(0, spawnOptions.motionDelay);
        this.collisionMode = spawnOptions.collisionMode;
        this.interpolationMode = spawnOptions.interpolationMode;
        this.maxRenderDistanceSquared = spawnOptions.maxRenderDistance <= 0.0 ? 0.0 : spawnOptions.maxRenderDistance * spawnOptions.maxRenderDistance;
        this.width = 0.6F;
        this.height = 1.8F;
        this.bounds = EMPTY_BOUNDS;
        this.setSize(spawnOptions.width, spawnOptions.height);
        this.setPosition(spawnOptions.x, spawnOptions.y, spawnOptions.z);
        this.previousX = this.x;
        this.previousY = this.y;
        this.previousZ = this.z;
        if (spawnOptions.randomizeVelocity) {
            this.setRandomizedVelocity(spawnOptions.velocityX, spawnOptions.velocityY, spawnOptions.velocityZ);
        } else {
            this.setVelocity(spawnOptions.velocityX, spawnOptions.velocityY, spawnOptions.velocityZ);
        }
        if (spawnOptions.lifetime <= 0) {
            this.lifetime = randomLifetime();
        } else {
            this.lifetime = spawnOptions.lifetime;
        }
    }

    public void setVelocityZ(double velocityZ2) {
        this.velocityZ = velocityZ2;
    }

    public void setHeight(float height2) {
        this.height = height2;
    }

    public double getPeakY() {
        return this.peakY;
    }

    public int getMotionDelay() {
        return this.motionDelay;
    }

    public void setAccelerateWhenBlocked(boolean accelerateWhenBlocked2) {
        this.accelerateWhenBlocked = accelerateWhenBlocked2;
    }

    public void setY(double y2) {
        this.y = y2;
    }

    public void setMaxRenderDistanceSquared(double maxRenderDistanceSquared2) {
        this.maxRenderDistanceSquared = maxRenderDistanceSquared2;
    }

    public void setInitialRestitution(float initialRestitution2) {
        this.initialRestitution = initialRestitution2;
    }

    public void setRestitution(float restitution2) {
        this.restitution = restitution2;
    }

    public void setPreviousX(double previousX2) {
        this.previousX = previousX2;
    }

    public void setPeakY(double peakY2) {
        this.peakY = peakY2;
    }

    public void setCollisionEnabled(boolean collisionEnabled2) {
        this.collisionEnabled = collisionEnabled2;
    }

    public void scaleVelocity(float velocityZ2) {
        this.velocityX *= velocityZ2;
        this.velocityY = (this.velocityY - 0.1) * velocityZ2 + 0.1;
        this.velocityZ *= velocityZ2;
    }

    public void setAge(int age2) {
        this.age = age2;
    }

    public boolean isAlive() {
        return !this.removed;
    }

    public void tickSafely() {
        try {
            this.tick();
        } catch (Throwable throwable) {
            this.remove();
        }
    }

    public double getVelocityY() {
        return this.velocityY;
    }

    protected void setSize(float width2, float height2) {
        if (width2 != this.width || height2 != this.height) {
            this.width = width2;
            this.height = height2;
            AABB bounds = this.getBounds();
            double var4 = (bounds.minX + bounds.maxX - width2) / 2.0;
            double var6 = (bounds.minZ + bounds.maxZ - width2) / 2.0;
            this.setBounds(new AABB(var4, bounds.minY, var6, var4 + this.width, bounds.minY + this.height, var6 + this.width));
        }
    }

    public float getScalePeakTime() {
        return this.scalePeakTime;
    }

    public void setEndScale(float endScale2) {
        this.endScale = endScale2;
    }

    protected void updatePositionFromBounds() {
        AABB bounds = this.getBounds();
        this.x = (bounds.minX + bounds.maxX) / 2.0;
        this.y = bounds.minY;
        this.z = (bounds.minZ + bounds.maxZ) / 2.0;
    }

    public float getAngularVelocity() {
        return this.angularVelocity;
    }

    public void setGravity(float gravity2) {
        this.gravity = gravity2;
    }

    public void setAngularVelocity(float angularVelocity2) {
        this.angularVelocity = angularVelocity2;
    }

    public void setCollisionMode(ParticleCollisionMode collisionMode2) {
        this.collisionMode = collisionMode2;
    }

    public void setZ(double z2) {
        this.z = z2;
    }

    public ClientParticle(double var1, double var3, double var5) {
        this(new ClientParticle.SpawnOptions(var1, var3, var5));
    }

    public void setScalePeakTime(float scalePeakTime2) {
        this.scalePeakTime = scalePeakTime2;
    }

    public int getAge() {
        return this.age;
    }

    public float getPeakScale() {
        return this.peakScale;
    }

    public double getZ() {
        return this.z;
    }

    public double interpolateX(float var1) {
        return this.previousX + (this.x - this.previousX) * var1;
    }

    public void setRandomizedVelocity(double var1, double var3, double var5) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        this.velocityX = var1 + (threadLocalRandom.nextDouble() * 2.0 - 1.0) * 0.4;
        this.velocityY = var3 + (threadLocalRandom.nextDouble() * 2.0 - 1.0) * 0.4;
        this.velocityZ = var5 + (threadLocalRandom.nextDouble() * 2.0 - 1.0) * 0.4;
        double var8 = (threadLocalRandom.nextDouble() + threadLocalRandom.nextDouble() + 1.0) * 0.15;
        double var10 = Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ);
        if (!(var10 < 1.0E-8)) {
            this.velocityX = this.velocityX / var10 * var8 * 0.4;
            this.velocityY = this.velocityY / var10 * var8 * 0.4 + 0.1;
            this.velocityZ = this.velocityZ / var10 * var8 * 0.4;
        }
    }

    public double interpolateY(float var1) {
        return this.previousY + (this.y - this.previousY) * var1;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public double getY() {
        return this.y;
    }

    public void setX(double x2) {
        this.x = x2;
    }

    public void setPreviousY(double previousY2) {
        this.previousY = previousY2;
    }

    public void setFriction(float friction2) {
        this.friction = friction2;
    }

    public void setRemoved(boolean removed2) {
        this.removed = removed2;
    }

    public boolean isCollisionEnabled() {
        return this.collisionEnabled;
    }

    public float interpolateScale(float var1) {
        int var2 = Math.max(1, this.lifetime);
        float var3 = Math.max(0.0F, this.age - 1 + var1);
        float var4 = Math.min(1.0F, var3 / var2);
        if (var4 < this.scalePeakTime) {
            float var6 = var4 / Math.max(1.0E-4F, this.scalePeakTime);
            return this.startScale + (this.peakScale - this.startScale) * smoothstep(var6);
        } else {
            float var5 = (var4 - this.scalePeakTime) / Math.max(1.0E-4F, 1.0F - this.scalePeakTime);
            return this.peakScale + (this.endScale - this.peakScale) * smoothstep(var5);
        }
    }

    public void setBounds(AABB bounds2) {
        this.bounds = bounds2;
    }

    public ParticleInterpolationMode getInterpolationMode() {
        return this.interpolationMode;
    }

    public void setWidth(float width2) {
        this.width = width2;
    }

    public double getPreviousY() {
        return this.previousY;
    }

    public float getPreviousRotation() {
        return this.previousRotation;
    }

    public void tick() {
        if (!this.removed) {
            this.previousX = this.x;
            this.previousY = this.y;
            this.previousZ = this.z;
            this.previousRotation = this.rotation;
            this.rotation = this.rotation + this.angularVelocity;
            int var1 = this.age++;
            if (var1 >= this.lifetime) {
                this.remove();
            } else if (var1 >= this.motionDelay) {
                if (this.stopped) {
                    this.stopMotion();
                } else {
                    switch (this.collisionMode) {
                        case SWEPT:
                            this.tickSweptCollision();
                            break;
                        case RICOCHET:
                            this.tickRicochet();
                    }
                }
            }
        }
    }

    public void setVelocityY(double velocityY2) {
        this.velocityY = velocityY2;
    }

    public double getVelocityX() {
        return this.velocityX;
    }

    public void setOnGround(boolean onGround2) {
        this.onGround = onGround2;
    }

    public float getFriction() {
        return this.friction;
    }

    public boolean isRemoved() {
        return this.removed;
    }

    public double getMaxRenderDistanceSquared() {
        return this.maxRenderDistanceSquared;
    }

    private void reflectVelocityAtBlock() {
        if (this.collisionEnabled) {
            ClientLevel level2 = mc.level;
            if (level2 != null && mc.player != null) {
                if (this.velocityX != 0.0 || this.velocityY != 0.0 || this.velocityZ != 0.0) {
                    Vec3 vector = new Vec3(this.x, this.y, this.z);
                    Vec3 vector2 = vector.add(this.velocityX, this.velocityY, this.velocityZ);
                    BlockHitResult blockHitResult = level2.clip(new ClipContext(vector, vector2, Block.COLLIDER, Fluid.NONE, mc.player));
                    if (blockHitResult.getType() == Type.BLOCK) {
                        Vec3i unitVec3i = blockHitResult.getDirection().getUnitVec3i();
                        double var6 = 2.0 * (this.velocityX * unitVec3i.getX() + this.velocityY * unitVec3i.getY() + this.velocityZ * unitVec3i.getZ());
                        this.velocityX = this.velocityX - unitVec3i.getX() * var6;
                        this.velocityY = this.velocityY - unitVec3i.getY() * var6;
                        this.velocityZ = this.velocityZ - unitVec3i.getZ() * var6;
                    }
                }
            }
        }
    }

    public void setRotation(float rotation2) {
        this.rotation = rotation2;
    }

    public AABB getBounds() {
        return this.bounds;
    }

    public double getX() {
        return this.x;
    }

    public static class SpawnOptions {
        private final double x;
        private final double y;
        private final double z;
        private double velocityX;
        private double velocityY;
        private double velocityZ;
        private int lifetime;
        private float gravity;
        private float friction;
        private float restitution;
        private boolean collisionEnabled;
        private boolean accelerateWhenBlocked;
        private float width;
        private float height;
        private int motionDelay;
        private boolean randomizeVelocity;
        private float angularVelocity;
        private float startScale;
        private float peakScale;
        private float endScale;
        private float scalePeakTime;
        private ParticleCollisionMode collisionMode;
        private ParticleInterpolationMode interpolationMode;
        private double maxRenderDistance;

        public ClientParticle.SpawnOptions withScaleCurve(float startScale2, float peakScale2, float endScale2, float scalePeakTime2) {
            this.startScale = startScale2;
            this.peakScale = peakScale2;
            this.endScale = endScale2;
            this.scalePeakTime = scalePeakTime2;
            return this;
        }

        public double getVelocityY() {
            return this.velocityY;
        }

        public float getHeight() {
            return this.height;
        }

        public ClientParticle.SpawnOptions withMaxRenderDistance(double maxRenderDistance2) {
            this.maxRenderDistance = maxRenderDistance2;
            return this;
        }

        public double getX() {
            return this.x;
        }

        public float getWidth() {
            return this.width;
        }

        public ClientParticle.SpawnOptions withRestitution(float var1) {
            this.restitution = Mth.clamp(var1, 0.0F, 1.95F);
            return this;
        }

        public float getScalePeakTime() {
            return this.scalePeakTime;
        }

        public float getAngularVelocity() {
            return this.angularVelocity;
        }

        public float getPeakScale() {
            return this.peakScale;
        }

        public ParticleCollisionMode getCollisionMode() {
            return this.collisionMode;
        }

        public ClientParticle.SpawnOptions withInterpolationMode(ParticleInterpolationMode particleInterpolationMode) {
            this.interpolationMode = particleInterpolationMode == null ? ParticleInterpolationMode.SMOOTH : particleInterpolationMode;
            return this;
        }

        public ClientParticle.SpawnOptions withRandomizeVelocity(boolean randomizeVelocity2) {
            this.randomizeVelocity = randomizeVelocity2;
            return this;
        }

        public double getVelocityX() {
            return this.velocityX;
        }

        public float getEndScale() {
            return this.endScale;
        }

        public ClientParticle.SpawnOptions withCollisionEnabled(boolean collisionEnabled2) {
            this.collisionEnabled = collisionEnabled2;
            return this;
        }

        public ClientParticle.SpawnOptions withFriction(float friction2) {
            this.friction = friction2;
            return this;
        }

        public double getY() {
            return this.y;
        }

        public ClientParticle.SpawnOptions withGravity(float gravity2) {
            this.gravity = gravity2;
            return this;
        }

        public ClientParticle.SpawnOptions withAccelerateWhenBlocked(boolean accelerateWhenBlocked2) {
            this.accelerateWhenBlocked = accelerateWhenBlocked2;
            return this;
        }

        public ParticleInterpolationMode getInterpolationMode() {
            return this.interpolationMode;
        }

        public ClientParticle.SpawnOptions withMotionDelay(int var1) {
            this.motionDelay = Math.max(0, var1);
            return this;
        }

        public int getMotionDelay() {
            return this.motionDelay;
        }

        public ClientParticle.SpawnOptions withVelocity(double velocityX2, double velocityY2, double velocityZ2) {
            this.velocityX = velocityX2;
            this.velocityY = velocityY2;
            this.velocityZ = velocityZ2;
            return this;
        }

        public int getLifetime() {
            return this.lifetime;
        }

        public SpawnOptions(double x2, double y2, double z2) {
            this.friction = 0.98F;
            this.collisionEnabled = true;
            this.width = 0.2F;
            this.height = 0.2F;
            this.startScale = 1.0F;
            this.peakScale = 1.0F;
            this.endScale = 1.0F;
            this.scalePeakTime = 0.2F;
            this.collisionMode = ParticleCollisionMode.SWEPT;
            this.interpolationMode = ParticleInterpolationMode.SMOOTH;
            this.x = x2;
            this.y = y2;
            this.z = z2;
        }

        public boolean isAccelerateWhenBlocked() {
            return this.accelerateWhenBlocked;
        }

        public ClientParticle.SpawnOptions withSize(float width2, float height2) {
            this.width = width2;
            this.height = height2;
            return this;
        }

        public boolean isRandomizeVelocity() {
            return this.randomizeVelocity;
        }

        public float getRestitution() {
            return this.restitution;
        }

        public float getGravity() {
            return this.gravity;
        }

        public double getMaxRenderDistance() {
            return this.maxRenderDistance;
        }

        public float getFriction() {
            return this.friction;
        }

        public double getVelocityZ() {
            return this.velocityZ;
        }

        public ClientParticle.SpawnOptions withLifetime(int lifetime2) {
            this.lifetime = lifetime2;
            return this;
        }

        public ClientParticle.SpawnOptions withAngularVelocity(float angularVelocity2) {
            this.angularVelocity = angularVelocity2;
            return this;
        }

        public ClientParticle.SpawnOptions withCollisionMode(ParticleCollisionMode particleCollisionMode) {
            this.collisionMode = particleCollisionMode == null ? ParticleCollisionMode.SWEPT : particleCollisionMode;
            return this;
        }

        public double getZ() {
            return this.z;
        }

        public float getStartScale() {
            return this.startScale;
        }

        public boolean isCollisionEnabled() {
            return this.collisionEnabled;
        }
    }
}
