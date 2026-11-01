package ess.ll.client.mixin;

import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerMixin_1170 {
   @Accessor("wasSprinting")
   boolean wasSprinting();

   @Accessor("wasSprinting")
   void setWasSprinting(boolean value);

   @Accessor("xLast")
   double lastX();

   @Accessor("yLast")
   double lastY();

   @Accessor("zLast")
   double lastZ();

   @Accessor("yRotLast")
   float lastYRot();

   @Accessor("xRotLast")
   float lastXRot();

   @Accessor("lastOnGround")
   boolean lastOnGround();

   @Accessor("lastHorizontalCollision")
   boolean lastHorizontalCollision();

   @Accessor("positionReminder")
   int positionReminder();

   @Accessor("ambientSoundHandlers")
   List<?> ambientSoundHandlers();

   @Accessor("xLast")
   void setLastX(double value);

   @Accessor("yLast")
   void setLastY(double value);

   @Accessor("zLast")
   void setLastZ(double value);

   @Accessor("yRotLast")
   void setLastYRot(float value);

   @Accessor("xRotLast")
   void setLastXRot(float value);

   @Accessor("lastOnGround")
   void setLastOnGround(boolean value);

   @Accessor("lastHorizontalCollision")
   void setLastHorizontalCollision(boolean value);

   @Accessor("positionReminder")
   void setPositionReminder(int value);
}
