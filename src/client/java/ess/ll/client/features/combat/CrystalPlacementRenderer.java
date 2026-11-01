package ess.ll.client.features.combat;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import ess.ll.client.event.Render3DEvent;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.render.ColorUtils;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * CrystalAura's placement mark: inside the block a crystal was put on, a see-through cube of the colour grows from
 * the centre until it fills the block, like a block being broken, stays while crystals keep going on it, and fades
 * once they stop. Plain coloured quads with no outline, no shaders; the crystal itself is not drawn.
 */
public class CrystalPlacementRenderer implements MinecraftAccess {
   private static final long GROW = 350L, HOLD = 450L, FADE = 300L;
   private static final int FILL_ALPHA = 85;

   /** Translucent filled quads drawn without the depth test, so the cube shows through the block around it. */
   private static final RenderType FILL = RenderType.create("ESS crystal mark", RenderSetup.builder(RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("ess-best", "pipeline/crystal_mark"))
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withCull(false).build())).createRenderSetup());

   /** One marked block: when it started filling and when a crystal last went on it. */
   private static final class Mark {
      final long born;
      long last;

      Mark(long now) {
         this.born = now;
         this.last = now;
      }
   }

   private static final Vector3f POINT = new Vector3f();
   private final Map<BlockPos, Mark> marks = new LinkedHashMap<>();

   /** A crystal was placed on base. A block that is still marked keeps its fill instead of starting over. */
   public void placed(BlockPos base) {
      long now = System.currentTimeMillis();
      Mark mark = this.marks.get(base);
      if (mark == null) this.marks.put(base.immutable(), new Mark(now));
      else mark.last = now;
   }

   public void clear() {
      this.marks.clear();
   }

   public void render(Render3DEvent event, int color) {
      if (this.marks.isEmpty()) return;
      long now = System.currentTimeMillis();
      Iterator<Map.Entry<BlockPos, Mark>> iterator = this.marks.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<BlockPos, Mark> entry = iterator.next();
         Mark mark = entry.getValue();
         long idle = now - mark.last;
         if (idle >= HOLD + FADE) {
            iterator.remove();
            continue;
         }
         float alpha = idle <= HOLD ? 1.0F : 1.0F - (float)(idle - HOLD) / FADE;
         float grown = easeOut((float)(now - mark.born) / GROW);
         BlockPos pos = entry.getKey();
         double half = 0.5 * grown + 0.002;
         double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
         AABB cube = new AABB(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half);
         fillBox(event, cube, ColorUtils.withAlpha(color, Math.round(FILL_ALPHA * alpha)));
      }
   }

   /**
    * A see-through filled box, no outline. Only the faces turned to the camera are drawn, so every pixel of the
    * box is covered once and its tint is even (the faces behind would show as darker bands through the front).
    */
   public static void fillBox(Render3DEvent event, AABB box, int color) {
      if (!event.getFrustum().isVisible(box)) return;
      Vec3 eye = event.getCamera().position();
      Matrix4f pose = event.getPoseStack().last().pose();
      VertexConsumer out = event.getVertexCollector().getBuffer(FILL);
      float x0 = (float)(box.minX - eye.x), y0 = (float)(box.minY - eye.y), z0 = (float)(box.minZ - eye.z);
      float x1 = (float)(box.maxX - eye.x), y1 = (float)(box.maxY - eye.y), z1 = (float)(box.maxZ - eye.z);
      if (y0 > 0) quad(out, pose, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
      if (y1 < 0) quad(out, pose, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
      if (z0 > 0) quad(out, pose, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
      if (z1 < 0) quad(out, pose, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
      if (x0 > 0) quad(out, pose, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
      if (x1 < 0) quad(out, pose, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
   }

   private static void quad(VertexConsumer out, Matrix4f pose, int color, float... corners) {
      for (int i = 0; i < 12; i += 3) {
         pose.transformPosition(corners[i], corners[i + 1], corners[i + 2], POINT);
         out.addVertex(POINT.x, POINT.y, POINT.z).setColor(color);
      }
   }

   private static float easeOut(float t) {
      float x = 1 - Math.clamp(t, 0, 1);
      return 1 - x * x * x;
   }
}
