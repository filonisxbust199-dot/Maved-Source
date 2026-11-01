package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.TargetEspPreviewContext;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import ess.ll.client.render.targetfx.api.MinecraftContext;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
public final class DeadChamsCompositeShader implements MinecraftContext {
   public static final DeadChamsCompositeShader deadChamsCompositeShader = new DeadChamsCompositeShader();
   private static final RenderPipeline renderPipeline = getRenderPipeline2(DeadChamsCompositeShader.string11, PrimitiveTopology.QUADS);
   private static final RenderPipeline renderPipeline2 = getRenderPipeline2(DeadChamsCompositeShader.string15, PrimitiveTopology.TRIANGLES);
   private static final RenderPipeline renderPipeline3 = RenderPipelineLayouts.getRenderPipelineLayouts6()
      .getRenderPipelineLayouts(DeadChamsCompositeShader.string2)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(DeadChamsCompositeShader.string10))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(DeadChamsCompositeShader.string4))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts10()
      .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
      .getRenderPipelineLayouts2(DeadChamsCompositeShader.string6, UniformType.UNIFORM_BUFFER)
      .getRenderPipelineLayouts12(DeadChamsCompositeShader.string7)
      .getRenderPipelineLayouts12(DeadChamsCompositeShader.string9)
      .getRenderPipeline();
   private TextureTarget textureTarget2;
   private TextureTarget textureTarget;
   private static final String string = "dead_chams";
   private static final String string5 = "target_esp_rhomb_distortion_mask";
   private static final String string12 = "SceneSampler";
   private static final String string17 = "GlowSampler";
   private static final String string13 = "GlowDistortion";
   private static final String string3 = "glow-distortion-scene";
   private static final String string8 = "glow-distortion-mask";
   private static final String string16 = "glowDistortionComposite";
   private static final String string14 = "glowDistortionMask";
   private static final String string11 = "dimasik/glow_distortion_mask";
   private static final String string15 = "dimasik/glow_distortion_mask_triangles";
   private static final String string2 = "dimasik/glow_distortion";
   private static final String string10 = "hands_fullscreen";
   private static final String string4 = "target_esp_rhomb_distortion";
   private static final String string6 = "GlowDistortion";
   private static final String string7 = "SceneSampler";
   private static final String string9 = "GlowSampler";

   private static RenderPipeline getRenderPipeline2(String text, PrimitiveTopology primitiveTopology) {
      return RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
         .getRenderPipelineLayouts(text)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string5))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
         .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, primitiveTopology)
         .getRenderPipeline();
   }

   public void updateState4(BufferBuilder bufferBuilder, float amount) {
      MeshData meshData = bufferBuilder.build();
      if (meshData != null) {
         try {
            if (!(amount <= 0.0F)) {
               DrawState drawState = meshData.drawState();
               int value = drawState.indexCount();
               AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
               GpuBuffer gpuBuffer = autoStorageIndexBuffer.getBuffer(value);
               IndexType indexType = autoStorageIndexBuffer.type();
               GpuBufferSlice gpuBufferSlice = GpuUniformBufferHelper.getGpuBufferSlice(meshData.vertexBuffer(), drawState.format().getVertexSize());
               this.updateState2(gpuBufferSlice, gpuBuffer, indexType, value, amount, drawState.primitiveTopology());
               return;
            }
         } finally {
            meshData.close();
         }
      }
   }

   public void updateState6(RenderTarget renderTarget, float amount2) {
      if (!(amount2 <= 0.0F)) {
         RenderTarget renderTarget2 = TargetEspPreviewContext.target();
         if (renderTarget2 != null && renderTarget2.getColorTextureView() != null && renderTarget != null && renderTarget.getColorTextureView() != null) {
            this.updateState(renderTarget2.width, renderTarget2.height, false);
            this.updateState3(renderTarget, renderTarget2, amount2, RenderSystem.getDevice().createCommandEncoder());
         }
      }
   }

   private DeadChamsCompositeShader() {
   }

   private void updateState2(GpuBufferSlice gpuBufferSlice2, GpuBuffer gpuBuffer2, IndexType indexType2, int value2, float amount3, PrimitiveTopology primitiveTopology2) {
      RenderTarget renderTarget3 = TargetEspPreviewContext.target();
      if (renderTarget3 != null && renderTarget3.getColorTextureView() != null && value2 > 0 && !(amount3 <= 0.0F)) {
         this.updateState(renderTarget3.width, renderTarget3.height, true);
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         RenderPass renderPass = commandEncoder.createRenderPass(
            () -> string14, this.textureTarget2.getColorTextureView(), Optional.of(new Vector4f(0.0F))
         );

         try {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(primitiveTopology2 == PrimitiveTopology.TRIANGLES ? renderPipeline2 : renderPipeline));
            renderPass.setVertexBuffer(0, gpuBufferSlice2);
            renderPass.setIndexBuffer(gpuBuffer2, indexType2);
            renderPass.drawIndexed(value2, 1, 0, 0, 0);
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

         this.updateState3(this.textureTarget2, renderTarget3, amount3, commandEncoder);
      }
   }

   public void updateState7(GpuBuffer gpuBuffer3, GpuBuffer gpuBuffer4, IndexType indexType3, int value3, float amount4, PrimitiveTopology primitiveTopology3) {
      this.updateState2(gpuBuffer3.slice(), gpuBuffer4, indexType3, value3, amount4, primitiveTopology3);
   }

   private void updateState3(RenderTarget renderTarget4, RenderTarget renderTarget5, float amount5, CommandEncoder commandEncoder2) {
      commandEncoder2.copyTextureToTexture(renderTarget5.getColorTexture(), this.textureTarget.getColorTexture(), 0, 0, 0, 0, 0, renderTarget5.width, renderTarget5.height);
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer buffer = Std140Builder.onStack(memoryStack, 16).putFloat(amount5).align(16).get();
         GpuBufferSlice gpuBufferSlice3 = GpuUniformBufferHelper.getGpuBufferSlice2(buffer);
         RenderPass renderPass2 = commandEncoder2.createRenderPass(() -> string16, renderTarget5.getColorTextureView(), Optional.empty());

         try {
            renderPass2.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline3));
            renderPass2.setUniform(
               string12, this.textureTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            );
            renderPass2.setUniform(string17, renderTarget4.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass2.setUniform(string13, gpuBufferSlice3);
            renderPass2.draw(3, 1, 0, 0);
         } catch (Throwable throwable3) {
            if (renderPass2 != null) {
               try {
                  renderPass2.close();
               } catch (Throwable throwable4) {
                  throwable3.addSuppressed(throwable4);
               }
            }

            throw throwable3;
         }

         if (renderPass2 != null) {
            renderPass2.close();
         }
      } catch (Throwable throwable5) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable throwable6) {
               throwable5.addSuppressed(throwable6);
            }
         }

         throw throwable5;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }
   }

   public void updateState5(GpuBuffer gpuBuffer5, GpuBuffer gpuBuffer6, IndexType indexType4, int value4, float amount6) {
      this.updateState7(gpuBuffer5, gpuBuffer6, indexType4, value4, amount6, PrimitiveTopology.QUADS);
   }

   private void updateState(int value5, int value6, boolean matches) {
      if (this.textureTarget == null) {
         this.textureTarget = new TextureTarget(string3, value5, value6, GpuFormat.RGBA8_UNORM, null);
      } else if (this.textureTarget.width != value5 || this.textureTarget.height != value6) {
         this.textureTarget.resize(value5, value6);
      }

      if (matches) {
         if (this.textureTarget2 == null) {
            this.textureTarget2 = new TextureTarget(string8, value5, value6, GpuFormat.RGBA8_UNORM, null);
         } else if (this.textureTarget2.width != value5 || this.textureTarget2.height != value6) {
            this.textureTarget2.resize(value5, value6);
         }
      }
   }
}
