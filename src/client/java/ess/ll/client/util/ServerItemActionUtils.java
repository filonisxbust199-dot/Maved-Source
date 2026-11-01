package ess.ll.client.util;

import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.integration.MinecraftAccess;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.BundleContents.Mutable;

public final class ServerItemActionUtils implements MinecraftAccess {
    private static final int NO_SCORE = Integer.MAX_VALUE;
    private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

    public static boolean useFromBundle(Predicate<ItemStack> predicate, Runnable task) {
        ServerItemActionUtils.BundleItemRef found = findInBundle(predicate);
        if (found == null) {
            return false;
        }
        ServerItemActionUtils.Placement placement = findPlacement(found);
        if (placement == null) {
            return false;
        }
        int containerId = mc.player.containerMenu.containerId;
        int bundleSlot = found.bundle().index;
        int index = found.index();
        int landSlot = placement.landSlotId();
        int parkSlot = placement.parkSlotId();
        InventoryActionManager.INSTANCE.enqueueClick(() -> pickFromBundle(containerId, bundleSlot, index), 1, false).delay(1, false).thenClick(() -> {
            leftClick(containerId, landSlot);
            if (parkSlot != -1) {
                putCarried(containerId, parkSlot);
            }
            putCarried(containerId, bundleSlot);
            task.run();
        }, 1, false);
        return true;
    }

    private static Slot emptyInventorySlot() {
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        Slot fallback = null;
        for (Slot candidate : mc.player.containerMenu.slots) {
            if (isInventorySlot(candidate) && candidate.getItem().isEmpty()) {
                if (candidate.getContainerSlot() != selectedSlot) {
                    return candidate;
                }
                fallback = candidate;
            }
        }
        return fallback;
    }

    private static ServerItemActionUtils.Placement findPlacement(ServerItemActionUtils.BundleItemRef found) {
        Slot empty = emptyInventorySlot();
        if (empty != null) {
            return new ServerItemActionUtils.Placement(empty.index, -1);
        }
        Slot merge = mergeSlotFor(found.target());
        if (merge != null) {
            return new ServerItemActionUtils.Placement(merge.index, -1);
        }
        Slot parked = null;
        Slot parkTarget = null;
        int bestScore = NO_SCORE;
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        for (Slot candidate : mc.player.containerMenu.slots) {
            if (isInventorySlot(candidate) && candidate != found.bundle() && candidate.getContainerSlot() != selectedSlot) {
                ItemStack item = candidate.getItem();
                if (!item.isEmpty() && !isBundle(item)) {
                    int score = slotScore(candidate, item);
                    if (score < bestScore) {
                        Slot target = parkTargetFor(found, candidate, item);
                        if (target != null) {
                            parked = candidate;
                            parkTarget = target;
                            bestScore = score;
                        }
                    }
                }
            }
        }
        return parked == null ? null : new ServerItemActionUtils.Placement(parked.index, parkTarget.index);
    }

    private static Slot slotAt(int index) {
        if (mc.player == null) {
            return null;
        }
        NonNullList<Slot> slots = mc.player.containerMenu.slots;
        return index >= 0 && index < slots.size() ? (Slot) slots.get(index) : null;
    }

    private static void selectBundleItem(int containerId, int slotId, int index) {
        if (mc.player != null && mc.player.containerMenu.containerId == containerId) {
            Slot slot = slotAt(slotId);
            if (slot != null) {
                ItemStack item = slot.getItem();
                if (isBundle(item)) {
                    if (BundleItem.getSelectedItemIndex(item) != index) {
                        BundleItem.toggleSelectedItem(item, index);
                        mc.player.connection.send(new ServerboundSelectBundleItemPacket(slotId, index));
                    }
                }
            }
        }
    }

    public static ItemStack useFromBundle(Item item, InteractionHand interactionHand, float yaw, float pitch) {
        return useFromBundle(stack -> stack.getItem() == item, interactionHand, yaw, pitch);
    }

    public static ItemStack useFromBundle(Predicate<ItemStack> predicate, InteractionHand interactionHand, float yaw, float pitch) {
        ServerItemActionUtils.BundleItemRef found = findInBundle(predicate);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        if (mc.player.getCooldowns().isOnCooldown(found.target())) {
            return ItemStack.EMPTY;
        }
        Slot slot = inventorySlot(interactionHand == InteractionHand.OFF_HAND ? 40 : mc.player.getInventory().getSelectedSlot());
        if (slot == null) {
            return ItemStack.EMPTY;
        }
        int containerId = mc.player.containerMenu.containerId;
        int bundleSlot = found.bundle().index;
        int index = found.index();
        int handSlot = slot.index;
        InventoryActionManager.INSTANCE.enqueueClick(() -> pickFromBundle(containerId, bundleSlot, index), 1, false).delay(1, false).thenClick(() -> leftClick(containerId, handSlot), 1, false).delay(1, false).thenUse(() -> {
            InteractionResult result = InventoryUtils.useHandWithResult(interactionHand, yaw, pitch);
            if (result.consumesAction()) {
                mc.player.swing(interactionHand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            }
        }, 1, false).delay(1, false).thenClick(() -> {
            putBackIfNeeded(containerId, handSlot);
            putCarried(containerId, bundleSlot);
        }, 1, false);
        return found.target();
    }

    public static Slot findBundleSlot(Predicate<ItemStack> predicate) {
        if (mc.player == null) {
            return null;
        }
        for (Slot slot : mc.player.containerMenu.slots) {
            if (slot.container == mc.player.getInventory()) {
                ItemStack item = slot.getItem();
                if (isBundle(item) && indexInBundle(item, predicate) != -1) {
                    return slot;
                }
            }
        }
        return null;
    }

    public static boolean hasInBundle(Item item) {
        return findBundleWith(item) != null;
    }

    private static ServerItemActionUtils.BundleItemRef findInBundle(Predicate<ItemStack> predicate) {
        if (mc.player == null || mc.gameMode == null) {
            return null;
        }
        if (InventoryActionManager.INSTANCE.isBusy()) {
            return null;
        }
        if (!mc.player.containerMenu.getCarried().isEmpty()) {
            return null;
        }
        Slot slot = findBundleSlot(predicate);
        if (slot == null) {
            return null;
        }
        BundleContents bundleContents = slot.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents == null) {
            return null;
        }
        int index = indexInBundle(slot.getItem(), predicate);
        return index == -1 ? null : new ServerItemActionUtils.BundleItemRef(slot, bundleContents, index, bundleContents.items().get(index).create());
    }

    public static int indexInBundle(ItemStack stack, Item item) {
        return indexInBundle(stack, candidate -> candidate.getItem() == item);
    }

    public static boolean useFromBundle(Item item, Runnable task) {
        return useFromBundle(stack -> stack.getItem() == item, task);
    }

    private static int slotScore(Slot slot, ItemStack stack) {
        return slot.getContainerSlot() < 9 ? stack.getCount() + 99 : stack.getCount();
    }

    private static void pickFromBundle(int containerId, int slotId, int index) {
        selectBundleItem(containerId, slotId, index);
        click(containerId, slotId, 1);
    }

    private static Slot inventorySlot(int containerSlot) {
        for (Slot slot : mc.player.containerMenu.slots) {
            if (slot.container == mc.player.getInventory() && slot.getContainerSlot() == containerSlot) {
                return slot;
            }
        }
        return null;
    }

    private static boolean fitsInBundle(BundleContents bundleContents, ItemStack stack) {
        return bundleContents == null ? false : bundleContents.asMutable().tryInsert(stack.copy()) >= stack.getCount();
    }

    private static boolean fitsAfterTaking(ServerItemActionUtils.BundleItemRef found, ItemStack stack) {
        Mutable mutable = found.contents().asMutable();
        if (found.contents().getSelectedItemIndex() != found.index()) {
            mutable.toggleSelectedItem(found.index());
        }
        return mutable.removeOne() == null ? false : mutable.tryInsert(stack.copy()) >= stack.getCount();
    }

    private ServerItemActionUtils() {
        throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
    }

    public static void collectContents(ItemStack stack, List<ItemStack> stacks) {
        BundleContents bundleContents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            var items = bundleContents.items();
            for (int i = 0; i < items.size(); i++) {
                ItemStack inner = ((ItemStackTemplate) items.get(i)).create();
                stacks.add(inner);
                if (isBundle(inner)) {
                    collectContents(inner, stacks);
                }
            }
        }
    }

    private static void putBackIfNeeded(int containerId, int slotId) {
        Slot slot = slotAt(slotId);
        if (slot != null) {
            if (!slot.getItem().isEmpty() || !mc.player.containerMenu.getCarried().isEmpty()) {
                leftClick(containerId, slotId);
            }
        }
    }

    private static void leftClick(int containerId, int slotId) {
        click(containerId, slotId, 0);
    }

    public static int indexInBundle(ItemStack stack, Predicate<ItemStack> predicate) {
        BundleContents bundleContents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents == null) {
            return -1;
        }
        var items = bundleContents.items();
        for (int i = 0; i < items.size(); i++) {
            if (predicate.test(((ItemStackTemplate) items.get(i)).create())) {
                return i;
            }
        }
        return -1;
    }

    public static Slot findBundleWith(Item item) {
        return findBundleSlot(stack -> stack.getItem() == item);
    }

    private static Slot mergeSlotFor(ItemStack stack) {
        for (Slot slot : mc.player.containerMenu.slots) {
            if (isInventorySlot(slot)) {
                ItemStack item = slot.getItem();
                if (ItemStack.isSameItemSameComponents(item, stack) && item.getCount() + stack.getCount() <= slot.getMaxStackSize(item)) {
                    return slot;
                }
            }
        }
        return null;
    }

    private static Slot parkTargetFor(ServerItemActionUtils.BundleItemRef found, Slot slot, ItemStack stack) {
        if (BundleContents.canItemBeInBundle(stack)) {
            if (fitsAfterTaking(found, stack)) {
                return found.bundle();
            }
            for (Slot other : mc.player.containerMenu.slots) {
                if (isInventorySlot(other) && other != slot && other != found.bundle()) {
                    ItemStack item = other.getItem();
                    if (isBundle(item) && fitsInBundle(item.get(DataComponents.BUNDLE_CONTENTS), stack)) {
                        return other;
                    }
                }
            }
        }
        Slot offhand = inventorySlot(40);
        return offhand != null && offhand.getItem().isEmpty() && offhand.mayPlace(stack) ? offhand : null;
    }

    private static boolean isInventorySlot(Slot slot) {
        if (slot.container != mc.player.getInventory()) {
            return false;
        }
        int containerSlot = slot.getContainerSlot();
        return containerSlot >= 0 && containerSlot < 36;
    }

    public static boolean isBundle(ItemStack stack) {
        return stack.getItem() instanceof BundleItem && stack.get(DataComponents.BUNDLE_CONTENTS) != null;
    }

    private static void click(int containerId, int slotId, int button) {
        if (mc.player != null && mc.gameMode != null) {
            if (mc.player.containerMenu.containerId == containerId) {
                mc.gameMode.handleContainerInput(containerId, slotId, button, ContainerInput.PICKUP, mc.player);
            }
        }
    }

    private static void putCarried(int containerId, int slotId) {
        if (mc.player != null && !mc.player.containerMenu.getCarried().isEmpty()) {
            leftClick(containerId, slotId);
        }
    }

    private record Placement(int landSlotId, int parkSlotId) {
    }

    private record BundleItemRef(Slot bundle, BundleContents contents, int index, ItemStack target) {
    }
}
