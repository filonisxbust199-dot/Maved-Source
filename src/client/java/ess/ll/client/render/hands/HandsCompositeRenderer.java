package ess.ll.client.render.hands;

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
import ess.ll.client.render.targetfx.internal.ClientIdentifiers;
import ess.ll.client.render.targetfx.internal.RenderPipelineLayouts;
import ess.ll.client.module.render.Hands;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemUseAnimation;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
public final class HandsCompositeRenderer {
   public static final HandsCompositeRenderer handsCompositeRenderer = new HandsCompositeRenderer();
   private static final int stateCounter4 = 48;
   private static final int stateCounter11 = 80;
   private static final float currentValue34 = 0.001F;
   private final RenderPipeline renderPipeline23 = getRenderPipeline3(-1);
   private final RenderPipeline[] renderPipelineArray = new RenderPipeline[6];
   private final RenderPipeline renderPipeline15;
   private final RenderPipeline renderPipeline24;
   private final RenderPipeline renderPipeline22;
   private final RenderPipeline renderPipeline10;
   private final RenderPipeline renderPipeline8;
   private final RenderPipeline renderPipeline17;
   private final RenderPipeline renderPipeline13;
   private final RenderPipeline renderPipeline19;
   private final RenderPipeline renderPipeline20;
   private final RenderPipeline renderPipeline12;
   private final RenderPipeline renderPipeline25;
   private final RenderPipeline renderPipeline27;
   private final RenderPipeline renderPipeline11;
   private final RenderPipeline renderPipeline21;
   private final RenderPipeline renderPipeline18;
   private final RenderPipeline renderPipeline26;
   private final RenderPipeline renderPipeline9;
   private final RenderPipeline renderPipeline16;
   private final RenderPipeline renderPipeline14;
   private final RenderPipeline renderPipeline7;
   private final RenderPipeline renderPipeline5;
   private final RenderPipeline renderPipeline6;
   private RenderTarget renderTarget30;
   private RenderTarget renderTarget31;
   private final HandsCompositeRenderer.LoLmBi3LPhwnPlIR loLmBi3LPhwnPlIR7;
   private final HandsCompositeRenderer.LoLmBi3LPhwnPlIR loLmBi3LPhwnPlIR5;
   private final HandsCompositeRenderer.LoLmBi3LPhwnPlIR loLmBi3LPhwnPlIR6;
   private TextureTarget textureTarget3;
   private TextureTarget textureTarget2;
   private GpuBuffer gpuBuffer8;
   private GpuBuffer gpuBuffer13;
   private GpuBuffer gpuBuffer6;
   private GpuBuffer gpuBuffer14;
   private GpuBuffer gpuBuffer11;
   private GpuBuffer gpuBuffer7;
   private GpuBuffer gpuBuffer9;
   private long lastUpdateMillis2;
   private double currentValue17;
   private int stateCounter2;
   private float currentValue44;
   private float currentValue50;
   private boolean stateActive4;
   private float currentValue20;
   private boolean stateActive6;
   private float currentValue29;
   private float currentValue3;
   private boolean stateActive3;
   private int stateCounter3;
   private boolean stateActive2;
   private final HandsCompositeRenderer.J6fV8uaRqaXhTRoo j6fV8uaRqaXhTRoo2;
   private final HandsCompositeRenderer.J6fV8uaRqaXhTRoo j6fV8uaRqaXhTRoo3;
   private static final String string73 = "base";
   private static final String string127 = "hands_composite";
   private static final String string22 = "hands_composite";
   private static final String string83 = "HandsData";
   private static final String string124 = "HandSampler";
   private static final String string129 = "HandDepthSampler";
   private static final String string69 = "HandMaskSampler";
   private static final String string32 = "HandSupportSampler";
   private static final String string112 = "BlurredSceneSampler";
   private static final String string49 = "BloomSampler";
   private static final String string33 = "HANDS_STYLE";
   private static final String string70 = "_mask";
   private static final String string37 = "_item";
   private static final String string87 = "_scene";
   private static final String string150 = "hands_fullscreen";
   private static final String string89 = "Sampler0";
   private static final String string25 = "HANDS_MASK";
   private static final String string5 = "HandsBlur";
   private static final String string4 = "dimasik/hands_nothing_composite";
   private static final String string68 = "hands_composite";
   private static final String string78 = "hands_nothing_composite";
   private static final String string18 = "HandsData";
   private static final String string56 = "HandSampler";
   private static final String string15 = "HandMaskSampler";
   private static final String string51 = "BloomSampler";
   private static final String string42 = "dimasik/hands_mask";
   private static final String string123 = "hands_fullscreen";
   private static final String string24 = "hands_mask";
   private static final String string58 = "HandSampler";
   private static final String string131 = "HandDepthSampler";
   private static final String string113 = "hands_downsample";
   private static final String string66 = "hands_downsample";
   private static final String string97 = "hands_upsample";
   private static final String string6 = "hands_gaussian";
   private static final String string135 = "hands_gaussian";
   private static final String string125 = "hands_support";
   private static final String string40 = "hands_downsample";
   private static final String string142 = "hands_upsample";
   private static final String string132 = "hands_gaussian";
   private static final String string9 = "dimasik/hands_item_color";
   private static final String string90 = "hands_fullscreen";
   private static final String string35 = "hands_item_color";
   private static final String string133 = "HandSampler";
   private static final String string29 = "HandMaskSampler";
   private static final String string128 = "dimasik/hands_outer_glow";
   private static final String string60 = "hands_fullscreen";
   private static final String string85 = "hands_outer_glow";
   private static final String string17 = "HandsGlow";
   private static final String string81 = "BloomSampler";
   private static final String string61 = "HandMaskSampler";
   private static final String string107 = "dimasik/hands_outline";
   private static final String string119 = "hands_fullscreen";
   private static final String string104 = "hands_outline";
   private static final String string44 = "HandSampler";
   private static final String string88 = "HandsGlow";
   private static final String string146 = "BloomSampler";
   private static final String string59 = "HandMaskSampler";
   private static final String string71 = "dimasik/hands_glow_outline";
   private static final String string84 = "hands_fullscreen";
   private static final String string80 = "hands_glow_outline";
   private static final String string65 = "HandSampler";
   private static final String string114 = "HandsGlow";
   private static final String string63 = "BloomSampler";
   private static final String string95 = "HandMaskSampler";
   private static final String string144 = "dimasik/hands_trail_displace";
   private static final String string126 = "hands_fullscreen";
   private static final String string57 = "hands_trail_displace";
   private static final String string52 = "TrailDisplace";
   private static final String string12 = "TrailSampler";
   private static final String string147 = "dimasik/hands_trail_energy_displace";
   private static final String string152 = "hands_fullscreen";
   private static final String string99 = "hands_trail_energy_displace";
   private static final String string108 = "TrailDisplace";
   private static final String string38 = "TrailSampler";
   private static final String string102 = "dimasik/hands_trail_flame";
   private static final String string28 = "hands_fullscreen";
   private static final String string74 = "hands_trail_flame";
   private static final String string101 = "TrailFlame";
   private static final String string72 = "BloomSampler";
   private static final String string121 = "dimasik/hands_trail_energy";
   private static final String string110 = "hands_fullscreen";
   private static final String string93 = "hands_trail_energy";
   private static final String string82 = "TrailEnergy";
   private static final String string134 = "BloomSampler";
   private static final String string100 = "dimasik/hands_trail_ribbon_displace";
   private static final String string138 = "hands_fullscreen";
   private static final String string149 = "hands_trail_ribbon_displace";
   private static final String string106 = "TrailDisplace";
   private static final String string20 = "TrailSampler";
   private static final String string47 = "dimasik/hands_trail_ribbon";
   private static final String string14 = "hands_fullscreen";
   private static final String string46 = "hands_trail_ribbon";
   private static final String string3 = "TrailRibbon";
   private static final String string54 = "BloomSampler";
   private static final String string77 = "dimasik/hands_trail_blit";
   private static final String string75 = "hands_fullscreen";
   private static final String string43 = "hands_trail_blit";
   private static final String string86 = "TrailSampler";
   private static final String string92 = "hands-bloom";
   private static final String string30 = "hands-item-bloom";
   private static final String string = "hands-mirror";
   private static final float currentValue6 = 0.001F;
   private static final float currentValue28 = 0.001F;
   private static final float currentValue2 = 0.001F;
   private static final float currentValue11 = 0.001F;
   private static final float currentValue4 = 0.6F;
   private static final float currentValue39 = 3.8F;
   private static final float currentValue33 = 0.25F;
   private static final String string79 = "hands-support";
   private static final float currentValue37 = 5.0F;
   private static final float currentValue10 = 3.0F;
   private static final float currentValue9 = 1.5F;
   private static final float currentValue8 = 2.5F;
   private static final float currentValue22 = 0.75F;
   private static final double currentValue53 = 0.5;
   private static final String string45 = "handsData";
   private static final float currentValue24 = 0.001F;
   private static final String string64 = "HandSampler";
   private static final String string19 = "HandMaskSampler";
   private static final String string120 = "BloomSampler";
   private static final String string117 = "HandSupportSampler";
   private static final String string27 = "HandDepthSampler";
   private static final String string115 = "HandMaskSampler";
   private static final String string26 = "BlurredSceneSampler";
   private static final String string139 = "HandsData";
   private static final String string21 = "HandSampler";
   private static final String string94 = "HandDepthSampler";
   private static final float currentValue45 = 0.75F;
   private static final String string53 = "HandSampler";
   private static final String string141 = "HandMaskSampler";
   private static final String string137 = "Sampler0";
   private static final String string34 = "HandsBlur";
   private static final String string98 = "handsOutlineData";
   private static final String string39 = "handsGlowData";
   private static final String string148 = "BloomSampler";
   private static final String string136 = "HandMaskSampler";
   private static final String string145 = "HandSampler";
   private static final String string96 = "HandsGlow";
   private static final float currentValue55 = 0.1F;
   private static final float currentValue18 = 1.0E9F;
   private static final float currentValue26 = 0.016666668F;
   private static final long lastUpdateMillis = 100000000000L;
   private static final float currentValue54 = 1.0E9F;
   private static final float currentValue47 = (float) (Math.PI * 8.0 / 5.0);
   private static final float currentValue31 = 0.035F;
   private static final float currentValue19 = 0.018F;
   private static final float currentValue15 = 180.0F;
   private static final float currentValue13 = 360.0F;
   private static final float currentValue41 = -180.0F;
   private static final float currentValue43 = 360.0F;
   private static final float currentValue32 = -0.25F;
   private static final float currentValue36 = 0.25F;
   private static final float currentValue7 = -0.25F;
   private static final float currentValue14 = 0.25F;
   private static final float currentValue30 = 0.045F;
   private static final float currentValue = 0.15F;
   private static final float currentValue27 = 0.048F;
   private static final float currentValue12 = 0.4F;
   private static final float currentValue52 = 0.012F;
   private static final float currentValue23 = 0.45F;
   private static final float currentValue46 = 0.32F;
   private static final String string8 = "handsTrailDisplaceData";
   private static final String string2 = "TrailSampler";
   private static final String string7 = "TrailDisplace";
   private static final String string76 = "TrailSampler";
   private static final String string109 = "handsTrailFlameData";
   private static final String string62 = "BloomSampler";
   private static final String string41 = "TrailFlame";
   private static final String string31 = "handsTrailEnergyData";
   private static final String string130 = "BloomSampler";
   private static final String string91 = "TrailEnergy";
   private static final float currentValue48 = 0.001F;
   private static final String string105 = "handsTrailRibbonData";
   private static final String string103 = "BloomSampler";
   private static final String string55 = "TrailRibbon";
   private static final float currentValue51 = 0.15F;
   private static final float currentValue21 = 0.15F;
   private static final float currentValue49 = 16.0F;
   private static final String string111 = "hands-trail-a";
   private static final String string16 = "hands-trail-b";
   private static final String string50 = "hands-mask";
   private static final float currentValue40 = 255.0F;
   private static final float currentValue38 = 255.0F;
   private static final float currentValue42 = 255.0F;
   private static final float currentValue16 = 255.0F;
   private static final String string36 = "handsTrailRibbon";
   private static final String string151 = "handsTrailEnergy";
   private static final String string11 = "handsTrailFlame";
   private static final String string140 = "handsTrailBlit";
   private static final String string48 = "handsTrailDisplace";
   private static final String string143 = "handsGlowOutline";
   private static final String string122 = "handsOutline";
   private static final String string23 = "handsOuterGlow";
   private static final String string67 = "handsBlur";
   private static final String string116 = "handsItemColor";
   private static final String string10 = "handsMask";
   private static final String string118 = "handsComposite";

   private void updateState15(CommandEncoder commandEncoder) {
      if (this.textureTarget3 != null && this.textureTarget2 != null) {
         commandEncoder.clearColorTexture(this.textureTarget3.getColorTexture(), new Vector4f(0.0F));
         commandEncoder.clearColorTexture(this.textureTarget2.getColorTexture(), new Vector4f(0.0F));
      }
   }

   private void updateState4(int value, int value2, CommandEncoder commandEncoder2) {
      if (this.textureTarget3 == null) {
         this.textureTarget3 = new TextureTarget(string111, value, value2, GpuFormat.RGBA8_UNORM, null);
         this.textureTarget2 = new TextureTarget(string16, value, value2, GpuFormat.RGBA8_UNORM, null);
         commandEncoder2.clearColorTexture(this.textureTarget3.getColorTexture(), new Vector4f(0.0F));
         commandEncoder2.clearColorTexture(this.textureTarget2.getColorTexture(), new Vector4f(0.0F));
      } else if (this.textureTarget3.width != value || this.textureTarget3.height != value2) {
         this.textureTarget3.resize(value, value2);
         this.textureTarget2.resize(value, value2);
         commandEncoder2.clearColorTexture(this.textureTarget3.getColorTexture(), new Vector4f(0.0F));
         commandEncoder2.clearColorTexture(this.textureTarget2.getColorTexture(), new Vector4f(0.0F));
      }
   }

   private void updateState13(RenderTarget renderTarget, RenderTarget renderTarget2, RenderPipeline renderPipeline, GpuBuffer gpuBuffer, CommandEncoder commandEncoder3) {
      RenderPass renderPass = commandEncoder3.createRenderPass(() -> string67, renderTarget2.getColorTextureView(), Optional.empty());

      try {
         renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
         renderPass.setUniform(string137, renderTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         if (gpuBuffer != null) {
            renderPass.setUniform(string34, gpuBuffer);
         }

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
   }

   private void updateState9(CommandEncoder commandEncoder4, RenderTarget renderTarget3, Hands hands, float amount, float amount2) {
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         Std140Builder std140Builder = Std140Builder.onStack(memoryStack, 64)
            .putVec4(hands.calculateValue26(), hands.checkCondition18() ? 1.0F : 0.0F, 0.0F, amount)
            .putVec4(amount2, hands.calculateValue46(), hands.calculateValue17(), 0.0F);
         getStd140Builder(std140Builder, hands.calculateValue51());
         getStd140Builder(std140Builder, hands.calculateValue22());
         ByteBuffer buffer = std140Builder.get();
         this.gpuBuffer7 = this.getGpuBuffer(this.gpuBuffer7, 64, string31);
         commandEncoder4.writeToBuffer(this.gpuBuffer7.slice(), buffer);
         RenderPass renderPass2 = commandEncoder4.createRenderPass(() -> string151, this.textureTarget2.getColorTextureView(), Optional.empty());

         try {
            renderPass2.setPipeline(RenderSystem.getCompiledPipeline(this.renderPipeline14));
            renderPass2.setUniform(string130, renderTarget3.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass2.setUniform(string91, this.gpuBuffer7);
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

   private RenderTarget getRenderTarget5(
      RenderTarget renderTarget4, HandsCompositeRenderer.LoLmBi3LPhwnPlIR loLmBi3LPhwnPlIR, int value3, float amount3, boolean matches, RenderTarget renderTarget5, CommandEncoder commandEncoder5
   ) {
      RenderTarget renderTarget6 = renderTarget4;

      for (int index = 0; index < value3; index++) {
         int value4 = Math.max(2, (renderTarget6.width + 1) / 2);
         int value5 = Math.max(2, (renderTarget6.height + 1) / 2);
         if (loLmBi3LPhwnPlIR.list.size() <= index) {
            loLmBi3LPhwnPlIR.list.add(null);
         }

         RenderTarget renderTarget7 = this.getRenderTarget2(
            loLmBi3LPhwnPlIR.list.get(index), loLmBi3LPhwnPlIR.string13 + "-" + index, value4, value5, loLmBi3LPhwnPlIR.gpuFormat4
         );
         loLmBi3LPhwnPlIR.list.set(index, renderTarget7);
         if (renderTarget5 != null && index == 0) {
            this.updateState20(renderTarget5, renderTarget7, commandEncoder5);
         } else {
            this.updateState13(
               renderTarget6,
               renderTarget7,
               loLmBi3LPhwnPlIR.stateActive5
                  ? this.renderPipeline10
                  : (loLmBi3LPhwnPlIR == this.loLmBi3LPhwnPlIR5 ? this.renderPipeline20 : this.renderPipeline22),
               null,
               commandEncoder5
            );
         }

         renderTarget6 = renderTarget7;
      }

      if (loLmBi3LPhwnPlIR != this.loLmBi3LPhwnPlIR6 && value3 == 1 && amount3 <= currentValue45) {
         return renderTarget6;
      } else {
         loLmBi3LPhwnPlIR.renderTarget29 = this.getRenderTarget2(
            loLmBi3LPhwnPlIR.renderTarget29, loLmBi3LPhwnPlIR.string13 + "-scratch", renderTarget6.width, renderTarget6.height, loLmBi3LPhwnPlIR.gpuFormat4
         );
         this.updateState7(loLmBi3LPhwnPlIR, renderTarget6.width, renderTarget6.height, amount3, commandEncoder5);
         RenderPipeline renderPipeline2 = loLmBi3LPhwnPlIR.stateActive5
            ? this.renderPipeline13
            : (loLmBi3LPhwnPlIR == this.loLmBi3LPhwnPlIR5 ? this.renderPipeline25 : this.renderPipeline17);
         this.updateState13(renderTarget6, loLmBi3LPhwnPlIR.renderTarget29, renderPipeline2, loLmBi3LPhwnPlIR.gpuBuffer12, commandEncoder5);
         this.updateState13(loLmBi3LPhwnPlIR.renderTarget29, renderTarget6, renderPipeline2, loLmBi3LPhwnPlIR.gpuBuffer10, commandEncoder5);
         if (matches && value3 > 1) {
            RenderTarget renderTarget8 = loLmBi3LPhwnPlIR.list.getFirst();
            this.updateState13(
               renderTarget6, renderTarget8, loLmBi3LPhwnPlIR == this.loLmBi3LPhwnPlIR5 ? this.renderPipeline12 : this.renderPipeline8, null, commandEncoder5
            );
            return renderTarget8;
         } else {
            return renderTarget6;
         }
      }
   }

   private static RenderPipeline getRenderPipeline2(String text, GpuFormat gpuFormat, boolean matches2) {
      boolean matches3 = gpuFormat == GpuFormat.R8_UNORM;
      RenderPipelineLayouts renderPipelineLayouts = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(
            ("dimasik/" + text + (matches3 ? string70 : (gpuFormat == GpuFormat.RGBA16_FLOAT ? string37 : string87)))
         )
         .getRenderPipelineLayouts22(gpuFormat)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string150))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(text))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts10()
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts12(string89);
      if (matches3) {
         renderPipelineLayouts.getRenderPipelineLayouts14(string25);
      }

      if (matches2) {
         renderPipelineLayouts.getRenderPipelineLayouts2(string5, UniformType.UNIFORM_BUFFER);
      }

      return renderPipelineLayouts.getRenderPipeline();
   }

   public void updateState() {
      if (this.renderTarget30 != null) {
         this.renderTarget30.destroyBuffers();
      }

      if (this.renderTarget31 != null) {
         this.renderTarget31.destroyBuffers();
      }

      this.renderTarget30 = null;
      this.renderTarget31 = null;
      this.updateState12(this.loLmBi3LPhwnPlIR7);
      this.updateState12(this.loLmBi3LPhwnPlIR5);
      this.updateState12(this.loLmBi3LPhwnPlIR6);
      if (this.textureTarget3 != null) {
         this.textureTarget3.destroyBuffers();
      }

      if (this.textureTarget2 != null) {
         this.textureTarget2.destroyBuffers();
      }

      this.textureTarget3 = null;
      this.textureTarget2 = null;
      this.updateState5();
      this.stateCounter3 = -1;
      this.stateActive2 = false;
      this.j6fV8uaRqaXhTRoo2.updateState10();
      this.j6fV8uaRqaXhTRoo3.updateState10();
      this.updateState11(this.gpuBuffer8);
      this.updateState11(this.gpuBuffer13);
      this.updateState11(this.gpuBuffer6);
      this.updateState11(this.gpuBuffer14);
      this.updateState11(this.gpuBuffer11);
      this.updateState11(this.gpuBuffer7);
      this.updateState11(this.gpuBuffer9);
      this.gpuBuffer8 = null;
      this.gpuBuffer13 = null;
      this.gpuBuffer6 = null;
      this.gpuBuffer14 = null;
      this.gpuBuffer11 = null;
      this.gpuBuffer7 = null;
      this.gpuBuffer9 = null;
   }

   private static Std140Builder getStd140Builder(Std140Builder std140Builder2, int value6) {
      return std140Builder2.putVec4(
         (value6 >>> 16 & 0xFF) / currentValue40,
         (value6 >>> 8 & 0xFF) / currentValue38,
         (value6 & 0xFF) / currentValue42,
         (value6 >>> 24 & 0xFF) / currentValue16
      );
   }

   private RenderTarget getRenderTarget(
      RenderTarget renderTarget9, HandsCompositeRenderer.LoLmBi3LPhwnPlIR loLmBi3LPhwnPlIR2, int value7, float amount4, boolean matches4, CommandEncoder commandEncoder6
   ) {
      return this.getRenderTarget5(renderTarget9, loLmBi3LPhwnPlIR2, value7, amount4, matches4, null, commandEncoder6);
   }

   private static RenderPipeline getRenderPipeline4(String text2, boolean matches5, boolean matches6) {
      return getRenderPipeline2(text2, matches5 ? GpuFormat.R8_UNORM : GpuFormat.RGBA8_UNORM, matches6);
   }

   private void updateState18(int value8, int value9) {
      if (this.renderTarget30 == null) {
         this.renderTarget30 = new TextureTarget(string50, value8, value9, GpuFormat.R8_UNORM, null);
      } else if (this.renderTarget30.width != value8 || this.renderTarget30.height != value9) {
         this.renderTarget30.resize(value8, value9);
         this.j6fV8uaRqaXhTRoo2.updateState10();
         this.j6fV8uaRqaXhTRoo3.updateState10();
      }
   }

   private void updateState8(Hands hands2, Minecraft minecraft, float amount5) {
      float amount6 = hands2.calculateValue33();
      float amount7 = 1.0F;
      boolean matches7 = false;
      if (minecraft.player != null && minecraft.player.isUsingItem()) {
         ItemUseAnimation itemUseAnimation = minecraft.player.getUseItem().getUseAnimation();
         matches7 = itemUseAnimation == ItemUseAnimation.EAT || itemUseAnimation == ItemUseAnimation.DRINK;
      }

      if (matches7) {
         amount6 = hands2.checkCondition8() ? hands2.calculateValue38() : currentValue51;
         amount7 = hands2.checkCondition8() ? hands2.calculateValue35() : 0.0F;
      } else if (minecraft.player != null && minecraft.player.isSwinging()) {
         amount6 = hands2.checkCondition12() ? hands2.calculateValue16() : currentValue21;
         amount7 = hands2.checkCondition12() ? hands2.calculateValue4() : 0.0F;
      }

      float amount8 = this.stateActive4 ? (float)(-Math.expm1(-amount5 * currentValue49)) : 1.0F;
      this.currentValue44 = this.currentValue44 + (amount6 - this.currentValue44) * amount8;
      this.currentValue50 = this.currentValue50 + (amount7 - this.currentValue50) * amount8;
      this.stateActive4 = true;
      this.currentValue17 = this.currentValue17 + amount5 * hands2.calculateValue();
      this.stateCounter2 = this.stateCounter2 + 1 & 4095;
   }

   private void updateState17(RenderTarget renderTarget10, CommandEncoder commandEncoder7) {
      RenderPass renderPass3 = commandEncoder7.createRenderPass(() -> string10, this.renderTarget30.getColorTextureView(), Optional.empty());

      try {
         renderPass3.setPipeline(RenderSystem.getCompiledPipeline(this.renderPipeline24));
         renderPass3.setUniform(string21, renderTarget10.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         renderPass3.setUniform(string94, renderTarget10.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         renderPass3.draw(3, 1, 0, 0);
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
   }

   private RenderTarget getRenderTarget2(RenderTarget renderTarget11, String text3, int value10, int value11, GpuFormat gpuFormat2) {
      if (renderTarget11 == null) {
         return (RenderTarget)(gpuFormat2 == GpuFormat.R8_UNORM
            ? new TextureTarget(text3, value10, value11, GpuFormat.R8_UNORM, null)
            : new TextureTarget(text3, value10, value11, gpuFormat2, null));
      } else {
         if (renderTarget11.width != value10 || renderTarget11.height != value11) {
            renderTarget11.resize(value10, value11);
         }

         return renderTarget11;
      }
   }

   private void updateState14(CommandEncoder commandEncoder8, RenderTarget renderTarget12, Hands hands3, float amount9) {
      MemoryStack memoryStack2 = MemoryStack.stackPush();

      try {
         Std140Builder std140Builder3 = Std140Builder.onStack(memoryStack2, 48).putVec4(hands3.calculateValue26(), hands3.checkCondition18() ? 1.0F : 0.0F, 0.0F, amount9);
         getStd140Builder(std140Builder3, hands3.calculateValue51());
         getStd140Builder(std140Builder3, hands3.calculateValue22());
         ByteBuffer buffer2 = std140Builder3.get();
         this.gpuBuffer11 = this.getGpuBuffer(this.gpuBuffer11, 48, string109);
         commandEncoder8.writeToBuffer(this.gpuBuffer11.slice(), buffer2);
         RenderPass renderPass4 = commandEncoder8.createRenderPass(() -> string11, this.textureTarget2.getColorTextureView(), Optional.empty());

         try {
            renderPass4.setPipeline(RenderSystem.getCompiledPipeline(this.renderPipeline16));
            renderPass4.setUniform(string62, renderTarget12.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass4.setUniform(string41, this.gpuBuffer11);
            renderPass4.draw(3, 1, 0, 0);
         } catch (Throwable throwable9) {
            if (renderPass4 != null) {
               try {
                  renderPass4.close();
               } catch (Throwable throwable10) {
                  throwable9.addSuppressed(throwable10);
               }
            }

            throw throwable9;
         }

         if (renderPass4 != null) {
            renderPass4.close();
         }
      } catch (Throwable throwable11) {
         if (memoryStack2 != null) {
            try {
               memoryStack2.close();
            } catch (Throwable throwable12) {
               throwable11.addSuppressed(throwable12);
            }
         }

         throw throwable11;
      }

      if (memoryStack2 != null) {
         memoryStack2.close();
      }
   }

   private void updateState6(RenderTarget renderTarget13, RenderTarget renderTarget14, Hands hands4, CommandEncoder commandEncoder9) {
      this.updateState4(renderTarget13.width, renderTarget13.height, commandEncoder9);
      boolean matches8 = hands4.checkCondition10();
      boolean matches9 = hands4.checkCondition14();
      if (this.stateCounter3 != hands4.calculateValue39()) {
         this.updateState15(commandEncoder9);
         this.stateCounter3 = hands4.calculateValue39();
         this.updateState5();
      }

      long nowNanos = System.nanoTime();
      float amount10 = this.lastUpdateMillis2 > 0L
         ? Math.min(currentValue55, (float)(nowNanos - this.lastUpdateMillis2) / currentValue18)
         : currentValue26;
      this.lastUpdateMillis2 = nowNanos;
      float amount11 = amount10;
      float amount12 = (float)(nowNanos % lastUpdateMillis) / currentValue54;
      Minecraft minecraft2 = Minecraft.getInstance();
      if (matches9) {
         this.updateState8(hands4, minecraft2, amount10);
         amount12 = (float)this.currentValue17;
      }

      float amount13 = currentValue47;
      float amount14 = hands4.calculateValue30();
      float amount15 = matches9 ? hands4.calculateValue14() : (matches8 ? currentValue31 * amount14 : hands4.calculateValue3());
      float amount16 = matches9 ? 0.0F : (matches8 ? currentValue19 * amount14 : hands4.calculateValue24());
      float amount17 = matches9 ? hands4.calculateValue34() : (matches8 ? hands4.calculateValue41() : hands4.calculateValue25());
      float amount18 = amount15 * amount10;
      float amount19 = ((float)Math.sin(amount12 * amount13) - (float)Math.sin((amount12 - amount10) * amount13)) * amount16;
      float amount20 = 0.0F;
      float amount21 = 0.0F;
      Camera camera = minecraft2.gameRenderer != null ? minecraft2.gameRenderer.mainCamera() : null;
      if (camera != null) {
         float amount22 = camera.yRot();
         float amount23 = camera.xRot();
         if (this.stateActive3) {
            float amount24 = amount22 - this.currentValue29;

            while (amount24 > currentValue15) {
               amount24 -= currentValue13;
            }

            while (amount24 < currentValue41) {
               amount24 += currentValue43;
            }

            float amount25 = amount23 - this.currentValue3;
            float amount26 = Math.max(1.0F, (float)((Integer)minecraft2.options.fov().get()).intValue());
            amount20 = Math.clamp(-(amount24 / amount26) * amount17, currentValue32, currentValue36);
            amount21 = Math.clamp(amount25 / amount26 * amount17, currentValue7, currentValue14);
         }

         this.currentValue29 = amount22;
         this.currentValue3 = amount23;
         this.stateActive3 = true;
      }

      boolean matches10 = minecraft2.player != null && minecraft2.player.isSwinging();
      boolean matches11 = hands4.checkCondition11() && hands4.checkCondition17() && matches10 && !this.stateActive6;
      this.stateActive6 = matches10;
      if (matches11) {
         this.currentValue20 = 1.0F;
      }

      float amount27 = (
            matches9
               ? currentValue30 / Math.max(currentValue, this.currentValue44)
               : (matches8 ? currentValue27 / Math.max(currentValue12, hands4.calculateValue45()) : hands4.calculateValue13())
         )
         + this.currentValue20 * hands4.calculateValue9() * currentValue52;
      this.currentValue20 = Math.max(0.0F, this.currentValue20 - amount10 / currentValue23);
      float amount28 = matches9 ? hands4.calculateValue11() : (matches8 ? currentValue46 * amount14 : hands4.calculateValue37());
      float amount29 = !matches9 && !matches8 ? hands4.calculateValue20() : 0.0F;
      MemoryStack memoryStack3 = MemoryStack.stackPush();

      try {
         ByteBuffer buffer3 = Std140Builder.onStack(memoryStack3, 48)
            .putVec2(amount19 + amount20, amount18 + amount21)
            .putVec2(this.textureTarget3.width, this.textureTarget3.height)
            .putVec4(amount27, amount12, amount11, amount28)
            .putVec4(matches9 ? hands4.calculateValue() : amount29, matches9 ? this.stateCounter2 : 0.0F, 0.0F, 0.0F)
            .get();
         this.gpuBuffer14 = this.getGpuBuffer(this.gpuBuffer14, 48, string8);
         commandEncoder9.writeToBuffer(this.gpuBuffer14.slice(), buffer3);
         RenderPass renderPass5 = commandEncoder9.createRenderPass(
            () -> string48, this.textureTarget2.getColorTextureView(), Optional.of(new Vector4f(0.0F))
         );

         try {
            renderPass5.setPipeline(RenderSystem.getCompiledPipeline(matches9 ? this.renderPipeline7 : (matches8 ? this.renderPipeline9 : this.renderPipeline26)));
            renderPass5.setUniform(
               string2, this.textureTarget3.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            );
            renderPass5.setUniform(string7, this.gpuBuffer14);
            renderPass5.draw(3, 1, 0, 0);
         } catch (Throwable throwable13) {
            if (renderPass5 != null) {
               try {
                  renderPass5.close();
               } catch (Throwable throwable14) {
                  throwable13.addSuppressed(throwable14);
               }
            }

            throw throwable13;
         }

         if (renderPass5 != null) {
            renderPass5.close();
         }
      } catch (Throwable throwable15) {
         if (memoryStack3 != null) {
            try {
               memoryStack3.close();
            } catch (Throwable throwable16) {
               throwable15.addSuppressed(throwable16);
            }
         }

         throw throwable15;
      }

      if (memoryStack3 != null) {
         memoryStack3.close();
      }

      if (matches9) {
         this.updateState3(commandEncoder9, renderTarget13, hands4, amount10, amount12);
      } else if (matches8) {
         this.updateState9(commandEncoder9, renderTarget13, hands4, amount10, amount12);
      } else {
         this.updateState14(commandEncoder9, renderTarget13, hands4, amount10);
      }

      RenderPass renderPass6 = commandEncoder9.createRenderPass(() -> string140, renderTarget14.getColorTextureView(), Optional.empty());

      try {
         renderPass6.setPipeline(RenderSystem.getCompiledPipeline(this.renderPipeline6));
         renderPass6.setUniform(
            string76, this.textureTarget2.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
         );
         renderPass6.draw(3, 1, 0, 0);
      } catch (Throwable throwable17) {
         if (renderPass6 != null) {
            try {
               renderPass6.close();
            } catch (Throwable throwable18) {
               throwable17.addSuppressed(throwable18);
            }
         }

         throw throwable17;
      }

      if (renderPass6 != null) {
         renderPass6.close();
      }

      TextureTarget textureTarget = this.textureTarget3;
      this.textureTarget3 = this.textureTarget2;
      this.textureTarget2 = textureTarget;
   }

   private void updateState12(HandsCompositeRenderer.LoLmBi3LPhwnPlIR loLmBi3LPhwnPlIR3) {
      for (RenderTarget renderTarget15 : loLmBi3LPhwnPlIR3.list) {
         renderTarget15.destroyBuffers();
      }

      loLmBi3LPhwnPlIR3.list.clear();
      if (loLmBi3LPhwnPlIR3.renderTarget29 != null) {
         loLmBi3LPhwnPlIR3.renderTarget29.destroyBuffers();
      }

      loLmBi3LPhwnPlIR3.renderTarget29 = null;
      this.updateState11(loLmBi3LPhwnPlIR3.gpuBuffer12);
      this.updateState11(loLmBi3LPhwnPlIR3.gpuBuffer10);
      loLmBi3LPhwnPlIR3.gpuBuffer12 = null;
      loLmBi3LPhwnPlIR3.gpuBuffer10 = null;
      loLmBi3LPhwnPlIR3.currentValue5 = Float.NaN;
   }

   private void updateState5() {
      this.lastUpdateMillis2 = 0L;
      this.currentValue20 = 0.0F;
      this.stateActive6 = false;
      this.stateActive3 = false;
      this.currentValue17 = 0.0;
      this.stateCounter2 = 0;
      this.stateActive4 = false;
   }

   private void updateState11(GpuBuffer gpuBuffer2) {
      if (gpuBuffer2 != null && !gpuBuffer2.isClosed()) {
         gpuBuffer2.close();
      }
   }

   public void updateState2(RenderTarget renderTarget16, RenderTarget renderTarget17, Hands hands5) {
      CommandEncoder commandEncoder10 = RenderSystem.getDevice().createCommandEncoder();
      if (this.stateActive2 != hands5.checkCondition18()) {
         this.stateActive2 = hands5.checkCondition18();
         this.j6fV8uaRqaXhTRoo2.updateState10();
         this.j6fV8uaRqaXhTRoo3.updateState10();
         this.updateState15(commandEncoder10);
         this.updateState12(this.stateActive2 ? this.loLmBi3LPhwnPlIR7 : this.loLmBi3LPhwnPlIR5);
      }

      boolean matches12 = hands5.checkCondition4();
      boolean matches13 = hands5.checkCondition7();
      boolean matches14 = hands5.checkCondition9() && hands5.calculateValue26() > currentValue6;
      boolean matches15 = hands5.checkCondition9() && (matches14 || hands5.calculateValue48() > currentValue28 || matches13);
      int value12 = hands5.calculateValue21();
      boolean matches16 = hands5.checkCondition16() && hands5.calculateValue49() > currentValue2 && value12 >>> 24 != 0;
      boolean matches17 = matches14 && matches16 && value12 >>> 24 == 255;
      RenderTarget renderTarget18 = renderTarget16;
      if (matches12 && hands5.calculateValue2() > currentValue11) {
         renderTarget18 = this.getRenderTarget(
            renderTarget17,
            this.loLmBi3LPhwnPlIR6,
            2,
            (currentValue4 + hands5.calculateValue43() * currentValue39) * currentValue33,
            false,
            commandEncoder10
         );
      }

      this.updateState18(renderTarget16.width, renderTarget16.height);
      this.updateState17(renderTarget16, commandEncoder10);
      boolean matches18 = hands5.checkCondition19();
      if (!matches18) {
         this.renderTarget31 = this.getRenderTarget3(
            this.renderTarget31, string79, Math.max(2, (renderTarget16.width + 1) / 2), Math.max(2, (renderTarget16.height + 1) / 2), true
         );
         this.updateState13(this.renderTarget30, this.renderTarget31, this.renderPipeline19, null, commandEncoder10);
      }

      if (!matches13 && this.stateCounter3 != -1) {
         this.updateState15(commandEncoder10);
         this.stateCounter3 = -1;
         this.updateState5();
      }

      RenderTarget renderTarget19 = null;
      if (matches15) {
         float amount30 = Math.clamp(hands5.calculateValue32(), 1.0F, currentValue37);
         if (hands5.checkCondition10()) {
            amount30 = Math.max(currentValue10, amount30);
         }

         int value13 = amount30 <= 1.0F ? 1 : 2;
         float amount31 = (currentValue9 + (amount30 - 1.0F) * currentValue8) / (1 << value13);
         renderTarget19 = this.getRenderTarget4(renderTarget16, hands5, value13, amount31, matches13, commandEncoder10);
         if (matches13) {
            this.updateState6(renderTarget19, renderTarget17, hands5, commandEncoder10);
         }

         if (matches14 && !matches17) {
            this.updateState16(renderTarget19, this.renderTarget30, renderTarget16, renderTarget17, hands5, true, false, this.renderPipeline11, commandEncoder10);
         }
      }

      if (matches16) {
         if (renderTarget19 == null) {
            renderTarget19 = this.getRenderTarget4(renderTarget16, hands5, 1, currentValue22, false, commandEncoder10);
         }

         this.updateState16(
            renderTarget19, this.renderTarget30, renderTarget16, renderTarget17, hands5, matches17, true, matches17 ? this.renderPipeline18 : this.renderPipeline21, commandEncoder10
         );
      }

      MemoryStack memoryStack4 = MemoryStack.stackPush();

      try {
         float amount32 = ((Integer)Minecraft.getInstance().options.fov().get()).intValue();
         float angleRadians = (float)Math.tan(Math.toRadians(amount32) * currentValue53);
         float amount33 = (float)renderTarget17.width / Math.max(1, renderTarget17.height);
         Std140Builder std140Builder4 = Std140Builder.onStack(memoryStack4, 240)
            .putVec4(hands5.calculateValue44(), hands5.calculateValue42(), hands5.calculateValue12(), hands5.calculateValue36())
            .putVec4(hands5.calculateValue8(), hands5.calculateValue10(), hands5.calculateValue47(), hands5.calculateValue50())
            .putVec4(
               hands5.checkCondition5() ? 1.0F : 0.0F,
               hands5.checkCondition() ? 1.0F : 0.0F,
               hands5.checkCondition15() ? 1.0F : 0.0F,
               hands5.checkCondition20() ? 1.0F : 0.0F
            )
            .putVec4(hands5.calculateValue28(), hands5.calculateValue6(), hands5.calculateValue15(), hands5.calculateValue18())
            .putVec4(hands5.calculateValue2(), hands5.calculateValue7(), matches15 ? 1.0F : 0.0F, hands5.calculateValue32())
            .putVec4(hands5.calculateValue26(), hands5.checkCondition18() ? 1.0F : 0.0F, 0.0F, hands5.calculateValue48())
            .putVec4(amount33, angleRadians, hands5.calculateValue23(), hands5.calculateValue43());
         getStd140Builder(std140Builder4, hands5.calculateValue40());
         getStd140Builder(std140Builder4, hands5.calculateValue31());
         getStd140Builder(std140Builder4, hands5.calculateValue5());
         getStd140Builder(std140Builder4, hands5.calculateValue29());
         getStd140Builder(std140Builder4, hands5.calculateValue51());
         getStd140Builder(std140Builder4, hands5.calculateValue22());
         ByteBuffer buffer4 = std140Builder4.putVec2(renderTarget16.width, renderTarget16.height).get();
         this.gpuBuffer8 = this.getGpuBuffer(this.gpuBuffer8, 240, string45);
         commandEncoder10.writeToBuffer(this.gpuBuffer8.slice(), buffer4);
      } catch (Throwable throwable19) {
         if (memoryStack4 != null) {
            try {
               memoryStack4.close();
            } catch (Throwable throwable20) {
               throwable19.addSuppressed(throwable20);
            }
         }

         throw throwable19;
      }

      if (memoryStack4 != null) {
         memoryStack4.close();
      }

      RenderPipeline renderPipeline3 = matches18 ? this.renderPipeline15 : this.renderPipeline23;
      if (!matches18 && hands5.checkCondition3() && hands5.calculateValue47() > currentValue24) {
         int value14 = Math.clamp((long)hands5.calculateValue42(), 0, this.renderPipelineArray.length - 1);
         if (this.renderPipelineArray[value14] == null) {
            this.renderPipelineArray[value14] = getRenderPipeline3(value14);
         }

         renderPipeline3 = this.renderPipelineArray[value14];
      }

      RenderPass renderPass7 = commandEncoder10.createRenderPass(() -> string118, renderTarget17.getColorTextureView(), Optional.empty());

      try {
         
         renderPass7.setUniform(string64, renderTarget16.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         if (matches18) {
            renderPass7.setUniform(
               string19, this.renderTarget30.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
            );
         }

         renderPass7.setUniform(
            string120,
            (renderTarget19 != null ? renderTarget19 : this.renderTarget30).getColorTextureView(),
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
         );
         if (!matches18) {
            renderPass7.setUniform(
               string117, this.renderTarget31.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            );
            renderPass7.setUniform(string27, renderTarget16.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass7.setUniform(
               string115, this.renderTarget30.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
            );
            renderPass7.setUniform(string26, renderTarget18.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         }

         renderPass7.setUniform(string139, this.gpuBuffer8);
         renderPass7.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline3));
         renderPass7.draw(3, 1, 0, 0);
      } catch (Throwable throwable21) {
         if (renderPass7 != null) {
            try {
               renderPass7.close();
            } catch (Throwable throwable22) {
               throwable21.addSuppressed(throwable22);
            }
         }

         throw throwable21;
      }

      if (renderPass7 != null) {
         renderPass7.close();
      }
   }

   private RenderTarget getRenderTarget3(RenderTarget renderTarget20, String text4, int value15, int value16, boolean matches19) {
      return this.getRenderTarget2(renderTarget20, text4, value15, value16, matches19 ? GpuFormat.R8_UNORM : GpuFormat.RGBA8_UNORM);
   }

   private static RenderPipeline getRenderPipeline3(int value17) {
      RenderPipelineLayouts renderPipelineLayouts2 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts("dimasik/hands_composite_" + (value17 < 0 ? string73 : String.valueOf(value17)))
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string127))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string22))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts16(true, true)
         .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string83, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string124)
         .getRenderPipelineLayouts12(string129)
         .getRenderPipelineLayouts12(string69)
         .getRenderPipelineLayouts12(string32)
         .getRenderPipelineLayouts12(string112)
         .getRenderPipelineLayouts12(string49);
      renderPipelineLayouts2.getRenderPipelineLayouts21(string33, value17);
      return renderPipelineLayouts2.getRenderPipeline();
   }

   private void updateState7(HandsCompositeRenderer.LoLmBi3LPhwnPlIR loLmBi3LPhwnPlIR4, int value18, int value19, float amount34, CommandEncoder commandEncoder11) {
      boolean matches20 = loLmBi3LPhwnPlIR4.gpuBuffer12 != null
         && !loLmBi3LPhwnPlIR4.gpuBuffer12.isClosed()
         && loLmBi3LPhwnPlIR4.gpuBuffer10 != null
         && !loLmBi3LPhwnPlIR4.gpuBuffer10.isClosed();
      if (!matches20 || loLmBi3LPhwnPlIR4.stateCounter9 != value18 || loLmBi3LPhwnPlIR4.stateCounter5 != value19 || Float.compare(loLmBi3LPhwnPlIR4.currentValue5, amount34) != 0) {
         loLmBi3LPhwnPlIR4.gpuBuffer12 = this.getGpuBuffer(loLmBi3LPhwnPlIR4.gpuBuffer12, 48, loLmBi3LPhwnPlIR4.string13 + "-horizontal");
         loLmBi3LPhwnPlIR4.gpuBuffer10 = this.getGpuBuffer(loLmBi3LPhwnPlIR4.gpuBuffer10, 48, loLmBi3LPhwnPlIR4.string13 + "-vertical");
         float[] instance = new float[7];
         float amount35 = 1.0F;
         instance[0] = 1.0F;

         for (int index2 = 1; index2 < instance.length; index2++) {
            instance[index2] = (float)Math.exp(-(index2 * index2) / (2.0F * amount34 * amount34));
            amount35 += instance[index2] * 2.0F;
         }

         float[] instance2 = new float[3];
         float[] instance3 = new float[3];

         for (int index3 = 0; index3 < instance3.length; index3++) {
            int value20 = index3 * 2 + 1;
            float amount36 = instance[value20] + instance[value20 + 1];
            instance2[index3] = value20 + (amount36 > 0.0F ? instance[value20 + 1] / amount36 : 0.0F);
            instance3[index3] = amount36 / amount35;
         }

         MemoryStack memoryStack5 = MemoryStack.stackPush();

         try {
            for (int index4 = 0; index4 < 2; index4++) {
               ByteBuffer buffer5 = Std140Builder.onStack(memoryStack5, 48)
                  .putVec4(index4 == 0 ? 1.0F / value18 : 0.0F, index4 == 1 ? 1.0F / value19 : 0.0F, 0.0F, 0.0F)
                  .putVec4(1.0F / amount35, instance3[0], instance3[1], instance3[2])
                  .putVec4(instance2[0], instance2[1], instance2[2], 0.0F)
                  .get();
               commandEncoder11.writeToBuffer((index4 == 0 ? loLmBi3LPhwnPlIR4.gpuBuffer12 : loLmBi3LPhwnPlIR4.gpuBuffer10).slice(), buffer5);
            }
         } catch (Throwable throwable23) {
            if (memoryStack5 != null) {
               try {
                  memoryStack5.close();
               } catch (Throwable throwable24) {
                  throwable23.addSuppressed(throwable24);
               }
            }

            throw throwable23;
         }

         if (memoryStack5 != null) {
            memoryStack5.close();
         }

         loLmBi3LPhwnPlIR4.stateCounter9 = value18;
         loLmBi3LPhwnPlIR4.stateCounter5 = value19;
         loLmBi3LPhwnPlIR4.currentValue5 = amount34;
      }
   }

   private void updateState16(
      RenderTarget renderTarget21,
      RenderTarget renderTarget22,
      RenderTarget renderTarget23,
      RenderTarget renderTarget24,
      Hands hands6,
      boolean matches21,
      boolean matches22,
      RenderPipeline renderPipeline4,
      CommandEncoder commandEncoder12
   ) {
      float amount37 = matches21 ? hands6.calculateValue26() : 0.0F;
      int value21 = hands6.calculateValue51();
      int value22 = hands6.calculateValue22();
      int value23 = hands6.calculateValue21();
      float amount38 = matches22 ? hands6.calculateValue49() : 0.0F;
      GpuBuffer gpuBuffer3 = matches22 ? this.gpuBuffer6 : this.gpuBuffer13;
      GpuBuffer gpuBuffer4 = this.getGpuBuffer(gpuBuffer3, 80, matches22 ? string98 : string39);
      HandsCompositeRenderer.J6fV8uaRqaXhTRoo j6fV8uaRqaXhTRoo = matches22 ? this.j6fV8uaRqaXhTRoo3 : this.j6fV8uaRqaXhTRoo2;
      if (gpuBuffer4 != gpuBuffer3 || !j6fV8uaRqaXhTRoo.checkCondition2(amount37, value21, value22, value23, renderTarget22.width, renderTarget22.height, amount38, matches22)) {
         MemoryStack memoryStack6 = MemoryStack.stackPush();

         try {
            Std140Builder std140Builder5 = Std140Builder.onStack(memoryStack6, 80).putVec4(amount37, hands6.checkCondition18() ? 1.0F : 0.0F, 0.0F, 0.0F);
            getStd140Builder(std140Builder5, value21);
            getStd140Builder(std140Builder5, value22);
            getStd140Builder(std140Builder5, value23);
            ByteBuffer buffer6 = std140Builder5.putVec4(renderTarget22.width, renderTarget22.height, amount38, matches22 ? 1.0F : 0.0F).get();
            commandEncoder12.writeToBuffer(gpuBuffer4.slice(), buffer6);
         } catch (Throwable throwable25) {
            if (memoryStack6 != null) {
               try {
                  memoryStack6.close();
               } catch (Throwable throwable26) {
                  throwable25.addSuppressed(throwable26);
               }
            }

            throw throwable25;
         }

         if (memoryStack6 != null) {
            memoryStack6.close();
         }

         j6fV8uaRqaXhTRoo.updateState19(amount37, value21, value22, value23, renderTarget22.width, renderTarget22.height, amount38, matches22);
      }

      if (matches22) {
         this.gpuBuffer6 = gpuBuffer4;
      } else {
         this.gpuBuffer13 = gpuBuffer4;
      }

      RenderPass renderPass8 = commandEncoder12.createRenderPass(
         () -> matches21 && matches22 ? string143 : (matches22 ? string122 : string23), renderTarget24.getColorTextureView(), Optional.empty()
      );

      try {
         renderPass8.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline4));
         renderPass8.setUniform(string148, renderTarget21.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         renderPass8.setUniform(string136, renderTarget22.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         if (matches22) {
            renderPass8.setUniform(string145, renderTarget23.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         }

         renderPass8.setUniform(string96, gpuBuffer4);
         renderPass8.draw(3, 1, 0, 0);
      } catch (Throwable throwable27) {
         if (renderPass8 != null) {
            try {
               renderPass8.close();
            } catch (Throwable throwable28) {
               throwable27.addSuppressed(throwable28);
            }
         }

         throw throwable27;
      }

      if (renderPass8 != null) {
         renderPass8.close();
      }
   }

   private void updateState20(RenderTarget renderTarget25, RenderTarget renderTarget26, CommandEncoder commandEncoder13) {
      RenderPass renderPass9 = commandEncoder13.createRenderPass(() -> string116, renderTarget26.getColorTextureView(), Optional.empty());

      try {
         renderPass9.setPipeline(RenderSystem.getCompiledPipeline(this.renderPipeline27));
         renderPass9.setUniform(string53, renderTarget25.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         renderPass9.setUniform(
            string141, this.renderTarget30.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
         );
         renderPass9.draw(3, 1, 0, 0);
      } catch (Throwable throwable29) {
         if (renderPass9 != null) {
            try {
               renderPass9.close();
            } catch (Throwable throwable30) {
               throwable29.addSuppressed(throwable30);
            }
         }

         throw throwable29;
      }

      if (renderPass9 != null) {
         renderPass9.close();
      }
   }

   private HandsCompositeRenderer() {
      this.renderPipeline15 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string4)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string68))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string78))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts16(true, true)
         .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string18, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string56)
         .getRenderPipelineLayouts12(string15)
         .getRenderPipelineLayouts12(string51)
         .getRenderPipeline();
      this.renderPipeline24 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string42)
         .getRenderPipelineLayouts22(GpuFormat.R8_UNORM)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string123))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string24))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts10()
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts12(string58)
         .getRenderPipelineLayouts12(string131)
         .getRenderPipeline();
      this.renderPipeline22 = getRenderPipeline4(string113, false, false);
      this.renderPipeline10 = getRenderPipeline4(string66, true, false);
      this.renderPipeline8 = getRenderPipeline4(string97, true, false);
      this.renderPipeline17 = getRenderPipeline4(string6, false, true);
      this.renderPipeline13 = getRenderPipeline4(string135, true, true);
      this.renderPipeline19 = getRenderPipeline4(string125, true, false);
      this.renderPipeline20 = getRenderPipeline2(string40, GpuFormat.RGBA16_FLOAT, false);
      this.renderPipeline12 = getRenderPipeline2(string142, GpuFormat.RGBA16_FLOAT, false);
      this.renderPipeline25 = getRenderPipeline2(string132, GpuFormat.RGBA16_FLOAT, true);
      this.renderPipeline27 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string9)
         .getRenderPipelineLayouts22(GpuFormat.RGBA16_FLOAT)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string90))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string35))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts10()
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts12(string133)
         .getRenderPipelineLayouts12(string29)
         .getRenderPipeline();
      this.renderPipeline11 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string128)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string60))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string85))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.LIGHTNING)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string17, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string81)
         .getRenderPipelineLayouts12(string61)
         .getRenderPipeline();
      this.renderPipeline21 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string107)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string119))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string104))
         .getRenderPipelineLayouts12(string44)
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string88, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string146)
         .getRenderPipelineLayouts12(string59)
         .getRenderPipeline();
      this.renderPipeline18 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string71)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string84))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string80))
         .getRenderPipelineLayouts12(string65)
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string114, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string63)
         .getRenderPipelineLayouts12(string95)
         .getRenderPipeline();
      this.renderPipeline26 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string144)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string126))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string57))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts10()
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string52, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string12)
         .getRenderPipeline();
      this.renderPipeline9 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string147)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string152))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string99))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts10()
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string108, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string38)
         .getRenderPipeline();
      this.renderPipeline16 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string102)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string28))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string74))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string101, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string72)
         .getRenderPipeline();
      this.renderPipeline14 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string121)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string110))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string93))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string82, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string134)
         .getRenderPipeline();
      this.renderPipeline7 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string100)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string138))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string149))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts10()
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string106, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string20)
         .getRenderPipeline();
      this.renderPipeline5 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string47)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string14))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string46))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts2(string3, UniformType.UNIFORM_BUFFER)
         .getRenderPipelineLayouts12(string54)
         .getRenderPipeline();
      this.renderPipeline6 = RenderPipelineLayouts.getRenderPipelineLayouts6()
         .getRenderPipelineLayouts(string77)
         .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(string75))
         .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(string43))
         .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
         .getRenderPipelineLayouts9(false)
         .getRenderPipelineLayouts3(false)
         .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
         .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
         .getRenderPipelineLayouts12(string86)
         .getRenderPipeline();
      this.loLmBi3LPhwnPlIR7 = new HandsCompositeRenderer.LoLmBi3LPhwnPlIR(string92, true);
      this.loLmBi3LPhwnPlIR5 = new HandsCompositeRenderer.LoLmBi3LPhwnPlIR(string30, GpuFormat.RGBA16_FLOAT);
      this.loLmBi3LPhwnPlIR6 = new HandsCompositeRenderer.LoLmBi3LPhwnPlIR(string, false);
      this.stateCounter3 = -1;
      this.j6fV8uaRqaXhTRoo2 = new HandsCompositeRenderer.J6fV8uaRqaXhTRoo();
      this.j6fV8uaRqaXhTRoo3 = new HandsCompositeRenderer.J6fV8uaRqaXhTRoo();
   }

   private RenderTarget getRenderTarget4(RenderTarget renderTarget27, Hands hands7, int value24, float amount39, boolean matches23, CommandEncoder commandEncoder14) {
      return this.getRenderTarget5(
         this.renderTarget30,
         hands7.checkCondition18() ? this.loLmBi3LPhwnPlIR5 : this.loLmBi3LPhwnPlIR7,
         value24,
         amount39,
         matches23,
         hands7.checkCondition18() ? renderTarget27 : null,
         commandEncoder14
      );
   }

   private GpuBuffer getGpuBuffer(GpuBuffer gpuBuffer5, int value25, String text5) {
      return gpuBuffer5 != null && !gpuBuffer5.isClosed() ? gpuBuffer5 : RenderSystem.getDevice().createBuffer(() -> text5, 136, value25);
   }

   private void updateState3(CommandEncoder commandEncoder15, RenderTarget renderTarget28, Hands hands8, float amount40, float amount41) {
      float amount42 = hands8.calculateValue26() * hands8.calculateValue27() * this.currentValue50;
      if (!(amount42 <= currentValue48)) {
         MemoryStack memoryStack7 = MemoryStack.stackPush();

         try {
            Std140Builder std140Builder6 = Std140Builder.onStack(memoryStack7, 64)
               .putVec4(amount42, amount41, amount40, hands8.calculateValue19())
               .putVec4(hands8.calculateValue11(), (float)renderTarget28.width / Math.max(1, renderTarget28.height), hands8.checkCondition18() ? 1.0F : 0.0F, 0.0F);
            getStd140Builder(std140Builder6, hands8.calculateValue51());
            getStd140Builder(std140Builder6, hands8.calculateValue22());
            this.gpuBuffer9 = this.getGpuBuffer(this.gpuBuffer9, 64, string105);
            commandEncoder15.writeToBuffer(this.gpuBuffer9.slice(), std140Builder6.get());
            RenderPass renderPass10 = commandEncoder15.createRenderPass(() -> string36, this.textureTarget2.getColorTextureView(), Optional.empty());

            try {
               renderPass10.setPipeline(RenderSystem.getCompiledPipeline(this.renderPipeline5));
               renderPass10.setUniform(string103, renderTarget28.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
               renderPass10.setUniform(string55, this.gpuBuffer9);
               renderPass10.draw(3, 1, 0, 0);
            } catch (Throwable throwable31) {
               if (renderPass10 != null) {
                  try {
                     renderPass10.close();
                  } catch (Throwable throwable32) {
                     throwable31.addSuppressed(throwable32);
                  }
               }

               throw throwable31;
            }

            if (renderPass10 != null) {
               renderPass10.close();
            }
         } catch (Throwable throwable33) {
            if (memoryStack7 != null) {
               try {
                  memoryStack7.close();
               } catch (Throwable throwable34) {
                  throwable33.addSuppressed(throwable34);
               }
            }

            throw throwable33;
         }

         if (memoryStack7 != null) {
            memoryStack7.close();
         }
      }
   }

   private static final class J6fV8uaRqaXhTRoo {
      private float currentValue25 = Float.NaN;
      private int stateCounter;
      private int stateCounter7;
      private int stateCounter8;
      private int stateCounter6 = -1;
      private int stateCounter10 = -1;
      private float currentValue35 = Float.NaN;
      private boolean stateActive;

      private void updateState19(float amount43, int value31, int value32, int value33, int value34, int value35, float amount44, boolean matches24) {
         this.currentValue25 = amount43;
         this.stateCounter = value31;
         this.stateCounter7 = value32;
         this.stateCounter8 = value33;
         this.stateCounter6 = value34;
         this.stateCounter10 = value35;
         this.currentValue35 = amount44;
         this.stateActive = matches24;
      }

      private void updateState10() {
         this.currentValue25 = Float.NaN;
         this.stateCounter6 = -1;
         this.stateCounter10 = -1;
         this.currentValue35 = Float.NaN;
      }

      private boolean checkCondition2(float amount45, int value36, int value37, int value38, int value39, int value40, float amount46, boolean matches25) {
         return Float.compare(this.currentValue25, amount45) == 0
            && this.stateCounter == value36
            && this.stateCounter7 == value37
            && this.stateCounter8 == value38
            && this.stateCounter6 == value39
            && this.stateCounter10 == value40
            && Float.compare(this.currentValue35, amount46) == 0
            && this.stateActive == matches25;
      }
   }

   private static final class LoLmBi3LPhwnPlIR {
      private final String string13;
      private final boolean stateActive5;
      private final GpuFormat gpuFormat4;
      private final List<RenderTarget> list = new ArrayList<>(2);
      private RenderTarget renderTarget29;
      private GpuBuffer gpuBuffer12;
      private GpuBuffer gpuBuffer10;
      private int stateCounter9;
      private int stateCounter5;
      private float currentValue5 = Float.NaN;

      private LoLmBi3LPhwnPlIR(String text7, boolean matches26) {
         this(text7, matches26 ? GpuFormat.R8_UNORM : GpuFormat.RGBA8_UNORM);
      }

      private LoLmBi3LPhwnPlIR(String text8, GpuFormat gpuFormat3) {
         this.string13 = text8;
         this.gpuFormat4 = gpuFormat3;
         this.stateActive5 = gpuFormat3 == GpuFormat.R8_UNORM;
      }
   }
}
