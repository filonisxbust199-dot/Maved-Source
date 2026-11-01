package ess.ll.client.features.inventory;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public class InventoryActionSequence implements MinecraftAccess {
    private final List<InventoryActionSequence.Step> steps = new ArrayList<>();
    private int stepIndex;
    private int pendingDelay;
    private boolean stopDuringDelay;
    private boolean skipSnapshotProtection = true;
    private boolean requireServerConfirmation;
    private boolean runBeforeMovementPacket;
    private boolean started;
    private int[] lockedSlots;

    public InventoryActionSequence thenClick(Runnable action, int priority, boolean closeContainer) {
        this.steps.add(new InventoryActionSequence.Step(action, this.pendingDelay, priority, closeContainer, false, this.stopDuringDelay));
        this.pendingDelay = 0;
        this.stopDuringDelay = false;
        return this;
    }

    public InventoryActionSequence beforeMovementPacket() {
        this.runBeforeMovementPacket = true;
        return this;
    }

    public InventoryActionSequence thenUse(Runnable action, int priority, boolean closeContainer) {
        this.steps.add(new InventoryActionSequence.Step(action, this.pendingDelay, priority, closeContainer, true, this.stopDuringDelay));
        this.pendingDelay = 0;
        this.stopDuringDelay = false;
        return this;
    }

    void markStarted() {
        this.started = true;
    }

    private void executeStep(InventoryActionSequence.Step step) {
        step.clicks().run();
        if (step.close() && mc.player != null) {
            PlayerUtils.sendSilently(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        }
    }

    public InventoryActionSequence delay(int ticks) {
        return this.delay(ticks, false);
    }

    public int currentPriority() {
        return this.stepIndex < this.steps.size() ? this.steps.get(this.stepIndex).priority() : 0;
    }

    public int[] getLockedSlots() {
        return this.lockedSlots;
    }

    public InventoryActionSequence delay(int ticks, boolean pauseMovement) {
        this.pendingDelay += ticks;
        if (pauseMovement) {
            this.stopDuringDelay = true;
        }
        return this;
    }

    public InventoryActionSequence awaitServerConfirmation() {
        this.requireServerConfirmation = true;
        return this;
    }

    public boolean shouldStopDuringDelay() {
        return this.stepIndex < this.steps.size() && this.steps.get(this.stepIndex).delayStop();
    }

    public InventoryActionSequence delayOneTick() {
        return this.delay(1, false);
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean requiresServerConfirmation() {
        return this.requireServerConfirmation;
    }

    public InventoryActionSequence lockSlots(int... slots) {
        this.lockedSlots = slots;
        return this;
    }

    public boolean hasNext() {
        return this.stepIndex < this.steps.size();
    }

    public InventoryActionSequence thenUse(Runnable action) {
        return this.thenUse(action, 1, false);
    }

    public boolean isUseStep() {
        return this.stepIndex < this.steps.size() && this.steps.get(this.stepIndex).use();
    }

    public boolean runsBeforeMovementPacket() {
        return this.runBeforeMovementPacket;
    }

    public boolean skipsSnapshotProtection() {
        return this.skipSnapshotProtection;
    }

    public int executeNext() {
        if (this.stepIndex >= this.steps.size()) {
            return 0;
        }
        InventoryActionSequence.Step step = this.steps.get(this.stepIndex++);
        this.executeStep(step);
        while (this.stepIndex < this.steps.size() && this.steps.get(this.stepIndex).delay() == 0 && this.steps.get(this.stepIndex).use() == step.use()) {
            step = this.steps.get(this.stepIndex++);
            this.executeStep(step);
        }
        return this.stepIndex < this.steps.size() ? this.steps.get(this.stepIndex).delay() : 0;
    }

    public InventoryActionSequence withSnapshotProtectionSkipped(boolean skip) {
        this.skipSnapshotProtection = skip;
        return this;
    }

    public InventoryActionSequence thenClick(Runnable action) {
        return this.thenClick(action, 1, true);
    }

    private record Step(Runnable clicks, int delay, int priority, boolean close, boolean use, boolean delayStop) {
    }
}
