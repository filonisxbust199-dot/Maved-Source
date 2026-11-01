package ess.ll.client.util;

import ess.ll.client.core.SimulatedPlayerInput;
import ess.ll.client.event.TickEvent;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.integration.MinecraftAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public final class AttackItemUtils implements MinecraftAccess {
    private static final int ZERO_UNUSED = 0;
    private static int cachedTick = AttackItemUtils.NO_TICK;
    private static boolean cachedFalling;
    private static final double FALL_DISTANCE_THRESHOLD = 3.0;
    private static final double PREDICTED_FALL_THRESHOLD = 3.0;
    private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";
    private static final int NO_TICK = Integer.MIN_VALUE;

    public static int maceHotbarSlot() {
        return InventoryUtils.findItemIndex(Items.MACE, true);
    }

    public static boolean isHoldingMace() {
        return mc.player != null && mc.player.getMainHandItem().is(Items.MACE);
    }

    private AttackItemUtils() {
        throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
    }

    public static Slot findMaceSlot() {
        return InventoryUtils.findSlot(Items.MACE);
    }

    public static boolean isFallingForMace() {
        int ticks = TickEvent.getTicks();
        if (cachedTick == ticks) {
            return cachedFalling;
        }
        cachedTick = ticks;
        cachedFalling = willFall();
        return cachedFalling;
    }

    public static boolean isFalling(LivingEntity livingEntity) {
        return livingEntity == null ? false : !livingEntity.onGround() && !livingEntity.isFallFlying() && livingEntity.fallDistance > FALL_DISTANCE_THRESHOLD;
    }

    private static boolean willFall() {
        if (mc.player == null || mc.player.input == null) {
            return false;
        }
        if (mc.player.isFallFlying()) {
            return false;
        }
        if (isFalling(mc.player)) {
            return true;
        }
        SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.ofClientPlayer(SimulatedPlayerInput.SimulatedInput.fromClientPlayer(mc.player.input.keyPresses));
        for (int i = 0; i < 20; i++) {
            simulatedPlayerInput.tick();
            if (simulatedPlayerInput.onGround || simulatedPlayerInput.inWater) {
                return false;
            }
            if (simulatedPlayerInput.fallDistance > PREDICTED_FALL_THRESHOLD) {
                return true;
            }
        }
        return false;
    }
}
