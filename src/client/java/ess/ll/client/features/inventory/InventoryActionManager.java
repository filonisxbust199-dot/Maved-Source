package ess.ll.client.features.inventory;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PostTickEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.network.PreMovementPacketEvent;
import ess.ll.client.util.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.core.NonNullList;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ess.ll.client.mixin.ClientboundContainerSetContentPacketMixin;
import ess.ll.client.mixin.ClientboundContainerSetSlotPacketMixin;

public enum InventoryActionManager implements MinecraftAccess {
    INSTANCE;

    private final ConcurrentLinkedDeque<InventoryActionSequence> sequences = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Runnable> afterTickActions = new ConcurrentLinkedDeque<>();
    private InventoryActionSequence activeSequence;
    private int movementPauseTicks;
    private int stepDelayTicks;
    private boolean waitingForInputReset;
    private ItemStack[] inventorySnapshot;
    private long[] snapshotSlotDeadlines;
    private long snapshotProtectionDeadline;
    private int snapshotContainerId;
    private ItemStack[] lockedSlotContents;
    private long[] lockedSlotDeadlines;
    private long slotLockDeadline;
    private int lockedContainerId;
    private ItemStack[] deferredSlotContents;
    private ItemStack[] protectedSlotContents;
    private int deferredContainerId;
    private volatile Connection inventoryConnection;
    private volatile boolean hasServerStateId;
    private volatile int contentUpdateCount;
    private InventoryActionSequence confirmingSequence;
    private InventoryActionSequence resyncSequence;
    private InventoryActionSequence beforeMovementSequence;
    private ItemStack[] confirmationBeforeSlots;
    private ItemStack[] confirmationAfterSlots;
    private ItemStack confirmationBeforeCarried;
    private ItemStack confirmationAfterCarried;
    private int originalStateId;
    private int confirmationStateId;
    private int confirmationUpdateCount;
    private int confirmationRetries;
    private long confirmationDeadline;

    public void register() {
        EventBus.register(this);
    }

    public InventoryActionSequence enqueueClick(Runnable runnable) {
        return this.enqueueClick(runnable, 1, true);
    }

    public InventoryActionSequence enqueueClick(Runnable runnable, int var2, boolean var3) {
        InventoryActionSequence inventoryActionSequence = new InventoryActionSequence();
        this.sequences.add(inventoryActionSequence);
        return inventoryActionSequence.thenClick(runnable, var2, var3);
    }

    public InventoryActionSequence enqueuePriorityClick(Runnable runnable, int var2, boolean var3) {
        InventoryActionSequence inventoryActionSequence = new InventoryActionSequence();
        this.sequences.offerFirst(inventoryActionSequence);
        return inventoryActionSequence.thenClick(runnable, var2, var3);
    }

    public InventoryActionSequence interruptWithClick(Runnable runnable, int var2, boolean var3) {
        InventoryActionSequence inventoryActionSequence = new InventoryActionSequence();
        inventoryActionSequence.thenClick(runnable, var2, var3);
        this.interruptWith(inventoryActionSequence);
        return inventoryActionSequence;
    }

    private void interruptWith(InventoryActionSequence inventoryActionSequence) {
        if (this.activeSequence != null && (this.activeSequence.hasNext() || this.activeSequence == this.confirmingSequence)) {
            if (!this.activeSequence.skipsSnapshotProtection()) {
                this.restoreInventorySnapshot();
            }
            if (this.activeSequence.hasNext()) {
                this.sequences.offerFirst(this.activeSequence);
            }
            this.activeSequence = null;
        }
        this.movementPauseTicks = 0;
        this.stepDelayTicks = 0;
        this.waitingForInputReset = false;
        this.inventorySnapshot = null;
        this.sequences.offerFirst(inventoryActionSequence);
    }

    public InventoryActionSequence enqueueUse(Runnable runnable) {
        return this.enqueueUse(runnable, 1, false);
    }

    public InventoryActionSequence enqueueUse(Runnable runnable, int var2, boolean var3) {
        InventoryActionSequence inventoryActionSequence = new InventoryActionSequence();
        this.sequences.add(inventoryActionSequence);
        return inventoryActionSequence.thenUse(runnable, var2, var3);
    }

    public boolean isBusy() {
        return this.activeSequence != null || this.confirmationBeforeSlots != null || !this.sequences.isEmpty() || !this.afterTickActions.isEmpty();
    }

    public void afterTick(Runnable runnable) {
        this.afterTickActions.add(runnable);
    }

    public void reset() {
        this.sequences.clear();
        this.afterTickActions.clear();
        this.activeSequence = null;
        this.movementPauseTicks = 0;
        this.stepDelayTicks = 0;
        this.waitingForInputReset = false;
        this.beforeMovementSequence = null;
        this.inventorySnapshot = null;
        this.snapshotSlotDeadlines = null;
        this.snapshotProtectionDeadline = 0L;
        this.lockedSlotContents = null;
        this.lockedSlotDeadlines = null;
        this.slotLockDeadline = 0L;
        this.deferredSlotContents = null;
        this.protectedSlotContents = null;
        this.clearServerConfirmation();
    }

    @Subscribe(4)
    private void onTick(TickEvent event) {
        if (mc.player != null && mc.level != null) {
            this.applyDeferredUpdates();
            boolean var2 = this.movementPauseTicks > 0;
            if (this.movementPauseTicks > 0) {
                this.movementPauseTicks--;
            }
            this.beforeMovementSequence = null;
            if (this.confirmationBeforeSlots != null) {
                this.checkServerConfirmation();
            }
            if (this.waitingForInputReset) {
                this.waitingForInputReset = false;
                this.executeStep();
            } else {
                if (this.activeSequence == null) {
                    this.activeSequence = this.sequences.poll();
                    if (this.activeSequence == null) {
                        return;
                    }
                    if (!this.activeSequence.skipsSnapshotProtection()) {
                        this.captureInventorySnapshot();
                    }
                }
                if (this.activeSequence != this.confirmingSequence) {
                    if (this.stepDelayTicks > 0) {
                        this.stepDelayTicks--;
                        if (this.stepDelayTicks > 0) {
                            return;
                        }
                    }
                    int var3 = this.activeSequence.currentPriority();
                    if (var3 > 0 && !var2 && !this.activeSequence.isUseStep()) {
                        this.movementPauseTicks = var3;
                        if (this.activeSequence.runsBeforeMovementPacket()) {
                            this.beforeMovementSequence = this.activeSequence;
                        }
                    } else {
                        if (var3 > this.movementPauseTicks) {
                            this.movementPauseTicks = var3;
                        }
                        if (this.activeSequence.isUseStep()) {
                            this.executeStep();
                        } else {
                            this.waitingForInputReset = true;
                        }
                    }
                }
            }
        } else {
            this.reset();
        }
    }

    @Subscribe(4)
    private void onPostTick(PostTickEvent event) {
        Runnable runnable;
        while ((runnable = this.afterTickActions.poll()) != null) {
            runnable.run();
        }
    }

    @Subscribe(4)
    private void onPreMovementPacket(PreMovementPacketEvent event) {
        InventoryActionSequence inventoryActionSequence = this.beforeMovementSequence;
        if (inventoryActionSequence != null) {
            this.beforeMovementSequence = null;
            if (inventoryActionSequence == this.activeSequence && mc.player != null) {
                this.executeStep();
            }
        }
    }

    @Subscribe(4)
    private void onPlayerInput(PlayerInputEvent event) {
        if (this.movementPauseTicks > 0) {
            event.reset();
            if (this.waitingForInputReset) {
                this.waitingForInputReset = false;
                this.executeStep();
            }
        }
    }

    @Subscribe(4)
    private void onPacket(PacketEvent event) {
        if (event.isReceive()) {
            this.trackServerInventoryState(event);
            if (mc.player != null) {
                boolean var2 = this.activeSequence != null && !this.activeSequence.skipsSnapshotProtection() && this.inventorySnapshot != null;
                if (var2 || this.deferredSlotContents != null || this.hasActiveSnapshotProtection() || this.hasActiveSlotLocks()) {
                    int containerId2 = mc.player.containerMenu.containerId;
                    long var4 = System.currentTimeMillis();
                    NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
                    if (event.getPacket() instanceof ClientboundContainerSetSlotPacket var7) {
                        if (var7.getContainerId() != containerId2) {
                            return;
                        }
                        ItemStack stack = this.protectedSlotUpdate(var7.getSlot(), var7.getItem(), var2, var4, containerId2, slots2, false);
                        if (stack == null) {
                            return;
                        }
                        ((ClientboundContainerSetSlotPacketMixin) (Object) var7).setItem(stack);
                    } else if (event.getPacket() instanceof ClientboundContainerSetContentPacket var8) {
                        if (var8.containerId() != containerId2) {
                            return;
                        }
                        var var16 = var8.items();
                        var var10 = new ArrayList(var16.size());
                        int var11 = 0;
                        for (int var12 = 0; var12 < var16.size(); var12++) {
                            ItemStack stack2 = this.protectedSlotUpdate(var12, (ItemStack) var16.get(var12), var2, var4, containerId2, slots2, true);
                            if (stack2 == null) {
                                var10.add((ItemStack) var16.get(var12));
                            } else {
                                var10.add(stack2);
                                var11++;
                            }
                        }
                        if (var11 == 0) {
                            return;
                        }
                        ((ClientboundContainerSetContentPacketMixin) (Object) var8).setItems(var10);
                    }
                }
            }
        }
    }

    private void trackServerInventoryState(PacketEvent event) {
        int var2;
        if (event.getPacket() instanceof ClientboundContainerSetContentPacket var4 && var4.containerId() == 0) {
            this.contentUpdateCount++;
            var2 = var4.stateId();
        } else {
            if (!(event.getPacket() instanceof ClientboundContainerSetSlotPacket var3) || var3.getContainerId() != 0) {
                return;
            }
            var2 = var3.getStateId();
        }
        this.inventoryConnection = event.getConnection();
        this.hasServerStateId = var2 != 0;
    }

    private ItemStack protectedSlotUpdate(int var1, ItemStack stack, boolean var3, long var4, int var6, List<Slot> items, boolean var8) {
        if (var1 < 0) {
            return null;
        }
        ItemStack stack2 = null;
        boolean var10 = false;
        if (this.inventorySnapshot != null && var6 == this.snapshotContainerId && var1 < this.inventorySnapshot.length && this.shouldProtectSnapshotSlot(var1, stack, var3, var4, items)) {
            stack2 = this.inventorySnapshot[var1];
        } else if (var8 && this.lockedSlotContents != null && var6 == this.lockedContainerId && var1 < this.lockedSlotContents.length && this.shouldProtectLockedSlot(var1, stack, var4, items)) {
            stack2 = this.lockedSlotContents[var1];
            var10 = true;
        }
        if (stack2 != null && false) {
            ;
        }
        this.rememberDeferredUpdate(var1, !var3 && !var10 && stack2 != null && !ItemStack.matches(stack, stack2) ? stack.copy() : null, stack2, var6, items);
        return stack2 == null ? null : stack2.copy();
    }

    private void rememberDeferredUpdate(int var1, ItemStack stack, ItemStack stack2, int var4, List<Slot> items) {
        if (stack == null) {
            if (this.deferredSlotContents != null && this.deferredContainerId == var4 && var1 < this.deferredSlotContents.length) {
                this.deferredSlotContents[var1] = null;
            }
        } else {
            if (this.deferredSlotContents == null || this.deferredSlotContents.length != items.size() || this.deferredContainerId != var4) {
                this.deferredSlotContents = new ItemStack[items.size()];
                this.protectedSlotContents = new ItemStack[items.size()];
                this.deferredContainerId = var4;
            }
            this.deferredSlotContents[var1] = stack;
            this.protectedSlotContents[var1] = stack2;
        }
    }

    private void applyDeferredUpdates() {
        if (this.deferredSlotContents != null) {
            if (this.activeSequence == null || this.activeSequence.skipsSnapshotProtection()) {
                int containerId2 = mc.player.containerMenu.containerId;
                if (containerId2 != this.deferredContainerId) {
                    this.deferredSlotContents = null;
                    this.protectedSlotContents = null;
                } else {
                    NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
                    long var3 = System.currentTimeMillis();
                    boolean var5 = false;
                    for (int var6 = 0; var6 < this.deferredSlotContents.length && var6 < slots2.size(); var6++) {
                        ItemStack stack = this.deferredSlotContents[var6];
                        if (stack != null) {
                            if (!this.isSlotProtectionExpired(var6, var3, containerId2)) {
                                var5 = true;
                            } else {
                                this.deferredSlotContents[var6] = null;
                                if (ItemStack.matches(((Slot) slots2.get(var6)).getItem(), this.protectedSlotContents[var6])) {
                                    ((Slot) slots2.get(var6)).set(stack);
                                }
                            }
                        }
                    }
                    if (!var5) {
                        this.deferredSlotContents = null;
                        this.protectedSlotContents = null;
                    }
                }
            }
        }
    }

    private boolean isSlotProtectionExpired(int var1, long var2, int var4) {
        return this.inventorySnapshot != null && this.snapshotContainerId == var4 && this.snapshotSlotDeadlines != null && var1 < this.snapshotSlotDeadlines.length && var2 <= this.snapshotSlotDeadlines[var1] ? false : this.lockedSlotContents == null || this.lockedContainerId != var4 || this.lockedSlotDeadlines == null || var1 >= this.lockedSlotDeadlines.length || var2 > this.lockedSlotDeadlines[var1];
    }

    private boolean hasActiveSnapshotProtection() {
        return this.inventorySnapshot != null && this.snapshotSlotDeadlines != null && System.currentTimeMillis() <= this.snapshotProtectionDeadline;
    }

    private boolean hasActiveSlotLocks() {
        return this.lockedSlotContents != null && System.currentTimeMillis() <= this.slotLockDeadline;
    }

    private boolean shouldProtectSnapshotSlot(int var1, ItemStack stack, boolean var3, long var4, List<Slot> items) {
        if (this.snapshotSlotDeadlines == null || var1 >= items.size() || var4 > this.snapshotSlotDeadlines[var1]) {
            return false;
        } else if (var3) {
            return true;
        } else if (!ItemStack.matches(this.inventorySnapshot[var1], ((Slot) items.get(var1)).getItem())) {
            this.snapshotSlotDeadlines[var1] = 0L;
            return false;
        } else if (ItemStack.isSameItem(stack, this.inventorySnapshot[var1])) {
            this.snapshotSlotDeadlines[var1] = 0L;
            return false;
        } else if (stack.isEmpty() && this.inventorySnapshot[var1].getCount() == 1 && !this.isHandSlot(var1)) {
            this.snapshotSlotDeadlines[var1] = 0L;
            return false;
        } else {
            return true;
        }
    }

    private boolean shouldProtectLockedSlot(int var1, ItemStack stack, long var3, List<Slot> items) {
        ItemStack stack2 = this.lockedSlotContents[var1];
        if (stack2 == null || var1 >= items.size() || var3 > this.lockedSlotDeadlines[var1]) {
            return false;
        } else if (!ItemStack.matches(stack2, ((Slot) items.get(var1)).getItem())) {
            this.lockedSlotDeadlines[var1] = 0L;
            return false;
        } else if (ItemStack.isSameItem(stack, stack2)) {
            this.lockedSlotDeadlines[var1] = 0L;
            return false;
        } else {
            return true;
        }
    }

    private void lockSequenceSlots(InventoryActionSequence inventoryActionSequence) {
        int[] var2 = inventoryActionSequence.getLockedSlots();
        if (var2 != null && mc.player != null) {
            NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
            int containerId2 = mc.player.containerMenu.containerId;
            if (this.lockedSlotContents == null || this.lockedSlotContents.length != slots2.size() || this.lockedContainerId != containerId2) {
                this.lockedSlotContents = new ItemStack[slots2.size()];
                this.lockedSlotDeadlines = new long[slots2.size()];
                this.lockedContainerId = containerId2;
            }
            long var5 = System.currentTimeMillis() + this.protectionDurationMillis();
            for (int var10 : var2) {
                if (var10 >= 0 && var10 < slots2.size()) {
                    this.lockedSlotContents[var10] = ((Slot) slots2.get(var10)).getItem().copy();
                    this.lockedSlotDeadlines[var10] = var5;
                    if (this.snapshotSlotDeadlines != null && var10 < this.snapshotSlotDeadlines.length) {
                        this.snapshotSlotDeadlines[var10] = 0L;
                    }
                }
            }
            if (var5 > this.slotLockDeadline) {
                this.slotLockDeadline = var5;
            }
        }
    }

    private boolean isHandSlot(int var1) {
        if (mc.player == null) {
            return false;
        } else {
            return var1 == 45 ? true : var1 == 36 + mc.player.getInventory().getSelectedSlot();
        }
    }

    private static ItemStack[] copySlotContents(List<Slot> items) {
        ItemStack[] stack = new ItemStack[items.size()];
        for (int var2 = 0; var2 < stack.length; var2++) {
            stack[var2] = ((Slot) items.get(var2)).getItem().copy();
        }
        return stack;
    }

    private void captureInventorySnapshot() {
        if (mc.player == null) {
            this.inventorySnapshot = null;
        } else {
            ItemStack[] stack = copySlotContents(mc.player.containerMenu.slots);
            this.inventorySnapshot = stack;
            this.snapshotContainerId = mc.player.containerMenu.containerId;
            if (this.snapshotSlotDeadlines == null || this.snapshotSlotDeadlines.length != stack.length) {
                this.snapshotSlotDeadlines = new long[stack.length];
            }
        }
    }

    private void restoreInventorySnapshot() {
        if (this.inventorySnapshot != null && mc.player != null && mc.player.containerMenu.containerId == this.snapshotContainerId) {
            NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
            long var2 = System.currentTimeMillis() + this.protectionDurationMillis();
            for (int var4 = 0; var4 < this.inventorySnapshot.length && var4 < slots2.size(); var4++) {
                if (!ItemStack.matches(this.inventorySnapshot[var4], ((Slot) slots2.get(var4)).getItem())) {
                    ((Slot) slots2.get(var4)).set(this.inventorySnapshot[var4].copy());
                    this.snapshotSlotDeadlines[var4] = var2;
                    if (var2 > this.snapshotProtectionDeadline) {
                        this.snapshotProtectionDeadline = var2;
                    }
                }
            }
        }
    }

    public long protectionDurationMillis() {
        return 1500L;
    }

    private void executeStep() {
        boolean var1 = this.activeSequence.requiresServerConfirmation() && this.confirmationBeforeSlots == null && this.canRequestServerConfirmation();
        if (var1) {
            this.beginServerConfirmation();
        }
        this.stepDelayTicks = this.activeSequence.executeNext();
        if (this.stepDelayTicks > 0 && this.activeSequence.shouldStopDuringDelay() && this.stepDelayTicks > this.movementPauseTicks) {
            this.movementPauseTicks = this.stepDelayTicks;
        }
        if (!this.activeSequence.skipsSnapshotProtection()) {
            this.restoreInventorySnapshot();
        }
        if (var1) {
            this.capturePredictedChanges();
        }
        if (!this.activeSequence.hasNext() && this.confirmingSequence != this.activeSequence) {
            this.finishSequence();
        }
    }

    private void finishSequence() {
        this.lockSequenceSlots(this.activeSequence);
        this.activeSequence.markStarted();
        this.activeSequence = null;
    }

    private boolean canRequestServerConfirmation() {
        return this.hasServerStateId && mc.player.containerMenu == mc.player.inventoryMenu && mc.getConnection() != null && mc.getConnection().getConnection() == this.inventoryConnection;
    }

    private void beginServerConfirmation() {
        InventoryMenu inventoryMenu2 = mc.player.inventoryMenu;
        this.confirmingSequence = this.activeSequence;
        this.confirmationBeforeSlots = copySlotContents(inventoryMenu2.slots);
        this.confirmationBeforeCarried = inventoryMenu2.getCarried().copy();
        this.originalStateId = inventoryMenu2.getStateId();
        this.confirmationStateId = this.originalStateId + 16384 & 32767;
        inventoryMenu2.stateId = this.confirmationStateId;
        this.confirmationUpdateCount = this.contentUpdateCount;
        this.confirmationRetries = 0;
    }

    private void capturePredictedChanges() {
        InventoryMenu inventoryMenu2 = mc.player.inventoryMenu;
        this.confirmationAfterSlots = copySlotContents(inventoryMenu2.slots);
        this.confirmationAfterCarried = inventoryMenu2.getCarried().copy();
        if (!this.hasPredictedChanges()) {
            if (inventoryMenu2.getStateId() == this.confirmationStateId) {
                inventoryMenu2.stateId = this.originalStateId;
            }
            this.clearServerConfirmation();
        } else {
            this.confirmationDeadline = System.currentTimeMillis() + 800L;
        }
    }

    private boolean hasPredictedChanges() {
        if (!ItemStack.matches(this.confirmationBeforeCarried, this.confirmationAfterCarried)) {
            return true;
        }
        for (int var1 = 0; var1 < this.confirmationBeforeSlots.length && var1 < this.confirmationAfterSlots.length; var1++) {
            if (!ItemStack.matches(this.confirmationBeforeSlots[var1], this.confirmationAfterSlots[var1])) {
                return true;
            }
        }
        return false;
    }

    private void checkServerConfirmation() {
        boolean var1 = this.contentUpdateCount != this.confirmationUpdateCount;
        if (var1 || System.currentTimeMillis() > this.confirmationDeadline) {
            if (!var1 && this.confirmationRetries < 2) {
                this.retryServerConfirmation();
            } else {
                InventoryActionSequence inventoryActionSequence = this.confirmingSequence;
                if (!var1) {
                    this.rollbackUnconfirmedChanges();
                }
                this.clearServerConfirmation();
                if (!var1 || !inventoryActionSequence.hasNext()) {
                    if (inventoryActionSequence != this.activeSequence) {
                        this.sequences.remove(inventoryActionSequence);
                        inventoryActionSequence.markStarted();
                    } else if (var1) {
                        this.finishSequence();
                    } else {
                        inventoryActionSequence.markStarted();
                        this.activeSequence = null;
                        this.stepDelayTicks = 0;
                    }
                }
            }
        }
    }

    private void retryServerConfirmation() {
        this.confirmationRetries++;
        this.confirmationDeadline = Long.MAX_VALUE;
        InventoryActionSequence inventoryActionSequence = this.confirmingSequence;
        if (inventoryActionSequence == this.activeSequence) {
            this.activeSequence = null;
            this.stepDelayTicks = 0;
            if (inventoryActionSequence.hasNext()) {
                this.sequences.offerFirst(inventoryActionSequence);
            }
        }
        this.resyncSequence = new InventoryActionSequence().thenClick(this::requestInventoryResync, 1, false);
        this.sequences.offerFirst(this.resyncSequence);
    }

    private void requestInventoryResync() {
        if (this.confirmationBeforeSlots != null && mc.player != null) {
            InventoryMenu inventoryMenu2 = mc.player.inventoryMenu;
            if (mc.player.containerMenu == inventoryMenu2 && inventoryMenu2.getCarried().isEmpty()) {
                inventoryMenu2.stateId = this.confirmationStateId;
                mc.gameMode.handleContainerInput(inventoryMenu2.containerId, -999, 0, ContainerInput.PICKUP, mc.player);
                PlayerUtils.sendSilently(new ServerboundContainerClosePacket(inventoryMenu2.containerId));
            }
            this.confirmationDeadline = System.currentTimeMillis() + 800L;
        }
    }

    private void rollbackUnconfirmedChanges() {
        InventoryMenu inventoryMenu2 = mc.player.inventoryMenu;
        NonNullList<Slot> slots2 = inventoryMenu2.slots;
        for (int var3 = 0; var3 < this.confirmationBeforeSlots.length && var3 < this.confirmationAfterSlots.length && var3 < slots2.size(); var3++) {
            if (!ItemStack.matches(this.confirmationBeforeSlots[var3], this.confirmationAfterSlots[var3])) {
                boolean var4 = ItemStack.matches(((Slot) slots2.get(var3)).getItem(), this.confirmationAfterSlots[var3]);
                if (var4) {
                    ((Slot) slots2.get(var3)).set(this.confirmationBeforeSlots[var3].copy());
                }
            }
        }
        if (!ItemStack.matches(this.confirmationBeforeCarried, this.confirmationAfterCarried) && ItemStack.matches(inventoryMenu2.getCarried(), this.confirmationAfterCarried)) {
            inventoryMenu2.setCarried(this.confirmationBeforeCarried.copy());
        }
        if (inventoryMenu2.getStateId() == this.confirmationStateId) {
            inventoryMenu2.stateId = this.originalStateId;
        }
    }

    private void clearServerConfirmation() {
        if (this.resyncSequence != null) {
            this.sequences.remove(this.resyncSequence);
            if (this.activeSequence == this.resyncSequence) {
                this.activeSequence = null;
                this.stepDelayTicks = 0;
                this.waitingForInputReset = false;
            }
            this.resyncSequence = null;
        }
        this.confirmingSequence = null;
        this.confirmationBeforeSlots = null;
        this.confirmationAfterSlots = null;
        this.confirmationBeforeCarried = null;
        this.confirmationAfterCarried = null;
    }
}
