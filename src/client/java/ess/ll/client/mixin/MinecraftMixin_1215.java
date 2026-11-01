package ess.ll.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ess.ll.client.integration.PlayerAnimationAccess;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin_1215 {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"))
    private void hmiDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player instanceof PlayerAnimationAccess playerAnimationAccess) {
            playerAnimationAccess.hMI5_0$resetMainHandSwing(false);
        }
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"))
    private boolean hmiDoItemUse(LocalPlayer player, InteractionHand interactionHand, net.minecraft.world.item.component.SwingAnimation animation, boolean sendPacket) {
        if (player instanceof PlayerAnimationAccess playerAnimationAccess2) {
            if (interactionHand == InteractionHand.MAIN_HAND) {
                playerAnimationAccess2.hMI5_0$resetMainHandSwing(true);
            } else {
                playerAnimationAccess2.hMI5_0$resetOffHandSwing(true);
            }
        }
        return player.swing(interactionHand, animation, sendPacket);
    }
}
