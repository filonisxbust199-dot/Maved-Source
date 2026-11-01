package ess.ll.client.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraMixin_1260 {
   @Invoker("setRotation")
   void IsetRotation(float yaw, float pitch);

   @Invoker("setPosition")
   void setPositionAccessor(double x, double y, double z);
}
