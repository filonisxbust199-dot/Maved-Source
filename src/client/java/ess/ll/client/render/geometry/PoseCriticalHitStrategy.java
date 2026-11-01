package ess.ll.client.render.geometry;

import ess.ll.client.core.SimulatedPlayerInput;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.TickEvent;
import ess.ll.client.features.combat.CriticalHitStrategy;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AttackAura;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Input;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public final class PoseCriticalHitStrategy implements CriticalHitStrategy, MinecraftAccess {
    private boolean suppressJump;
    private int delayedJumpTicks;
    private int predictionTick;
    private PoseCriticalHitStrategy.CriticalWindow jumpWindow;
    private PoseCriticalHitStrategy.CriticalWindow fallWindow;
    private static final int INITIAL_TICK = Integer.MIN_VALUE;
    private static final int RESET_TICK = Integer.MIN_VALUE;

    private boolean isPoseReady() {
        return this.desiredPose() == mc.player.getPose() && ((LivingEntityMixin_1248) (Object) mc.player).getJumpDelay() <= 1;
    }

    @Override
    public boolean shouldAllowFallbackAttack(AttackAura attackAura) {
        return this.canSimulateCritical(attackAura) && !this.hasCriticalWindow();
    }

    @Override
    public boolean isApplicable(AttackAura attackAura) {
        return attackAura.isAttackAccelerationEnabled();
    }

    private boolean shouldUseJumpPrediction(int var1) {
        int var2 = this.jumpWindow.critIn(var1);
        int var3 = this.fallWindow.critIn(var1);
        return var2 < 0 && var3 < 0 ? this.jumpWindow.landsNoLaterThan(this.fallWindow) : var2 >= 0 && (var3 < 0 || var2 <= var3);
    }

    private boolean hasCriticalWindow() {
        int ticks = TickEvent.getTicks();
        if (this.predictionTick != ticks) {
            this.predictionTick = ticks;
            Pose pose = this.desiredPose();
            this.jumpWindow = this.simulateCriticalWindow(pose, true);
            this.fallWindow = this.hasUpwardMotionOnGround() ? this.simulateCriticalWindow(pose, false) : PoseCriticalHitStrategy.CriticalWindow.NONE;
        }
        return this.jumpWindow.exists() || this.fallWindow.exists();
    }

    @Override
    public int ticksUntilCritical(AttackAura attackAura) {
        if (this.canSimulateCritical(attackAura) && this.shouldDelayJump(attackAura)) {
            int var2 = attackAura.ticksUntilAttack();
            if (this.fallWindow.exists()) {
                return this.shouldUseJumpPrediction(var2) ? this.jumpWindow.critIn(var2) : this.fallWindow.critIn(var2);
            } else {
                return this.jumpDelayTicks(attackAura) + this.jumpWindow.start();
            }
        } else {
            return -1;
        }
    }

    private PoseCriticalHitStrategy.CriticalWindow simulateCriticalWindow(Pose pose, boolean var2) {
        Input keyPresses2 = mc.player.input.keyPresses;
        boolean var4 = pose == Pose.CROUCHING;
        SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.ofClientPlayer(SimulatedPlayerInput.SimulatedInput.fromClientPlayer(withJumpAndSneak(keyPresses2, var2, var4)));
        simulatedPlayerInput.setDimensions(mc.player.getDimensions(pose));
        simulatedPlayerInput.jumpDelay = 0;
        int var6 = -1;
        int var7 = -1;
        for (int var8 = 1; var8 <= 20; var8++) {
            simulatedPlayerInput.tick();
            if (var8 == 1) {
                simulatedPlayerInput.input.keys = withJumpAndSneak(keyPresses2, false, var4);
            }
            if (simulatedPlayerInput.onGround) {
                return new PoseCriticalHitStrategy.CriticalWindow(var6, var7, var8);
            }
            if (!simulatedPlayerInput.inWater && simulatedPlayerInput.fallDistance > 0.0F) {
                if (var6 < 0) {
                    var6 = var8;
                }
                var7 = var8;
            } else if (var6 > 0) {
                break;
            }
        }
        return new PoseCriticalHitStrategy.CriticalWindow(var6, var7, -1);
    }

    @Override
    public void update(AttackAura attackAura) {
        if (!this.canSimulateCritical(attackAura)) {
            this.reset();
        } else if (this.shouldDelayJump(attackAura) && this.jumpDelayTicks(attackAura) != 0) {
            this.delayedJumpTicks++;
            this.suppressJump = this.delayedJumpTicks <= 12;
        } else {
            this.clearJumpDelay();
        }
    }

    @Override
    public void reset() {
        this.predictionTick = RESET_TICK;
        this.jumpWindow = PoseCriticalHitStrategy.CriticalWindow.NONE;
        this.fallWindow = PoseCriticalHitStrategy.CriticalWindow.NONE;
        this.clearJumpDelay();
    }

    private Pose desiredPose() {
        Pose pose = mc.player.getPose();
        return pose == Pose.STANDING && mc.options.keyShift.isDown() ? Pose.CROUCHING : pose;
    }

    public PoseCriticalHitStrategy() {
        this.predictionTick = INITIAL_TICK;
        this.jumpWindow = PoseCriticalHitStrategy.CriticalWindow.NONE;
        this.fallWindow = PoseCriticalHitStrategy.CriticalWindow.NONE;
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
            return !player2.isInWater() && !player2.isInLava() ? Math.abs(attackAura.targetDistance) <= attackAura.effectiveAttackRange() : false;
        }
    }

    private int preferredAttackTick() {
        return Math.max(this.jumpWindow.start(), this.jumpWindow.end() - 1);
    }

    private static Input withJumpAndSneak(Input input, boolean var1, boolean var2) {
        return new Input(input.forward(), input.backward(), input.left(), input.right(), var1, var2, input.sprint());
    }

    private boolean shouldDelayJump(AttackAura attackAura) {
        return mc.player.onGround() && !attackAura.canCriticalHitNow() && this.hasCriticalWindow();
    }

    private boolean hasUpwardMotionOnGround() {
        return mc.player.onGround() && mc.player.getDeltaMovement().y > 0.0;
    }

    private int jumpDelayTicks(AttackAura attackAura) {
        int var2 = attackAura.ticksUntilAttack();
        if (this.fallWindow.exists()) {
            return this.shouldUseJumpPrediction(var2) ? 0 : 1;
        }
        int var3 = Math.max(0, var2 - this.preferredAttackTick());
        return var3 == 0 && !this.isPoseReady() ? 1 : var3;
    }

    @Override
    public void modifyInput(PlayerInputEvent event) {
        if (this.suppressJump && mc.options.keyJump.isDown()) {
            event.setJump(false);
        }
    }

    private void clearJumpDelay() {
        this.suppressJump = false;
        this.delayedJumpTicks = 0;
    }

    private record CriticalWindow(int start, int end, int land) {
        private static final PoseCriticalHitStrategy.CriticalWindow NONE = new PoseCriticalHitStrategy.CriticalWindow(-1, -1, -1);

        private boolean exists() {
            return this.start > 0;
        }

        private int critIn(int var1) {
            return this.exists() && this.end >= var1 ? Math.max(this.start, var1) : -1;
        }

        private boolean landsNoLaterThan(PoseCriticalHitStrategy.CriticalWindow criticalWindow) {
            return this.land > 0 && (criticalWindow.land < 0 || this.land <= criticalWindow.land);
        }
    }
}
