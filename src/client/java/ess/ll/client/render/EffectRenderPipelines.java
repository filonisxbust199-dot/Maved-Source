package ess.ll.client.render;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import java.util.HashMap;
import java.util.Map;
public final class EffectRenderPipelines {
 private static final Map<Identifier,RenderType> PARTICLES=new HashMap<>();
 private static final RenderPipeline GHOST_NO_DEPTH=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
  .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/target_ghost_no_depth"))
  .withVertexShader(Identifier.fromNamespaceAndPath("hex","core/particle"))
  .withFragmentShader(Identifier.fromNamespaceAndPath("hex","core/particle"))
  .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
  .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE,BlendFactor.ONE,BlendFactor.ONE,BlendFactor.ZERO)))
  .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false)).withCull(true).build());
 private static final RenderPipeline GHOST_DEPTH_PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
  .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/target_ghost_depth"))
  .withVertexShader(Identifier.fromNamespaceAndPath("hex","core/particle"))
  .withFragmentShader(Identifier.fromNamespaceAndPath("hex","core/particle"))
  .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
  .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE,BlendFactor.ONE,BlendFactor.ONE,BlendFactor.ZERO)))
  .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false)).withCull(true).build());
 private static final RenderType GHOST_DEPTH=RenderType.create("TargetESP ghost depth",RenderSetup.builder(GHOST_DEPTH_PIPELINE)
  .withTexture("Sampler0",Identifier.fromNamespaceAndPath("hex","particles/orbiznew.png")).createRenderSetup());
 private static final RenderType GHOST_WALL=RenderType.create("TargetESP ghost walls",RenderSetup.builder(GHOST_NO_DEPTH)
  .withTexture("Sampler0",Identifier.fromNamespaceAndPath("hex","particles/orbiznew.png")).createRenderSetup());
 private static RenderType colored(String name,PrimitiveTopology topology,boolean depth,BlendFunction blend) {
  RenderPipeline pipeline=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
   .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/target_"+name))
   .withVertexBinding(0,topology==PrimitiveTopology.LINES?DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH:DefaultVertexFormat.POSITION_COLOR)
   .withPrimitiveTopology(topology).withColorTargetState(new ColorTargetState(blend))
   .withDepthStencilState(new DepthStencilState(depth?CompareOp.GREATER_THAN_OR_EQUAL:CompareOp.ALWAYS_PASS,false))
   .withCull(false).build());
  return RenderType.create("TargetESP "+name,RenderSetup.builder(pipeline).createRenderSetup());
 }
 private static RenderType targetLine(String name,boolean depth,BlendFunction blend) {
  RenderPipeline pipeline=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
   .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/target_"+name))
   .withVertexShader(Identifier.fromNamespaceAndPath("hex","core/prediction_line"))
   .withFragmentShader(Identifier.fromNamespaceAndPath("hex","core/prediction_line"))
   .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
   .withPrimitiveTopology(PrimitiveTopology.LINES).withColorTargetState(new ColorTargetState(blend))
   .withDepthStencilState(new DepthStencilState(depth?CompareOp.GREATER_THAN_OR_EQUAL:CompareOp.ALWAYS_PASS,false))
   .withCull(false).build());
  return RenderType.create("TargetESP "+name,RenderSetup.builder(pipeline).createRenderSetup());
 }
 private static final RenderType CIRCLE_WALL=colored("circle_wall",PrimitiveTopology.TRIANGLE_STRIP,false,BlendFunction.OVERLAY);
 private static final RenderType CIRCLE_LINE=targetLine("circle_line",false,BlendFunction.OVERLAY);
 private static final RenderType OLD_CIRCLE_LINE=targetLine("old_circle_line",true,BlendFunction.TRANSLUCENT);
 private static final RenderType OLD_CIRCLE_WALL=colored("old_circle_wall",PrimitiveTopology.TRIANGLE_STRIP,true,BlendFunction.TRANSLUCENT);
 private static final Map<Identifier,RenderType> SQUARES=new HashMap<>();
 private static final RenderPipeline SQUARE_PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
  .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/target_square"))
  .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
  .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
  .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false)).withCull(false).build());
 private static final RenderPipeline PARTICLE_PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
  .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/particles"))
  .withVertexShader(Identifier.fromNamespaceAndPath("hex","core/particle"))
  .withFragmentShader(Identifier.fromNamespaceAndPath("hex","core/particle"))
  .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR)
  .withPrimitiveTopology(PrimitiveTopology.QUADS)
  .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE,BlendFactor.ONE,BlendFactor.ONE,BlendFactor.ZERO)))
  .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
  .withCull(true).build());
 public static final RenderType LINES=RenderTypes.lines();
 public static RenderType translucentEntity(Identifier texture){return RenderTypes.entityTranslucent(texture);}
 public static RenderType translucentSprite(Identifier texture){return RenderTypes.entityTranslucent(texture);}
 public static RenderType particles(Identifier texture){return PARTICLES.computeIfAbsent(texture,id->RenderType.create("ESS particles",RenderSetup.builder(PARTICLE_PIPELINE).withTexture("Sampler0",id).createRenderSetup()));}
 public static RenderType lines(){return RenderTypes.lines();}
 public static RenderType ghost(boolean through){return through?GHOST_WALL:GHOST_DEPTH;}
 public static RenderType legacyCircleFill(){return OLD_CIRCLE_WALL;}
 public static RenderType legacyCircleOutline(){return OLD_CIRCLE_LINE;}
 public static RenderType circleOutline(){return CIRCLE_LINE;}
 public static RenderType circleFill(){return CIRCLE_WALL;}
 public static RenderType targetSquare(Identifier texture){return SQUARES.computeIfAbsent(texture,id->RenderType.create("TargetESP square",RenderSetup.builder(SQUARE_PIPELINE).withTexture("Sampler0",id).createRenderSetup()));}
}
