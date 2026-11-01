package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ess.ll.client.module.render.Hands;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands: first-person hands are drawn into the module's offscreen target instead of the main one,
 * then composited back with the selected effect once renderItemInHand finishes.
 */
@Mixin(GameRenderer.class)
public class HandsCaptureMixin {
    @Inject(method="renderItemInHand",at=@At(value="INVOKE",
            target="Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"))
    private void ess$beginHandsCapture(CallbackInfo ci) {
        Hands.INSTANCE.updateState();
    }

    @WrapOperation(method="renderItemInHand",at=@At(value="INVOKE",
            target="Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"))
    private RenderPass ess$routeHands(CommandEncoder encoder,Supplier<String> label,GpuTextureView color,Optional<Vector4fc> clearColor,
                                      GpuTextureView depth,OptionalDouble clearDepth,Operation<RenderPass> original) {
        RenderTarget capture=Hands.renderTarget;
        if(Hands.stateActive && capture!=null)
            return original.call(encoder,label,capture.getColorTextureView(),clearColor,capture.getDepthTextureView(),clearDepth);
        return original.call(encoder,label,color,clearColor,depth,clearDepth);
    }

    @Inject(method="renderItemInHand",at=@At("RETURN"))
    private void ess$compositeHands(CallbackInfo ci) {
        Hands.INSTANCE.updateState2();
    }
}
