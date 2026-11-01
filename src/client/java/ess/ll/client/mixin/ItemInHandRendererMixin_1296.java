package ess.ll.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.integration.HeldItemRenderHooks;
import ess.ll.client.integration.ItemInHandRendererHooks;
import ess.ll.client.integration.PlayerArmRenderAccess;
import ess.ll.client.render.ItemAnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HoldMyItems hands: when that swing mode is on, the whole arm and item are drawn by ItemInHandRendererHooks
 * instead of vanilla. In 26.3 the renderer works from render states, so the frame's states are kept here for the
 * arm and map calls the hooks make. FreeLook also turns the hands with the camera instead of the body.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin_1296 implements PlayerArmRenderAccess {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private PlayerRenderState hmi$player;

    @Unique
    private FirstPersonHandsAndItemsRenderState hmi$items;

    @Shadow
    protected abstract void renderPlayerArm(PoseStack pose, SubmitNodeCollector collector, int light, float equip, float swing, HumanoidArm arm, PlayerRenderState player);

    @Shadow
    protected abstract void renderMap(PoseStack pose, SubmitNodeCollector collector, int light, ItemStack map, boolean mainHand, FirstPersonHandsAndItemsRenderState items);

    @Override
    public void hmi$renderPlayerArm(PoseStack pose, SubmitNodeCollector collector, int light, float equip, float swing, HumanoidArm arm) {
        if (this.hmi$player != null)
            this.renderPlayerArm(pose, collector, light, equip, swing, arm, this.hmi$player);
    }

    @Override
    public void hmi$renderMap(PoseStack pose, SubmitNodeCollector collector, int light, ItemStack map) {
        if (this.hmi$items != null)
            this.renderMap(pose, collector, light, map, map == this.hmi$items.mainHandItem, this.hmi$items);
    }

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void hmi$beginFrame(float tickDelta, PoseStack pose, SubmitNodeCollector collector, PlayerRenderState player, FirstPersonHandsAndItemsRenderState items, CallbackInfo ci) {
        this.hmi$player = player;
        this.hmi$items = items;
        if (this.minecraft.player != null && this.minecraft.getCameraEntity() == this.minecraft.player) {
            var camera = this.minecraft.gameRenderer.mainCamera();
            items.viewXRot = camera.xRot();
            // Vanilla subtracts yBob without wrapping. Keep both values in the same angular interval.
            items.viewYRot = items.yBob + net.minecraft.util.Mth.wrapDegrees(camera.yRot() - items.yBob);
        }
        if (ItemInHandRendererHooks.enabled) {
            ItemAnimationState.INSTANCE.updateFrame();
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hmi$overhaul(PlayerRenderState player, FirstPersonHandsAndItemsRenderState items, float tickDelta, float xRot, InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        // CS knife skins draw their own arm and knife with their own animations.
        if (ess.ll.client.wardrobe.CsKnife.render(this, hand, swing, stack, pose, collector, light)) {
            ci.cancel();
            return;
        }
        if (ItemInHandRendererHooks.enabled && this.minecraft.player != null) {
            ItemStack shown = HeldItemRenderHooks.mainHandStack(hand, this.minecraft, stack);
            ItemInHandRendererHooks.renderArmWithItem((FirstPersonHandsAndItemsRenderer) (Object) this, this.minecraft.player, tickDelta, xRot, hand, swing, shown, equip, pose, collector, light);
            ci.cancel();
        }
    }
}
