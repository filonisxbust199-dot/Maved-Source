package ess.ll.client.mixin;
import ess.ll.client.event.CameraRotationEvent;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.sugar.Local;
@Mixin(Camera.class)
public class CombatCameraMixin {
 @Inject(method="alignWithEntity",at=@At("RETURN"))
 private void ess$freeCamera(float delta, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
   Camera camera=(Camera)(Object)this;
   var position=ess.ll.client.event.CameraPositionEvent.INSTANCE.fire(camera.position());
   ((CameraMixin_1260)(Object)this).setPositionAccessor(position.x,position.y,position.z);
 }
 @com.llamalad7.mixinextras.injector.ModifyReturnValue(method="isDetached",at=@At("RETURN"))
 private boolean ess$freeCameraDetached(boolean original) {
   return original || ess.ll.client.util.ModuleManager.INSTANCE.freeCam().isEnabled();
 }
 @Redirect(method="alignWithEntity",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;setRotation(FF)V",ordinal=1))
 private void ess$rotation(Camera camera,float yaw,float pitch,@Local(ordinal=0,argsOnly=true) float delta){CameraRotationEvent.INSTANCE.fire(camera,yaw,pitch,delta);}
}
