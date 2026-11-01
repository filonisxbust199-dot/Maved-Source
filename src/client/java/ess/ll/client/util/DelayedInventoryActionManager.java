package ess.ll.client.util;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.inventory.InventoryActionController;
import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.inventory.InventoryActionSequence;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.integration.MinecraftAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum DelayedInventoryActionManager implements MinecraftAccess, InventoryActionController {
    INSTANCE;

    private static final int ZERO_A = 0;
    private static final int ZERO_B = 0;
    private final ConcurrentLinkedDeque<DelayedInventoryActionManager.QueuedUse> queue = new ConcurrentLinkedDeque<>();
    private int cooldown;

    public void init() {
        EventBus.register(this);
    }

    public void delayAtLeast(int ticks) {
        if (ticks > this.cooldown) {
            this.cooldown = ticks;
        }
    }

    @Subscribe
    @Override
    public void onPlayerUpdate(PlayerUpdateEvent playerUpdateEvent) {
        if (mc.player == null || mc.level == null) {
            this.abort();
            this.clearQueue();
            this.cooldown = 0;
        } else if (this.cooldown > 0) {
            this.cooldown--;
        } else {
            this.processQueue();
        }
    }

    public boolean isIdle() {
        return this.queue.isEmpty() && !InventoryActionManager.INSTANCE.isBusy();
    }

    @Override
    public void useItem(Item item) {
        this.useItem(InteractionHand.MAIN_HAND, item);
    }

    @Override
    public void useSlot(Slot slot) {
        this.useSlot(InteractionHand.MAIN_HAND, slot);
    }

    @Override
    public void useItem(InteractionHand interactionHand, Item item) {
        if (mc.player != null && mc.level != null) {
            this.use(interactionHand, item, Float.MAX_VALUE, Float.MAX_VALUE);
        }
    }

    @Override
    public void useSlot(InteractionHand interactionHand, Slot slot) {
        if (mc.player != null && mc.level != null) {
            this.use(interactionHand, slot, Float.MAX_VALUE, Float.MAX_VALUE);
        }
    }

    @Override
    public void useItem(Item item, float yaw, float pitch) {
        this.use(InteractionHand.MAIN_HAND, item, yaw, pitch);
    }

    @Override
    public void useSlot(Slot slot, float yaw, float pitch) {
        this.use(InteractionHand.MAIN_HAND, slot, yaw, pitch);
    }

    @Override
    public void use(InteractionHand interactionHand, Object object, float yaw, float pitch) {
        if (mc.player != null && mc.level != null && mc.gameMode != null) {
            if (object != null) {
                if (this.queue.size() < 16) {
                    if (this.itemOf(object) != Items.FIREWORK_ROCKET || this.countQueued(Items.FIREWORK_ROCKET) < 2) {
                        this.queue.offerLast(new DelayedInventoryActionManager.QueuedUse(interactionHand, object, yaw, pitch));
                        if (this.cooldown == 0) {
                            this.processQueue();
                        }
                    }
                }
            }
        }
    }

    public void useItemNow(Item item, float yaw, float pitch) {
        if (mc.player != null && mc.gameMode != null) {
            if (!InventoryUtils.isOnCooldown(item)) {
                if (mc.player.getMainHandItem().getItem() == item) {
                    InventoryActionManager.INSTANCE.enqueueUse(() -> InventoryUtils.useHand(InteractionHand.MAIN_HAND, yaw, pitch), 0, false).withSnapshotProtectionSkipped(true);
                } else {
                    int selectedSlot = mc.player.getInventory().getSelectedSlot();
                    int hotbarSlot = InventoryUtils.findItemIndex(item, true);
                    if (hotbarSlot != -1) {
                        InventoryActionManager.INSTANCE.enqueueUse(() -> {
                            if (selectedSlot != hotbarSlot) {
                                mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
                            }
                            InventoryUtils.useHand(InteractionHand.MAIN_HAND, yaw, pitch);
                            if (selectedSlot != hotbarSlot) {
                                mc.player.connection.send(new ServerboundSetCarriedItemPacket(selectedSlot));
                            }
                        }, 0, false);
                    } else {
                        int inventorySlot = InventoryUtils.findItemIndex(item, false);
                        if (inventorySlot == -1) {
                            this.useFromServer(InteractionHand.MAIN_HAND, item, yaw, pitch);
                        } else {
                            int containerId = mc.player.containerMenu.containerId;
                            InventoryActionManager.INSTANCE.enqueueClick(() -> {
                                mc.gameMode.handleContainerInput(containerId, inventorySlot, selectedSlot, ContainerInput.SWAP, mc.player);
                                InventoryUtils.useHand(InteractionHand.MAIN_HAND, yaw, pitch);
                                mc.gameMode.handleContainerInput(containerId, inventorySlot, selectedSlot, ContainerInput.SWAP, mc.player);
                            }, 1, true);
                        }
                    }
                }
            }
        }
    }

    private Item itemOf(Object object) {
        if (object instanceof Item item) {
            return item;
        } else {
            return object instanceof Slot slot ? slot.getItem().getItem() : null;
        }
    }

    private int countQueued(Item item) {
        int count = 0;
        for (DelayedInventoryActionManager.QueuedUse action : this.queue) {
            if (this.itemOf(action.target()) == item) {
                count++;
            }
        }
        return count;
    }

    private void perform(InteractionHand interactionHand, Object object, float yaw, float pitch) {
        if (mc.player != null) {
            Slot slot = null;
            Item item;
            ItemStack stack;
            if (object instanceof Item typedItem) {
                item = typedItem;
                stack = InventoryUtils.findStack(typedItem);
            } else {
                if (!(object instanceof Slot typedSlot)) {
                    return;
                }
                slot = typedSlot;
                stack = typedSlot.getItem();
                if (stack == null || stack.isEmpty()) {
                    return;
                }
                item = stack.getItem();
            }
            if (item != Items.AIR) {
                if (item == Items.FIREWORK_ROCKET || stack.isEmpty() || !mc.player.getCooldowns().isOnCooldown(stack)) {
                    Item heldItem = interactionHand == InteractionHand.OFF_HAND ? mc.player.getOffhandItem().getItem() : mc.player.getMainHandItem().getItem();
                    if (slot != null && slot.container == mc.player.getInventory()) {
                        this.useFromSlot(slot, interactionHand, yaw, pitch);
                    } else if (heldItem == item) {
                        if (InventoryUtils.useHand(interactionHand, yaw, pitch, item != Items.FIREWORK_ROCKET)) {
                            mc.player.swing(interactionHand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                        }
                    } else {
                        boolean offHand = interactionHand == InteractionHand.OFF_HAND;
                        int inventorySlot = InventoryUtils.findItemIndex(item, false);
                        int hotbarSlot = InventoryUtils.findItemIndex(item, true);
                        if (hotbarSlot == -1 && inventorySlot == -1) {
                            this.useFromServer(interactionHand, item, yaw, pitch);
                        } else {
                            int selectedSlot = mc.player.getInventory().getSelectedSlot();
                            int targetSlot = offHand ? 40 : selectedSlot;
                            int containerId = mc.player.containerMenu.containerId;
                            if (hotbarSlot == -1) {
                                this.swapAndUse(containerId, inventorySlot, targetSlot, interactionHand, yaw, pitch);
                            } else if (offHand) {
                                this.swapHotbarAndUse(containerId, hotbarSlot, targetSlot, interactionHand, yaw, pitch);
                            } else {
                                this.selectAndUse(hotbarSlot, selectedSlot, interactionHand, yaw, pitch);
                            }
                        }
                    }
                }
            }
        }
    }

    private void useFromServer(InteractionHand interactionHand, Item item, float yaw, float pitch) {
        boolean handled = ServerItemActionUtils.useFromBundle(item, () -> {
            if (InventoryUtils.findItemIndex(item, true) != -1 || InventoryUtils.findItemIndex(item, false) != -1) {
                this.use(interactionHand, item, yaw, pitch);
            }
        });
        if (!handled) {
            ServerItemActionUtils.useFromBundle(item, interactionHand, yaw, pitch);
        }
    }

    private void useFromSlot(Slot slot, InteractionHand interactionHand, float yaw, float pitch) {
        if (mc.player != null) {
            int containerSlot = slot.getContainerSlot();
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            int targetSlot = interactionHand == InteractionHand.OFF_HAND ? 40 : selectedSlot;
            int containerId = mc.player.containerMenu.containerId;
            if (containerSlot >= 0 && containerSlot < 9) {
                if (interactionHand == InteractionHand.OFF_HAND) {
                    this.swapAndUse(containerId, slot.index, targetSlot, interactionHand, yaw, pitch);
                } else {
                    this.selectAndUse(containerSlot, selectedSlot, interactionHand, yaw, pitch);
                }
            } else {
                if (containerSlot >= 9 && containerSlot < 36) {
                    this.swapAndUse(containerId, slot.index, targetSlot, interactionHand, yaw, pitch);
                }
            }
        }
    }

    private void swapHotbarAndUse(int containerId, int hotbarSlot, int targetSlot, InteractionHand interactionHand, float yaw, float pitch) {
        int menuSlot = hotbarSlot + 36;
        this.swapAndUse(containerId, menuSlot, targetSlot, interactionHand, yaw, pitch);
    }

    private void swapAndUse(int containerId, int slot, int targetSlot, InteractionHand interactionHand, float yaw, float pitch) {
        InventoryActionManager.INSTANCE.enqueueClick(() -> mc.gameMode.handleContainerInput(containerId, slot, targetSlot, ContainerInput.SWAP, mc.player), 1, true).delay(1, false).thenUse(() -> {
            InteractionResult result = InventoryUtils.useHandWithResult(interactionHand, yaw, pitch);
            if (!mc.player.getItemInHand(interactionHand).is(Items.FIREWORK_ROCKET))
                mc.player.swing(interactionHand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        }, 1, false).delay(1, false).thenClick(() -> mc.gameMode.handleContainerInput(containerId, slot, targetSlot, ContainerInput.SWAP, mc.player), 1, true).withSnapshotProtectionSkipped(false);
    }

    private void selectAndUse(int hotbarSlot, int selectedSlot, InteractionHand interactionHand, float yaw, float pitch) {
        InventoryActionManager.INSTANCE.enqueueUse(() -> {
            if (selectedSlot != hotbarSlot) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
            }
        }, 0, false).delayOneTick().thenUse(() -> InventoryUtils.useHand(interactionHand, yaw, pitch, !mc.player.getItemInHand(interactionHand).is(Items.FIREWORK_ROCKET)), 0, false).delayOneTick().thenUse(() -> {
            if (selectedSlot != hotbarSlot) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(selectedSlot));
            }
        }, 0, false);
    }

    @Override
    public InventoryActionSequence swapSlots(int from, int to) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return null;
        }
        if (from == to || from == -1 || to == -1) {
            return null;
        }
        if (InventoryActionManager.INSTANCE.isBusy()) {
            return null;
        }
        int containerId = mc.player.containerMenu.containerId;
        int tempSlot = mc.player.getInventory().getSelectedSlot() % 8 + 1;
        return from >= 36 && from < 45 ? InventoryActionManager.INSTANCE.enqueueClick(() -> mc.gameMode.handleContainerInput(containerId, to, from - 36, ContainerInput.SWAP, mc.player)) : InventoryActionManager.INSTANCE.enqueueClick(() -> mc.gameMode.handleContainerInput(containerId, from, tempSlot, ContainerInput.SWAP, mc.player)).delayOneTick().thenClick(() -> mc.gameMode.handleContainerInput(containerId, to, tempSlot, ContainerInput.SWAP, mc.player)).delayOneTick().thenClick(() -> mc.gameMode.handleContainerInput(containerId, from, tempSlot, ContainerInput.SWAP, mc.player));
    }

    private void processQueue() {
        if (mc.gameMode != null) {
            if (!InventoryActionManager.INSTANCE.isBusy()) {
                DelayedInventoryActionManager.QueuedUse next = this.queue.peekFirst();
                if (next != null) {
                    if (!this.isBatchable(next)) {
                        DelayedInventoryActionManager.QueuedUse polled = this.queue.pollFirst();
                        if (polled != null) {
                            this.perform(polled.hand(), polled.target(), polled.yaw(), polled.pitch());
                        }
                    } else {
                        ArrayList batch = new ArrayList();
                        while (batch.size() < 3) {
                            DelayedInventoryActionManager.QueuedUse candidate = this.queue.peekFirst();
                            if (candidate == null || !this.isBatchable(candidate)) {
                                break;
                            }
                            this.queue.pollFirst();
                            batch.add(candidate);
                        }
                        if (batch.size() == 1) {
                            DelayedInventoryActionManager.QueuedUse single = (DelayedInventoryActionManager.QueuedUse) batch.get(0);
                            this.perform(single.hand(), single.target(), single.yaw(), single.pitch());
                        } else if (!batch.isEmpty()) {
                            this.useBatch(batch);
                        }
                    }
                }
            }
        }
    }

    private boolean isBatchable(DelayedInventoryActionManager.QueuedUse action) {
        return action.hand() == InteractionHand.MAIN_HAND && action.target() instanceof Item;
    }

    private void useBatch(List<DelayedInventoryActionManager.QueuedUse> actions) {
        int containerId = mc.player.containerMenu.containerId;
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        boolean[] usedHotbar = new boolean[9];
        ArrayList swaps = new ArrayList();
        ArrayList uses = new ArrayList();
        for (DelayedInventoryActionManager.QueuedUse action : actions) {
            Item item = (Item) action.target();
            int hotbarSlot = InventoryUtils.findItemIndex(item, true);
            if (hotbarSlot != -1) {
                usedHotbar[hotbarSlot] = true;
                uses.add(new DelayedInventoryActionManager.BatchUse(hotbarSlot, action.yaw(), action.pitch()));
            } else {
                int inventorySlot = InventoryUtils.findItemIndex(item, false);
                if (inventorySlot != -1) {
                    int swappedTo = this.findSwap(swaps, inventorySlot);
                    if (swappedTo != -1) {
                        uses.add(new DelayedInventoryActionManager.BatchUse(swappedTo, action.yaw(), action.pitch()));
                    } else {
                        int freeSlot = nearestFreeSlot(usedHotbar, selectedSlot);
                        if (freeSlot != -1) {
                            usedHotbar[freeSlot] = true;
                            swaps.add(new int[] { inventorySlot, freeSlot });
                            uses.add(new DelayedInventoryActionManager.BatchUse(freeSlot, action.yaw(), action.pitch()));
                        }
                    }
                }
            }
        }
        if (!uses.isEmpty()) {
            int[] currentSlot = new int[] { selectedSlot };
            int delay = swaps.isEmpty() ? 0 : 1;
            InventoryActionManager.INSTANCE.enqueueClick(() -> this.applySwaps(containerId, swaps), delay, !swaps.isEmpty()).delayOneTick().thenUse(() -> {
                for (DelayedInventoryActionManager.BatchUse use : (Iterable<DelayedInventoryActionManager.BatchUse>) (Iterable) uses) {
                    this.selectSlot(currentSlot, use.hotbarSlot());
                    InteractionResult result = InventoryUtils.useHandWithResult(InteractionHand.MAIN_HAND, use.yaw(), use.pitch());
                    if (result.consumesAction()) {
                        mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                    }
                }
                this.selectSlot(currentSlot, selectedSlot);
            }, delay, false).delayOneTick().thenClick(() -> this.applySwaps(containerId, swaps), delay, !swaps.isEmpty());
        }
    }

    private void applySwaps(int containerId, List<int[]> swaps) {
        if (mc.player != null && mc.gameMode != null) {
            if (!swaps.isEmpty()) {
                for (int[] swap : swaps) {
                    mc.gameMode.handleContainerInput(containerId, swap[0], swap[1], ContainerInput.SWAP, mc.player);
                }
            }
        }
    }

    private int findSwap(List<int[]> swaps, int inventorySlot) {
        for (int[] swap : swaps) {
            if (swap[0] == inventorySlot) {
                return swap[1];
            }
        }
        return -1;
    }

    private static int nearestFreeSlot(boolean[] used, int preferred) {
        if (preferred >= 0 && preferred < 9 && !used[preferred]) {
            return preferred;
        }
        for (int i = 1; i < 9; i++) {
            int right = preferred + i;
            if (right < 9 && !used[right]) {
                return right;
            }
            int left = preferred - i;
            if (left >= 0 && !used[left]) {
                return left;
            }
        }
        return -1;
    }

    private void selectSlot(int[] current, int slot) {
        if (mc.player != null) {
            if (slot >= 0 && slot <= 8 && current[0] != slot) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
                current[0] = slot;
            }
        }
    }

    private void clearQueue() {
        this.queue.clear();
    }

    @Override
    public void abort() {
        InventoryActionManager.INSTANCE.reset();
    }

    public ConcurrentLinkedDeque<DelayedInventoryActionManager.QueuedUse> getQueue() {
        return this.queue;
    }

    public int getCooldown() {
        return this.cooldown;
    }

    private record QueuedUse(InteractionHand hand, Object target, float yaw, float pitch) {
    }

    private record BatchUse(int hotbarSlot, float yaw, float pitch) {
    }
}
