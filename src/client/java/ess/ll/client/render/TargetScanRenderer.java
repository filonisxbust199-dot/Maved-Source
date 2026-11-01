package ess.ll.client.render;

import ess.ll.client.integration.LevelRendererAccess;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass.RenderArea;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TargetScanRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("QuickClient");
    private static final Supplier<String> LABEL = () -> "hex target scan";
    private static final ByteBuffer UNIFORM_DATA = ByteBuffer.allocateDirect(160).order(ByteOrder.nativeOrder());
    private static final GpuBuffer[] UNIFORM_BUFFERS = new GpuBuffer[3];
    private static int bufferIndex;
    private static RenderPipeline pipeline;
    private static boolean pending;
    private static boolean failed;
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f inverseViewProjection = new Matrix4f();
    private static final Matrix4f viewProjection = new Matrix4f();
    private static final Vector4f scratchVector = new Vector4f();
    private static int viewportX;
    private static int viewportY;
    private static int viewportWidth;
    private static int viewportHeight;

    private TargetScanRenderer() {
    }

    private static void setViewport(int var0, int var1, int var2, int var3) {
        viewportX = var0;
        viewportY = var1;
        viewportWidth = Math.max(0, var2);
        viewportHeight = Math.max(0, var3);
    }

    private static void renderPass(float var0, float var1, float var2, float var3, float var4, float var5, float var6) {
        RenderTarget renderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int var8 = renderTarget == null ? 0 : renderTarget.width;
        int var9 = renderTarget == null ? 0 : renderTarget.height;
        if (var8 > 0 && var9 > 0) {
            scratchVector.set(var0, var1 + var4 * 0.5F, var2, 1.0F).mul(viewProjection);
            if (scratchVector.w <= 1.0E-4F) {
                setViewport(0, 0, 0, 0);
            } else {
                float var10 = Math.max(var1 - var3, var5 - var6);
                float var11 = Math.min(var1 + var4 + var3, var5 + var6);
                if (var10 > var11) {
                    setViewport(0, 0, 0, 0);
                } else {
                    float var12 = Float.MAX_VALUE;
                    float var13 = Float.MAX_VALUE;
                    float var14 = -Float.MAX_VALUE;
                    float var15 = -Float.MAX_VALUE;
                    for (int var16 = 0; var16 < 8; var16++) {
                        float var17 = var0 + ((var16 & 1) == 0 ? -var3 : var3);
                        float var18 = var2 + ((var16 & 2) == 0 ? -var3 : var3);
                        float var19 = (var16 & 4) == 0 ? var10 : var11;
                        scratchVector.set(var17, var19, var18, 1.0F).mul(viewProjection);
                        if (scratchVector.w <= 1.0E-4F) {
                            setViewport(0, 0, var8, var9);
                            return;
                        }
                        float var20 = scratchVector.x / scratchVector.w * 0.5F + 0.5F;
                        float var21 = scratchVector.y / scratchVector.w * 0.5F + 0.5F;
                        var12 = Math.min(var12, var20);
                        var14 = Math.max(var14, var20);
                        var13 = Math.min(var13, var21);
                        var15 = Math.max(var15, var21);
                    }
                    int var22 = Mth.clamp(Mth.floor(var12 * var8) - 2, 0, var8);
                    int var23 = Mth.clamp(Mth.ceil(var14 * var8) + 2, 0, var8);
                    int var24 = Mth.clamp(Mth.floor(var13 * var9) - 2, 0, var9);
                    int var25 = Mth.clamp(Mth.ceil(var15 * var9) + 2, 0, var9);
                    setViewport(var22, var24, var23 - var22, var25 - var24);
                }
            }
        } else {
            setViewport(0, 0, 0, 0);
        }
    }

    private static void putColor(int var0) {
        UNIFORM_DATA.putFloat((var0 >> 16 & 0xFF) / 255.0F).putFloat((var0 >> 8 & 0xFF) / 255.0F).putFloat((var0 & 0xFF) / 255.0F).putFloat((var0 >>> 24 & 0xFF) / 255.0F);
    }

    private static RenderPipeline pipeline() {
        if (pipeline == null) {
            pipeline = RenderPipelines.register(RenderPipeline.builder().withLocation(id("pipeline/target_scan")).withVertexShader("core/screenquad").withFragmentShader(id("post/target_scan")).withBindGroupLayout(BindGroupLayout.builder().withUniform("MaskSampler", UniformType.COMBINED_IMAGE_SAMPLER).withUniform("DepthSampler", UniformType.COMBINED_IMAGE_SAMPLER).withUniform("TargetScan", UniformType.UNIFORM_BUFFER).build()).withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY)).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
        }
        return pipeline;
    }

    public static void flush() {
        if (pending) {
            pending = false;
            if (!failed && viewportWidth > 0 && viewportHeight > 0) {
                Minecraft instance = Minecraft.getInstance();
                RenderTarget renderTarget = ((LevelRendererAccess) instance.levelRenderer).hexRawOutlineTarget();
                RenderTarget renderTarget2 = instance.gameRenderer.mainRenderTarget();
                if (renderTarget != null && renderTarget2 != null && renderTarget2.getDepthTextureView() != null) {
                    CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                    bufferIndex = (bufferIndex + 1) % 3;
                    GpuBuffer gpuBuffer = UNIFORM_BUFFERS[bufferIndex];
                    if (gpuBuffer == null) {
                        gpuBuffer = RenderSystem.getDevice().createBuffer(LABEL, 136, 160L);
                        UNIFORM_BUFFERS[bufferIndex] = gpuBuffer;
                    }
                    UNIFORM_DATA.rewind();
                    commandEncoder.writeToBuffer(gpuBuffer.slice(), UNIFORM_DATA);
                    try (RenderPass renderPass = commandEncoder.createRenderPass(LABEL, renderTarget2.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty(), new RenderArea(viewportX, viewportY, viewportWidth, viewportHeight))) {
                        renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline()));
                        RenderSystem.bindDefaultUniforms(renderPass);
                        renderPass.setUniform("TargetScan", gpuBuffer);
                        renderPass.setUniform("MaskSampler", renderTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                        renderPass.setUniform("DepthSampler", renderTarget2.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                        renderPass.draw(3, 1, 0, 0);
                    } catch (Throwable throwable) {
                        failed = true;
                        LOGGER.error("[target scan] проход отключён после сбоя", throwable);
                    }
                }
            }
        }
    }

    public static void cancel() {
        pending = false;
    }

    private static Identifier id(String var0) {
        return Identifier.fromNamespaceAndPath("Hex".toLowerCase(), var0);
    }

    public static void queue(Matrix4f matrix, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, int var11, int var12, float var13, float var14) {
        if (matrix != null && !(var13 <= 0.0F) && !(var5 <= 0.0F)) {
            float var15 = var4 + var6;
            viewProjection.set(projection).mul(matrix);
            inverseViewProjection.set(viewProjection).invert();
            renderPass(var1, var2, var3, var15, var5, var7, Math.max(1.4434999F, var14));
            UNIFORM_DATA.clear();
            inverseViewProjection.get(UNIFORM_DATA);
            UNIFORM_DATA.position(64);
            UNIFORM_DATA.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var15);
            UNIFORM_DATA.putFloat(var7).putFloat(var8).putFloat(var9).putFloat(var14);
            putColor(var11);
            putColor(var12);
            UNIFORM_DATA.putFloat(var5).putFloat(var13).putFloat(var6).putFloat(0.0F);
            double var16 = var10 * (double) 2.6F;
            double var18 = var10 * (double) 3.4F;
            UNIFORM_DATA.putFloat((float) Math.sin(var16)).putFloat((float) Math.cos(var16)).putFloat((float) Math.sin(var18)).putFloat((float) Math.cos(var18));
            UNIFORM_DATA.flip();
            pending = true;
        }
    }

    public static void setProjectionMatrix(Matrix4f matrix) {
        if (matrix != null) {
            projection.set(matrix);
        }
    }
}
