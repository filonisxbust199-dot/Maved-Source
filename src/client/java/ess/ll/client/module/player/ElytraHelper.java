package ess.ll.client.module.player;

import ess.ll.client.event.ElytraStartEvent;
import ess.ll.client.event.KeyInputEvent;
import ess.ll.client.event.MobEffectVisibilityEvent;
import ess.ll.client.event.MouseButtonEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.PostPlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.inventory.InventoryActionSequence;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.KeyBindSetting;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.ui.InputAction;
import ess.ll.client.ui.KeyBinding;
import ess.ll.client.util.DelayedInventoryActionManager;
import ess.ll.client.util.MovementUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class ElytraHelper extends ClientModule {
    private final ToggleSetting autoTakeoff;
    private final ModeSetting takeoffMode;
    private final KeyBindSetting swapKey;
    private final KeyBindSetting fireworkKey;
    private final ToggleSetting legitimateSwap;
    private final ToggleSetting legitimateUse;
    private final ToggleSetting takeoffWithLevitation;
    private boolean swapRequested;
    private boolean fireworkRequested;
    private int airborneTicks;
    private int jumpToggleTicks;
    private int flightTicks;
    private int failedTakeoffs;
    private boolean wasGliding;
    private int swapCooldownTicks;
    private InventoryActionSequence pendingSwap;
    private static final float KEEP_YAW = Float.MAX_VALUE;
    private static final float KEEP_PITCH = Float.MAX_VALUE;

    public boolean isAutoTakeoffActive() {
        return this.isEnabled() && this.autoTakeoff.isEnabled() && this.canStartGliding();
    }

    @Subscribe
    private void onKeyInput(KeyInputEvent event) {
        this.handleKey(event.getKey(), event.getAction());
    }

    @Subscribe
    private void onMouseButton(MouseButtonEvent event) {
        this.handleKey(ess.ll.client.command.Keys.MOUSE_BASE + event.getButton(), event.getAction());
    }

    private void swapChestSlot(Slot slot) {
        this.swapCooldownTicks = 5;
        this.pendingSwap = this.legitimateSwap.isEnabled() ? DelayedInventoryActionManager.INSTANCE.swapSlots(slot.index, 6) : InventoryUtils.scheduleSwap(slot.index, 6);
        if (this.pendingSwap != null) {
            this.pendingSwap.awaitServerConfirmation();
        }
    }

    private Slot findChestplate() {
        Item[] item = new Item[] { Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE };
        for (Item item2 : item) {
            Slot slot = InventoryUtils.findSlot(item2);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    public void toggleElytra() {
        if (mc.player == null || this.isSwapPending()) {
            return;
        }
        Slot slot;
        if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            slot = this.findChestplate();
            if (slot == null) {
                slot = this.findEmptyInventorySlot();
            }
        } else {
            slot = InventoryUtils.findSlot(Items.ELYTRA);
        }
        if (slot != null) {
            this.swapChestSlot(slot);
        }
    }

    public void useFirework() {
        if (mc.player != null) {
            if (!mc.player.isFallFlying() && !mc.player.onGround() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && mc.player.tryToStartFallFlying()) {
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            if (mc.player.isFallFlying()) {
                Slot slot = InventoryUtils.findSlot(Items.FIREWORK_ROCKET);
                if (slot != null) {
                    if (this.legitimateUse.isEnabled()) {
                        DelayedInventoryActionManager.INSTANCE.useSlot(InteractionHand.OFF_HAND, slot);
                    } else {
                        // Use the same slot-aware action for hotbar and inventory rockets.
                        // The old hotbar shortcut only sent a carried-slot packet and could
                        // use the previously selected item on 26.3.
                        InventoryUtils.useFromSlot(slot, InteractionHand.MAIN_HAND, KEEP_YAW, KEEP_PITCH, false);
                    }
                }
            }
        }
    }

    @Subscribe
    private void onPostPlayerInput(PostPlayerInputEvent event) {
        if (mc.player != null) {
            if (mc.player.isFallFlying()) {
                this.flightTicks++;
            } else {
                this.flightTicks = 0;
            }
        }
    }

    private boolean canRetryTakeoff() {
        return this.airborneTicks >= 1 && this.failedTakeoffs < 3 && !MovementUtils.willLand(mc.player) && this.hasTakeoffClearance();
    }

    @Subscribe
    private void onMobEffectVisibility(MobEffectVisibilityEvent event) {
        if (mc.player != null) {
            if (this.takeoffWithLevitation.getValue() && !mc.player.isFallFlying() && event.getEffect() == MobEffects.LEVITATION.value()) {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void onElytraStart(ElytraStartEvent event) {
        if (this.autoTakeoff.isEnabled() && this.canStartGliding()) {
            if (!mc.player.onGround()) {
                if (this.swapCooldownTicks <= 0) {
                    if (this.takeoffMode.is("Рейджовый")) {
                        event.requestStart();
                    } else if (this.canRetryTakeoff()) {
                        if (mc.player.input.keyPresses.jump() && !mc.player.getLastSentInput().jump()) {
                            event.requestStart();
                        }
                    }
                }
            }
        }
    }

    public ElytraHelper() {
        super(ModuleCategory.Player, ModuleSubCategory.Main);
        this.autoTakeoff = new ToggleSetting("Авто Взлет").withDefault(false).withContainer(this);
        this.takeoffMode = new ModeSetting("Режим взлёта").addModes("Легитный", "Рейджовый").withDefault("Легитный").withVisibility(() -> this.autoTakeoff.isEnabled()).withContainer(this);
        this.swapKey = new KeyBindSetting("Кнопка свапа").withDefault(new KeyBinding()).withContainer(this);
        this.fireworkKey = new KeyBindSetting("Кнопка фейера").withDefault(new KeyBinding()).withContainer(this);
        this.legitimateSwap = new ToggleSetting("Легитный свап").withDefault(false).withContainer(this);
        this.legitimateUse = new ToggleSetting("Легитное использование").withDefault(false).withContainer(this);
        this.takeoffWithLevitation = new ToggleSetting("Взлетать с левитацией").withDefault(false).withVisibility(() -> this.autoTakeoff.isEnabled()).withContainer(this);
        this.swapRequested = false;
        this.fireworkRequested = false;
        this.jumpToggleTicks = 2;
        this.setName("Elytra Helper");
    }

    private void handleKey(int key, InputAction inputAction) {
        if (inputAction != InputAction.RELEASE) {
            if (this.swapKey.getValue().getKey() == key) {
                this.swapRequested = true;
            }
            if (this.fireworkKey.getValue().getKey() == key) {
                this.fireworkRequested = true;
            }
        }
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player != null) {
            if (this.swapCooldownTicks > 0) {
                this.swapCooldownTicks--;
            }
            if (this.swapRequested) {
                this.toggleElytra();
            }
            if (this.fireworkRequested) {
                this.useFirework();
            }
            this.fireworkRequested = false;
            this.swapRequested = false;
        }
    }

    private Slot findEmptyInventorySlot() {
        if (mc.player == null) {
            return null;
        }
        AbstractContainerMenu menu = mc.player.containerMenu;
        int startSlot = inventoryStartIndex(menu.slots.size());
        for (int slotIndex = startSlot; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.getSlot(slotIndex);
            if (slot.getItem().isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    public void trackFlightEnd() {
        if (mc.player != null) {
            boolean fallFlying = mc.player.isFallFlying();
            if (this.wasGliding && !fallFlying) {
                if (this.flightTicks < 10) {
                    this.failedTakeoffs++;
                } else {
                    this.failedTakeoffs = 0;
                }
            }
            this.wasGliding = fallFlying;
        }
    }

    private boolean hasTakeoffClearance() {
        AABB bounds = mc.player.getBoundingBox().expandTowards(0.0, -0.6, 0.0);
        return mc.player.level().noCollision(mc.player, bounds);
    }

    private boolean isSwapPending() {
        return this.pendingSwap != null && !this.pendingSwap.isStarted() && InventoryActionManager.INSTANCE.isBusy();
    }

    private boolean canStartGliding() {
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }
        if (!player.isFallFlying() && !player.isSpectator() && !player.getAbilities().flying) {
            if (!player.isPassenger() && !player.onClimbable() && !player.isInWater()) {
                if (player.hasEffect(MobEffects.LEVITATION)) {
                    return false;
                }
                for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
                    if (LivingEntity.canGlideUsing(player.getItemBySlot(equipmentSlot), equipmentSlot)) {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Subscribe(3)
    public void onPlayerInput(PlayerInputEvent event) {
        if (this.autoTakeoff.isEnabled() && this.canStartGliding()) {
            if (this.swapCooldownTicks <= 0) {
                boolean onGround = mc.player.onGround();
                if (onGround) {
                    this.airborneTicks = 0;
                    this.jumpToggleTicks = 2;
                    this.failedTakeoffs = 0;
                } else {
                    this.airborneTicks++;
                }
                if (this.takeoffMode.is("Рейджовый")) {
                    if (onGround && !event.isJump()) {
                        event.setJump(true);
                    }
                } else {
                    if (this.jumpToggleTicks < 2) {
                        this.jumpToggleTicks++;
                    }
                    boolean previousJump = mc.player.getLastSentInput().jump();
                    if (onGround) {
                        if (!event.isJump()) {
                            event.setJump(true);
                        }
                    } else if (this.canRetryTakeoff() && this.jumpToggleTicks >= 2) {
                        if (previousJump) {
                            event.setJump(false);
                        } else {
                            event.setJump(true);
                            this.jumpToggleTicks = 0;
                        }
                    } else if (previousJump) {
                        event.setJump(false);
                    }
                    event.isJump();
                }
            }
        }
    }

    private static int inventoryStartIndex(int slotCount) {
        return Math.max(0, slotCount - 37);
    }
}
