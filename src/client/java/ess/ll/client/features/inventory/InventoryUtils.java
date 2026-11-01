package ess.ll.client.features.inventory;

import ess.ll.client.core.TickTaskScheduler;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AutoTotem;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

public final class InventoryUtils implements MinecraftAccess {
    static int savedOffhandSlot = -1;
    private static int serverSelectedSlot = -1;
    static int previousSlot = -1;
    public static boolean fireworkUsed = false;

    public static void useStack(ItemStack stack, InteractionHand interactionHand, float var2, float var3) {
        if (mc.player != null) {
            if (!mc.player.getCooldowns().isOnCooldown(stack)) {
                Slot slot = findSlot(stack.getItem());
                if (slot != null) {
                    useFromSlot(slot, interactionHand, var2, var3);
                }
            }
        }
    }

    public static Slot findPlayerSlot(Item item) {
        return findSlot(var1 -> var1.getItem().equals(item), true);
    }

    private static int hotbarIndex(Slot slot) {
        if (slot != null && mc.player != null) {
            if (slot.container != mc.player.getInventory()) {
                return -1;
            }
            int containerSlot = slot.getContainerSlot();
            return containerSlot >= 0 && containerSlot < 9 ? containerSlot : -1;
        } else {
            return -1;
        }
    }

    public static void clickSlot(Slot slot, int var1, ContainerInput containerInput, boolean var3) {
        if (slot != null) {
            clickSlot(slot.index, var1, containerInput, var3);
        }
    }

    public static void swapIntoHand(int var0, InteractionHand interactionHand, boolean var2) {
        if (var0 != -1) {
            int var3 = interactionHand.equals(InteractionHand.MAIN_HAND) ? mc.player.getInventory().getSelectedSlot() : 40;
            if (var2) {
                TickTaskScheduler.runInInventory(() -> clickSlot(var0, var3, ContainerInput.SWAP, false), 1);
            } else {
                clickSlot(var0, var3, ContainerInput.SWAP, false);
            }
        }
    }

    public static boolean isOnCooldown(Item item) {
        if (mc.player == null) {
            return false;
        }
        ItemStack stack = findStack(item);
        return !stack.isEmpty() && mc.player.getCooldowns().isOnCooldown(stack);
    }

    public static boolean isProtectedSword(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD || item == Items.IRON_SWORD;
    }

    public static List<Slot> getContainerSlots() {
        if (mc.player == null) {
            return null;
        }
        int var0 = mc.player.containerMenu.slots.size() - 37;
        return var0 < mc.player.containerMenu.slots.size() ? mc.player.containerMenu.slots : null;
    }

    public static boolean isProtectedItem(ItemStack stack) {
        Item item = stack.getItem();
        return !isProtectedSword(stack) && !(stack.is(ItemTags.AXES)) && item != Items.MACE ? item == Items.ELYTRA || item == Items.TOTEM_OF_UNDYING || item == Items.END_CRYSTAL || item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN : true;
    }

    public static boolean selectServerSlot(int var0) {
        if (mc.player == null) {
            return false;
        }
        if (var0 < 0 || var0 > 8) {
            return false;
        }
        if (serverSelectedSlot == var0) {
            return false;
        }
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(var0));
        serverSelectedSlot = var0;
        return true;
    }

    public static boolean isValidClick(int var0, int var1, ContainerInput containerInput) {
        return switch (containerInput) {
            case PICKUP, QUICK_MOVE, CLONE ->
                var1 >= 0 && var1 <= 2;
            case SWAP ->
                var0 >= 0 && (var1 >= 0 && var1 < 9 || var1 == 40);
            case THROW ->
                var1 == 0 || var1 == 1;
            case QUICK_CRAFT ->
                var1 >= 0 && var1 <= 10 && var1 != 3 && var1 != 7;
            case PICKUP_ALL ->
                var1 == 0;
        };
    }

    public static void swapIntoHand(Slot slot, InteractionHand interactionHand, boolean var2) {
        if (slot != null) {
            swapIntoHand(slot.index, interactionHand, var2);
        }
    }

    public static int findEmptyHotbarSlot(int var0) {
        if (mc.player == null) {
            return -1;
        }
        NonNullList<ItemStack> nonEquipmentItems = mc.player.getInventory().getNonEquipmentItems();
        for (int var2 = 0; var2 < 9; var2++) {
            if (var2 != var0 && ((ItemStack) nonEquipmentItems.get(var2)).isEmpty() && mc.player.getInventory().getSelectedSlot() != var2) {
                return var2;
            }
        }
        return -1;
    }

    private static Slot findSlot(Predicate<ItemStack> predicate, boolean var1) {
        NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
        int var3 = slots2.size() - 37;
        return slots2.stream().filter(var3x -> predicate.test(var3x.getItem()) && (!var1 || var3x.index >= var3)).findFirst().orElse(null);
    }

    public static void useStack(ItemStack stack, float var1, float var2) {
        if (mc.player != null) {
            if (!mc.player.getCooldowns().isOnCooldown(stack)) {
                Slot slot = findSlot(stack.getItem());
                if (slot != null) {
                    useFromSlot(slot, InteractionHand.MAIN_HAND, var1, var2);
                }
            }
        }
    }

    public static int findAxeIndex(boolean var0) {
        if (mc.player == null) {
            return -1;
        }
        int var1 = var0 ? 0 : 9;
        int var2 = var0 ? 9 : 36;
        NonNullList<ItemStack> nonEquipmentItems = mc.player.getInventory().getNonEquipmentItems();
        for (int var4 = var1; var4 < var2; var4++) {
            if (((ItemStack) nonEquipmentItems.get(var4)).is(ItemTags.AXES)) {
                return var4;
            }
        }
        return -1;
    }

    public static int countInContainer(Item item) {
        return mc.player.containerMenu.slots.stream().filter(var1 -> var1.getItem().getItem().equals(item)).mapToInt(var0x -> var0x.getItem().getCount()).sum();
    }

    private InventoryUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Slot findAnySlot(List<Item> items) {
        return findSlot(var1 -> items.contains(var1.getItem()), false);
    }

    public static ItemStack findStack(Item item) {
        Slot slot = findSlot(item);
        return slot == null ? ItemStack.EMPTY : slot.getItem();
    }

    public static void useOffhandTemporarily(Item item) {
        if (mc.player != null) {
            if (!AutoTotem.isSwapInProgress()) {
                boolean var1 = !mc.player.getOffhandItem().isEmpty();
                Slot slot = findSlot(item);
                if (slot != null) {
                    int index2 = slot.index;
                    if (index2 >= 0) {
                        handleContainerClick(mc.player.containerMenu.containerId, index2, 40, ContainerInput.SWAP, mc.player);
                        useHand(InteractionHand.OFF_HAND, Float.MAX_VALUE, Float.MAX_VALUE, false);
                        if (var1 && savedOffhandSlot == -1) {
                            savedOffhandSlot = index2;
                        }
                    }
                    if (mc.player.getOffhandItem().getItem() == item) {
                        if (savedOffhandSlot == -1) {
                            return;
                        }
                        handleContainerClick(mc.player.containerMenu.containerId, savedOffhandSlot, 40, ContainerInput.SWAP, mc.player);
                        savedOffhandSlot = -1;
                    }
                }
            }
        }
    }

    public static long countEmptyHotbarSlots() {
        return getHotbarSlots().stream().filter(var0 -> var0.getItem().isEmpty()).count();
    }

    public static InventoryActionSequence scheduleSwap(int var0, int var1) {
        return var0 != var1 && var0 != -1 ? InventoryActionManager.INSTANCE.enqueueClick(() -> swapSlots(var0, var1)) : null;
    }

    public static Slot findNamedSlot(Item item, String var1) {
        if (mc.player == null) {
            return null;
        }
        for (int var2 = 0; var2 < mc.player.containerMenu.slots.size(); var2++) {
            Slot slot = mc.player.containerMenu.getSlot(var2);
            ItemStack item2 = slot.getItem();
            if (item2.getItem() == item && (var1 == null || var1.isEmpty() || item2.getHoverName().getString().equals(var1))) {
                return slot;
            }
        }
        return null;
    }

    public static boolean useHand(InteractionHand interactionHand, float var1, float var2) {
        return useHand(interactionHand, var1, var2, true);
    }

    public static int findItemIndex(Item item, boolean var1) {
        if (mc.player == null) {
            return -1;
        }
        if (var1) {
            for (int var2 = 0; var2 < 9; var2++) {
                if (mc.player.getInventory().getItem(var2).getItem() == item) {
                    return var2;
                }
            }
        } else {
            for (int var3 = 9; var3 < 45; var3++) {
                if (mc.player.getInventory().getItem(var3).getItem() == item) {
                    return var3;
                }
            }
        }
        return -1;
    }

    public static Slot findAnyPlayerSlot(List<Item> items) {
        return findSlot(var1 -> items.contains(var1.getItem()), true);
    }

    public static int findHotbarIndex(Item item) {
        return IntStream.range(0, 9).filter(var1 -> mc.player.getInventory().getItem(var1).getItem().equals(item)).findFirst().orElse(-1);
    }

    public static boolean containsItem(Item item) {
        if (mc.player == null) {
            return false;
        }
        for (ItemStack stack : mc.player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    public static int findMatchingIndex(Predicate<ItemStack> predicate, boolean var1) {
        int var2 = var1 ? mc.player.getInventory().getContainerSize() : 9;
        return IntStream.range(0, var2).filter(var1x -> {
            ItemStack item = mc.player.getInventory().getItem(var1x);
            return !item.isEmpty() && predicate.test(item);
        }).findFirst().orElse(-1);
    }

    public static void swapSlots(int var0, int var1) {
        if (var0 < 9) {
            clickSlot(var1, var0, ContainerInput.SWAP, false);
        } else if (var0 >= 36 && var0 < 45) {
            clickSlot(var1, var0 - 36, ContainerInput.SWAP, false);
        } else {
            int var2 = findTemporaryHotbarSlot();
            if (var2 == -1) {
                clickSlot(var0, 0, ContainerInput.PICKUP, false);
                clickSlot(var1, 0, ContainerInput.PICKUP, false);
                if (!mc.player.containerMenu.getCarried().isEmpty()) {
                    clickSlot(var0, 0, ContainerInput.PICKUP, false);
                }
            } else {
                clickSlot(var0, var2, ContainerInput.SWAP, false);
                clickSlot(var1, var2, ContainerInput.SWAP, false);
                clickSlot(var0, var2, ContainerInput.SWAP, false);
            }
        }
    }

    private static void sendSelectedSlot(int var0) {
        if (mc.player != null && var0 >= 0 && var0 <= 8) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(var0));
        }
    }

    public static void useFirework() {
        boolean usingItem = mc.player.isUsingItem();
        Item FIREWORK_ROCKET2 = Items.FIREWORK_ROCKET;
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        if (!usingItem) {
            for (int var3 = 0; var3 < 9; var3++) {
                if (mc.player.getInventory().getItem(var3).getItem() == FIREWORK_ROCKET2) {
                    if (var3 != mc.player.getInventory().getSelectedSlot()) {
                        mc.player.connection.send(new ServerboundSetCarriedItemPacket(var3));
                    }
                    useHand(InteractionHand.MAIN_HAND, Float.MAX_VALUE, Float.MAX_VALUE, false);
                    if (var3 != mc.player.getInventory().getSelectedSlot()) {
                        mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
                    }
                    fireworkUsed = true;
                    return;
                }
            }
            InventoryActionManager.INSTANCE.enqueueClick(() -> {
                for (int var1x = 0; var1x < 36; var1x++) {
                    if (mc.player.getInventory().getItem(var1x).getItem() == FIREWORK_ROCKET2) {
                        handleContainerClick(0, var1x, 8, ContainerInput.SWAP, mc.player);
                        break;
                    }
                }
            }, 1, true);
        } else {
            InventoryActionManager.INSTANCE.enqueueUse(() -> useOffhandTemporarily(Items.FIREWORK_ROCKET));
        }
    }

    public static boolean hasCustomProperties(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.isEnchanted() && !stack.hasFoil()) {
            if (Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))) {
                return true;
            }
            if (!stack.has(DataComponents.CUSTOM_MODEL_DATA) && !stack.has(DataComponents.CUSTOM_NAME)) {
                ItemLore itemLore = stack.get(DataComponents.LORE);
                if (itemLore != null && !itemLore.lines().isEmpty()) {
                    return true;
                }
                ItemAttributeModifiers itemAttributeModifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                return itemAttributeModifiers != null && !itemAttributeModifiers.modifiers().isEmpty();
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static boolean useFromSlot(Slot slot, InteractionHand interactionHand, float var2, float var3) {
        return useFromSlot(slot, interactionHand, var2, var3, true);
    }

    /**
     * Use an item from a temporary slot without forcing an attack-hand swing.
     */
    public static boolean useFromSlot(Slot slot, InteractionHand interactionHand, float var2, float var3, boolean swing) {
        if (mc.player == null) {
            return false;
        } else {
            int var4 = interactionHand == InteractionHand.MAIN_HAND ? hotbarIndex(slot) : -1;
            if (var4 != -1) {
                int selectedSlot = mc.player.getInventory().getSelectedSlot();
                sendSelectedSlot(var4);
                useHand(interactionHand, var2, var3, swing);
                sendSelectedSlot(selectedSlot);
                return true;
            } else {
                InventoryActionManager.INSTANCE.enqueueClick(() -> {
                    swapIntoHand(slot, interactionHand, false);
                    useHand(interactionHand, var2, var3, swing);
                    swapIntoHand(slot, interactionHand, false);
                });
                return true;
            }
        }
    }

    public static void clickSlot(int var0, int var1, ContainerInput containerInput, boolean var3) {
        clickContainer(0, var0, var1, containerInput, var3);
    }

    public static List<Slot> getHotbarSlots() {
        NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
        return new ArrayList<>(slots2.stream().filter(var1 -> var1.index > slots2.size() - 10).toList());
    }

    public static void clickContainer(int var0, int var1, int var2, ContainerInput containerInput, boolean var4) {
        if (mc.player != null) {
            handleContainerClick(var0, var1, var2, containerInput, mc.player);
        }
    }

    public static InteractionResult useHandWithResult(InteractionHand interactionHand, float var1, float var2) {
        if (mc.gameMode != null && mc.player != null) {
            if (var1 != Float.MAX_VALUE || var2 != Float.MAX_VALUE) {
                RotationManager.rotate(new Rotation(var1, var2), 9999.0F, 99999, false);
            }
            return mc.gameMode.useItem(mc.player, interactionHand);
        } else {
            return InteractionResult.FAIL;
        }
    }

    public static int findTemporaryHotbarSlot() {
        int var0 = findEmptyHotbarSlot(-1);
        return var0 != -1 ? var0 : findReplaceableHotbarSlot(-1);
    }

    public static int countItems(Item item) {
        return IntStream.range(0, 36).filter(var1 -> mc.player.getInventory().getItem(var1).getItem().equals(item)).map(var0x -> mc.player.getInventory().getItem(var0x).getCount()).sum();
    }

    public static int findReplaceableHotbarSlot(int var0) {
        if (mc.player == null) {
            return -1;
        }
        NonNullList<ItemStack> nonEquipmentItems = mc.player.getInventory().getNonEquipmentItems();
        for (int var2 = 0; var2 < 9; var2++) {
            if (var2 != var0 && mc.player.getInventory().getSelectedSlot() != var2 && !isProtectedItem((ItemStack) nonEquipmentItems.get(var2))) {
                return var2;
            }
        }
        return -1;
    }

    public static Slot findSlot(Item item) {
        return findSlot(var1 -> var1.getItem() == item, false);
    }

    public static void handleContainerClick(int var0, int var1, int var2, ContainerInput containerInput, Player player) {
        if (mc.gameMode != null && isValidClick(var1, var2, containerInput)) {
            mc.gameMode.handleContainerInput(var0, var1, var2, containerInput, player);
        }
    }

    public static Slot findAxeSlot() {
        return findSlot(var0 -> var0.is(ItemTags.AXES), false);
    }

    public static boolean useHand(InteractionHand interactionHand, float var1, float var2, boolean var3) {
        if (mc.gameMode != null && mc.player != null) {
            if (var1 != Float.MAX_VALUE || var2 != Float.MAX_VALUE) {
                RotationManager.rotate(new Rotation(var1, var2), 9999.0F, 99999, false);
            }
            mc.gameMode.useItem(mc.player, interactionHand);
            if (var3) {
                mc.player.swing(interactionHand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            }
            return false;
        } else {
            return false;
        }
    }
}
