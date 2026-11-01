package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.TargetEspPreviewContext;
import ess.ll.client.render.RenderPipelineCache;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import ess.ll.client.render.targetfx.api.MinecraftContext;
import ess.ll.client.render.targetfx.internal.StringConcatSupport;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
public final class CubeGlowShader implements MinecraftContext {
   public static final Identifier resourceIdentifier2 = Identifier.fromNamespaceAndPath(
      CubeGlowShader.string2, CubeGlowShader.string4
   );
   public static final Identifier resourceIdentifier = Identifier.fromNamespaceAndPath(
      CubeGlowShader.string11, CubeGlowShader.string9
   );
   private static final RenderPipeline[] renderPipelineArray = new RenderPipeline[]{
      getRenderPipeline2(false, false, false), getRenderPipeline2(false, false, true)
   };
   private static final RenderPipeline[] renderPipelineArray3 = new RenderPipeline[]{
      getRenderPipeline2(true, false, false), getRenderPipeline2(true, false, true)
   };
   private static final RenderPipeline[] renderPipelineArray2 = new RenderPipeline[]{
      getRenderPipeline2(false, true, false), getRenderPipeline2(false, true, true)
   };
   private static final String string7 = "nightix_cube_glow";
   private static final String string = "nightix_cube";
   private static final String string10 = "nightix_cube_glow";
   private static final String string12 = "nightix_cube";
   private static final String string6 = "Sampler0";
   private static final String string5 = "Sampler0";
   private static final String string8 = "Nightix cubes";
   private static final String string3 = "Nightix cube vertices";
   private static final String string2 = "dimasik";
   private static final String string4 = "textures/nightix/particles_1.png";
   private static final String string11 = "dimasik";
   private static final String string9 = "textures/nightix/particles_2.png";

   private CubeGlowShader() {
   }

   public static void updateState3(BufferBuilder bufferBuilder, Matrix4f matrix4f, float amount, int value) {
      for (byte index = -1; index <= 1; index += 2) {
         updateState6(bufferBuilder, matrix4f, -amount, index * amount, -amount, amount, index * amount, -amount, value);
         updateState6(bufferBuilder, matrix4f, amount, index * amount, -amount, amount, index * amount, amount, value);
         updateState6(bufferBuilder, matrix4f, amount, index * amount, amount, -amount, index * amount, amount, value);
         updateState6(bufferBuilder, matrix4f, -amount, index * amount, amount, -amount, index * amount, -amount, value);
      }

      for (byte index2 = -1; index2 <= 1; index2 += 2) {
         for (byte index3 = -1; index3 <= 1; index3 += 2) {
            updateState6(bufferBuilder, matrix4f, index2 * amount, -amount, index3 * amount, index2 * amount, amount, index3 * amount, value);
         }
      }
   }

   private static void updateState6(
      BufferBuilder bufferBuilder2, Matrix4f matrix4f2, float amount2, float amount3, float amount4, float amount5, float amount6, float amount7, int value2
   ) {
      bufferBuilder2.addVertex(matrix4f2, amount2, amount3, amount4).setColor(value2);
      bufferBuilder2.addVertex(matrix4f2, amount5, amount6, amount7).setColor(value2);
   }

   public static BufferBuilder getBufferBuilder2(boolean matches, boolean matches2) {
      return GpuResourceScope.getGpuResourceScope()
         .getBufferBuilder(
            matches ? PrimitiveTopology.DEBUG_LINES : PrimitiveTopology.QUADS, matches2 ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR
         );
   }

   private static void updateState5(
      BufferBuilder bufferBuilder3,
      Matrix4f matrix4f3,
      float amount8,
      float amount9,
      float amount10,
      float amount11,
      float amount12,
      float amount13,
      float amount14,
      float amount15,
      float amount16,
      float amount17,
      float amount18,
      float amount19,
      int value3
   ) {
      bufferBuilder3.addVertex(matrix4f3, amount8, amount9, amount10).setColor(value3);
      bufferBuilder3.addVertex(matrix4f3, amount11, amount12, amount13).setColor(value3);
      bufferBuilder3.addVertex(matrix4f3, amount14, amount15, amount16).setColor(value3);
      bufferBuilder3.addVertex(matrix4f3, amount17, amount18, amount19).setColor(value3);
   }

   public static void updateState2(BufferBuilder bufferBuilder4, Matrix4f matrix4f4, float amount20, int value4) {
      bufferBuilder4.addVertex(matrix4f4, -amount20, -amount20, 0.0F).setColor(value4).setUv(0.0F, 1.0F);
      bufferBuilder4.addVertex(matrix4f4, amount20, -amount20, 0.0F).setColor(value4).setUv(1.0F, 1.0F);
      bufferBuilder4.addVertex(matrix4f4, amount20, amount20, 0.0F).setColor(value4).setUv(1.0F, 0.0F);
      bufferBuilder4.addVertex(matrix4f4, -amount20, amount20, 0.0F).setColor(value4).setUv(0.0F, 0.0F);
   }

   private static RenderPipeline getRenderPipeline2(boolean matches3, boolean matches4, boolean matches5) {
      RenderPipelineLayouts renderPipelineLayouts = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
         .getRenderPipelineLayouts(StringConcatSupport.concat464(matches3, matches4, matches5))
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(matches4 ? string7 : string))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(matches4 ? string10 : string12))
         .getRenderPipelineLayouts8(
            matches4 ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR, matches3 ? PrimitiveTopology.DEBUG_LINES : PrimitiveTopology.QUADS
         )
         .getRenderPipelineLayouts5(matches5 ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.LIGHTNING);
      if (matches4) {
         renderPipelineLayouts.getRenderPipelineLayouts12(string6);
      }

      return renderPipelineLayouts.getRenderPipeline();
   }

   public static void updateState(BufferBuilder bufferBuilder5, Matrix4f matrix4f5, float amount21, int value5) {
      updateState5(bufferBuilder5, matrix4f5, -amount21, amount21, -amount21, amount21, amount21, -amount21, amount21, amount21, amount21, -amount21, amount21, amount21, value5);
      updateState5(bufferBuilder5, matrix4f5, -amount21, -amount21, amount21, amount21, -amount21, amount21, amount21, -amount21, -amount21, -amount21, -amount21, -amount21, value5);
      updateState5(bufferBuilder5, matrix4f5, amount21, -amount21, -amount21, amount21, -amount21, amount21, amount21, amount21, amount21, amount21, amount21, -amount21, value5);
      updateState5(bufferBuilder5, matrix4f5, -amount21, -amount21, amount21, -amount21, -amount21, -amount21, -amount21, amount21, -amount21, -amount21, amount21, amount21, value5);
      updateState5(bufferBuilder5, matrix4f5, -amount21, -amount21, amount21, amount21, -amount21, amount21, amount21, amount21, amount21, -amount21, amount21, amount21, value5);
      updateState5(bufferBuilder5, matrix4f5, amount21, -amount21, -amount21, -amount21, -amount21, -amount21, -amount21, amount21, -amount21, amount21, amount21, -amount21, value5);
   }

   public static void updateState4(BufferBuilder bufferBuilder6, boolean matches6, Identifier identifier, boolean matches7) {
      RenderPipeline pipeline = (identifier != null ? renderPipelineArray2 : (matches6 ? renderPipelineArray3 : renderPipelineArray))[matches7 ? 1 : 0];
      MeshData meshData = bufferBuilder6.build();
      if (meshData != null) {
         if (!RenderPipelineCache.isCompiled(pipeline)) {
            meshData.close();
            return;
         }
         MeshData meshData2 = meshData;

         try {
            GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> string3, 32, meshData.vertexBuffer());

            try {
               DrawState drawState = meshData.drawState();
               AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
               GpuBuffer gpuBuffer2 = autoStorageIndexBuffer.getBuffer(drawState.indexCount());
               RenderTarget renderTarget = TargetEspPreviewContext.target();
               AbstractTexture abstractTexture = identifier == null ? null : MINECRAFT.getTextureManager().getTexture(identifier);
               RenderPass renderPass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(
                     () -> string8, renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty()
                  );

               try {
                  RenderSystem.bindDefaultUniforms(renderPass);
                  renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
                  if (abstractTexture != null) {
                     renderPass.setUniform(string5, abstractTexture.getTextureView(), abstractTexture.getSampler());
                  }

                  renderPass.setVertexBuffer(0, gpuBuffer.slice());
                  renderPass.setIndexBuffer(gpuBuffer2, autoStorageIndexBuffer.type());
                  renderPass.drawIndexed(drawState.indexCount(), 1, 0, 0, 0);
               } catch (Throwable throwable) {
                  if (renderPass != null) {
                     try {
                        renderPass.close();
                     } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                     }
                  }

                  throw throwable;
               }

               if (renderPass != null) {
                  renderPass.close();
               }
            } catch (Throwable throwable3) {
               if (gpuBuffer != null) {
                  try {
                     gpuBuffer.close();
                  } catch (Throwable throwable4) {
                     throwable3.addSuppressed(throwable4);
                  }
               }

               throw throwable3;
            }

            if (gpuBuffer != null) {
               gpuBuffer.close();
            }
         } catch (Throwable throwable5) {
            if (meshData != null) {
               try {
                  meshData2.close();
               } catch (Throwable throwable6) {
                  throwable5.addSuppressed(throwable6);
               }
            }

            throw throwable5;
         }

         if (meshData != null) {
            meshData.close();
         }
      }
   }
}
