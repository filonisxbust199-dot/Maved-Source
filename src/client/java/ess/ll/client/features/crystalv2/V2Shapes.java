package ess.ll.client.features.crystalv2;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import ess.ll.client.render.WorldFx;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** The maved shape batch: camera-relative coloured triangles, drawn through the walls in a pass of their own. */
public final class V2Shapes {
   private static final int MAX_VERTICES = 8192;
   private static final int VERTEX_STRIDE = 16;
   private static final int BUFFER_SIZE = MAX_VERTICES * VERTEX_STRIDE;
   private static final Identifier SHADER = Identifier.withDefaultNamespace("core/position_color");
   private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
      RenderPipeline.builder()
         .withLocation(Identifier.fromNamespaceAndPath("ess-best", "pipeline/v2_shapes_xray"))
         .withVertexShader(SHADER)
         .withFragmentShader(SHADER)
         .withBindGroupLayout(
            BindGroupLayout.builder()
               .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
               .withUniform("Projection", UniformType.UNIFORM_BUFFER)
               .build()
         )
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withCull(false)
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
         .build()
   );
   private static ByteBuffer staging;
   private static int vertexCount;

   private V2Shapes() {
   }

   public static void begin() {
      if (staging == null) {
         staging = MemoryUtil.memAlloc(BUFFER_SIZE);
      }

      staging.clear();
      vertexCount = 0;
   }

   public static boolean hasCapacity(int verts) {
      return vertexCount + verts <= MAX_VERTICES;
   }

   public static void emitFill(float cx, float cy, float cz, float extent, int argb) {
      float x0 = cx - extent;
      float y0 = cy - extent;
      float z0 = cz - extent;
      float x1 = cx + extent;
      float y1 = cy + extent;
      float z1 = cz + extent;
      quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, argb);
      quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, argb);
      quad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, argb);
      quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, argb);
      quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, argb);
      quad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, argb);
   }

   private static void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int argb) {
      tri(ax, ay, az, bx, by, bz, cx, cy, cz, argb);
      tri(ax, ay, az, cx, cy, cz, dx, dy, dz, argb);
   }

   private static void tri(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int argb) {
      if (vertexCount + 3 <= MAX_VERTICES) {
         putVertex(x1, y1, z1, argb);
         putVertex(x2, y2, z2, argb);
         putVertex(x3, y3, z3, argb);
         vertexCount += 3;
      }
   }

   private static void putVertex(float x, float y, float z, int argb) {
      staging.putFloat(x)
         .putFloat(y)
         .putFloat(z)
         .put((byte)(argb >> 16 & 0xFF))
         .put((byte)(argb >> 8 & 0xFF))
         .put((byte)(argb & 0xFF))
         .put((byte)(argb >> 24 & 0xFF));
   }

   public static void flush() {
      if (vertexCount != 0) {
         var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
         if (WorldFx.usable(target)) {
            staging.flip();
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            var vertices = encoder.transientMemory().uploadGpu(staging, 4, 32);
            var transform = RenderSystem.getDynamicUniforms().writeTransform(WorldFx.view(), new Vector4f(1.0F), new Vector3f(), new Matrix4f());

            try (MemoryStack stack = MemoryStack.stackPush()) {
               var projection = WorldFx.upload(Std140Builder.onStack(stack, 64).putMat4f(WorldFx.projection()).get());

               try (var pass = encoder.createRenderPass(() -> "ESS v2 shapes", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                  pass.setPipeline(RenderSystem.getCompiledPipeline(THROUGH_WALLS));
                  RenderSystem.bindDefaultUniforms(pass);
                  pass.setUniform("DynamicTransforms", transform);
                  pass.setUniform("Projection", projection);
                  pass.setVertexBuffer(0, vertices);
                  pass.draw(vertexCount, 1, 0, 0);
               }
            }
         }

         staging.clear();
         vertexCount = 0;
      }
   }
}
