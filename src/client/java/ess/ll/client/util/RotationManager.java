package ess.ll.client.util;

import ess.ll.client.core.FreeLookController;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.FrameDeltaEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.features.world.BlockPlacementManager;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.integration.RotationRenderHooks;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.network.PostMovementPacketEvent;
import net.minecraft.util.Mth;

public class RotationManager implements MinecraftAccess {
    private static int lastAttackTick = RotationManager.NEVER_ATTACK_TICK;
    private static int yawBudgetTick = RotationManager.NO_YAW_TICK;
    private static float yawUsed;
    private static float lastYawUsed;
    private static int pitchBudgetTick = RotationManager.NO_PITCH_TICK;
    private static float pitchUsed;
    private static RotationManager.State state = RotationManager.State.IDLE;
    private static int priority;
    private static long lastAimMs;
    private static long holdUntilMs;
    private static int holdPriority;
    private static boolean visibleRotation;
    private static float resetYawSpeed;
    private static float resetPitchSpeed;
    private static Rotation resetTarget;
    private static Rotation renderRotation;
    private static Rotation pendingRotation;
    private static Rotation pendingRenderRotation;
    private static float pendingYawSpeed;
    private static float pendingPitchSpeed;
    private static float pendingResetYawSpeed;
    private static float pendingResetPitchSpeed;
    private static int pendingPriority;
    private static boolean pendingVisible;
    private static int pendingAge;
    private static float serverYaw = Float.NaN;
    private static float serverPitch = Float.NaN;
    public static boolean moveCorrectionRequested;
    private static boolean correctingMovement;
    private static int correctionPriority;
    private static RotationManager.ResetCondition resetCondition;
    private static RotationManager.ResetCondition frameResetCondition;
    private static final double NO_DIFFERENCE = Double.MAX_VALUE;
    private static final float MIN_FORWARD = -1.0F;
    private static final float MIN_STRAFE = -1.0F;
    private static final int NEVER_TICK = -1073741824;
    private static final long AIM_HOLD_MS = 120L;
    private static final float MAX_RESET_YAW_SPEED = 180.0F;
    private static final float MAX_RESET_PITCH_SPEED = 180.0F;
    private static final float RESET_YAW_SPEED = 9999.0F;
    private static final float RESET_PITCH_SPEED = 9999.0F;
    private static final float RESET_DONE_YAW = 5.0F;
    private static final float RESET_DONE_PITCH = 5.0F;
    private static final float RESET_YAW_TOLERANCE = 5.0F;
    private static final float RESET_PITCH_TOLERANCE = 5.0F;
    private static final float MIN_RESET_YAW_SPEED = 180.0F;
    private static final float MIN_RESET_PITCH_SPEED = 180.0F;
    private static final float AIM_YAW_TOLERANCE = 1.0E-4F;
    private static final float AIM_PITCH_TOLERANCE = 1.0E-4F;
    private static final float SNAP_YAW_CAP = 19.5F;
    private static final float YAW_CAP = 180.0F;
    private static final float SNAP_THRESHOLD = 20.0F;
    private static final float SNAP_PREVIOUS_THRESHOLD = 20.0F;
    private static final float SNAP_REPEAT_TOLERANCE = 4.0F;
    private static final float SHAKE_PITCH_CAP = 19.7F;
    private static final float MIN_PITCH = -90.0F;
    private static final float MAX_PITCH = 90.0F;
    private static final int NEVER_ATTACK_TICK = -1073741824;
    private static final int NO_YAW_TICK = Integer.MIN_VALUE;
    private static final int NO_PITCH_TICK = Integer.MIN_VALUE;

    public static void releaseHold(int priority) {
        if (priority >= holdPriority) {
            holdUntilMs = 0L;
        }
    }

    private static void updateResetTarget() {
        if (frameResetCondition == null) {
            if (!(resetYawSpeed > MAX_RESET_YAW_SPEED) || !(resetPitchSpeed > MAX_RESET_PITCH_SPEED)) {
                AttackAura attackAura = ModuleManager.INSTANCE.aura();
                if (attackAura != null) {
                    Rotation rotation = attackAura.currentRotation();
                    if (rotation != null) {
                        resetTarget = rotation;
                    }
                }
            }
        }
    }

    private static void finish() {
        state = RotationManager.State.IDLE;
        priority = 0;
        visibleRotation = false;
        moveCorrectionRequested = false;
        correctingMovement = false;
        resetCondition = null;
        frameResetCondition = null;
        resetTarget = null;
        renderRotation = null;
        FreeLookController.setActive(false);
    }

    private static void correctMovement(PlayerInputEvent playerInputEvent) {
        float forward = playerInputEvent.getForward();
        float strafe = playerInputEvent.getStrafe();
        if (forward != 0.0F || strafe != 0.0F) {
            float yRot = mc.player.getYRot();
            double targetYaw = Mth.wrapDegrees(Math.toDegrees(MovementUtils.directionRadians(FreeLookController.getYaw(), forward, strafe)));
            float bestForward = 0.0F;
            float bestStrafe = 0.0F;
            double bestDiff = NO_DIFFERENCE;
            for (float candidateForward = MIN_FORWARD; candidateForward <= 1.0F; candidateForward++) {
                for (float candidateStrafe = MIN_STRAFE; candidateStrafe <= 1.0F; candidateStrafe++) {
                    if (candidateForward != 0.0F || candidateStrafe != 0.0F) {
                        double candidateYaw = Mth.wrapDegrees(Math.toDegrees(MovementUtils.directionRadians(yRot, candidateForward, candidateStrafe)));
                        double diff = Math.abs(Mth.wrapDegrees(targetYaw - candidateYaw));
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            bestForward = candidateForward;
                            bestStrafe = candidateStrafe;
                        }
                    }
                }
            }
            playerInputEvent.setForward(bestForward);
            playerInputEvent.setStrafe(bestStrafe);
        }
    }

    private static float limitPitch(float delta) {
        if (isShakeCapped() && !BlockPlacementManager.canAttackWithinRotationBudget()) {
            int tick = mc.player.tickCount;
            if (tick != pitchBudgetTick) {
                pitchBudgetTick = tick;
                pitchUsed = 0.0F;
            }
            float remaining = SHAKE_PITCH_CAP - pitchUsed;
            if (remaining <= 0.0F) {
                return 0.0F;
            }
            float limited = Mth.clamp(delta, -remaining, remaining);
            pitchUsed = pitchUsed + Math.abs(limited);
            return limited;
        } else {
            return delta;
        }
    }

    private static boolean aimAt(Rotation rotation, float yawSpeed, float pitchSpeed) {
        return turnTowards(rotation, yawSpeed, pitchSpeed, AIM_YAW_TOLERANCE, AIM_PITCH_TOLERANCE);
    }

    public static void clearFrameResetCondition() {
        frameResetCondition = null;
    }

    private static float limitYaw(float delta) {
        int tick = mc.player.tickCount;
        if (tick != yawBudgetTick) {
            yawBudgetTick = tick;
            lastYawUsed = yawUsed;
            yawUsed = 0.0F;
        }
        float cap = isSnap(Math.abs(delta) + yawUsed) ? SNAP_YAW_CAP : YAW_CAP;
        float remaining = cap - yawUsed;
        if (remaining <= 0.0F) {
            return 0.0F;
        }
        float limited = Mth.clamp(delta, -remaining, remaining);
        yawUsed = yawUsed + Math.abs(limited);
        return limited;
    }

    private static boolean isShakeCapped() {
        return attackedWithin(7);
    }

    private static boolean startAim(Rotation rotation, Rotation renderRotation, float yawSpeed, float pitchSpeed, float resetYawSpeed, float resetPitchSpeed, int priority, boolean visible, boolean force) {
        if (state == RotationManager.State.IDLE) {
            correctingMovement = false;
        }
        if (visible) {
            FreeLookController.setActive(false);
        } else if (state == RotationManager.State.IDLE || force || visibleRotation) {
            FreeLookController.setActive(true);
        }
        visibleRotation = visible;
        RotationManager.resetYawSpeed = resetYawSpeed;
        RotationManager.resetPitchSpeed = resetPitchSpeed;
        RotationManager.priority = priority;
        lastAimMs = System.currentTimeMillis();
        state = RotationManager.State.AIM;
        RotationManager.renderRotation = renderRotation;
        return aimAt(rotation, yawSpeed, pitchSpeed);
    }

    public static boolean rotate(Rotation rotation, float speed, int priority, boolean visible) {
        return rotate(rotation, null, speed, priority, visible);
    }

    @Subscribe
    private void onFrameDelta(FrameDeltaEvent frameDeltaEvent) {
        if (frameDeltaEvent.getDelta() == 1.0F) {
            if (state == RotationManager.State.RESET) {
                if (frameResetCondition != null) {
                    if (!hasPending()) {
                        if (frameResetCondition.isDone()) {
                            finish();
                        }
                    }
                }
            }
        }
    }

    private static boolean isSnap(float total) {
        return !isSnapCapped() ? false : total > SNAP_THRESHOLD && lastYawUsed > SNAP_PREVIOUS_THRESHOLD && Math.abs(total - lastYawUsed) < SNAP_REPEAT_TOLERANCE;
    }

    public static void clearResetCondition() {
        resetCondition = null;
    }

    public static boolean rotate(Rotation rotation, Rotation renderRotation, float yawSpeed, float pitchSpeed, int priority, boolean visible) {
        return rotate(rotation, renderRotation, yawSpeed, pitchSpeed, yawSpeed, pitchSpeed, priority, visible);
    }

    public static boolean rotate(Rotation rotation, float yawSpeed, float pitchSpeed, int priority, boolean visible) {
        return rotate(rotation, null, yawSpeed, pitchSpeed, priority, visible);
    }

    @Subscribe(4)
    private void onPlayerInput(PlayerInputEvent playerInputEvent) {
        boolean requested = moveCorrectionRequested;
        moveCorrectionRequested = false;
        if (mc.player != null && state != RotationManager.State.IDLE) {
            if (requested) {
                correctingMovement = true;
                correctionPriority = priority;
            } else if (state == RotationManager.State.AIM && priority != correctionPriority) {
                correctingMovement = false;
            }
            if (correctingMovement) {
                correctMovement(playerInputEvent);
            }
        } else {
            correctingMovement = false;
        }
    }

    public static boolean rotate(Rotation rotation, float yawSpeed, float pitchSpeed, float resetYawSpeed, float resetPitchSpeed, int priority, boolean visible) {
        return rotate(rotation, null, yawSpeed, pitchSpeed, resetYawSpeed, resetPitchSpeed, priority, visible);
    }

    public static void markAttack() {
        if (mc.player != null) {
            lastAttackTick = mc.player.tickCount;
        }
    }

    private static boolean isSnapCapped() {
        return attackedWithin(2);
    }

    public static float getServerPitch() {
        return serverPitch;
    }

    public static int getPriority() {
        return priority;
    }

    private static boolean canReset() {
        return resetYawSpeed <= MIN_RESET_YAW_SPEED || resetPitchSpeed <= MIN_RESET_PITCH_SPEED;
    }

    @Subscribe
    private void onPostMovementPacket(PostMovementPacketEvent postMovementPacketEvent) {
        if (mc.player == null) {
            serverYaw = Float.NaN;
            serverPitch = Float.NaN;
        } else {
            serverYaw = mc.player.getYRot();
            serverPitch = mc.player.getXRot();
        }
    }

    private static boolean hasPending() {
        return pendingRotation != null && pendingPriority > 0 && pendingAge <= 1;
    }

    public static boolean isBusy(int priority) {
        return RotationManager.priority > priority;
    }

    public static float getServerYaw() {
        return serverYaw;
    }

    public RotationManager() {
        EventBus.register(this);
    }

    private static void queue(Rotation rotation, Rotation renderRotation, float yawSpeed, float pitchSpeed, float resetYawSpeed, float resetPitchSpeed, int priority, boolean visible) {
        boolean replace = pendingRotation == null || pendingAge > 1 || priority >= pendingPriority;
        if (replace) {
            pendingRotation = rotation;
            pendingRenderRotation = renderRotation;
            pendingYawSpeed = yawSpeed;
            pendingPitchSpeed = pitchSpeed;
            pendingResetYawSpeed = resetYawSpeed;
            pendingResetPitchSpeed = resetPitchSpeed;
            pendingPriority = priority;
            pendingVisible = visible;
            pendingAge = 0;
        }
    }

    public static void setFrameResetCondition(RotationManager.ResetCondition condition) {
        frameResetCondition = condition;
    }

    public static boolean rotate(Rotation rotation, Rotation renderRotation, float yawSpeed, float pitchSpeed, float resetYawSpeed, float resetPitchSpeed, int priority, boolean visible) {
        if (RotationManager.priority > priority) {
            queue(rotation, renderRotation, yawSpeed, pitchSpeed, resetYawSpeed, resetPitchSpeed, priority, visible);
            return false;
        } else {
            return startAim(rotation, renderRotation, yawSpeed, pitchSpeed, resetYawSpeed, resetPitchSpeed, priority, visible, false);
        }
    }

    private static boolean turnTowards(Rotation rotation, float yawSpeed, float pitchSpeed, float yawTolerance, float pitchTolerance) {
        if (mc.player == null) {
            return false;
        }
        Rotation current = new Rotation(mc.player);
        float deltaYaw = Mth.wrapDegrees(rotation.getYaw() - current.getYaw());
        float deltaPitch = rotation.getPitch() - current.getPitch();
        float total = Math.abs(deltaYaw) + Math.abs(deltaPitch);
        float maxYaw = total == 0.0F ? 0.0F : Math.abs(deltaYaw / total) * yawSpeed;
        float maxPitch = total == 0.0F ? 0.0F : Math.abs(deltaPitch / total) * pitchSpeed;
        float newPitch = Mth.clamp(mc.player.getXRot() + limitPitch(RotationMath.snapToGcd(Mth.clamp(deltaPitch, -maxPitch, maxPitch))), MIN_PITCH, MAX_PITCH);
        float newYaw = mc.player.getYRot() + limitYaw(RotationMath.snapToGcd(Mth.clamp(deltaYaw, -maxYaw, maxYaw)));
        mc.player.setYRot(newYaw);
        mc.player.setXRot(newPitch);
        float yawError = Math.abs(Mth.wrapDegrees(rotation.getYaw() - newYaw));
        float pitchError = Math.abs(rotation.getPitch() - newPitch);
        return yawError <= yawTolerance && pitchError <= pitchTolerance;
    }

    public static void releasePriority(int priority) {
        if (RotationManager.priority == priority) {
            RotationManager.priority = 0;
        }
    }

    public static void setResetCondition(RotationManager.ResetCondition condition) {
        resetCondition = condition;
    }

    public static boolean rotate(Rotation rotation, Rotation renderRotation, float speed, int priority, boolean visible) {
        return rotate(rotation, renderRotation, speed, speed, priority, visible);
    }

    public static void hold(int priority, long ms) {
        if (priority >= RotationManager.priority) {
            holdUntilMs = System.currentTimeMillis() + ms;
            holdPriority = priority;
        }
    }

    private static void updateReset() {
        if (mc.player != null) {
            if (hasPending()) {
                startAim(pendingRotation, pendingRenderRotation, pendingYawSpeed, pendingPitchSpeed, pendingResetYawSpeed, pendingResetPitchSpeed, pendingPriority, pendingVisible, true);
                clearPending();
            } else if (frameResetCondition == null) {
                if (resetCondition != null) {
                    if (resetCondition.isDone()) {
                        finish();
                    }
                } else if (resetTarget != null && canReset()) {
                    aimAt(resetTarget, RESET_YAW_SPEED, RESET_PITCH_SPEED);
                    Rotation freeLook = Rotation.freeLook();
                    Rotation current = new Rotation(mc.player);
                    float yawDiff = Math.abs(Mth.wrapDegrees(freeLook.getYaw() - current.getYaw()));
                    float pitchDiff = Math.abs(freeLook.getPitch() - current.getPitch());
                    if (yawDiff <= RESET_DONE_YAW && pitchDiff <= RESET_DONE_PITCH) {
                        finish();
                    }
                } else {
                    boolean done = turnTowards(Rotation.freeLook(), resetYawSpeed, resetPitchSpeed, RESET_YAW_TOLERANCE, RESET_PITCH_TOLERANCE);
                    if (done) {
                        finish();
                    }
                }
            }
        }
    }

    public static String debugInfo() {
        int ticksSinceAttack = mc.player == null ? -1 : mc.player.tickCount - lastAttackTick;
        return "prio=" + priority + " snapCap=" + isSnapCapped() + "/" + Math.round(lastYawUsed) + " shakeCap=" + isShakeCapped() + " послеУдара=" + ticksSinceAttack + "т task=" + state;
    }

    @Subscribe(4)
    private void onTick(TickEvent tickEvent) {
        RotationRenderHooks.update(renderRotation);
        if (pendingRotation != null) {
            pendingAge++;
        }
        if (state == RotationManager.State.AIM && System.currentTimeMillis() - lastAimMs >= AIM_HOLD_MS && System.currentTimeMillis() >= holdUntilMs) {
            state = RotationManager.State.RESET;
            resetTarget = null;
            renderRotation = null;
        }
        if (state == RotationManager.State.RESET) {
            updateResetTarget();
            updateReset();
        }
    }

    private static boolean attackedWithin(int ticks) {
        if (mc.player == null) {
            return false;
        } else {
            int ticksSince = mc.player.tickCount - lastAttackTick;
            if (ticksSince < 0) {
                lastAttackTick = NEVER_TICK;
                return false;
            } else {
                return ticksSince <= ticks;
            }
        }
    }

    private static void clearPending() {
        pendingRotation = null;
        pendingRenderRotation = null;
        pendingPriority = 0;
        pendingAge = 0;
    }

    private enum State {
        AIM, RESET, IDLE
    }

    @FunctionalInterface
    public interface ResetCondition {
        boolean isDone();
    }
}
