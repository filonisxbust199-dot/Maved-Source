package ess.ll.client.render;

import ess.ll.client.integration.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class PlayerAnimationUtils {
    public static boolean isSpinAttacking(AbstractClientPlayer player) {
        return player.isAutoSpinAttack();
    }

    public static double getY(AbstractClientPlayer player) {
        return player.getY();
    }

    public static int getTickCount(AbstractClientPlayer player) {
        return player.tickCount;
    }

    public static double getSwingCount(AbstractClientPlayer player) {
        return player instanceof PlayerAnimationAccess ? ((PlayerAnimationAccess) player).hMI5_0$getSwingCount() : 0.0;
    }

    public static double getSpeed(AbstractClientPlayer player) {
        return player.getDeltaMovement().length();
    }

    public static double getZ(AbstractClientPlayer player) {
        return player.getZ();
    }

    public static double getVelocityY(AbstractClientPlayer player) {
        return player.getDeltaMovement().y;
    }

    public static boolean isOnCooldown(ItemStack stack, AbstractClientPlayer player) {
        return player.getCooldowns().isOnCooldown(stack);
    }

    public static boolean isUsingItem(AbstractClientPlayer player) {
        return player.isUsingItem();
    }

    public static InteractionHand getUsedHand(AbstractClientPlayer player) {
        return player.getUsedItemHand();
    }

    public static boolean isSwimming(AbstractClientPlayer player) {
        return player.isSwimming();
    }

    public static boolean isScoping(AbstractClientPlayer player) {
        return player.isScoping();
    }

    public static double getHealth(AbstractClientPlayer player) {
        return player.getHealth();
    }

    public static double getHeadYaw(AbstractClientPlayer player) {
        return player.getYHeadRot();
    }

    public static boolean isSneaking(AbstractClientPlayer player) {
        return player.isShiftKeyDown();
    }

    public static double getPitch(AbstractClientPlayer player) {
        return player.getXRot();
    }

    public static boolean isCrawling(AbstractClientPlayer player) {
        return player.isVisuallyCrawling();
    }

    public static ItemStack getOffhandItem(AbstractClientPlayer player) {
        return player.getOffhandItem();
    }

    public static ItemStack getMainHandItem(AbstractClientPlayer player) {
        return player.getMainHandItem();
    }

    public static boolean isOnGround(AbstractClientPlayer player) {
        return player.onGround();
    }

    public static double getVelocityX(AbstractClientPlayer player) {
        return player.getDeltaMovement().x;
    }

    public static boolean isInWater(AbstractClientPlayer player) {
        return player.isInWater();
    }

    public static boolean isUnderwater(AbstractClientPlayer player) {
        return player.isUnderWater();
    }

    public static double getX(AbstractClientPlayer player) {
        return player.getX();
    }

    public static boolean isClimbing(AbstractClientPlayer player) {
        return player.onClimbable();
    }

    public static double getVelocityZ(AbstractClientPlayer player) {
        return player.getDeltaMovement().z;
    }

    private PlayerAnimationUtils() {
    }
}
