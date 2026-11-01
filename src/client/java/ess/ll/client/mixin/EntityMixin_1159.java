package ess.ll.client.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityMixin_1159 {
   @Accessor("yRot")
   void IsetYaw(float yaw);

   @Accessor("xRot")
   void IsetPitch(float pitch);

   @Accessor("yRotO")
   float getPrevYaw_();

   @Accessor("xRotO")
   float getPrevPitch_();

   @Accessor("dimensions")
   EntityDimensions getDimensions_();

   @Accessor("stuckSpeedMultiplier")
   Vec3 getStuckSpeedMultiplier();

   @Invoker("setSharedFlag")
   void IsetFlag(int flag, boolean value);
}
