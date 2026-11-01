package ess.ll.client.module.player;

import ess.ll.client.event.HeldItemRenderEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.SelectedSlotEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.features.world.BlockBreakManager;
import ess.ll.client.features.world.BlockBreakSpeedUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ModuleManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import ess.ll.client.mixin.MultiPlayerGameModeMixin_1262;

public class AutoTool extends ClientModule {
    private final ModeSetting mode;
    private final ToggleSetting useInventory;
    private int originalSelectedSlot;
    private int autoSelectedSlot;
    private int nextToolSlot;
    private int restoreDelayTicks;
    private int pendingHotbarSlot;
    private int pendingInventorySlot;
    private Item pendingTool;
    private int swappedHotbarSlot;
    private int swappedInventorySlot;
    private Item swappedTool;

    private void resetPendingSwap() {
        this.resetToolSelection();
        this.pendingHotbarSlot = -1;
        this.pendingInventorySlot = -1;
        this.pendingTool = null;
    }

    private void resetToolSelection() {
        this.originalSelectedSlot = -1;
        this.autoSelectedSlot = -1;
        this.restoreDelayTicks = 0;
    }

    public AutoTool() {
        super(ModuleCategory.Player, ModuleSubCategory.Main);
        this.mode = new ModeSetting("Режим").addModes("Обычный", "Скрытый").withDefault("Обычный").withContainer(this);
        this.useInventory = new ToggleSetting("Брать из инвентаря").withDefault(false).withContainer(this);
        this.originalSelectedSlot = -1;
        this.autoSelectedSlot = -1;
        this.nextToolSlot = -1;
        this.pendingHotbarSlot = -1;
        this.pendingInventorySlot = -1;
        this.swappedHotbarSlot = -1;
        this.swappedInventorySlot = -1;
        this.setName("Auto Tool");
    }

    private void prepareInventoryTool(BlockState blockState) {
        if (this.useInventory.isEnabled()) {
            if (this.pendingHotbarSlot == -1 && !InventoryActionManager.INSTANCE.isBusy()) {
                if (mc.player.containerMenu == mc.player.inventoryMenu) {
                    int inventorySlot = findBetterInventoryTool(blockState, bestHotbarTool(blockState));
                    if (inventorySlot == -1) {
                        if (this.swappedHotbarSlot != -1 && bestHotbarSlot(blockState) != this.swappedHotbarSlot) {
                            this.restoreInventorySwap();
                        }
                    } else {
                        int hotbarSlot = this.swappedHotbarSlot;
                        if (hotbarSlot != -1) {
                            boolean restoringRequestedTool = inventorySlot == this.swappedInventorySlot;
                            this.restoreInventorySwap();
                            if (restoringRequestedTool) {
                                return;
                            }
                        } else {
                            hotbarSlot = this.findTemporaryHotbarSlot(blockState);
                        }
                        if (hotbarSlot != -1) {
                            this.scheduleHotbarSwap(inventorySlot, hotbarSlot);
                            this.pendingHotbarSlot = hotbarSlot;
                            this.pendingInventorySlot = inventorySlot;
                            this.pendingTool = mc.player.getInventory().getItem(inventorySlot).getItem();
                        }
                    }
                }
            }
        }
    }

    private static boolean isUnenchantedShears(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.SHEARS && BlockBreakSpeedUtils.miningEfficiency(stack) <= 0.0F;
    }

    private void confirmInventorySwap() {
        if (this.pendingHotbarSlot != -1 && !InventoryActionManager.INSTANCE.isBusy()) {
            int hotbarSlot = this.pendingHotbarSlot;
            int inventorySlot = this.pendingInventorySlot;
            Item tool = this.pendingTool;
            this.pendingHotbarSlot = -1;
            this.pendingInventorySlot = -1;
            this.pendingTool = null;
            if (mc.player.getInventory().getItem(hotbarSlot).getItem() == tool) {
                this.swappedHotbarSlot = hotbarSlot;
                this.swappedInventorySlot = inventorySlot;
                this.swappedTool = tool;
            }
        }
    }

    private static float miningSpeed(ItemStack stack, BlockState blockState) {
        return BlockBreakSpeedUtils.normalizedDestroySpeed(stack, blockState);
    }

    private int findTemporaryHotbarSlot(BlockState blockState) {
        int emptySlot = InventoryUtils.findEmptyHotbarSlot(this.originalSelectedSlot);
        if (emptySlot != -1) {
            return emptySlot;
        }
        int bestSlot = bestHotbarSlot(blockState);
        return bestSlot != -1 && bestSlot != this.originalSelectedSlot ? bestSlot : InventoryUtils.findReplaceableHotbarSlot(this.originalSelectedSlot);
    }

    private void clearSwapRestoreState() {
        this.swappedHotbarSlot = -1;
        this.swappedInventorySlot = -1;
        this.swappedTool = null;
    }

    private void selectToolForBlock(BlockPos blockPos) {
        BlockState blockState = mc.level.getBlockState(blockPos);
        if (!(blockState.getDestroyProgress(mc.player, mc.level, blockPos) >= 1.0F)) {
            this.prepareInventoryTool(blockState);
            this.nextToolSlot = this.findFasterHotbarSlot(blockState);
        }
    }

    private void scheduleHotbarSwap(int inventorySlot, int hotbarSlot) {
        int containerId = mc.player.containerMenu.containerId;
        InventoryActionManager.INSTANCE.enqueueClick(() -> mc.gameMode.handleContainerInput(containerId, inventorySlot, hotbarSlot, ContainerInput.SWAP, mc.player));
    }

    private void scheduleToolRestore() {
        this.restoreDelayTicks = this.swappedHotbarSlot == -1 ? 2 : 3;
    }

    @Subscribe
    public void onSelectedSlot(SelectedSlotEvent event) {
        this.resetToolSelection();
    }

    private BlockPos targetedBlock() {
        BlockBreakManager mining = BlockBreakManager.INSTANCE;
        if (mining.hasTarget() && !mc.options.keyAttack.isDown()) {
            return mining.canMineTarget() ? mining.targetPosition() : null;
        } else if (mc.gui.screen() != null) {
            return null;
        } else if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == Type.BLOCK) {
            return mc.level.getBlockState(blockHit.getBlockPos()).isAir() ? null : blockHit.getBlockPos();
        } else {
            return null;
        }
    }

    private boolean isMining() {
        return mc.gameMode.isDestroying() || mc.options.keyAttack.isDown() || BlockBreakManager.INSTANCE.hasTarget();
    }

    @Subscribe
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.nextToolSlot != -1 && mc.gameMode != null) {
            int toolSlot = this.nextToolSlot;
            this.nextToolSlot = -1;
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            if (selectedSlot != toolSlot) {
                this.rememberSelectedSlot();
                mc.player.getInventory().setSelectedSlot(toolSlot);
                this.autoSelectedSlot = toolSlot;
                ((MultiPlayerGameModeMixin_1262) (Object) mc.gameMode).hookEnsureHasSentCarriedItem();
            }
        }
    }

    @Subscribe
    public void onHeldItemRender(HeldItemRenderEvent event) {
        if (this.mode.is("Скрытый")) {
            int displaySlot = this.originalSelectedSlot;
            if (displaySlot >= 0 && displaySlot < 9) {
                switch (event.getKind()) {
                    case SELECTED_SLOT:
                        event.setSlot(displaySlot);
                        break;
                    case MAIN_HAND_STACK:
                        if (event.getPlayer() instanceof LocalPlayer) {
                            event.setStack(mc.player.getInventory().getItem(displaySlot));
                        }
                        break;
                    case STACK_IN_ARM:
                        if (event.getLivingEntity() instanceof LocalPlayer && event.getArm() == mc.player.getMainArm()) {
                            event.setStack(mc.player.getInventory().getItem(displaySlot));
                        }
                }
            }
        }
    }

    private boolean hasManualSlotChange() {
        return this.autoSelectedSlot != -1 && mc.player.getInventory().getSelectedSlot() != this.autoSelectedSlot;
    }

    private static int findBetterInventoryTool(BlockState blockState, ItemStack bestTool) {
        float bestSpeed = miningSpeed(bestTool, blockState);
        int bestSlot = -1;
        for (int slot = 9; slot < 36; slot++) {
            ItemStack candidate = mc.player.getInventory().getItem(slot);
            float candidateSpeed = miningSpeed(candidate, blockState);
            if (isBetterTool(candidate, bestTool, candidateSpeed, bestSpeed, blockState)) {
                bestSpeed = candidateSpeed;
                bestSlot = slot;
                bestTool = candidate;
            }
        }
        return bestSlot;
    }

    @Subscribe
    public void onTick(TickEvent event) {
        this.nextToolSlot = -1;
        if (mc.player != null && mc.level != null && mc.gameMode != null && !mc.player.isCreative()) {
            this.confirmInventorySwap();
            if (this.isMining()) {
                BlockPos blockPos = this.targetedBlock();
                if (blockPos != null) {
                    this.selectToolForBlock(blockPos);
                }
                if (this.originalSelectedSlot != -1 || this.swappedHotbarSlot != -1) {
                    this.scheduleToolRestore();
                }
            } else if (this.originalSelectedSlot != -1 || this.swappedHotbarSlot != -1) {
                if (this.restoreDelayTicks > 0) {
                    this.restoreDelayTicks--;
                } else if (!this.hasManualSlotChange()) {
                    this.restoreSelectedSlot();
                    this.restoreInventorySwap();
                    this.resetPendingSwap();
                }
            }
        } else {
            this.resetPendingSwap();
            this.clearSwapRestoreState();
        }
    }

    private int findFasterHotbarSlot(BlockState blockState) {
        Inventory inventory = mc.player.getInventory();
        int toolSlot = bestHotbarSlot(blockState);
        if (toolSlot != -1 && toolSlot != inventory.getSelectedSlot()) {
            float toolSpeed = miningSpeed(inventory.getItem(toolSlot), blockState);
            float currentSpeed = miningSpeed(inventory.getItem(inventory.getSelectedSlot()), blockState);
            return toolSpeed > currentSpeed + 1.0E-4F ? toolSlot : -1;
        } else {
            return -1;
        }
    }

    private static ItemStack bestHotbarTool(BlockState blockState) {
        int toolSlot = bestHotbarSlot(blockState);
        return toolSlot == -1 ? mc.player.getMainHandItem() : mc.player.getInventory().getItem(toolSlot);
    }

    private void restoreSelectedSlot() {
        if (this.originalSelectedSlot != -1 && mc.player != null) {
            if (this.autoSelectedSlot == -1 || mc.player.getInventory().getSelectedSlot() == this.autoSelectedSlot) {
                int selectedSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(this.originalSelectedSlot);
            }
        }
    }

    private static boolean isBetterTool(ItemStack candidate, ItemStack current, float candidateSpeed, float currentSpeed, BlockState blockState) {
        if (candidateSpeed > currentSpeed + 1.0E-4F) {
            return true;
        }
        if (candidateSpeed + 1.0E-4F < currentSpeed) {
            return false;
        }
        boolean candidateHarvestsDrops = candidate.isCorrectToolForDrops(blockState);
        boolean currentHarvestsDrops = !current.isEmpty() && current.isCorrectToolForDrops(blockState);
        if (candidateHarvestsDrops != currentHarvestsDrops) {
            return candidateHarvestsDrops;
        }
        boolean candidateIsUnenchantedShears = isUnenchantedShears(candidate);
        boolean currentIsUnenchantedShears = isUnenchantedShears(current);
        return candidateIsUnenchantedShears != currentIsUnenchantedShears ? !candidateIsUnenchantedShears : false;
    }

    public static int bestHotbarSlot(BlockState blockState) {
        if (mc.player == null) {
            return -1;
        }
        int bestSlot = -1;
        ItemStack bestTool = ItemStack.EMPTY;
        float bestSpeed = miningSpeed(ItemStack.EMPTY, blockState);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack candidate = mc.player.getInventory().getItem(slot);
            float candidateSpeed = miningSpeed(candidate, blockState);
            if (isBetterTool(candidate, bestTool, candidateSpeed, bestSpeed, blockState)) {
                bestSpeed = candidateSpeed;
                bestSlot = slot;
                bestTool = candidate;
            }
        }
        return bestSlot;
    }

    public static ItemStack bestAvailableTool(BlockState blockState) {
        if (mc.player == null) {
            return ItemStack.EMPTY;
        } else {
            ItemStack hotbarTool = bestHotbarTool(blockState);
            AutoTool autoTool = ModuleManager.INSTANCE.autoTool();
            if (autoTool != null && autoTool.isEnabled() && autoTool.useInventory.isEnabled()) {
                int inventorySlot = findBetterInventoryTool(blockState, hotbarTool);
                return inventorySlot == -1 ? hotbarTool : mc.player.getInventory().getItem(inventorySlot);
            } else {
                return hotbarTool;
            }
        }
    }

    private void rememberSelectedSlot() {
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        if (this.originalSelectedSlot == -1 || selectedSlot != this.autoSelectedSlot) {
            this.originalSelectedSlot = selectedSlot;
        }
    }

    public static int activeToolSlot(BlockState blockState) {
        AutoTool autoTool = ModuleManager.INSTANCE.autoTool();
        return autoTool != null && autoTool.isEnabled() ? bestHotbarSlot(blockState) : -1;
    }

    private void restoreInventorySwap() {
        if (this.swappedHotbarSlot != -1 && mc.player != null && mc.gameMode != null) {
            if (mc.player.containerMenu == mc.player.inventoryMenu) {
                Inventory inventory = mc.player.getInventory();
                int hotbarSlot = this.swappedHotbarSlot;
                int inventorySlot = this.swappedInventorySlot;
                Item tool = this.swappedTool;
                this.clearSwapRestoreState();
                if (inventory.getItem(hotbarSlot).getItem() == tool) {
                    this.scheduleHotbarSwap(inventorySlot, hotbarSlot);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.restoreSelectedSlot();
        this.restoreInventorySwap();
        this.resetPendingSwap();
        this.clearSwapRestoreState();
        super.onDisable();
    }
}
