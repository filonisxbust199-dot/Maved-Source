package ess.ll.client.module.movement;

import ess.ll.client.event.FireworkBoostEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.integration.OptionalModules.ElytraTarget;
import ess.ll.client.network.PostMovementPacketEvent;
import ess.ll.client.setting.ActionListSetting;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ModuleManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ElytraBooster extends ClientModule {
    private final NumberSetting<Float>[] yawSpeedTable = new NumberSetting[9];
    private final NumberSetting<Float>[] pitchSpeedTable = new NumberSetting[9];
    private final ModeSetting mode;
    private final ModeSetting anticheat;
    private final ModeSetting version;
    private final ToggleSetting adaptSpeed;
    private final NumberSetting<Float> vectorDistance;
    private final NumberSetting<Float> slowdownStrength;
    private double boostRamp;
    private int lastUpdateTick;
    private Vec3 previousMovement;
    private int turnHoldTicks;
    private float currentYaw;
    private float currentPitch;
    private float previousYaw;
    private float previousPitch;
    private int rotationTick;
    private Vec3 lastMovement;
    private int lastMovementTick;
    private final NumberSetting<Float> horizontalSpeed;
    private final NumberSetting<Float> verticalSpeed;
    private final ActionListSetting presets;

    public Vec3 getPreviousMovement() {
        return this.previousMovement;
    }

    public ElytraBooster() {
        super(ModuleCategory.Movement, ModuleSubCategory.Main);
        this.mode = new ModeSetting("Режим").addModes("Обычный", "Кастомный", "Клиентский").withDefault("Обычный").withContainer(this);
        this.anticheat = new ModeSetting("Античит").addModes("ReallyWorld", "Grim").withDefault("ReallyWorld").withVisibility(() -> this.mode.is("Клиентский")).withContainer(this);
        this.version = new ModeSetting("Версия").addModes("1.17.1", "1.18.1").withDefault("1.18.1").withVisibility(() -> this.mode.is("Клиентский")).withContainer(this);
        this.adaptSpeed = new ToggleSetting("Адаптация скорости").withDefault(false).withContainer(this);
        this.vectorDistance = new NumberSetting<Float>("Дистанция от вектора").withMin(1.0F).withMax(8.0F).withStep(0.5F).withDefault(6.0F).withVisibility(this.adaptSpeed::isEnabled).withContainer(this);
        this.slowdownStrength = new NumberSetting<Float>("Сила замедления").withMin(0.1F).withMax(1.0F).withStep(0.05F).withDefault(1.0F).withVisibility(this.adaptSpeed::isEnabled).withContainer(this);
        this.currentYaw = Float.NaN;
        this.currentPitch = Float.NaN;
        this.previousYaw = Float.NaN;
        this.previousPitch = Float.NaN;
        this.rotationTick = -1;
        this.lastMovementTick = -1;
        this.horizontalSpeed = new NumberSetting<Float>("speedX").withMin(1.5F).withMax(2.5F).withStep(0.01F).withDefault(1.5F).withVisibility(() -> this.mode.is("Обычный")).withContainer(this);
        this.verticalSpeed = new NumberSetting<Float>("speedY").withMin(1.5F).withMax(2.5F).withStep(0.01F).withDefault(1.5F).withVisibility(() -> this.mode.is("Обычный")).withContainer(this);
        this.presets = new ActionListSetting("Пресеты").addAction("Default", () -> {
            this.yawSpeedTable[0].setValue(1.5F);
            this.yawSpeedTable[1].setValue(1.5F);
            this.yawSpeedTable[2].setValue(1.5F);
            this.yawSpeedTable[3].setValue(1.5F);
            this.yawSpeedTable[4].setValue(1.5F);
            this.yawSpeedTable[5].setValue(1.5F);
            this.yawSpeedTable[6].setValue(1.5F);
            this.yawSpeedTable[7].setValue(1.5F);
            this.yawSpeedTable[8].setValue(1.5F);
            this.pitchSpeedTable[0].setValue(1.5F);
            this.pitchSpeedTable[1].setValue(1.5F);
            this.pitchSpeedTable[2].setValue(1.5F);
            this.pitchSpeedTable[3].setValue(1.5F);
            this.pitchSpeedTable[4].setValue(1.5F);
            this.pitchSpeedTable[5].setValue(1.5F);
            this.pitchSpeedTable[6].setValue(1.5F);
            this.pitchSpeedTable[7].setValue(1.5F);
            this.pitchSpeedTable[8].setValue(1.5F);
        }).addAction("Default Grim", () -> {
            this.yawSpeedTable[0].setValue(1.58F);
            this.yawSpeedTable[1].setValue(1.62F);
            this.yawSpeedTable[2].setValue(1.65F);
            this.yawSpeedTable[3].setValue(1.69F);
            this.yawSpeedTable[4].setValue(1.73F);
            this.yawSpeedTable[5].setValue(1.81F);
            this.yawSpeedTable[6].setValue(1.97F);
            this.yawSpeedTable[7].setValue(2.09F);
            this.yawSpeedTable[8].setValue(2.19F);
            this.pitchSpeedTable[0].setValue(1.56F);
            this.pitchSpeedTable[1].setValue(1.61F);
            this.pitchSpeedTable[2].setValue(1.74F);
            this.pitchSpeedTable[3].setValue(1.76F);
            this.pitchSpeedTable[4].setValue(1.77F);
            this.pitchSpeedTable[5].setValue(1.88F);
            this.pitchSpeedTable[6].setValue(1.9F);
            this.pitchSpeedTable[7].setValue(2.01F);
            this.pitchSpeedTable[8].setValue(2.24F);
        }).addAction("ReallyWorld Rage", () -> {
            this.yawSpeedTable[0].setValue(1.58F);
            this.yawSpeedTable[1].setValue(1.62F);
            this.yawSpeedTable[2].setValue(1.65F);
            this.yawSpeedTable[3].setValue(1.69F);
            this.yawSpeedTable[4].setValue(1.73F);
            this.yawSpeedTable[5].setValue(1.79F);
            this.yawSpeedTable[6].setValue(1.79F);
            this.yawSpeedTable[7].setValue(1.79F);
            this.yawSpeedTable[8].setValue(1.81F);
            this.pitchSpeedTable[0].setValue(1.54F);
            this.pitchSpeedTable[1].setValue(1.57F);
            this.pitchSpeedTable[2].setValue(1.58F);
            this.pitchSpeedTable[3].setValue(1.6F);
            this.pitchSpeedTable[4].setValue(1.63F);
            this.pitchSpeedTable[5].setValue(1.7F);
            this.pitchSpeedTable[6].setValue(1.89F);
            this.pitchSpeedTable[7].setValue(2.01F);
            this.pitchSpeedTable[8].setValue(2.24F);
        }).withVisibility(() -> this.mode.is("Кастомный")).withContainer(this);
        this.setName("Elytra Booster");
        String[] var1 = new String[] { "0 - 5", "5 - 10", "10 - 15", "15 - 20", "20 - 25", "25 - 30", "30 - 35", "35 - 40", "40 - 45" };
        for (int var2 = 0; var2 < this.yawSpeedTable.length; var2++) {
            this.yawSpeedTable[var2] = new NumberSetting<Float>("yaw " + var1[var2]).withMin(1.5F).withMax(2.5F).withStep(0.01F).withDefault(1.5F).withVisibility(() -> this.mode.is("Кастомный")).withContainer(this);
        }
        for (int var3 = 0; var3 < this.pitchSpeedTable.length; var3++) {
            this.pitchSpeedTable[var3] = new NumberSetting<Float>("pitch " + var1[var3]).withMin(1.5F).withMax(2.5F).withStep(0.01F).withDefault(1.5F).withVisibility(() -> this.mode.is("Кастомный")).withContainer(this);
        }
    }

    private Vec2 bestBoostFactors(Vec3 vector, Vec3 vector2, Vec3 vector3, double var4, double var6, double var8) {
        float yRot = mc.player.getYRot();
        float xRot = mc.player.getXRot();
        float var12 = Mth.wrapDegrees(yRot - this.previousYawOr(yRot));
        float var13 = xRot - this.previousPitchOr(xRot);
        float[] var14 = new float[] { xRot, Mth.clamp(xRot + var13 * 0.5F, -90.0F, 90.0F), Mth.clamp(xRot + var13, -90.0F, 90.0F) };
        Vec3[] vector4 = new Vec3[] { vector, Vec3.directionFromRotation(var14[1], yRot + var12 * 0.5F), Vec3.directionFromRotation(var14[2], yRot + var12) };
        Vec3[] vector5 = new Vec3[vector4.length];
        Vec3[] vector6 = new Vec3[vector4.length];
        double var18 = this.angleToLook(vector);
        Vec3 vector7 = new Vec3(maxNegativeAxis(rotateUnitPositive(vector.x, var18), vector.x, var8), maxNegativeAxis(rotateUnitPositive(vector.y, var18), vector.y, var8), maxNegativeAxis(rotateUnitPositive(vector.z, var18), vector.z, var8));
        Vec3 vector8 = new Vec3(maxPositiveAxis(rotateUnitNegative(vector.x, var18), vector.x, var8), maxPositiveAxis(rotateUnitNegative(vector.y, var18), vector.y, var8), maxPositiveAxis(rotateUnitNegative(vector.z, var18), vector.z, var8));
        for (int var22 = 0; var22 < vector4.length; var22++) {
            Vec3 vector9 = this.rotateHorizontal(vector2, vector4[var22], var14[var22]);
            vector5[var22] = new Vec3(vector9.x + Math.min(0.0, vector7.x - vector2.x), vector9.y + Math.min(0.0, vector7.y - vector2.y), vector9.z + Math.min(0.0, vector7.z - vector2.z));
            vector6[var22] = new Vec3(vector9.x + Math.max(0.0, vector8.x - vector2.x), vector9.y + Math.max(0.0, vector8.y - vector2.y), vector9.z + Math.max(0.0, vector8.z - vector2.z));
        }
        double var26 = this.searchBoost(vector, vector4, var14, vector5, vector6, vector3, var4, var6, true);
        double var24 = this.searchBoost(vector, vector4, var14, vector5, vector6, vector3, var4, var26, false);
        var26 = this.searchBoost(vector, vector4, var14, vector5, vector6, vector3, var24, var6, true);
        var24 = this.searchBoost(vector, vector4, var14, vector5, vector6, vector3, var4, var26, false);
        return new Vec2((float) var24, (float) var26);
    }

    private boolean isBoostSafe(double var1, double var3, double var5, double var7, float var9) {
        return isWithinLimits(var1, var3, var5, var7, var9);
    }

    private Vec2 tableBoostFactors() {
        if (mc.player == null) {
            return new Vec2(1.5F, 1.5F);
        }
        float var1 = Mth.wrapDegrees(mc.player.getYRot());
        float var2 = Math.abs(Mth.wrapDegrees(mc.player.getXRot()));
        AttackAura attackAura = ModuleManager.INSTANCE.aura();
        ElytraTarget elytraTarget2 = ModuleManager.INSTANCE.elytraTarget();
        var1 = this.octantAngle(var1);
        var2 = this.octantAngle(var2);
        float var5 = this.yawSpeedAt(var1);
        float var6 = this.pitchSpeedAt(var2);
        if (var6 > var5) {
            var5 = var6;
        }
        return new Vec2(var5, var6);
    }

    public ModeSetting getVersion() {
        return this.version;
    }

    public ToggleSetting getAdaptSpeed() {
        return this.adaptSpeed;
    }

    public NumberSetting<Float>[] getYawSpeedTable() {
        return this.yawSpeedTable;
    }

    public int getTurnHoldTicks() {
        return this.turnHoldTicks;
    }

    public float getPreviousPitch() {
        return this.previousPitch;
    }

    private float pitchSpeedAt(float var1) {
        int var2 = (int) (var1 / 5.0F);
        if (var2 >= this.pitchSpeedTable.length) {
            var2 = this.pitchSpeedTable.length - 1;
        }
        return this.pitchSpeedTable[var2].getValue();
    }

    public int getLastMovementTick() {
        return this.lastMovementTick;
    }

    private double maxSafeBoost(Vec3 vector, Vec3[] vector2, float[] var3, Vec3[] vector3, Vec3[] vector4, Vec3 vector5, double var7, double var9, double var11, double var13, boolean var15) {
        for (int var16 = 0; var16 < 8; var16++) {
            double var17 = (var11 + var13) * 0.5;
            if (this.refineBoost(vector, vector2, var3, vector3, vector4, vector5, var7, var9, var17, var15) <= 0.0) {
                var11 = var17;
            } else {
                var13 = var17;
            }
        }
        return var11;
    }

    private static double rotateUnitNegative(double var0, double var2) {
        double var4 = Math.sqrt(Math.max(0.0, 1.0 - var0 * var0));
        return Mth.clamp(Math.cos(var2) * var0 - Math.sin(var2) * var4, -1.0, 1.0);
    }

    public Vec3 getLastMovement() {
        return this.lastMovement;
    }

    public double getBoostRamp() {
        return this.boostRamp;
    }

    private static double rotateUnitPositive(double var0, double var2) {
        double var4 = Math.sqrt(Math.max(0.0, 1.0 - var0 * var0));
        return Mth.clamp(Math.cos(var2) * var0 + Math.sin(var2) * var4, -1.0, 1.0);
    }

    public NumberSetting<Float> getVerticalSpeed() {
        return this.verticalSpeed;
    }

    private Vec3 stepBoost(Vec3 vector, Vec3 vector2, double var3, double var5) {
        return vector2.add(vector.x * 0.1 + (vector.x * var3 - vector2.x) * 0.5, vector.y * 0.1 + (vector.y * var5 - vector2.y) * 0.5, vector.z * 0.1 + (vector.z * var3 - vector2.z) * 0.5);
    }

    private static boolean isWithinLimits(double var0, double var2, double var4, double var6, float var8) {
        double var9 = Math.max((-1.7), (Math.min((-0.05), var2) + Math.min((-0.05), var4)) * 1.7) * 0.9;
        double var11 = Math.min(1.7, (Math.max(0.05, var2) + Math.max(0.05, var4)) * 1.7) * 0.9;
        double var13 = 0.1 + var8 * 0.5;
        double var15 = var6 * 0.5 + var2 * var13;
        double var17 = var6 * 0.5 + var0 * var13;
        return var17 >= Math.min(var15, var9) && var17 <= Math.max(var15, var11);
    }

    private static double maxPositiveAxis(double var0, double var2, double var4) {
        return Math.min(var4, (Math.max(0.05, var0) + Math.max(0.05, var2)) * 1.7);
    }

    private float previousYawOr(float var1) {
        if (mc.player != null && this.rotationTick >= 0 && mc.player.tickCount - this.rotationTick <= 1) {
            float var2 = this.rotationTick == mc.player.tickCount ? this.previousYaw : this.currentYaw;
            return Float.isNaN(var2) ? var1 : var2;
        } else {
            return var1;
        }
    }

    public NumberSetting<Float> getHorizontalSpeed() {
        return this.horizontalSpeed;
    }

    private Vec3 lookVector(Vec3 vector) {
        float var2 = this.previousYawOr(Float.NaN);
        float var3 = this.previousPitchOr(Float.NaN);
        return !Float.isNaN(var2) && !Float.isNaN(var3) ? Vec3.directionFromRotation(var3, var2) : vector;
    }

    private boolean isBoostValid(Vec3 vector, Vec3 vector2, Vec3 vector3, Vec3 vector4, float var5, float var6) {
        return this.isBoostSafe(vector.x, vector2.x, vector3.x, vector4.x, var5) && this.isBoostSafe(vector.y, vector2.y, vector3.y, vector4.y, var6) && this.isBoostSafe(vector.z, vector2.z, vector3.z, vector4.z, var5);
    }

    public ModeSetting getAnticheat() {
        return this.anticheat;
    }

    private static double clampBoost(double var0, double var2, double var4) {
        double var6 = var4 - var2 <= 0.04 ? 0.0 : 0.02;
        if (var0 < var2 + var6) {
            return var2 + var6 - var0;
        } else {
            return var0 > var4 - var6 ? var0 - (var4 - var6) : 0.0;
        }
    }

    public NumberSetting<Float>[] getPitchSpeedTable() {
        return this.pitchSpeedTable;
    }

    private static double maxNegativeAxis(double var0, double var2, double var4) {
        return Math.max(-var4, (Math.min((-0.05), var0) + Math.min((-0.05), var2)) * 1.7);
    }

    public NumberSetting<Float> getVectorDistance() {
        return this.vectorDistance;
    }

    private double refineBoost(Vec3 vector, Vec3[] vector2, float[] var3, Vec3[] vector3, Vec3[] vector4, Vec3 vector5, double var7, double var9, double var11, boolean var13) {
        Vec3 vector6 = this.stepBoost(vector, vector5, var13 ? var7 : var11, var13 ? var11 : var9);
        double var15 = 0.0;
        for (int var17 = 0; var17 < vector2.length; var17++) {
            Vec3 vector7 = this.rotateHorizontal(vector6, vector2[var17], var3[var17]);
            double var19 = var13 ? clampBoost(vector7.y, vector3[var17].y, vector4[var17].y) : clampBoost(vector7.x, vector3[var17].x, vector4[var17].x) + clampBoost(vector7.z, vector3[var17].z, vector4[var17].z);
            if (var19 > var15) {
                var15 = var19;
            }
        }
        return var15;
    }

    public NumberSetting<Float> getSlowdownStrength() {
        return this.slowdownStrength;
    }

    private double searchBoost(Vec3 vector, Vec3[] vector2, float[] var3, Vec3[] vector3, Vec3[] vector4, Vec3 vector5, double var7, double var9, boolean var11) {
        double var12 = var11 ? var9 : var7;
        if (var12 <= 0.0) {
            return var12;
        }
        if (this.refineBoost(vector, vector2, var3, vector3, vector4, vector5, var7, var9, var12, var11) <= 0.0) {
            return var12;
        }
        double var14 = (var12 - 0.0) / 16.0;
        double var16 = var12;
        double var18 = Double.MAX_VALUE;
        for (int var20 = 1; var20 <= 16; var20++) {
            double var21 = var12 - var14 * var20;
            double var23 = this.refineBoost(vector, vector2, var3, vector3, vector4, vector5, var7, var9, var21, var11);
            if (var23 < var18) {
                var18 = var23;
                var16 = var21;
            }
            if (var23 <= 0.0) {
                return this.maxSafeBoost(vector, vector2, var3, vector3, vector4, vector5, var7, var9, var21, var21 + var14, var11);
            }
        }
        return var16;
    }

    private Vec3 targetDirection(Vec3 vector, float var2, float var3) {
        AttackAura attackAura = ModuleManager.INSTANCE.aura();
        ElytraTarget elytraTarget2 = ModuleManager.INSTANCE.elytraTarget();
        LivingEntity entity = AttackAura.target;
        if (mc.player == null || entity == null) {
            return this.resetBoost(vector);
        }
        if (!mc.player.isFallFlying()) {
            return this.resetBoost(vector);
        }
        if (!attackAura.isEnabled() || !attackAura.chasingElytraTarget || !elytraTarget2.isEnabled()) {
            return this.resetBoost(vector);
        }
        if (attackAura.predictedAimPoint == null) {
            return this.resetBoost(vector);
        }
        double distance = mc.player.distanceTo(entity);
        if (distance > 5.0) {
            return this.resetBoost(vector);
        }
        Vec3 vector2 = attackAura.predictedAimPoint.subtract(mc.player.getEyePosition());
        if (vector2.lengthSqr() < 1.0E-6) {
            return this.resetBoost(vector);
        }
        Vec3 vector3 = vector2.normalize();
        double var11 = Mth.clamp(vector.dot(vector3), -1.0, 1.0);
        double var13 = Math.toDegrees(Math.acos(var11));
        if (var13 < 1.0) {
            return this.resetBoost(vector);
        }
        int tickCount2 = mc.player.tickCount;
        if (tickCount2 - this.lastUpdateTick > 2) {
            this.boostRamp = 0.0;
        }
        if (tickCount2 != this.lastUpdateTick && this.turnHoldTicks > 0) {
            this.turnHoldTicks--;
        }
        this.lastUpdateTick = tickCount2;
        Vec3 deltaMovement = entity.getDeltaMovement();
        if (this.previousMovement != null) {
            double var17 = deltaMovement.horizontalDistance();
            double var19 = this.previousMovement.horizontalDistance();
            if (var17 > 0.05 && var19 > 0.05) {
                double var21 = (deltaMovement.x * this.previousMovement.x + deltaMovement.z * this.previousMovement.z) / (var17 * var19);
                if (var21 < Math.cos(Math.toRadians(35.0))) {
                    this.turnHoldTicks = 6;
                }
            }
        }
        this.previousMovement = deltaMovement;
        Vec3 deltaMovement2 = mc.player.getDeltaMovement();
        double var18 = 1.0 - Mth.clamp(distance / 5.0, 0.0, 1.0);
        double var20 = 1.0 - Mth.clamp((var13 - 15.0) / 75.0, 0.0, 1.0);
        double var22 = Mth.clamp(1.0 - deltaMovement2.length() / 0.8, 0.0, 1.0);
        double var24 = Math.max(var18, Math.max(var20, var22));
        if (this.turnHoldTicks > 0) {
            var24 *= 0.3;
        }
        Vec3 vector4 = Vec3.directionFromRotation(mc.player.xRotO, mc.player.yRotO);
        double var27 = Mth.clamp(vector.dot(vector4), -1.0, 1.0);
        double var29 = Math.toDegrees(Math.acos(var27));
        var24 *= 1.0 - Mth.clamp(var29 / 25.0, 0.0, 0.5);
        this.boostRamp = this.boostRamp + Mth.clamp(var24 - this.boostRamp, -0.3, 0.25);
        double var31 = this.boostRamp;
        if (var31 < 0.001) {
            return vector;
        }
        for (int var33 = 0; var33 < 6; var33++) {
            Vec3 vector5 = vector.lerp(vector3, var31);
            if (vector5.lengthSqr() < 1.0E-6) {
                break;
            }
            vector5 = vector5.normalize();
            if (this.isBoostValid(vector5, vector, vector4, deltaMovement2, var2, var3)) {
                return vector5;
            }
            var31 *= 0.6;
        }
        return vector;
    }

    private float octantAngle(float var1) {
        return foldToOctant(var1);
    }

    private Vec3 rotateHorizontal(Vec3 vector, Vec3 vector2, float var3) {
        float var4 = var3 * (float) (Math.PI / 180.0);
        double var5 = Math.sqrt(vector2.x * vector2.x + vector2.z * vector2.z);
        double var7 = Math.abs(vector.x) < 0.003 ? 0.0 : vector.x;
        double var9 = Math.abs(vector.y) < 0.003 ? 0.0 : vector.y;
        double var11 = Math.abs(vector.z) < 0.003 ? 0.0 : vector.z;
        double var13 = Math.sqrt(var7 * var7 + var11 * var11);
        double attributeValue = mc.player.getAttributeValue(Attributes.GRAVITY);
        if (var9 <= 0.0 && mc.player.hasEffect(MobEffects.SLOW_FALLING)) {
            attributeValue = Math.min(attributeValue, 0.01);
        }
        double var17 = Mth.square(Math.cos(var4));
        double var19 = var7;
        double var21 = var9 + attributeValue * ((-1.0) + var17 * 0.75);
        double var23 = var11;
        if (var21 < 0.0 && var5 > 0.0) {
            double var25 = var21 * -0.1 * var17;
            var19 += vector2.x * var25 / var5;
            var21 += var25;
            var23 += vector2.z * var25 / var5;
        }
        if (var4 < 0.0F && var5 > 0.0) {
            double var27 = var13 * -Mth.sin(var4) * 0.04;
            var19 += -vector2.x * var27 / var5;
            var21 += var27 * 3.2;
            var23 += -vector2.z * var27 / var5;
        }
        if (var5 > 0.0) {
            var19 += (vector2.x / var5 * var13 - var19) * 0.1;
            var23 += (vector2.z / var5 * var13 - var23) * 0.1;
        }
        return new Vec3(var19 * 0.99, var21 * 0.98, var23 * 0.99);
    }

    public int getLastUpdateTick() {
        return this.lastUpdateTick;
    }

    private double solveBoostMultiplier(Vec3 vector, Vec3 vector2, double var3) {
        double var5 = 0.5 * vector.x;
        double var7 = 0.5 * vector.z;
        double var9 = var5 * var5 + var7 * var7;
        if (var9 < 1.0E-9) {
            return 2.5;
        }
        double var11 = 0.5 * vector2.x + 0.1 * vector.x;
        double var13 = 0.5 * vector2.z + 0.1 * vector.z;
        double var15 = 2.0 * (var5 * var11 + var7 * var13);
        double var17 = var11 * var11 + var13 * var13 - var3 * var3;
        double var19 = var15 * var15 - 4.0 * var9 * var17;
        return var19 < 0.0 ? -var15 / (2.0 * var9) : (-var15 + Math.sqrt(var19)) / (2.0 * var9);
    }

    private static float foldToOctant(float var0) {
        float var1 = Math.abs(var0);
        if (var1 > 90.0F) {
            var1 = 180.0F - var1;
        }
        if (var1 > 45.0F) {
            var1 = 90.0F - var1;
        }
        return var1;
    }

    private Vec2 manualBoostFactors() {
        return new Vec2(this.horizontalSpeed.getValue(), this.verticalSpeed.getValue());
    }

    public int getRotationTick() {
        return this.rotationTick;
    }

    public ActionListSetting getPresets() {
        return this.presets;
    }

    public float getPreviousYaw() {
        return this.previousYaw;
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    private Vec3 resetBoost(Vec3 vector) {
        this.boostRamp = 0.0;
        return vector;
    }

    private double boostToReach(double var1, double var3, double var5, double var7, double var9) {
        if (Math.abs(var1) < 0.001) {
            return 2.5;
        }
        double var11 = Math.min(var7, (Math.max(0.05, var1) + Math.max(0.05, var3)) * 1.7);
        double var13 = Math.max(-var7, (Math.min((-0.05), var1) + Math.min((-0.05), var3)) * 1.7);
        double var15 = (var1 > 0.0 ? var11 : -var13) - var9;
        if (var15 < 0.0) {
            var15 = 0.0;
        }
        double var17 = var1 > 0.0 ? var15 : -var15;
        double var19 = (var17 - 0.5 * var5) / var1;
        return (var19 - 0.1) / 0.5;
    }

    public float getCurrentPitch() {
        return this.currentPitch;
    }

    private Vec3 latestMovement(Vec3 vector) {
        return this.lastMovement != null && mc.player != null && mc.player.tickCount - this.lastMovementTick <= 1 ? this.lastMovement : vector;
    }

    private float adaptiveSpeedFactor() {
        if (!this.adaptSpeed.isEnabled()) {
            return 1.0F;
        }
        AttackAura attackAura = ModuleManager.INSTANCE.aura();
        LivingEntity entity = AttackAura.target;
        if (mc.player == null || entity == null || !attackAura.isEnabled()) {
            return 1.0F;
        }
        if (!mc.player.isFallFlying()) {
            return 1.0F;
        }
        Vec3 eyePosition = mc.player.getEyePosition();
        Vec3 vector = attackAura.predictedAimPoint != null ? attackAura.predictedAimPoint : entity.getBoundingBox().getCenter();
        double distance = eyePosition.distanceTo(vector);
        double var7 = this.vectorDistance.getValue().floatValue();
        if (distance >= var7) {
            return 1.0F;
        }
        float value = this.slowdownStrength.getValue();
        return (float) (1.0 - value * (1.0 - distance / var7));
    }

    @Subscribe(4)
    private void onPostMovementPacket(PostMovementPacketEvent event) {
        if (mc.player == null) {
            this.rotationTick = -1;
            this.currentYaw = Float.NaN;
            this.currentPitch = Float.NaN;
            this.previousYaw = Float.NaN;
            this.previousPitch = Float.NaN;
            this.lastMovement = null;
            this.lastMovementTick = -1;
        } else {
            this.lastMovement = mc.player.getDeltaMovement();
            this.lastMovementTick = mc.player.tickCount;
            this.previousYaw = this.currentYaw;
            this.previousPitch = this.currentPitch;
            this.currentYaw = mc.player.getYRot();
            this.currentPitch = mc.player.getXRot();
            this.rotationTick = mc.player.tickCount;
        }
    }

    @Subscribe
    private void onFireworkBoost(FireworkBoostEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            Vec2 vec2 = switch ((String) this.mode.getValue()) {
                case "Кастомный" ->
                    this.tableBoostFactors();
                case "Клиентский" ->
                    this.anticheatBoostFactors();
                case "Обычный" ->
                    this.manualBoostFactors();
                default ->
                    new Vec2(1.5F, 1.5F);
            };
            float var5 = this.adaptiveSpeedFactor();
            if (var5 < 1.0F) {
                vec2 = new Vec2(vec2.x * var5, vec2.y * var5);
            }
            event.setHorizontalSpeed(vec2.x);
            event.setVerticalSpeed(vec2.y);
            event.getDirection();
            event.getAcceleration();
        }
    }

    private float yawSpeedAt(float var1) {
        int var2 = (int) (var1 / 5.0F);
        if (var2 >= this.yawSpeedTable.length) {
            var2 = this.yawSpeedTable.length - 1;
        }
        return this.yawSpeedTable[var2].getValue();
    }

    private double angleToLook(Vec3 vector) {
        Vec3 vector2 = this.lookVector(vector);
        double var3 = Math.toDegrees(Math.acos(Mth.clamp(vector.dot(vector2), -1.0, 1.0)));
        return Math.toRadians(Mth.clamp(var3 * 1.3, 8.0, 40.0));
    }

    private float previousPitchOr(float var1) {
        if (mc.player != null && this.rotationTick >= 0 && mc.player.tickCount - this.rotationTick <= 1) {
            float var2 = this.rotationTick == mc.player.tickCount ? this.previousPitch : this.currentPitch;
            return Float.isNaN(var2) ? var1 : var2;
        } else {
            return var1;
        }
    }

    private Vec2 anticheatBoostFactors() {
        LocalPlayer player2 = mc.player;
        if (player2 == null) {
            return new Vec2(1.5F, 1.5F);
        }
        boolean var2 = this.anticheat.is("Grim");
        boolean var3 = this.version.is("1.17.1");
        double var4 = var3 ? 1.7 : 1.68;
        double var6 = var3 ? 1.93 : 1.9;
        Vec3 lookAngle = player2.getLookAngle();
        Vec3 vector = this.lookVector(Vec3.directionFromRotation(player2.xRotO, player2.yRotO));
        Vec3 deltaMovement = player2.getDeltaMovement();
        Vec3 vector2 = this.latestMovement(deltaMovement);
        Vec2 vec2 = this.bestBoostFactors(lookAngle, vector2, deltaMovement, 2.5, 2.5, var4);
        if (!var2) {
            double var13 = 0.5 * deltaMovement.y + lookAngle.y * (0.1 + 0.5 * vec2.y);
            double var15 = var13 < 0.0 ? var6 - 0.45 * -var13 : var6;
            double var17 = this.solveBoostMultiplier(lookAngle, deltaMovement, var15);
            if (var17 < vec2.x) {
                vec2 = this.bestBoostFactors(lookAngle, vector2, deltaMovement, var17, vec2.y, var4);
            }
        }
        double x2 = vec2.x;
        double y2 = vec2.y;
        return vec2;
    }

    public float getCurrentYaw() {
        return this.currentYaw;
    }
}
