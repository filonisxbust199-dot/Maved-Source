package ess.ll.client.features.crystalv2;

import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** The maved fading boxes: full colour and no size at the start, a growing cube that fades out over 1.5 s. */
public final class V2Visuals {
   private static final long DURATION_MS = 1500L;
   private static final int BOX_VERTS = 468;
   private final ConcurrentLinkedQueue<Visual> visuals = new ConcurrentLinkedQueue<>();

   private record Visual(BlockPos pos, float r, float g, float b, long start) {
   }

   public V2Visuals() {
      LevelRenderEvents.END_MAIN.register(context -> this.render());
   }

   public void add(BlockPos pos, float r, float g, float b) {
      this.visuals.add(new Visual(pos.immutable(), r, g, b, System.currentTimeMillis()));
   }

   public boolean has(BlockPos pos) {
      for (Visual visual : this.visuals) {
         if (visual.pos.equals(pos)) {
            return true;
         }
      }

      return false;
   }

   private void render() {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && client.player != null && !this.visuals.isEmpty()) {
         Vec3 camera = client.gameRenderer.mainCamera().position();
         long now = System.currentTimeMillis();
         this.visuals.removeIf(visual -> value(now - visual.start) <= 0.0);
         V2Shapes.begin();

         for (Visual visual : this.visuals) {
            double value = value(now - visual.start);
            float alpha = (float)Math.clamp(value, 0.0, 1.0);
            if (!(alpha <= 0.0F)) {
               if (!V2Shapes.hasCapacity(BOX_VERTS)) {
                  V2Shapes.flush();
                  V2Shapes.begin();
               }

               float x0 = (float)(visual.pos.getX() - camera.x);
               float y0 = (float)(visual.pos.getY() - camera.y);
               float z0 = (float)(visual.pos.getZ() - camera.z);
               int color = argb(alpha, visual.r, visual.g, visual.b);
               V2Shapes.emitFill(x0 + 0.5F, y0 + 0.5F, z0 + 0.5F, (float)(1.0 - Math.min(value, 1.0)) / 2.0F, color);
            }
         }

         V2Shapes.flush();
      }
   }

   /** From 1 to 0 with easeOutBack, which overshoots and ends the box a little before the duration. */
   private static double value(long elapsed) {
      double x = Math.min(elapsed / (double)DURATION_MS, 1.0) - 1.0;
      double c1 = 1.70158;
      double c3 = c1 + 1.0;
      return 1.0 - (1.0 + c3 * x * x * x + c1 * x * x);
   }

   private static int argb(float alpha, float r, float g, float b) {
      int a = Math.clamp((int)(alpha * 255.0F), 0, 255);
      int red = Math.clamp((int)(r * 255.0F), 0, 255);
      int green = Math.clamp((int)(g * 255.0F), 0, 255);
      int blue = Math.clamp((int)(b * 255.0F), 0, 255);
      return a << 24 | red << 16 | green << 8 | blue;
   }
}
