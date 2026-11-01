package ess.ll.client.features.combat;

import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.AttackCooldownUtils;
import ess.ll.client.util.RaycastUtils;
import ess.ll.client.util.RotationMath;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public final class MatrixAuraRotation implements AuraRotationStrategy {
    private int noiseSeed = GuiHitTestUtils.RANDOM.nextInt(1024);

    @Override
    public void reset() {
        this.noiseSeed = GuiHitTestUtils.RANDOM.nextInt(1024);
    }

    @Override
    public Set<AuraRotationUpdateMode> updateModes() {
        return EnumSet.of(AuraRotationUpdateMode.TICK);
    }

    @Override
    public void calculateRotation(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        this.applyInstantTurn(var1, var2);
        float var3 = Math.abs(var1.yawDelta()) + Math.abs(var1.pitchDelta());
        int var4 = AttackCooldownUtils.ticksUntilCharged(mc.player);
        int var5 = var1.isGliding() ? 2 : 1;
        boolean var6 = this.isInRange(var1);
        var2.setReturnSpeeds(1000.0F, 1000.0F);
        float var7;
        float var8;
        if (var6 && var4 <= var5) {
            var7 = var3;
            var8 = var3;
        } else {
            float var9 = GuiHitTestUtils.clamp01(var3 / 90.0F);
            float var10 = Mth.lerp(var9 * var9, 0.55F, 0.85F + (float) Math.random() * 0.25F);
            float var11 = Math.max(0.15F, var3 * var10);
            if (var6 && var4 <= 3 && !this.isLookingAtTarget(var1)) {
                var11 = Math.max(var11, var3 / ((var4 - var5) * 3.0F));
            }
            var7 = var11;
            var8 = var11 / 2.0F;
        }
        float var15 = (mc.player.tickCount + this.noiseSeed & 1L) == 0L ? 1.0F : -1.0F;
        var7 *= 1.0F + var15 * 0.3F;
        var8 = Math.max(var8, Math.max(2.0F, 0.5F * var3));
        var8 = Math.min(var8, var1.isGliding() ? 22.0F : 13.0F);
        if (((LivingEntityMixin_1248) (Object) mc.player).getAttackStrengthTicker() <= 1) {
            var7 = Math.min(var7, 18.0F);
        }
        var2.setSpeeds(var7, var8);
    }

    private static float noise(long var0) {
        long var2 = var0 * -7046029254386353131L;
        var2 ^= var2 >>> 29;
        var2 *= -4658895280553007687L;
        var2 ^= var2 >>> 32;
        return (float) (var2 >>> 40) / 1.6777216E7F;
    }

    private void applyInstantTurn(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        double var3 = Math.max(1.0, var1.aura().distanceToTarget(var1.target()));
        AABB bounds = var1.targetBox();
        float var6 = (float) Math.toDegrees(Math.atan(bounds.getXsize() * 0.5 / var3));
        float var7 = (float) Math.toDegrees(Math.atan(bounds.getYsize() * 0.5 / var3));
        if (!(var6 <= 0.0F) || !(var7 <= 0.0F)) {
            float var8 = RotationMath.gcd();
            float var9 = Mth.clamp(Math.max(1.8F, var6 * 0.35F), 0.0F, var6 * 0.85F);
            float var10 = Mth.clamp(Math.max(3.2F * var8, var7 * 0.3F), 0.0F, Math.min(var7 * 0.85F, 4.5F));
            float var11 = Math.min(var6 * 0.2F, 1.5F);
            long var12 = mc.player.tickCount + this.noiseSeed;
            float var14 = (var12 & 1L) == 0L ? 1.0F : -1.0F;
            float var15 = (float) ((var12 % 4L + 4L) % 4L) / 4.0F;
            float var16 = 4.0F * Math.abs(var15 - 0.5F) - 1.0F;
            float var17 = (noise(var12) - 0.5F) * 2.0F * var9 * 1.2F;
            float var18 = var9 * var14 + var17 + var11 * (float) Math.sin((float) var12 * 0.37F);
            float var19 = var10 * var16;
            this.applyStep(var1, var2, var18, var19, var3);
        }
    }

    private boolean isLookingAtTarget(AuraRotationController.RotationContext var1) {
        return RaycastUtils.isLookingAt(var1.currentYaw(), var1.currentPitch(), var1.aura().effectiveAttackRange(), var1.target(), false, var1.targetBox());
    }

    private void applyStep(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2, float var3, float var4, double var5) {
        float var7 = var2.getYaw();
        float var8 = var2.getPitch();
        double var9 = var5 + 2.0;
        for (float var11 = 1.0F; var11 >= 0.25F; var11 -= 0.375F) {
            float var12 = var7 + var3 * var11;
            float var13 = Mth.clamp(var8 + var4 * var11, -89.9F, 89.9F);
            if (RaycastUtils.isLookingAt(var12, var13, var9, var1.target(), false, var1.targetBox())) {
                var2.setYaw(var12);
                var2.setPitch(var13);
                return;
            }
        }
        for (float var14 = 1.0F; var14 >= 0.3F; var14 -= 0.35F) {
            float var15 = Mth.clamp(var8 + var4 * var14, -89.9F, 89.9F);
            if (RaycastUtils.isLookingAt(var7, var15, var9, var1.target(), false, var1.targetBox())) {
                var2.setPitch(var15);
                return;
            }
        }
    }

    @Override
    public String name() {
        return "Matrix";
    }

    private boolean isInRange(AuraRotationController.RotationContext var1) {
        AttackAura attackAura = var1.aura();
        return attackAura.distanceToTarget(var1.target()) <= attackAura.effectiveTargetRange() + 1.0F;
    }
}
