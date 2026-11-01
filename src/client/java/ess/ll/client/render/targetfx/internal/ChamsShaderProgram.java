package ess.ll.client.render.targetfx.internal;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4f;
public final class ChamsShaderProgram {
   private static final String DEAD_CHAMS_SHADER = "dead_chams";
   private static final String PRIVATE_GLOW_SHADER = "private_glow";
   private static final String SAMPLER_NAME = "Sampler0";

   private ChamsShaderProgram() {
   }

   public static void updateState(BufferBuilder bufferBuilder, Matrix4f matrix4f, float amount, int value) {
      float amount2 = amount / 2.0F;
      bufferBuilder.addVertex(matrix4f, -amount2, amount2, 0.0F).setUv(0.0F, 1.0F).setColor(value);
      bufferBuilder.addVertex(matrix4f, amount2, amount2, 0.0F).setUv(1.0F, 1.0F).setColor(value);
      bufferBuilder.addVertex(matrix4f, amount2, -amount2, 0.0F).setUv(1.0F, 0.0F).setColor(value);
      bufferBuilder.addVertex(matrix4f, -amount2, -amount2, 0.0F).setUv(0.0F, 0.0F).setColor(value);
   }

   public static RenderPipeline getRenderPipeline2(String text, CompareOp compareOp) {
      return RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
         .getRenderPipelineLayouts(text)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(DEAD_CHAMS_SHADER))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(PRIVATE_GLOW_SHADER))
         .getRenderPipelineLayouts5(compareOp)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.OVERLAY)
         .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
         .getRenderPipelineLayouts12(SAMPLER_NAME)
         .getRenderPipeline();
   }
}
