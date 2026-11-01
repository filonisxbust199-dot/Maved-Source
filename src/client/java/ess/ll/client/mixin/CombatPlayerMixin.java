package ess.ll.client.mixin;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;
import ess.ll.client.event.*;
import ess.ll.client.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ess.ll.client.event.CanSprintEvent;
@Mixin(LocalPlayer.class)
public abstract class CombatPlayerMixin {
   @ModifyExpressionValue(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
   private boolean ess$autoSprintCanStart(boolean original){
      return (Object)this==net.minecraft.client.Minecraft.getInstance().player
          ? CanSprintEvent.INSTANCE.fire(original,(LocalPlayer)(Object)this):original;
   }
   @ModifyExpressionValue(method="pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
      at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
   private static EntityHitResult ess$filterRaycastEntity(EntityHitResult hit) {
      return RaycastEvent.INSTANCE.fireEntity(hit);
   }
   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V"))
   private void onUpdate(CallbackInfo ci) {
      if (((Object)this == net.minecraft.client.Minecraft.getInstance().player)) {
         PlayerUpdateEvent.INSTANCE.dispatch();
      }
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = Shift.AFTER))
   private void onPostUpdate(CallbackInfo ci2) {
      if (((Object)this == net.minecraft.client.Minecraft.getInstance().player)) {
         PostPlayerInputEvent.INSTANCE.dispatch();
      }
   }

   @Inject(
      method = "sendPosition",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z", shift = Shift.AFTER),
      cancellable = true
   )
   private void sendMovementPacketsStart(CallbackInfo ci3) {
      if (((Object)this == net.minecraft.client.Minecraft.getInstance().player)) {
         PreMovementPacketEvent.INSTANCE.fire((LocalPlayer)(Object)this, ci3);
      }
   }

   @Inject(
      method = {"sendPosition", "tick"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendIsSprintingIfNeeded()V", shift = Shift.AFTER)
   )
   private void onSprintSync(CallbackInfo ci4) {
      if (((Object)this == net.minecraft.client.Minecraft.getInstance().player)) {
         SprintSyncEvent.INSTANCE.dispatch();
      }
   }

   @Inject(method = "sendPosition", at = @At("TAIL"))
   private void sendMovementPacketsEnd(CallbackInfo ci5) {
      if (((Object)this == net.minecraft.client.Minecraft.getInstance().player)) {
         PostMovementPacketEvent.INSTANCE.dispatch();
      }
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
   private float modifyPitch(float original) {
      return !((Object)this == net.minecraft.client.Minecraft.getInstance().player) ? original : PreMovementPacketEvent.INSTANCE.pitchOr(original);
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
   private float modifyYaw(float original) {
      return !((Object)this == net.minecraft.client.Minecraft.getInstance().player) ? original : PreMovementPacketEvent.INSTANCE.yawOr(original);
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
   private double modifyGetX(double original) {
      return !((Object)this == net.minecraft.client.Minecraft.getInstance().player) ? original : PreMovementPacketEvent.INSTANCE.xOr(original);
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
   private double modifyGetY(double original) {
      return !((Object)this == net.minecraft.client.Minecraft.getInstance().player) ? original : PreMovementPacketEvent.INSTANCE.yOr(original);
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
   private double modifyGetZ(double original) {
      return !((Object)this == net.minecraft.client.Minecraft.getInstance().player) ? original : PreMovementPacketEvent.INSTANCE.zOr(original);
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
   private boolean modifyOnGround(boolean original) {
      return !((Object)this == net.minecraft.client.Minecraft.getInstance().player) ? original : PreMovementPacketEvent.INSTANCE.onGroundOr(original);
   }
}
