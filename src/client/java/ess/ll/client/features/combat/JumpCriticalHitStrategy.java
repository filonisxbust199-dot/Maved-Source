package ess.ll.client.features.combat;

import ess.ll.client.core.SimulatedPlayerInput;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.TickEvent;
import ess.ll.client.features.world.WorldCollisionUtils;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.util.ModuleManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Input;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public final class JumpCriticalHitStrategy implements CriticalHitStrategy, MinecraftAccess {
    private boolean suppressJump;
    private int delayedJumpTicks;
    private int readyAttackTicks;
    private int predictionTick;
    private int jumpCriticalTick;
    private int fallCriticalTick;
    private static final int INITIAL_TICK = Integer.MIN_VALUE;
    private static final int RESET_TICK = Integer.MIN_VALUE;

    @Override
    public boolean isApplicable(AttackAura attackAura) {
        return attackAura.shouldAccelerateCriticals() || ModuleManager.INSTANCE.criticals().isSlowFallingCriticalActive();
    }

    @Override
    public boolean shouldAllowFallbackAttack(AttackAura attackAura) {
        if (this.canSimulateCritical(attackAura) && attackAura.hasNearbyCollision()) {
            this.updatePredictions();
            int var2 = this.preferredCriticalTick();
            if (var2 < 0) {
                return true;
            }
            if (attackAura.ticksUntilAttack() > 0) {
                return false;
            }
            int var3 = maximumAttackWait();
            return var2 > var3 || this.readyAttackTicks > var3;
        } else {
            return false;
        }
    }

    @Override
    public void update(AttackAura attackAura) {
        if (!this.canSimulateCritical(attackAura)) {
            this.reset();
        } else {
            this.readyAttackTicks = attackAura.ticksUntilAttack() > 0 ? 0 : this.readyAttackTicks + 1;
            this.updatePredictions();
            boolean var2 = this.fallCriticalTick < 0 && (this.jumpCriticalTick < 0 || attackAura.ticksUntilAttack() <= this.jumpCriticalTick) && !this.isJumpOnCooldown();
            this.delayedJumpTicks = var2 ? 0 : this.delayedJumpTicks + 1;
            this.suppressJump = !var2 && this.delayedJumpTicks <= 20;
        }
    }

    @Override
    public int ticksUntilCritical(AttackAura attackAura) {
        if (!this.canSimulateCritical(attackAura)) {
            return -1;
        }
        this.updatePredictions();
        int var2 = this.preferredCriticalTick();
        return var2 < 0 ? -1 : Math.max(var2, attackAura.ticksUntilAttack());
    }

    private static Input withJump(Input input, boolean var1) {
        return new Input(input.forward(), input.backward(), input.left(), input.right(), var1, input.shift(), input.sprint());
    }

    private boolean isJumpOnCooldown() {
        return mc.player.onGround() && mc.player.getFluidHeight(FluidTags.WATER) <= mc.player.getFluidJumpThreshold() && ((LivingEntityMixin_1248) (Object) mc.player).getJumpDelay() > 1;
    }

    @Override
    public void reset() {
        this.predictionTick = RESET_TICK;
        this.jumpCriticalTick = -1;
        this.fallCriticalTick = -1;
        this.suppressJump = false;
        this.delayedJumpTicks = 0;
        this.readyAttackTicks = 0;
    }

    @Override
    public void modifyInput(PlayerInputEvent event) {
        if (this.suppressJump && mc.options.keyJump.isDown()) {
            event.setJump(false);
        }
    }

    private static int maximumAttackWait() {
        float currentItemAttackStrengthDelay = mc.player.getCurrentItemAttackStrengthDelay();
        if (Float.isFinite(currentItemAttackStrengthDelay) && !(currentItemAttackStrengthDelay <= 0.0F)) {
            float var1 = ModuleManager.INSTANCE.criticals().isSlowFallingCriticalActive() ? 1.0F : 0.5F;
            return Math.max(2, (int) (currentItemAttackStrengthDelay * var1));
        } else {
            return 5;
        }
    }

    private static boolean shouldContinueJumping(SimulatedPlayerInput simulatedPlayerInput) {
        return simulatedPlayerInput.getFluidHeight(FluidTags.WATER) > 0.3 || simulatedPlayerInput.velocity.y < 0.06;
    }

    public JumpCriticalHitStrategy() {
        this.predictionTick = INITIAL_TICK;
        this.jumpCriticalTick = -1;
        this.fallCriticalTick = -1;
    }

    private void updatePredictions() {
        int ticks = TickEvent.getTicks();
        if (this.predictionTick != ticks) {
            this.predictionTick = ticks;
            this.jumpCriticalTick = this.simulateCriticalTick(true, 30);
            this.fallCriticalTick = this.simulateCriticalTick(false, 8);
        }
    }

    private int simulateCriticalTick(boolean var1, int var2) {
        Input keyPresses2 = mc.player.input.keyPresses;
        SimulatedPlayerInput.SimulatedInput simulatedInput = SimulatedPlayerInput.SimulatedInput.fromClientPlayer(withJump(keyPresses2, var1));
        SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.ofClientPlayer(simulatedInput);
        simulatedPlayerInput.jumpDelay = 0;
        for (int var6 = 1; var6 <= var2; var6++) {
            if (var6 > 1) {
                simulatedInput.keys = withJump(keyPresses2, var1 && shouldContinueJumping(simulatedPlayerInput));
            }
            simulatedPlayerInput.tick();
            if (!simulatedPlayerInput.inWater && !simulatedPlayerInput.onGround && !(simulatedPlayerInput.fallDistance <= 0.0F)) {
                double var7 = WorldCollisionUtils.waterSurfaceHeight(simulatedPlayerInput.boundingBox);
                if (Double.isNaN(var7) || simulatedPlayerInput.boundingBox.minY - var7 >= 0.01) {
                    return var6;
                }
            }
        }
        return -1;
    }

    private boolean canSimulateCritical(AttackAura attackAura) {
        LocalPlayer player2 = mc.player;
        if (player2 == null || mc.level == null || player2.input == null) {
            return false;
        } else if (!mc.options.keyJump.isDown() || AttackAura.currentTarget() == null) {
            return false;
        } else if (player2.isPassenger() || player2.getAbilities().flying || player2.isFallFlying()) {
            return false;
        } else {
            return player2.isInWater() && !player2.isUnderWater() && !player2.isInLava() ? Math.abs(attackAura.targetDistance) <= attackAura.effectiveAttackRange() : false;
        }
    }

    private int preferredCriticalTick() {
        return this.fallCriticalTick < 0 ? this.jumpCriticalTick : this.fallCriticalTick;
    }
}
