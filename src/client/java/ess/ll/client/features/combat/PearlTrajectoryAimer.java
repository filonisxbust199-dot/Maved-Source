package ess.ll.client.features.combat;

import ess.ll.client.features.social.FriendManager;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.module.combat.PearlTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class PearlTrajectoryAimer implements MinecraftAccess {
    private long lastUpdateTime = 0L;
    private ThrownEnderpearl trackedPearl = null;
    private Vec3 landing = null;
    private Entity lastTarget = null;
    private long lastTargetTime = 0L;

    private Vec3 landingOf(float var1, float var2) {
        Vec3 vector = this.simulateLanding(this.throwOrigin(), this.throwVelocity(var1, var2));
        return vector != null && !PearlTarget.isInsideTree(vector) ? vector : null;
    }

    private boolean isBlocked(Vec3 vector, Vec3 vector2) {
        return this.clip(vector, vector2).getType() == Type.BLOCK;
    }

    private Vec3 simulateLanding(Vec3 vector, Vec3 vector2) {
        double minY = mc.level.getMinY();
        for (int var5 = 0; var5 < 160; var5++) {
            vector2 = vector2.subtract(0.0, 0.03, 0.0).scale(0.99);
            Vec3 vector3 = vector.add(vector2);
            BlockHitResult blockHitResult = this.clip(vector, vector3);
            if (blockHitResult.getType() == Type.BLOCK) {
                return blockHitResult.getLocation();
            }
            if (vector3.y <= minY) {
                return new Vec3(vector3.x, minY, vector3.z);
            }
            vector = vector3;
        }
        return null;
    }

    private float[] scanPitch(Vec3 vector, float var2, float var3, float var4, float var5, double var6) {
        float var8 = var3;
        while (var8 >= var4) {
            Vec3 vector2 = this.landingOf(var2, var8);
            if (vector2 != null && vector.distanceTo(vector2) <= var6) {
                return new float[] { var2, clampPitch(var8) };
            }
            var8 -= var5;
        }
        return null;
    }

    private void update(double var1, boolean var3) {
        long var4 = System.currentTimeMillis();
        if (var4 - this.lastUpdateTime >= 70L) {
            this.lastUpdateTime = var4;
            this.landing = null;
            this.trackedPearl = this.findPearl(var1, var3);
            if (this.trackedPearl != null && this.trackedPearl.isAlive()) {
                Vec3 vector = this.predictPearlLanding(this.trackedPearl);
                if (vector != null && !this.isSafeLanding(vector)) {
                    this.landing = vector;
                }
            }
        }
    }

    private boolean isPassable(BlockPos blockPos) {
        return mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty();
    }

    private Vec3 throwOrigin() {
        return new Vec3(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
    }

    private static float clampPitch(float var0) {
        return Mth.clamp(var0, -90.0F, 90.0F);
    }

    private double landingDistance() {
        Vec3 eyePosition = mc.player.getEyePosition(1.0F);
        double distance = eyePosition.distanceTo(this.landing);
        boolean var4 = this.landing.y - eyePosition.y > 5.0;
        if (this.isBlocked(eyePosition, this.landing)) {
            return var4 ? 6.0 : 3.75;
        } else if (var4) {
            return 4.5;
        } else {
            return distance > 60.0 ? 3.0 : 2.25;
        }
    }

    private Entity resolveAuraTarget() {
        long var1 = System.currentTimeMillis();
        LivingEntity entity = AttackAura.currentTarget();
        if (entity != null && entity.isAlive()) {
            this.lastTarget = entity;
            this.lastTargetTime = var1;
            return entity;
        }
        if (this.lastTarget == null) {
            return null;
        }
        if (this.lastTarget.isAlive() && !this.lastTarget.isRemoved() && var1 - this.lastTargetTime < 10000L) {
            return this.lastTarget;
        }
        this.lastTarget = null;
        this.lastTargetTime = 0L;
        return null;
    }

    private BlockHitResult clip(Vec3 vector, Vec3 vector2) {
        return mc.level.clip(new ClipContext(vector, vector2, Block.COLLIDER, Fluid.NONE, mc.player));
    }

    public PearlTrajectoryAimer.ThrowSolution solve(double var1, boolean var3) {
        this.update(var1, var3);
        if (this.landing == null) {
            return null;
        }
        float[] var4 = this.solveDirect(this.landing);
        if (var4 == null) {
            var4 = this.solveFine(this.landing);
            if (var4 == null) {
                return null;
            }
        }
        Vec3 vector = this.landingOf(var4[0], var4[1]);
        if (vector != null && vector.distanceTo(this.landing) > this.landingDistance()) {
            var4 = this.solveFine(this.landing);
            if (var4 == null) {
                return null;
            }
        }
        return new PearlTrajectoryAimer.ThrowSolution(var4[0], var4[1]);
    }

    private float[] scanAngles(Vec3 vector, float var2, float var3, float var4, float var5, float var6, float var7, double var8) {
        float var10 = var5;
        while (var10 >= var6) {
            float var11 = -var3;
            while (var11 <= var3) {
                float var12 = var2 + var11;
                Vec3 vector2 = this.landingOf(var12, var10);
                if (vector2 != null && vector.distanceTo(vector2) <= var8) {
                    return new float[] { var12, clampPitch(var10) };
                }
                var11 += var4;
            }
            var10 -= var7;
        }
        return null;
    }

    private double[] evaluateAngles(float var1, float var2, Vec3 vector) {
        Vec3 vector2 = this.throwOrigin();
        Vec3 vector3 = this.throwVelocity(var1, var2);
        double minY = mc.level.getMinY();
        for (int var8 = 0; var8 < 160; var8++) {
            vector3 = vector3.subtract(0.0, 0.03, 0.0).scale(0.99);
            Vec3 vector4 = vector2.add(vector3);
            BlockHitResult blockHitResult = this.clip(vector2, vector4);
            Vec3 vector5 = null;
            if (blockHitResult.getType() == Type.BLOCK) {
                vector5 = blockHitResult.getLocation();
            } else if (vector4.y <= minY) {
                vector5 = new Vec3(vector4.x, minY, vector4.z);
            }
            if (vector5 != null) {
                if (PearlTarget.isInsideTree(vector5)) {
                    return null;
                }
                return new double[] { vector5.distanceTo(vector), var8 + 1 };
            }
            vector2 = vector4;
        }
        return null;
    }

    private Vec3 throwVelocity(float var1, float var2) {
        double var3 = Math.toRadians(var1);
        double var5 = Math.toRadians(var2);
        double var7 = Math.cos(var5);
        return new Vec3(-Math.sin(var3) * var7 * 1.5, -Math.sin(var5) * 1.5 + mc.player.getDeltaMovement().y, Math.cos(var3) * var7 * 1.5);
    }

    private float[] solveDirect(Vec3 vector) {
        Vec3 eyePosition = mc.player.getEyePosition(1.0F);
        float var3 = yawBetween(eyePosition, vector);
        double distance = eyePosition.distanceTo(vector);
        boolean var6 = vector.y - eyePosition.y > 5.0;
        float var7 = var6 ? 89.0F : 85.0F;
        float var8 = var6 ? -80.0F : (distance > 60.0 ? -50.0F : -30.0F);
        float var9 = var6 ? 0.3F : (distance > 60.0 ? 0.5F : 0.42F);
        double var10 = var6 ? 4.0 : 2.0;
        float var12 = 0.0F;
        double var13 = Double.MAX_VALUE;
        double var15 = Double.MAX_VALUE;
        for (float var17 = var7; var17 >= var8; var17 -= var9) {
            double[] var18 = this.evaluateAngles(var3, var17, vector);
            if (var18 != null && !(var18[0] > var10)) {
                boolean var19 = var18[1] < var13 || var18[1] == var13 && var18[0] < var15;
                if (var19) {
                    var13 = var18[1];
                    var12 = var17;
                    var15 = var18[0];
                }
            }
        }
        return var13 == Double.MAX_VALUE ? null : new float[] { var3, clampPitch(var12) };
    }

    private ThrownEnderpearl findPearl(double var1, boolean var3) {
        Entity entity = var3 ? this.resolveAuraTarget() : null;
        Vec3 position2 = mc.player.position();
        ThrownEnderpearl thrownEnderpearl = null;
        double var7 = Double.MAX_VALUE;
        for (Entity entity2 : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(130.0))) {
            if (entity2 instanceof ThrownEnderpearl var11 && var11.isAlive()) {
                Entity owner = var11.getOwner();
                if ((!var3 || entity != null && owner != null && owner.getId() == entity.getId()) && !(owner instanceof Player var13 && FriendManager.INSTANCE.isFriend(var13))) {
                    Vec3 vector = this.predictPearlLanding(var11);
                    if (vector != null) {
                        double distance = position2.distanceTo(vector);
                        if (!(distance < var1) && !(distance > 120.0) && !(distance >= var7)) {
                            thrownEnderpearl = var11;
                            var7 = distance;
                        }
                    }
                }
            }
        }
        return thrownEnderpearl;
    }

    private float[] solveFine(Vec3 vector) {
        Vec3 eyePosition = mc.player.getEyePosition(1.0F);
        float var3 = yawBetween(eyePosition, vector);
        double distance = eyePosition.distanceTo(vector);
        boolean var6 = vector.y - eyePosition.y > 5.0;
        boolean var7 = distance > 60.0;
        float var8 = var6 ? 89.0F : 85.0F;
        float var9 = var6 ? -85.0F : (var7 ? -70.0F : -50.0F);
        float var10 = var6 ? 1.0F : (var7 ? 1.2F : 1.5F);
        float var11 = var6 ? 40.0F : (var7 ? 30.0F : 20.0F);
        float var12 = var6 ? 2.0F : (var7 ? 3.0F : 2.5F);
        double var13 = var6 ? 4.5 : (var7 ? 3.0 : 2.25);
        float[] var15 = this.scanPitch(vector, var3, var8, var9, var10, var13);
        if (var15 != null) {
            return var15;
        }
        for (float var16 = -var11; var16 <= var11; var16 += var12) {
            if (var16 != 0.0F) {
                float[] var17 = this.scanPitch(vector, var3 + var16, var8, var9, var10, var13);
                if (var17 != null) {
                    return var17;
                }
            }
        }
        if (var6) {
            float[] var18 = this.scanAngles(vector, var3, var11, var12, 89.0F, 75.0F, 1.5F, var13 * 1.5);
            if (var18 != null) {
                return var18;
            }
        }
        return this.scanAngles(vector, var3, var11, var12 * 2.0F, var8, 70.0F, 2.0F, var6 ? 6.0 : 3.75);
    }

    public void reset() {
        this.clearState();
        this.lastTarget = null;
        this.lastTargetTime = 0L;
    }

    private boolean isSafeLanding(Vec3 vector) {
        BlockPos blockPos = BlockPos.containing(vector);
        return this.isPassable(blockPos.below()) && this.isPassable(blockPos.below(2));
    }

    private static float yawBetween(Vec3 vector, Vec3 vector2) {
        return (float) Math.toDegrees(Math.atan2(vector2.z - vector.z, vector2.x - vector.x)) - 90.0F;
    }

    private Vec3 predictPearlLanding(ThrownEnderpearl thrownEnderpearl) {
        return this.simulateLanding(thrownEnderpearl.position(), thrownEnderpearl.getDeltaMovement());
    }

    public void clearState() {
        this.lastUpdateTime = 0L;
        this.trackedPearl = null;
        this.landing = null;
    }

    public record ThrowSolution(float yaw, float pitch) {
    }
}
