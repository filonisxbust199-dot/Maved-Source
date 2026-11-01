package ess.ll.client.module.misc;

import ess.ll.client.event.CameraRotationEvent;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.MouseTurnEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.module.ClientModules;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.setting.Settings.*;
import ess.ll.client.util.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * AimingItems, ported from dimasik: smoothly turns the camera towards valuable dropped items
 * (heads, elytra, god apples, shards, auras) while Aura has no target and FreeCam is off.
 */
public final class AimingItems {
    public static final MultiBooleanSettings ELEMENTS = new MultiBooleanSettings("Элементы", "Шары", "Элитра", "Чарки", "Осколки", "Ауры");
    public static final Setting[] SETTINGS = { ELEMENTS };
    private static final AimingItems INSTANCE = new AimingItems();
    private static boolean registered;
    private ItemEntity itemEntity;
    private float yaw, pitch;
    private boolean aiming;
    private long lastNanos;

    private AimingItems() {
    }

    public static boolean enabled() {
        return ClientModules.AIMING_ITEMS != null && ClientModules.AIMING_ITEMS.enabled();
    }

    /**
     * Keeps the event listeners registered only while the module is on.
     */
    public static void tick() {
        boolean on = enabled();
        if (on == registered)
            return;
        registered = on;
        if (on)
            EventBus.register(INSTANCE);
        else {
            EventBus.unregister(INSTANCE);
            INSTANCE.reset();
        }
    }

    private static boolean isAura(ItemStack stack, String name) {
        return stack.is(Items.SUNFLOWER) && name.contains("Аура Охотника") || stack.is(Items.CLAY_BALL) && name.contains("Аура Твёрдости Брони") || stack.is(Items.POPPED_CHORUS_FRUIT) && name.contains("Аура Телепортации") || stack.is(Items.GOLD_NUGGET) && name.contains("Аура Богача") || stack.is(Items.DYE.white()) && name.contains("Аура Защиты От Падения");
    }

    @Subscribe
    private void onTick(TickEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }
        itemEntity = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || item.isRemoved())
                continue;
            ItemStack stack = item.getItem();
            String name = stack.getHoverName().getString();
            if (ELEMENTS.selected("Шары") && stack.is(Items.PLAYER_HEAD) || ELEMENTS.selected("Осколки") && name.contains("Осколок") || ELEMENTS.selected("Элитра") && stack.is(Items.ELYTRA) || ELEMENTS.selected("Чарки") && stack.is(Items.ENCHANTED_GOLDEN_APPLE) || ELEMENTS.selected("Ауры") && isAura(stack, name))
                itemEntity = item;
        }
        if (!active())
            reset();
    }

    /**
     * The mouse does not turn the player while the camera is locked on an item.
     */
    @Subscribe
    private void onMouseTurn(MouseTurnEvent event) {
        if (active())
            event.cancel();
    }

    @Subscribe
    private void onCameraRotation(CameraRotationEvent event) {
        if (!active()) {
            reset();
            return;
        }
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 eye = player.getEyePosition(partial), item = itemEntity.getPosition(partial);
        double dx = item.x - eye.x, dy = item.y + itemEntity.getBbHeight() / 2.0 - eye.y, dz = item.z - eye.z;
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        long now = System.nanoTime();
        if (!aiming) {
            yaw = player.getYRot();
            pitch = player.getXRot();
            aiming = true;
            lastNanos = now;
        }
        // Frame-rate independent easing towards the item.
        float dt = Mth.clamp((now - lastNanos) * 1.0E-9F, 0, .05f);
        lastNanos = now;
        float k = 1 - (float) Math.exp(-7.5f * dt);
        yaw += Mth.wrapDegrees(targetYaw - yaw) * k;
        pitch = Mth.clamp(pitch + (targetPitch - pitch) * k, -90, 90);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        event.setYaw(yaw);
        event.setPitch(pitch);
    }

    private boolean active() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || itemEntity == null || itemEntity.isRemoved() || itemEntity.level() != mc.level)
            return false;
        var aura = ModuleManager.INSTANCE.aura();
        return !(aura.isEnabled() && AttackAura.currentTarget() != null) && !ModuleManager.INSTANCE.freeCam().isEnabled();
    }

    private void reset() {
        itemEntity = null;
        aiming = false;
        lastNanos = 0;
    }
}
