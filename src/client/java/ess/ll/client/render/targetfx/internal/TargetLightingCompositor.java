package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.TargetEspPreviewContext;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ess.ll.client.render.targetfx.api.MinecraftContext;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
public final class TargetLightingCompositor implements MinecraftContext {
   private static final int stateCounter = 4;
   private static final int stateCounter3 = 4448;
   private static final RenderPipeline renderPipeline = RenderPipelineLayouts.getRenderPipelineLayouts6()
      .getRenderPipelineLayouts(TargetLightingCompositor.string11)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetLightingCompositor.string9))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetLightingCompositor.string4))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE))
      .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
      .getRenderPipelineLayouts2(TargetLightingCompositor.string7, UniformType.UNIFORM_BUFFER)
      .getRenderPipelineLayouts12(TargetLightingCompositor.string8)
      .getRenderPipelineLayouts12(TargetLightingCompositor.string12)
      .getRenderPipeline();
   private static final Vector3f[] vector3fArray2 = new Vector3f[4];
   private static final Vector3f[] vector3fArray = new Vector3f[4];
   private static final float[] floatArray2 = new float[4];
   private static final float[] floatArray3 = new float[4];
   private static final float[] floatArray = new float[4];
   private static final Matrix4f matrix4f5 = new Matrix4f();
   private static final Matrix4f matrix4f4 = new Matrix4f();
   private static final Matrix4f matrix4f6 = new Matrix4f();
   private static final TargetLightingBlockSampler targetLightingBlockSampler = new TargetLightingBlockSampler();
   private static final Vector3f vector3f = new Vector3f();
   private static final Vector4f vector4f = new Vector4f();
   private static GpuTexture gpuTexture4;
   private static GpuTextureView gpuTextureView;
   private static GpuTexture gpuTexture3;
   private static GpuTextureView gpuTextureView2;
   private static boolean stateActive2;
   private static boolean stateActive;
   private static int stateCounter2;
   private static final String string10 = "dimasik-target-lighting-depth";
   private static final String string3 = "dimasik-target-lighting-color";
   private static final float currentValue18 = 0.02F;
   private static final float currentValue13 = 255.0F;
   private static final float currentValue17 = 0.025F;
   private static final float currentValue2 = 6400.0F;
   private static final float currentValue16 = 1.2F;
   private static final float currentValue15 = 255.0F;
   private static final float currentValue8 = 255.0F;
   private static final float currentValue7 = 255.0F;
   private static final float currentValue19 = 1.6F;
   private static final float currentValue6 = 1.6F;
   private static final float currentValue4 = 1.8F;
   private static final float currentValue14 = 3.8F;
   private static final float currentValue10 = 0.64F;
   private static final float currentValue11 = 0.01F;
   private static final float currentValue5 = 0.5F;
   private static final float currentValue = 0.5F;
   private static final float currentValue3 = 0.5F;
   private static final float currentValue12 = 0.5F;
   private static final float currentValue9 = 4.5F;
   private static final String string5 = "TargetLighting";
   private static final String string2 = "DepthSampler";
   private static final String string = "SceneSampler";
   private static final String string6 = "Target surface lighting";
   private static final String string11 = "dimasik/target_surface_light";
   private static final String string9 = "hands_fullscreen";
   private static final String string4 = "target_surface_light";
   private static final String string7 = "TargetLighting";
   private static final String string8 = "DepthSampler";
   private static final String string12 = "SceneSampler";

   public static void updateState6() {
      stateActive2 = false;
      stateActive = false;
      stateCounter2 = 0;
   }

   public static void updateState3() {
      updateState6();
      if (gpuTextureView != null && !gpuTextureView.isClosed()) {
         gpuTextureView.close();
      }

      if (gpuTexture4 != null && !gpuTexture4.isClosed()) {
         gpuTexture4.close();
      }

      if (gpuTextureView2 != null && !gpuTextureView2.isClosed()) {
         gpuTextureView2.close();
      }

      if (gpuTexture3 != null && !gpuTexture3.isClosed()) {
         gpuTexture3.close();
      }

      gpuTextureView = null;
      gpuTexture4 = null;
      gpuTextureView2 = null;
      gpuTexture3 = null;
      targetLightingBlockSampler.reset();
   }

   public static void updateState5(boolean matches, Matrix4f matrix4f) {
      stateActive = matches && stateActive2;
      stateCounter2 = 0;
      matrix4f5.set(matrix4f);
   }

   public static void updateState4(float amount, float amount2, float amount3, float amount4, int value) {
      updateState2(matrix4f5, amount, amount2, amount3, amount4, value);
   }

   public static void updateState7(Matrix4f matrix4f2) {
      stateActive = false;
      if (stateActive2 && stateCounter2 != 0 && MINECRAFT.level != null && matrix4f2 != null) {
         stateActive2 = false;
         RenderTarget renderTarget = TargetEspPreviewContext.target();
         if (renderTarget != null
            && renderTarget.getColorTextureView() != null
            && gpuTexture4 != null
            && !gpuTexture4.isClosed()
            && gpuTextureView != null
            && !gpuTextureView.isClosed()
            && gpuTexture3 != null
            && !gpuTexture3.isClosed()
            && gpuTextureView2 != null
            && !gpuTextureView2.isClosed()
            && gpuTexture4.getWidth(0) == renderTarget.width
            && gpuTexture4.getHeight(0) == renderTarget.height) {
            matrix4f4.set(matrix4f2).invert();
            matrix4f6.set(matrix4f5).invert();
            if (matrix4f4.isFinite() && matrix4f6.isFinite()) {
               float amount5 = 1.0F;
               float amount6 = 1.0F;
               float amount7 = 0.0F;
               float amount8 = 0.0F;

               for (int index = 0; index < stateCounter2; index++) {
                  Vector3f position = vector3fArray2[index];
                  float amount9 = floatArray[index];
                  if (!(position.z - amount9 >= 0.0F)) {
                     for (int index2 = 0; index2 < 8; index2++) {
                        vector4f.set(
                              position.x + ((index2 & 1) == 0 ? -amount9 : amount9),
                              position.y + ((index2 & 2) == 0 ? -amount9 : amount9),
                              position.z + ((index2 & 4) == 0 ? -amount9 : amount9),
                              1.0F
                           )
                           .mul(matrix4f2);
                        if (vector4f.w <= currentValue11) {
                           amount6 = 0.0F;
                           amount5 = 0.0F;
                           amount8 = 1.0F;
                           amount7 = 1.0F;
                           break;
                        }

                        float amount10 = vector4f.x / vector4f.w * currentValue5 + currentValue;
                        float amount11 = vector4f.y / vector4f.w * currentValue3 + currentValue12;
                        amount5 = Math.min(amount5, amount10);
                        amount6 = Math.min(amount6, amount11);
                        amount7 = Math.max(amount7, amount10);
                        amount8 = Math.max(amount8, amount11);
                     }
                  }
               }

               int value2 = Math.max(0, (int)Math.floor(amount5 * renderTarget.width) - 2);
               int value3 = Math.max(0, (int)Math.floor(amount6 * renderTarget.height) - 2);
               int value4 = Math.min(renderTarget.width, (int)Math.ceil(amount7 * renderTarget.width) + 2);
               int value5 = Math.min(renderTarget.height, (int)Math.ceil(amount8 * renderTarget.height) + 2);
               if (value4 > value2 && value5 > value3) {
                  Vec3 position2 = TargetEspPreviewContext.camera().position();
                  targetLightingBlockSampler.updateBlockOcclusion(
                     matrix4f6, vector3fArray2, floatArray, stateCounter2, position2
                  );
                  MemoryStack memoryStack = MemoryStack.stackPush();

                  try {
                     Std140Builder std140Builder = Std140Builder.onStack(memoryStack, 4448)
                        .putMat4f(matrix4f2)
                        .putMat4f(matrix4f4)
                        .putVec4(1.0F / renderTarget.width, 1.0F / renderTarget.height, stateCounter2, currentValue9);

                     for (int index3 = 0; index3 < 4; index3++) {
                        Vector3f position3 = vector3fArray2[index3];
                        std140Builder.putVec4(position3.x, position3.y, position3.z, floatArray[index3]);
                     }

                     for (int index4 = 0; index4 < 4; index4++) {
                        Vector3f position4 = vector3fArray[index4];
                        std140Builder.putVec4(position4.x, position4.y, position4.z, floatArray3[index4]);
                     }

                     std140Builder.putMat4f(matrix4f6)
                        .putVec4(
                           targetLightingBlockSampler.calculateValue(position2),
                           targetLightingBlockSampler.calculateValue2(position2),
                           targetLightingBlockSampler.getIntArray(position2),
                           2.0F
                        );
                     int[] instance = targetLightingBlockSampler.getOcclusionBits();

                     for (int index5 = 0; index5 < instance.length; index5 += 4) {
                        std140Builder.putIVec4(instance[index5], instance[index5 + 1], instance[index5 + 2], instance[index5 + 3]);
                     }

                     GpuBufferSlice gpuBufferSlice = GpuUniformBufferHelper.getGpuBufferSlice2(std140Builder.get());
                     RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> string6, renderTarget.getColorTextureView(), Optional.empty());

                     try {
                        renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
                        renderPass.setUniform(string5, gpuBufferSlice);
                        renderPass.setUniform(string2, gpuTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                        renderPass.setUniform(string, gpuTextureView2, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                        renderPass.enableScissor(value2, value3, value4 - value2, value5 - value3);
                        renderPass.draw(3, 1, 0, 0);
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
                     if (memoryStack != null) {
                        try {
                           memoryStack.close();
                        } catch (Throwable throwable4) {
                           throwable3.addSuppressed(throwable4);
                        }
                     }

                     throw throwable3;
                  }

                  if (memoryStack != null) {
                     memoryStack.close();
                  }
               }
            }
         }
      }
   }

   private TargetLightingCompositor() {
   }

   static {
      for (int index6 = 0; index6 < 4; index6++) {
         vector3fArray2[index6] = new Vector3f();
         vector3fArray[index6] = new Vector3f();
      }
   }

   public static void updateState2(Matrix4f matrix4f3, float amount12, float amount13, float amount14, float amount15, int value6) {
      if (stateActive && !(amount15 <= currentValue18)) {
         float amount16 = (value6 >>> 24) / currentValue13;
         if (!(amount16 <= currentValue17)) {
            matrix4f3.transformPosition(amount12, amount13, amount14, vector3f);
            if (vector3f.isFinite() && !(vector3f.lengthSquared() > currentValue2)) {
               float amount17 = Math.min(amount15, currentValue16) * amount16;
               int value7 = -1;
               float amount18 = Float.POSITIVE_INFINITY;

               for (int index7 = 0; index7 < stateCounter2; index7++) {
                  float amount19 = vector3fArray2[index7].distanceSquared(vector3f);
                  if (amount19 < amount18) {
                     value7 = index7;
                     amount18 = amount19;
                  }
               }

               float amount20 = (value6 >> 16 & 0xFF) / currentValue15;
               float amount21 = (value6 >> 8 & 0xFF) / currentValue8;
               float amount22 = (value6 & 0xFF) / currentValue7;
               float amount23 = Math.clamp(currentValue19 + amount15 * currentValue6, currentValue4, currentValue14);
               if (value7 >= 0 && (!(amount18 > currentValue10) || stateCounter2 >= 4)) {
                  float amount24 = floatArray2[value7] + amount17;
                  float amount25 = amount17 / amount24;
                  vector3fArray2[value7].lerp(vector3f, amount25);
                  Vector3f position5 = vector3fArray[value7];
                  position5.set(position5.x + (amount20 - position5.x) * amount25, position5.y + (amount21 - position5.y) * amount25, position5.z + (amount22 - position5.z) * amount25);
                  floatArray2[value7] = amount24;
                  floatArray3[value7] = Math.max(floatArray3[value7], amount17);
                  floatArray[value7] = Math.max(floatArray[value7], amount23);
               } else {
                  int value8 = stateCounter2++;
                  vector3fArray2[value8].set(vector3f);
                  vector3fArray[value8].set(amount20, amount21, amount22);
                  floatArray2[value8] = floatArray3[value8] = amount17;
                  floatArray[value8] = amount23;
               }
            }
         }
      }
   }

   public static void updateState() {
      if (!stateActive2 && MINECRAFT.level != null) {
         RenderTarget renderTarget2 = TargetEspPreviewContext.target();
         if (renderTarget2 != null) {
            GpuTexture gpuTexture = renderTarget2.getDepthTexture();
            GpuTexture gpuTexture2 = renderTarget2.getColorTexture();
            if (gpuTexture != null && !gpuTexture.isClosed() && gpuTexture2 != null && !gpuTexture2.isClosed()) {
               int width = gpuTexture.getWidth(0);
               int height = gpuTexture.getHeight(0);
               if (width > 0 && height > 0) {
                  if (gpuTexture4 == null
                     || gpuTexture4.isClosed()
                     || gpuTextureView == null
                     || gpuTextureView.isClosed()
                     || gpuTexture4.getWidth(0) != width
                     || gpuTexture4.getHeight(0) != height
                     || gpuTexture4.getFormat() != gpuTexture.getFormat()
                     || gpuTexture3 == null
                     || gpuTexture3.isClosed()
                     || gpuTextureView2 == null
                     || gpuTextureView2.isClosed()
                     || gpuTexture3.getFormat() != gpuTexture2.getFormat()) {
                     updateState3();
                     gpuTexture4 = RenderSystem.getDevice().createTexture(string10, 5, gpuTexture.getFormat(), width, height, 1, 1);
                     gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture4);
                     gpuTexture3 = RenderSystem.getDevice().createTexture(string3, 5, gpuTexture2.getFormat(), width, height, 1, 1);
                     gpuTextureView2 = RenderSystem.getDevice().createTextureView(gpuTexture3);
                  }

                  CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                  commandEncoder.copyTextureToTexture(gpuTexture, gpuTexture4, 0, 0, 0, 0, 0, width, height);
                  commandEncoder.copyTextureToTexture(gpuTexture2, gpuTexture3, 0, 0, 0, 0, 0, width, height);
                  stateActive2 = true;
               }
            }
         }
      }
   }
}
