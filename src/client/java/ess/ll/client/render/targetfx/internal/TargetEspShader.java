package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.TargetEspPreviewContext;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import ess.ll.client.render.targetfx.internal.StringConcatSupport;
import ess.ll.client.render.DimasikRenderPipelinesAccessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import ess.ll.client.render.targetfx.internal.DynamicUniformStorage;
import ess.ll.client.render.targetfx.internal.DynamicUniformStorage.DynamicUniform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class TargetEspShader {
   public static final String string7 = "Auto";
   public static final String string23 = "Always";
   public static final String string10 = "Never";
   private static final Minecraft minecraft2 = Minecraft.getInstance();
   private static final Logger logger = LoggerFactory.getLogger(TargetEspShader.string22);
   private static final List<Runnable> list = new ArrayList<>();
   private static final BlendFunction blendFunction2 = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO);
   private static final BlendFunction blendFunction = new BlendFunction(
      BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA
   );
   private static final RenderPipeline renderPipeline5 = getRenderPipeline2(TargetEspShader.string13, CompareOp.ALWAYS_PASS);
   private static final RenderPipeline renderPipeline6 = getRenderPipeline2(
      TargetEspShader.string26, CompareOp.GREATER_THAN_OR_EQUAL
   );
   private static final RenderPipeline renderPipeline3 = DimasikRenderPipelinesAccessor.register(
      RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspShader.string31))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspShader.string37))
         .getRenderPipelineLayouts12(TargetEspShader.string12)
         .getRenderPipelineLayouts2(TargetEspShader.string30, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts7(BlendFunction.OVERLAY)
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION, PrimitiveTopology.TRIANGLE_STRIP)
         .getRenderPipelineLayouts15(Identifier.fromNamespaceAndPath(TargetEspShader.string14, TargetEspShader.string38))
         .getRenderPipeline()
   );
   private static final RenderPipeline renderPipeline4 = DimasikRenderPipelinesAccessor.register(
      RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspShader.string20))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspShader.string6))
         .getRenderPipelineLayouts12(TargetEspShader.string)
         .getRenderPipelineLayouts2(TargetEspShader.string24, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts7(blendFunction)
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION, PrimitiveTopology.TRIANGLE_STRIP)
         .getRenderPipelineLayouts15(Identifier.fromNamespaceAndPath(TargetEspShader.string3, TargetEspShader.string2))
         .getRenderPipeline()
   );
   private static final DynamicUniformStorage<TargetEspShader.szGtUBFAEr0CJ7Uv> dynamicUniformStorage3 = new DynamicUniformStorage(
      TargetEspShader.string28, 96, 256
   );
   private static final DynamicUniformStorage<TargetEspShader.abXuACoQvVDQJPr4> dynamicUniformStorage2 = new DynamicUniformStorage(
      TargetEspShader.string4, 16, 256
   );
   private static final DynamicUniformStorage<TargetEspShader.abXuACoQvVDQJPr4> dynamicUniformStorage = new DynamicUniformStorage(
      TargetEspShader.string16, 16, 256
   );
   private static GpuBuffer gpuBuffer2;
   private static long lastUpdateMillis5;
   private static GpuBuffer gpuBuffer3;
   private static GpuSampler gpuSampler;
   private static TextureTarget textureTarget5;
   private static TextureTarget textureTarget3;
   private static TextureTarget textureTarget4;
   private static long lastUpdateMillis3;
   private static float currentValue35;
   private static boolean stateActive;
   private static final String string27 = "target/lightning_line";
   private static final String string18 = "target/lightning";
   private static final String string17 = "LightningLine";
   private static final String string42 = "dimasik";
   private static final float currentValue31 = 12.0F;
   private static final float currentValue2 = 0.007843138F;
   private static final String string8 = "Lightning TargetESP disabled after a render failure";
   private static final float currentValue9 = 0.016666668F;
   private static final float currentValue39 = 1.0E9F;
   private static final float currentValue43 = 0.05F;
   private static final float currentValue15 = 0.75F;
   private static final float currentValue24 = 0.16F;
   private static final double currentValue36 = 0.35;
   private static final int stateCounter4 = -16777216;
   private static final int stateCounter2 = 16718349;
   private static final float currentValue46 = 0.5F;
   private static final float currentValue38 = 0.085F;
   private static final float currentValue45 = 10.0F;
   private static final float currentValue30 = 22.0F;
   private static final float currentValue34 = 0.4F;
   private static final float currentValue51 = 9.0F;
   private static final float currentValue10 = 6.0F;
   private static final float currentValue20 = 1.3F;
   private static final float currentValue21 = (float) (Math.PI * 2);
   private static final float currentValue64 = 0.08F;
   private static final float currentValue49 = 0.42F;
   private static final float currentValue55 = 0.3F;
   private static final float currentValue18 = 0.55F;
   private static final float currentValue47 = 0.45F;
   private static final float currentValue23 = 0.01F;
   private static final float currentValue7 = 0.16F;
   private static final float currentValue3 = 1.7F;
   private static final float currentValue14 = 2.1F;
   private static final float currentValue29 = (float) Math.PI;
   private static final float currentValue50 = 0.6F;
   private static final float currentValue37 = 0.5F;
   private static final float currentValue6 = -1.0F;
   private static final float currentValue60 = 0.85F;
   private static final float currentValue42 = 0.05F;
   private static final float currentValue41 = 0.67F;
   private static final float currentValue33 = 0.12F;
   private static final float currentValue63 = 0.9F;
   private static final float currentValue62 = 10.0F;
   private static final float currentValue17 = 0.5F;
   private static final float currentValue12 = 1.0E-4F;
   private static final float currentValue48 = 0.9F;
   private static final float currentValue4 = 0.9F;
   private static final float currentValue40 = 1.0E-5F;
   private static final float currentValue11 = 0.3F;
   private static final float currentValue53 = 0.5F;
   private static final float currentValue5 = 0.5F;
   private static final float currentValue66 = 0.5F;
   private static final float currentValue13 = 0.5F;
   private static final float currentValue26 = 0.45F;
   private static final float currentValue = 0.4F;
   private static final float currentValue19 = 0.4F;
   private static final float currentValue57 = 0.4F;
   private static final float currentValue27 = 1.0E-5F;
   private static final float currentValue58 = 0.75F;
   private static final float currentValue54 = 0.55F;
   private static final float currentValue61 = 0.7F;
   private static final float currentValue52 = 1.0E-5F;
   private static final float currentValue8 = 3.0F;
   private static final int stateCounter3 = 374761393;
   private static final int stateCounter5 = 668265263;
   private static final int stateCounter6 = 1274126177;
   private static final int stateCounter = 1274126177;
   private static final int stateCounter7 = 65535;
   private static final float currentValue16 = 32768.0F;
   private static final String string19 = "LightningLine";
   private static final String string39 = "Dimasik TargetESP lightning blur horizontal";
   private static final float currentValue59 = 0.5F;
   private static final String string36 = "Dimasik TargetESP lightning blur vertical";
   private static final String string25 = "LightningComposite";
   private static final String string33 = "Sampler0";
   private static final String string21 = "LightningBlur";
   private static final String string34 = "Sampler0";
   private static final String string15 = "Always";
   private static final String string40 = "Never";
   private static final double currentValue25 = 1.0E-8;
   private static final double currentValue65 = 0.5;
   private static final float currentValue56 = 36.0F;
   private static final int stateCounter8 = 16777215;
   private static final String string11 = "Dimasik TargetESP lightning capture";
   private static final String string9 = "Dimasik TargetESP lightning blur A";
   private static final String string32 = "Dimasik TargetESP lightning blur B";
   private static final long lastUpdateMillis = 8192L;
   private static final float currentValue22 = -1.0F;
   private static final float currentValue28 = -1.0F;
   private static final float currentValue32 = -1.0F;
   private static final float currentValue44 = -1.0F;
   private static final long lastUpdateMillis4 = 4L;
   private static final long lastUpdateMillis2 = 4L;
   private static final String string41 = "Dimasik TargetESP lightning fullscreen quad";
   private static final String string29 = "Dimasik TargetESP lightning vertices";
   private static final String string35 = "Dimasik TargetESP lightning bloom composite";
   private static final String string5 = "Dimasik TargetESP lightning";
   private static final String string22 = "dimasik-target-lightning";
   private static final String string13 = "target_lightning";
   private static final String string26 = "target_lightning_depth";
   private static final String string31 = "esp/esp_fullscreen";
   private static final String string37 = "target/lightning_blur";
   private static final String string12 = "Sampler0";
   private static final String string30 = "LightningBlur";
   private static final String string14 = "dimasik";
   private static final String string38 = "pipeline/target_lightning_blur";
   private static final String string20 = "esp/esp_fullscreen";
   private static final String string6 = "target/lightning_composite";
   private static final String string = "Sampler0";
   private static final String string24 = "LightningComposite";
   private static final String string3 = "dimasik";
   private static final String string2 = "pipeline/target_lightning_composite";
   private static final String string28 = "Target lightning line UBO";
   private static final String string4 = "Target lightning blur UBO";
   private static final String string16 = "Target lightning composite UBO";

   private static void getGpuSampler12(RenderTarget renderTarget, float amount, float amount2, boolean matches, float amount3) {
      if (textureTarget5 != null && textureTarget3 != null && textureTarget4 != null && gpuBuffer3 != null) {
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         updateState10(commandEncoder, textureTarget5.getColorTextureView(), textureTarget4, amount, string39);
         updateState10(
            commandEncoder, textureTarget4.getColorTextureView(), textureTarget3, -amount * currentValue59, string36
         );
         if (matches) {
            DeadChamsCompositeShader.deadChamsCompositeShader.updateState6(textureTarget3, amount3);
         }

         GpuBufferSlice gpuBufferSlice = dynamicUniformStorage.writeUniform(new TargetEspShader.abXuACoQvVDQJPr4(amount2));
         RenderPass renderPass = commandEncoder.createRenderPass(() -> string35, renderTarget.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty());

         try {
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline4));
            renderPass.setUniform(string25, gpuBufferSlice);
            renderPass.setUniform(string33, textureTarget3.getColorTextureView(), getGpuSampler7());
            renderPass.setVertexBuffer(0, gpuBuffer3.slice());
            renderPass.draw(4, 1, 0, 0);
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
      }
   }

   private static void getGpuSampler9() {
      getGpuSampler3(textureTarget5);
      getGpuSampler3(textureTarget3);
      getGpuSampler3(textureTarget4);
      textureTarget5 = null;
      textureTarget3 = null;
      textureTarget4 = null;
      updateState3(gpuBuffer2);
      gpuBuffer2 = null;
      lastUpdateMillis5 = 0L;
      updateState3(gpuBuffer3);
      gpuBuffer3 = null;
      gpuSampler = null;
   }

   private static void getGpuSampler2(
      Camera camera, float amount4, LivingEntity entity, float amount5, int value2, boolean matches2, String text, boolean matches3, float amount6
   ) {
      RenderTarget renderTarget2 = TargetEspPreviewContext.target();
      if (renderTarget2 != null && renderTarget2.width > 0 && renderTarget2.height > 0) {
         updateState5(renderTarget2.width, renderTarget2.height);
         getGpuSampler14(RenderSystem.getDevice());
         long nowNanos = System.nanoTime();
         float amount7 = lastUpdateMillis3 == 0L
            ? currentValue9
            : Math.min((float)(nowNanos - lastUpdateMillis3) / currentValue39, currentValue43);
         lastUpdateMillis3 = nowNanos;
         currentValue35 += amount7;
         Vec3 position = camera.position();
         float x = (float)(Mth.lerp(amount4, entity.xOld, entity.getX()) - position.x);
         float y = (float)(Mth.lerp(amount4, entity.yOld, entity.getY()) - position.y);
         float z = (float)(Mth.lerp(amount4, entity.zOld, entity.getZ()) - position.z);
         float amount8 = entity.getBbHeight();
         float amount9 = entity.getBbWidth() * currentValue15 + currentValue24;
         int value3 = value2;
         if (matches2 && entity.hurtTime > 0) {
            float amount10 = Math.max(0.0F, entity.hurtTime - amount4);
            float amount11 = Math.max(1.0F, (float)entity.hurtDuration);
            float amount12 = Mth.clamp(amount10 / amount11, 0.0F, 1.0F);
            float amount13 = (float)Math.pow(amount12, currentValue36);
            value3 = getGpuSampler5(value2, value2 & stateCounter4 | stateCounter2, amount13);
         }

         boolean matches4 = checkCondition(text, position, entity);
         RenderPipeline renderPipeline = matches4 ? renderPipeline6 : renderPipeline5;
         Matrix4f matrix4f = getGpuSampler6();
         if (matrix4f != null) {
            float amount14 = y + amount8 * currentValue46;
            double amount15 = Math.sqrt(x * x + amount14 * amount14 + z * z);
            float amount16 = calculateValue2(currentValue38, amount15, matrix4f, renderTarget2.height);
            MeshData meshData = getGpuSampler11(x, y, z, amount8, amount9, value3, amount5, currentValue45, 1.0F);
            if (meshData != null) {
               updateState9(
                  meshData,
                  renderPipeline,
                  matrix4f,
                  renderTarget2.width,
                  renderTarget2.height,
                  textureTarget5.getColorTextureView(),
                  renderTarget2.getDepthTextureView(),
                  true
               );
               getGpuSampler12(renderTarget2, amount16, amount5, matches3, amount6);
            }

            MeshData meshData2 = getGpuSampler11(x, y, z, amount8, amount9, value3, amount5, currentValue30, currentValue34);
            updateState9(
               meshData2, renderPipeline, matrix4f, renderTarget2.width, renderTarget2.height, renderTarget2.getColorTextureView(), renderTarget2.getDepthTextureView(), false
            );
            MeshData meshData3 = getGpuSampler11(x, y, z, amount8, amount9, value3, amount5, currentValue51, 1.0F);
            updateState9(
               meshData3, renderPipeline, matrix4f, renderTarget2.width, renderTarget2.height, renderTarget2.getColorTextureView(), renderTarget2.getDepthTextureView(), false
            );
         }
      }
   }

   private static int getGpuSampler13(int value4, float amount17) {
      int value5 = Math.round((value4 >>> 24 & 0xFF) * Mth.clamp(amount17, 0.0F, 1.0F));
      return value5 << 24 | value4 & stateCounter8;
   }

   private static void getGpuSampler4(
      BufferBuilder bufferBuilder, float amount18, float amount19, float amount20, float amount21, float amount22, float amount23, int value6, int value7, float amount24
   ) {
      float amount25 = amount21 - amount18;
      float amount26 = amount22 - amount19;
      float amount27 = amount23 - amount20;
      float amount28 = (float)Math.sqrt(amount25 * amount25 + amount26 * amount26 + amount27 * amount27);
      if (!(amount28 < currentValue52)) {
         float amount29 = 1.0F / amount28;
         float amount30 = amount25 * amount29;
         float amount31 = amount26 * amount29;
         float amount32 = amount27 * amount29;
         bufferBuilder.addVertex(amount18, amount19, amount20).setColor(value6).setNormal(amount30, amount31, amount32).setLineWidth(amount24);
         bufferBuilder.addVertex(amount21, amount22, amount23).setColor(value7).setNormal(amount30, amount31, amount32).setLineWidth(amount24);
      }
   }

   private static void getGpuSampler(int value8) {
      if (gpuBuffer2 == null || gpuBuffer2.isClosed() || lastUpdateMillis5 < value8) {
         updateState3(gpuBuffer2);
         long value9 = lastUpdateMillis;

         while (value9 < value8) {
            value9 <<= 1;
         }

         gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> string29, 40, value9);
         lastUpdateMillis5 = value9;
      }
   }

   private static void getGpuSampler3(TextureTarget textureTarget) {
      if (textureTarget != null) {
         list.add(textureTarget::destroyBuffers);
      }
   }

   private TargetEspShader() {
   }

   public static void updateState2() {
      getGpuSampler9();
      updateState7();
      lastUpdateMillis3 = 0L;
      currentValue35 = 0.0F;
      stateActive = false;
   }

   private static Matrix4f getGpuSampler6() {
      Matrix4f matrix4f2 = ScreenProjectionMath.projectionMatrixCopy();
      Matrix4f matrix4f3 = ScreenProjectionMath.viewMatrixCopy();
      return matrix4f2 != null && matrix4f3 != null ? matrix4f2.mul(matrix4f3) : null;
   }

   private static void getGpuSampler14(GpuDevice gpuDevice) {
      if (gpuBuffer3 == null || gpuBuffer3.isClosed()) {
         gpuBuffer3 = null;
         float[] instance = new float[]{
            currentValue22, currentValue28, 0.0F, 1.0F, currentValue32, 0.0F, currentValue44, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F
         };
         ByteBuffer buffer = ByteBuffer.allocateDirect(instance.length * 4).order(ByteOrder.nativeOrder());

         for (float index : instance) {
            buffer.putFloat(index);
         }

         buffer.flip();
         gpuBuffer3 = gpuDevice.createBuffer(() -> string41, 40, instance.length * lastUpdateMillis4);
         gpuDevice.createCommandEncoder().writeToBuffer(gpuBuffer3.slice(0L, instance.length * lastUpdateMillis2), buffer);
      }
   }

   public static void getGpuSampler8(
      Camera camera2, float amount33, LivingEntity entity2, float amount34, int value10, boolean matches5, String text2, boolean matches6, float amount35
   ) {
      if (!stateActive
         && camera2 != null
         && entity2 != null
         && !(amount34 <= currentValue2)
         && minecraft2.level != null
         && minecraft2.player != null) {
         try {
            getGpuSampler2(camera2, amount33, entity2, amount34, value10, matches5, text2, matches6, amount35);
         } catch (Throwable throwable3) {
            stateActive = true;
            logger.error(string8, throwable3);
            getGpuSampler9();
         }
      }
   }

   private static int getGpuSampler5(int value11, int value12, float amount36) {
      float amount37 = Mth.clamp(amount36, 0.0F, 1.0F);
      int value13 = Mth.lerpInt(amount37, value11 >>> 24 & 0xFF, value12 >>> 24 & 0xFF);
      int value14 = Mth.lerpInt(amount37, value11 >>> 16 & 0xFF, value12 >>> 16 & 0xFF);
      int value15 = Mth.lerpInt(amount37, value11 >>> 8 & 0xFF, value12 >>> 8 & 0xFF);
      int value16 = Mth.lerpInt(amount37, value11 & 0xFF, value12 & 0xFF);
      return value13 << 24 | value14 << 16 | value15 << 8 | value16;
   }

   private static void getGpuSampler10(
      BufferBuilder bufferBuilder2,
      float amount38,
      float amount39,
      float amount40,
      float amount41,
      float amount42,
      float amount43,
      int value17,
      float amount44,
      float amount45,
      float amount46,
      int value18,
      int value19,
      int value20,
      int value21,
      float amount47
   ) {
      float amount48 = amount41 - amount38;
      float amount49 = amount42 - amount39;
      float amount50 = amount43 - amount40;
      float amount51 = (float)Math.sqrt(amount48 * amount48 + amount49 * amount49 + amount50 * amount50);
      if (value17 > 0 && !(amount51 < currentValue12)) {
         float amount52 = amount48 / amount51;
         float amount53 = amount49 / amount51;
         float amount54 = amount50 / amount51;
         float amount55 = Math.abs(amount53) < currentValue48 ? 0.0F : 1.0F;
         float amount56 = Math.abs(amount53) < currentValue4 ? 1.0F : 0.0F;
         float amount57 = -amount54 * amount56;
         float amount58 = amount54 * amount55;
         float amount59 = amount52 * amount56 - amount53 * amount55;
         float amount60 = (float)Math.sqrt(amount57 * amount57 + amount58 * amount58 + amount59 * amount59);
         if (amount60 < currentValue40) {
            amount60 = 1.0F;
         }

         amount57 /= amount60;
         amount58 /= amount60;
         amount59 /= amount60;
         float amount61 = amount53 * amount59 - amount54 * amount58;
         float amount62 = amount54 * amount57 - amount52 * amount59;
         float amount63 = amount52 * amount58 - amount53 * amount57;
         float amount64 = calculateValue3(value19, 0, 0, value21, amount47);
         float amount65 = calculateValue3(value19, 1, 0, value21, amount47);
         float amount66 = amount51 * currentValue11;
         float amount67 = (amount38 + amount41) * currentValue53 + (amount57 * amount64 + amount61 * amount65) * amount66;
         float amount68 = (amount39 + amount42) * currentValue5 + (amount58 * amount64 + amount62 * amount65) * amount66;
         float amount69 = (amount40 + amount43) * currentValue66 + (amount59 * amount64 + amount63 * amount65) * amount66;
         float amount70 = (amount45 + amount46) * currentValue13;
         getGpuSampler10(bufferBuilder2, amount38, amount39, amount40, amount67, amount68, amount69, value17 - 1, amount44, amount45, amount70, value18, value19 * 2 + 7, value20, value21, amount47);
         getGpuSampler10(bufferBuilder2, amount67, amount68, amount69, amount41, amount42, amount43, value17 - 1, amount44, amount70, amount46, value18, value19 * 2 + 9, value20, value21, amount47);
         if (value20 < 2 && value17 >= 2 && calculateValue3(value19, 2, 0, value21, amount47) > currentValue26) {
            float amount71 = calculateValue3(value19, 3, 0, value21, amount47);
            float amount72 = calculateValue3(value19, 4, 0, value21, amount47);
            float amount73 = amount52 * currentValue + amount57 * amount71 + amount61 * amount72;
            float amount74 = amount53 * currentValue19 + amount58 * amount71 + amount62 * amount72;
            float amount75 = amount54 * currentValue57 + amount59 * amount71 + amount63 * amount72;
            float amount76 = (float)Math.sqrt(amount73 * amount73 + amount74 * amount74 + amount75 * amount75);
            if (amount76 < currentValue27) {
               amount76 = 1.0F;
            }

            float amount77 = amount51 * currentValue58 / amount76;
            getGpuSampler10(
               bufferBuilder2,
               amount67,
               amount68,
               amount69,
               amount67 + amount73 * amount77,
               amount68 + amount74 * amount77,
               amount69 + amount75 * amount77,
               Math.min(value17 - 1, 3),
               amount44 * currentValue54,
               amount70 * currentValue61,
               0.0F,
               value18,
               value19 * 3 + 13,
               value20 + 1,
               value21,
               amount47
            );
         }
      } else {
         getGpuSampler4(bufferBuilder2, amount38, amount39, amount40, amount41, amount42, amount43, getGpuSampler13(value18, amount45), getGpuSampler13(value18, amount46), amount44);
      }
   }

   private static MeshData getGpuSampler11(float amount78, float amount79, float amount80, float amount81, float amount82, int value22, float amount83, float amount84, float amount85) {
      BufferBuilder bufferBuilder3 = GpuResourceScope.getGpuResourceScope()
         .getBufferBuilder(PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
      byte amount86 = 3;
      byte amount87 = 3;
      float amount88 = currentValue35 * currentValue10;
      int value23 = (int)amount88;
      float amount89 = amount88 - value23;
      float amount90 = currentValue20;
      float amount91 = currentValue21;

      for (int index2 = 0; index2 < amount86; index2++) {
         float amount92 = currentValue35 * amount90 + (float)index2 / amount86;
         int value24 = (int)Math.floor(amount92);
         float amount93 = amount92 - value24;
         float amount94 = Mth.clamp(amount93 / currentValue64, 0.0F, 1.0F);
         float amount95 = 1.0F - Mth.clamp((amount93 - currentValue49) / currentValue55, 0.0F, 1.0F);
         float amount96 = Mth.clamp(currentValue18 + currentValue47 * calculateValue3(index2, 900, 0, value23, amount89), 0.0F, 1.0F);
         float amount97 = amount94 * amount95 * amount96;
         if (!(amount97 <= currentValue23)) {
            float amount98 = amount83 * amount97 * amount85;
            float amount99 = amount81 * currentValue7 * (float)Math.sin(currentValue35 * currentValue3 + index2 * currentValue14);
            float amount100 = calculateValue(index2, 11, value24) * currentValue29;
            float amount101 = currentValue50 + currentValue37 * Math.abs(calculateValue(index2, 12, value24));
            float amount102 = calculateValue(index2, 13, value24) >= 0.0F ? 1.0F : currentValue6;
            float amount103 = amount79 + amount81 * (currentValue60 + currentValue42 * calculateValue(index2, 14, value24));
            float amount104 = amount81 * currentValue41;
            float amount105 = amount82 * (1.0F + currentValue33 * calculateValue(index2, 15, value24));
            float amount106 = 0.0F;
            float amount107 = 0.0F;
            float amount108 = 0.0F;

            for (int index3 = 0; index3 <= amount87; index3++) {
               float amount109 = (float)index3 / amount87;
               float amount110 = amount100 + currentValue35 * currentValue63 + amount102 * amount101 * amount91 * amount109;
               float amount111 = amount78 + (float)Math.cos(amount110) * amount105;
               float amount112 = amount80 + (float)Math.sin(amount110) * amount105;
               float amount113 = amount103 - amount104 * amount109 + amount99;
               if (amount84 == currentValue62 && index3 > 0 && index3 < amount87) {
                  TargetLightingCompositor.updateState4(amount111, amount113, amount112, currentValue17, getGpuSampler13(value22, amount98));
               }

               if (index3 > 0) {
                  int value25 = (index2 * 131 + index3) * 17 + value24 * 101 | 1;
                  getGpuSampler10(bufferBuilder3, amount106, amount107, amount108, amount111, amount113, amount112, 4, amount84, amount98, amount98, value22, value25, 0, value23, amount89);
               }

               amount106 = amount111;
               amount107 = amount113;
               amount108 = amount112;
            }
         }
      }

      return bufferBuilder3.build();
   }

   private static GpuSampler getGpuSampler7() {
      if (gpuSampler == null) {
         gpuSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
      }

      return gpuSampler;
   }

   public static void updateState8(Camera camera3, float amount114, LivingEntity entity3, float amount115, int value26, boolean matches7, String text3) {
      getGpuSampler8(camera3, amount114, entity3, amount115, value26, matches7, text3, false, currentValue31);
   }

   private static float calculateValue(int value27, int value28, int value29) {
      int value30 = value27 * stateCounter3 + value28 * stateCounter5 + value29 * stateCounter6;
      value30 = (value30 ^ value30 >>> 13) * stateCounter;
      value30 ^= value30 >>> 16;
      return (value30 & stateCounter7) / currentValue16 - 1.0F;
   }

   public static void updateState() {
      dynamicUniformStorage3.endFrame();
      dynamicUniformStorage2.endFrame();
      dynamicUniformStorage.endFrame();
      updateState7();
   }

   private static void updateState10(CommandEncoder commandEncoder2, GpuTextureView gpuTextureView, TextureTarget textureTarget2, float amount116, String text4) {
      GpuBufferSlice gpuBufferSlice2 = dynamicUniformStorage2.writeUniform(new TargetEspShader.abXuACoQvVDQJPr4(amount116));
      RenderPass renderPass2 = commandEncoder2.createRenderPass(() -> text4, textureTarget2.getColorTextureView(), Optional.of(new Vector4f(0.0F)), null, OptionalDouble.empty());

      try {
         renderPass2.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline3));
         renderPass2.setUniform(string21, gpuBufferSlice2);
         renderPass2.setUniform(string34, gpuTextureView, getGpuSampler7());
         renderPass2.setVertexBuffer(0, gpuBuffer3.slice());
         renderPass2.draw(4, 1, 0, 0);
      } catch (Throwable throwable4) {
         if (renderPass2 != null) {
            try {
               renderPass2.close();
            } catch (Throwable throwable5) {
               throwable4.addSuppressed(throwable5);
            }
         }

         throw throwable4;
      }

      if (renderPass2 != null) {
         renderPass2.close();
      }
   }

   private static void updateState7() {
      for (int index4 = 0; index4 < list.size(); index4++) {
         try {
            list.get(index4).run();
         } catch (Throwable throwable6) {
         }
      }

      list.clear();
   }

   private static void updateState9(
      MeshData meshData4, RenderPipeline renderPipeline2, Matrix4f matrix4f4, int value31, int value32, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, boolean matches8
   ) {
      if (meshData4 != null && gpuTextureView2 != null) {
         MeshData meshData5 = meshData4;

         label72: {
            try {
               ByteBuffer buffer2 = meshData4.vertexBuffer();
               int value33 = buffer2.remaining();
               if (value33 <= 0) {
                  break label72;
               }

               getGpuSampler(value33);
               CommandEncoder commandEncoder3 = RenderSystem.getDevice().createCommandEncoder();
               commandEncoder3.writeToBuffer(gpuBuffer2.slice(0L, value33), buffer2);
               GpuBufferSlice gpuBufferSlice3 = dynamicUniformStorage3.writeUniform(new TargetEspShader.szGtUBFAEr0CJ7Uv(matrix4f4, (float)value31, (float)value32));
               RenderPass renderPass3 = commandEncoder3.createRenderPass(
                  () -> string5, gpuTextureView2, matches8 ? Optional.of(new Vector4f(0.0F)) : Optional.empty(), gpuTextureView3, OptionalDouble.empty()
               );

               try {
                  renderPass3.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline2));
                  renderPass3.setUniform(string19, gpuBufferSlice3);
                  renderPass3.setVertexBuffer(0, gpuBuffer2.slice());
                  DrawState drawState = meshData4.drawState();
                  AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
                  renderPass3.setIndexBuffer(autoStorageIndexBuffer.getBuffer(drawState.indexCount()), autoStorageIndexBuffer.type());
                  renderPass3.drawIndexed(drawState.indexCount(), 1, 0, 0, 0);
               } catch (Throwable throwable7) {
                  if (renderPass3 != null) {
                     try {
                        renderPass3.close();
                     } catch (Throwable throwable8) {
                        throwable7.addSuppressed(throwable8);
                     }
                  }

                  throw throwable7;
               }

               if (renderPass3 != null) {
                  renderPass3.close();
               }
            } catch (Throwable throwable9) {
               if (meshData4 != null) {
                  try {
                     meshData5.close();
                  } catch (Throwable throwable10) {
                     throwable9.addSuppressed(throwable10);
                  }
               }

               throw throwable9;
            }

            if (meshData4 != null) {
               meshData4.close();
            }

            return;
         }

         if (meshData4 != null) {
            meshData4.close();
         }
      }
   }

   private static void updateState5(int value34, int value35) {
      int value36 = Math.max(1, value34 / 2);
      int value37 = Math.max(1, value35 / 2);
      if (textureTarget5 == null || textureTarget5.width != value34 || textureTarget5.height != value35) {
         getGpuSampler3(textureTarget5);
         textureTarget5 = new TextureTarget(string11, value34, value35, GpuFormat.RGBA8_UNORM, null);
      }

      if (textureTarget3 == null || textureTarget3.width != value36 || textureTarget3.height != value37) {
         getGpuSampler3(textureTarget3);
         textureTarget3 = new TextureTarget(string9, value36, value37, GpuFormat.RGBA8_UNORM, null);
      }

      if (textureTarget4 == null || textureTarget4.width != value36 || textureTarget4.height != value37) {
         getGpuSampler3(textureTarget4);
         textureTarget4 = new TextureTarget(string32, value36, value37, GpuFormat.RGBA8_UNORM, null);
      }
   }

   private static float calculateValue2(float amount117, double amount118, Matrix4f matrix4f5, int value38) {
      float amount119 = (float)Math.sqrt(matrix4f5.m01() * matrix4f5.m01() + matrix4f5.m11() * matrix4f5.m11() + matrix4f5.m21() * matrix4f5.m21());
      float amount120 = amount119 * value38 / (2.0F * (float)Math.max(amount118, currentValue65));
      return Mth.clamp(amount117 * amount120, 1.0F, currentValue56);
   }

   private static float calculateValue3(int value39, int value40, int value41, int value42, float amount121) {
      float amount122 = calculateValue(value39, value40, value42 * 4 + value41);
      float amount123 = calculateValue(value39, value40, (value42 + 1) * 4 + value41);
      float amount124 = amount121 * amount121 * (currentValue8 - 2.0F * amount121);
      return amount122 + (amount123 - amount122) * amount124;
   }

   private static void updateState3(GpuBuffer gpuBuffer) {
      if (gpuBuffer != null) {
         list.add(() -> {
            if (!gpuBuffer.isClosed()) {
               gpuBuffer.close();
            }
         });
      }
   }

   private static boolean checkCondition(String text5, Vec3 position2, LivingEntity entity4) {
      if (string15.equalsIgnoreCase(text5)) {
         return true;
      } else if (!string40.equalsIgnoreCase(text5) && minecraft2.level != null) {
         AABB aabb = entity4.getBoundingBox();
         Vec3 position3 = new Vec3(Mth.clamp(position2.x, aabb.minX, aabb.maxX), Mth.clamp(position2.y, aabb.minY, aabb.maxY), Mth.clamp(position2.z, aabb.minZ, aabb.maxZ));
         if (position3.distanceToSqr(position2) < currentValue25) {
            return false;
         } else {
            BlockHitResult blockHitResult = minecraft2.level.clip(new ClipContext(position2, position3, Block.OUTLINE, Fluid.NONE, minecraft2.player));
            return blockHitResult.getType() != Type.MISS;
         }
      } else {
         return false;
      }
   }

   private static RenderPipeline getRenderPipeline2(String text6, CompareOp compareOp) {
      return DimasikRenderPipelinesAccessor.register(
         RenderPipelineLayouts.getRenderPipelineLayouts6()
            .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string27))
            .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string18))
            .getRenderPipelineLayouts2(string17, UniformType.UNIFORM_BUFFER)
            .getRenderPipelineLayouts7(blendFunction2)
            .getRenderPipelineLayouts5(compareOp)
            .getRenderPipelineLayouts9(false)
            .getRenderPipelineLayouts3(false)
            .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, PrimitiveTopology.LINES)
            .getRenderPipelineLayouts15(Identifier.fromNamespaceAndPath(string42, StringConcatSupport.concat95(text6)))
            .getRenderPipeline()
      );
   }

   private record abXuACoQvVDQJPr4(float value) implements DynamicUniform {
      public void write(ByteBuffer buffer3) {
         buffer3.putFloat(this.value).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      }
   }

   private record szGtUBFAEr0CJ7Uv(Matrix4f mvp, float screenWidth, float screenHeight) implements DynamicUniform {
      public void write(ByteBuffer buffer4) {
         this.mvp.get(buffer4);
         buffer4.position(buffer4.position() + 64);
         buffer4.putFloat(1.0F).putFloat(1.0F).putFloat(1.0F).putFloat(1.0F);
         buffer4.putFloat(this.screenWidth).putFloat(this.screenHeight).putFloat(0.0F).putFloat(0.0F);
      }
   }
}
