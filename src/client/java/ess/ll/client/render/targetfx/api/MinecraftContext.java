package ess.ll.client.render.targetfx.api;

import java.util.Random;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
public interface MinecraftContext {
   @NotNull
   Minecraft MINECRAFT = Minecraft.getInstance();

   default float randomSignedOffset(float amount) {
      return (new Random().nextFloat() * 2.0F - 1.0F) * amount;
   }
}
