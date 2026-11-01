package ess.ll.client.features.combat;

import ess.ll.client.integration.EntityExtensionAccess;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public class AuraTargetPointTracker {
    private Vec3 lastAimPoint = Vec3.ZERO;

    public Vec3 getLastAimPoint() {
        return this.lastAimPoint;
    }

    private Vec3 predictedCenter(LivingEntity entity) {
        AuraTargetPointTracker.MotionSample var2 = this.readMotion(entity);
        Vec3 vector = entity.position().add(0.0, 0.5, 0.0);
        Vec3 vector2 = vector;
        Vec3 deltaMovement = entity.getDeltaMovement();
        for (int var6 = 0; var6 < 8; var6++) {
            deltaMovement = this.offsetTowards(deltaMovement, var2);
            vector2 = vector2.add(deltaMovement);
        }
        return vector2.subtract(vector);
    }

    public Vec3 predictedAimPoint(LivingEntity entity, float var2, float var3) {
        if (entity == null) {
            this.lastAimPoint = Vec3.ZERO;
            return Vec3.ZERO;
        } else {
            Vec3 vector = entity.position().add(0.0, 0.5, 0.0);
            Vec2 vec2f = ((EntityExtensionAccess) entity).getVec2f();
            Vec3 vector2 = entity.calculateViewVector(vec2f.y, vec2f.x).normalize();
            return this.extrapolate(vector, vector2, var2, var3);
        }
    }

    private Vec3 extrapolate(Vec3 vector, Vec3 vector2, float var3, float var4) {
        this.lastAimPoint = vector.add(vector2.scale(var3));
        return vector.add(vector2.scale(var4));
    }

    private AuraTargetPointTracker.MotionSample readMotion(LivingEntity entity) {
        Vec2 vec2f = ((EntityExtensionAccess) entity).getVec2f();
        float x2 = vec2f.x;
        float y2 = vec2f.y;
        Vec3 viewVector = entity.calculateViewVector(y2, x2);
        float var6 = y2 * (float) (Math.PI / 180.0);
        double var7 = Math.sqrt(viewVector.x * viewVector.x + viewVector.z * viewVector.z);
        double var9 = Mth.square(Math.cos(var6));
        boolean var11 = var7 > 0.0;
        double var12 = ((LivingEntityMixin_1248) (Object) entity).IgetEffectiveGravity();
        boolean var14 = entity.hasEffect(MobEffects.SLOW_FALLING);
        double var15 = -1.0 + var9 * 0.75;
        return new AuraTargetPointTracker.MotionSample(var11 ? viewVector.x / var7 : 0.0, var11 ? viewVector.z / var7 : 0.0, var11, var12 * var15, Math.min(var12, 0.01) * var15, var14, -0.1 * var9, var6 < 0.0F && var11, -Mth.sin(var6) * 0.04);
    }

    public void reset() {
        this.lastAimPoint = Vec3.ZERO;
    }

    public Vec3 aimPoint(LivingEntity entity, float var2, float var3) {
        if (entity == null) {
            this.lastAimPoint = Vec3.ZERO;
            return Vec3.ZERO;
        } else {
            Vec3 vector = entity.position().add(0.0, 0.5, 0.0);
            Vec3 vector2 = this.predictedCenter(entity);
            if (vector2.lengthSqr() < 1.0E-4) {
                Vec2 vec2f = ((EntityExtensionAccess) entity).getVec2f();
                Vec3 vector3 = entity.calculateViewVector(vec2f.y, vec2f.x).normalize();
                return this.extrapolate(vector, vector3, 0.0F, 0.0F);
            } else {
                double count = vector2.length();
                Vec3 vector4 = vector2.scale(1.0 / count);
                float var9 = (float) (count / 8.0);
                return this.extrapolate(vector, vector4, var2 * var9, var3 * var9);
            }
        }
    }

    private Vec3 offsetTowards(Vec3 vector, AuraTargetPointTracker.MotionSample var2) {
        double x2 = vector.x;
        double y2 = vector.y;
        double z2 = vector.z;
        double var9 = Math.sqrt(x2 * x2 + z2 * z2);
        y2 += var2.slowFalling() && y2 <= 0.0 ? var2.gravLiftSlow() : var2.gravLiftNormal();
        if (var2.hasHor() && y2 < 0.0) {
            double var11 = y2 * var2.diveCoef();
            x2 += var2.lookX() * var11;
            y2 += var11;
            z2 += var2.lookZ() * var11;
        }
        if (var2.liftUp()) {
            double var14 = var9 * var2.liftCoef();
            x2 -= var2.lookX() * var14;
            y2 += var14 * 3.2;
            z2 -= var2.lookZ() * var14;
        }
        if (var2.hasHor()) {
            x2 += (var2.lookX() * var9 - x2) * 0.1;
            z2 += (var2.lookZ() * var9 - z2) * 0.1;
        }
        return new Vec3(x2 * 0.99F, y2 * 0.98F, z2 * 0.99F);
    }

    private record MotionSample(double lookX, double lookZ, boolean hasHor, double gravLiftNormal, double gravLiftSlow, boolean slowFalling, double diveCoef, boolean liftUp, double liftCoef) {
    }
}
