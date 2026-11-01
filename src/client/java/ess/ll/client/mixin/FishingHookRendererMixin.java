package ess.ll.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ess.ll.client.render.ItemAnimationState;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
    private static final double FOV_SCALE = 0;
    private static final float PLANE_X = 0;

    @Inject(method = "getPlayerHandPos", at = @At("HEAD"), cancellable = true)
    private void hmi$handPos(Player player, float partialTick, float swing, CallbackInfoReturnable<Vec3> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && player == mc.player) {
            float fov = mc.options.fov().get().intValue();
            double scale = FOV_SCALE / fov;
            int side = FishingHookRenderer.getHoldingArm(player) == HumanoidArm.RIGHT ? 1 : -1;
            Matrix4f matrix = side == 1 ? ItemAnimationState.INSTANCE.mainHandPose : ItemAnimationState.INSTANCE.offhandPose;
            Vector4f vector4f = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
            vector4f.mul(matrix);
            Vec3 vec = new Vec3(vector4f.x(), vector4f.y(), vector4f.z()).scale(scale);
            Vec3 vec2 = mc.gameRenderer.mainCamera().getNearPlane(fov).getPointOnPlane(PLANE_X, 0.0F).scale(scale).add(vec);
            cir.setReturnValue(mc.gameRenderer.mainCamera().position().add(vec2));
        }
    }
}
