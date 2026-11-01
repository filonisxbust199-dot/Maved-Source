package ess.ll.client.render.targetfx.internal;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
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
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.render.targetfx.api.MinecraftContext;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
public final class BloomCompositor implements AutoCloseable, MinecraftContext {
   private static final int stateCounter2 = 32;
   private static final int stateCounter = 80;
   private static final RenderPipeline renderPipeline3 = RenderPipelineLayouts.getRenderPipelineLayouts6()
      .getRenderPipelineLayouts(BloomCompositor.string18)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(BloomCompositor.string13))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(BloomCompositor.string6))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts10()
      .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
      .getRenderPipelineLayouts12(BloomCompositor.string8)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline = RenderPipelineLayouts.getRenderPipelineLayouts6()
      .getRenderPipelineLayouts(BloomCompositor.string23)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(BloomCompositor.string3))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(BloomCompositor.string19))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts10()
      .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
      .getRenderPipelineLayouts2(BloomCompositor.string25, UniformType.UNIFORM_BUFFER)
      .getRenderPipelineLayouts12(BloomCompositor.string)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline2 = RenderPipelineLayouts.getRenderPipelineLayouts6()
      .getRenderPipelineLayouts(BloomCompositor.string15)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(BloomCompositor.string4))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(BloomCompositor.string11))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.LIGHTNING)
      .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
      .getRenderPipelineLayouts2(BloomCompositor.string12, UniformType.UNIFORM_BUFFER)
      .getRenderPipelineLayouts12(BloomCompositor.string16)
      .getRenderPipelineLayouts12(BloomCompositor.string24)
      .getRenderPipeline();
   private static TextureTarget textureTarget2;
   private static TextureTarget textureTarget;
   private static GpuBuffer gpuBuffer4;
   private static GpuBuffer gpuBuffer5;
   private static GpuBuffer gpuBuffer3;
   private static final float currentValue4 = 12.0F;
   private static final String string9 = "InSampler";
   private static final String string17 = "BloomSampler";
   private static final String string28 = "MaskSampler";
   private static final String string5 = "EspOuterGlow";
   private static final String string10 = "InSampler";
   private static final String string27 = "EspBlur";
   private static final String string2 = "entityEspOtherBlurHorizontalUniforms";
   private static final String string14 = "entityEspOtherBlurVerticalUniforms";
   private static final String string22 = "entityEspOtherGlowUniforms";
   private static final float currentValue2 = 0.5F;
   private static final float currentValue8 = 0.5F;
   private static final float currentValue3 = 0.5F;
   private static final String string21 = "entity-esp-other-blur-horizontal";
   private static final String string29 = "entity-esp-other-blur-vertical";
   private static final float currentValue6 = 255.0F;
   private static final float currentValue5 = 255.0F;
   private static final float currentValue7 = 255.0F;
   private static final float currentValue = 255.0F;
   private static final String string20 = "entityEspOtherBlur";
   private static final String string7 = "entityEspOtherGlow";
   private static final String string26 = "entityEspOtherMask";
   private static final String string18 = "dimasik/entity_esp_other_mask";
   private static final String string13 = "hands_fullscreen";
   private static final String string6 = "entity_esp_mask";
   private static final String string8 = "InSampler";
   private static final String string23 = "dimasik/entity_esp_other_blur";
   private static final String string3 = "hands_fullscreen";
   private static final String string19 = "entity_esp_gaussian_blur";
   private static final String string25 = "EspBlur";
   private static final String string = "InSampler";
   private static final String string15 = "dimasik/entity_esp_other_glow";
   private static final String string4 = "hands_fullscreen";
   private static final String string11 = "entity_esp_outer_glow";
   private static final String string12 = "EspOuterGlow";
   private static final String string16 = "BloomSampler";
   private static final String string24 = "MaskSampler";

   private static void updateState3(
      CommandEncoder commandEncoder, RenderTarget renderTarget, int value, float amount, float amount2, boolean matches, float amount3, int value2, float amount4
   ) {
      gpuBuffer3 = getGpuBuffer(gpuBuffer3, 80, string22);
      float amount5 = calculateValue(value);
      float amount6 = calculateValue3(value);
      float amount7 = calculateValue2(value);
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer buffer = Std140Builder.onStack(memoryStack, 80)
            .putVec4(amount, matches ? 1.0F : 0.0F, amount2, amount3)
            .putVec4(amount5, amount6, amount7, 1.0F)
            .putVec4(amount5 * currentValue2, amount6 * currentValue8, amount7 * currentValue3, amount4)
            .putVec4(calculateValue(value2), calculateValue3(value2), calculateValue2(value2), calculateValue4(value2))
            .putVec4(renderTarget.width, renderTarget.height, amount4, 0.0F)
            .get();
         commandEncoder.writeToBuffer(gpuBuffer3.slice(), buffer);
      } catch (Throwable throwable) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable throwable2) {
               throwable.addSuppressed(throwable2);
            }
         }

         throw throwable;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }
   }

   private static GpuBuffer getGpuBuffer(GpuBuffer gpuBuffer, int value3, String text) {
      return gpuBuffer != null && !gpuBuffer.isClosed() ? gpuBuffer : RenderSystem.getDevice().createBuffer(() -> text, 136, value3);
   }

   private static float calculateValue2(int value4) {
      return (value4 & 0xFF) / currentValue7;
   }

   private static boolean checkCondition(RenderTarget renderTarget2) {
      return renderTarget2 != null && renderTarget2.getColorTextureView() != null;
   }

   private static void updateState5(int value5, int value6) {
      if (textureTarget2 == null || textureTarget == null) {
         textureTarget2 = new TextureTarget(string21, value5, value6, GpuFormat.RGBA8_UNORM, null);
         textureTarget = new TextureTarget(string29, value5, value6, GpuFormat.RGBA8_UNORM, null);
      } else if (textureTarget2.width != value5 || textureTarget2.height != value6) {
         textureTarget2.resize(value5, value6);
         textureTarget.resize(value5, value6);
      }
   }

   public static void updateState2(
      RenderTarget renderTarget3, RenderTarget renderTarget4, int value7, float amount8, float amount9, float amount10, boolean matches2, float amount11, int value8, float amount12
   ) {
      if (checkCondition(renderTarget3) && checkCondition(renderTarget4)) {
         int value9 = Math.max(1, renderTarget3.width / 2);
         int value10 = Math.max(1, renderTarget3.height / 2);
         float amount13 = Math.clamp(amount8, 1.0F, currentValue4);
         updateState5(value9, value10);
         CommandEncoder commandEncoder2 = RenderSystem.getDevice().createCommandEncoder();
         updateState4(commandEncoder2, value9, value10, amount13);
         updateState3(commandEncoder2, renderTarget3, value7, amount9, amount10, matches2, amount11, value8, amount12);
         RenderPass renderPass = commandEncoder2.createRenderPass(() -> string26, textureTarget2.getColorTextureView(), Optional.of(new Vector4f(0.0F)));

         try {
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline3));
            renderPass.setUniform(string9, renderTarget3.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.draw(3, 1, 0, 0);
         } catch (Throwable throwable3) {
            if (renderPass != null) {
               try {
                  renderPass.close();
               } catch (Throwable throwable4) {
                  throwable3.addSuppressed(throwable4);
               }
            }

            throw throwable3;
         }

         if (renderPass != null) {
            renderPass.close();
         }

         updateState6(commandEncoder2, textureTarget2, textureTarget, gpuBuffer4);
         updateState6(commandEncoder2, textureTarget, textureTarget2, gpuBuffer5);
         renderPass = commandEncoder2.createRenderPass(() -> string7, renderTarget4.getColorTextureView(), Optional.empty());

         try {
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline2));
            renderPass.setUniform(
               string17, textureTarget2.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            );
            renderPass.setUniform(string28, renderTarget3.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.setUniform(string5, gpuBuffer3);
            renderPass.draw(3, 1, 0, 0);
         } catch (Throwable throwable5) {
            if (renderPass != null) {
               try {
                  renderPass.close();
               } catch (Throwable throwable6) {
                  throwable5.addSuppressed(throwable6);
               }
            }

            throw throwable5;
         }

         if (renderPass != null) {
            renderPass.close();
         }
      }
   }

   private static void updateState6(CommandEncoder commandEncoder3, RenderTarget renderTarget5, RenderTarget renderTarget6, GpuBuffer gpuBuffer2) {
      RenderPass renderPass2 = commandEncoder3.createRenderPass(() -> string20, renderTarget6.getColorTextureView(), Optional.of(new Vector4f(0.0F)));

      try {
         renderPass2.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
         renderPass2.setUniform(string10, renderTarget5.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         renderPass2.setUniform(string27, gpuBuffer2);
         renderPass2.draw(3, 1, 0, 0);
      } catch (Throwable throwable7) {
         if (renderPass2 != null) {
            try {
               renderPass2.close();
            } catch (Throwable throwable8) {
               throwable7.addSuppressed(throwable8);
            }
         }

         throw throwable7;
      }

      if (renderPass2 != null) {
         renderPass2.close();
      }
   }

   private static float calculateValue3(int value11) {
      return (value11 >>> 8 & 0xFF) / currentValue5;
   }

   private static float calculateValue4(int value12) {
      return (value12 >>> 24 & 0xFF) / currentValue;
   }

   @Override
   public void close() {
      if (textureTarget2 != null) {
         textureTarget2.destroyBuffers();
         textureTarget2 = null;
      }

      if (textureTarget != null) {
         textureTarget.destroyBuffers();
         textureTarget = null;
      }

      if (gpuBuffer4 != null) {
         gpuBuffer4.close();
         gpuBuffer4 = null;
      }

      if (gpuBuffer5 != null) {
         gpuBuffer5.close();
         gpuBuffer5 = null;
      }

      if (gpuBuffer3 != null) {
         gpuBuffer3.close();
         gpuBuffer3 = null;
      }
   }

   public static void updateState(RenderTarget renderTarget7, RenderTarget renderTarget8, int value13, float amount14, float amount15, float amount16) {
      updateState2(renderTarget7, renderTarget8, value13, amount14, amount15, amount16, false, 0.0F, 0, 0.0F);
   }

   private static void updateState4(CommandEncoder commandEncoder4, int value14, int value15, float amount17) {
      gpuBuffer4 = getGpuBuffer(gpuBuffer4, 32, string2);
      gpuBuffer5 = getGpuBuffer(gpuBuffer5, 32, string14);
      MemoryStack memoryStack2 = MemoryStack.stackPush();

      try {
         ByteBuffer buffer2 = Std140Builder.onStack(memoryStack2, 32).putVec2(1.0F / value14, 1.0F / value15).putVec2(1.0F, 0.0F).putFloat(amount17).align(16).get();
         commandEncoder4.writeToBuffer(gpuBuffer4.slice(), buffer2);
         ByteBuffer buffer3 = Std140Builder.onStack(memoryStack2, 32).putVec2(1.0F / value14, 1.0F / value15).putVec2(0.0F, 1.0F).putFloat(amount17).align(16).get();
         commandEncoder4.writeToBuffer(gpuBuffer5.slice(), buffer3);
      } catch (Throwable throwable9) {
         if (memoryStack2 != null) {
            try {
               memoryStack2.close();
            } catch (Throwable throwable10) {
               throwable9.addSuppressed(throwable10);
            }
         }

         throw throwable9;
      }

      if (memoryStack2 != null) {
         memoryStack2.close();
      }
   }

   private static float calculateValue(int value16) {
      return (value16 >>> 16 & 0xFF) / currentValue6;
   }
}
