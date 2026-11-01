package ess.ll.client.module.combat;

import ess.ll.client.event.Subscribe;
import ess.ll.client.core.TickTaskScheduler;
import ess.ll.client.event.TickEvent;
import ess.ll.client.event.WorldLoadEvent;
import ess.ll.client.features.combat.ExplosionDamageUtils;
import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.inventory.InventoryActionSequence;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.features.world.WorldCollisionUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.setting.MultiToggleSetting;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ModuleManager;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AutoTotem extends ClientModule {
    private final MultiToggleSetting dangerChecks;
    private final NumberSetting<Float> healthThreshold;
    private final NumberSetting<Float> elytraHealthThreshold;
    private final ToggleSetting restoreOffhand;
    private final NumberSetting<Integer> restoreSwapSpeed;
    private final ToggleSetting preserveOrb;
    private final NumberSetting<Float> orbHealthThreshold;
    private final ToggleSetting cancelItemUse;
    private final ToggleSetting preserveEnchantedTotem;
    private final NumberSetting<Float> crystalRange;
    private final NumberSetting<Float> anchorRange;
    private final NumberSetting<Float> minecartRange;
    private final NumberSetting<Float> tntRange;
    private final BlockingQueue<AutoTotem.ExplosionNotice> pendingExplosions;
    private final MutableBlockPos searchPosition;
    private ItemStack restoreItem;
    private int restoreSlot;
    private int previousRestoreSlot;
    private int restoreDelay;
    private int missingRestoreTicks;
    private int restoreAttempts;
    private boolean restoreNeedsVerification;
    private volatile int trackedSwapSlot;
    private volatile long trackingDeadlineMillis;
    private volatile ItemStack pendingSlotUpdate;
    private int lastRestoreSlot;
    private InventoryActionSequence swapSequence;
    private boolean emergencySwap;
    private int pendingSwapTicks;
    private boolean dangerDetected;
    private volatile float predictedHealth;
    private volatile boolean damagePending;

    private boolean isContainerScreenOpen() {
        return mc.gui.screen() instanceof AbstractContainerScreen;
    }

    private boolean isHealthLow() {
        float var1 = mc.player.isFallFlying() ? this.elytraHealthThreshold.getValue() : this.healthThreshold.getValue();
        return this.effectiveHealth() <= var1;
    }

    private boolean hasTotemInOffhand() {
        return mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
    }

    @Subscribe(0)
    private void onTick(TickEvent event) {
        if (mc.player != null && mc.level != null && mc.player.isAlive()) {
            this.expirePendingSwap();
            this.updateRestoreTracking();
            this.dangerDetected = this.consumeNearbyExplosions() || this.needsTotem();
            boolean var2 = this.damagePending;
            this.damagePending = false;
            if (var2) {
                this.restoreDelay = 100;
            } else if (this.dangerDetected) {
                this.restoreDelay = Math.max(this.restoreDelay, 20);
            } else if (this.restoreDelay > 0) {
                this.restoreDelay--;
            }
            if (this.dangerDetected) {
                this.previousRestoreSlot = -1;
                if (this.hasTotemInOffhand() || this.isEmergencySwapPending() || this.scheduleEmergencySwap()) {
                    return;
                }
            }
            if (!this.hasPendingSwap()) {
                if (this.restoreOffhand.isEnabled()) {
                    this.updateRestore();
                } else {
                    this.clearRestoreState();
                }
            }
        } else {
            this.resetState();
        }
    }

    private void swapOffhand(int var1) {
        if (this.swapSequence != null) {
            this.swapSequence.lockSlots(45, var1);
        }
        InventoryUtils.handleContainerClick(mc.player.containerMenu.containerId, var1, 40, ContainerInput.SWAP, mc.player);
    }

    private float effectiveHealth() {
        float var1 = Math.min(mc.player.getHealth(), this.predictedHealth);
        if (this.dangerChecks.isSelected("Золотые сердца") && mc.player.hasEffect(MobEffects.ABSORPTION)) {
            var1 += mc.player.getAbsorptionAmount();
        }
        return var1;
    }

    private void equipTotem() {
        if (mc.player != null && !this.hasTotemInOffhand()) {
            Slot slot = this.findTotem();
            if (slot != null) {
                this.swapOffhand(slot.index);
                this.beginRestoreTracking(slot.index);
            }
        }
    }

    private void restoreOffhandItem() {
        if (mc.player != null && !this.dangerDetected && this.pendingExplosions.isEmpty() && !this.restoreItem.isEmpty() && !this.isOffhandRestored() && !this.hasOtherOffhandItem() && !this.isContainerScreenOpen()) {
            int var1 = this.resolveRestoreSlot();
            if (var1 != -1) {
                this.restoreAttempts++;
                this.swapOffhand(var1);
                this.lastRestoreSlot = var1;
                this.restoreNeedsVerification = true;
            }
        }
    }

    private void updateRestore() {
        if (!this.restoreItem.isEmpty() && !this.isContainerScreenOpen()) {
            if (this.restoreNeedsVerification) {
                this.restoreNeedsVerification = false;
                if (!this.wasRestoreSwappedBack()) {
                    this.clearRestoreState();
                    return;
                }
            }
            if (this.isOffhandRestored()) {
                this.clearRestoreState();
            } else if (!this.hasOtherOffhandItem() && this.canFindRestoreItem()) {
                this.missingRestoreTicks = 0;
                if (this.resolveRestoreSlot() != -1 && this.restoreDelay <= 0) {
                    if (this.previousRestoreSlot < 0) {
                        this.previousRestoreSlot = Math.max(0, 10 - this.restoreSwapSpeed.getValue());
                    }
                    if (this.previousRestoreSlot > 0) {
                        this.previousRestoreSlot--;
                    } else {
                        this.previousRestoreSlot = Math.min(100, 2 << Math.min(this.restoreAttempts, 6));
                        this.trackSwap(InventoryActionManager.INSTANCE.enqueuePriorityClick(this::restoreOffhandItem, 1, this.isPlayerMenu()).awaitServerConfirmation(), false);
                    }
                }
            } else {
                if (++this.missingRestoreTicks > 20) {
                    this.clearRestoreState();
                }
            }
        }
    }

    private boolean isStandingOnObsidian() {
        if (!this.dangerChecks.isSelected("Обсидиан")) {
            return false;
        }
        AABB boundingBox = mc.player.getBoundingBox();
        int var2 = Mth.floor(boundingBox.minY - 0.03);
        for (int var3 = Mth.floor(boundingBox.minX); var3 <= Mth.floor(boundingBox.maxX); var3++) {
            for (int var4 = Mth.floor(boundingBox.minZ); var4 <= Mth.floor(boundingBox.maxZ); var4++) {
                for (int var5 = var2; var5 > var2 - 2; var5--) {
                    if (mc.level.getBlockState(this.searchPosition.set(var3, var5, var4)).is(Blocks.OBSIDIAN)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasOtherOffhandItem() {
        ItemStack offhandItem = mc.player.getOffhandItem();
        return !offhandItem.isEmpty() && offhandItem.getItem() != Items.TOTEM_OF_UNDYING;
    }

    private boolean hasDangerousAnchorNearby() {
        if (!this.dangerChecks.isSelected("Якорь")) {
            return false;
        }
        float value = this.anchorRange.getValue();
        double var2 = value * value;
        int var4 = Mth.ceil(value);
        Vec3 eyePosition = mc.player.getEyePosition();
        AABB boundingBox = mc.player.getBoundingBox();
        BlockPos blockPos = mc.player.blockPosition();
        for (int var8 = -var4; var8 <= var4; var8++) {
            for (int var9 = -var4; var9 <= var4; var9++) {
                for (int var10 = -var4; var10 <= var4; var10++) {
                    MutableBlockPos blockPos2 = this.searchPosition.set(blockPos.getX() + var8, blockPos.getY() + var9, blockPos.getZ() + var10);
                    BlockState blockState = mc.level.getBlockState(blockPos2);
                    if (blockState.is(Blocks.RESPAWN_ANCHOR) && blockState.getValue(RespawnAnchorBlock.CHARGE) != 0 && !(ExplosionDamageUtils.distanceSquaredToBlock(eyePosition, blockPos2) > var2) && this.doesAnchorExplode(blockPos2) && ExplosionDamageUtils.explosionDamage(Vec3.atCenterOf(blockPos2), mc.player, boundingBox, -1.0F, 10.0) > 0.0F) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSwapInProgress() {
        AutoTotem autoTotem2 = ModuleManager.INSTANCE.autoTotem();
        return autoTotem2 != null && autoTotem2.isBusy();
    }

    private boolean needsTotem() {
        return this.shouldPreserveOrb() ? this.isHealthLow() : this.isHealthLow() || this.hasCrystalNearby() || this.hasDangerousAnchorNearby() || this.isStandingOnObsidian() || this.dangerChecks.isSelected("Вагонетка с тнт") && WorldCollisionUtils.hasEntityNearPlayer(MinecartTNT.class, this.minecartRange.getValue().floatValue()) || this.dangerChecks.isSelected("Обычный тнт") && WorldCollisionUtils.hasEntityNearPlayer(PrimedTnt.class, this.tntRange.getValue().floatValue()) || this.isFallingDangerously();
    }

    private void resetState() {
        this.swapSequence = null;
        this.emergencySwap = false;
        this.pendingSwapTicks = 0;
        this.dangerDetected = false;
        this.predictedHealth = Float.MAX_VALUE;
        this.damagePending = false;
        this.trackedSwapSlot = -1;
        this.trackingDeadlineMillis = 0L;
        this.pendingSlotUpdate = null;
        this.lastRestoreSlot = -1;
        this.pendingExplosions.clear();
        this.clearRestoreState();
    }

    private boolean shouldPreserveOrb() {
        if (!this.preserveOrb.isEnabled() || mc.player.getOffhandItem().getItem() != Items.PLAYER_HEAD) {
            return false;
        } else {
            return this.dangerChecks.isSelected("Падение") && mc.player.fallDistance > 5.0 ? false : this.effectiveHealth() > this.orbHealthThreshold.getValue();
        }
    }

    private boolean hasCrystalNearby() {
        return this.dangerChecks.isSelected("Кристаллы") && WorldCollisionUtils.hasEntityNearPlayer(EndCrystal.class, this.crystalRange.getValue().floatValue());
    }

    public AutoTotem() {
        super(ModuleCategory.Combat, ModuleSubCategory.Other);
        this.dangerChecks = new MultiToggleSetting("Проверки на").addOption(NamedOption.of("Золотые сердца", true)).addOption(NamedOption.of("Кристаллы", true)).addOption(NamedOption.of("Якорь", true)).addOption(NamedOption.of("Обсидиан", false)).addOption(NamedOption.of("Падение", false)).addOption(NamedOption.of("Обычный тнт", false)).addOption(NamedOption.of("Вагонетка с тнт", false)).withContainer(this);
        this.healthThreshold = new NumberSetting<Float>("Уровень здоровья").withMin(1.0F).withMax(20.0F).withStep(0.5F).withDefault(4.0F).withContainer(this);
        this.elytraHealthThreshold = new NumberSetting<Float>("Здоровье на элитре").withMin(1.0F).withMax(20.0F).withStep(0.5F).withDefault(10.0F).withContainer(this);
        this.restoreOffhand = new ToggleSetting("Возвращать предмет").withDefault(false).withContainer(this);
        this.restoreSwapSpeed = new NumberSetting<Integer>("Скорость Обратного свапа").withMin(1).withMax(10).withStep(1).withDefault(6).withVisibility(this.restoreOffhand::isEnabled).withContainer(this);
        this.preserveOrb = new ToggleSetting("Не менять шар").withDefault(false).withContainer(this);
        this.orbHealthThreshold = new NumberSetting<Float>("Ур. здоровья с шаром").withMin(1.0F).withMax(20.0F).withStep(0.5F).withDefault(12.0F).withVisibility(this.preserveOrb::isEnabled).withContainer(this);
        this.cancelItemUse = new ToggleSetting("Отменять использование предмета").withDefault(false).withContainer(this);
        this.preserveEnchantedTotem = new ToggleSetting("Сохранять зачарованный").withDefault(false).withContainer(this);
        this.crystalRange = new NumberSetting<Float>("Дистанция от кристалла").withMin(1.0F).withMax(8.0F).withStep(0.5F).withDefault(8.0F).withVisibility(() -> this.dangerChecks.isSelected("Кристаллы")).withContainer(this);
        this.anchorRange = new NumberSetting<Float>("Дистанция до якоря").withMin(1.0F).withMax(10.0F).withStep(0.5F).withDefault(6.0F).withVisibility(() -> this.dangerChecks.isSelected("Якорь")).withContainer(this);
        this.minecartRange = new NumberSetting<Float>("Дистанция до вагонетки").withMin(1.0F).withMax(10.0F).withStep(0.5F).withDefault(6.0F).withVisibility(() -> this.dangerChecks.isSelected("Вагонетка с тнт")).withContainer(this);
        this.tntRange = new NumberSetting<Float>("Дистанция до тнт").withMin(3.0F).withMax(50.0F).withStep(1.0F).withDefault(10.0F).withVisibility(() -> this.dangerChecks.isSelected("Обычный тнт")).withContainer(this);
        this.pendingExplosions = new ArrayBlockingQueue<>(32);
        this.searchPosition = new MutableBlockPos();
        this.restoreItem = ItemStack.EMPTY;
        this.restoreSlot = -1;
        this.previousRestoreSlot = -1;
        this.trackedSwapSlot = -1;
        this.lastRestoreSlot = -1;
        this.predictedHealth = Float.MAX_VALUE;
        this.setName("Auto Totem");
    }

    private boolean hasPendingSwap() {
        return this.swapSequence != null && this.swapSequence.hasNext() && this.pendingSwapTicks <= 20;
    }

    private boolean canFindRestoreItem() {
        return this.findRestoreSlot(false, true) != -1;
    }

    private void updateRestoreTracking() {
        if (this.trackingDeadlineMillis != 0L) {
            if (System.currentTimeMillis() > this.trackingDeadlineMillis) {
                this.trackingDeadlineMillis = 0L;
                this.trackedSwapSlot = -1;
                this.pendingSlotUpdate = null;
            } else {
                ItemStack stack = this.pendingSlotUpdate;
                if (stack != null) {
                    this.pendingSlotUpdate = null;
                    this.rememberRestoreItem(stack);
                }
            }
        }
    }

    @Subscribe(0)
    private void onPacket(PacketEvent event) {
        if (event.isReceive()) {
            if (event.getPacket() instanceof ClientboundSetHealthPacket var8) {
                this.predictedHealth = var8.getHealth();
            } else if (event.getPacket() instanceof ClientboundDamageEventPacket var7) {
                if (mc.player != null && var7.entityId() == mc.player.getId() && isExplosionDamage(var7.sourceType())) {
                    this.damagePending = true;
                }
            } else if (event.getPacket() instanceof ClientboundContainerSetContentPacket var6) {
                if (var6.containerId() == 0 && this.trackedSwapSlot >= 0 && this.trackedSwapSlot < var6.items().size()) {
                    this.trackSlotUpdate(var6.items().get(this.trackedSwapSlot));
                }
            } else if (event.getPacket() instanceof ClientboundContainerSetSlotPacket var5) {
                if (var5.getContainerId() == 0 && var5.getSlot() == this.trackedSwapSlot) {
                    this.trackSlotUpdate(var5.getItem());
                }
            } else if (event.getPacket() instanceof ClientboundBlockUpdatePacket var4) {
                BlockPos pos = var4.getPos();
                if (this.dangerChecks.isSelected("Якорь") && var4.getBlockState().is(Blocks.RESPAWN_ANCHOR) && this.doesAnchorExplode(pos)) {
                    this.pendingExplosions.offer(new AutoTotem.ExplosionNotice(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.anchorRange.getValue()));
                }
            } else if (event.getPacket() instanceof ClientboundAddEntityPacket var2) {
                float var14 = this.dangerRange(var2.getType());
                if (var14 > 0.0F) {
                    this.pendingExplosions.offer(new AutoTotem.ExplosionNotice(var2.getX(), var2.getY(), var2.getZ(), var14));
                }
            }
        }
    }

    private void clearRestoreState() {
        this.restoreItem = ItemStack.EMPTY;
        this.restoreDelay = 0;
        this.restoreSlot = -1;
        this.previousRestoreSlot = -1;
        this.missingRestoreTicks = 0;
        this.restoreAttempts = 0;
        this.restoreNeedsVerification = false;
    }

    private float dangerRange(EntityType<?> entityType) {
        if (entityType == EntityTypes.END_CRYSTAL) {
            return this.dangerChecks.isSelected("Кристаллы") ? this.crystalRange.getValue() : 0.0F;
        } else if (entityType == EntityTypes.TNT) {
            return this.dangerChecks.isSelected("Обычный тнт") ? this.tntRange.getValue() : 0.0F;
        } else if (entityType == EntityTypes.TNT_MINECART) {
            return this.dangerChecks.isSelected("Вагонетка с тнт") ? this.minecartRange.getValue() : 0.0F;
        } else {
            return 0.0F;
        }
    }

    private boolean matchesRestoreItem(ItemStack stack, boolean var2) {
        return !stack.isEmpty() && stack.getItem() == this.restoreItem.getItem() ? !var2 || ItemStack.isSameItemSameComponents(stack, this.restoreItem) : false;
    }

    private static boolean isExplosionDamage(Holder<DamageType> holder) {
        return holder.is(DamageTypes.EXPLOSION) || holder.is(DamageTypes.PLAYER_EXPLOSION) || holder.is(DamageTypes.BAD_RESPAWN_POINT);
    }

    private boolean consumeNearbyExplosions() {
        boolean var1 = false;
        AutoTotem.ExplosionNotice var2;
        while ((var2 = this.pendingExplosions.poll()) != null) {
            var1 |= mc.player.distanceToSqr(var2.x(), var2.y(), var2.z()) <= var2.range() * var2.range();
        }
        return var1 && !this.shouldPreserveOrb();
    }

    private void rememberRestoreItem(ItemStack stack) {
        if (stack.getItem() != Items.TOTEM_OF_UNDYING) {
            if (stack.isEmpty()) {
                if (this.restoreSlot == this.trackedSwapSlot) {
                    this.clearRestoreState();
                }
            } else {
                this.restoreItem = stack.copy();
                this.restoreSlot = this.trackedSwapSlot;
                this.missingRestoreTicks = 0;
                this.restoreAttempts = 0;
                this.restoreNeedsVerification = false;
                this.previousRestoreSlot = -1;
            }
        }
    }

    private boolean doesAnchorExplode(BlockPos blockPos) {
        return mc.level != null && !mc.level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, blockPos);
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return slot.container == mc.player.getInventory() && slot.getContainerSlot() != 40;
    }

    private Slot findTotem() {
        boolean enabled = this.preserveEnchantedTotem.isEnabled();
        Slot slot = null;
        for (Slot slot2 : mc.player.containerMenu.slots) {
            if (this.isPlayerInventorySlot(slot2)) {
                ItemStack item = slot2.getItem();
                if (item.getItem() == Items.TOTEM_OF_UNDYING) {
                    if (!enabled || !InventoryUtils.hasCustomProperties(item)) {
                        return slot2;
                    }
                    if (slot == null) {
                        slot = slot2;
                    }
                }
            }
        }
        return slot;
    }

    private void trackSwap(InventoryActionSequence inventoryActionSequence, boolean var2) {
        this.swapSequence = inventoryActionSequence;
        this.emergencySwap = var2;
        this.pendingSwapTicks = 0;
        // Apply InventoryWalk's configured slowdown to both the emergency swap
        // and the later restore. Its scheduler clears this when the module is off.
        var inventoryMove = ModuleManager.INSTANCE.inventoryMove();
        if (inventoryMove.isMoving()) {
            TickTaskScheduler.setFreezeTicks(Math.max(TickTaskScheduler.getFreezeTicks(), inventoryMove.delayTicks(2)));
        }
    }

    private void trackSlotUpdate(ItemStack stack) {
        if (System.currentTimeMillis() <= this.trackingDeadlineMillis && mc.player != null) {
            NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
            int var3 = this.trackedSwapSlot;
            if (var3 >= 0 && var3 < slots2.size() && !ItemStack.isSameItem(stack, ((Slot) slots2.get(var3)).getItem())) {
                this.pendingSlotUpdate = stack.copy();
            }
        }
    }

    private boolean scheduleEmergencySwap() {
        Slot slot = this.findTotem();
        if (slot == null) {
            return false;
        }
        if (this.cancelItemUse.isEnabled() && mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
        }
        this.trackSwap(InventoryActionManager.INSTANCE.interruptWithClick(this::equipTotem, 1, this.isPlayerMenu()).awaitServerConfirmation().beforeMovementPacket(), true);
        return true;
    }

    private boolean isEmergencySwapPending() {
        return this.emergencySwap && this.hasPendingSwap();
    }

    private boolean wasRestoreSwappedBack() {
        Slot slot = this.getPlayerSlot(this.lastRestoreSlot);
        return slot != null && this.matchesRestoreItem(slot.getItem(), false) && this.hasTotemInOffhand();
    }

    public boolean isBusy() {
        if (!this.isEnabled() || mc.player == null || mc.level == null) {
            return false;
        } else {
            return this.hasPendingSwap() ? true : this.dangerDetected && !this.hasTotemInOffhand() && this.findTotem() != null;
        }
    }

    @Override
    public void onDisable() {
        this.resetState();
        super.onDisable();
    }

    private void beginRestoreTracking(int var1) {
        if (this.restoreOffhand.isEnabled()) {
            this.pendingSlotUpdate = null;
            this.trackedSwapSlot = var1;
            this.trackingDeadlineMillis = System.currentTimeMillis() + InventoryActionManager.INSTANCE.protectionDurationMillis();
            Slot slot = this.getPlayerSlot(var1);
            if (slot != null) {
                this.rememberRestoreItem(slot.getItem());
            }
        }
    }

    private int findRestoreSlot(boolean var1, boolean var2) {
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        for (Slot slot : mc.player.containerMenu.slots) {
            if (this.isPlayerInventorySlot(slot) && (var2 || slot.getContainerSlot() != selectedSlot) && this.matchesRestoreItem(slot.getItem(), var1)) {
                return slot.index;
            }
        }
        return -1;
    }

    private Slot getPlayerSlot(int var1) {
        if (var1 < 0) {
            return null;
        }
        NonNullList<Slot> slots2 = mc.player.containerMenu.slots;
        if (var1 >= slots2.size()) {
            return null;
        }
        Slot slot = (Slot) slots2.get(var1);
        return this.isPlayerInventorySlot(slot) ? slot : null;
    }

    private boolean isPlayerMenu() {
        return mc.player.containerMenu == mc.player.inventoryMenu;
    }

    private int resolveRestoreSlot() {
        Slot slot = this.getPlayerSlot(this.restoreSlot);
        if (slot != null && this.matchesRestoreItem(slot.getItem(), true)) {
            return this.restoreSlot;
        }
        int var2 = this.findRestoreSlot(true, false);
        if (var2 == -1) {
            var2 = this.findRestoreSlot(false, false);
        }
        if (var2 != -1) {
            this.restoreSlot = var2;
        }
        return var2;
    }

    private boolean isFallingDangerously() {
        return this.dangerChecks.isSelected("Падение") && !mc.player.isInWater() && mc.player.fallDistance > 10.0;
    }

    private boolean isOffhandRestored() {
        return this.matchesRestoreItem(mc.player.getOffhandItem(), false);
    }

    @Subscribe
    private void onWorldLoad(WorldLoadEvent event) {
        this.resetState();
    }

    private void expirePendingSwap() {
        if (this.swapSequence != null) {
            if (!this.swapSequence.hasNext() || ++this.pendingSwapTicks > 20) {
                this.swapSequence = null;
                this.pendingSwapTicks = 0;
            }
        }
    }

    private record ExplosionNotice(double x, double y, double z, float range) {
    }
}
