package ess.ll.client.module.player;

import ess.ll.client.core.FreeLookController;
import ess.ll.client.event.KeyInputEvent;
import ess.ll.client.event.MouseButtonEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PreMovementPacketEvent;
import ess.ll.client.setting.KeyBindSetting;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.ui.InputAction;
import ess.ll.client.ui.KeyBinding;
import ess.ll.client.util.DelayedInventoryActionManager;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public class ClickPearl extends ClientModule {
    private final ModeSetting throwMode;
    private final KeyBindSetting throwKey;
    private boolean throwRequested;

    @Subscribe
    private void onMouseButton(MouseButtonEvent event) {
        this.handleKey(ess.ll.client.command.Keys.MOUSE_BASE + event.getButton(), event.getAction());
    }

    private void handleKey(int var1, InputAction inputAction) {
        if (inputAction != InputAction.RELEASE) {
            if (var1 == this.throwKey.getValue().getKey()) {
                this.throwRequested = true;
            }
        }
    }

    public ClickPearl() {
        super(ModuleCategory.Player, ModuleSubCategory.Other);
        this.throwMode = new ModeSetting("Моды").addModes("Легитно", "Рейджово").withDefault("Рейджово").withContainer(this);
        this.throwKey = new KeyBindSetting("Кнопка перла").withDefault(new KeyBinding(ess.ll.client.command.Keys.MOUSE_BASE + 2)).withContainer(this);
        this.throwRequested = false;
        this.setName("Click Pearl");
    }

    @Subscribe
    private void onKeyInput(KeyInputEvent event) {
        this.handleKey(event.getKey(), event.getAction());
    }

    @Subscribe
    private void onPreMovementPacket(PreMovementPacketEvent event) {
        if (mc.player != null && mc.level != null) {
            if (this.throwRequested) {
                this.throwPearl(event, FreeLookController.getYaw(), FreeLookController.getPitch());
                this.throwRequested = false;
            }
        }
    }

    public boolean throwPearl(PreMovementPacketEvent event, float var2, float var3) {
        if (mc.player == null) {
            return false;
        }
        Slot slot = InventoryUtils.findSlot(Items.ENDER_PEARL);
        if (slot != null && !slot.getItem().isEmpty()) {
            if (mc.player.getCooldowns().isOnCooldown(slot.getItem())) {
                return false;
            } else {
                var3 = clampThrowPitch(var3);
                if (this.throwMode.is("Легитно")) {
                    DelayedInventoryActionManager.INSTANCE.useItem(Items.ENDER_PEARL, var2, var3);
                    return true;
                } else {
                    DelayedInventoryActionManager.INSTANCE.useItemNow(Items.ENDER_PEARL, var2, var3);
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private static float clampThrowPitch(float var0) {
        return Math.max((-89.0F), Math.min(89.0F, var0));
    }
}
