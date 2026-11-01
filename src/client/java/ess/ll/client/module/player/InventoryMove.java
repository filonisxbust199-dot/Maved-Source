package ess.ll.client.module.player;

import ess.ll.client.event.ContainerClickEvent;
import ess.ll.client.event.ContainerCloseEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.inventory.InventoryInputPolicy;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.MovementUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class InventoryMove extends ClientModule {
    public KeyMapping[] movementKeys;
    private final List<InventoryMove.QueuedClick> pendingClicks = new CopyOnWriteArrayList<>();
    private final List<ItemStack> savedSlots = new ArrayList<>();
    private ItemStack savedCarriedItem = ItemStack.EMPTY;
    private int savedContainerId = -1;
    private boolean replayingClicks = false;
    public final ToggleSetting unusedOption;
    public final ToggleSetting matrixBypass;

    public InventoryMove() {
        super(ModuleCategory.Player, ModuleSubCategory.Main);
        this.unusedOption = new ToggleSetting("Замедлять").withDefault(false).withContainer(this);
        this.matrixBypass = new ToggleSetting("Обход Matrix").withDefault(false).withContainer(this);
        this.setName("Inventory Move");
    }

    private void clearSnapshot() {
        this.savedSlots.clear();
        this.savedCarriedItem = ItemStack.EMPTY;
        this.savedContainerId = -1;
    }

    @Subscribe
    public void onContainerClick(ContainerClickEvent event) {
        if (this.unusedOption.isEnabled()) {
            if (mc.gui.screen() instanceof InventoryScreen) {
                if (!this.replayingClicks) {
                    if (MovementUtils.isMoving() || !this.pendingClicks.isEmpty()) {
                        this.queueClick(event.getContainerId(), event.getSlot(), event.getButton(), event.getInput());
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    public int delayTicks(int var1) {
        return this.matrixBypass.isEnabled() ? var1 + 1 : var1;
    }

    private void restoreSnapshot() {
        if (this.savedContainerId != -1) {
            if (mc.player != null) {
                AbstractContainerMenu containerMenu2 = mc.player.containerMenu;
                if (matchesSnapshot(this.savedContainerId, containerMenu2.containerId, this.savedSlots.size(), containerMenu2.slots.size())) {
                    for (int var2 = 0; var2 < containerMenu2.slots.size(); var2++) {
                        containerMenu2.slots.get(var2).set(this.savedSlots.get(var2));
                    }
                }
            }
        }
    }

    private void queueClick(int var1, int var2, int var3, ContainerInput containerInput) {
        if (mc.player != null) {
            if (this.pendingClicks.isEmpty()) {
                this.captureSnapshot();
            }
            this.pendingClicks.add(new InventoryMove.QueuedClick(var1, var2, var3, containerInput));
            mc.player.containerMenu.clicked(var2, var3, containerInput, mc.player);
        }
    }

    private static boolean matchesSnapshot(int var0, int var1, int var2, int var3) {
        return var1 == var0 && var2 == var3;
    }

    private boolean isMovementKeyDown() {
        this.initializeMovementKeys();
        Window window = mc.getWindow();
        return Arrays.stream(this.movementKeys).anyMatch(var1x -> InputConstants.isKeyDown(var1x.getDefaultKey().getValue()));
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (this.unusedOption.isEnabled()) {
            if (mc.gui.screen() instanceof InventoryScreen && event.getPacket() instanceof ClientboundContainerClosePacket) {
                event.setCancelled(true);
            }
        }
    }

    @Subscribe
    public void onContainerClose(ContainerCloseEvent event) {
        if (this.unusedOption.isEnabled()) {
            if (event.getScreen() instanceof InventoryScreen) {
                event.cancel();
                this.syncMovementKeys();
                InventoryActionManager.INSTANCE.enqueueClick(() -> {
                    if (!this.pendingClicks.isEmpty()) {
                        this.replayingClicks = true;
                        try {
                            this.restoreSnapshot();
                            this.pendingClicks.forEach(var0 -> mc.gameMode.handleContainerInput(var0.containerId(), var0.slotNum(), var0.buttonNum(), var0.input(), mc.player));
                            this.finishClickReplay();
                        } finally {
                            this.replayingClicks = false;
                        }
                        this.pendingClicks.clear();
                    }
                    this.clearSnapshot();
                    mc.player.connection.send(new ServerboundContainerClosePacket(event.getContainerId()));
                    mc.player.clientSideCloseContainer();
                }, 1, true);
            }
        }
    }

    private void initializeMovementKeys() {
        if (this.movementKeys == null) {
            this.movementKeys = new KeyMapping[] { mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump };
        }
    }

    private void releaseMovementKeys() {
        Arrays.stream(this.movementKeys).forEach(var0 -> var0.setDown(false));
    }

    private void captureSnapshot() {
        if (mc.player != null) {
            AbstractContainerMenu containerMenu2 = mc.player.containerMenu;
            this.savedSlots.clear();
            for (int var2 = 0; var2 < containerMenu2.slots.size(); var2++) {
                this.savedSlots.add(containerMenu2.slots.get(var2).getItem().copy());
            }
            this.savedContainerId = containerMenu2.containerId;
        }
    }

    private void syncMovementKeys() {
        this.initializeMovementKeys();
        Window window = mc.getWindow();
        Arrays.stream(this.movementKeys).forEach(var1x -> var1x.setDown(InputConstants.isKeyDown(var1x.getDefaultKey().getValue())));
    }

    private void finishClickReplay() {
        if (mc.player != null) {
            AbstractContainerMenu containerMenu2 = mc.player.containerMenu;
        }
    }

    public boolean isMoving() {
        return this.isEnabled() && (this.unusedOption.isEnabled() || this.matrixBypass.isEnabled()) && (MovementUtils.isMoving() || this.isMovementKeyDown());
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        this.initializeMovementKeys();
        if (mc.gui.screen() != null) {
            if (InventoryInputPolicy.consumesMovementKeys(mc.gui.screen())) {
                this.releaseMovementKeys();
            } else {
                this.syncMovementKeys();
            }
        }
    }

    private record QueuedClick(int containerId, int slotNum, int buttonNum, ContainerInput input) {
    }
}
