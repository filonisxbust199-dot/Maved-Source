package ess.ll.client.render.targetfx.internal;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.PolygonMode;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
public final class RenderPipelineLayouts {
   public static final VertexFormat vertexFormat = VertexFormat.builder(0).build();
   public static final Snippet snippet = RenderPipeline.builder(new Snippet[0])
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .buildSnippet();
   private final Builder builder3;
   private final com.mojang.renderpearl.api.pipeline.BindGroupLayout.Builder builder2 = BindGroupLayout.builder();
   private final Set<String> set = new HashSet<>();
   private boolean stateActive3;
   private CompareOp compareOp2 = CompareOp.GREATER_THAN_OR_EQUAL;
   private boolean stateActive = true;
   private boolean stateActive2;
   private float currentValue2;
   private float currentValue;
   private Optional<BlendFunction> optional = Optional.empty();
   private int stateCounter = 15;
   private boolean stateActive4;
   private GpuFormat gpuFormat2 = GpuFormat.RGBA8_UNORM;

   public RenderPipelineLayouts getRenderPipelineLayouts22(GpuFormat gpuFormat) {
      this.gpuFormat2 = gpuFormat;
      this.stateActive4 = true;
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts20(PolygonMode polygonMode) {
      this.builder3.withPolygonMode(polygonMode);
      return this;
   }

   private RenderPipelineLayouts(Snippet... snippetArray) {
      this.builder3 = RenderPipeline.builder(snippetArray);

      for (Snippet snippet2 : snippetArray) {
         snippet2.bindGroupLayouts().ifPresent(items -> items.forEach(bindGroupLayout -> {
            bindGroupLayout.uniforms().forEach(uniformDescription -> this.set.add(uniformDescription.name()));
         }));
         snippet2.depthStencilState().ifPresent(depthStencilState -> {
            this.compareOp2 = depthStencilState.depthTest();
            this.stateActive = depthStencilState.writeDepth();
            this.currentValue2 = depthStencilState.depthBiasScaleFactor();
            this.currentValue = depthStencilState.depthBiasConstant();
         });
         if (snippet2.colorTargetStates() != null && snippet2.activeColorTargetStateCount() > 0 && snippet2.colorTargetStates()[0] != null) {
            this.gpuFormat2 = snippet2.colorTargetStates()[0].format();
            this.optional = snippet2.colorTargetStates()[0].blendFunction();
            this.stateCounter = snippet2.colorTargetStates()[0].writeMask();
         }
      }
   }

   public static RenderPipelineLayouts getRenderPipelineLayouts6(Snippet... snippetArray2) {
      return new RenderPipelineLayouts(snippetArray2);
   }

   public RenderPipelineLayouts getRenderPipelineLayouts5(CompareOp compareOp) {
      this.compareOp2 = compareOp;
      this.stateActive2 = true;
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts16(boolean matches, boolean matches2) {
      this.stateCounter = (matches ? 7 : 0) | (matches2 ? 8 : 0);
      this.stateActive4 = true;
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts18(float amount, float amount2) {
      this.currentValue2 = amount;
      this.currentValue = amount2;
      this.stateActive2 = true;
      return this;
   }

   public RenderPipeline getRenderPipeline() {
      this.updateState();
      return this.builder3.build();
   }

   public RenderPipelineLayouts getRenderPipelineLayouts13(String text, float amount3) {
      this.builder3.withShaderDefine(text, amount3);
      return this;
   }

   private void updateState() {
      if (this.stateActive3) {
         this.builder3.withBindGroupLayout(this.builder2.build());
         this.stateActive3 = false;
      }

      if (this.stateActive2) {
         this.builder3
            .withDepthStencilState(
               this.compareOp2 == CompareOp.ALWAYS_PASS
                  ? Optional.empty()
                  : Optional.of(
                     new DepthStencilState(this.compareOp2, this.stateActive, this.currentValue2, this.currentValue)
                  )
            );
      }

      if (this.stateActive4) {
         this.builder3
            .withColorTargetState(new ColorTargetState(this.optional, this.gpuFormat2, this.stateCounter));
      }
   }

   public RenderPipelineLayouts getRenderPipelineLayouts3(boolean matches3) {
      this.builder3.withCull(matches3);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts(String text2) {
      this.builder3.withLocation(text2);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts10() {
      this.optional = Optional.empty();
      this.stateActive4 = true;
      return this;
   }

   public Snippet getSnippet() {
      this.updateState();
      return this.builder3.buildSnippet();
   }

   public RenderPipelineLayouts getRenderPipelineLayouts2(String text3, UniformType uniformType) {
      if (this.set.add(text3)) {
         this.builder2.withUniform(text3, uniformType);
         this.stateActive3 = true;
      }

      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts19(String text4) {
      this.builder3.withFragmentShader(text4);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts7(BlendFunction blendFunction) {
      this.optional = Optional.of(blendFunction);
      this.stateActive4 = true;
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts21(String text5, int value) {
      this.builder3.withShaderDefine(text5, value);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts11(Identifier identifier) {
      this.builder3.withVertexShader(identifier);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts14(String text6) {
      this.builder3.withShaderDefine(text6);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts9(boolean matches4) {
      this.stateActive = matches4;
      this.stateActive2 = true;
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts17(String text7) {
      this.builder3.withVertexShader(text7);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts8(VertexFormat vertexFormat2, PrimitiveTopology primitiveTopology) {
      if (vertexFormat2.getVertexSize() != 0) {
         this.builder3.withVertexBinding(0, vertexFormat2);
      }

      this.builder3.withPrimitiveTopology(primitiveTopology);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts4(Identifier identifier2) {
      this.builder3.withFragmentShader(identifier2);
      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts12(String text8) {
      if (this.set.add(text8)) {
         this.builder2.withUniform(text8, UniformType.COMBINED_IMAGE_SAMPLER);
         this.stateActive3 = true;
      }

      return this;
   }

   public RenderPipelineLayouts getRenderPipelineLayouts15(Identifier identifier3) {
      this.builder3.withLocation(identifier3);
      return this;
   }
}
