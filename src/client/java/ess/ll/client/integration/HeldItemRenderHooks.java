package ess.ll.client.integration;

import ess.ll.client.core.FreeLookController;
import ess.ll.client.event.HeldItemRenderEvent;
import ess.ll.client.module.render.SwingAnimations;
import ess.ll.client.util.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

// Ported from test-26.2 for the swing animations. SeeInvisible and the CustomHands glow are not part of this
// client (it has its own Hands module), so their hooks pass the vanilla values through.
public final class HeldItemRenderHooks {
    public static float cameraYawOr(Minecraft mc, float yaw) {
        return FreeLookController.isActive() && mc.getEntityRenderDispatcher().camera != null ? mc.getEntityRenderDispatcher().camera.yRot() : yaw;
    }

    /**
     * Whether an invisible player's arm is drawn anyway (SeeInvisible).
     */
    public static boolean drawInvisibleArm() {
        return false;
    }

    private HeldItemRenderHooks() {
    }

    public static float swingSpeed() {
        SwingAnimations swingAnimations = ModuleManager.INSTANCE.swingAnimations();
        return swingAnimations == null ? 1.0F : swingAnimations.currentItemScale();
    }

    public static float cameraPitchOr(Minecraft mc, float pitch) {
        return FreeLookController.isActive() && mc.getEntityRenderDispatcher().camera != null ? mc.getEntityRenderDispatcher().camera.xRot() : pitch;
    }

    public static ItemStack mainHandStack(InteractionHand interactionHand, Minecraft mc, ItemStack stack) {
        return interactionHand == InteractionHand.MAIN_HAND && mc.player != null ? HeldItemRenderEvent.INSTANCE.fireMainHandStack(mc.player, stack) : stack;
    }

    public static int selectedSlot(int slot) {
        return slot;
    }

    public static void onItemChanged(ItemStack oldStack, ItemStack newStack) {
    }
}
