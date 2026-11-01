package ess.ll.client.render.targetfx;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ess.ll.client.render.targetfx.api.RingParticleEmitter;
import ess.ll.client.render.targetfx.api.MinecraftContext;
import ess.ll.client.render.targetfx.event.DeltaTrackerPoseStackEvent;
import ess.ll.client.render.targetfx.internal.ColorMath;
import ess.ll.client.render.targetfx.internal.ChamsShaderProgram;
import ess.ll.client.render.targetfx.internal.TargetEspShader;
import ess.ll.client.render.targetfx.internal.RenderPipelineLayouts;
import ess.ll.client.render.targetfx.internal.GpuResourceScope;
import ess.ll.client.render.targetfx.internal.BloomCompositor;
import ess.ll.client.render.targetfx.internal.DeadChamsCompositeShader;
import ess.ll.client.render.targetfx.internal.CubeGlowShader;
import ess.ll.client.render.targetfx.internal.TargetLightingCompositor;
import ess.ll.client.render.targetfx.internal.ClientIdentifiers;
import ess.ll.client.render.DimasikRenderPipelinesAccessor;
import ess.ll.client.render.RenderPipelineCache;
import ess.ll.client.render.targetfx.model.ThemeColorRole;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
public final class TargetEspRenderer implements MinecraftContext {
   private static final Identifier resourceIdentifier10 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string100, TargetEspRenderer.string122
   );
   private static final Identifier resourceIdentifier11 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string135, TargetEspRenderer.string73
   );
   private static final Identifier resourceIdentifier = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string99, TargetEspRenderer.string91
   );
   private static final Identifier resourceIdentifier6 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string59, TargetEspRenderer.string145
   );
   private static final Identifier resourceIdentifier8 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string137, TargetEspRenderer.string96
   );
   private static final Identifier resourceIdentifier3 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string86, TargetEspRenderer.string155
   );
   private static final Identifier resourceIdentifier5 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string11, TargetEspRenderer.string141
   );
   private static final Identifier resourceIdentifier4 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string3, TargetEspRenderer.string108
   );
   private static final Identifier resourceIdentifier9 = Identifier.fromNamespaceAndPath(
      TargetEspRenderer.string34, TargetEspRenderer.string148
   );
   private static final RenderPipeline renderPipeline29 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string19)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string60))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string35))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string84)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline15 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string163)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string156))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string123))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(true)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string6)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline25 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string90)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string81))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string87))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(true)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string13)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline23 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string20)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string85))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string27))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(true)
      .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline17 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string128)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string26))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string157))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(true)
      .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline10 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string82)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string43))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string143))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string5)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline24 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string140)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string167))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string83))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.LIGHTNING)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline13 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string58)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string76))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string113))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.LIGHTNING)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline30 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string121)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string151))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string112))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.LIGHTNING)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string118)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline19 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string166)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string21))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string106))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.LIGHTNING)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string159)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline28 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string24)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string158))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string17))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline21 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string133)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string103))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string80))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline11 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string171)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string169))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string22))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.OVERLAY)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline12 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string67)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string125))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string68))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts10()
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline14 = RenderPipelineLayouts.getRenderPipelineLayouts6()
      .getRenderPipelineLayouts(TargetEspRenderer.string165)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string136))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string124))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT)
      .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
      .getRenderPipelineLayouts12(TargetEspRenderer.string144)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline26 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string36)
      .getRenderPipelineLayouts18(1.0F, 1.0F)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string74))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string61))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string23)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline16 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string97)
      .getRenderPipelineLayouts18(1.0F, 1.0F)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string95))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string18))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(true)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipelineLayouts12(TargetEspRenderer.string48)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline20 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string42)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string57))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string7))
      .getRenderPipelineLayouts5(CompareOp.GREATER_THAN_OR_EQUAL)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline22 = RenderPipelineLayouts.getRenderPipelineLayouts6()
      .getRenderPipelineLayouts(TargetEspRenderer.string98)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string49))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string62))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.TRANSLUCENT)
      .getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat, PrimitiveTopology.TRIANGLES)
      .getRenderPipelineLayouts12(TargetEspRenderer.string162)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline18 = RenderPipelineLayouts.getRenderPipelineLayouts6(DimasikRenderPipelinesAccessor.getPositionColorSnippet())
      .getRenderPipelineLayouts(TargetEspRenderer.string147)
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.DEBUG_LINES)
      .getRenderPipeline();
   private static final RenderPipeline renderPipeline8 = RenderPipelineLayouts.getRenderPipelineLayouts6(RenderPipelineLayouts.snippet)
      .getRenderPipelineLayouts(TargetEspRenderer.string115)
      .getRenderPipelineLayouts2(TargetEspRenderer.string69, UniformType.UNIFORM_BUFFER)
      .getRenderPipelineLayouts11(ClientIdentifiers.clientResource(TargetEspRenderer.string142))
      .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(TargetEspRenderer.string38))
      .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
      .getRenderPipelineLayouts9(false)
      .getRenderPipelineLayouts3(false)
      .getRenderPipelineLayouts7(BlendFunction.ADDITIVE)
      .getRenderPipelineLayouts8(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .getRenderPipeline();
   private static final float currentValue14 = 0.82F;
   private static LivingEntity livingEntity;
   private static float currentValue629;
   private static float currentValue1101;
   private static long lastUpdateMillis45;
   private static String string109 = "";
   private static final List<Vector3f> list5 = new ArrayList<>();
   private static final float[][] floatArray = new float[][]{
      {0.0F, TargetEspRenderer.currentValue245, 0.0F},
      {0.0F, TargetEspRenderer.currentValue478, 0.0F},
      {1.0F, 0.0F, 0.0F},
      {TargetEspRenderer.currentValue502, 0.0F, 0.0F},
      {0.0F, 0.0F, 1.0F},
      {0.0F, 0.0F, TargetEspRenderer.currentValue1071}
   };
   private static final int[][] intArray = new int[][]{{0, 4, 2}, {0, 3, 4}, {0, 5, 3}, {0, 2, 5}, {1, 2, 4}, {1, 4, 3}, {1, 3, 5}, {1, 5, 2}};
   private static final float[] floatArray2 = new float[]{
      1.0F,
      TargetEspRenderer.currentValue599,
      TargetEspRenderer.currentValue686,
      TargetEspRenderer.currentValue522,
      TargetEspRenderer.currentValue554,
      TargetEspRenderer.currentValue162,
      TargetEspRenderer.currentValue979,
      TargetEspRenderer.currentValue819
   };
   private static int stateCounter8 = TargetEspRenderer.stateCounter54;
   private static float currentValue924;
   private static float currentValue27;
   private static final List<Vector3f> list2 = new ArrayList<>();
   private static int stateCounter57 = TargetEspRenderer.stateCounter23;
   private static final Random random5 = new Random();
   private static final List<TargetEspRenderer.vbgKRi4F34zTzWmv> list4 = new ArrayList<>();
   private static long lastUpdateMillis30;
   private static TextureTarget textureTarget2;
   private static final LinkedList<Vec3>[] linkedListArray = new LinkedList[]{
      new LinkedList(), new LinkedList(), new LinkedList(), new LinkedList(), new LinkedList()
   };
   private static final List<TargetEspRenderer.CefWHVrCHHOwSLC4> list9 = new ArrayList<>();
   private static double currentValue573;
   private static final long lastUpdateMillis13 = System.currentTimeMillis();
   private static float currentValue920;
   private static long lastUpdateMillis32 = System.nanoTime();
   private static long lastUpdateMillis3;
   private static int stateCounter14 = -1;
   private static int stateCounter10 = TargetEspRenderer.stateCounter6;
   private static float currentValue377 = TargetEspRenderer.currentValue77;
   private static String string149 = TargetEspRenderer.string117;
   private static String string170 = TargetEspRenderer.string105;
   private static final double[] doubleArray2 = getStateActive82(true);
   private static final double[] doubleArray = getStateActive82(false);
   private static final double currentValue48 = 9.0;
   private static float currentValue256 = 1.0F;
   private static float currentValue505;
   private static double currentValue378;
   private static boolean stateActive5;
   private static int stateCounter40 = TargetEspRenderer.stateCounter28;
   private static int stateCounter15 = 3;
   private static boolean stateActive = true;
   private static String string52 = TargetEspRenderer.string72;
   private static long lastUpdateMillis41;
   private static float currentValue25;
   private static float currentValue820;
   private static String string168 = TargetEspRenderer.string94;
   private static boolean stateActive11;
   private static String string107 = TargetEspRenderer.string66;
   private static boolean stateActive6;
   private static boolean stateActive4 = true;
   private static String string47 = "";
   private static String string111 = "";
   private static String string71 = "";
   private static final LinkedList<TargetEspRenderer.BNt5vHvMwoeyBNKB> linkedList3 = new LinkedList<>();
   private static long lastUpdateMillis35;
   private static int stateCounter31 = TargetEspRenderer.stateCounter53;
   private static final long lastUpdateMillis = 90L;
   private static final List<TargetEspRenderer.F0nIo1tGX54h6o8A> list = new ArrayList<>();
   private static long lastUpdateMillis9;
   private static final long lastUpdateMillis2 = 60L;
   private static final List<TargetEspRenderer.SjzMjyMYX7ylg4dN> list3 = new ArrayList<>();
   private static long lastUpdateMillis27;
   private static int stateCounter36 = TargetEspRenderer.stateCounter22;
   private static float currentValue595;
   private static TextureTarget textureTarget3;
   private static boolean stateActive3;
   private static float currentValue908 = TargetEspRenderer.currentValue250;
   private static boolean stateActive2;
   private static boolean stateActive12 = true;
   private static boolean stateActive8;
   private static int stateCounter35 = -1;
   private static float currentValue17 = 1.0F;
   private static float currentValue246;
   private static boolean stateActive7;
   private static boolean stateActive10;
   private static double currentValue57;
   private static double currentValue1034;
   private static double currentValue519;
   private static double currentValue949;
   private static double currentValue836;
   private static double currentValue791;
   private static boolean stateActive13;
   private static DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent28;
   private static LivingEntity livingEntity2;
   private static String string14;
   private static int stateCounter43;
   private static int stateCounter32;
   private static GpuBufferSlice gpuBufferSlice2;
   private static ProjectionType projectionType2;
   private static final Identifier resourceIdentifier7 = ClientIdentifiers.assetIdentifier(TargetEspRenderer.string65);
   private static final RenderPipeline renderPipeline9 = ChamsShaderProgram.getRenderPipeline2(
      TargetEspRenderer.string29, CompareOp.GREATER_THAN_OR_EQUAL
   );
   private static final RenderPipeline renderPipeline27 = ChamsShaderProgram.getRenderPipeline2(
      TargetEspRenderer.string40, CompareOp.ALWAYS_PASS
   );
   private static float currentValue806 = TargetEspRenderer.currentValue1051;
   private static int stateCounter13 = 14;
   private static boolean stateActive9 = true;
   private static float currentValue130;
   private static long lastUpdateMillis23;
   private static int stateCounter26 = TargetEspRenderer.stateCounter4;
   private static int stateCounter37;
   private static final float currentValue742 = 0.1F;
   private static final float currentValue227 = 5.0F;
   private static final String string150 = "Тип 1";
   private static final String string63 = "Тип 1";
   private static final String string64 = "Тип 1";
   private static final String string129 = "Тип 1";
   private static final String string138 = "Тип 2";
   private static final int stateCounter20 = Integer.MIN_VALUE;
   private static final int stateCounter49 = Integer.MIN_VALUE;
   private static final int stateCounter58 = Integer.MIN_VALUE;
   private static final int stateCounter42 = Integer.MIN_VALUE;
   private static final float currentValue694 = 0.01F;
   private static final float currentValue772 = 0.01F;
   private static final float currentValue319 = 5.5F;
   private static final String string120 = "InSampler";
   private static final String string131 = "Призраки";
   private static final String string160 = "Кристалики";
   private static final String string154 = "Молнии";
   private static final float currentValue292 = 0.1F;
   private static final float currentValue123 = 1000.0F;
   private static final String string15 = "Кольцо";
   private static final String string70 = "Тип 3";
   private static final String string116 = "Куб";
   private static final float currentValue1187 = 1.05F;
   private static final float currentValue1092 = 0.3F;
   private static final String string101 = "Молнии";
   private static final String string55 = "Тип 3";
   private static final int stateCounter24 = Integer.MIN_VALUE;
   private static final int stateCounter11 = Integer.MIN_VALUE;
   private static final int stateCounter44 = Integer.MIN_VALUE;
   private static final int stateCounter9 = Integer.MIN_VALUE;
   private static final String string146 = "Кристалики";
   private static final String string161 = "Тип 4";
   private static final float currentValue739 = 1000.0F;
   private static final float currentValue580 = 0.15F;
   private static final double currentValue228 = Math.PI;
   private static final double currentValue974 = 2.0;
   private static final float currentValue141 = 0.001F;
   private static final float currentValue468 = 0.01F;
   private static final int stateCounter52 = -65536;
   private static final int stateCounter12 = -65536;
   private static final String string30 = "Призраки";
   private static final String string16 = "Кристалики";
   private static final String string126 = "Молнии";
   private static final String string78 = "Кольцо";
   private static final String string39 = "Обруч";
   private static final String string50 = "Призраки";
   private static final String string51 = "Кристалики";
   private static final String string28 = "Ромбики";
   private static final String string12 = "Молнии";
   private static final String string134 = "Куб";
   private static final String string33 = "Квадрат";
   private static final String string25 = "Треугольник";
   private static final String string2 = "Пентограмма";
   private static final String string8 = "Тип 3";
   private static final String string127 = "Тип 2";
   private static final int stateCounter51 = Integer.MIN_VALUE;
   private static final String string130 = "Тип 2";
   private static final String string153 = "Тип 3";
   private static final String string104 = "Тип 4";
   private static final String string37 = "Тип 5";
   private static final String string45 = "Тип 6";
   private static final String string9 = "Тип 7";
   private static final String string75 = "Тип 8";
   private static final int stateCounter = Integer.MIN_VALUE;
   private static final int stateCounter3 = Integer.MIN_VALUE;
   private static final String string89 = "Тип 2";
   private static final String string31 = "Тип 3";
   private static final String string54 = "Тип 4";
   private static final String string119 = "Тип 3";
   private static final String string139 = "Тип 1";
   private static final String string164 = "Тип 3";
   private static final String string32 = "Never";
   private static final String string56 = "Auto";
   private static final String string88 = "Молнии";
   private static final String string53 = "Кристалики";
   private static final long lastUpdateMillis11 = 360000L;
   private static final float currentValue615 = 11.5F;
   private static final float currentValue940 = 0.001F;
   private static final float currentValue710 = 1.18F;
   private static final float currentValue1149 = 0.18F;
   private static final double currentValue826 = 0.035;
   private static final double currentValue1172 = 1.7;
   private static final double currentValue642 = 2.41;
   private static final float currentValue1075 = 0.68F;
   private static final float currentValue654 = 0.3F;
   private static final float currentValue1065 = 0.36F;
   private static final float currentValue51 = 0.28F;
   private static final float currentValue799 = 0.42F;
   private static final double currentValue572 = 0.035;
   private static final double currentValue127 = 1.7;
   private static final double currentValue767 = 2.41;
   private static final float currentValue443 = 1.15F;
   private static final float currentValue732 = -0.9F;
   private static final float currentValue1133 = 37.7F;
   private static final float currentValue626 = 91.345F;
   private static final float currentValue986 = 12.17F;
   private static final float currentValue691 = 43758.547F;
   private static final float currentValue297 = 0.075F;
   private static final float currentValue5 = 0.115F;
   private static final float currentValue1096 = 0.03F;
   private static final float currentValue1193 = 0.8F;
   private static final float currentValue208 = 1.73F;
   private static final float currentValue549 = 0.06F;
   private static final float currentValue839 = 255.0F;
   private static final float currentValue1005 = 155.0F;
   private static final float currentValue578 = 0.7F;
   private static final float currentValue284 = 1.08F;
   private static final long lastUpdateMillis38 = 360000L;
   private static final float currentValue768 = 40.0F;
   private static final long lastUpdateMillis28 = 360000L;
   private static final float currentValue384 = 11.5F;
   private static final float currentValue81 = 0.001F;
   private static final float currentValue498 = 1.27F;
   private static final float currentValue442 = 0.08F;
   private static final float currentValue613 = 0.85F;
   private static final float currentValue684 = 0.55F;
   private static final float currentValue322 = 1.18F;
   private static final float currentValue671 = 0.18F;
   private static final float currentValue289 = 0.18F;
   private static final float currentValue776 = 3.6F;
   private static final float currentValue596 = 0.09F;
   private static final float currentValue408 = 1.8F;
   private static final float currentValue222 = 0.24F;
   private static final float currentValue278 = 0.28F;
   private static final float currentValue1115 = 0.34F;
   private static final float currentValue88 = 255.0F;
   private static final float currentValue85 = 155.0F;
   private static final float currentValue373 = 1.08F;
   private static final float currentValue1047 = 0.82F;
   private static final float currentValue268 = 1.18F;
   private static final float currentValue325 = 1.0E-5F;
   private static final float currentValue939 = 1.7F;
   private static final float currentValue466 = 2.41F;
   private static final float currentValue371 = 0.035F;
   private static final float currentValue724 = 6.8F;
   private static final float currentValue559 = 0.88F;
   private static final float currentValue166 = 0.5F;
   private static final float currentValue985 = 1.15F;
   private static final float currentValue1137 = -0.9F;
   private static final float currentValue386 = 37.7F;
   private static final float currentValue637 = 360.0F;
   private static final float currentValue59 = 0.82F;
   private static final float currentValue488 = 0.42F;
   private static final float currentValue852 = 0.18F;
   private static final float currentValue460 = 1.75F;
   private static final float currentValue244 = 0.02F;
   private static final float currentValue471 = 0.08F;
   private static final float currentValue389 = 0.82F;
   private static final float currentValue1184 = 0.82F;
   private static final float currentValue831 = 1.35F;
   private static final float currentValue7 = 0.45F;
   private static final float currentValue918 = 1.5F;
   private static final float currentValue548 = 14.0F;
   private static final float currentValue851 = 0.85F;
   private static final float currentValue874 = 0.12F;
   private static final float currentValue1073 = 18.0F;
   private static final float currentValue997 = 0.16F;
   private static final float currentValue1045 = 0.42F;
   private static final float currentValue149 = 205.0F;
   private static final float currentValue352 = 0.78F;
   private static final float currentValue455 = 0.95F;
   private static final float currentValue916 = 112.0F;
   private static final float currentValue34 = 0.35F;
   private static final float currentValue516 = 0.6F;
   private static final float currentValue827 = 300.0F;
   private static final float currentValue1126 = 0.58F;
   private static final float currentValue362 = 0.82F;
   private static final float currentValue812 = 55.0F;
   private static final float currentValue298 = 0.48F;
   private static final float currentValue147 = 0.35F;
   private static final float currentValue156 = 238.0F;
   private static final float currentValue201 = 0.2F;
   private static final float currentValue733 = 1.05F;
   private static final float currentValue139 = 150.0F;
   private static final float currentValue107 = 0.68F;
   private static final float currentValue456 = 0.58F;
   private static final float currentValue594 = 342.0F;
   private static final float currentValue136 = 0.88F;
   private static final float currentValue1004 = 0.9F;
   private static final float currentValue651 = 88.0F;
   private static final float currentValue535 = 0.55F;
   private static final float currentValue648 = 0.48F;
   private static final float currentValue777 = 275.0F;
   private static final float currentValue606 = 0.4F;
   private static final float currentValue692 = 1.12F;
   private static final float currentValue521 = 24.0F;
   private static final float currentValue255 = 0.72F;
   private static final float currentValue1183 = 0.68F;
   private static final float currentValue902 = 190.0F;
   private static final float currentValue1040 = 0.3F;
   private static final float currentValue1006 = 0.38F;
   private static final float currentValue168 = 128.0F;
   private static final float currentValue235 = 0.62F;
   private static final float currentValue987 = 0.94F;
   private static final float currentValue1068 = 318.0F;
   private static final float currentValue863 = 0.83F;
   private static final float currentValue856 = 0.55F;
   private static final float currentValue188 = 63.0F;
   private static final float currentValue665 = 0.12F;
   private static final float currentValue1080 = 0.76F;
   private static final float currentValue667 = 224.0F;
   private static final float currentValue781 = 0.5F;
   private static final float currentValue730 = 1.08F;
   private static final float currentValue457 = 165.0F;
   private static final float currentValue678 = 0.92F;
   private static final float currentValue943 = 0.45F;
   private static final float currentValue1167 = 355.0F;
   private static final float currentValue716 = 0.26F;
   private static final float currentValue364 = 0.87F;
   private static final float currentValue674 = 98.0F;
   private static final float currentValue935 = 0.66F;
   private static final float currentValue142 = 0.62F;
   private static final float currentValue467 = 260.0F;
   private static final float currentValue482 = 0.44F;
   private static final float currentValue922 = 38.0F;
   private static final float currentValue758 = 0.76F;
   private static final float currentValue513 = 0.52F;
   private static final float currentValue163 = 208.0F;
   private static final float currentValue55 = 0.57F;
   private static final float currentValue880 = 0.72F;
   private static final float currentValue912 = 330.0F;
   private static final float currentValue1139 = 0.14F;
   private static final float currentValue972 = 0.58F;
   private static final float currentValue955 = 82.0F;
   private static final float currentValue61 = 0.32F;
   private static final float currentValue109 = 0.5F;
   private static final float currentValue587 = 176.0F;
   private static final float currentValue159 = 0.5F;
   private static final float currentValue981 = 0.68F;
   private static final float currentValue432 = 286.0F;
   private static final float currentValue711 = 0.7F;
   private static final float currentValue688 = 0.56F;
   private static final float currentValue687 = 44.0F;
   private static final float currentValue832 = 0.88F;
   private static final float currentValue120 = 0.74F;
   private static final float currentValue291 = 0.08F;
   private static final float currentValue80 = 0.04F;
   private static final double currentValue333 = Math.PI / 2;
   private static final double currentValue311 = Math.PI / 2;
   private static final double currentValue63 = Math.PI / 2;
   private static final double currentValue1022 = Math.PI / 2;
   private static final float currentValue462 = 0.22F;
   private static final float currentValue1109 = 1.08F;
   private static final double currentValue438 = 0.18;
   private static final double currentValue894 = 0.82;
   private static final double currentValue134 = 0.34;
   private static final double currentValue272 = 0.155;
   private static final double currentValue709 = 0.031;
   private static final double currentValue1017 = 0.2;
   private static final double currentValue630 = 0.8;
   private static final double currentValue801 = 0.58;
   private static final double currentValue1158 = 0.58;
   private static final int stateCounter7 = -65536;
   private static final float currentValue1030 = 0.76F;
   private static final float currentValue788 = 0.24F;
   private static final float currentValue499 = 0.35F;
   private static final double currentValue864 = 3.0;
   private static final double currentValue506 = 0.19;
   private static final double currentValue194 = 0.02;
   private static final double currentValue343 = 0.014;
   private static final double currentValue346 = 0.3;
   private static final double currentValue905 = 0.12;
   private static final double currentValue376 = 0.05;
   private static final double currentValue450 = 2.45;
   private static final double currentValue664 = 0.05;
   private static final double currentValue885 = 0.04;
   private static final double currentValue15 = 0.045;
   private static final double currentValue75 = 0.018;
   private static final double currentValue315 = 0.96;
   private static final double currentValue1185 = 0.04;
   private static final double currentValue86 = 0.065;
   private static final double currentValue677 = 0.018;
   private static final double currentValue766 = Math.PI;
   private static final double currentValue913 = 2.15;
   private static final double currentValue1135 = 0.9;
   private static final double currentValue233 = Math.PI * 2;
   private static final double currentValue837 = Math.PI * 2;
   private static final double currentValue983 = -Math.PI / 2;
   private static final double currentValue1085 = Math.PI;
   private static final double currentValue441 = -Math.PI / 2;
   private static final double currentValue1168 = Math.PI;
   private static final double currentValue95 = Math.PI * 2;
   private static final double currentValue1058 = Math.PI * 2;
   private static final String string4 = "target-esp-cube-core";
   private static final float currentValue146 = 1.4F;
   private static final long lastUpdateMillis36 = 100000L;
   private static final float currentValue264 = 1.5F;
   private static final float currentValue173 = 360.0F;
   private static final float currentValue712 = 80.0F;
   private static final float currentValue1142 = 0.08F;
   private static final float currentValue1171 = 0.002F;
   private static final float currentValue543 = 0.3F;
   private static final float currentValue491 = 0.007F;
   private static final float currentValue761 = 0.5F;
   private static final float currentValue221 = 1.2F;
   private static final float currentValue9 = 0.5F;
   private static final float currentValue1027 = 0.15F;
   private static final float currentValue1106 = -0.15F;
   private static final float currentValue647 = 5.0F;
   private static final float currentValue617 = 0.62F;
   private static final float currentValue313 = 6.0F;
   private static final float currentValue693 = 0.05F;
   private static final double currentValue991 = 2.0;
   private static final float currentValue303 = 20.0F;
   private static final float currentValue1170 = 0.7F;
   private static final double currentValue779 = 0.15;
   private static final double currentValue206 = 10.0;
   private static final double currentValue898 = 25.0;
   private static final double currentValue507 = 0.285;
   private static final double currentValue454 = -1.0;
   private static final double currentValue889 = -1.0;
   private static final double currentValue959 = -1.0;
   private static final double currentValue1118 = -1.0;
   private static final double currentValue952 = -1.0;
   private static final int stateCounter47 = Integer.MIN_VALUE;
   private static final float currentValue341 = 0.22F;
   private static final float currentValue296 = 0.22F;
   private static final float currentValue231 = 0.22F;
   private static final float currentValue299 = 0.78F;
   private static final float currentValue759 = 0.01F;
   private static final float currentValue531 = 0.12F;
   private static final double currentValue508 = 9.0;
   private static final double currentValue1132 = 0.5;
   private static final float currentValue1081 = 0.65F;
   private static final float currentValue225 = 0.45F;
   private static final double currentValue382 = 0.7;
   private static final double currentValue1103 = 1.15;
   private static final float currentValue112 = 0.18F;
   private static final float currentValue119 = 0.001F;
   private static final float currentValue1177 = 0.8F;
   private static final float currentValue866 = 0.5F;
   private static final double currentValue1052 = 0.1;
   private static final double currentValue1049 = 1.7;
   private static final float currentValue280 = 0.7F;
   private static final float currentValue218 = 0.25F;
   private static final float currentValue41 = 20.0F;
   private static final double currentValue18 = 0.0035;
   private static final float currentValue308 = 4.0F;
   private static final float currentValue367 = 0.12F;
   private static final float currentValue775 = 0.21F;
   private static final float currentValue104 = 0.01F;
   private static final double currentValue70 = 3.1;
   private static final float currentValue645 = 0.4F;
   private static final float currentValue585 = 0.12F;
   private static final float currentValue19 = 0.24F;
   private static final double currentValue602 = 0.55;
   private static final double currentValue830 = Math.PI * 2;
   private static final double currentValue282 = 26.0;
   private static final double currentValue131 = 0.15;
   private static final double currentValue203 = 3.0;
   private static final float currentValue290 = 6.0F;
   private static final double currentValue1141 = 0.35;
   private static final float currentValue50 = 1.5F;
   private static final long lastUpdateMillis21 = 900L;
   private static final float currentValue342 = 900.0F;
   private static final float currentValue994 = 1.25F;
   private static final float currentValue669 = 1.05F;
   private static final float currentValue84 = 0.4F;
   private static final long lastUpdateMillis39 = 25L;
   private static final double currentValue128 = 0.15;
   private static final double currentValue901 = 25.0;
   private static final double currentValue954 = -1.0;
   private static final float currentValue977 = 0.2F;
   private static final double currentValue696 = -1.0;
   private static final double currentValue497 = -1.0;
   private static final float currentValue396 = 0.2F;
   private static final double currentValue1050 = -1.0;
   private static final double currentValue747 = -1.0;
   private static final float currentValue638 = 0.2F;
   private static final double currentValue700 = 0.14;
   private static final double currentValue962 = 1.0E-5;
   private static final double currentValue555 = 5.0;
   private static final double currentValue252 = 0.5;
   private static final double currentValue833 = 1.5;
   private static final double currentValue334 = 0.1;
   private static final double currentValue475 = 0.65;
   private static final double currentValue848 = 0.1;
   private static final double currentValue842 = 0.65;
   private static final double currentValue946 = 0.1;
   private static final double currentValue1008 = 0.65;
   private static final float currentValue54 = 2.1F;
   private static final float currentValue817 = 0.7F;
   private static final float currentValue1113 = 0.24F;
   private static final float currentValue500 = 0.14F;
   private static final float currentValue928 = 2.1F;
   private static final float currentValue350 = 0.48F;
   private static final double currentValue887 = 0.7;
   private static final double currentValue33 = 0.7;
   private static final double currentValue453 = -1.0;
   private static final double currentValue721 = -1.0;
   private static final double currentValue359 = -1.0;
   private static final float currentValue404 = 2.70158F;
   private static final float currentValue690 = 1.70158F;
   private static final long lastUpdateMillis22 = 200L;
   private static final float currentValue366 = 0.01F;
   private static final float currentValue511 = 0.6F;
   private static final float currentValue636 = 0.5F;
   private static final float currentValue169 = 0.2F;
   private static final float currentValue966 = 0.38F;
   private static final float currentValue524 = 0.45F;
   private static final float currentValue29 = 0.85F;
   private static final float currentValue126 = 0.02F;
   private static final long lastUpdateMillis40 = 90L;
   private static final float currentValue751 = 0.02F;
   private static final float currentValue400 = 0.38F;
   private static final float currentValue1037 = 0.14F;
   private static final float currentValue165 = 0.2F;
   private static final float currentValue1129 = 0.22F;
   private static final float currentValue1134 = 0.02F;
   private static final float currentValue865 = 0.14F;
   private static final float currentValue393 = 2.2F;
   private static final float currentValue295 = 0.55F;
   private static final float currentValue904 = 1.35F;
   private static final float currentValue1003 = 0.75F;
   private static final float currentValue722 = 0.02F;
   private static final float currentValue137 = 0.14F;
   private static final float currentValue948 = 0.45F;
   private static final float currentValue1083 = 0.6F;
   private static final float currentValue1163 = 0.25F;
   private static final float currentValue190 = 0.45F;
   private static final float currentValue210 = 0.12F;
   private static final float currentValue439 = 0.16F;
   private static final float currentValue360 = 0.5F;
   private static final float currentValue915 = 0.5F;
   private static final float currentValue422 = 1.05F;
   private static final float currentValue103 = 0.55F;
   private static final float currentValue735 = 0.82F;
   private static final float currentValue658 = 0.1F;
   private static final float currentValue216 = 0.12F;
   private static final float currentValue958 = 0.55F;
   private static final float currentValue6 = 0.25F;
   private static final float currentValue815 = 1.25F;
   private static final float currentValue354 = 0.55F;
   private static final float currentValue792 = 0.9F;
   private static final float currentValue823 = 0.1F;
   private static final float currentValue967 = 0.25F;
   private static final float currentValue324 = 0.2F;
   private static final float currentValue398 = 1.25F;
   private static final float currentValue1161 = 0.55F;
   private static final double currentValue209 = Math.PI;
   private static final double currentValue969 = 2.0;
   private static final float currentValue79 = 1.2F;
   private static final float currentValue800 = 0.35F;
   private static final float currentValue121 = 0.03F;
   private static final float currentValue515 = 100.0F;
   private static final float currentValue1145 = 100.0F;
   private static final float currentValue1042 = 0.12F;
   private static final float currentValue302 = 0.78F;
   private static final float currentValue372 = 0.22F;
   private static final float currentValue381 = 0.92F;
   private static final float currentValue789 = 0.08F;
   private static final long lastUpdateMillis50 = 90L;
   private static final float currentValue1082 = 19.37F;
   private static final float currentValue1007 = 0.5F;
   private static final float currentValue369 = 0.35F;
   private static final float currentValue1061 = 0.5F;
   private static final float currentValue566 = 0.1F;
   private static final float currentValue249 = 0.5F;
   private static final float currentValue199 = 0.07F;
   private static final double currentValue539 = Math.PI;
   private static final double currentValue944 = 2.0;
   private static final float currentValue10 = 0.92F;
   private static final float currentValue910 = 1.1F;
   private static final double currentValue111 = 0.14;
   private static final long lastUpdateMillis48 = 31L;
   private static final long lastUpdateMillis15 = 97L;
   private static final double currentValue260 = 1.0E-6;
   private static final double currentValue37 = 0.55;
   private static final float currentValue329 = 27.0F;
   private static final double currentValue719 = 0.015;
   private static final double currentValue97 = 1.0E-8;
   private static final double currentValue576 = 0.85;
   private static final double currentValue30 = 1.0E-8;
   private static final double currentValue682 = 0.5;
   private static final double currentValue220 = 2.0;
   private static final double currentValue512 = 0.5;
   private static final double currentValue816 = 2.0;
   private static final double currentValue485 = 0.35;
   private static final double currentValue868 = 0.5;
   private static final double currentValue355 = 0.5;
   private static final double currentValue847 = 0.5;
   private static final float currentValue783 = 1.0E-4F;
   private static final double currentValue565 = 1.0E-10;
   private static final double currentValue242 = 0.5;
   private static final double currentValue205 = 1.0E-8;
   private static final double currentValue380 = 1.0E-8;
   private static final double currentValue978 = 1.0E-8;
   private static final double currentValue803 = -1.0;
   private static final double currentValue411 = 1.0E-8;
   private static final float currentValue1169 = 0.5F;
   private static final float currentValue8 = 0.5F;
   private static final float currentValue591 = 0.5F;
   private static final float currentValue102 = 0.5F;
   private static final float currentValue100 = 1.0E-4F;
   private static final float currentValue523 = 0.001F;
   private static final double currentValue906 = 1.0E-9;
   private static final double currentValue646 = 0.9;
   private static final double currentValue213 = 1.0E-9;
   private static final double currentValue273 = -1.0;
   private static final double currentValue316 = 1.0E-9;
   private static final float currentValue = 4.0F;
   private static final float currentValue58 = 0.25F;
   private static final float currentValue489 = 0.001F;
   private static final float currentValue1053 = 2.4F;
   private static final float currentValue73 = 2.4F;
   private static final double currentValue622 = Math.PI * 2;
   private static final double currentValue1002 = 6.0;
   private static final double currentValue640 = Math.PI * 2;
   private static final double currentValue984 = 6.0;
   private static final float currentValue774 = 6.0F;
   private static final float currentValue39 = 6.0F;
   private static final float currentValue1060 = 0.72F;
   private static final float currentValue679 = 0.28F;
   private static final double currentValue305 = Math.PI;
   private static final double currentValue409 = 0.75;
   private static final float currentValue612 = 6.0F;
   private static final float currentValue1108 = 15.0F;
   private static final float currentValue526 = 10.0F;
   private static final float currentValue379 = 0.5F;
   private static final float currentValue782 = 0.5F;
   private static final int stateCounter41 = 374761393;
   private static final int stateCounter30 = 668265263;
   private static final int stateCounter16 = 1274126177;
   private static final float currentValue705 = 0.5F;
   private static final float currentValue427 = 2.03F;
   private static final float currentValue116 = 0.5F;
   private static final double currentValue641 = 1.0E-9;
   private static final double currentValue1093 = 0.75;
   private static final long lastUpdateMillis8 = 60L;
   private static final float currentValue390 = 0.01F;
   private static final float currentValue177 = 0.4F;
   private static final float currentValue399 = 0.9F;
   private static final double currentValue1105 = Math.PI;
   private static final float currentValue175 = 0.02F;
   private static final double currentValue527 = 1.0E-9;
   private static final float currentValue461 = 0.3F;
   private static final float currentValue911 = 0.34F;
   private static final float currentValue584 = 0.18F;
   private static final float currentValue1079 = 0.18F;
   private static final float currentValue356 = 0.3F;
   private static final float currentValue480 = 0.09F;
   private static final float currentValue689 = 0.42F;
   private static final float currentValue13 = 0.05F;
   private static final float currentValue207 = 0.6F;
   private static final float currentValue240 = 0.028F;
   private static final float currentValue533 = 0.85F;
   private static final float currentValue35 = 0.014F;
   private static final float currentValue961 = 0.006F;
   private static final double currentValue795 = 0.6;
   private static final double currentValue87 = 0.5;
   private static final double currentValue167 = 0.2;
   private static final double currentValue796 = 0.22;
   private static final double currentValue365 = 0.18;
   private static final float currentValue253 = 1.6F;
   private static final float currentValue436 = 1.2F;
   private static final float currentValue196 = 0.85F;
   private static final float currentValue723 = 0.9F;
   private static final float currentValue656 = (float) Math.PI;
   private static final float currentValue261 = 0.82F;
   private static final float currentValue1186 = 0.1F;
   private static final float currentValue990 = 0.18F;
   private static final double currentValue1076 = 0.06;
   private static final double currentValue501 = 0.4;
   private static final float currentValue571 = 0.55F;
   private static final float currentValue810 = 0.35F;
   private static final float currentValue155 = 100.0F;
   private static final float currentValue909 = 0.85F;
   private static final float currentValue481 = 0.55F;
   private static final double currentValue628 = Math.PI;
   private static final double currentValue708 = 2.0;
   private static final double currentValue540 = 0.5;
   private static final double currentValue1130 = 0.35;
   private static final double currentValue1107 = 0.92;
   private static final double currentValue697 = 0.16;
   private static final double currentValue1097 = 0.06;
   private static final double currentValue1195 = 0.88;
   private static final double currentValue765 = 0.5;
   private static final double currentValue254 = 0.05;
   private static final double currentValue547 = 0.5;
   private static final double currentValue846 = 0.5;
   private static final double currentValue1127 = 2.0;
   private static final double currentValue226 = 0.5;
   private static final double currentValue618 = 0.5;
   private static final double currentValue1009 = 0.9;
   private static final double currentValue265 = Math.PI;
   private static final double currentValue870 = 2.0;
   private static final long lastUpdateMillis10 = 80L;
   private static final float currentValue405 = 0.4F;
   private static final long lastUpdateMillis12 = 30L;
   private static final float currentValue657 = 0.35F;
   private static final float currentValue950 = 0.85F;
   private static final float currentValue975 = 0.2F;
   private static final float currentValue1098 = 0.4F;
   private static final float currentValue428 = 0.6F;
   private static final float currentValue94 = 0.07F;
   private static final float currentValue306 = 0.8F;
   private static final float currentValue133 = 0.2F;
   private static final double currentValue752 = 0.05;
   private static final float currentValue936 = 0.015F;
   private static final float currentValue937 = 0.001F;
   private static final float currentValue391 = 0.001F;
   private static final float currentValue179 = 0.5F;
   private static final float currentValue238 = 0.5F;
   private static final float currentValue808 = 0.5F;
   private static final float currentValue570 = 0.5F;
   private static final float currentValue96 = 0.045F;
   private static final float currentValue464 = 0.6F;
   private static final float currentValue633 = 0.5F;
   private static final float currentValue520 = 0.024F;
   private static final float currentValue784 = 11.0F;
   private static final float currentValue1039 = 0.012F;
   private static final float currentValue215 = 0.55F;
   private static final float currentValue494 = 0.45F;
   private static final float currentValue809 = 23.0F;
   private static final double currentValue251 = 0.55;
   private static final double currentValue861 = 0.5;
   private static final double currentValue872 = 0.18;
   private static final double currentValue794 = Math.PI;
   private static final double currentValue1021 = 2.0;
   private static final double currentValue212 = 0.2;
   private static final double currentValue1026 = 0.55;
   private static final double currentValue419 = 0.2;
   private static final double currentValue1111 = 0.55;
   private static final double currentValue785 = 0.5;
   private static final double currentValue807 = 1.25;
   private static final double currentValue114 = 0.9;
   private static final double currentValue552 = 1.6;
   private static final double currentValue433 = 2.4;
   private static final double currentValue152 = 2.6;
   private static final double currentValue843 = 0.55;
   private static final double currentValue310 = 0.35;
   private static final double currentValue43 = 1.0E-6;
   private static final double currentValue556 = -1.0;
   private static final double currentValue337 = 1.0E-8;
   private static final double currentValue76 = -1.0;
   private static final double currentValue608 = 0.5;
   private static final double currentValue385 = 0.35;
   private static final double currentValue882 = 0.55;
   private static final double currentValue653 = 0.15;
   private static final double currentValue269 = 0.4;
   private static final float currentValue248 = 0.5F;
   private static final float currentValue62 = 0.05F;
   private static final float currentValue625 = 0.5F;
   private static final double currentValue287 = 0.22;
   private static final double currentValue899 = 0.18;
   private static final float currentValue884 = 0.45F;
   private static final float currentValue811 = 0.3F;
   private static final float currentValue731 = 0.5F;
   private static final double currentValue1013 = Math.PI;
   private static final double currentValue1143 = 0.4;
   private static final double currentValue860 = 0.35;
   private static final double currentValue583 = 0.3;
   private static final double currentValue968 = 0.4;
   private static final double currentValue52 = 0.16;
   private static final double currentValue1010 = 0.12;
   private static final float currentValue953 = 0.35F;
   private static final float currentValue1154 = 0.3F;
   private static final float currentValue294 = 100.0F;
   private static final double currentValue397 = -1.0;
   private static final float currentValue60 = 0.4F;
   private static final float currentValue1074 = 0.6F;
   private static final double currentValue1174 = 0.5;
   private static final double currentValue219 = 1.0E-8;
   private static final double currentValue1110 = 1.0E-8;
   private static final double currentValue32 = 1.0E-8;
   private static final double currentValue56 = 1.0E-8;
   private static final double currentValue236 = 0.5;
   private static final double currentValue895 = 2.0;
   private static final double currentValue741 = 0.5;
   private static final double currentValue323 = 0.55;
   private static final double currentValue1018 = 0.52;
   private static final double currentValue1146 = 0.52;
   private static final float currentValue108 = 1.0E-4F;
   private static final float currentValue1190 = 0.75F;
   private static final float currentValue465 = 0.28F;
   private static final float currentValue437 = 0.01F;
   private static final double currentValue932 = 1.0E-8;
   private static final double currentValue375 = 0.88;
   private static final double currentValue970 = 1.0E-8;
   private static final double currentValue603 = 0.45;
   private static final double currentValue993 = 0.45;
   private static final float currentValue1091 = 0.5F;
   private static final float currentValue745 = 1.18F;
   private static final float currentValue834 = 0.52F;
   private static final float currentValue449 = 1.18F;
   private static final float currentValue448 = 0.72F;
   private static final float currentValue871 = 0.12F;
   private static final float currentValue989 = 0.88F;
   private static final float currentValue1173 = 0.12F;
   private static final double currentValue557 = Math.PI * 2;
   private static final double currentValue927 = Math.PI * 2;
   private static final double currentValue1043 = 0.5;
   private static final double currentValue706 = 2.0;
   private static final double currentValue275 = 2.0;
   private static final double currentValue1099 = 5.0;
   private static final double currentValue243 = 4.0;
   private static final double currentValue1175 = 3.0;
   private static final double currentValue197 = 3.0;
   private static final double currentValue627 = 0.5;
   private static final double currentValue406 = 2.0;
   private static final double currentValue611 = 2.0;
   private static final double currentValue787 = 5.0;
   private static final double currentValue550 = 4.0;
   private static final double currentValue1014 = 3.0;
   private static final double currentValue661 = 3.0;
   private static final double currentValue307 = 0.5;
   private static final double currentValue896 = 2.0;
   private static final double currentValue725 = 2.0;
   private static final double currentValue279 = 5.0;
   private static final double currentValue593 = 4.0;
   private static final double currentValue1114 = 3.0;
   private static final double currentValue1015 = 3.0;
   private static final float currentValue26 = 4.0F;
   private static final float currentValue336 = 1.25F;
   private static final String string110 = "InSampler";
   private static final String string92 = "target-esp-lightning-glow-mask";
   private static final double currentValue1020 = 12.9898;
   private static final double currentValue542 = 78.233;
   private static final float currentValue431 = 43758.547F;
   private static final float currentValue496 = 0.001F;
   private static final double currentValue728 = 0.58;
   private static final float currentValue64 = 0.95F;
   private static final float currentValue45 = 0.05F;
   private static final float currentValue828 = 2.6667F;
   private static final float currentValue1086 = 58.0F;
   private static final float currentValue737 = 58.0F;
   private static final float currentValue534 = 16.0F;
   private static final float currentValue771 = 5.5F;
   private static final float currentValue650 = -4.2F;
   private static final float currentValue1095 = 7.4F;
   private static final float currentValue923 = 130.0F;
   private static final float currentValue387 = 250.0F;
   private static final float currentValue635 = 0.9F;
   private static final float currentValue274 = 0.35F;
   private static final float currentValue514 = 0.16F;
   private static final float currentValue1121 = 0.4F;
   private static final float currentValue822 = 0.6F;
   private static final float currentValue1148 = 0.34F;
   private static final float currentValue1077 = 6.0F;
   private static final float currentValue105 = 0.1F;
   private static final float currentValue996 = 0.08F;
   private static final float currentValue631 = 0.82F;
   private static final float currentValue652 = -7.0F;
   private static final float currentValue259 = 7.0F;
   private static final float currentValue140 = 0.095F;
   private static final float currentValue586 = 0.88F;
   private static final float currentValue263 = 0.2F;
   private static final float currentValue402 = 0.5F;
   private static final float currentValue193 = 0.32F;
   private static final float currentValue394 = 0.0628F;
   private static final double currentValue1024 = 0.98;
   private static final String string41 = "Тип 2";
   private static final float currentValue276 = 1.5F;
   private static final float currentValue1028 = 1.2F;
   private static final double currentValue143 = 1.667;
   private static final float currentValue1159 = 180.0F;
   private static final float currentValue956 = 255.0F;
   private static final double currentValue191 = 0.5;
   private static final float currentValue530 = 3.0F;
   private static final float currentValue271 = 0.08F;
   private static final float currentValue886 = 0.04F;
   private static final float currentValue277 = 0.42F;
   private static final float currentValue418 = 0.32F;
   private static final float currentValue338 = 0.001F;
   private static final double currentValue417 = 0.85F;
   private static final float currentValue424 = 0.3F;
   private static final float currentValue921 = 0.1764706F;
   private static final float currentValue536 = 2.1F;
   private static final float currentValue176 = -0.95F;
   private static final float currentValue423 = 0.6F;
   private static final float currentValue1070 = 0.95F;
   private static final float currentValue74 = 0.6F;
   private static final float currentValue763 = -1.05F;
   private static final float currentValue21 = 5.0F;
   private static final float currentValue616 = 0.85F;
   private static final float currentValue414 = 3.0F;
   private static final float currentValue1194 = 3.0F;
   private static final float currentValue643 = 0.5F;
   private static final float currentValue430 = 0.7F;
   private static final float currentValue762 = 3.0F;
   private static final float currentValue224 = 0.08F;
   private static final float currentValue483 = 3.0F;
   private static final float currentValue538 = 3.0F;
   private static final float currentValue720 = 0.9F;
   private static final float currentValue1136 = 80.0F;
   private static final float currentValue877 = 175.0F;
   private static final float currentValue917 = 255.0F;
   private static final float currentValue314 = 0.95F;
   private static final float currentValue195 = 1.9F;
   private static final float currentValue1152 = 0.002F;
   private static final float currentValue327 = 0.35F;
   private static final float currentValue11 = 2.2F;
   private static final float currentValue601 = 0.45F;
   private static final float currentValue90 = 1.3F;
   private static final float currentValue469 = 3.0F;
   private static final double currentValue340 = Math.PI * 2;
   private static final double currentValue988 = Math.PI;
   private static final float currentValue484 = 0.15F;
   private static final float currentValue718 = 0.5F;
   private static final float currentValue1166 = 0.5F;
   private static final double currentValue154 = Math.PI;
   private static final double currentValue78 = 3.0;
   private static final float currentValue317 = 0.6F;
   private static final float currentValue46 = 1.65F;
   private static final float currentValue407 = 0.55F;
   private static final float currentValue452 = 0.5F;
   private static final float currentValue563 = 0.16F;
   private static final float currentValue992 = 12.9898F;
   private static final float currentValue588 = 43758.547F;
   private static final float currentValue83 = 0.01F;
   private static final float currentValue153 = 0.52F;
   private static final float currentValue727 = 0.18F;
   private static final float currentValue579 = 0.04F;
   private static final double currentValue736 = 3.1;
   private static final float currentValue1084 = 0.68F;
   private static final float currentValue1090 = 1.14F;
   private static final float currentValue476 = 0.15F;
   private static final double currentValue644 = 0.55;
   private static final double currentValue170 = 0.035;
   private static final float currentValue1035 = 1.32F;
   private static final float currentValue214 = 0.18F;
   private static final float currentValue1153 = 0.4F;
   private static final double currentValue655 = 1.28;
   private static final double currentValue92 = 0.17;
   private static final double currentValue748 = 34.0;
   private static final double currentValue891 = 9.0;
   private static final double currentValue129 = 0.7;
   private static final double currentValue1138 = 0.82;
   private static final double currentValue144 = 0.48;
   private static final float currentValue754 = 0.88F;
   private static final float currentValue1125 = 0.12F;
   private static final float currentValue1046 = 0.85365856F;
   private static final float currentValue187 = 0.55F;
   private static final float currentValue529 = 0.22F;
   private static final float currentValue1102 = 0.42F;
   private static final float currentValue164 = 0.16F;
   private static final float currentValue234 = 0.14F;
   private static final float currentValue1001 = 0.22F;
   private static final float currentValue42 = 0.78F;
   private static final float currentValue1019 = 0.28F;
   private static final float currentValue1189 = 0.2F;
   private static final float currentValue713 = 0.72F;
   private static final float currentValue444 = 0.28F;
   private static final double currentValue1062 = 4.2;
   private static final double currentValue933 = 1.256637;
   private static final float currentValue183 = 0.11F;
   private static final float currentValue1119 = 0.88F;
   private static final float currentValue1104 = 0.9F;
   private static final float currentValue301 = 0.2F;
   private static final float currentValue357 = 0.5F;
   private static final float currentValue200 = 0.32F;
   private static final float currentValue510 = 0.42F;
   private static final float currentValue900 = 3.0F;
   private static final float currentValue202 = 5.0F;
   private static final float currentValue875 = 0.09F;
   private static final float currentValue415 = 0.015F;
   private static final float currentValue546 = 0.92F;
   private static final float currentValue135 = 0.7F;
   private static final float currentValue383 = 0.16F;
   private static final float currentValue293 = 2.1F;
   private static final float currentValue859 = 0.55F;
   private static final float currentValue649 = 0.28F;
   private static final float currentValue1089 = 0.06F;
   private static final double currentValue150 = 1.55;
   private static final double currentValue934 = Math.PI;
   private static final double currentValue429 = 2.0;
   private static final double currentValue486 = 5.0;
   private static final double currentValue328 = 1.04;
   private static final double currentValue624 = 1.04;
   private static final float currentValue995 = 0.07F;
   private static final float currentValue532 = 0.7F;
   private static final float currentValue474 = 0.48F;
   private static final float currentValue204 = 0.82F;
   private static final double currentValue907 = Math.PI / 2;
   private static final double currentValue257 = Math.PI;
   private static final double currentValue662 = 2.0;
   private static final double currentValue1033 = 5.0;
   private static final double currentValue553 = 1.0E-6;
   private static final float currentValue610 = 0.68F;
   private static final float currentValue670 = 0.72F;
   private static final float currentValue161 = 1.15F;
   private static final float currentValue12 = 0.16F;
   private static final double currentValue89 = 2.0;
   private static final double currentValue824 = 0.17;
   private static final double currentValue1120 = 22.0;
   private static final float currentValue22 = 0.46F;
   private static final float currentValue1055 = 2.72F;
   private static final float currentValue283 = 0.22F;
   private static final float currentValue1192 = 2.6667F;
   private static final float currentValue590 = 0.48F;
   private static final float currentValue931 = 0.94F;
   private static final double currentValue567 = 5.0;
   private static final float currentValue930 = 13.0F;
   private static final double currentValue151 = 0.075;
   private static final float currentValue180 = 0.42F;
   private static final float currentValue941 = 0.07F;
   private static final float currentValue1116 = 0.105F;
   private static final float currentValue561 = 0.72F;
   private static final float currentValue36 = 0.9F;
   private static final float currentValue879 = 0.5F;
   private static final float currentValue614 = 0.4F;
   private static final double currentValue115 = Math.PI * 2;
   private static final float currentValue609 = 360.0F;
   private static final float currentValue545 = 0.13F;
   private static final float currentValue44 = 0.52F;
   private static final float currentValue790 = 0.82F;
   private static final float currentValue676 = 0.17F;
   private static final float currentValue479 = 0.115F;
   private static final float currentValue1069 = 0.085F;
   private static final float currentValue600 = 0.65F;
   private static final float currentValue919 = 0.01F;
   private static final double currentValue744 = 1550.0;
   private static final double currentValue560 = Math.PI;
   private static final double currentValue769 = 2.0;
   private static final float currentValue503 = 10.0F;
   private static final float currentValue897 = 0.25F;
   private static final double currentValue825 = 0.5;
   private static final double currentValue592 = 0.5;
   private static final double currentValue673 = 0.2875;
   private static final float currentValue976 = 0.005F;
   private static final float currentValue857 = 0.008F;
   private static final float currentValue412 = 0.3F;
   private static final float currentValue685 = 0.75F;
   private static final float currentValue786 = 0.65F;
   private static final float currentValue1144 = 0.65F;
   private static final int stateCounter21 = -3329486;
   private static final double currentValue1124 = 5.0;
   private static final float currentValue211 = 5.0F;
   private static final float currentValue853 = 0.1F;
   private static final float currentValue965 = 0.8F;
   private static final double currentValue1044 = 0.5;
   private static final float currentValue1147 = 0.3F;
   private static final float currentValue770 = 0.2F;
   private static final float currentValue421 = 0.2F;
   private static final float currentValue517 = 0.8F;
   private static final float currentValue862 = 50.0F;
   private static final float currentValue347 = 0.005F;
   private static final float currentValue938 = 1800.0F;
   private static final long lastUpdateMillis37 = 1500L;
   private static final double currentValue1160 = 2.0;
   private static final double currentValue1151 = 2.0;
   private static final float currentValue232 = 0.5F;
   private static final double currentValue881 = -2.0;
   private static final double currentValue893 = 2.0;
   private static final double currentValue344 = 2.0;
   private static final double currentValue1064 = 2.0;
   private static final double currentValue239 = 2.0;
   private static final float currentValue1036 = 0.5F;
   private static final float currentValue798 = 2000000.0F;
   private static final float currentValue16 = 10.0F;
   private static final float currentValue574 = 2.5F;
   private static final float currentValue1056 = 0.7F;
   private static final float currentValue331 = 14.0F;
   private static final float currentValue71 = 0.5F;
   private static final float currentValue743 = 0.5F;
   private static final float currentValue28 = 0.5F;
   private static final float currentValue845 = 0.2F;
   private static final float currentValue942 = 3.5F;
   private static final float currentValue659 = 15.0F;
   private static final float currentValue160 = 0.5F;
   private static final double currentValue217 = Math.PI;
   private static final double currentValue999 = 2.0;
   private static final long lastUpdateMillis7 = 12345L;
   private static final double currentValue699 = 0.5;
   private static final double currentValue869 = 0.5;
   private static final double currentValue729 = 0.5;
   private static final float currentValue181 = 10.0F;
   private static final float currentValue838 = 0.15F;
   private static final float currentValue1156 = 0.5882353F;
   private static final double currentValue425 = 0.5;
   private static final float currentValue821 = 1.5F;
   private static final float currentValue403 = 1.5F;
   private static final float currentValue351 = 1.5F;
   private static final double currentValue410 = 4.0;
   private static final float currentValue805 = 8.0F;
   private static final float currentValue321 = 0.55F;
   private static final float currentValue345 = 0.2F;
   private static final float currentValue876 = 0.5F;
   private static final double currentValue717 = 125.0;
   private static final double currentValue1094 = 0.2;
   private static final double currentValue597 = 10.0;
   private static final double currentValue890 = 15.0;
   private static final double currentValue24 = 0.45;
   private static final double currentValue281 = 0.45;
   private static final float currentValue1016 = 0.045F;
   private static final double currentValue740 = 0.35F;
   private static final float currentValue639 = 0.045F;
   private static final double currentValue858 = 0.35F;
   private static final float currentValue49 = 0.045F;
   private static final float currentValue157 = 0.2F;
   private static final float currentValue764 = 0.35F;
   private static final float currentValue575 = 1.5F;
   private static final float currentValue818 = 2.25F;
   private static final float currentValue620 = 0.2F;
   private static final long lastUpdateMillis26 = 16L;
   private static final double currentValue1031 = 4.0;
   private static final double currentValue420 = 360.0;
   private static final float currentValue171 = 0.6F;
   private static final float currentValue1112 = 0.5F;
   private static final float currentValue145 = 0.4F;
   private static final double currentValue237 = 360.0;
   private static final double currentValue335 = 2.0;
   private static final float currentValue401 = 0.4F;
   private static final float currentValue332 = 100.0F;
   private static final float currentValue683 = 0.01F;
   private static final float currentValue270 = 400.0F;
   private static final float currentValue267 = 400.0F;
   private static final float currentValue172 = 800.0F;
   private static final float currentValue925 = 400.0F;
   private static final float currentValue903 = 0.01F;
   private static final float currentValue750 = 0.01F;
   private static final double currentValue1182 = 0.004;
   private static final double currentValue604 = Math.PI * 2;
   private static final double currentValue300 = 0.15;
   private static final double currentValue726 = -1.0;
   private static final double currentValue577 = 0.8;
   private static final double currentValue445 = 0.55;
   private static final double currentValue660 = 0.5;
   private static final double currentValue106 = 0.04;
   private static final float currentValue1128 = 0.34F;
   private static final float currentValue358 = 0.21F;
   private static final double currentValue361 = 1.8;
   private static final float currentValue581 = 0.2F;
   private static final double currentValue634 = Math.PI;
   private static final double currentValue760 = 10.0;
   private static final float currentValue339 = 0.1F;
   private static final double currentValue738 = Math.PI;
   private static final double currentValue929 = 2.0;
   private static final long lastUpdateMillis6 = 10L;
   private static final long lastUpdateMillis17 = 360L;
   private static final float currentValue118 = 0.75F;
   private static final float currentValue558 = 0.75F;
   private static final float currentValue778 = 0.75F;
   private static final double currentValue680 = 255.0;
   private static final float currentValue125 = 180.0F;
   private static final int stateCounter55 = -39836;
   private static final int stateCounter5 = 16777215;
   private static final double currentValue504 = 2000.0;
   private static final float currentValue525 = 360.0F;
   private static final float currentValue802 = 20.0F;
   private static final float currentValue753 = 0.3F;
   private static final double currentValue110 = 1.5;
   private static final double currentValue605 = 0.7F;
   private static final double currentValue31 = 1.5;
   private static final double currentValue495 = 0.7F;
   private static final double currentValue318 = 10430.378350470453;
   private static final int stateCounter27 = 65535;
   private static final double currentValue963 = Math.PI;
   private static final double currentValue746 = 2.0;
   private static final double currentValue288 = 65536.0;
   private static final float currentValue262 = 0.3F;
   private static final float currentValue663 = 0.35F;
   private static final float currentValue701 = 255.0F;
   private static final float currentValue1180 = 0.06F;
   private static final float currentValue780 = 0.2F;
   private static final float currentValue122 = 255.0F;
   private static final float currentValue702 = 0.06F;
   private static final float currentValue266 = 255.0F;
   private static final long lastUpdateMillis18 = 360000L;
   private static final float currentValue619 = 7.25F;
   private static final float currentValue1029 = 0.001F;
   private static final float currentValue470 = 1.5F;
   private static final float currentValue69 = 1.25F;
   private static final float currentValue793 = 0.5F;
   private static final double currentValue892 = 0.05;
   private static final float currentValue1123 = 0.15F;
   private static final float currentValue101 = 19.3F;
   private static final float currentValue1059 = 0.9625F;
   private static final float currentValue757 = 0.2F;
   private static final float currentValue458 = 0.4375F;
   private static final float currentValue844 = 0.22F;
   private static final double currentValue348 = 2.0;
   private static final float currentValue148 = 1.5F;
   private static final float currentValue1072 = 1.25F;
   private static final float currentValue666 = 0.5F;
   private static final long lastUpdateMillis16 = 360000L;
   private static final float currentValue117 = 2.5F;
   private static final float currentValue528 = 0.3F;
   private static final float currentValue186 = 0.1F;
   private static final float currentValue440 = 0.1F;
   private static final float currentValue568 = 0.3F;
   private static final float currentValue835 = 0.1F;
   private static final float currentValue326 = 1.5F;
   private static final float currentValue537 = 0.2F;
   private static final float currentValue541 = 0.6F;
   private static final float currentValue632 = 0.2F;
   private static final float currentValue854 = 1.5F;
   private static final long lastUpdateMillis25 = 133769420L;
   private static final float currentValue873 = 12.0F;
   private static final float currentValue99 = -1.0F;
   private static final float currentValue184 = 360.0F;
   private static final float currentValue192 = Float.MAX_VALUE;
   private static final double currentValue867 = 240.0;
   private static final float currentValue1000 = 260.0F;
   private static final double currentValue749 = 0.9;
   private static final float currentValue426 = 60.0F;
   private static final float currentValue67 = 0.05F;
   private static final int stateCounter50 = -52686;
   private static final float currentValue363 = 0.5F;
   private static final float currentValue621 = 0.5F;
   private static final float currentValue23 = 0.5F;
   private static final float currentValue447 = 0.5F;
   private static final float currentValue463 = 1.0E-8F;
   private static final float currentValue1063 = -1.0F;
   private static final float currentValue198 = 1.3F;
   private static final float currentValue773 = 1.3F;
   private static final float currentValue1191 = 1.3F;
   private static final float currentValue223 = 0.92F;
   private static final float currentValue1066 = 1.7F;
   private static final float currentValue492 = 0.16F;
   private static final float currentValue1078 = 0.54F;
   private static final float currentValue672 = 0.76F;
   private static final float currentValue695 = 0.92F;
   private static final float currentValue1038 = 4.3F;
   private static final float currentValue945 = 0.16F;
   private static final float currentValue714 = 1.12F;
   private static final float currentValue286 = 0.92F;
   private static final float currentValue1155 = 8.1F;
   private static final float currentValue2 = 0.12F;
   private static final float currentValue668 = 12.6F;
   private static final float currentValue1088 = 0.5F;
   private static final float currentValue756 = 0.07F;
   private static final float currentValue947 = 17.2F;
   private static final float currentValue413 = 0.5F;
   private static final float currentValue38 = 0.07F;
   private static final float currentValue562 = 0.58F;
   private static final float currentValue569 = 0.42F;
   private static final float currentValue715 = 0.18F;
   private static final float currentValue829 = 0.45F;
   private static final float currentValue1057 = 0.45F;
   private static final float currentValue473 = 0.45F;
   private static final float currentValue446 = 0.45F;
   private static final float currentValue258 = 0.45F;
   private static final float currentValue964 = 0.45F;
   private static final float currentValue1122 = 0.45F;
   private static final float currentValue1157 = 0.45F;
   private static final float currentValue1032 = 0.61F;
   private static final float currentValue564 = 23.0F;
   private static final float currentValue878 = 1.27F;
   private static final float currentValue304 = 51.0F;
   private static final float currentValue285 = 17.0F;
   private static final float currentValue982 = 1.3F;
   private static final float currentValue1087 = 1.3F;
   private static final float currentValue755 = 1.3F;
   private static final float currentValue926 = 0.62F;
   private static final float currentValue472 = 0.92F;
   private static final float currentValue703 = 1.7F;
   private static final float currentValue960 = 0.16F;
   private static final float currentValue973 = 0.76F;
   private static final float currentValue544 = 0.92F;
   private static final float currentValue1179 = 4.3F;
   private static final float currentValue53 = 0.16F;
   private static final float currentValue957 = 0.76F;
   private static final float currentValue1131 = 0.92F;
   private static final float currentValue320 = 8.1F;
   private static final float currentValue174 = 0.16F;
   private static final float currentValue3 = 12.6F;
   private static final float currentValue490 = 0.5F;
   private static final float currentValue582 = 0.08F;
   private static final float currentValue185 = 17.2F;
   private static final float currentValue1117 = 0.5F;
   private static final float currentValue47 = 0.08F;
   private static final float currentValue1041 = 0.58F;
   private static final float currentValue951 = 0.6F;
   private static final float currentValue518 = 0.6F;
   private static final float currentValue309 = 0.6F;
   private static final float currentValue451 = 0.6F;
   private static final float currentValue551 = 0.6F;
   private static final float currentValue1011 = 0.6F;
   private static final float currentValue797 = 0.6F;
   private static final float currentValue698 = 0.6F;
   private static final float currentValue241 = 78.233F;
   private static final float currentValue623 = 43758.547F;
   private static final float currentValue247 = 0.5F;
   private static final float currentValue589 = 1.0E-8F;
   private static final float currentValue1164 = -1.0F;
   private static final float currentValue1181 = 1.34F;
   private static final float currentValue840 = 1.34F;
   private static final float currentValue189 = 0.84F;
   private static final float currentValue998 = 0.36F;
   private static final float currentValue353 = 1.12F;
   private static final float currentValue349 = 0.5F;
   private static final float currentValue675 = 0.5F;
   private static final float currentValue368 = 0.5F;
   private static final float currentValue182 = 0.5F;
   private static final float currentValue888 = 0.5F;
   private static final float currentValue66 = 0.5F;
   private static final float currentValue1054 = 0.5F;
   private static final float currentValue850 = 0.5F;
   private static final float currentValue1188 = 0.5F;
   private static final float currentValue1176 = 0.5F;
   private static final float currentValue855 = 0.5F;
   private static final float currentValue230 = 0.5F;
   private static final float currentValue1067 = 0.5F;
   private static final float currentValue392 = 0.5F;
   private static final float currentValue477 = 0.5F;
   private static final float currentValue1140 = 0.5F;
   private static final float currentValue493 = 0.5F;
   private static final float currentValue1165 = 0.5F;
   private static final float currentValue416 = 0.5F;
   private static final float currentValue132 = 0.5F;
   private static final float currentValue971 = 0.5F;
   private static final float currentValue734 = 0.5F;
   private static final float currentValue72 = 255.0F;
   private static final float currentValue813 = 0.5F;
   private static final float currentValue509 = 255.0F;
   private static final float currentValue804 = 10.0F;
   private static final float currentValue459 = 360.0F;
   private static final float currentValue370 = 0.5F;
   private static final long lastUpdateMillis29 = 4L;
   private static final long lastUpdateMillis33 = 360L;
   private static final float currentValue814 = 180.0F;
   private static final float currentValue178 = 0.85F;
   private static final float currentValue229 = 3.0F;
   private static final String string114 = "Sampler0";
   private static final String string102 = "targetEspMask";
   private static final String string132 = "targetEspMaskVertices";
   private static final String string = "targetEspColor";
   private static final String string93 = "targetEspColorVertices";
   private static final String string10 = "targetEsp";
   private static final String string79 = "targetEspVertices";
   private static final int stateCounter25 = 16777215;
   private static final String string77 = "targetEspTriangleLines";
   private static final String string152 = "targetEspTriangleLines";
   private static final String string44 = "targetEspLightningCore";
   private static final String string46 = "targetEspCubeComposite";
   private static final String string100 = "dimasik";
   private static final String string122 = "icons/world_render/target.png";
   private static final String string135 = "dimasik";
   private static final String string73 = "icons/world_render/target1.png";
   private static final String string99 = "dimasik";
   private static final String string91 = "icons/world_render/firepart2.png";
   private static final String string59 = "dimasik";
   private static final String string145 = "textures/targetesp/zenith_ghost.png";
   private static final String string137 = "dimasik";
   private static final String string96 = "icons/world_render/bloom.png";
   private static final String string86 = "dimasik";
   private static final String string155 = "images/particles/bloom.png";
   private static final String string11 = "dimasik";
   private static final String string141 = "images/particles/bloom.png";
   private static final String string3 = "dimasik";
   private static final String string108 = "images/targetesp/glow.png";
   private static final String string34 = "dimasik";
   private static final String string148 = "textures/targetesp/lightning_arc.png";
   private static final String string19 = "dimasik/target_esp";
   private static final String string60 = "dead_chams";
   private static final String string35 = "dead_chams";
   private static final String string84 = "Sampler0";
   private static final String string163 = "dimasik/target_esp_ghost_no_depth";
   private static final String string156 = "zenith_target_esp";
   private static final String string123 = "zenith_target_esp";
   private static final String string6 = "Sampler0";
   private static final String string90 = "dimasik/target_esp_ghost_depth";
   private static final String string81 = "zenith_target_esp";
   private static final String string87 = "zenith_target_esp";
   private static final String string13 = "Sampler0";
   private static final String string20 = "dimasik/target_esp_crystal_color";
   private static final String string85 = "target_esp_crystal";
   private static final String string27 = "target_esp_crystal";
   private static final String string128 = "dimasik/target_esp_crystal_color_no_depth";
   private static final String string26 = "target_esp_crystal";
   private static final String string157 = "target_esp_crystal";
   private static final String string82 = "dimasik/target_esp_crystal_glow_depth";
   private static final String string43 = "dead_chams";
   private static final String string143 = "dead_chams";
   private static final String string5 = "Sampler0";
   private static final String string140 = "dimasik/target_esp_orbit_crystal_depth";
   private static final String string167 = "target_esp_crystal";
   private static final String string83 = "target_esp_crystal";
   private static final String string58 = "dimasik/target_esp_orbit_crystal_no_depth";
   private static final String string76 = "target_esp_crystal";
   private static final String string113 = "target_esp_crystal";
   private static final String string121 = "dimasik/target_esp_orbit_glow_depth";
   private static final String string151 = "target_esp_orbit_glow";
   private static final String string112 = "target_esp_orbit_glow";
   private static final String string118 = "Sampler0";
   private static final String string166 = "dimasik/target_esp_orbit_glow_no_depth";
   private static final String string21 = "target_esp_orbit_glow";
   private static final String string106 = "target_esp_orbit_glow";
   private static final String string159 = "Sampler0";
   private static final String string24 = "dimasik/target_esp_ring";
   private static final String string158 = "target_esp_ring";
   private static final String string17 = "target_esp_ring";
   private static final String string133 = "dimasik/target_esp_hoop";
   private static final String string103 = "target_esp_ring";
   private static final String string80 = "target_esp_ring";
   private static final String string171 = "dimasik/target_esp_ring_wave";
   private static final String string169 = "target_esp_cube";
   private static final String string22 = "target_esp_cube";
   private static final String string67 = "dimasik/target_esp_cube";
   private static final String string125 = "target_esp_cube";
   private static final String string68 = "target_esp_cube";
   private static final String string165 = "dimasik/target_esp_cube_composite";
   private static final String string136 = "hands_fullscreen";
   private static final String string124 = "target_esp_cube_composite";
   private static final String string144 = "InSampler";
   private static final String string36 = "dimasik/target_esp_lightning_arc";
   private static final String string74 = "dead_chams";
   private static final String string61 = "dead_chams";
   private static final String string23 = "Sampler0";
   private static final String string97 = "dimasik/target_esp_lightning_arc_depth";
   private static final String string95 = "dead_chams";
   private static final String string18 = "dead_chams";
   private static final String string48 = "Sampler0";
   private static final String string42 = "dimasik/target_esp_lightning";
   private static final String string57 = "target_esp_lightning";
   private static final String string7 = "target_esp_lightning";
   private static final String string98 = "dimasik/target_esp_lightning_core";
   private static final String string49 = "hands_fullscreen";
   private static final String string62 = "target_esp_lightning_core";
   private static final String string162 = "InSampler";
   private static final String string147 = "dimasik/target_esp_triangle_lines";
   private static final String string115 = "dimasik/target_esp_pentagram";
   private static final String string69 = "Globals";
   private static final String string142 = "target_esp_pentagram";
   private static final String string38 = "target_esp_pentagram";
   private static final float currentValue245 = 1.5F;
   private static final float currentValue478 = -1.5F;
   private static final float currentValue502 = -1.0F;
   private static final float currentValue1071 = -1.0F;
   private static final float currentValue599 = 0.8F;
   private static final float currentValue686 = 0.6F;
   private static final float currentValue522 = 0.9F;
   private static final float currentValue554 = 0.7F;
   private static final float currentValue162 = 0.5F;
   private static final float currentValue979 = 0.4F;
   private static final float currentValue819 = 0.6F;
   private static final int stateCounter54 = Integer.MIN_VALUE;
   private static final int stateCounter23 = Integer.MIN_VALUE;
   private static final int stateCounter6 = -11486721;
   private static final float currentValue77 = 1.2F;
   private static final String string117 = "Тип 1";
   private static final String string105 = "Тип 1";
   private static final int stateCounter28 = Integer.MIN_VALUE;
   private static final String string72 = "Тип 1";
   private static final String string94 = "Тип 1";
   private static final String string66 = "Тип 2";
   private static final int stateCounter53 = Integer.MIN_VALUE;
   private static final int stateCounter22 = Integer.MIN_VALUE;
   private static final float currentValue250 = 12.0F;
   private static final String string65 = "private/glow";
   private static final String string29 = "dimasik/target_esp_private_depth";
   private static final String string40 = "dimasik/target_esp_private_no_depth";
   private static final float currentValue1051 = 0.2F;
   private static final int stateCounter4 = Integer.MIN_VALUE;

   public static boolean isTextureTarget2() {
      return livingEntity != null && currentValue629 > currentValue772;
   }

   private static void onDeltaTrackerPoseStack2(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent, Vec3 position, LivingEntity entity, int value, float amount, float amount2) {
      position = position.subtract(TargetEspPreviewContext.camera().position());
      float amount3 = getStateActive87(amount);
      if (!(amount3 <= currentValue83)) {
         float amount4 = entity.getBbHeight();
         float amount5 = Mth.lerp(amount3, currentValue153, 1.0F) * getStateActive814(currentValue629);
         amount5 *= 1.0F + amount2 * currentValue727;
         float amount6 = 1.0F + currentValue579 * (float)Math.sin(currentValue1101 * currentValue736);
         float amount7 = Math.max(currentValue1084, entity.getBbWidth() * currentValue1090 + currentValue476) * amount5 * amount6;
         Matrix4f matrix4f = deltaTrackerPoseStackEvent.getPoseStack().last().pose();
         BufferBuilder bufferBuilder = getBufferBuilder3();
         Vec3[] vec3Array = getTextureTarget26(getVec3(new Vec3(0.0, 1.0, 0.0)), -currentValue1101 * currentValue644);
         getStateActive827(
            bufferBuilder,
            matrix4f,
            position.add(0.0, currentValue170, 0.0),
            vec3Array[0],
            vec3Array[1],
            amount7 * currentValue1035,
            getStateActive837(getTextureTarget22(value, currentValue214), amount3 * currentValue1153)
         );
         double id = currentValue1101 * currentValue655 + entity.getId() * currentValue92;
         double angleRadians = Math.toRadians(currentValue748 + currentValue891 * Math.sin(currentValue1101 * currentValue129));
         Vec3 position2 = new Vec3(Math.sin(angleRadians) * Math.cos(id), Math.cos(angleRadians), Math.sin(angleRadians) * Math.sin(id));
         Vec3[] vec3Array2 = getTextureTarget26(getVec3(position2), currentValue1101 * currentValue1138);
         Vec3 position3 = position.add(0.0, amount4 * currentValue144, 0.0);
         getStateActive827(
            bufferBuilder, matrix4f, position3, vec3Array2[0], vec3Array2[1], amount7, getStateActive837(value, amount3 * (currentValue754 + amount2 * currentValue1125))
         );
         getStateActive857(bufferBuilder, renderPipeline8, true);
         float amount8 = amount7 * currentValue1046;
         BufferBuilder bufferBuilder2 = getBufferBuilder3();
         addCameraFacingSprite(
            bufferBuilder2,
            matrix4f,
            position3.x,
            position3.y,
            position3.z,
            amount7 * (currentValue187 + amount2 * currentValue529),
            getStateActive837(getTextureTarget22(value, currentValue1102), amount3 * (currentValue164 + amount2 * currentValue234))
         );
         addCameraFacingSprite(
            bufferBuilder2,
            matrix4f,
            position3.x,
            position3.y,
            position3.z,
            amount7 * currentValue1001,
            getStateActive837(getTextureTarget22(value, currentValue42), amount3 * (currentValue1019 + amount2 * currentValue1189))
         );

         for (int index = 0; index < 5; index++) {
            Vec3 position4 = getTextureTarget210(position3, vec3Array2[0], vec3Array2[1], amount8, index);
            float amount9 = currentValue713
               + currentValue444 * (float)Math.sin(currentValue1101 * currentValue1062 + index * currentValue933);
            addCameraFacingSprite(
               bufferBuilder2,
               matrix4f,
               position4.x,
               position4.y,
               position4.z,
               currentValue183 * amount9,
               getStateActive837(getTextureTarget22(value, currentValue1119), amount3 * currentValue1104)
            );
            addCameraFacingSprite(
               bufferBuilder2,
               matrix4f,
               position4.x,
               position4.y,
               position4.z,
               currentValue301 * amount9,
               getStateActive837(getTextureTarget22(value, currentValue357), amount3 * currentValue200)
            );
         }

         int[] instance = new int[]{0, 2, 4, 1, 3};

         for (int index2 = 0; index2 < 3; index2++) {
            float amount10 = (currentValue1101 * currentValue510 + index2 / currentValue900) % 1.0F;
            float amount11 = amount10 * currentValue202;
            int value2 = Math.min(4, (int)amount11);
            float amount12 = amount11 - value2;
            Vec3 position5 = getTextureTarget210(position3, vec3Array2[0], vec3Array2[1], amount8, instance[value2]);
            Vec3 position6 = getTextureTarget210(position3, vec3Array2[0], vec3Array2[1], amount8, instance[(value2 + 1) % 5]);
            Vec3 position7 = position5.lerp(position6, amount12);
            float amount13 = currentValue875 - index2 * currentValue415;
            addCameraFacingSprite(
               bufferBuilder2,
               matrix4f,
               position7.x,
               position7.y,
               position7.z,
               amount13,
               getStateActive837(getTextureTarget22(value, currentValue546), amount3 * (currentValue135 - index2 * currentValue383))
            );
            addCameraFacingSprite(
               bufferBuilder2,
               matrix4f,
               position7.x,
               position7.y,
               position7.z,
               amount13 * currentValue293,
               getStateActive837(getTextureTarget22(value, currentValue859), amount3 * (currentValue649 - index2 * currentValue1089))
            );
         }

         for (int index3 = 0; index3 < 5; index3++) {
            double amount14 = currentValue1101 * currentValue150 + index3 * currentValue934 * currentValue429 / currentValue486;
            Vec3 position8 = position3.add(vec3Array2[0].scale(Math.cos(amount14) * amount7 * currentValue328))
               .add(vec3Array2[1].scale(Math.sin(amount14) * amount7 * currentValue624));
            addCameraFacingSprite(
               bufferBuilder2,
               matrix4f,
               position8.x,
               position8.y,
               position8.z,
               currentValue995,
               getStateActive837(getTextureTarget22(value, currentValue532), amount3 * currentValue474)
            );
         }

         updateState9(bufferBuilder2, resourceIdentifier8, true);
      }
   }

   private static void getTextureTarget23(
      BufferBuilder bufferBuilder3, PoseStack poseStack, float amount15, float amount16, float amount17, float amount18, int value3, float amount19, float amount20
   ) {
      poseStack.pushPose();
      poseStack.translate(amount15, amount16, amount17);
      float amount21 = amount19 * currentValue247;
      float amount22 = -amount15;
      float amount23 = amount21 - amount16;
      float amount24 = -amount17;
      float amount25 = amount22 * amount22 + amount23 * amount23 + amount24 * amount24;
      if (amount25 > currentValue589) {
         float amount26 = 1.0F / (float)Math.sqrt(amount25);
         float amount27 = Mth.clamp(amount23 * amount26, currentValue1164, 1.0F);
         float amount28 = (float)Math.toDegrees(Math.atan2(amount22, amount24));
         float amount29 = -((float)Math.toDegrees(Math.asin(amount27)));
         poseStack.mulPose(new Matrix4f().rotate(Axis.YP.rotationDegrees(amount28)));
         poseStack.mulPose(new Matrix4f().rotate(Axis.XP.rotationDegrees(amount29)));
      }

      poseStack.mulPose(new Matrix4f().rotate(Axis.ZP.rotationDegrees(amount20)));
      poseStack.scale(amount18 * currentValue1181, amount18 * currentValue840, amount18 * currentValue189);
      Matrix4f matrix4f2 = poseStack.last().pose();
      float amount30 = currentValue998;
      float amount31 = currentValue353;
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, amount31, -amount30, 0.0F, 0.0F, 0.0F, amount30, 0.0F, value3);
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, amount31, 0.0F, amount30, 0.0F, amount30, 0.0F, 0.0F, value3);
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, amount31, amount30, 0.0F, 0.0F, 0.0F, -amount30, 0.0F, value3);
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, amount31, 0.0F, -amount30, 0.0F, -amount30, 0.0F, 0.0F, value3);
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, -amount31, 0.0F, amount30, 0.0F, -amount30, 0.0F, 0.0F, value3);
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, -amount31, amount30, 0.0F, 0.0F, 0.0F, amount30, 0.0F, value3);
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, -amount31, 0.0F, -amount30, 0.0F, amount30, 0.0F, 0.0F, value3);
      getStateActive88(bufferBuilder3, matrix4f2, 0.0F, 0.0F, -amount31, -amount30, 0.0F, 0.0F, 0.0F, -amount30, 0.0F, value3);
      poseStack.popPose();
   }

   private static float getTextureTarget211(int value4, int value5) {
      float amount32 = (float)value4 / Math.max(1, value5 - 1);
      return currentValue1060 + currentValue679 * (float)Math.sin(currentValue305 * Math.pow(amount32, currentValue409));
   }

   private static void onDeltaTrackerPoseStack3(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent2, Vec3 position9, LivingEntity entity2, int value6, int value7, float amount33, long value8) {
      position9 = position9.subtract(TargetEspPreviewContext.camera().position());
      getStateActive860(entity2);
      float amount34 = (float)(value8 % lastUpdateMillis11) / currentValue615;
      float amount35 = (float)value8 * currentValue940;
      float amount36 = currentValue710 - currentValue1149 * amount33;
      float angleRadians2 = (float)Math.sin(Math.toRadians(amount34));
      float angleRadians3 = (float)Math.cos(Math.toRadians(amount34));
      BufferBuilder bufferBuilder4 = getBufferBuilder3();
      PoseStack poseStack2 = deltaTrackerPoseStackEvent2.getPoseStack();

      for (int index4 = 0; index4 < list2.size(); index4++) {
         Vector3f position10 = list2.get(index4);
         float amount37 = position10.x * angleRadians3 - position10.z * angleRadians2;
         float amount38 = position10.x * angleRadians2 + position10.z * angleRadians3;
         float amount39 = amount37 * amount36;
         float amount40 = amount38 * amount36;
         float amount41 = position10.y + (float)(currentValue826 * Math.sin(amount35 * currentValue1172 + index4 * currentValue642));
         updateState13(
            bufferBuilder4, poseStack2, position9.x + amount39, position9.y + amount41, position9.z + amount40, currentValue1075, getStateActive837(value6, amount33 * currentValue654)
         );
         updateState13(
            bufferBuilder4,
            poseStack2,
            position9.x + amount39,
            position9.y + amount41,
            position9.z + amount40,
            currentValue1065,
            getStateActive837(getTextureTarget22(value6, currentValue51), amount33 * currentValue799)
         );
      }

      getStateActive67(bufferBuilder4, resourceIdentifier8, true, getStateActive64());
      BufferBuilder bufferBuilder5 = getBufferBuilder3();

      for (int index5 = 0; index5 < list2.size(); index5++) {
         Vector3f position11 = list2.get(index5);
         float amount42 = position11.x * angleRadians3 - position11.z * angleRadians2;
         float amount43 = position11.x * angleRadians2 + position11.z * angleRadians3;
         float amount44 = amount42 * amount36;
         float amount45 = amount43 * amount36;
         float amount46 = position11.y + (float)(currentValue572 * Math.sin(amount35 * currentValue127 + index5 * currentValue767));
         float amount47 = amount34 * (index5 % 2 == 0 ? currentValue443 : currentValue732) + index5 * currentValue1133;
         float amount48 = (float)Math.sin(index5 * currentValue626 + currentValue986) * currentValue691;
         amount48 -= (float)Math.floor(amount48);
         float amount49 = currentValue297 + amount48 * currentValue5;
         float amount50 = currentValue1096
            + ((float)Math.sin(amount35 * currentValue1193 + index5 * currentValue208) + 1.0F) * currentValue549;
         int color2 = ColorMath.packRgba(
            ColorMath.redChannel(value6),
            ColorMath.greenChannel(value6),
            ColorMath.blueChannel(value6),
            Math.round(currentValue839 * amount33)
         );
         int color3 = ColorMath.packRgba(
            ColorMath.redChannel(value6),
            ColorMath.greenChannel(value6),
            ColorMath.blueChannel(value6),
            Math.round(currentValue1005 * amount33)
         );
         int value9 = getTextureTarget22(color2, amount50);
         int color4 = ColorMath.interpolateRgba(value9, -1, currentValue578);
         poseStack2.pushPose();
         poseStack2.translate(position9.x, position9.y, position9.z);
         updateState17(bufferBuilder5, poseStack2, amount44, amount46, amount45, amount49 * currentValue284, value9, color4, amount47, true, index5, true);
         updateState17(bufferBuilder5, poseStack2, amount44, amount46, amount45, amount49, value9, color4, amount47, true, index5, false);
         poseStack2.popPose();
      }

      getStateActive611(bufferBuilder5, getTextureTarget213());
   }

   private static void getTextureTarget28(BufferBuilder bufferBuilder6, Matrix4f matrix4f3, float amount51, float amount52, float amount53, float amount54, float amount55, int value10) {
      addColoredVertex(bufferBuilder6, matrix4f3, amount51, amount52, amount53, amount54, amount55, ColorMath.getVector4f(value10));
   }

   private static void getTextureTarget27(BufferBuilder bufferBuilder7, Matrix4f matrix4f4, double amount56, double amount57, double amount58, int value11, float amount59) {
      if (!(amount59 <= currentValue104)) {
         float amount60 = getStateActive68(currentValue256);
         double amount61 = currentValue70 * amount60;
         int value12 = getStateActive837(value11, amount59);
         int value13 = getStateActive837(value11, amount59 * currentValue645);
         float amount62 = currentValue585 + currentValue19 * (1.0F - amount60);
         double amount63 = currentValue602 * amount60;

         for (int index6 = 0; index6 < 26; index6++) {
            double amount64 = currentValue830 * index6 / currentValue282 + currentValue378 * currentValue131;
            double amount65 = amount56 + Math.cos(amount64) * amount61;
            double amount66 = amount58 + Math.sin(amount64) * amount61;
            double amount67 = amount57 + Math.sin(amount64 * currentValue203 + currentValue256 * currentValue290) * amount63 * currentValue1141;
            addCameraFacingSprite(bufferBuilder7, matrix4f4, amount65, amount67, amount66, amount62, value12);
            addCameraFacingSprite(bufferBuilder7, matrix4f4, amount65, amount67, amount66, amount62 * currentValue50, value13);
         }
      }
   }

   private static void onDeltaTrackerPoseStack5(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent3, Vec3 position12, LivingEntity entity3, int value14, float amount68) {
      BufferBuilder bufferBuilder8 = GpuResourceScope.getGpuResourceScope().getBufferBuilder(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      Vec3 position13 = position12.subtract(TargetEspPreviewContext.camera().position());
      PoseStack poseStack3 = new PoseStack();
      poseStack3.mulPose(deltaTrackerPoseStackEvent3.getPoseStack().last().pose());
      double amount69 = currentValue1101 * currentValue867;
      Quaternionf quaternionf = TargetEspPreviewContext.camera().rotation();
      RingParticleEmitter.updateState(
         entity3.getBbWidth(), entity3.getBbHeight(), amount69, amount68, currentValue806, stateCounter13, (amount70, amount71, amount72, amount73, value15) -> {
            if (value15 != 0) {
               poseStack3.pushPose();
               poseStack3.translate(position13.x + amount70, position13.y + amount71, position13.z + amount72);
               poseStack3.mulPose(new Matrix4f().rotate(quaternionf));
               ChamsShaderProgram.updateState(bufferBuilder8, poseStack3.last().pose(), amount73, value14 & stateCounter25 | value15 << 24);
               poseStack3.popPose();
            }
         }
      );
      getStateActive67(bufferBuilder8, resourceIdentifier7, false, stateActive9 ? renderPipeline27 : renderPipeline9);
   }

   private static float getTextureTarget2(float amount74, float amount75) {
      float amount76 = 0.0F;
      float amount77 = currentValue705;
      float amount78 = 1.0F;

      for (int index7 = 0; index7 < 4; index7++) {
         amount76 += getStateActive853(amount74 * amount78, amount75 * amount78) * amount77;
         amount78 *= currentValue427;
         amount77 *= currentValue116;
      }

      return Mth.clamp(amount76, 0.0F, 1.0F);
   }

   private static void onDeltaTrackerPoseStack16(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent4, Vec3 position14, LivingEntity entity4, int value16, float amount79) {
      if (stateCounter31 != entity4.getId()) {
         linkedList3.clear();
         lastUpdateMillis35 = 0L;
         stateCounter31 = entity4.getId();
      }

      Vec3 position15 = TargetEspPreviewContext.camera().position();
      double amount80 = position14.x;
      double amount81 = position14.y + entity4.getBbHeight() / currentValue991;
      double amount82 = position14.z;
      double amount83 = amount80 - position15.x;
      double amount84 = amount81 - position15.y;
      double amount85 = amount82 - position15.z;
      long nowMillis = System.currentTimeMillis();
      PoseStack poseStack4 = deltaTrackerPoseStackEvent4.getPoseStack();
      BufferBuilder bufferBuilder9 = getBufferBuilder3();
      getStateActive69(poseStack4, bufferBuilder9, position15, amount79, value16);

      for (int index8 = 0; index8 < 20; index8++) {
         float amount86 = 1.0F - index8 / currentValue303 * currentValue1170;
         double amount87 = currentValue779 * (nowMillis - index8 * currentValue206) / currentValue898;
         float amount88 = (float)(currentValue507 * amount86);
         getStateActive843(poseStack4, bufferBuilder9, amount83, amount84, amount85, amount87, 1.0, 1.0, currentValue454, amount88, amount79, value16);
         getStateActive843(poseStack4, bufferBuilder9, amount83, amount84, amount85, amount87, currentValue889, 1.0, currentValue959, amount88, amount79, value16);
         getStateActive843(poseStack4, bufferBuilder9, amount83, amount84, amount85, amount87, currentValue1118, currentValue952, 1.0, amount88, amount79, value16);
      }

      updateState21(amount80, amount81, amount82, nowMillis, amount79);
      updateState9(bufferBuilder9, resourceIdentifier, true);
   }

   private static int getTextureTarget22(int value17, float amount89) {
      amount89 = Mth.clamp(amount89, 0.0F, 1.0F);
      return ColorMath.packRgba(
         Math.round((float)Mth.lerpInt(amount89, ColorMath.redChannel(value17), 255)),
         Math.round((float)Mth.lerpInt(amount89, ColorMath.greenChannel(value17), 255)),
         Math.round((float)Mth.lerpInt(amount89, ColorMath.blueChannel(value17), 255)),
         ColorMath.alphaChannel(value17)
      );
   }

   private static void getTextureTarget24(
      BufferBuilder bufferBuilder10,
      Matrix4f matrix4f5,
      Vec3 position16,
      Vec3 position17,
      double amount90,
      double amount91,
      double amount92,
      double amount93,
      double amount94,
      double amount95,
      float amount96,
      int value18
   ) {
      for (byte index9 = -1; index9 <= 1; index9 += 2) {
         for (byte index10 = -1; index10 <= 1; index10 += 2) {
            for (byte index11 = -1; index11 <= 1; index11 += 2) {
               double amount97 = 1.0 - Math.pow(1.0F - amount96, currentValue864);
               Vec3 position18 = getStateActive836(new Vec3(index9 * amount92, index10 * amount92, index11 * amount92), amount90, amount91, 0.0);
               Vec3 position19 = new Vec3(position18.x, 0.0, position18.z).normalize();
               double amount98 = amount97
                  * (currentValue506 + (index10 > 0 ? currentValue194 : 0.0) + (index9 * index11 > 0 ? currentValue343 : 0.0));
               double amount99 = getStateActive835(position18.y, amount95, amount96, index9, index10, index11);
               Vec3 position20 = new Vec3(position18.x + position19.x * amount98, amount99, position18.z + position19.z * amount98);
               double amount100 = amount97 * (currentValue346 + (index9 * index10 * index11 > 0 ? currentValue905 : currentValue376));
               Vec3 position21 = getStateActive836(new Vec3(-index9, 0.0, 0.0), amount90, amount91, 0.0);
               Vec3 position22 = getStateActive836(new Vec3(0.0, -index10, 0.0), amount90, amount91, 0.0);
               Vec3 position23 = getStateActive836(new Vec3(0.0, 0.0, -index11), amount90, amount91, 0.0);
               position21 = getStateActive836(position21, amount100 * index11, amount100 * index10, amount100 * index9);
               position22 = getStateActive836(position22, amount100 * index11, amount100 * index10, amount100 * index9);
               position23 = getStateActive836(position23, amount100 * index11, amount100 * index10, amount100 * index9);
               getStateActive845(bufferBuilder10, matrix4f5, position16, position17, position20, position21, position22, position23, amount93, amount94, value18);
            }
         }
      }
   }

   private static int getTextureTarget29(int value19, int value20, float amount101) {
      float nowMillis2 = ((float)System.currentTimeMillis() / currentValue804 + amount101) % currentValue459;
      float angleRadians4 = ((float)Math.sin(Math.toRadians(nowMillis2)) + 1.0F) * currentValue370;
      return ColorMath.interpolateRgba(value19, value20, angleRadians4);
   }

   private static Vec3[] getTextureTarget26(Vec3[] vec3Array3, double amount102) {
      double amount103 = Math.cos(amount102);
      double amount104 = Math.sin(amount102);
      Vec3 position24 = vec3Array3[0];
      Vec3 position25 = vec3Array3[1];
      return new Vec3[]{position24.scale(amount103).add(position25.scale(amount104)), position24.scale(-amount104).add(position25.scale(amount103))};
   }

   private static RenderPipeline getTextureTarget213() {
      return stateActive11 ? renderPipeline17 : renderPipeline23;
   }

   private static void getTextureTarget25(
      BufferBuilder bufferBuilder11, Matrix4f matrix4f6, double amount105, double amount106, double amount107, float amount108, float amount109, float amount110, int value21
   ) {
      Vector4f vector4f = ColorMath.getVector4f(value21);
      float amount111 = amount108 * currentValue23;
      float angleRadians5 = (float)Math.toRadians(amount109);
      float angleRadians6 = (float)Math.toRadians(amount110);
      float amount112 = (float)Math.cos(angleRadians5) * amount111;
      float amount113 = (float)Math.sin(angleRadians5) * amount111;
      float amount114 = (float)Math.sin(angleRadians5) * (float)Math.sin(angleRadians6) * amount111;
      float amount115 = (float)Math.cos(angleRadians6) * amount111;
      float amount116 = -((float)Math.cos(angleRadians5)) * (float)Math.sin(angleRadians6) * amount111;
      addColoredVertex(bufferBuilder11, matrix4f6, (float)amount105 - amount112 - amount114, (float)amount106 - amount115, (float)amount107 - amount113 - amount116, 0.0F, 1.0F, vector4f);
      addColoredVertex(bufferBuilder11, matrix4f6, (float)amount105 - amount112 + amount114, (float)amount106 + amount115, (float)amount107 - amount113 + amount116, 0.0F, 0.0F, vector4f);
      addColoredVertex(bufferBuilder11, matrix4f6, (float)amount105 + amount112 + amount114, (float)amount106 + amount115, (float)amount107 + amount113 + amount116, 1.0F, 0.0F, vector4f);
      addColoredVertex(bufferBuilder11, matrix4f6, (float)amount105 + amount112 - amount114, (float)amount106 - amount115, (float)amount107 + amount113 - amount116, 1.0F, 1.0F, vector4f);
   }

   private static Vec3 getTextureTarget210(Vec3 position26, Vec3 position27, Vec3 position28, float amount117, int value22) {
      double amount118 = currentValue907 + value22 * currentValue257 * currentValue662 / currentValue1033;
      return position26.add(position27.scale(Math.cos(amount118) * amount117)).add(position28.scale(Math.sin(amount118) * amount117));
   }

   private static TextureTarget getTextureTarget212(int value23, int value24) {
      if (textureTarget2 == null) {
         textureTarget2 = new TextureTarget(string92, value23, value24, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
      } else if (textureTarget2.width != value23 || textureTarget2.height != value24) {
         textureTarget2.resize(value23, value24);
      }

      return textureTarget2;
   }

   private static void getStateActive611(BufferBuilder bufferBuilder12, RenderPipeline renderPipeline) {
      getStateActive857(bufferBuilder12, renderPipeline, false);
   }

   private static int getStateActive63(LivingEntity entity5, int value25, float amount119, long value26) {
      if (stateCounter26 != entity5.getId()) {
         stateCounter26 = entity5.getId();
         stateCounter37 = entity5.hurtTime;
         lastUpdateMillis23 = 0L;
         currentValue130 = 0.0F;
      }

      if (entity5.hurtTime > stateCounter37) {
         lastUpdateMillis23 = value26;
      }

      stateCounter37 = entity5.hurtTime;
      if (!stateActive) {
         currentValue130 = 0.0F;
         return value25;
      } else {
         float amount120 = Mth.clamp((float)(value26 - lastUpdateMillis23) / currentValue1000, 0.0F, 1.0F);
         float amount121 = 1.0F - getStateActive87(amount120);
         float amount122 = 1.0F - (float)Math.pow(currentValue749, amount119 * currentValue426);
         currentValue130 = Mth.lerp(amount122, currentValue130, amount121);
         return currentValue130 < currentValue67
            ? value25
            : ColorMath.interpolateRgba(value25, stateCounter50, currentValue130);
      }
   }

   private static float getStateActive613(float amount123) {
      return amount123 * amount123 * amount123 * (amount123 * (amount123 * currentValue612 - currentValue1108) + currentValue526);
   }

   private static List<Vec3> getStateActive614(Vec3 position29, Vec3 position30, int value27, double amount124) {
      if (value27 <= 0) {
         ArrayList items = new ArrayList(2);
         items.add(position29);
         items.add(position30);
         return items;
      } else {
         Vec3 position31 = position30.subtract(position29);
         Vec3 position32 = position29.add(position31.scale(currentValue547));
         Vec3 position33 = getStateActive822(position31);
         double amount125 = (random5.nextDouble() - currentValue846) * currentValue1127 * amount124;
         Vec3 position34 = position32.add(position33.scale(amount125));
         List items2 = getStateActive614(position29, position34, value27 - 1, amount124 * currentValue226);
         List items3 = getStateActive614(position34, position30, value27 - 1, amount124 * currentValue618);
         items2.remove(items2.size() - 1);
         items2.addAll(items3);
         return items2;
      }
   }

   private static RenderPipeline getStateActive610() {
      return stateActive11 ? renderPipeline13 : renderPipeline24;
   }

   private static void getStateActive67(BufferBuilder bufferBuilder13, Identifier identifier, boolean matches, RenderPipeline renderPipeline2) {
      MeshData meshData = bufferBuilder13.build();
      if (meshData != null) {
         GpuBuffer gpuBuffer = null;

         try {
            DrawState drawState = meshData.drawState();
            int value28 = drawState.indexCount();
            AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
            GpuBuffer gpuBuffer2 = autoStorageIndexBuffer.getBuffer(value28);
            IndexType indexType = autoStorageIndexBuffer.type();
            gpuBuffer = RenderSystem.getDevice().createBuffer(() -> string79, 32, meshData.vertexBuffer());
            if (matches && stateActive3) {
               DeadChamsCompositeShader.deadChamsCompositeShader.updateState5(gpuBuffer, gpuBuffer2, indexType, value28, currentValue908);
            }

            RenderTarget renderTarget = TargetEspPreviewContext.target();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            AbstractTexture abstractTexture = MINECRAFT.getTextureManager().getTexture(identifier);
            RenderPass renderPass = commandEncoder.createRenderPass(
               () -> string10, renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(renderPass);
               if (RenderPipelineCache.isCompiled(renderPipeline2)) {
                  renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline2));
                  renderPass.setUniform(string114, abstractTexture.getTextureView(), abstractTexture.getSampler());
                  renderPass.setVertexBuffer(0, gpuBuffer.slice());
                  renderPass.setIndexBuffer(gpuBuffer2, indexType);
                  renderPass.drawIndexed(value28, 1, 0, 0, 0);
               }
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
         } finally {
            if (gpuBuffer != null) {
               gpuBuffer.close();
            }

            meshData.close();
         }
      }
   }

   private static RenderPipeline getStateActive64() {
      return stateActive11 ? renderPipeline29 : renderPipeline10;
   }

   private static float getStateActive68(float amount126) {
      amount126 = Mth.clamp(amount126, 0.0F, 1.0F);
      return amount126 * amount126 * (currentValue229 - 2.0F * amount126);
   }

   private static void getStateActive66(BufferBuilder bufferBuilder14, Identifier identifier2) {
      updateState9(bufferBuilder14, identifier2, false);
   }

   private static void getStateActive65(BufferBuilder bufferBuilder15, Matrix4f matrix4f7, List<Vec3> items4, float amount127, float amount128, int value29, float amount129, float amount130) {
      int size2 = items4.size() - 1;
      if (size2 > 0 && !(amount127 <= 0.0F) && !(amount128 <= currentValue108)) {
         float amount131 = amount127 * size2;

         for (int index12 = 0; index12 < size2 && !(amount131 <= index12); index12++) {
            float amount132 = Mth.clamp(amount131 - index12, 0.0F, 1.0F);
            float amount133 = getStateActive859(index12 + 1, amount129);
            float amount134 = 1.0F - getStateActive68(Mth.clamp((amount130 - amount133 * currentValue1190) / currentValue465, 0.0F, 1.0F));
            if (!(amount134 <= currentValue437)) {
               Vec3 position35 = (Vec3)items4.get(index12);
               Vec3 position36 = (Vec3)items4.get(index12 + 1);
               Vec3 position37 = amount132 < 1.0F ? position35.lerp(position36, amount132) : position36;
               Vec3 position38 = position37.subtract(position35);
               if (!(position38.lengthSqr() < currentValue932)) {
                  position38 = position38.normalize();
                  Vec3 position39 = Math.abs(position38.y) < currentValue375 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
                  Vec3 position40 = position38.cross(position39);
                  if (position40.lengthSqr() < currentValue970) {
                     position40 = new Vec3(0.0, 0.0, 1.0);
                  }

                  position40 = position40.normalize();
                  Vec3 position41 = position38.cross(position40).normalize();
                  if (index12 > 0) {
                     position35 = position35.subtract(position38.scale(amount128 * currentValue603));
                  }

                  if (amount132 >= 1.0F && index12 < size2 - 1) {
                     position37 = position37.add(position38.scale(amount128 * currentValue993));
                  }

                  float amount135 = (index12 + amount132 * currentValue1091) / size2;
                  float amount136 = Mth.lerp(amount135, currentValue745, currentValue834);
                  float amount137 = amount132 < 1.0F ? currentValue449 : 1.0F;
                  float amount138 = amount128 * amount136 * amount137;
                  float amount139 = amount138 * Mth.lerp(amount132, 1.0F, currentValue448);
                  float amount140 = (1.0F - amount135 * currentValue871) * (currentValue989 + amount133 * currentValue1173) * amount134;
                  int value30 = getStateActive837(value29, amount140);
                  byte amount141 = 6;

                  for (int index13 = 0; index13 < amount141; index13++) {
                     double amount142 = currentValue557 * index13 / amount141;
                     double amount143 = currentValue927 * (index13 + 1) / amount141;
                     Vec3 position42 = position35.add(position40.scale(Math.cos(amount142) * amount138)).add(position41.scale(Math.sin(amount142) * amount138));
                     Vec3 position43 = position35.add(position40.scale(Math.cos(amount143) * amount138)).add(position41.scale(Math.sin(amount143) * amount138));
                     Vec3 position44 = position37.add(position40.scale(Math.cos(amount142) * amount139)).add(position41.scale(Math.sin(amount142) * amount139));
                     Vec3 position45 = position37.add(position40.scale(Math.cos(amount143) * amount139)).add(position41.scale(Math.sin(amount143) * amount139));
                     getTextureTarget28(bufferBuilder15, matrix4f7, (float)position42.x, (float)position42.y, (float)position42.z, 0.0F, (float)index13 / amount141, value30);
                     getTextureTarget28(bufferBuilder15, matrix4f7, (float)position43.x, (float)position43.y, (float)position43.z, 0.0F, (index13 + 1.0F) / amount141, value30);
                     getTextureTarget28(bufferBuilder15, matrix4f7, (float)position45.x, (float)position45.y, (float)position45.z, amount132, (index13 + 1.0F) / amount141, value30);
                     getTextureTarget28(bufferBuilder15, matrix4f7, (float)position44.x, (float)position44.y, (float)position44.z, amount132, (float)index13 / amount141, value30);
                  }
               }
            }
         }
      }
   }

   private static void getStateActive62(BufferBuilder bufferBuilder16, Matrix4f matrix4f8, Vec3 position46, Vec3 position47, Vec3 position48, double amount144, int value31, int value32, int value33) {
      for (int index14 = 0; index14 < value31; index14++) {
         double amount145 = currentValue983 + currentValue1085 * index14 / value31;
         double amount146 = currentValue441 + currentValue1168 * (index14 + 1) / value31;

         for (int index15 = 0; index15 < value32; index15++) {
            double amount147 = currentValue95 * index15 / value32;
            double amount148 = currentValue1058 * (index15 + 1) / value32;
            Vec3 position49 = getStateActive855(position48, amount144, amount145, amount147);
            Vec3 position50 = getStateActive855(position48, amount144, amount145, amount148);
            Vec3 position51 = getStateActive855(position48, amount144, amount146, amount148);
            Vec3 position52 = getStateActive855(position48, amount144, amount146, amount147);
            updateState11(
               bufferBuilder16,
               matrix4f8,
               getStateActive838(position49, position46, position47),
               getStateActive838(position50, position46, position47),
               getStateActive838(position51, position46, position47),
               getStateActive838(position52, position46, position47),
               value33
            );
         }
      }
   }

   private static void onDeltaTrackerPoseStack23(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent5, Vec3 position53, LivingEntity entity6, int value34, int value35, float amount149) {
      Vec3 position54 = TargetEspPreviewContext.camera().position();
      Vec3 position55 = position53.add(0.0, entity6.getBbHeight() * currentValue425, 0.0).subtract(position54);
      BufferBuilder bufferBuilder17 = getBufferBuilder3();
      PoseStack poseStack5 = deltaTrackerPoseStackEvent5.getPoseStack();
      poseStack5.pushPose();
      poseStack5.translate(position55.x, position55.y, position55.z);
      poseStack5.scale(currentValue821, currentValue403, currentValue351);
      double nowMillis3 = (System.currentTimeMillis() - lastUpdateMillis13) / currentValue410;

      for (int index16 = 0; index16 < 4; index16++) {
         for (int index17 = 0; index17 < 9; index17++) {
            poseStack5.pushPose();
            float amount150 = index17 / currentValue805;
            float amount151 = (currentValue321 * (1.0F - amount150) + currentValue345 * amount150) * currentValue876;
            double amount152 = nowMillis3 - index16 * currentValue717;
            double amount153 = currentValue1094 * (amount152 - index17 * currentValue597) / currentValue890;
            double amount154 = 0.0;
            double amount155 = -Math.cos(amount153) * currentValue24;
            double amount156 = Math.sin(amount153) * currentValue281;
            switch (index16) {
               case 0:
                  amount154 += index17 * currentValue1016;
                  poseStack5.translate(amount155, amount154 + currentValue740, amount156);
                  break;
               case 1:
               case 3:
                  amount154 -= index17 * currentValue639;
                  poseStack5.translate(amount155, amount154 - currentValue858, amount156);
                  break;
               case 2:
               default:
                  amount154 += index17 * currentValue49;
                  poseStack5.translate(amount155, amount154, amount156);
            }

            int value36 = getStateActive837(getStateActive820(value34, value35, index17 * 25, false), amount149);
            poseStack5.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
            Matrix4f matrix4f9 = poseStack5.last().pose();
            getStateActive841(bufferBuilder17, matrix4f9, amount151, getStateActive832(value36));
            if (stateActive2) {
               int value37 = getStateActive837(getTextureTarget22(value36, currentValue157), currentValue764);
               getStateActive841(bufferBuilder17, matrix4f9, amount151 * currentValue575, getStateActive832(value37));
               getStateActive841(
                  bufferBuilder17, matrix4f9, amount151 * currentValue818, getStateActive832(getStateActive837(value37, currentValue620))
               );
            }

            poseStack5.popPose();
         }
      }

      poseStack5.popPose();
      getStateActive831(bufferBuilder17, resourceIdentifier6);
   }

   private static Vec3 getStateActive6(double amount157, double amount158, double amount159, double amount160) {
      double amount161 = Math.sin(amount157) * currentValue887;
      double amount162 = Math.cos(amount157) * currentValue33;
      if (amount158 == 1.0 && amount159 == 1.0 && amount160 == currentValue453) {
         return new Vec3(amount161, amount162, -amount162);
      } else {
         return amount158 == currentValue721 && amount159 == 1.0 && amount160 == currentValue359 ? new Vec3(-amount161, amount161, -amount162) : new Vec3(-amount161, -amount161, amount162);
      }
   }

   private static void getStateActive612(
      BufferBuilder bufferBuilder18,
      Matrix4f matrix4f10,
      double amount163,
      double amount164,
      double amount165,
      long value38,
      double amount166,
      float amount167,
      float amount168,
      int value39,
      int value40,
      int value41
   ) {
      double amount169 = currentValue1052 * (1.0 + currentValue1049 * amount168);
      float amount170 = currentValue280 + currentValue218 * amount168;

      for (int index18 = 0; index18 < 20; index18++) {
         float amount171 = 1.0F - index18 / currentValue41 * amount170;
         double amount172 = value38 * currentValue18 - index18 * amount169 + currentValue378;
         double amount173 = Math.sin(amount172) * amount166;
         double amount174 = Math.cos(amount172) * amount166;
         double amount175;
         double amount176;
         double amount177;
         switch (value41) {
            case 0:
               amount175 = amount163 + amount173;
               amount176 = amount164 + amount174;
               amount177 = amount165 - amount174;
               break;
            case 1:
               amount175 = amount163 - amount173;
               amount176 = amount164 + amount173;
               amount177 = amount165 - amount174;
               break;
            default:
               amount175 = amount163 - amount173;
               amount176 = amount164 - amount173;
               amount177 = amount165 + amount174;
         }

         float amount178 = currentValue308 * amount171 * amount167;
         addCameraFacingSprite(bufferBuilder18, matrix4f10, amount175, amount176, amount177, amount178 * currentValue367, value39);
         addCameraFacingSprite(bufferBuilder18, matrix4f10, amount175, amount176, amount177, amount178 * currentValue775, value40);
      }
   }

   private static void getStateActive69(PoseStack poseStack6, BufferBuilder bufferBuilder19, Vec3 position56, float amount179, int value42) {
      long nowMillis4 = System.currentTimeMillis();
      Iterator iterator = linkedList3.iterator();

      while (iterator.hasNext()) {
         TargetEspRenderer.BNt5vHvMwoeyBNKB bnt5vhvmwoeybnkb = (TargetEspRenderer.BNt5vHvMwoeyBNKB)iterator.next();
         long value43 = nowMillis4 - bnt5vhvmwoeybnkb.lastUpdateMillis20;
         if (value43 > lastUpdateMillis21) {
            iterator.remove();
         } else {
            float amount180 = 1.0F - (float)value43 / currentValue342;
            if (amount180 <= 0.0F) {
               iterator.remove();
            } else {
               float amount181 = Mth.clamp(amount179 * bnt5vhvmwoeybnkb.currentValue980 * amount180 * currentValue994, 0.0F, 1.0F);
               float amount182 = bnt5vhvmwoeybnkb.currentValue707 * (currentValue669 + currentValue84 * amount180);
               updateState20(
                  poseStack6,
                  bufferBuilder19,
                  bnt5vhvmwoeybnkb.vec33.x - position56.x,
                  bnt5vhvmwoeybnkb.vec33.y - position56.y,
                  bnt5vhvmwoeybnkb.vec33.z - position56.z,
                  amount182,
                  amount181,
                  value42
               );
            }
         }
      }
   }

   private static boolean isStateActive6(String text) {
      if (string88.equals(text)) {
         return stateActive6;
      } else {
         return string53.equals(text) ? stateActive11 : true;
      }
   }

   private static BufferBuilder getStateActive815() {
      return GpuResourceScope.getGpuResourceScope().getBufferBuilder(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
   }

   private static void onDeltaTrackerPoseStack12(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent6, Vec3 position57, LivingEntity entity7, int value44, float amount183) {
      position57 = position57.subtract(TargetEspPreviewContext.camera().position());
      float amount184 = currentValue1101 * currentValue377;
      float amount185 = entity7.getBbWidth() * currentValue36;
      float amount186 = entity7.getBbHeight();
      float amount187 = getStateActive87(amount183);
      int value45 = getStateActive812(value44, currentValue879);
      short amount188 = 144;
      byte amount189 = 32;
      Matrix4f matrix4f11 = deltaTrackerPoseStackEvent6.getPoseStack().last().pose();
      BufferBuilder bufferBuilder20 = getBufferBuilder3();
      double amount190 = Math.abs(Math.sin(amount184)) * amount186;
      double amount191 = Math.abs(Math.sin(amount184 - currentValue614)) * amount186;

      for (int index19 = 0; index19 < amount188; index19++) {
         double amount192 = currentValue115 * index19 / amount188;
         double amount193 = position57.x + Math.cos(amount192) * amount185;
         double amount194 = position57.z + Math.sin(amount192) * amount185;
         int value46 = getTextureTarget29(value44, value45, index19 * (currentValue609 / amount188));
         addCameraFacingSprite(
            bufferBuilder20, matrix4f11, amount193, position57.y + amount190, amount194, currentValue545, getStateActive837(value46, amount187 * currentValue44)
         );

         for (int index20 = 1; index20 <= amount189; index20++) {
            float amount195 = (float)index20 / amount189;
            double amount196 = amount190 + (amount191 - amount190) * amount195;
            float amount197 = amount187 * (1.0F - amount195 * currentValue790) * currentValue676;
            float amount198 = Mth.lerp(amount195, currentValue479, currentValue1069);
            addCameraFacingSprite(bufferBuilder20, matrix4f11, amount193, position57.y + amount196, amount194, amount198, getStateActive837(value46, amount197));
         }
      }

      updateState9(bufferBuilder20, resourceIdentifier4, true);
   }

   private static void getStateActive844(BufferBuilder bufferBuilder21, Matrix4f matrix4f12, List<Vec3> items5, float amount199, float amount200, int value47, int value48, float amount201) {
      int size3 = items5.size() - 1;
      if (size3 > 0 && !(amount200 <= currentValue100) && !(amount199 <= currentValue523)) {
         float amount202 = amount199 * size3;
         int value49 = Math.min(size3 - 1, (int)amount202);
         Vec3[] vec3Array4 = new Vec3[value49 + 2];
         Vec3[] vec3Array5 = new Vec3[value49 + 2];
         Vec3[] vec3Array6 = new Vec3[value49 + 2];
         Vec3 position58 = null;

         for (int index21 = 0; index21 < vec3Array4.length; index21++) {
            Vec3 position59 = (Vec3)items5.get(Math.max(0, index21 - 1));
            Vec3 position60 = (Vec3)items5.get(Math.min(items5.size() - 1, index21 + 1));
            Vec3 position61 = position60.subtract(position59);
            if (position61.lengthSqr() < currentValue906) {
               position61 = new Vec3(0.0, 1.0, 0.0);
            }

            position61 = position61.normalize();
            Vec3 position62 = Math.abs(position61.y) < currentValue646 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            Vec3 position63 = position61.cross(position62);
            if (position63.lengthSqr() < currentValue213) {
               position63 = new Vec3(1.0, 0.0, 0.0);
            }

            position63 = position63.normalize();
            if (position58 != null && position63.dot(position58) < 0.0) {
               position63 = position63.scale(currentValue273);
            }

            if (position58 != null) {
               position63 = position58.add(position63).normalize();
               if (position63.lengthSqr() < currentValue316) {
                  position63 = position58;
               }
            }

            Vec3 position64 = position61.cross(position63).normalize();
            vec3Array4[index21] = position61;
            vec3Array5[index21] = position63;
            vec3Array6[index21] = position64;
            position58 = position63;
         }

         float amount203 = value48 / currentValue;
         float amount204 = amount203 + currentValue58;
         byte amount205 = 6;
         float amount206 = amount200;

         for (int index22 = 0; index22 <= value49; index22++) {
            float amount207 = Mth.clamp(amount202 - index22, 0.0F, 1.0F);
            if (amount207 <= currentValue489) {
               break;
            }

            float size4 = amount206 * getTextureTarget211(index22, items5.size());
            float size5 = amount206 * getTextureTarget211(index22 + 1, items5.size());
            if (amount207 < 1.0F) {
               size5 = Mth.lerp(amount207, size4, size5);
            }

            Vec3 position65 = (Vec3)items5.get(index22);
            Vec3 position66 = (Vec3)items5.get(index22 + 1);
            if (amount207 < 1.0F) {
               position66 = position65.lerp(position66, amount207);
            }

            Vec3 position67 = vec3Array5[index22];
            Vec3 position68 = vec3Array6[index22];
            Vec3 position69 = index22 + 1 < vec3Array5.length ? vec3Array5[index22 + 1] : vec3Array5[index22];
            Vec3 position70 = index22 + 1 < vec3Array6.length ? vec3Array6[index22 + 1] : vec3Array6[index22];
            if (amount207 < 1.0F) {
               position69 = position67.lerp(position69, amount207).normalize();
               position70 = position68.lerp(position70, amount207).normalize();
            }

            float amount208 = (float)index22 / size3 * currentValue1053 + amount201;
            float amount209 = (index22 + amount207) / size3 * currentValue73 + amount201;
            int value50 = amount207 < 1.0F ? getStateActive837(value47, amount207) : value47;

            for (int index23 = 0; index23 < 6; index23++) {
               float amount210 = (float)(currentValue622 * index23 / currentValue1002);
               float amount211 = (float)(currentValue640 * (index23 + 1) / currentValue984);
               float amount212 = (float)Math.cos(amount210);
               float amount213 = (float)Math.sin(amount210);
               float amount214 = (float)Math.cos(amount211);
               float amount215 = (float)Math.sin(amount211);
               Vec3 position71 = position65.add(position67.scale(amount212 * size4)).add(position68.scale(amount213 * size4));
               Vec3 position72 = position65.add(position67.scale(amount214 * size4)).add(position68.scale(amount215 * size4));
               Vec3 position73 = position66.add(position69.scale(amount214 * size5)).add(position70.scale(amount215 * size5));
               Vec3 position74 = position66.add(position69.scale(amount212 * size5)).add(position70.scale(amount213 * size5));
               float amount216 = Mth.lerp(index23 / currentValue774, amount203, amount204);
               float amount217 = Mth.lerp((index23 + 1) / currentValue39, amount203, amount204);
               getTextureTarget28(bufferBuilder21, matrix4f12, (float)position71.x, (float)position71.y, (float)position71.z, amount208, amount216, value47);
               getTextureTarget28(bufferBuilder21, matrix4f12, (float)position72.x, (float)position72.y, (float)position72.z, amount208, amount217, value47);
               getTextureTarget28(bufferBuilder21, matrix4f12, (float)position73.x, (float)position73.y, (float)position73.z, amount209, amount217, value50);
               getTextureTarget28(bufferBuilder21, matrix4f12, (float)position74.x, (float)position74.y, (float)position74.z, amount209, amount216, value50);
            }
         }
      }
   }

   private static void getStateActive88(
      BufferBuilder bufferBuilder22, Matrix4f matrix4f13, float amount218, float amount219, float amount220, float amount221, float amount222, float amount223, float amount224, float amount225, float amount226, int value51
   ) {
      Vector4f vector4f2 = ColorMath.getVector4f(value51);
      addColoredVertex(bufferBuilder22, matrix4f13, amount218, amount219, amount220, currentValue1054, currentValue850, vector4f2);
      addColoredVertex(bufferBuilder22, matrix4f13, amount221, amount222, amount223, currentValue1188, currentValue1176, vector4f2);
      addColoredVertex(bufferBuilder22, matrix4f13, amount224, amount225, amount226, currentValue855, currentValue230, vector4f2);
      addColoredVertex(bufferBuilder22, matrix4f13, amount224, amount225, amount226, currentValue1067, currentValue392, vector4f2);
   }

   private static int getStateActive86(int value52, int value53, int value54, float amount227, float amount228) {
      int color5 = ClickGuiRenderer.checkCondition2() ? ColorMath.themeGradientColor(value54) : getStateActive820(value52, value53, value54, false);
      int value55 = getTextureTarget22(color5, 1.0F - amount228);
      if (amount227 > 0.0F) {
         int color6 = ColorMath.animatedGradientColor(stateCounter21, -29556, value54, 4);
         value55 = ColorMath.interpolateRgba(value55, color6, amount227);
      }

      return value55;
   }

   private static void onDeltaTrackerPoseStack(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent7, Vec3 position75, LivingEntity entity8, int value56, float amount229, float amount230, boolean matches2) {
      if (!(amount229 <= currentValue759)) {
         if (stateCounter40 != entity8.getId()) {
            stateCounter40 = entity8.getId();
            currentValue256 = 1.0F;
            currentValue505 = 0.0F;
            stateActive5 = false;
         }

         boolean matches3 = matches2 && entity8.hurtTime > 7;
         if (matches3 && !stateActive5) {
            currentValue256 = 0.0F;
            stateActive5 = true;
         } else if (!matches3) {
            stateActive5 = false;
         }

         if (currentValue256 < 1.0F) {
            currentValue256 = Math.min(1.0F, currentValue256 + amount230 / 1.0F);
         }

         currentValue505 = getStateActive824(
            currentValue505, getStateActive852(currentValue256), amount230 / currentValue531
         );
         float amount231 = currentValue505;
         currentValue378 = currentValue378 + amount230 * amount231 * currentValue508;
         Vec3 position76 = position75.subtract(TargetEspPreviewContext.camera().position());
         double amount232 = position76.x;
         double amount233 = position76.y + entity8.getBbHeight() * currentValue1132;
         double amount234 = position76.z;
         long nowMillis5 = System.currentTimeMillis();
         int color7 = ColorMath.interpolateRgba(value56, -1, amount231 * currentValue1081);
         int value57 = getStateActive837(color7, Math.min(1.0F, amount229 * (1.0F + amount231)));
         int value58 = getStateActive837(color7, amount229 * currentValue225);
         double amount235 = currentValue382 * (1.0 + currentValue1103 * amount231);
         float amount236 = 1.0F * (1.0F + currentValue112 * amount231);
         Matrix4f matrix4f14 = deltaTrackerPoseStackEvent7.getPoseStack().last().pose();
         BufferBuilder bufferBuilder23 = getBufferBuilder3();

         for (int index24 = 0; index24 < 3; index24++) {
            getStateActive612(bufferBuilder23, matrix4f14, amount232, amount233, amount234, nowMillis5, amount235, amount236, amount231, value57, value58, index24);
         }

         if (amount231 > currentValue119) {
            getTextureTarget27(bufferBuilder23, matrix4f14, amount232, amount233, amount234, color7, amount229 * amount231);
            addCameraFacingSprite(
               bufferBuilder23, matrix4f14, amount232, amount233, amount234, currentValue1177 * amount231, getStateActive837(color7, amount229 * amount231 * currentValue866)
            );
         }

         updateState9(bufferBuilder23, resourceIdentifier4, true);
      }
   }

   private static void onDeltaTrackerPoseStack9(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent8, Vec3 position77, LivingEntity entity9, int value59, float amount237, long value60, boolean matches4) {
      list.removeIf(f0nIo1tGX54h6o8A -> value60 - f0nIo1tGX54h6o8A.lastUpdateMillis51 > f0nIo1tGX54h6o8A.lastUpdateMillis42);
      list.removeIf(f0nIo1tGX54h6o8A2 -> f0nIo1tGX54h6o8A2.currentValue124 < 1.0F);
      if (matches4) {
         while (list.size() < 2) {
            list.add(getStateActive840(value60 - random5.nextInt(120)));
         }

         if (value60 - lastUpdateMillis9 >= lastUpdateMillis22 && list.size() < 3) {
            list.add(getStateActive840(value60));
            lastUpdateMillis9 = value60;
         }
      }

      if (!list.isEmpty() && !(amount237 <= currentValue366)) {
         Vec3 position78 = position77.subtract(TargetEspPreviewContext.camera().position());
         float amount238 = Math.max(entity9.getBbHeight(), currentValue511);
         float amount239 = Math.max(entity9.getBbWidth() * currentValue636, currentValue169);
         float amount240 = amount239 + currentValue966;
         Matrix4f matrix4f15 = deltaTrackerPoseStackEvent8.getPoseStack().last().pose();
         PoseStack poseStack7 = deltaTrackerPoseStackEvent8.getPoseStack();
         int value61 = getTextureTarget22(value59, currentValue524);
         int value62 = getTextureTarget22(value59, currentValue29);
         ArrayList items6 = new ArrayList(list.size());
         float[] instance2 = new float[list.size()];

         for (int index25 = 0; index25 < list.size(); index25++) {
            TargetEspRenderer.F0nIo1tGX54h6o8A f0nIo1tGX54h6o8A3 = list.get(index25);
            float amount241 = calculateValue10(f0nIo1tGX54h6o8A3, value60, amount237);
            instance2[index25] = amount241;
            ArrayList items7 = new ArrayList(28);
            if (amount241 > currentValue126) {
               int value63 = (int)((value60 - f0nIo1tGX54h6o8A3.lastUpdateMillis51) / lastUpdateMillis40);
               updateState16(f0nIo1tGX54h6o8A3, position78, amount238, amount240, value63, items7);
            }

            items6.add(items7);
         }

         BufferBuilder bufferBuilder24 = getBufferBuilder3();

         for (int index26 = 0; index26 < list.size(); index26++) {
            float amount242 = instance2[index26];
            List items8 = (List)items6.get(index26);
            if (!(amount242 <= currentValue751) && items8.size() >= 2) {
               TargetEspRenderer.F0nIo1tGX54h6o8A f0nIo1tGX54h6o8A4 = list.get(index26);

               for (int index27 = 0; index27 < items8.size(); index27++) {
                  Vec3 position79 = (Vec3)items8.get(index27);
                  float size6 = getTextureTarget211(index27, items8.size()) * f0nIo1tGX54h6o8A4.currentValue374;
                  updateState13(
                     bufferBuilder24, poseStack7, position79.x, position79.y, position79.z, currentValue400 * size6, getStateActive837(value59, amount242 * currentValue1037)
                  );
                  updateState13(
                     bufferBuilder24, poseStack7, position79.x, position79.y, position79.z, currentValue165 * size6, getStateActive837(value61, amount242 * currentValue1129)
                  );
               }
            }
         }

         getStateActive67(bufferBuilder24, resourceIdentifier8, false, renderPipeline26);
         BufferBuilder bufferBuilder25 = getBufferBuilder3();

         for (int index28 = 0; index28 < list.size(); index28++) {
            float amount243 = instance2[index28];
            List items9 = (List)items6.get(index28);
            if (!(amount243 <= currentValue1134) && items9.size() >= 2) {
               float amount244 = currentValue865 * list.get(index28).currentValue374;
               getStateActive84(bufferBuilder25, matrix4f15, items9, amount244 * currentValue393, getStateActive837(value59, amount243 * currentValue295));
               getStateActive84(bufferBuilder25, matrix4f15, items9, amount244 * currentValue904, getStateActive837(value61, amount243 * currentValue1003));
            }
         }

         getStateActive67(bufferBuilder25, resourceIdentifier4, false, renderPipeline26);
         BufferBuilder bufferBuilder26 = getBufferBuilder3();

         for (int index29 = 0; index29 < list.size(); index29++) {
            float amount245 = instance2[index29];
            List items10 = (List)items6.get(index29);
            if (!(amount245 <= currentValue722) && items10.size() >= 2) {
               float amount246 = currentValue137 * list.get(index29).currentValue374;
               getStateActive84(bufferBuilder26, matrix4f15, items10, amount246, getStateActive837(value62, amount245));
               getStateActive84(bufferBuilder26, matrix4f15, items10, amount246 * currentValue948, getStateActive837(-1, amount245));
            }
         }

         getStateActive67(bufferBuilder26, resourceIdentifier4, false, renderPipeline26);
      }
   }

   private static void onDeltaTrackerPoseStack19(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent9, Vec3 position80, LivingEntity entity10, int value64, int value65, float amount247, long value66) {
      position80 = position80.subtract(TargetEspPreviewContext.camera().position());
      getStateActive860(entity10);
      byte amount248 = 12;
      byte amount249 = 20;
      int value67 = amount248 + amount249;
      float amount250 = (float)(value66 % lastUpdateMillis38) / currentValue768;
      float amount251 = (float)(value66 % lastUpdateMillis28) / currentValue384;
      float amount252 = (float)value66 * currentValue81;
      float amount253 = entity10.getBbWidth() * currentValue498;
      float amount254 = currentValue442 * (currentValue613 + currentValue684 * amount247);
      float amount255 = entity10.getBbHeight();
      float amount256 = currentValue322 - currentValue671 * amount247;
      PoseStack poseStack8 = deltaTrackerPoseStackEvent9.getPoseStack();
      BufferBuilder bufferBuilder27 = getBufferBuilder3();

      for (int index30 = 0; index30 < value67; index30++) {
         TargetEspRenderer.CGX9upVJA0hI2PBi cgx9upvja0hi2pbi = getStateActive846(index30, amount248, amount249, amount250, amount251, amount252, amount253, amount255, amount254, amount256);
         float size7 = currentValue289 + cgx9upvja0hi2pbi.size() * currentValue776;
         float size8 = currentValue596 + cgx9upvja0hi2pbi.size() * currentValue408;
         updateState13(
            bufferBuilder27,
            poseStack8,
            position80.x + cgx9upvja0hi2pbi.getX(),
            position80.y + cgx9upvja0hi2pbi.getY(),
            position80.z + cgx9upvja0hi2pbi.getZ2(),
            size7,
            getStateActive837(value64, amount247 * currentValue222)
         );
         updateState13(
            bufferBuilder27,
            poseStack8,
            position80.x + cgx9upvja0hi2pbi.getX(),
            position80.y + cgx9upvja0hi2pbi.getY(),
            position80.z + cgx9upvja0hi2pbi.getZ2(),
            size8,
            getStateActive837(getTextureTarget22(value64, currentValue278), amount247 * currentValue1115)
         );
      }

      updateState9(bufferBuilder27, resourceIdentifier8, true);
      int color8 = ColorMath.packRgba(
         ColorMath.redChannel(value64),
         ColorMath.greenChannel(value64),
         ColorMath.blueChannel(value64),
         Math.round(currentValue88 * amount247)
      );
      int color9 = ColorMath.packRgba(
         ColorMath.redChannel(value64),
         ColorMath.greenChannel(value64),
         ColorMath.blueChannel(value64),
         Math.round(currentValue85 * amount247)
      );
      BufferBuilder bufferBuilder28 = getBufferBuilder3();
      poseStack8.pushPose();
      poseStack8.translate(position80.x, position80.y, position80.z);

      for (int index31 = 0; index31 < value67; index31++) {
         TargetEspRenderer.CGX9upVJA0hI2PBi cgx9upvja0hi2pbi2 = getStateActive846(index31, amount248, amount249, amount250, amount251, amount252, amount253, amount255, amount254, amount256);
         getStateActive828(
            bufferBuilder28,
            poseStack8,
            cgx9upvja0hi2pbi2.getX(),
            cgx9upvja0hi2pbi2.getY(),
            cgx9upvja0hi2pbi2.getZ2(),
            cgx9upvja0hi2pbi2.size() * currentValue373,
            color9,
            value65,
            cgx9upvja0hi2pbi2.spin(),
            index31,
            amount255,
            true
         );
         getStateActive828(
            bufferBuilder28,
            poseStack8,
            cgx9upvja0hi2pbi2.getX(),
            cgx9upvja0hi2pbi2.getY(),
            cgx9upvja0hi2pbi2.getZ2(),
            cgx9upvja0hi2pbi2.size(),
            color8,
            value65,
            cgx9upvja0hi2pbi2.spin(),
            index31,
            amount255,
            false
         );
      }

      poseStack8.popPose();
      getStateActive611(bufferBuilder28, renderPipeline23);
   }

   private static void getStateActive841(BufferBuilder bufferBuilder29, Matrix4f matrix4f16, float amount257, int value68) {
      TargetLightingCompositor.updateState2(matrix4f16, 0.0F, 0.0F, 0.0F, amount257 * 2.0F, value68);
      Vector4f vector4f3 = ColorMath.getVector4f(value68);
      addColoredVertex(bufferBuilder29, matrix4f16, -amount257, -amount257, 0.0F, 1.0F, 1.0F, vector4f3);
      addColoredVertex(bufferBuilder29, matrix4f16, amount257, -amount257, 0.0F, 0.0F, 1.0F, vector4f3);
      addColoredVertex(bufferBuilder29, matrix4f16, amount257, amount257, 0.0F, 0.0F, 0.0F, vector4f3);
      addColoredVertex(bufferBuilder29, matrix4f16, -amount257, amount257, 0.0F, 1.0F, 0.0F, vector4f3);
   }

   private static void getStateActive819(PoseStack poseStack9, int value69, float amount258, long value70) {
      byte amount259 = 5;
      float[] instance3 = new float[]{currentValue176, currentValue423};
      float[] instance4 = new float[]{currentValue1070, currentValue74};
      float[] instance5 = new float[]{0.0F, currentValue763};
      float[][][] instance6 = new float[6][][];

      for (int index32 = 0; index32 <= 5; index32++) {
         float amount260 = index32 / currentValue21;
         float amount261 = instance3[0] + (instance5[0] - instance3[0]) * amount260;
         float amount262 = instance3[1] + (instance5[1] - instance3[1]) * amount260;
         float amount263 = instance4[0] + (instance5[0] - instance4[0]) * amount260;
         float amount264 = instance4[1] + (instance5[1] - instance4[1]) * amount260;
         int value71 = 5 - index32;
         instance6[index32] = new float[value71 + 1][2];

         for (int index33 = 0; index33 <= value71; index33++) {
            float amount265 = value71 == 0 ? 0.0F : (float)index33 / value71;
            instance6[index32][index33][0] = amount261 + (amount263 - amount261) * amount265;
            instance6[index32][index33][1] = amount262 + (amount264 - amount262) * amount265;
         }
      }

      ArrayList items11 = new ArrayList();

      for (int index34 = 0; index34 < 5; index34++) {
         int value72 = 5 - index34;

         for (int index35 = 0; index35 < value72; index35++) {
            items11.add(new float[][]{instance6[index34][index35], instance6[index34][index35 + 1], instance6[index34 + 1][index35]});
            if (index35 < value72 - 1) {
               items11.add(new float[][]{instance6[index34][index35 + 1], instance6[index34 + 1][index35 + 1], instance6[index34 + 1][index35]});
            }
         }
      }

      Matrix4f matrix4f17 = poseStack9.last().pose();
      int color10 = ColorMath.blendRgba(value69, -1, currentValue616);
      BufferBuilder bufferBuilder30 = getBufferBuilder3();

      for (int index36 = 0; index36 < items11.size(); index36++) {
         float[][] instance7 = (float[][])items11.get(index36);
         float amount266 = (instance7[0][0] + instance7[1][0] + instance7[2][0]) / currentValue414;
         float amount267 = (instance7[0][1] + instance7[1][1] + instance7[2][1]) / currentValue1194;
         float[] instance8 = getStateActive85(amount266, value70);
         float amount268 = instance8[0];
         float amount269 = instance8[1];
         float amount270 = instance8[2];
         float amount271 = instance8[3];
         Matrix4f matrix4f18 = getStateActive825(matrix4f17, amount266, amount267, amount268, amount269);
         int value73 = getStateActive854(amount267, index36, value69);
         if (amount270 > 0.0F) {
            value73 = ColorMath.blendRgba(value73, -1, amount270 * currentValue643);
         }

         if (amount271 > 0.0F) {
            value73 = ColorMath.blendRgba(value73, color10, Mth.clamp(amount271 * currentValue430, 0.0F, 1.0F));
         }

         getStateActive88(
            bufferBuilder30, matrix4f18, instance7[0][0], instance7[0][1], 0.0F, instance7[1][0], instance7[1][1], 0.0F, instance7[2][0], instance7[2][1], 0.0F, getStateActive837(value73, amount258)
         );
      }

      getStateActive857(bufferBuilder30, renderPipeline12, true);
      ArrayList items12 = new ArrayList();

      for (int index37 = 0; index37 < items11.size(); index37++) {
         float[][] instance9 = (float[][])items11.get(index37);
         float amount272 = (instance9[0][0] + instance9[1][0] + instance9[2][0]) / currentValue762;
         if (getStateActive85(amount272, value70)[2] > currentValue224) {
            items12.add(index37);
         }
      }

      if (!items12.isEmpty()) {
         BufferBuilder bufferBuilder31 = GpuResourceScope.getGpuResourceScope()
            .getBufferBuilder(PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

         for (int index38 : (Iterable<Integer>)(Iterable<?>)(items12)) {
            float[][] instance10 = (float[][])items11.get(index38);
            float amount273 = (instance10[0][0] + instance10[1][0] + instance10[2][0]) / currentValue483;
            float amount274 = (instance10[0][1] + instance10[1][1] + instance10[2][1]) / currentValue538;
            float[] instance11 = getStateActive85(amount273, value70);
            Matrix4f matrix4f19 = getStateActive825(matrix4f17, amount273, amount274, instance11[0], instance11[1]);
            int color11 = getStateActive837(
               ColorMath.blendRgba(value69, -1, currentValue720),
               (currentValue1136 + currentValue877 * instance11[2]) / currentValue917 * amount258
            );
            Vector4f vector4f4 = ColorMath.getVector4f(color11);
            getStateActive826(bufferBuilder31, matrix4f19, instance10[0][0], instance10[0][1], 0.0F, instance10[1][0], instance10[1][1], 0.0F, vector4f4);
            getStateActive826(bufferBuilder31, matrix4f19, instance10[1][0], instance10[1][1], 0.0F, instance10[2][0], instance10[2][1], 0.0F, vector4f4);
            getStateActive826(bufferBuilder31, matrix4f19, instance10[2][0], instance10[2][1], 0.0F, instance10[0][0], instance10[0][1], 0.0F, vector4f4);
         }

         getStateActive842(bufferBuilder31, renderPipeline18);
      }
   }

   private static void onDeltaTrackerPoseStack8(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent10, Vec3 position81, LivingEntity entity11, int value74, int value75, float amount275, float amount276) {
      Vec3 position82 = TargetEspPreviewContext.camera().position();
      float amount277 = entity11.getBbHeight();
      long value76 = lastUpdateMillis37;
      double nowMillis6 = System.currentTimeMillis() % value76;
      boolean matches5 = nowMillis6 > value76 / currentValue1160;
      float amount278 = (float)(nowMillis6 / (value76 / currentValue1151));
      if (matches5) {
         amount278--;
      } else {
         amount278 = 1.0F - amount278;
      }

      amount278 = amount278 < currentValue232
         ? 2.0F * amount278 * amount278
         : (float)(1.0 - Math.pow(currentValue881 * amount278 + currentValue893, currentValue344) / currentValue1064);
      double amount279 = amount277 / currentValue239 * (amount278 > currentValue1036 ? 1.0 - amount278 : amount278) * (matches5 ? -1 : 1);
      long nowNanos = System.nanoTime();
      float amount280 = (float)(nowNanos - lastUpdateMillis32) / currentValue798;
      lastUpdateMillis32 = nowNanos;
      float amount281 = entity11.hurtTime <= 0 ? 0.0F : (entity11.hurtTime - amount276) / currentValue16;
      currentValue920 = currentValue920 + Math.max(0.0F, amount281) * amount280;
      float nowMillis7 = (float)(System.currentTimeMillis() - lastUpdateMillis13) / currentValue574 + currentValue920;
      BufferBuilder bufferBuilder32 = getBufferBuilder3();
      PoseStack poseStack10 = deltaTrackerPoseStackEvent10.getPoseStack();
      poseStack10.pushPose();
      poseStack10.translate(position81.x - position82.x, position81.y - position82.y + amount277 * amount278 + amount279, position81.z - position82.z);
      float amount282 = currentValue1056;

      for (int index39 = 0; index39 < 4; index39++) {
         for (int index40 = 0; index40 < 15; index40++) {
            poseStack10.pushPose();
            float amount283 = index40 / currentValue331;
            float amount284 = (currentValue71 * (1.0F - amount283) + currentValue743 * amount283) * amount275 * currentValue28;
            float amount285 = currentValue845 * (nowMillis7 - index40 * currentValue942) / currentValue659;
            boolean matches6 = amount283 < currentValue160;
            float amount286 = matches6 ? amount283 * 2.0F : (1.0F - amount283) * 2.0F;
            double amount287 = Math.sin(amount286 * currentValue217) * currentValue999;
            Random random = new Random(index40 * lastUpdateMillis7);
            double amount288 = (random.nextDouble() - currentValue699) * amount287;
            double amount289 = (random.nextDouble() - currentValue869) * amount287;
            double amount290 = (random.nextDouble() - currentValue729) * amount287;
            amount288 = amount288 * amount275 - amount288;
            amount289 = amount289 * amount275 - amount289;
            amount290 = amount290 * amount275 - amount290;
            switch (index39) {
               case 0:
                  poseStack10.translate(Math.cos(amount285) * amount282 + amount288, amount289, Math.sin(amount285) * amount282 + amount290);
                  break;
               case 1:
                  poseStack10.translate(-Math.sin(amount285) * amount282 + amount288, amount289, Math.cos(amount285) * amount282 + amount290);
                  break;
               case 2:
                  poseStack10.translate(-Math.cos(amount285) * amount282 + amount288, amount289, -Math.sin(amount285) * amount282 + amount290);
                  break;
               default:
                  poseStack10.translate(Math.sin(amount285) * amount282 + amount288, amount289, -Math.cos(amount285) * amount282 + amount290);
            }

            int value77 = getTextureTarget22(getTextureTarget29(value74, value75, index40 * currentValue181), currentValue838);
            int value78 = getStateActive837(value77, amount275 * currentValue1156);
            poseStack10.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
            getStateActive841(bufferBuilder32, poseStack10.last().pose(), amount284, getStateActive832(value78));
            poseStack10.popPose();
         }
      }

      poseStack10.popPose();
      getStateActive831(bufferBuilder32, resourceIdentifier6);
   }

   private static void getStateActive857(BufferBuilder bufferBuilder33, RenderPipeline renderPipeline3, boolean matches7) {
      MeshData meshData2 = bufferBuilder33.build();
      if (meshData2 != null) {
         GpuBuffer gpuBuffer3 = null;

         try {
            DrawState drawState2 = meshData2.drawState();
            int value79 = drawState2.indexCount();
            AutoStorageIndexBuffer autoStorageIndexBuffer2 = RenderSystem.getSequentialBuffer(drawState2.primitiveTopology());
            GpuBuffer gpuBuffer4 = autoStorageIndexBuffer2.getBuffer(value79);
            IndexType indexType2 = autoStorageIndexBuffer2.type();
            gpuBuffer3 = RenderSystem.getDevice().createBuffer(() -> string93, 32, meshData2.vertexBuffer());
            if (matches7 && stateActive3) {
               DeadChamsCompositeShader.deadChamsCompositeShader.updateState5(gpuBuffer3, gpuBuffer4, indexType2, value79, currentValue908);
            }

            RenderTarget renderTarget2 = TargetEspPreviewContext.target();
            CommandEncoder commandEncoder2 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass renderPass2 = commandEncoder2.createRenderPass(
               () -> string, renderTarget2.getColorTextureView(), Optional.empty(), renderTarget2.getDepthTextureView(), OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(renderPass2);
               if (RenderPipelineCache.isCompiled(renderPipeline3)) {
                  renderPass2.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline3));
                  renderPass2.setVertexBuffer(0, gpuBuffer3.slice());
                  renderPass2.setIndexBuffer(gpuBuffer4, indexType2);
                  renderPass2.drawIndexed(value79, 1, 0, 0, 0);
               }
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
         } finally {
            if (gpuBuffer3 != null) {
               gpuBuffer3.close();
            }

            meshData2.close();
         }
      }
   }

   private static void addBillboardQuad(BufferBuilder bufferBuilder34, Matrix4f matrix4f20, float amount291, int value80) {
      TargetLightingCompositor.updateState2(matrix4f20, 0.0F, 0.0F, 0.0F, amount291, value80);
      Vector4f vector4f5 = ColorMath.getVector4f(value80);
      float amount292 = amount291 * currentValue363;
      addColoredVertex(bufferBuilder34, matrix4f20, -amount292, -amount292, 0.0F, 0.0F, 1.0F, vector4f5);
      addColoredVertex(bufferBuilder34, matrix4f20, -amount292, amount292, 0.0F, 0.0F, 0.0F, vector4f5);
      addColoredVertex(bufferBuilder34, matrix4f20, amount292, amount292, 0.0F, 1.0F, 0.0F, vector4f5);
      addColoredVertex(bufferBuilder34, matrix4f20, amount292, -amount292, 0.0F, 1.0F, 1.0F, vector4f5);
   }

   private static TargetEspRenderer.SjzMjyMYX7ylg4dN getStateActive850(LivingEntity entity12, Vec3 position83, long value81, float amount293) {
      double amount294 = Math.max((double)entity12.getBbHeight(), currentValue795);
      double amount295 = Math.max(entity12.getBbWidth() * currentValue87, currentValue167)
         + currentValue796
         + random5.nextDouble() * currentValue365;
      int value82 = random5.nextBoolean() ? 1 : -1;
      float amount296 = currentValue253 + random5.nextFloat() * currentValue436;
      float amount297 = amount293 * (currentValue196 + random5.nextFloat() * currentValue723)
         + random5.nextFloat() * currentValue656 * 2.0F;
      float amount298 = random5.nextFloat() * currentValue261;
      float amount299 = Math.min(1.0F, amount298 + currentValue1186 + random5.nextFloat() * currentValue990);
      Vec3 position84 = getStateActive823(position83, amount294, amount295, amount297, amount296, value82, amount298);
      Vec3 position85 = getStateActive823(position83, amount294, amount295, amount297, amount296, value82, amount299);
      double amount300 = position84.distanceTo(position85);
      List items13 = getStateActive614(position84, position85, 3, Math.max(currentValue1076, amount300 * currentValue501));
      ArrayList items14 = new ArrayList(items13.size());

      for (Vec3 position86 : (Iterable<Vec3>)(Iterable<?>)(items13)) {
         items14.add(position86.subtract(position83));
      }

      float amount301 = currentValue571 + random5.nextFloat() * currentValue810;
      return new TargetEspRenderer.SjzMjyMYX7ylg4dN(
         items14,
         value81,
         System.nanoTime(),
         (long)(520 + random5.nextInt(260)),
         random5.nextFloat() * currentValue155,
         currentValue909 + random5.nextFloat() * currentValue481,
         value82,
         amount301
      );
   }

   public static void renderQueuedTargetEsp() {
      stateActive13 = false;
      if (deltaTrackerPoseStackEvent28 != null && string14 != null) {
         GpuBufferSlice gpuBufferSlice = RenderSystem.getProjectionMatrixBuffer();
         ProjectionType projectionType = RenderSystem.getProjectionType();
         DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent11 = deltaTrackerPoseStackEvent28;
         LivingEntity entity13 = livingEntity2;
         String text2 = string14;
         int value83 = stateCounter43;
         int value84 = stateCounter32;
         deltaTrackerPoseStackEvent28 = null;
         livingEntity2 = null;
         string14 = null;

         try {
            RenderSystem.setProjectionMatrix(gpuBufferSlice2, projectionType2);
            renderTargetEsp(deltaTrackerPoseStackEvent11, entity13, text2, value83, value84);
            if (stateActive8) {
               getStateActive830();
            }
         } finally {
            RenderSystem.setProjectionMatrix(gpuBufferSlice, projectionType);
         }
      }
   }

   /** Drop the previous target immediately when Aura stops supplying one. */
   public static void clearTargetEffects() {
      deltaTrackerPoseStackEvent28 = null;
      livingEntity2 = null;
      livingEntity = null;
      string14 = null;
      stateActive13 = false;
      stateActive8 = false;
      currentValue629 = 0.0F;
      list.clear();
      list2.clear();
      list3.clear();
      list4.clear();
      list5.clear();
      list9.clear();
      linkedList3.clear();
   }

   private static float getStateActive859(int value85, float amount302) {
      float amount303 = (float)Math.sin(value85 * currentValue1020 + amount302 * currentValue542);
      amount303 *= currentValue431;
      return amount303 - (float)Math.floor(amount303);
   }

   private static TargetEspRenderer.CGX9upVJA0hI2PBi getStateActive846(
      int value86, int value87, int value88, float amount304, float amount305, float amount306, float amount307, float amount308, float amount309, float amount310
   ) {
      if (value86 >= value87) {
         int value89 = value86 - value87;
         int size9 = value89 * list2.size() / value88;
         Vector3f position87 = list2.get(size9);
         float angleRadians7 = (float)Math.toRadians(amount305);
         float amount311 = Mth.sin(angleRadians7);
         float amount312 = Mth.cos(angleRadians7);
         float amount313 = position87.x * amount312 - position87.z * amount311;
         float amount314 = position87.x * amount311 + position87.z * amount312;
         float amount315 = Mth.sqrt(amount313 * amount313 + amount314 * amount314);
         float amount316 = amount307 * currentValue1047;
         float amount317 = Math.max(amount315 * currentValue268, amount316);
         if (amount315 > currentValue325) {
            float amount318 = amount317 / amount315;
            amount313 *= amount318;
            amount314 *= amount318;
         }

         amount313 *= amount310;
         amount314 *= amount310;
         float amount319 = position87.y + (float)Math.sin(amount306 * currentValue939 + value89 * currentValue466) * currentValue371;
         float amount320 = calculateValue(value89, currentValue724);
         float amount321 = amount309 * (currentValue559 + amount320 * currentValue166);
         float amount322 = amount305 * (value89 % 2 == 0 ? currentValue985 : currentValue1137) + value89 * currentValue386;
         return new TargetEspRenderer.CGX9upVJA0hI2PBi(amount313, amount319, amount314, amount321, amount322);
      } else {
         float amount323 = currentValue637 * value86 / value87;
         float angleRadians8 = (float)Math.toRadians(amount323 + amount304);
         float amount324 = currentValue59 + Math.abs((float)Math.sin(angleRadians8 * 2.0F)) * currentValue488;
         float amount325 = amount307 * amount324 * amount310;
         float amount326 = (float)Math.sin(angleRadians8) * amount325;
         float amount327 = (float)Math.cos(angleRadians8) * amount325;
         float amount328 = currentValue852 + Math.abs((float)Math.sin(angleRadians8 * currentValue460 + amount304 * currentValue244));
         float amount329 = amount308 * (currentValue471 + currentValue389 * amount328);
         float amount330 = amount309 * (currentValue1184 + Math.abs((float)Math.sin(angleRadians8 * currentValue831)) * currentValue7);
         float amount331 = amount304 * currentValue918 + amount323 * currentValue548;
         return new TargetEspRenderer.CGX9upVJA0hI2PBi(amount326, amount329, amount327, amount330, amount331);
      }
   }

   private static Vec3[] getStateActive811(Vec3 position88) {
      Vec3 position89 = Math.abs(position88.y) > currentValue1024 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 position90 = position89.cross(position88).normalize();
      Vec3 position91 = position88.cross(position90).normalize();
      return new Vec3[]{position90, position91};
   }

   private static TargetEspRenderer.vbgKRi4F34zTzWmv getStateActive834(LivingEntity entity14, Vec3 position92, long value90) {
      double amount332 = Math.max((double)entity14.getBbHeight(), currentValue251);
      double amount333 = Math.max(entity14.getBbWidth() * currentValue861, currentValue872);
      double amount334 = random5.nextDouble() * currentValue794 * currentValue1021;
      double amount335 = amount333 * (currentValue212 + random5.nextDouble() * currentValue1026);
      double amount336 = amount332 * (currentValue419 + random5.nextDouble() * currentValue1111);
      Vec3 position93 = position92.add(Math.cos(amount334) * amount335, amount336, Math.sin(amount334) * amount335);
      double amount337 = amount334 + (random5.nextDouble() - currentValue785) * currentValue807;
      double amount338 = amount333 + currentValue114 + random5.nextDouble() * currentValue552;
      double amount339 = amount332 + currentValue433 + random5.nextDouble() * currentValue152;
      Vec3 position94 = position92.add(Math.cos(amount337) * amount338, amount339, Math.sin(amount337) * amount338);
      ArrayList items15 = new ArrayList();
      List items16 = getStateActive858(position94, position93, 4, currentValue843 + random5.nextDouble() * currentValue310);
      items15.add(new TargetEspRenderer.FDRJZV2OjTT35QNI(items16, 1.0F, 0.0F));
      int value91 = 1 + random5.nextInt(3);

      for (int index41 = 0; index41 < value91; index41++) {
         int size10 = 3 + random5.nextInt(Math.max(1, items16.size() - 6));
         Vec3 position95 = (Vec3)items16.get(size10);
         Vec3 position96 = position93.subtract(position94);
         if (position96.lengthSqr() < currentValue43) {
            position96 = new Vec3(0.0, currentValue556, 0.0);
         }

         position96 = position96.normalize();
         Vec3 position97 = position96.cross(new Vec3(0.0, 1.0, 0.0));
         if (position97.lengthSqr() < currentValue337) {
            position97 = new Vec3(1.0, 0.0, 0.0);
         }

         position97 = position97.normalize();
         Vec3 position98 = position97.scale(random5.nextBoolean() ? 1.0 : currentValue76)
            .add(0.0, random5.nextDouble() * currentValue608 - currentValue385, 0.0)
            .normalize();
         double amount340 = currentValue882 + random5.nextDouble() * 1.0;
         Vec3 position99 = position95.add(position98.scale(amount340)).add(position96.scale(currentValue653 + random5.nextDouble() * currentValue269));
         float size11 = Mth.clamp((float)size10 / (items16.size() - 1) * currentValue248, currentValue62, currentValue625);
         items15.add(
            new TargetEspRenderer.FDRJZV2OjTT35QNI(
               getStateActive858(position95, position99, 3, currentValue287 + random5.nextDouble() * currentValue899),
               currentValue884 + random5.nextFloat() * currentValue811,
               size11
            )
         );
      }

      if (random5.nextFloat() < currentValue731) {
         double amount341 = amount334
            + currentValue1013
               * (currentValue1143 + random5.nextDouble() * currentValue860)
               * (random5.nextBoolean() ? 1 : -1);
         Vec3 position100 = position92.add(
            Math.cos(amount341) * amount333, amount332 * (currentValue583 + random5.nextDouble() * currentValue968), Math.sin(amount341) * amount333
         );
         items15.add(
            new TargetEspRenderer.FDRJZV2OjTT35QNI(
               getStateActive858(position93, position100, 3, currentValue52 + random5.nextDouble() * currentValue1010),
               currentValue953,
               currentValue1154
            )
         );
      }

      return new TargetEspRenderer.vbgKRi4F34zTzWmv(
         items15,
         position93,
         position92,
         value90,
         (long)(560 + random5.nextInt(340)),
         random5.nextFloat() * currentValue294,
         -1,
         0.0,
         currentValue397,
         currentValue60,
         currentValue1074
      );
   }

   private static void getStateActive89(Vec3[] vec3Array7, int value92, int value93, double amount342, Random random2) {
      if (value93 - value92 > 1) {
         int value94 = value92 + value93 >>> 1;
         Vec3 position101 = vec3Array7[value92];
         Vec3 position102 = vec3Array7[value93];
         Vec3 position103 = position101.add(position102).scale(currentValue1174);
         Vec3 position104 = position102.subtract(position101);
         if (position104.lengthSqr() < currentValue219) {
            vec3Array7[value94] = position103;
         } else {
            Vec3 position105 = position104.cross(new Vec3(0.0, 1.0, 0.0));
            if (position105.lengthSqr() < currentValue1110) {
               position105 = position104.cross(new Vec3(1.0, 0.0, 0.0));
            }

            if (position105.lengthSqr() < currentValue32) {
               position105 = new Vec3(1.0, 0.0, 0.0);
            }

            position105 = position105.normalize();
            Vec3 position106 = position104.cross(position105);
            if (position106.lengthSqr() < currentValue56) {
               position106 = new Vec3(0.0, 1.0, 0.0);
            } else {
               position106 = position106.normalize();
            }

            double amount343 = (random2.nextDouble() - currentValue236) * currentValue895 * amount342;
            double amount344 = (random2.nextDouble() - currentValue741) * amount342 * currentValue323;
            vec3Array7[value94] = position103.add(position105.scale(amount343)).add(position106.scale(amount344));
            getStateActive89(vec3Array7, value92, value94, amount342 * currentValue1018, random2);
            getStateActive89(vec3Array7, value94, value93, amount342 * currentValue1146, random2);
         }
      }
   }

   private static void getStateActive845(
      BufferBuilder bufferBuilder35, Matrix4f matrix4f21, Vec3 position107, Vec3 position108, Vec3 position109, Vec3 position110, Vec3 position111, Vec3 position112, double amount345, double amount346, int value95
   ) {
      getStateActive62(bufferBuilder35, matrix4f21, position107, position108, position109, amount346, 3, 6, value95);
      Vec3[] vec3Array8 = new Vec3[]{position110, position111, position112};

      for (Vec3 position113 : vec3Array8) {
         Vec3 position114 = position109.add(position113.scale(amount345));
         getStateActive847(bufferBuilder35, matrix4f21, position107, position108, position109, position114, amount346, 6, value95);
         getStateActive62(bufferBuilder35, matrix4f21, position107, position108, position114, amount346, 3, 6, value95);
      }
   }

   private static void onDeltaTrackerPoseStack7(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent12, Vec3 position115, LivingEntity entity15, int value96, int value97, boolean matches8) {
      long nowMillis8 = System.currentTimeMillis();
      if (matches8 && nowMillis8 - lastUpdateMillis3 >= lastUpdateMillis26) {
         lastUpdateMillis3 = nowMillis8;
         currentValue573 = currentValue573 + currentValue1031;
         double amount347 = currentValue573 % currentValue420;
         float amount348 = currentValue171 + entity15.getBbWidth() * currentValue1112;
         float amount349 = entity15.getBbHeight() / 2.0F;

         for (int index42 = 0; index42 < 4; index42++) {
            float amount350 = (index42 - 1) * currentValue145;
            double amount351 = amount347 + amount350 * currentValue237;
            double angleRadians9 = Math.toRadians(-amount351);
            double amount352 = Math.sin(angleRadians9 + currentValue335) * amount350;
            double amount353 = Math.cos(angleRadians9) * amount348;
            double amount354 = Math.sin(angleRadians9) * amount348;
            int value98 = getStateActive820(value96, value97, (int)amount347 + 90 * index42, false);
            list9.add(new TargetEspRenderer.CefWHVrCHHOwSLC4(position115.x + amount353, position115.y + amount349 + amount352, position115.z + amount354, nowMillis8, value98));
         }
      }

      Vec3 position116 = TargetEspPreviewContext.camera().position();
      BufferBuilder bufferBuilder36 = getBufferBuilder3();
      PoseStack poseStack11 = deltaTrackerPoseStackEvent12.getPoseStack();
      float amount355 = currentValue401;
      Iterator iterator2 = list9.iterator();

      while (iterator2.hasNext()) {
         TargetEspRenderer.CefWHVrCHHOwSLC4 cefWHVrCHHOwSLC4 = (TargetEspRenderer.CefWHVrCHHOwSLC4)iterator2.next();
         float amount356 = (float)(nowMillis8 - cefWHVrCHHOwSLC4.lastUpdateMillis4);
         cefWHVrCHHOwSLC4.updateState19(nowMillis8);
         if (amount356 > currentValue332 && cefWHVrCHHOwSLC4.currentValue93 < currentValue683) {
            iterator2.remove();
         } else {
            float amount357;
            if (amount356 < currentValue270) {
               amount357 = 1.0F - amount356 / currentValue267;
            } else if (amount356 < currentValue172) {
               amount357 = amount356 / currentValue925 - 1.0F;
            } else {
               amount357 = 1.0F;
            }

            amount357 = Mth.clamp(amount357, 0.0F, 1.0F);
            if (!(cefWHVrCHHOwSLC4.currentValue93 <= currentValue903)) {
               poseStack11.pushPose();
               poseStack11.translate(cefWHVrCHHOwSLC4.currentValue312 - position116.x, cefWHVrCHHOwSLC4.currentValue883 - position116.y, cefWHVrCHHOwSLC4.currentValue68 - position116.z);
               poseStack11.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
               getStateActive841(
                  bufferBuilder36,
                  poseStack11.last().pose(),
                  amount355 * amount357,
                  getStateActive832(getStateActive837(cefWHVrCHHOwSLC4.stateCounter48, cefWHVrCHHOwSLC4.currentValue93))
               );
               poseStack11.popPose();
            }
         }
      }

      getStateActive831(bufferBuilder36, resourceIdentifier6);
   }

   private static void getStateActive826(
      BufferBuilder bufferBuilder37, Matrix4f matrix4f22, float amount358, float amount359, float amount360, float amount361, float amount362, float amount363, Vector4f vector4f6
   ) {
      bufferBuilder37.addVertex(matrix4f22, amount358, amount359, amount360).setColor(vector4f6.x, vector4f6.y, vector4f6.z, vector4f6.w);
      bufferBuilder37.addVertex(matrix4f22, amount361, amount362, amount363).setColor(vector4f6.x, vector4f6.y, vector4f6.z, vector4f6.w);
   }

   private static void getStateActive84(BufferBuilder bufferBuilder38, Matrix4f matrix4f23, List<Vec3> items17, float amount364, int value99) {
      if (items17.size() >= 2 && !(amount364 <= currentValue783)) {
         Vec3 position117 = Vec3.ZERO;
         Vec3[] vec3Array9 = new Vec3[items17.size() - 1];

         for (int index43 = 0; index43 < items17.size() - 1; index43++) {
            Vec3 position118 = (Vec3)items17.get(index43);
            Vec3 position119 = (Vec3)items17.get(index43 + 1);
            Vec3 position120 = position119.subtract(position118);
            if (position120.lengthSqr() < currentValue565) {
               vec3Array9[index43] = index43 > 0 ? vec3Array9[index43 - 1] : new Vec3(1.0, 0.0, 0.0);
            } else {
               position120 = position120.normalize();
               Vec3 position121 = position118.add(position119).scale(currentValue242);
               Vec3 position122 = position117.subtract(position121);
               if (position122.lengthSqr() < currentValue205) {
                  position122 = new Vec3(0.0, 0.0, 1.0);
               }

               Vec3 position123 = position120.cross(position122.normalize());
               if (position123.lengthSqr() < currentValue380) {
                  position123 = position120.cross(new Vec3(0.0, 1.0, 0.0));
                  if (position123.lengthSqr() < currentValue978) {
                     position123 = new Vec3(1.0, 0.0, 0.0);
                  }
               }

               position123 = position123.normalize();
               if (index43 > 0 && position123.dot(vec3Array9[index43 - 1]) < 0.0) {
                  position123 = position123.scale(currentValue803);
               }

               vec3Array9[index43] = position123;
            }
         }

         Vec3[] vec3Array10 = new Vec3[items17.size()];

         for (int index44 = 0; index44 < items17.size(); index44++) {
            Vec3 position124;
            if (index44 == 0) {
               position124 = vec3Array9[0];
            } else if (index44 == items17.size() - 1) {
               position124 = vec3Array9[vec3Array9.length - 1];
            } else {
               Vec3 position125 = vec3Array9[index44 - 1];
               Vec3 position126 = vec3Array9[index44];
               position124 = position125.add(position126);
               if (position124.lengthSqr() < currentValue411) {
                  position124 = position125;
               } else {
                  position124 = position124.normalize();
               }
            }

            vec3Array10[index44] = position124.scale(amount364 * getTextureTarget211(index44, items17.size()));
         }

         for (int index45 = 0; index45 < items17.size() - 1; index45++) {
            Vec3 position127 = (Vec3)items17.get(index45);
            Vec3 position128 = (Vec3)items17.get(index45 + 1);
            Vec3 position129 = position127.subtract(vec3Array10[index45]);
            Vec3 position130 = position127.add(vec3Array10[index45]);
            Vec3 position131 = position128.subtract(vec3Array10[index45 + 1]);
            Vec3 position132 = position128.add(vec3Array10[index45 + 1]);
            getTextureTarget28(bufferBuilder38, matrix4f23, (float)position129.x, (float)position129.y, (float)position129.z, 0.0F, currentValue1169, value99);
            getTextureTarget28(bufferBuilder38, matrix4f23, (float)position130.x, (float)position130.y, (float)position130.z, 1.0F, currentValue8, value99);
            getTextureTarget28(bufferBuilder38, matrix4f23, (float)position132.x, (float)position132.y, (float)position132.z, 1.0F, currentValue591, value99);
            getTextureTarget28(bufferBuilder38, matrix4f23, (float)position131.x, (float)position131.y, (float)position131.z, 0.0F, currentValue102, value99);
         }
      }
   }

   public static void getStateActive817() {
      lastUpdateMillis41 = 0L;
      currentValue25 = 0.0F;
      currentValue820 = 0.0F;
      livingEntity = null;
      currentValue629 = 0.0F;
      currentValue1101 = 0.0F;
      lastUpdateMillis45 = 0L;
      string109 = "";
      list5.clear();
      stateCounter8 = stateCounter20;
      list2.clear();
      stateCounter57 = stateCounter49;
      list4.clear();
      lastUpdateMillis30 = 0L;
      if (textureTarget2 != null) {
         textureTarget2.destroyBuffers();
         textureTarget2 = null;
      }

      list.clear();
      lastUpdateMillis9 = 0L;
      list3.clear();
      lastUpdateMillis27 = 0L;
      string47 = "";
      string111 = "";
      string71 = "";
      linkedList3.clear();
      lastUpdateMillis35 = 0L;
      stateCounter31 = stateCounter58;
      getStateActive810();
      TargetEspShader.updateState2();
      getStateActive848();
      stateCounter36 = stateCounter42;
      stateActive8 = false;
      currentValue246 = 0.0F;
      stateActive7 = false;
      stateActive10 = false;
      currentValue57 = 0.0;
      currentValue1034 = 0.0;
      currentValue519 = 0.0;
      currentValue949 = 0.0;
      currentValue836 = 0.0;
      currentValue791 = 0.0;
   }

   private static float[] getStateActive85(float amount365, long value100) {
      float amount366 = 1.0F - Mth.clamp((amount365 + currentValue314) / currentValue195, 0.0F, 1.0F);
      float amount367 = (float)value100 * currentValue1152;
      amount367 -= (float)Math.floor(amount367);
      float amount368 = currentValue327;
      float amount369 = currentValue11;
      float amount370 = currentValue601;
      float amount371 = currentValue90;
      if (amount367 >= amount368) {
         return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      } else {
         float amount372 = amount367 / amount368 * amount369;
         float amount373 = (amount372 - amount366) / amount370;
         if (amount373 > 0.0F && amount373 < 1.0F) {
            float amount374 = amount373 * amount373 * (currentValue469 - 2.0F * amount373);
            float amount375 = (float)(currentValue340 * amount374);
            float amount376 = (float)Math.sin(currentValue988 * amount373);
            return new float[]{amount375, amount376 * currentValue484, Math.abs((float)Math.sin(amount375)), 0.0F};
         } else if (amount373 >= 1.0F && amount373 < 1.0F + amount371) {
            float amount377 = (amount373 - 1.0F) / amount371;
            float amount378 = 1.0F - amount377;
            float amount379 = currentValue718 + currentValue1166 * (float)Math.sin(amount377 * currentValue154 * currentValue78);
            return new float[]{0.0F, 0.0F, 0.0F, amount378 * amount379};
         } else {
            return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
         }
      }
   }

   private static RenderPipeline getStateActive833() {
      return stateActive11 ? renderPipeline19 : renderPipeline30;
   }

   private static Vec3 getStateActive838(Vec3 position133, Vec3 position134, Vec3 position135) {
      return position133.add(position134).subtract(position135);
   }

   private static void getStateActive860(LivingEntity entity16) {
      if (stateCounter57 != entity16.getId()) {
         stateCounter57 = entity16.getId();
         list2.clear();
         float amount380 = entity16.getBbWidth() * currentValue851 + currentValue874;
         float amount381 = entity16.getBbHeight();
         float[][] instance12 = new float[][]{
            {currentValue1073, currentValue997, currentValue1045},
            {currentValue149, currentValue352, currentValue455},
            {currentValue916, currentValue34, currentValue516},
            {currentValue827, currentValue1126, currentValue362},
            {currentValue812, currentValue298, currentValue147},
            {currentValue156, currentValue201, currentValue733},
            {currentValue139, currentValue107, currentValue456},
            {currentValue594, currentValue136, currentValue1004},
            {currentValue651, currentValue535, currentValue648},
            {currentValue777, currentValue606, currentValue692},
            {currentValue521, currentValue255, currentValue1183},
            {currentValue902, currentValue1040, currentValue1006},
            {currentValue168, currentValue235, currentValue987},
            {currentValue1068, currentValue863, currentValue856},
            {currentValue188, currentValue665, currentValue1080},
            {currentValue667, currentValue781, currentValue730},
            {currentValue457, currentValue678, currentValue943},
            {currentValue1167, currentValue716, currentValue364},
            {currentValue674, currentValue935, currentValue142},
            {currentValue467, currentValue482, 1.0F},
            {currentValue922, currentValue758, currentValue513},
            {currentValue163, currentValue55, currentValue880},
            {currentValue912, currentValue1139, currentValue972},
            {currentValue955, currentValue61, currentValue109},
            {currentValue587, currentValue159, currentValue981},
            {currentValue432, currentValue711, currentValue688},
            {currentValue687, currentValue832, currentValue120}
         };

         for (float[] instance13 : instance12) {
            float angleRadians10 = (float)Math.toRadians(instance13[0]);
            float amount382 = amount380 * instance13[2];
            list2.add(
               new Vector3f(Mth.cos(angleRadians10) * amount382, Mth.clamp(instance13[1] * amount381, currentValue291, amount381 - currentValue80), Mth.sin(angleRadians10) * amount382)
            );
         }
      }
   }

   private static void getStateActive831(BufferBuilder bufferBuilder39, Identifier identifier3) {
      getStateActive67(bufferBuilder39, identifier3, false, stateActive12 ? renderPipeline15 : renderPipeline25);
   }

   private static void getStateActive843(
      PoseStack poseStack12,
      BufferBuilder bufferBuilder40,
      double amount383,
      double amount384,
      double amount385,
      double amount386,
      double amount387,
      double amount388,
      double amount389,
      float amount390,
      float amount391,
      int value101
   ) {
      Vec3 position136 = getStateActive6(amount386, amount387, amount388, amount389);
      Vec3 position137 = getStateActive6(amount386 + currentValue700, amount387, amount388, amount389);
      double amount392 = position137.x - position136.x;
      double amount393 = position137.y - position136.y;
      double amount394 = position137.z - position136.z;
      double amount395 = Math.sqrt(amount392 * amount392 + amount393 * amount393 + amount394 * amount394);
      double amount396 = amount395 > currentValue962 ? 1.0 / amount395 : 0.0;
      double amount397 = amount392 * amount396;
      double amount398 = amount393 * amount396;
      double amount399 = amount394 * amount396;
      float amount400 = (float)Mth.clamp(amount395 * currentValue555, currentValue252, currentValue833);
      poseStack12.pushPose();
      poseStack12.translate(
         amount383 + position136.x - amount397 * (currentValue334 + amount390 * currentValue475),
         amount384 + position136.y - amount398 * (currentValue848 + amount390 * currentValue842),
         amount385 + position136.z - amount399 * (currentValue946 + amount390 * currentValue1008)
      );
      poseStack12.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
      Matrix4f matrix4f24 = poseStack12.last().pose();
      float amount401 = amount390 * (currentValue54 + currentValue817 * amount400);
      addBillboardQuad(bufferBuilder40, matrix4f24, amount401, getStateActive837(value101, amount391 * (currentValue1113 + currentValue500 * amount400)));
      addBillboardQuad(bufferBuilder40, matrix4f24, amount390, getStateActive837(value101, amount391));
      poseStack12.popPose();
   }

   private static Matrix4f getStateActive825(Matrix4f matrix4f25, float amount402, float amount403, float amount404, float amount405) {
      Matrix4f matrix4f26 = new Matrix4f(matrix4f25);
      matrix4f26.translate(amount402, amount403, amount405);
      matrix4f26.rotate(amount404, 0.0F, 1.0F, 0.0F);
      matrix4f26.translate(-amount402, -amount403, 0.0F);
      return matrix4f26;
   }

   private static List<Vec3> getStateActive858(Vec3 position138, Vec3 position139, int value102, double amount406) {
      int value103 = (1 << value102) + 1;
      Vec3[] vec3Array11 = new Vec3[value103];
      vec3Array11[0] = position138;
      vec3Array11[value103 - 1] = position139;
      getStateActive89(vec3Array11, 0, value103 - 1, amount406, random5);
      ArrayList items18 = new ArrayList(value103);

      for (Vec3 position140 : vec3Array11) {
         items18.add(position140);
      }

      return items18;
   }

   public static void getStateActive830() {
      if (stateActive8 && textureTarget3 != null) {
         stateActive8 = false;
         RenderTarget renderTarget3 = TargetEspPreviewContext.target();
         if (renderTarget3 != null && renderTarget3.getColorTextureView() != null) {
            BloomCompositor.updateState(textureTarget3, renderTarget3, stateCounter35, currentValue319, currentValue17, 1.0F);
            CommandEncoder commandEncoder3 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass renderPass3 = commandEncoder3.createRenderPass(() -> string46, renderTarget3.getColorTextureView(), Optional.empty());

            try {
               if (RenderPipelineCache.isCompiled(renderPipeline14)) {
                  renderPass3.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline14));
                  renderPass3.setUniform(
                     string120, textureTarget3.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
                  );
                  renderPass3.draw(3, 1, 0, 0);
               }
            } catch (Throwable throwable5) {
               if (renderPass3 != null) {
                  try {
                     renderPass3.close();
                  } catch (Throwable throwable6) {
                     throwable5.addSuppressed(throwable6);
                  }
               }

               throw throwable5;
            }

            if (renderPass3 != null) {
               renderPass3.close();
            }
         }
      }
   }

   private static Vec3 getStateActive855(Vec3 position141, double amount407, double amount408, double amount409) {
      double amount410 = Math.cos(amount408) * amount407;
      return position141.add(Math.cos(amount409) * amount410, Math.sin(amount408) * amount407, Math.sin(amount409) * amount410);
   }

   private static float getStateActive852(float amount411) {
      if (amount411 <= 0.0F || amount411 >= 1.0F) {
         return 0.0F;
      } else {
         return amount411 < currentValue341
            ? getStateActive68(amount411 / currentValue296)
            : getStateActive68(1.0F - (amount411 - currentValue231) / currentValue299);
      }
   }

   private static TargetEspRenderer.F0nIo1tGX54h6o8A getStateActive840(long value104) {
      float amount412 = random5.nextFloat();
      float amount413;
      float amount414;
      float amount415;
      if (amount412 < currentValue1083) {
         float amount416 = currentValue1163 + random5.nextFloat() * currentValue190;
         float amount417 = currentValue210 + random5.nextFloat() * currentValue439;
         amount414 = amount416 - amount417 * currentValue360;
         amount413 = amount416 + amount417 * currentValue915;
         amount415 = currentValue422 + random5.nextFloat() * currentValue103;
      } else if (amount412 < currentValue735) {
         amount414 = currentValue658 + random5.nextFloat() * currentValue216;
         amount413 = currentValue958 + random5.nextFloat() * currentValue6;
         amount415 = currentValue815 + random5.nextFloat() * currentValue354;
      } else {
         amount414 = currentValue792 - random5.nextFloat() * currentValue823;
         amount413 = currentValue967 + random5.nextFloat() * currentValue324;
         amount415 = currentValue398 + random5.nextFloat() * currentValue1161;
      }

      return new TargetEspRenderer.F0nIo1tGX54h6o8A(
         random5.nextDouble() * currentValue209 * currentValue969,
         random5.nextBoolean() ? 1 : -1,
         amount415,
         amount413,
         amount414,
         1.0F,
         currentValue79 + random5.nextFloat() * currentValue800,
         currentValue121,
         random5.nextInt(4),
         random5.nextFloat() * currentValue515,
         value104,
         (long)(1500 + random5.nextInt(500)),
         random5.nextFloat() * currentValue1145
      );
   }

   private TargetEspRenderer() {
   }

   private static void onDeltaTrackerPoseStack22(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent13, Vec3 position142, LivingEntity entity17, int value105, int value106, float amount418, long value107) {
      position142 = position142.subtract(TargetEspPreviewContext.camera().position());
      updateState15(entity17);
      float amount419 = (float)value107 * currentValue496 + currentValue595;
      float amount420 = entity17.getBbHeight();
      Vec3 position143 = position142.add(0.0, amount420 * currentValue728, 0.0);
      Matrix4f matrix4f27 = deltaTrackerPoseStackEvent13.getPoseStack().last().pose();
      float amount421 = entity17.getBbWidth() * currentValue64 + currentValue45;
      float amount422 = amount421 * currentValue828;
      float[] instance14 = new float[]{currentValue1086, currentValue737, currentValue534};
      float[] instance15 = new float[]{currentValue771, currentValue650, currentValue1095};
      float[] instance16 = new float[]{0.0F, currentValue923, currentValue387};
      BufferBuilder bufferBuilder41 = getBufferBuilder3();
      BufferBuilder bufferBuilder42 = getBufferBuilder3();
      addCameraFacingSprite(
         bufferBuilder42,
         matrix4f27,
         position143.x,
         position143.y,
         position143.z,
         amount421 * currentValue635,
         getStateActive837(getTextureTarget22(value105, currentValue274), amount418 * currentValue514)
      );
      addCameraFacingSprite(
         bufferBuilder42,
         matrix4f27,
         position143.x,
         position143.y,
         position143.z,
         amount421 * currentValue1121,
         getStateActive837(getTextureTarget22(value105, currentValue822), amount418 * currentValue1148)
      );

      for (int index46 = 0; index46 < instance14.length; index46++) {
         float amount423 = amount419 * instance15[index46] * currentValue1077 + instance16[index46];
         double angleRadians11 = Math.toRadians(amount423);
         double angleRadians12 = Math.toRadians(instance14[index46]);
         Vec3 position144 = new Vec3(Math.sin(angleRadians12) * Math.cos(angleRadians11), Math.cos(angleRadians12), Math.sin(angleRadians12) * Math.sin(angleRadians11));
         Vec3[] vec3Array12 = getStateActive811(position144);
         int value108 = index46 == instance14.length - 1 ? getTextureTarget22(value106, currentValue105) : getTextureTarget22(value105, currentValue996);
         updateState14(bufferBuilder41, matrix4f27, position143, vec3Array12[0], vec3Array12[1], amount422, getStateActive837(value108, amount418 * currentValue631));
         double angleRadians13 = Math.toRadians(amount423 * (instance15[index46] < 0.0F ? currentValue652 : currentValue259) + instance16[index46]);
         Vec3 position145 = position143.add(vec3Array12[0].scale(Math.cos(angleRadians13) * amount421)).add(vec3Array12[1].scale(Math.sin(angleRadians13) * amount421));
         addCameraFacingSprite(
            bufferBuilder42,
            matrix4f27,
            position145.x,
            position145.y,
            position145.z,
            currentValue140,
            getStateActive837(getTextureTarget22(value105, currentValue586), amount418)
         );
         addCameraFacingSprite(
            bufferBuilder42,
            matrix4f27,
            position145.x,
            position145.y,
            position145.z,
            currentValue263,
            getStateActive837(getTextureTarget22(value105, currentValue402), amount418 * currentValue193)
         );
      }

      getStateActive611(bufferBuilder41, renderPipeline28);
      updateState9(bufferBuilder42, resourceIdentifier8, true);
   }

   private static void onDeltaTrackerPoseStack14(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent14, Vec3 position146, LivingEntity entity18, int value109, int value110, float amount424, float amount425) {
      float amount426 = amount424 * currentValue600;
      if (!(amount426 <= currentValue919)) {
         Vec3 position147 = TargetEspPreviewContext.camera().position();
         float amount427 = entity18.getBbHeight();
         long value111 = Math.max(1L, Math.round(currentValue744 / currentValue377));
         double nowMillis9 = (double)(System.currentTimeMillis() % value111) / value111 * currentValue560 * currentValue769;
         float amount428 = 0.0F;
         if (stateActive && entity18.hurtTime > 0) {
            amount428 = Mth.clamp((entity18.hurtTime - amount425) / currentValue503, 0.0F, 1.0F);
         }

         float amount429 = entity18.getBbWidth() * currentValue897;
         float amount430 = entity18.getBbWidth() + amount429 - amount429 * amount426;
         float amount431 = (float)(position146.x - position147.x);
         float amount432 = (float)(position146.y - position147.y);
         float amount433 = (float)(position146.z - position147.z);
         float amount434 = amount432 + amount427 * (float)(currentValue825 + currentValue592 * Math.cos(nowMillis9));
         float amount435 = (float)(Math.sin(nowMillis9) * amount427 * currentValue673);
         Matrix4f matrix4f28 = deltaTrackerPoseStackEvent14.getPoseStack().last().pose();
         BufferBuilder bufferBuilder43 = getBufferBuilder3();
         byte amount436 = 2;
         float amount437 = currentValue976;
         float amount438 = currentValue857;

         for (int index47 = 0; index47 < amount436; index47++) {
            float amount439 = amount434 + amount437 / amount436 * index47;
            float amount440 = amount426 * (1.0F - (float)index47 / amount436 * currentValue412);
            int value112 = index47 * 10;

            for (int index48 = 0; index48 < 360; index48 += 5) {
               int value113 = index48 / 5;
               int value114 = getStateActive86(value109, value110, index48 * 5 + value112, amount428, currentValue685);
               Vector4f vector4f7 = ColorMath.getVector4f(getStateActive837(value114, amount440));
               float amount441 = amount431 + (float)(doubleArray2[value113] * amount430);
               float amount442 = amount433 + (float)(doubleArray[value113] * amount430);
               float amount443 = amount431 + (float)(doubleArray2[value113 + 1] * amount430);
               float amount444 = amount433 + (float)(doubleArray[value113 + 1] * amount430);
               float amount445 = amount443 - amount441;
               float amount446 = amount444 - amount442;
               float amount447 = Mth.sqrt(amount445 * amount445 + amount446 * amount446);
               float amount448 = amount447 == 0.0F ? amount438 : -amount446 / amount447 * amount438;
               float amount449 = amount447 == 0.0F ? 0.0F : amount445 / amount447 * amount438;
               addColoredVertex(bufferBuilder43, matrix4f28, amount441 + amount448, amount439, amount442 + amount449, 0.0F, 0.0F, vector4f7);
               addColoredVertex(bufferBuilder43, matrix4f28, amount443 + amount448, amount439, amount444 + amount449, 0.0F, 1.0F, vector4f7);
               addColoredVertex(bufferBuilder43, matrix4f28, amount443 - amount448, amount439, amount444 - amount449, 1.0F, 1.0F, vector4f7);
               addColoredVertex(bufferBuilder43, matrix4f28, amount441 - amount448, amount439, amount442 - amount449, 1.0F, 0.0F, vector4f7);
            }
         }

         for (int index49 = 0; index49 < 360; index49 += 5) {
            int value115 = index49 / 5;
            int value116 = getStateActive86(value109, value110, index49 * 20, amount428, currentValue786);
            int value117 = getStateActive86(value109, value110, (index49 + 5) * 20, amount428, currentValue1144);
            Vector4f vector4f8 = ColorMath.getVector4f(getStateActive837(value116, amount426));
            Vector4f vector4f9 = ColorMath.getVector4f(getStateActive837(value116, 0.0F));
            Vector4f vector4f10 = ColorMath.getVector4f(getStateActive837(value117, amount426));
            Vector4f vector4f11 = ColorMath.getVector4f(getStateActive837(value117, 0.0F));
            float amount450 = amount431 + (float)(doubleArray2[value115] * amount430);
            float amount451 = amount433 + (float)(doubleArray[value115] * amount430);
            float amount452 = amount431 + (float)(doubleArray2[value115 + 1] * amount430);
            float amount453 = amount433 + (float)(doubleArray[value115 + 1] * amount430);
            float amount454 = amount434 + amount435;
            addColoredVertex(bufferBuilder43, matrix4f28, amount450, amount434, amount451, 0.0F, 0.0F, vector4f8);
            addColoredVertex(bufferBuilder43, matrix4f28, amount450, amount454, amount451, 0.0F, 1.0F, vector4f9);
            addColoredVertex(bufferBuilder43, matrix4f28, amount452, amount454, amount453, 1.0F, 1.0F, vector4f11);
            addColoredVertex(bufferBuilder43, matrix4f28, amount452, amount434, amount453, 1.0F, 0.0F, vector4f10);
         }

         getStateActive611(bufferBuilder43, renderPipeline11);
      }
   }

   private static void onDeltaTrackerPoseStack25(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent15, Vec3 position148, LivingEntity entity19, int value118, int value119, float amount455) {
      if (amount455 <= currentValue750) {
         getStateActive848();
      } else {
         position148 = position148.subtract(TargetEspPreviewContext.camera().position());
         long nowMillis10 = System.currentTimeMillis();
         PoseStack poseStack13 = deltaTrackerPoseStackEvent15.getPoseStack();
         BufferBuilder bufferBuilder44 = getBufferBuilder3();

         for (int index50 = 0; index50 < linkedListArray.length; index50++) {
            double amount456 = nowMillis10 * currentValue1182;
            double amount457 = index50 * (currentValue604 / linkedListArray.length);
            double amount458 = entity19.getBbWidth() + currentValue300;
            double amount459 = Math.sin(amount456 + amount457) * amount458;
            double amount460 = Math.cos(amount456 + amount457) * amount458;
            double amount461 = index50 % 2 == 0 ? 1.0 : currentValue726;
            double amount462 = Math.sin((amount456 + amount457) * currentValue577) * amount461;
            double amount463 = amount462 * (entity19.getBbHeight() * currentValue445) + entity19.getBbHeight() * currentValue660;
            LinkedList linkedList = linkedListArray[index50];
            Vec3 position149 = new Vec3(amount459, amount463, amount460);
            if (!linkedList.isEmpty()) {
               Vec3 position150 = (Vec3)linkedList.getFirst();
               double amount464 = position150.distanceTo(position149);
               int value120 = Mth.clamp((int)Math.ceil(amount464 / currentValue106), 1, 6);

               for (int index51 = 1; index51 <= value120; index51++) {
                  double amount465 = (double)index51 / value120;
                  linkedList.addFirst(position150.lerp(position149, amount465));
               }
            } else {
               linkedList.addFirst(position149);
            }

            while (linkedList.size() > 34) {
               linkedList.removeLast();
            }

            int value121 = 0;
            int size12 = linkedList.size();

            for (Vec3 position151 : (Iterable<Vec3>)(Iterable<?>)(linkedList)) {
               float amount466 = 1.0F - (float)value121 / size12;
               float amount467 = (float)(position148.x + position151.x);
               float amount468 = (float)(position148.y + position151.y);
               float amount469 = (float)(position148.z + position151.z);
               int color12 = ColorMath.interpolateRgba(value118, value119, amount466);
               float amount470 = value121 == 0 ? currentValue1128 : currentValue358;
               float amount471 = amount470 * amount466 * amount455;
               float amount472 = (float)(Math.pow(amount466, currentValue361) * amount455);
               updateState13(bufferBuilder44, poseStack13, amount467, amount468, amount469, amount471, getStateActive837(color12, amount472));
               if (value121 == 0) {
                  updateState13(bufferBuilder44, poseStack13, amount467, amount468, amount469, currentValue581 * amount455, getStateActive837(-1, amount455));
               }

               value121++;
            }
         }

         updateState9(bufferBuilder44, resourceIdentifier3, false);
      }
   }

   private static float getStateActive824(float amount473, float amount474, float amount475) {
      if (amount473 < amount474) {
         return Math.min(amount473 + amount475, amount474);
      } else {
         return amount473 > amount474 ? Math.max(amount473 - amount475, amount474) : amount473;
      }
   }

   private static int getStateActive837(int value122, float amount476) {
      return ColorMath.packRgba(
         ColorMath.redChannel(value122),
         ColorMath.greenChannel(value122),
         ColorMath.blueChannel(value122),
         ColorMath.alphaChannel(value122) * Mth.clamp(amount476, 0.0F, 1.0F)
      );
   }

   private static void onDeltaTrackerPoseStack11(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent16, Vec3 position152, LivingEntity entity20, int value123, float amount477, long value124) {
      Vec3 position153 = TargetEspPreviewContext.camera().position();
      Vec3 position154 = entity20.position().add(0.0, entity20.getBbHeight() / currentValue348, 0.0);
      float amount478 = entity20.getBbWidth() * currentValue148;
      float amount479 = entity20.getBbHeight();
      float amount480 = currentValue1072 - currentValue666 * amount477;
      float amount481 = (float)(value124 % lastUpdateMillis16) / currentValue117 + amount477;
      PoseStack poseStack14 = deltaTrackerPoseStackEvent16.getPoseStack();
      BufferBuilder bufferBuilder45 = getStateActive815();

      for (int index52 = 0; index52 < 360; index52 += 20) {
         double angleRadians14 = Math.toRadians(index52 + amount481 * currentValue528);
         float amount482 = (float)Math.sin(angleRadians14) * amount478 * amount480;
         float amount483 = (float)Math.cos(angleRadians14) * amount478 * amount480;
         float amount484 = currentValue186 + amount479 * Math.abs((float)Math.sin(index52));
         Vec3 position155 = position152.add(amount482, amount484, amount483);
         poseStack14.pushPose();
         poseStack14.translate(position155.x - position153.x, position155.y - position153.y, position155.z - position153.z);
         Vector3f position156 = new Vector3f((float)(position154.x - position155.x), (float)(position154.y - position155.y), (float)(position154.z - position155.z)).normalize();
         poseStack14.mulPose(new Matrix4f().rotate(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), position156)));
         updateState18(bufferBuilder45, poseStack14, currentValue440, getStateActive837(value123, amount477));
         poseStack14.popPose();
      }

      getStateActive611(bufferBuilder45, getStateActive610());
      BufferBuilder bufferBuilder46 = getBufferBuilder3();

      for (int index53 = 0; index53 < 360; index53 += 20) {
         double angleRadians15 = Math.toRadians(index53 + amount481 * currentValue568);
         float amount485 = (float)Math.sin(angleRadians15) * amount478 * amount480;
         float amount486 = (float)Math.cos(angleRadians15) * amount478 * amount480;
         float amount487 = currentValue835 + amount479 * Math.abs((float)Math.sin(index53));
         Vec3 position157 = position152.add(amount485, amount487, amount486);
         updateState13(
            bufferBuilder46,
            poseStack14,
            position157.x - position153.x,
            position157.y - position153.y,
            position157.z - position153.z,
            currentValue326 * amount477,
            getStateActive837(value123, amount477 * currentValue537)
         );
         updateState13(
            bufferBuilder46,
            poseStack14,
            position157.x - position153.x,
            position157.y - position153.y,
            position157.z - position153.z,
            currentValue541 * amount477,
            getStateActive837(value123, amount477 * currentValue632)
         );
      }

      getStateActive67(bufferBuilder46, resourceIdentifier5, false, getStateActive833());
   }

   public static void updateState5(boolean matches9, float amount488, int value125) {
      stateActive9 = matches9;
      currentValue806 = amount488;
      stateCounter13 = value125;
   }

   private static float getStateActive83(int value126) {
      float amount489 = (float)Math.sin(value126 * currentValue992) * currentValue588;
      return amount489 - (float)Math.floor(amount489);
   }

   private static void getStateActive848() {
      for (LinkedList linkedList2 : linkedListArray) {
         linkedList2.clear();
      }
   }

   private static void addCameraFacingSprite(BufferBuilder bufferBuilder47, Matrix4f matrix4f29, double amount490, double amount491, double amount492, float amount493, int value127) {
      PoseStack poseStack15 = new PoseStack();
      poseStack15.mulPose(matrix4f29);
      poseStack15.translate(amount490, amount491, amount492);
      poseStack15.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
      addBillboardQuad(bufferBuilder47, poseStack15.last().pose(), amount493, value127);
   }

   private static Vec3 getStateActive836(Vec3 position158, double amount494, double amount495, double amount496) {
      double amount497 = Math.sin(amount494);
      double amount498 = Math.cos(amount494);
      double amount499 = position158.y * amount498 - position158.z * amount497;
      double amount500 = position158.y * amount497 + position158.z * amount498;
      position158 = new Vec3(position158.x, amount499, amount500);
      double amount501 = Math.sin(amount495);
      double amount502 = Math.cos(amount495);
      double amount503 = position158.x * amount502 - position158.z * amount501;
      amount500 = position158.x * amount501 + position158.z * amount502;
      position158 = new Vec3(amount503, position158.y, amount500);
      double amount504 = Math.sin(amount496);
      double amount505 = Math.cos(amount496);
      amount503 = position158.x * amount505 - position158.y * amount504;
      amount499 = position158.x * amount504 + position158.y * amount505;
      return new Vec3(amount503, amount499, position158.z);
   }

   private static void onDeltaTrackerPoseStack6(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent17, Vec3 position159, LivingEntity entity21, int value128, float amount506, float amount507, long value129) {
      float amount508 = (float)Math.sin(entity21.hurtTime * currentValue634 / currentValue760);
      currentValue820 = currentValue820
         + (amount508 - currentValue820) * (float)Math.sin(Math.min(amount507 / currentValue339, 1.0F) * currentValue738 / currentValue929);
      int value130 = (int)((value129 / lastUpdateMillis6 + 1L) % lastUpdateMillis17);
      value130 = value130 >= 180 ? 360 - value130 : value130;
      int color13 = ColorMath.packRgba(
         Math.min(255, Math.round(ColorMath.redChannel(value128) / currentValue118)),
         Math.min(255, Math.round(ColorMath.greenChannel(value128) / currentValue558)),
         Math.min(255, Math.round(ColorMath.blueChannel(value128) / currentValue778)),
         currentValue680
      );
      int color14 = ColorMath.interpolateRgba(value128, color13, value130 / currentValue125);
      if (stateActive) {
         color14 = ColorMath.interpolateRgba(color14, stateCounter55, currentValue820);
      }

      color14 &= stateCounter5;
      float amount509 = (float)Math.cos(value129 / currentValue504) * currentValue525 + currentValue820 * currentValue802;
      float amount510 = (1.0F - currentValue753 * currentValue820) * amount506;
      Camera camera = TargetEspPreviewContext.camera();
      Vec3 position160 = camera.position();
      PoseStack poseStack16 = deltaTrackerPoseStackEvent17.getPoseStack();

      for (int index54 = 0; index54 < 3; index54++) {
         BufferBuilder bufferBuilder48 = CubeGlowShader.getBufferBuilder2(index54 == 2, index54 == 0);

         for (int index55 = 0; index55 < 360; index55 += 12) {
            float angleRadians16 = (float)Math.toRadians(index55 + amount509);
            float amount511 = (float)(Math.sin(angleRadians16) * entity21.getBbWidth() * currentValue110 * currentValue605);
            float amount512 = (float)(Math.cos(angleRadians16) * entity21.getBbWidth() * currentValue31 * currentValue495);
            double amount513 = entity21.getBbHeight()
               * Math.abs(
                  Math.sin(
                     ((int)(index55 * currentValue318) & stateCounter27)
                        * currentValue963
                        * currentValue746
                        / currentValue288
                  )
               );
            poseStack16.pushPose();
            poseStack16.translate(position159.x + amount511 - position160.x, position159.y + amount513 - position160.y, position159.z + amount512 - position160.z);
            if (index54 == 0) {
               poseStack16.mulPose(new Matrix4f().rotate(camera.rotation()));
               CubeGlowShader.updateState2(
                  bufferBuilder48, poseStack16.last().pose(), currentValue262 * amount510, color14 | (int)(amount506 * currentValue663 * currentValue701) << 24
               );
            } else {
               poseStack16.mulPose(new Matrix4f().rotate(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(-amount511, (float)(-amount513), -amount512).normalize())));
               if (index54 == 1) {
                  CubeGlowShader.updateState(
                     bufferBuilder48, poseStack16.last().pose(), currentValue1180 * amount510, color14 | (int)(amount506 * currentValue780 * currentValue122) << 24
                  );
               } else {
                  CubeGlowShader.updateState3(
                     bufferBuilder48, poseStack16.last().pose(), currentValue702 * amount510, color14 | (int)(amount506 * currentValue266) << 24
                  );
               }
            }

            poseStack16.popPose();
         }

         CubeGlowShader.updateState4(bufferBuilder48, index54 == 2, index54 == 0 ? CubeGlowShader.resourceIdentifier2 : null, !stateActive11);
      }
   }

   private static void getStateActive816(
      double amount514, double amount515, double amount516, double amount517, double amount518, double amount519, double amount520, float amount521, float amount522
   ) {
      Vec3 position161 = getStateActive6(amount517, amount518, amount519, amount520);
      linkedList3.addLast(
         new TargetEspRenderer.BNt5vHvMwoeyBNKB(new Vec3(amount514 + position161.x, amount515 + position161.y, amount516 + position161.z), System.currentTimeMillis(), amount521, amount522)
      );

      while (linkedList3.size() > 130) {
         linkedList3.pollFirst();
      }
   }

   private static void onDeltaTrackerPoseStack18(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent18, Vec3 position162, int value131, float amount523) {
      position162 = position162.subtract(TargetEspPreviewContext.camera().position());
      BufferBuilder bufferBuilder49 = getBufferBuilder3();
      PoseStack poseStack17 = deltaTrackerPoseStackEvent18.getPoseStack();
      float amount524 = currentValue1101 * currentValue211;
      float amount525 = getStateActive68(currentValue629);

      for (byte index56 = 0; index56 < 9; index56 += 3) {
         for (int index57 = 0; index57 < 12; index57++) {
            float amount526 = amount524 + index57 * currentValue853;
            int value132 = index56 * index56;
            double amount527 = position162.x + currentValue965 * Mth.sin(amount526 + value132);
            double amount528 = position162.y
               + currentValue1044
               + currentValue1147 * Mth.sin(amount524 + index57 * currentValue770)
               + currentValue421 * index56;
            double amount529 = position162.z + currentValue517 * Mth.cos(amount526 - value132);
            float amount530 = currentValue862 * amount525 * (currentValue347 + index57 / currentValue938);
            poseStack17.pushPose();
            poseStack17.translate(amount527, amount528, amount529);
            poseStack17.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
            addBillboardQuad(bufferBuilder49, poseStack17.last().pose(), amount530, getStateActive837(value131, amount523));
            poseStack17.popPose();
         }
      }

      updateState9(bufferBuilder49, resourceIdentifier, true);
   }

   private static int getStateActive812(int value133, float amount531) {
      return ColorMath.packRgba(
         Math.min(255, Math.round(ColorMath.redChannel(value133) * amount531)),
         Math.min(255, Math.round(ColorMath.greenChannel(value133) * amount531)),
         Math.min(255, Math.round(ColorMath.blueChannel(value133) * amount531)),
         ColorMath.alphaChannel(value133)
      );
   }

   private static void getStateActive827(BufferBuilder bufferBuilder50, Matrix4f matrix4f30, Vec3 position163, Vec3 position164, Vec3 position165, float amount532, int value134) {
      updateState14(bufferBuilder50, matrix4f30, position163, position164, position165, amount532 * 2.0F / currentValue204, value134);
   }

   private static float getStateActive851(int value135, float amount533, float amount534) {
      return switch (value135 & 3) {
         case 0 -> amount533 + amount534;
         case 1 -> -amount533 + amount534;
         case 2 -> amount533 - amount534;
         default -> -amount533 - amount534;
      };
   }

   public static void getStateActive8() {
      TargetLightingCompositor.updateState6();
      stateActive13 = true;
      deltaTrackerPoseStackEvent28 = null;
      livingEntity2 = null;
      string14 = null;
      gpuBufferSlice2 = RenderSystem.getProjectionMatrixBuffer();
      projectionType2 = RenderSystem.getProjectionType();
   }

   private static Vec3 getStateActive823(Vec3 position166, double amount535, double amount536, float amount537, float amount538, int value136, float amount539) {
      double amount540 = amount537
         + value136 * amount538 * currentValue628 * currentValue708 * amount539
         + (random5.nextDouble() - currentValue540) * currentValue1130;
      double amount541 = amount536 * (currentValue1107 + random5.nextDouble() * currentValue697);
      double amount542 = amount535 * (currentValue1097 + amount539 * currentValue1195)
         + (random5.nextDouble() - currentValue765) * amount535 * currentValue254;
      return position166.add(Math.cos(amount540) * amount541, amount542, Math.sin(amount540) * amount541);
   }

   private static void getStateActive849(
      BufferBuilder bufferBuilder51,
      Matrix4f matrix4f31,
      float amount543,
      float amount544,
      float amount545,
      float amount546,
      float amount547,
      float amount548,
      float amount549,
      float amount550,
      float amount551,
      int value137,
      int value138,
      int value139
   ) {
      Vector4f vector4f12 = ColorMath.getVector4f(value137);
      Vector4f vector4f13 = ColorMath.getVector4f(value138);
      Vector4f vector4f14 = ColorMath.getVector4f(value139);
      addColoredVertex(bufferBuilder51, matrix4f31, amount543, amount544, amount545, currentValue477, currentValue1140, vector4f12);
      addColoredVertex(bufferBuilder51, matrix4f31, amount549, amount550, amount551, currentValue493, currentValue1165, vector4f14);
      addColoredVertex(bufferBuilder51, matrix4f31, amount546, amount547, amount548, currentValue416, currentValue132, vector4f13);
      addColoredVertex(bufferBuilder51, matrix4f31, amount546, amount547, amount548, currentValue971, currentValue734, vector4f13);
   }

   public static void updateState7(boolean matches10, float amount552) {
      stateActive3 = matches10;
      currentValue908 = amount552;
   }

   private static float getStateActive814(float amount553) {
      float amount554 = Mth.clamp(amount553, 0.0F, 1.0F) - 1.0F;
      return 1.0F + currentValue404 * amount554 * amount554 * amount554 + currentValue690 * amount554 * amount554;
   }

   private static void onDeltaTrackerPoseStack10(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent19, Vec3 position167, LivingEntity entity22, int value140, float amount555, long value141, boolean matches11) {
      double nowNanos2 = System.nanoTime() * currentValue641;
      float amount556 = (float)(nowNanos2 * currentValue1093);
      if (matches11 && value141 - lastUpdateMillis27 >= lastUpdateMillis8 && list3.size() < 18) {
         for (int index58 = 0; index58 < 2 && list3.size() < 18; index58++) {
            list3.add(getStateActive850(entity22, position167, value141, amount556));
         }

         lastUpdateMillis27 = value141;
      }

      list3.removeIf(sjzMjyMYX7ylg4dN -> value141 - sjzMjyMYX7ylg4dN.lastUpdateMillis47 > sjzMjyMYX7ylg4dN.lastUpdateMillis34);
      if (!list3.isEmpty() && !(amount555 <= currentValue390)) {
         Vec3 position168 = TargetEspPreviewContext.camera().position();
         Matrix4f matrix4f32 = deltaTrackerPoseStackEvent19.getPoseStack().last().pose();
         PoseStack poseStack18 = deltaTrackerPoseStackEvent19.getPoseStack();
         int value142 = getTextureTarget22(value140, currentValue177);
         int value143 = getTextureTarget22(value140, currentValue399);
         BufferBuilder bufferBuilder52 = getBufferBuilder3();
         BufferBuilder bufferBuilder53 = getBufferBuilder3();

         for (TargetEspRenderer.SjzMjyMYX7ylg4dN sjzMjyMYX7ylg4dN2 : list3) {
            float amount557 = (float)(value141 - sjzMjyMYX7ylg4dN2.lastUpdateMillis47) / (float)sjzMjyMYX7ylg4dN2.lastUpdateMillis34;
            if (!(amount557 < 0.0F) && !(amount557 > 1.0F)) {
               float amount558 = (float)Math.sin(currentValue1105 * Mth.clamp(amount557, 0.0F, 1.0F));
               amount558 = getStateActive68(amount558);
               float amount559 = amount555 * amount558;
               if (!(amount559 <= currentValue175)) {
                  double nowNanos3 = (System.nanoTime() - sjzMjyMYX7ylg4dN2.lastUpdateMillis14) * currentValue527;
                  float amount560 = (float)(nowNanos3 * sjzMjyMYX7ylg4dN2.currentValue914 * sjzMjyMYX7ylg4dN2.stateCounter2);
                  float amount561 = (float)Math.cos(amount560);
                  float amount562 = (float)Math.sin(amount560);
                  ArrayList items19 = new ArrayList(sjzMjyMYX7ylg4dN2.list8.size());

                  for (Vec3 position169 : sjzMjyMYX7ylg4dN2.list8) {
                     double amount563 = position169.x * amount561 - position169.z * amount562;
                     double amount564 = position169.x * amount562 + position169.z * amount561;
                     items19.add(position167.add(amount563, position169.y, amount564).subtract(position168));
                  }

                  if (items19.size() >= 2) {
                     for (int index59 = 0; index59 < items19.size(); index59++) {
                        Vec3 position170 = (Vec3)items19.get(index59);
                        float size13 = 1.0F - (float)index59 / Math.max(1, items19.size() - 1) * currentValue461;
                        updateState13(
                           bufferBuilder52,
                           poseStack18,
                           position170.x,
                           position170.y,
                           position170.z,
                           currentValue911 * size13 * sjzMjyMYX7ylg4dN2.currentValue20,
                           getStateActive837(value140, amount559 * currentValue584)
                        );
                        updateState13(
                           bufferBuilder52,
                           poseStack18,
                           position170.x,
                           position170.y,
                           position170.z,
                           currentValue1079 * size13 * sjzMjyMYX7ylg4dN2.currentValue20,
                           getStateActive837(value142, amount559 * currentValue356)
                        );
                        updateState13(
                           bufferBuilder52,
                           poseStack18,
                           position170.x,
                           position170.y,
                           position170.z,
                           currentValue480 * size13 * sjzMjyMYX7ylg4dN2.currentValue20,
                           getStateActive837(value143, amount559 * currentValue689)
                        );
                     }

                     getStateActive84(
                        bufferBuilder53,
                        matrix4f32,
                        items19,
                        currentValue13 * sjzMjyMYX7ylg4dN2.currentValue20,
                        getStateActive837(value140, amount559 * currentValue207)
                     );
                     getStateActive84(
                        bufferBuilder53,
                        matrix4f32,
                        items19,
                        currentValue240 * sjzMjyMYX7ylg4dN2.currentValue20,
                        getStateActive837(value142, amount559 * currentValue533)
                     );
                     getStateActive84(bufferBuilder53, matrix4f32, items19, currentValue35 * sjzMjyMYX7ylg4dN2.currentValue20, getStateActive837(value143, amount559));
                     getStateActive84(bufferBuilder53, matrix4f32, items19, currentValue961 * sjzMjyMYX7ylg4dN2.currentValue20, getStateActive837(-1, amount559));
                  }
               }
            }
         }

         getStateActive67(bufferBuilder52, resourceIdentifier8, false, renderPipeline26);
         getStateActive67(bufferBuilder53, resourceIdentifier4, false, renderPipeline26);
      }
   }

   private static double[] getStateActive82(boolean matches12) {
      double[] instance17 = new double[73];

      for (int index60 = 0; index60 <= 72; index60++) {
         double angleRadians17 = Math.toRadians(index60 * currentValue1124);
         instance17[index60] = matches12 ? Math.cos(angleRadians17) : Math.sin(angleRadians17);
      }

      return instance17;
   }

   private static int getStateActive820(int value144, int value145, int value146, boolean matches13) {
      int nowMillis11 = (int)((System.currentTimeMillis() / lastUpdateMillis29 + value146) % lastUpdateMillis33);
      if (nowMillis11 >= 180) {
         nowMillis11 = 360 - nowMillis11;
      }

      int color15 = ColorMath.interpolateRgba(value144, value145, nowMillis11 / currentValue814);
      return matches13 ? getTextureTarget22(color15, currentValue178) : color15;
   }

   private static void getStateActive810() {
      currentValue256 = 1.0F;
      currentValue505 = 0.0F;
      currentValue378 = 0.0;
      stateActive5 = false;
      stateCounter40 = stateCounter47;
   }

   private static void getStateActive813(BufferBuilder bufferBuilder54, BufferBuilder bufferBuilder55, RenderPipeline renderPipeline4) {
      getStateActive818(bufferBuilder54, bufferBuilder55, renderPipeline4, -1);
   }

    private static void renderOrbitingParticles(DeltaTrackerPoseStackEvent event, Vec3 targetPosition, LivingEntity target, int color, float opacity) {
       Vec3 cameraRelativePosition = targetPosition.subtract(TargetEspPreviewContext.camera().position());
       float radius = target.getBbWidth() * currentValue146;
       float animationPhase = (float)(System.currentTimeMillis() % lastUpdateMillis36) / currentValue264;
       int segmentCount = Mth.clamp(stateCounter15, 2, 4);
       float segmentDegrees = currentValue173 / segmentCount;
       float visibleArcDegrees = currentValue712;
       float gapDegrees = segmentDegrees - visibleArcDegrees;
       int arcProgressDegrees = 0;
       int remainingGapDegrees = 0;
       int bandIndex = 0;
       Matrix4f pose = event.getPoseStack().last().pose();
       BufferBuilder vertices = getBufferBuilder3();

       for (int angleDegrees = 0; angleDegrees < 360; angleDegrees += 2) {
          if (remainingGapDegrees > 0) {
             remainingGapDegrees -= 2;
             if (remainingGapDegrees <= 0) {
                bandIndex++;
             }
             continue;
          }

          float outerSize = currentValue1142 + currentValue1171 * arcProgressDegrees;
          float innerSize = currentValue543 + currentValue491 * arcProgressDegrees;
          arcProgressDegrees += 2;
          if (arcProgressDegrees > visibleArcDegrees) {
             remainingGapDegrees = (int)gapDegrees;
             arcProgressDegrees = 0;
             continue;
          }

          float radiusScale = Math.max(currentValue761, currentValue221 - currentValue9 * opacity);
          float radians = (float)Math.toRadians(angleDegrees + animationPhase);
          float x = (float)(cameraRelativePosition.x + Math.sin(radians) * radius * radiusScale);
          float z = (float)(cameraRelativePosition.z + Math.cos(radians) * radius * radiusScale);
          float bandOffset = (bandIndex % 2 == 0 ? currentValue1027 : currentValue1106) * target.getBbHeight();
          float wave = (float)Math.sin(Math.toRadians(angleDegrees / 2.0F + animationPhase / currentValue647));
          float y = (float)(cameraRelativePosition.y + target.getBbHeight() * currentValue617
             + target.getBbHeight() / currentValue313 * wave + bandOffset);
          addCameraFacingSprite(vertices, pose, x, y, z, innerSize, getStateActive837(color, opacity * currentValue693));
          addCameraFacingSprite(vertices, pose, x, y, z, outerSize, getStateActive837(color, opacity));
       }

       updateState9(vertices, resourceIdentifier4, true);
    }

   private static void getStateActive818(BufferBuilder bufferBuilder56, BufferBuilder bufferBuilder57, RenderPipeline renderPipeline5, int value147) {
      RenderTarget renderTarget4 = TargetEspPreviewContext.target();
      if (renderTarget4 != null && renderTarget4.getColorTextureView() != null && renderTarget4.getDepthTextureView() != null) {
         TextureTarget textureTarget = getTextureTarget212(renderTarget4.width, renderTarget4.height);
         textureTarget.copyDepthFrom(renderTarget4);
         getStateActive856(bufferBuilder57, renderPipeline5, textureTarget, true);
         BloomCompositor.updateState(textureTarget, renderTarget4, value147, currentValue26, currentValue336, 1.0F);
         getStateActive611(bufferBuilder56, renderPipeline5);
         CommandEncoder commandEncoder4 = RenderSystem.getDevice().createCommandEncoder();
         RenderPass renderPass4 = commandEncoder4.createRenderPass(() -> string44, renderTarget4.getColorTextureView(), Optional.empty());

         try {
            if (RenderPipelineCache.isCompiled(renderPipeline22)) {
               renderPass4.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline22));
               renderPass4.setUniform(string110, textureTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
               renderPass4.draw(3, 1, 0, 0);
            }
         } catch (Throwable throwable7) {
            if (renderPass4 != null) {
               try {
                  renderPass4.close();
               } catch (Throwable throwable8) {
                  throwable7.addSuppressed(throwable8);
               }
            }

            throw throwable7;
         }

         if (renderPass4 != null) {
            renderPass4.close();
         }
      }
   }

   public static void renderTargetEsp(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent20, LivingEntity entity23, String text3, int value148, int value149) {
      if (stateActive13
         && stateActive4
         && (text3.equals(string131) || text3.equals(string160) || text3.equals(string154))
         && (entity23 != null && entity23.isAlive() || isTextureTarget2())) {
         TargetLightingCompositor.updateState();
      }

      if (stateActive13 && isStateActive6(text3)) {
         deltaTrackerPoseStackEvent28 = deltaTrackerPoseStackEvent20;
         livingEntity2 = entity23;
         string14 = text3;
         stateCounter43 = value148;
         stateCounter32 = value149;
      } else {
         long nowMillis12 = System.currentTimeMillis();
         float amount565 = lastUpdateMillis45 == 0L ? 0.0F : Math.min(currentValue292, (float)(nowMillis12 - lastUpdateMillis45) / currentValue123);
         lastUpdateMillis45 = nowMillis12;
         boolean alive = entity23 != null && entity23.isAlive();
         if (alive) {
            stateCounter14 = value148;
            stateCounter10 = value149;
         }

         boolean matches14 = text3.equals(string15) && string149.equals(string70);
         float amount566 = matches14 ? 1.0F : (!alive && text3.equals(string116) ? currentValue1187 : currentValue1092);
         currentValue629 = getStateActive824(currentValue629, alive ? 1.0F : 0.0F, amount565 / amount566);
         if (alive) {
            livingEntity = entity23;
         }

         if (!string109.equals(text3)) {
            if (string101.equals(string109) && string55.equals(string71)) {
               TargetEspShader.updateState2();
            }

            currentValue1101 = 0.0F;
            list2.clear();
            stateCounter57 = stateCounter24;
            list5.clear();
            stateCounter8 = stateCounter11;
            list4.clear();
            lastUpdateMillis30 = 0L;
            list.clear();
            lastUpdateMillis9 = 0L;
            list3.clear();
            lastUpdateMillis27 = 0L;
            getStateActive848();
            list9.clear();
            currentValue573 = 0.0;
            stateCounter36 = stateCounter44;
            stateActive8 = false;
            currentValue246 = 0.0F;
            stateActive7 = false;
            stateActive10 = false;
            currentValue57 = 0.0;
            currentValue1034 = 0.0;
            currentValue519 = 0.0;
            currentValue949 = 0.0;
            currentValue836 = 0.0;
            currentValue791 = 0.0;
            string47 = "";
            string111 = "";
            string71 = "";
            linkedList3.clear();
            lastUpdateMillis35 = 0L;
            stateCounter31 = stateCounter9;
            getStateActive810();
            string109 = text3;
         }

         boolean matches15 = text3.equals(string146) && string168.equals(string161);
         float amount567 = lastUpdateMillis41 == 0L ? 0.0F : (float)(nowMillis12 - lastUpdateMillis41) / currentValue739;
         if (matches15) {
            currentValue25 = currentValue25
               + ((alive ? 1.0F : 0.0F) - currentValue25)
                  * (float)Math.sin(Math.min(amount567 / currentValue580, 1.0F) * currentValue228 / currentValue974);
            currentValue629 = currentValue25;
            lastUpdateMillis41 = nowMillis12;
         } else {
            lastUpdateMillis41 = 0L;
            currentValue25 = 0.0F;
            currentValue820 = 0.0F;
         }

         if (livingEntity != null && !(currentValue629 <= (matches15 ? currentValue141 : currentValue468))) {
            currentValue1101 += amount565;
            float amount568 = deltaTrackerPoseStackEvent20.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Vec3 position171 = new Vec3(
               Mth.lerp(amount568, livingEntity.xo, livingEntity.getX()),
               Mth.lerp(amount568, livingEntity.yo, livingEntity.getY()),
               Mth.lerp(amount568, livingEntity.zo, livingEntity.getZ())
            );
            float amount569 = livingEntity.hurtDuration <= 0
               ? 0.0F
               : Mth.clamp((float)livingEntity.hurtTime / livingEntity.hurtDuration, 0.0F, 1.0F);
            int color16 = stateActive ? ColorMath.interpolateRgba(value148, stateCounter52, amount569) : value148;
            int color17 = stateActive ? ColorMath.interpolateRgba(value149, stateCounter12, amount569) : value149;
            float amount570 = matches15 ? currentValue629 : getStateActive68(currentValue629);
            TargetLightingCompositor.updateState5(
               stateActive4 && (text3.equals(string30) || text3.equals(string16) || text3.equals(string126)),
               deltaTrackerPoseStackEvent20.getPoseStack().last().pose()
            );

            try {
               switch (text3) {
                  case string78:
                     if (string8.equals(string149)) {
                        float amount571 = alive
                           ? getStateActive87(currentValue629)
                           : 1.0F - (1.0F - currentValue629) * (1.0F - currentValue629);
                        onDeltaTrackerPoseStack5(deltaTrackerPoseStackEvent20, position171, livingEntity, getStateActive63(livingEntity, value148, amount565, nowMillis12), amount571);
                     } else if (string127.equals(string149)) {
                        onDeltaTrackerPoseStack14(deltaTrackerPoseStackEvent20, position171, livingEntity, value148, value149, amount570, amount568);
                     } else {
                        onDeltaTrackerPoseStack12(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570);
                     }
                     break;
                  case string39:
                     onDeltaTrackerPoseStack24(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570);
                     break;
                  case string50:
                     if (!string47.equals(string52)) {
                        getStateActive848();
                        list9.clear();
                        currentValue573 = 0.0;
                        linkedList3.clear();
                        lastUpdateMillis35 = 0L;
                        stateCounter31 = stateCounter51;
                        getStateActive810();
                        string47 = string52;
                     }

                     String text4 = string52;
                     switch (text4) {
                        case string130:
                           onDeltaTrackerPoseStack23(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, color17, amount570);
                           return;
                        case string153:
                           onDeltaTrackerPoseStack25(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, color17, amount570);
                           return;
                        case string104:
                           onDeltaTrackerPoseStack7(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, color17, alive);
                           return;
                        case string37:
                           onDeltaTrackerPoseStack8(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, color17, currentValue629, amount568);
                           return;
                        case string45:
                           renderOrbitingParticles(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570);
                           return;
                        case string9:
                           onDeltaTrackerPoseStack16(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570);
                           return;
                        case string75:
                           onDeltaTrackerPoseStack(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570, amount565, alive);
                           return;
                        default:
                           onDeltaTrackerPoseStack18(deltaTrackerPoseStackEvent20, position171, color16, amount570);
                           return;
                     }
                  case string51:
                     if (!string111.equals(string168)) {
                        list2.clear();
                        stateCounter57 = stateCounter;
                        list5.clear();
                        stateCounter8 = stateCounter3;
                        string111 = string168;
                     }

                     String text5 = string168;
                     switch (text5) {
                        case string89:
                           onDeltaTrackerPoseStack21(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570, nowMillis12);
                           return;
                        case string31:
                           onDeltaTrackerPoseStack11(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570, nowMillis12);
                           return;
                        case string54:
                           onDeltaTrackerPoseStack6(deltaTrackerPoseStackEvent20, position171, livingEntity, value148, amount570, amount567, nowMillis12);
                           return;
                        default:
                           onDeltaTrackerPoseStack3(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, value149, amount570, nowMillis12);
                           return;
                     }
                  case string28:
                     onDeltaTrackerPoseStack19(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, value149, amount570, nowMillis12);
                     break;
                  case string12:
                     try {
                        if (!string71.equals(string107)) {
                           if (string119.equals(string71)) {
                              TargetEspShader.updateState2();
                           }

                           list4.clear();
                           list.clear();
                           list3.clear();
                           lastUpdateMillis30 = 0L;
                           lastUpdateMillis9 = 0L;
                           lastUpdateMillis27 = 0L;
                           string71 = string107;
                        }

                        if (string139.equals(string107)) {
                           onDeltaTrackerPoseStack4(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570, nowMillis12, alive, amount569);
                        } else if (string164.equals(string107)) {
                           TargetEspShader.updateState8(
                              TargetEspPreviewContext.camera(),
                              amount568,
                              livingEntity,
                              amount570,
                              value148,
                              stateActive,
                              stateActive6 ? string32 : string56
                           );
                        } else {
                           onDeltaTrackerPoseStack10(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570, nowMillis12, alive);
                        }
                     } catch (Throwable throwable9) {
                        throwable9.printStackTrace();
                        list4.clear();
                        list.clear();
                        list3.clear();
                        lastUpdateMillis30 = 0L;
                        lastUpdateMillis9 = 0L;
                        lastUpdateMillis27 = 0L;
                     }
                     break;
                  case string134:
                     onDeltaTrackerPoseStack17(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, currentValue629, amount565, alive, amount569);
                     break;
                  case string33:
                     onDeltaTrackerPoseStack20(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570);
                     break;
                  case string25:
                     onDeltaTrackerPoseStack15(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570, nowMillis12);
                     break;
                  case string2:
                     onDeltaTrackerPoseStack2(deltaTrackerPoseStackEvent20, position171, livingEntity, color16, amount570, amount569);
               }
            } finally {
               TargetLightingCompositor.updateState7(deltaTrackerPoseStackEvent20.getMatrix4f2());
            }
         } else {
            if (!alive) {
               livingEntity = null;
            }

            getStateActive848();
            list9.clear();
            linkedList3.clear();
         }
      }
   }

   private static void onDeltaTrackerPoseStack20(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent21, Vec3 position172, LivingEntity entity24, int value150, float amount572) {
      position172 = position172.subtract(TargetEspPreviewContext.camera().position());
      boolean matches16 = string41.equals(string170);
      float amount573 = entity24.hurtDuration <= 0 ? 0.0F : Mth.clamp((float)entity24.hurtTime / entity24.hurtDuration, 0.0F, 1.0F);
      float amount574 = Mth.lerpInt(currentValue629, 2, 1) * (matches16 ? currentValue276 : currentValue1028);
      float amount575 = (float)Math.sin(currentValue1101 * currentValue143) * currentValue1159;
      int value151 = value150;
      if (matches16) {
         int color18 = Mth.clamp((int)(ColorMath.redChannel(value150) * (1.0F - amount573) + currentValue956 * amount573), 0, 255);
         int color19 = Mth.clamp((int)(ColorMath.greenChannel(value150) * (1.0F - amount573)), 0, 255);
         int color20 = Mth.clamp((int)(ColorMath.blueChannel(value150) * (1.0F - amount573)), 0, 255);
         value151 = ColorMath.packRgba(color18, color19, color20, ColorMath.alphaChannel(value150));
      }

      BufferBuilder bufferBuilder58 = getBufferBuilder3();
      PoseStack poseStack19 = deltaTrackerPoseStackEvent21.getPoseStack();
      poseStack19.pushPose();
      poseStack19.translate(position172.x, position172.y + entity24.getBbHeight() * currentValue191, position172.z);
      poseStack19.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
      poseStack19.mulPose(new Matrix4f().rotate(Axis.ZP.rotationDegrees(amount575)));
      addBillboardQuad(bufferBuilder58, poseStack19.last().pose(), amount574, getStateActive837(value151, amount572));
      poseStack19.popPose();
      updateState9(bufferBuilder58, matches16 ? resourceIdentifier11 : resourceIdentifier10, true);
   }

   private static void onDeltaTrackerPoseStack4(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent22, Vec3 position173, LivingEntity entity25, int value152, float amount576, long value153, boolean matches17, float amount577) {
      if (matches17 && value153 - lastUpdateMillis30 >= lastUpdateMillis10 && list4.size() < 5) {
         list4.add(getStateActive834(entity25, position173, value153));
         if (random5.nextFloat() < currentValue405 && list4.size() < 5) {
            list4.add(getStateActive834(entity25, position173, value153 + lastUpdateMillis12));
         }

         lastUpdateMillis30 = value153;
      }

      Iterator iterator3 = list4.iterator();

      while (iterator3.hasNext()) {
         TargetEspRenderer.vbgKRi4F34zTzWmv instance18 = (TargetEspRenderer.vbgKRi4F34zTzWmv)iterator3.next();
         if (value153 - instance18.lastUpdateMillis5 > instance18.lastUpdateMillis24) {
            iterator3.remove();
         }
      }

      if (!list4.isEmpty()) {
         int value154 = getTextureTarget22(value152, currentValue657);
         int value155 = getTextureTarget22(value152, currentValue950);
         BufferBuilder bufferBuilder59 = getBufferBuilder3();
         BufferBuilder bufferBuilder60 = getBufferBuilder3();
         Matrix4f matrix4f33 = deltaTrackerPoseStackEvent22.getPoseStack().last().pose();
         Vec3 position174 = TargetEspPreviewContext.camera().position();

         for (TargetEspRenderer.vbgKRi4F34zTzWmv instance19 : list4) {
            float amount578 = (float)(value153 - instance19.lastUpdateMillis5) / (float)instance19.lastUpdateMillis24;
            if (!(amount578 < 0.0F)) {
               float amount579 = getStateActive87(Mth.clamp(amount578 / currentValue975, 0.0F, 1.0F));
               float amount580 = 1.0F - getStateActive68(Mth.clamp((amount578 - currentValue1098) / currentValue428, 0.0F, 1.0F));
               float amount581 = 1.0F - getStateActive68(Mth.clamp(amount578 / currentValue94, 0.0F, 1.0F));
               float amount582 = currentValue306 + currentValue133 * (float)Math.sin(value153 * currentValue752 + instance19.currentValue158);
               float amount583 = amount576 * amount580 * amount582;
               if (!(amount583 <= currentValue936)) {
                  Vec3 position175 = position173.subtract(instance19.vec3);

                  for (TargetEspRenderer.FDRJZV2OjTT35QNI fdrjzv2ojtt35qni : instance19.list6) {
                     float amount584 = Mth.clamp(
                        (amount579 - fdrjzv2ojtt35qni.currentValue138) / Math.max(currentValue937, 1.0F - fdrjzv2ojtt35qni.currentValue138), 0.0F, 1.0F
                     );
                     if (!(amount584 <= currentValue391)) {
                        ArrayList items20 = new ArrayList(fdrjzv2ojtt35qni.list7.size());

                        for (Vec3 position176 : fdrjzv2ojtt35qni.list7) {
                           items20.add(position176.add(position175).subtract(position174));
                        }

                        float amount585 = amount583 * (currentValue179 + fdrjzv2ojtt35qni.currentValue40 * currentValue238);

                        for (int index61 = 1; index61 <= 2; index61++) {
                           float size14 = (items20.size() - 1) * amount584 * index61 * currentValue808;
                           int size15 = Math.min((int)size14, items20.size() - 1);
                           Vec3 position177 = ((Vec3)items20.get(size15)).lerp((Vec3)items20.get(Math.min(size15 + 1, items20.size() - 1)), size14 - size15);
                           TargetLightingCompositor.updateState4(
                              (float)position177.x,
                              (float)position177.y,
                              (float)position177.z,
                              currentValue570 * fdrjzv2ojtt35qni.currentValue40,
                              getStateActive837(value152, amount585)
                           );
                        }

                        getStateActive65(
                           bufferBuilder59,
                           matrix4f33,
                           items20,
                           amount584,
                           fdrjzv2ojtt35qni.currentValue40 * currentValue96,
                           getStateActive837(value154, amount585 * (currentValue464 + amount581 * currentValue633)),
                           instance19.currentValue158,
                           0.0F
                        );
                        getStateActive65(
                           bufferBuilder60,
                           matrix4f33,
                           items20,
                           amount584,
                           fdrjzv2ojtt35qni.currentValue40 * currentValue520,
                           getStateActive837(value155, Math.min(1.0F, amount585 * (1.0F + amount581))),
                           instance19.currentValue158 + currentValue784,
                           0.0F
                        );
                        getStateActive65(
                           bufferBuilder59,
                           matrix4f33,
                           items20,
                           amount584,
                           fdrjzv2ojtt35qni.currentValue40 * currentValue1039,
                           getStateActive837(-1, Math.min(1.0F, amount585 * (currentValue215 + amount581 * currentValue494))),
                           instance19.currentValue158 + currentValue809,
                           0.0F
                        );
                     }
                  }
               }
            }
         }

         getStateActive818(bufferBuilder59, bufferBuilder60, renderPipeline20, value152);
      }
   }

   private static void onDeltaTrackerPoseStack24(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent23, Vec3 position178, LivingEntity entity26, int value156, float amount586) {
      position178 = position178.subtract(TargetEspPreviewContext.camera().position());
      float amount587 = getStateActive87(amount586);
      float amount588 = entity26.getBbHeight();
      float amount589 = Mth.lerp(amount587, currentValue610, 1.0F);
      float amount590 = Math.max(currentValue670, entity26.getBbWidth() * currentValue161 + currentValue12) * amount589;
      double id2 = currentValue1101 * currentValue89 + entity26.getId() * currentValue824;
      double angleRadians18 = Math.toRadians(currentValue1120);
      Vec3 position179 = new Vec3(Math.sin(angleRadians18) * Math.cos(id2), Math.cos(angleRadians18), Math.sin(angleRadians18) * Math.sin(id2));
      Vec3[] vec3Array13 = getStateActive811(position179);
      Vec3 position180 = position178.add(0.0, amount588 * currentValue22, 0.0);
      Matrix4f matrix4f34 = deltaTrackerPoseStackEvent23.getPoseStack().last().pose();
      BufferBuilder bufferBuilder61 = getBufferBuilder3();
      updateState14(
         bufferBuilder61, matrix4f34, position180, vec3Array13[0], vec3Array13[1], amount590 * currentValue1055, getStateActive837(value156, amount587 * currentValue283)
      );
      updateState14(
         bufferBuilder61,
         matrix4f34,
         position180,
         vec3Array13[0],
         vec3Array13[1],
         amount590 * currentValue1192,
         getStateActive837(getTextureTarget22(value156, currentValue590), amount587 * currentValue931)
      );
      getStateActive611(bufferBuilder61, renderPipeline21);
      BufferBuilder bufferBuilder62 = getBufferBuilder3();
      double amount591 = currentValue1101 * currentValue567;

      for (int index62 = 0; index62 < 13; index62++) {
         float amount592 = 1.0F - index62 / currentValue930;
         double amount593 = amount591 - index62 * currentValue151;
         Vec3 position181 = position180.add(vec3Array13[0].scale(Math.cos(amount593) * amount590)).add(vec3Array13[1].scale(Math.sin(amount593) * amount590));
         float amount594 = amount587 * amount592 * amount592 * currentValue180;
         float amount595 = currentValue941 + amount592 * currentValue1116;
         addCameraFacingSprite(
            bufferBuilder62, matrix4f34, position181.x, position181.y, position181.z, amount595, getStateActive837(getTextureTarget22(value156, currentValue561), amount594)
         );
      }

      updateState9(bufferBuilder62, resourceIdentifier8, true);
   }

   private static int getStateActive829(int value157) {
      int value158 = value157 * stateCounter41 + stateCounter30;
      value158 = (value158 ^ value158 >> 13) * stateCounter16;
      return value158 & 0xFF;
   }

   public static void updateState8(boolean matches18, boolean matches19) {
      stateActive2 = matches18;
      stateActive12 = matches19;
   }

   private static int getStateActive832(int value159) {
      float color21 = ColorMath.alphaChannel(value159) / currentValue72;
      float color22 = ColorMath.redChannel(value159) * color21;
      float color23 = ColorMath.greenChannel(value159) * color21;
      float color24 = ColorMath.blueChannel(value159) * color21;
      float amount596 = Math.max(color22, Math.max(color23, color24)) * currentValue813;
      if (amount596 <= 0.0F) {
         return 0;
      } else {
         int value160 = Mth.clamp(Math.round(amount596), 1, 255);
         float amount597 = currentValue509 / (value160 * 2.0F);
         return ColorMath.packRgba(
            Mth.clamp(Math.round(color22 * amount597), 0, 255), Mth.clamp(Math.round(color23 * amount597), 0, 255), Mth.clamp(Math.round(color24 * amount597), 0, 255), value160
         );
      }
   }

   public static void updateState6(
      float amount598, String text6, int value161, boolean matches20, String text7, String text8, boolean matches21, String text9, boolean matches22, boolean matches23, String text10
   ) {
      currentValue377 = Mth.clamp(amount598, currentValue742, currentValue227);
      string149 = text6 != null && !text6.isEmpty() ? text6 : string150;
      string170 = text10 != null && !text10.isEmpty() ? text10 : string63;
      stateCounter15 = Mth.clamp(value161, 2, 4);
      stateActive = matches20;
      string52 = text7 != null && !text7.isEmpty() ? text7 : string64;
      string168 = text8 != null && !text8.isEmpty() ? text8 : string129;
      stateActive11 = matches21;
      string107 = text9 != null && !text9.isEmpty() ? text9 : string138;
      stateActive6 = matches22;
      stateActive4 = matches23;
   }

   private static int getStateActive854(float amount599, int value162, int value163) {
      float amount600 = Mth.clamp((currentValue317 - amount599) / currentValue46, 0.0F, 1.0F);
      int color25 = ColorMath.blendRgba(value163, -1, currentValue407);
      int color26 = ColorMath.blendRgba(color25, value163, amount600);
      float amount601 = (getStateActive83(value162) - currentValue452) * currentValue563;
      return ColorMath.blendRgba(color26, amount601 >= 0.0F ? -1 : value163, Math.abs(amount601));
   }

   private static void getStateActive842(BufferBuilder bufferBuilder63, RenderPipeline renderPipeline6) {
      MeshData meshData3 = bufferBuilder63.build();
      if (meshData3 != null) {
         GpuBuffer gpuBuffer5 = null;

         try {
            DrawState drawState3 = meshData3.drawState();
            int value164 = drawState3.indexCount();
            AutoStorageIndexBuffer autoStorageIndexBuffer3 = RenderSystem.getSequentialBuffer(drawState3.primitiveTopology());
            GpuBuffer gpuBuffer6 = autoStorageIndexBuffer3.getBuffer(value164);
            IndexType indexType3 = autoStorageIndexBuffer3.type();
            gpuBuffer5 = RenderSystem.getDevice().createBuffer(() -> string152, 32, meshData3.vertexBuffer());
            RenderTarget renderTarget5 = TargetEspPreviewContext.target();
            CommandEncoder commandEncoder5 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass renderPass5 = commandEncoder5.createRenderPass(
               () -> string77, renderTarget5.getColorTextureView(), Optional.empty(), renderTarget5.getDepthTextureView(), OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(renderPass5);
               if (RenderPipelineCache.isCompiled(renderPipeline6)) {
                  renderPass5.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline6));
                  renderPass5.setVertexBuffer(0, gpuBuffer5.slice());
                  renderPass5.setIndexBuffer(gpuBuffer6, indexType3);
                  renderPass5.drawIndexed(value164, 1, 0, 0, 0);
               }
            } catch (Throwable throwable10) {
               if (renderPass5 != null) {
                  try {
                     renderPass5.close();
                  } catch (Throwable throwable11) {
                     throwable10.addSuppressed(throwable11);
                  }
               }

               throw throwable10;
            }

            if (renderPass5 != null) {
               renderPass5.close();
            }
         } finally {
            if (gpuBuffer5 != null) {
               gpuBuffer5.close();
            }

            meshData3.close();
         }
      }
   }

   private static float getStateActive853(float amount602, float amount603) {
      int value165 = (int)Math.floor(amount602) & 0xFF;
      int value166 = (int)Math.floor(amount603) & 0xFF;
      float amount604 = amount602 - (float)Math.floor(amount602);
      float amount605 = amount603 - (float)Math.floor(amount603);
      float amount606 = getStateActive613(amount604);
      float amount607 = getStateActive613(amount605);
      int value167 = getStateActive829(value165) + value166;
      int value168 = getStateActive829(value165) + value166 + 1;
      int value169 = getStateActive829(value165 + 1) + value166;
      int value170 = getStateActive829(value165 + 1) + value166 + 1;
      float amount608 = Mth.lerp(
         amount606, getStateActive851(getStateActive829(value167), amount604, amount605), getStateActive851(getStateActive829(value169), amount604 - 1.0F, amount605)
      );
      float amount609 = Mth.lerp(
         amount606,
         getStateActive851(getStateActive829(value168), amount604, amount605 - 1.0F),
         getStateActive851(getStateActive829(value170), amount604 - 1.0F, amount605 - 1.0F)
      );
      return Mth.clamp(Mth.lerp(amount607, amount608, amount609) * currentValue379 + currentValue782, 0.0F, 1.0F);
   }

   public static void onDeltaTrackerPoseStack13(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent24) {
      if (livingEntity != null && currentValue629 > currentValue694) {
         renderTargetEsp(deltaTrackerPoseStackEvent24, null, string109, stateCounter14, stateCounter10);
      }
   }

   private static void getStateActive821(LivingEntity entity27) {
      float amount610 = entity27.getBbWidth() * currentValue854;
      float amount611 = entity27.getBbHeight();
      if (stateCounter8 != entity27.getId() || currentValue924 != amount610 || currentValue27 != amount611) {
         stateCounter8 = entity27.getId();
         currentValue924 = amount610;
         currentValue27 = amount611;
         list5.clear();
         Random random3 = new Random(entity27.getId() * lastUpdateMillis25);
         int value171 = (int)(amount610 + amount611 * currentValue873);

         for (int index63 = 0; index63 < value171; index63++) {
            Vector3f position182 = null;
            float amount612 = currentValue99;

            for (int index64 = 0; index64 < 15; index64++) {
               float amount613 = random3.nextFloat() * currentValue184;
               float amount614 = random3.nextFloat() * amount611;
               float angleRadians19 = (float)Math.toRadians(amount613);
               float amount615 = (float)Math.sin(angleRadians19) * amount610;
               float amount616 = (float)Math.cos(angleRadians19) * amount610;
               if (list5.isEmpty()) {
                  position182 = new Vector3f(amount615, amount614, amount616);
                  break;
               }

               float amount617 = currentValue192;

               for (Vector3f position183 : list5) {
                  float amount618 = position183.x - amount615;
                  float amount619 = position183.y - amount614;
                  float amount620 = position183.z - amount616;
                  amount617 = Math.min(amount617, amount618 * amount618 + amount619 * amount619 + amount620 * amount620);
               }

               if (amount617 > amount612) {
                  amount612 = amount617;
                  position182 = new Vector3f(amount615, amount614, amount616);
               }
            }

            if (position182 != null) {
               list5.add(position182);
            }
         }
      }
   }

   private static Vec3 getStateActive822(Vec3 position184) {
      Vec3 position185 = position184.normalize();
      Vec3 position186 = Math.abs(position185.y) < currentValue1009 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 position187 = position185.cross(position186).normalize();
      Vec3 position188 = position185.cross(position187).normalize();
      double amount621 = random5.nextDouble() * currentValue265 * currentValue870;
      return position187.scale(Math.cos(amount621)).add(position188.scale(Math.sin(amount621)));
   }

   private static void getStateActive856(BufferBuilder bufferBuilder64, RenderPipeline renderPipeline7, RenderTarget renderTarget6, boolean matches24) {
      MeshData meshData4 = bufferBuilder64.build();
      if (meshData4 != null) {
         GpuBuffer gpuBuffer7 = null;

         try {
            DrawState drawState4 = meshData4.drawState();
            int value172 = drawState4.indexCount();
            AutoStorageIndexBuffer autoStorageIndexBuffer4 = RenderSystem.getSequentialBuffer(drawState4.primitiveTopology());
            GpuBuffer gpuBuffer8 = autoStorageIndexBuffer4.getBuffer(value172);
            IndexType indexType4 = autoStorageIndexBuffer4.type();
            gpuBuffer7 = RenderSystem.getDevice().createBuffer(() -> string132, 32, meshData4.vertexBuffer());
            CommandEncoder commandEncoder6 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass renderPass6 = commandEncoder6.createRenderPass(
               () -> string102,
               renderTarget6.getColorTextureView(),
               matches24 ? Optional.of(new Vector4f(0.0F)) : Optional.empty(),
               renderTarget6.getDepthTextureView(),
               OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(renderPass6);
               if (RenderPipelineCache.isCompiled(renderPipeline7)) {
                  renderPass6.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline7));
                  renderPass6.setVertexBuffer(0, gpuBuffer7.slice());
                  renderPass6.setIndexBuffer(gpuBuffer8, indexType4);
                  renderPass6.drawIndexed(value172, 1, 0, 0, 0);
               }
            } catch (Throwable throwable12) {
               if (renderPass6 != null) {
                  try {
                     renderPass6.close();
                  } catch (Throwable throwable13) {
                     throwable12.addSuppressed(throwable13);
                  }
               }

               throw throwable12;
            }

            if (renderPass6 != null) {
               renderPass6.close();
            }
         } finally {
            if (gpuBuffer7 != null) {
               gpuBuffer7.close();
            }

            meshData4.close();
         }
      }
   }

   private static void addColoredVertex(BufferBuilder bufferBuilder65, Matrix4f matrix4f35, float amount622, float amount623, float amount624, float amount625, float amount626, Vector4f vector4f15) {
      bufferBuilder65.addVertex(matrix4f35, amount622, amount623, amount624).setUv(amount625, amount626).setColor(vector4f15.x, vector4f15.y, vector4f15.z, vector4f15.w);
   }

   private static double getStateActive835(double amount627, double amount628, float amount629, int value173, int value174, int value175) {
      double amount630 = currentValue450 + (value173 > 0 ? currentValue664 : 0.0) + (value175 > 0 ? currentValue885 : 0.0);
      double amount631 = currentValue15 + (value174 > 0 ? currentValue75 : 0.0);
      double amount632 = amount627 + amount631 * amount629 - amount630 * amount629 * amount629;
      if (amount632 >= amount628) {
         return amount632;
      } else {
         double amount633 = Mth.clamp(Math.sqrt(Math.max(0.0, (amount627 - amount628) / amount630)), 0.0, currentValue315);
         double amount634 = Mth.clamp((amount629 - amount633) / Math.max(currentValue1185, 1.0 - amount633), 0.0, 1.0);
         double amount635 = currentValue86 + (value173 * value175 > 0 ? currentValue677 : 0.0);
         double amount636 = Math.abs(Math.sin(amount634 * currentValue766 * currentValue913)) * amount635 * (1.0 - amount634) * (1.0 - amount634);
         return amount628 + amount636;
      }
   }

   private static List<Vec3> getStateActive839(List<Vec3> items21, int value176) {
      if (items21.size() >= 3 && value176 > 1) {
         ArrayList items22 = new ArrayList((items21.size() - 1) * value176 + 1);

         for (int index65 = 0; index65 < items21.size() - 1; index65++) {
            Vec3 position189 = (Vec3)items21.get(Math.max(0, index65 - 1));
            Vec3 position190 = (Vec3)items21.get(index65);
            Vec3 position191 = (Vec3)items21.get(index65 + 1);
            Vec3 position192 = (Vec3)items21.get(Math.min(items21.size() - 1, index65 + 2));

            for (int index66 = 0; index66 < value176; index66++) {
               double amount637 = (double)index66 / value176;
               items22.add(getVec32(position189, position190, position191, position192, amount637));
            }
         }

         items22.add((Vec3)items21.get(items21.size() - 1));
         return items22;
      } else {
         return new ArrayList<>(items21);
      }
   }

   private static void onDeltaTrackerPoseStack21(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent25, Vec3 position193, LivingEntity entity28, int value177, float amount638, long value178) {
      position193 = position193.subtract(TargetEspPreviewContext.camera().position());
      getStateActive821(entity28);
      float amount639 = (float)(value178 % lastUpdateMillis18) / currentValue619;
      float amount640 = (float)value178 * currentValue1029;
      float amount641 = entity28.getBbWidth() * currentValue470;
      float amount642 = entity28.getBbHeight();
      float amount643 = currentValue69 - currentValue793 * amount638;
      float angleRadians20 = (float)Math.sin(Math.toRadians(amount639));
      float angleRadians21 = (float)Math.cos(Math.toRadians(amount639));
      int value179 = getStateActive837(value177, amount638);
      PoseStack poseStack20 = deltaTrackerPoseStackEvent25.getPoseStack();
      BufferBuilder bufferBuilder66 = getBufferBuilder3();

      for (int index67 = 0; index67 < list5.size(); index67++) {
         Vector3f position194 = list5.get(index67);
         float amount644 = position194.x * angleRadians21 - position194.z * angleRadians20;
         float amount645 = position194.x * angleRadians20 + position194.z * angleRadians21;
         float amount646 = amount644 * amount643;
         float amount647 = amount645 * amount643;
         float amount648 = position194.y + (float)(currentValue892 * Math.sin(amount640 * 2.0F + index67 * 1337));
         poseStack20.pushPose();
         poseStack20.translate(position193.x, position193.y, position193.z);
         getTextureTarget23(bufferBuilder66, poseStack20, amount646, amount648, amount647, currentValue1123, value179, amount642, amount639 + index67 * currentValue101);
         poseStack20.popPose();
         updateState13(
            bufferBuilder66, poseStack20, position193.x + amount646, position193.y + amount648, position193.z + amount647, currentValue1059, getStateActive837(value177, amount638 * currentValue757)
         );
         updateState13(
            bufferBuilder66, poseStack20, position193.x + amount646, position193.y + amount648, position193.z + amount647, currentValue458, getStateActive837(value177, amount638 * currentValue844)
         );
      }

      getStateActive67(bufferBuilder66, resourceIdentifier8, true, getStateActive64());
   }

   private static float getStateActive87(float amount649) {
      float amount650 = 1.0F - Mth.clamp(amount649, 0.0F, 1.0F);
      return 1.0F - amount650 * amount650 * amount650;
   }

   private static void getStateActive828(
      BufferBuilder bufferBuilder67, PoseStack poseStack21, float amount651, float amount652, float amount653, float amount654, int value180, int value181, float amount655, int value182, float amount656, boolean matches25
   ) {
      poseStack21.pushPose();
      poseStack21.translate(amount651, amount652, amount653);
      float amount657 = amount656 * currentValue447;
      float amount658 = -amount651;
      float amount659 = amount657 - amount652;
      float amount660 = -amount653;
      float amount661 = amount658 * amount658 + amount659 * amount659 + amount660 * amount660;
      if (amount661 > currentValue463) {
         float amount662 = 1.0F / (float)Math.sqrt(amount661);
         float amount663 = Mth.clamp(amount659 * amount662, currentValue1063, 1.0F);
         float amount664 = (float)Math.toDegrees(Math.atan2(amount658, amount660));
         float amount665 = -((float)Math.toDegrees(Math.asin(amount663)));
         poseStack21.mulPose(new Matrix4f().rotate(Axis.YP.rotationDegrees(amount664)));
         poseStack21.mulPose(new Matrix4f().rotate(Axis.XP.rotationDegrees(amount665)));
      }

      poseStack21.mulPose(new Matrix4f().rotate(Axis.ZP.rotationDegrees(amount655)));
      poseStack21.scale(amount654 * currentValue198, amount654 * currentValue773, amount654 * currentValue1191);
      Matrix4f matrix4f36 = poseStack21.last().pose();
      float amount666 = currentValue223 + calculateValue(value182, currentValue1066) * currentValue492;
      float amount667 = currentValue1078 * amount666;
      float amount668 = currentValue672 * (currentValue695 + calculateValue(value182, currentValue1038) * currentValue945);
      float amount669 = currentValue714 * (currentValue286 + calculateValue(value182, currentValue1155) * currentValue2);
      float amount670 = (calculateValue(value182, currentValue668) - currentValue1088) * currentValue756;
      float amount671 = (calculateValue(value182, currentValue947) - currentValue413) * currentValue38;
      int color27 = ColorMath.alphaChannel(value180);
      int color28 = ColorMath.packRgba(
         ColorMath.redChannel(value181), ColorMath.greenChannel(value181), ColorMath.blueChannel(value181), color27
      );
      int value183 = matches25 ? getTextureTarget22(value180, currentValue562) : value180;
      int value184 = matches25 ? getTextureTarget22(color28, currentValue569) : color28;
      int value185 = matches25 ? getTextureTarget22(value180, currentValue715) : value180;
      getStateActive849(bufferBuilder67, matrix4f36, amount670, amount671, amount668, -amount667, 0.0F, 0.0F, 0.0F, amount667, 0.0F, value183, value185, value185);
      getStateActive849(bufferBuilder67, matrix4f36, amount670, amount671, amount668, 0.0F, amount667, 0.0F, amount667, 0.0F, 0.0F, value183, value185, value185);
      getStateActive849(bufferBuilder67, matrix4f36, amount670, amount671, amount668, amount667, 0.0F, 0.0F, 0.0F, -amount667, 0.0F, value183, value185, value185);
      getStateActive849(bufferBuilder67, matrix4f36, amount670, amount671, amount668, 0.0F, -amount667, 0.0F, -amount667, 0.0F, 0.0F, value183, value185, value185);
      getStateActive849(
         bufferBuilder67, matrix4f36, -amount670 * currentValue829, -amount671 * currentValue1057, -amount669, 0.0F, amount667, 0.0F, -amount667, 0.0F, 0.0F, value184, value185, value185
      );
      getStateActive849(
         bufferBuilder67, matrix4f36, -amount670 * currentValue473, -amount671 * currentValue446, -amount669, amount667, 0.0F, 0.0F, 0.0F, amount667, 0.0F, value184, value185, value185
      );
      getStateActive849(
         bufferBuilder67, matrix4f36, -amount670 * currentValue258, -amount671 * currentValue964, -amount669, 0.0F, -amount667, 0.0F, amount667, 0.0F, 0.0F, value184, value185, value185
      );
      getStateActive849(
         bufferBuilder67, matrix4f36, -amount670 * currentValue1122, -amount671 * currentValue1157, -amount669, -amount667, 0.0F, 0.0F, 0.0F, -amount667, 0.0F, value184, value185, value185
      );
      poseStack21.popPose();
   }

   private static void getStateActive847(BufferBuilder bufferBuilder68, Matrix4f matrix4f37, Vec3 position195, Vec3 position196, Vec3 position197, Vec3 position198, double amount672, int value186, int value187) {
      Vec3 position199 = position198.subtract(position197).normalize();
      Vec3 position200 = Math.abs(position199.y) < currentValue1135 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 position201 = position199.cross(position200).normalize().scale(amount672);
      Vec3 position202 = position199.cross(position201).normalize().scale(amount672);

      for (int index68 = 0; index68 < value186; index68++) {
         double amount673 = currentValue233 * index68 / value186;
         double amount674 = currentValue837 * (index68 + 1) / value186;
         Vec3 position203 = position201.scale(Math.cos(amount673)).add(position202.scale(Math.sin(amount673)));
         Vec3 position204 = position201.scale(Math.cos(amount674)).add(position202.scale(Math.sin(amount674)));
         updateState11(
            bufferBuilder68,
            matrix4f37,
            getStateActive838(position197.add(position203), position195, position196),
            getStateActive838(position197.add(position204), position195, position196),
            getStateActive838(position198.add(position204), position195, position196),
            getStateActive838(position198.add(position203), position195, position196),
            value187
         );
      }
   }

   public static boolean isStateActive8() {
      return stateActive8;
   }

   private static void updateState22(BufferBuilder bufferBuilder69, BufferBuilder bufferBuilder70) {
      getStateActive818(bufferBuilder69, bufferBuilder70, renderPipeline20, -1);
   }

   private static void updateState9(BufferBuilder bufferBuilder71, Identifier identifier4, boolean matches26) {
      getStateActive67(bufferBuilder71, identifier4, matches26, renderPipeline29);
   }

   private static void onDeltaTrackerPoseStack15(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent26, Vec3 position205, LivingEntity entity29, int value188, float amount675, long value189) {
      position205 = position205.subtract(TargetEspPreviewContext.camera().position());
      float amount676 = (float)Math.sin(currentValue1101 * currentValue530) * currentValue271;
      float amount677 = 1.0F + currentValue886 * (float)Math.sin(currentValue1101 * 2.0F);
      float amount678 = (currentValue277 + entity29.getBbWidth() * currentValue418) * amount675 * getStateActive814(currentValue629) * amount677;
      if (!(amount678 <= currentValue338)) {
         double amount679 = position205.x;
         double amount680 = position205.y + entity29.getBbHeight() + currentValue417 + amount676;
         double amount681 = position205.z;
         float amount682 = -TargetEspPreviewContext.camera().yRot();
         PoseStack poseStack22 = deltaTrackerPoseStackEvent26.getPoseStack();
         BufferBuilder bufferBuilder72 = getBufferBuilder3();
         poseStack22.pushPose();
         poseStack22.translate(amount679, amount680, amount681);
         poseStack22.mulPose(new Matrix4f().rotate(Axis.YP.rotationDegrees(amount682)));
         int color29 = getStateActive837(ColorMath.blendRgba(value188, -1, currentValue424), currentValue921 * amount675);
         addBillboardQuad(bufferBuilder72, poseStack22.last().pose(), amount678 * currentValue536, color29);
         poseStack22.popPose();
         updateState9(bufferBuilder72, resourceIdentifier4, true);
         poseStack22.pushPose();
         poseStack22.translate(amount679, amount680, amount681);
         poseStack22.mulPose(new Matrix4f().rotate(Axis.YP.rotationDegrees(amount682)));
         poseStack22.scale(amount678, amount678, amount678);
         getStateActive819(poseStack22, value188, amount675, value189);
         poseStack22.popPose();
      }
   }

   private static void updateState17(
      BufferBuilder bufferBuilder73,
      PoseStack poseStack23,
      float amount683,
      float amount684,
      float amount685,
      float amount686,
      int value190,
      int value191,
      float amount687,
      boolean matches27,
      int value192,
      boolean matches28
   ) {
      poseStack23.pushPose();
      poseStack23.translate(amount683, amount684, amount685);
      poseStack23.mulPose(new Matrix4f().rotate(Axis.XP.rotationDegrees(amount687 * currentValue1032 + currentValue564)));
      poseStack23.mulPose(new Matrix4f().rotate(Axis.YP.rotationDegrees(amount687 * currentValue878 + currentValue304)));
      poseStack23.mulPose(new Matrix4f().rotate(Axis.ZP.rotationDegrees(amount687 + currentValue285)));
      poseStack23.scale(amount686 * currentValue982, amount686 * currentValue1087, amount686 * currentValue755);
      Matrix4f matrix4f38 = poseStack23.last().pose();
      float amount688 = currentValue926 * (currentValue472 + calculateValue(value192, currentValue703) * currentValue960);
      float amount689 = currentValue973 * (currentValue544 + calculateValue(value192, currentValue1179) * currentValue53);
      float amount690 = currentValue957 * (currentValue1131 + calculateValue(value192, currentValue320) * currentValue174);
      float amount691 = (calculateValue(value192, currentValue3) - currentValue490) * currentValue582;
      float amount692 = (calculateValue(value192, currentValue185) - currentValue1117) * currentValue47;
      int value193 = matches27 ? getTextureTarget22(value190, currentValue1041) : value190;
      int color30 = matches27
         ? ColorMath.packRgba(
            ColorMath.redChannel(value191),
            ColorMath.greenChannel(value191),
            ColorMath.blueChannel(value191),
            ColorMath.alphaChannel(value190)
         )
         : value190;
      int value194 = matches28 ? color30 : value193;
      int value195 = matches28 ? value193 : color30;
      getStateActive849(bufferBuilder73, matrix4f38, amount691, amount692, amount689, -amount688, 0.0F, 0.0F, 0.0F, amount688, 0.0F, value194, value190, value190);
      getStateActive849(bufferBuilder73, matrix4f38, amount691, amount692, amount689, 0.0F, amount688, 0.0F, amount688, 0.0F, 0.0F, value194, value190, value190);
      getStateActive849(bufferBuilder73, matrix4f38, amount691, amount692, amount689, amount688, 0.0F, 0.0F, 0.0F, -amount688, 0.0F, value194, value190, value190);
      getStateActive849(bufferBuilder73, matrix4f38, amount691, amount692, amount689, 0.0F, -amount688, 0.0F, -amount688, 0.0F, 0.0F, value194, value190, value190);
      getStateActive849(
         bufferBuilder73, matrix4f38, -amount691 * currentValue951, -amount692 * currentValue518, -amount690, 0.0F, amount688, 0.0F, -amount688, 0.0F, 0.0F, value195, value190, value190
      );
      getStateActive849(
         bufferBuilder73, matrix4f38, -amount691 * currentValue309, -amount692 * currentValue451, -amount690, amount688, 0.0F, 0.0F, 0.0F, amount688, 0.0F, value195, value190, value190
      );
      getStateActive849(
         bufferBuilder73, matrix4f38, -amount691 * currentValue551, -amount692 * currentValue1011, -amount690, 0.0F, -amount688, 0.0F, amount688, 0.0F, 0.0F, value195, value190, value190
      );
      getStateActive849(
         bufferBuilder73, matrix4f38, -amount691 * currentValue797, -amount692 * currentValue698, -amount690, -amount688, 0.0F, 0.0F, 0.0F, -amount688, 0.0F, value195, value190, value190
      );
      poseStack23.popPose();
   }

   private static void updateState11(BufferBuilder bufferBuilder74, Matrix4f matrix4f39, Vec3 position206, Vec3 position207, Vec3 position208, Vec3 position209, int value196) {
      getTextureTarget28(bufferBuilder74, matrix4f39, (float)position206.x, (float)position206.y, (float)position206.z, 0.0F, 0.0F, value196);
      getTextureTarget28(bufferBuilder74, matrix4f39, (float)position207.x, (float)position207.y, (float)position207.z, 1.0F, 0.0F, value196);
      getTextureTarget28(bufferBuilder74, matrix4f39, (float)position208.x, (float)position208.y, (float)position208.z, 1.0F, 1.0F, value196);
      getTextureTarget28(bufferBuilder74, matrix4f39, (float)position209.x, (float)position209.y, (float)position209.z, 0.0F, 1.0F, value196);
   }

   private static void updateState18(BufferBuilder bufferBuilder75, PoseStack poseStack24, float amount693, int value197) {
      poseStack24.pushPose();
      poseStack24.scale(amount693, amount693, amount693);
      Matrix4f matrix4f40 = poseStack24.last().pose();

      for (int index69 = 0; index69 < intArray.length; index69++) {
         int color31 = ColorMath.packRgba(
            Math.round(ColorMath.redChannel(value197) * floatArray2[index69]),
            Math.round(ColorMath.greenChannel(value197) * floatArray2[index69]),
            Math.round(ColorMath.blueChannel(value197) * floatArray2[index69]),
            ColorMath.alphaChannel(value197)
         );
         float[] instance20 = floatArray[intArray[index69][0]];
         float[] instance21 = floatArray[intArray[index69][1]];
         float[] instance22 = floatArray[intArray[index69][2]];
         Vector4f vector4f16 = ColorMath.getVector4f(color31);
         addColoredVertex(bufferBuilder75, matrix4f40, instance20[0], instance20[1], instance20[2], currentValue349, currentValue675, vector4f16);
         addColoredVertex(bufferBuilder75, matrix4f40, instance21[0], instance21[1], instance21[2], currentValue368, currentValue182, vector4f16);
         addColoredVertex(bufferBuilder75, matrix4f40, instance22[0], instance22[1], instance22[2], currentValue888, currentValue66, vector4f16);
      }

      poseStack24.popPose();
   }

   private static float calculateValue(int value198, float amount694) {
      float amount695 = (float)Math.sin(value198 * currentValue241 + amount694) * currentValue623;
      return amount695 - (float)Math.floor(amount695);
   }

   private static void updateState20(PoseStack poseStack25, BufferBuilder bufferBuilder76, double amount696, double amount697, double amount698, float amount699, float amount700, int value199) {
      poseStack25.pushPose();
      poseStack25.translate(amount696, amount697, amount698);
      poseStack25.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
      Matrix4f matrix4f41 = poseStack25.last().pose();
      addBillboardQuad(bufferBuilder76, matrix4f41, amount699 * currentValue928, getStateActive837(value199, amount700 * currentValue350));
      addBillboardQuad(bufferBuilder76, matrix4f41, amount699, getStateActive837(value199, amount700));
      poseStack25.popPose();
   }

   private static void updateState12(Vec3[] vec3Array14, int value200, int value201, double amount701, int value202, Random random4) {
      if (value201 - value200 >= 2 && !(amount701 < currentValue719) && value202 <= 3) {
         int value203 = (value200 + value201) / 2;
         Vec3 position210 = vec3Array14[value200];
         Vec3 position211 = vec3Array14[value201];
         Vec3 position212 = position211.subtract(position210);
         if (!(position212.lengthSqr() < currentValue97)) {
            position212 = position212.normalize();
            Vec3 position213 = position212.cross(Math.abs(position212.y) < currentValue576 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0));
            if (position213.lengthSqr() < currentValue30) {
               position213 = new Vec3(0.0, 0.0, 1.0);
            }

            position213 = position213.normalize();
            Vec3 position214 = position212.cross(position213).normalize();
            double amount702 = (random4.nextDouble() - currentValue682) * currentValue220 * amount701;
            double amount703 = (random4.nextDouble() - currentValue512) * currentValue816 * amount701 * currentValue485;
            vec3Array14[value203] = position210.lerp(position211, currentValue868).add(position213.scale(amount702)).add(position214.scale(amount703));
            updateState12(vec3Array14, value200, value203, amount701 * currentValue355, value202 + 1, random4);
            updateState12(vec3Array14, value203, value201, amount701 * currentValue847, value202 + 1, random4);
         }
      }
   }

   private static void updateState21(double amount704, double amount705, double amount706, long value204, float amount707) {
      long nowMillis13 = System.currentTimeMillis();
      if (nowMillis13 - lastUpdateMillis35 >= lastUpdateMillis39) {
         lastUpdateMillis35 = nowMillis13;
         double amount708 = currentValue128 * value204 / currentValue901;
         getStateActive816(amount704, amount705, amount706, amount708, 1.0, 1.0, currentValue954, currentValue977, amount707);
         getStateActive816(amount704, amount705, amount706, amount708, currentValue696, 1.0, currentValue497, currentValue396, amount707);
         getStateActive816(amount704, amount705, amount706, amount708, currentValue1050, currentValue747, 1.0, currentValue638, amount707);
      }
   }

   private static BufferBuilder getBufferBuilder3() {
      return GpuResourceScope.getGpuResourceScope().getBufferBuilder(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
   }

   private static void updateState13(BufferBuilder bufferBuilder77, PoseStack poseStack26, double amount709, double amount710, double amount711, float amount712, int value205) {
      poseStack26.pushPose();
      poseStack26.translate(amount709, amount710, amount711);
      poseStack26.mulPose(new Matrix4f().rotate(TargetEspPreviewContext.camera().rotation()));
      addBillboardQuad(bufferBuilder77, poseStack26.last().pose(), amount712, value205);
      poseStack26.popPose();
   }

   private static void updateState14(BufferBuilder bufferBuilder78, Matrix4f matrix4f42, Vec3 position215, Vec3 position216, Vec3 position217, float amount713, int value206) {
      Vector4f vector4f17 = ColorMath.getVector4f(value206);
      float amount714 = amount713 * currentValue621;
      Vec3 position218 = position215.subtract(position216.scale(amount714)).subtract(position217.scale(amount714));
      Vec3 position219 = position215.subtract(position216.scale(amount714)).add(position217.scale(amount714));
      Vec3 position220 = position215.add(position216.scale(amount714)).add(position217.scale(amount714));
      Vec3 position221 = position215.add(position216.scale(amount714)).subtract(position217.scale(amount714));
      addColoredVertex(bufferBuilder78, matrix4f42, (float)position218.x, (float)position218.y, (float)position218.z, 0.0F, 0.0F, vector4f17);
      addColoredVertex(bufferBuilder78, matrix4f42, (float)position219.x, (float)position219.y, (float)position219.z, 0.0F, 1.0F, vector4f17);
      addColoredVertex(bufferBuilder78, matrix4f42, (float)position220.x, (float)position220.y, (float)position220.z, 1.0F, 1.0F, vector4f17);
      addColoredVertex(bufferBuilder78, matrix4f42, (float)position221.x, (float)position221.y, (float)position221.z, 1.0F, 0.0F, vector4f17);
   }

   private static Vec3[] getVec3(Vec3 position222) {
      Vec3 position223 = new Vec3(0.0, 1.0, 0.0).subtract(position222.scale(position222.y));
      if (position223.lengthSqr() < currentValue553) {
         return getStateActive811(position222);
      } else {
         Vec3 position224 = position223.normalize();
         Vec3 position225 = position224.cross(position222).normalize();
         return new Vec3[]{position225, position224};
      }
   }

   private static void updateState15(LivingEntity entity30) {
      if (stateCounter36 != entity30.getId()) {
         stateCounter36 = entity30.getId();
         currentValue595 = entity30.getId() % 1000 * currentValue394;
      }
   }

   private static void onDeltaTrackerPoseStack17(DeltaTrackerPoseStackEvent deltaTrackerPoseStackEvent27, Vec3 position226, LivingEntity entity31, int value207, float amount715, float amount716, boolean matches29, float amount717) {
      Matrix4f matrix4f43 = deltaTrackerPoseStackEvent27.getPoseStack().last().pose();
      Vec3 position227 = TargetEspPreviewContext.camera().position();
      boolean matches30 = matches29 && entity31.hurtTime > 7;
      if (matches30 && !stateActive7) {
         currentValue519 = currentValue57;
         currentValue949 = currentValue1034;
         currentValue836 = currentValue519;
         currentValue791 = currentValue949;
         switch (random5.nextInt(4)) {
            case 0:
               currentValue791 = currentValue791 + currentValue333;
               break;
            case 1:
               currentValue791 = currentValue791 - currentValue311;
               break;
            case 2:
               currentValue836 = currentValue836 + currentValue63;
               break;
            default:
               currentValue836 = currentValue836 - currentValue1022;
         }

         currentValue246 = 0.0F;
         stateActive10 = true;
         stateActive7 = true;
      } else if (!matches30) {
         stateActive7 = false;
      }

      if (!matches29 && stateActive10) {
         float amount718 = getStateActive68(currentValue246);
         currentValue57 = Mth.lerp(amount718, currentValue519, currentValue836);
         currentValue1034 = Mth.lerp(amount718, currentValue949, currentValue791);
         stateActive10 = false;
      }

      if (stateActive10) {
         currentValue246 = Math.min(1.0F, currentValue246 + amount716 / currentValue462);
         if (currentValue246 >= 1.0F) {
            currentValue57 = currentValue836;
            currentValue1034 = currentValue791;
            stateActive10 = false;
         }
      }

      float amount719 = getStateActive68(currentValue246);
      float amount720 = 0.0F;
      amount720 = getStateActive68(amount720);
      float amount721 = matches29 ? Mth.clamp(amount715, 0.0F, 1.0F) : 1.0F;
      float amount722 = matches29 ? Math.min(currentValue1109, getStateActive814(getStateActive68(amount721))) : 1.0F;
      float amount723 = matches29 ? 0.0F : Mth.clamp(1.0F - amount715, 0.0F, 1.0F);
      double amount724 = 1.0;
      double amount725 = currentValue438 + amount722 * currentValue894;
      double amount726 = currentValue134 * amount724 * amount725;
      double amount727 = currentValue272 * amount724 * amount725;
      double amount728 = currentValue709 * amount724 * (currentValue1017 + amount722 * currentValue630);
      double amount729 = stateActive10 ? Mth.lerp(amount719, currentValue519, currentValue836) : currentValue57;
      double amount730 = stateActive10 ? Mth.lerp(amount719, currentValue949, currentValue791) : currentValue1034;
      Vec3 position228 = position226.add(0.0, entity31.getBbHeight() * currentValue801, 0.0);
      double amount731 = -entity31.getBbHeight() * currentValue1158 + amount728;
      int value208 = ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN);
      int color32 = stateActive ? ColorMath.interpolateRgba(value208, stateCounter7, amount717) : value208;
      float amount732 = matches29 ? 1.0F : 1.0F - getStateActive68(Mth.clamp((amount723 - currentValue1030) / currentValue788, 0.0F, 1.0F));
      int value209 = getStateActive837(color32, amount732);
      RenderTarget renderTarget7 = TargetEspPreviewContext.target();
      updateState10(renderTarget7.width, renderTarget7.height);
      BufferBuilder bufferBuilder79 = getBufferBuilder3();
      getTextureTarget24(bufferBuilder79, matrix4f43, position228, position227, amount729, amount730, amount726, amount727, amount728, amount731, amount723, value209);
      getStateActive856(bufferBuilder79, renderPipeline12, textureTarget3, true);
      if (stateActive3) {
         DeadChamsCompositeShader.deadChamsCompositeShader.updateState6(textureTarget3, currentValue908);
      }

      stateCounter35 = color32;
      currentValue17 = 1.0F + amount720 * currentValue499;
      stateActive8 = true;
   }

   private static float calculateValue10(TargetEspRenderer.F0nIo1tGX54h6o8A f0nIo1tGX54h6o8A5, long value210, float amount733) {
      float amount734 = (float)(value210 - f0nIo1tGX54h6o8A5.lastUpdateMillis51) / (float)f0nIo1tGX54h6o8A5.lastUpdateMillis42;
      if (!(amount734 < 0.0F) && !(amount734 > 1.0F)) {
         float amount735 = getStateActive87(Mth.clamp(amount734 / currentValue1042, 0.0F, 1.0F));
         float amount736 = 1.0F - getStateActive68(Mth.clamp((amount734 - currentValue302) / currentValue372, 0.0F, 1.0F));
         float amount737 = currentValue381
            + currentValue789
               * getStateActive859((int)((value210 - f0nIo1tGX54h6o8A5.lastUpdateMillis51) / lastUpdateMillis50) + 1, f0nIo1tGX54h6o8A5.currentValue681);
         return amount733 * amount735 * amount736 * amount737;
      } else {
         return 0.0F;
      }
   }

   private static void updateState16(TargetEspRenderer.F0nIo1tGX54h6o8A f0nIo1tGX54h6o8A6, Vec3 position229, float amount738, float amount739, int value211, List<Vec3> items23) {
      items23.clear();
      byte amount740 = 12;
      Vec3[] vec3Array15 = new Vec3[amount740];
      float amount741 = f0nIo1tGX54h6o8A6.currentValue91 + value211 * currentValue1082;
      float amount742 = Math.max(f0nIo1tGX54h6o8A6.currentValue124, 1.0F);

      for (int index70 = 0; index70 < amount740; index70++) {
         float amount743 = (float)index70 / (amount740 - 1);
         float amount744 = (getStateActive859(index70 + 3 + value211 * 11, amount741) - currentValue1007) * currentValue369;
         float amount745 = (getStateActive859(index70 + 71 + value211 * 5, amount741) - currentValue1061) * currentValue566;
         float amount746 = (getStateActive859(index70 + 19 + value211, amount741) - currentValue249) * currentValue199;
         double amount747 = f0nIo1tGX54h6o8A6.currentValue1025 + f0nIo1tGX54h6o8A6.stateCounter18 * amount742 * currentValue539 * currentValue944 * amount743 + amount744;
         double amount748 = amount739 * f0nIo1tGX54h6o8A6.currentValue4 * (1.0F + amount745);
         amount748 = Mth.clamp((float)amount748, amount739 * currentValue10, amount739 * currentValue910);
         double amount749 = amount738 * Mth.lerp(amount743, f0nIo1tGX54h6o8A6.currentValue1023, f0nIo1tGX54h6o8A6.currentValue1162) + amount738 * amount746;
         vec3Array15[index70] = position229.add(Math.cos(amount747) * amount748, amount749, Math.sin(amount747) * amount748);
      }

      updateState12(
         vec3Array15, 0, amount740 - 1, amount739 * currentValue111, 0, new Random(Float.floatToIntBits(amount741) * lastUpdateMillis48 + value211 * lastUpdateMillis15)
      );

      for (int index71 = 0; index71 < amount740; index71++) {
         Vec3 position230 = vec3Array15[index71].subtract(position229);
         double amount750 = Math.sqrt(position230.x * position230.x + position230.z * position230.z);
         if (amount750 > currentValue260) {
            double amount751 = amount739 * f0nIo1tGX54h6o8A6.currentValue4;
            double amount752 = amount751 / amount750;
            double amount753 = currentValue37;
            vec3Array15[index71] = position229.add(position230.x * (1.0 - amount753 + amount753 * amount752), position230.y, position230.z * (1.0 - amount753 + amount753 * amount752));
         }
      }

      for (int index72 = 0; index72 < 28; index72++) {
         float amount754 = index72 / currentValue329;
         float amount755 = amount754 * (amount740 - 1);
         int value212 = Math.min(amount740 - 2, (int)amount755);
         float amount756 = amount755 - value212;
         items23.add(vec3Array15[value212].lerp(vec3Array15[value212 + 1], amount756));
      }
   }

   private static Vec3 getVec32(Vec3 position231, Vec3 position232, Vec3 position233, Vec3 position234, double amount757) {
      double amount758 = amount757 * amount757;
      double amount759 = amount758 * amount757;
      return new Vec3(
         currentValue1043
            * (
               currentValue706 * position232.x
                  + (-position231.x + position233.x) * amount757
                  + (currentValue275 * position231.x - currentValue1099 * position232.x + currentValue243 * position233.x - position234.x) * amount758
                  + (-position231.x + currentValue1175 * position232.x - currentValue197 * position233.x + position234.x) * amount759
            ),
         currentValue627
            * (
               currentValue406 * position232.y
                  + (-position231.y + position233.y) * amount757
                  + (currentValue611 * position231.y - currentValue787 * position232.y + currentValue550 * position233.y - position234.y) * amount758
                  + (-position231.y + currentValue1014 * position232.y - currentValue661 * position233.y + position234.y) * amount759
            ),
         currentValue307
            * (
               currentValue896 * position232.z
                  + (-position231.z + position233.z) * amount757
                  + (currentValue725 * position231.z - currentValue279 * position232.z + currentValue593 * position233.z - position234.z) * amount758
                  + (-position231.z + currentValue1114 * position232.z - currentValue1015 * position233.z + position234.z) * amount759
            )
      );
   }

   private static void updateState10(int value213, int value214) {
      if (textureTarget3 == null) {
         textureTarget3 = new TextureTarget(string4, value213, value214, GpuFormat.RGBA8_UNORM, null);
      } else if (textureTarget3.width != value213 || textureTarget3.height != value214) {
         textureTarget3.resize(value213, value214);
      }
   }

   private static final class BNt5vHvMwoeyBNKB {
      private final Vec3 vec33;
      private final long lastUpdateMillis20;
      private final float currentValue707;
      private final float currentValue980;

      private BNt5vHvMwoeyBNKB(Vec3 position235, long value215, float amount760, float amount761) {
         this.vec33 = position235;
         this.lastUpdateMillis20 = value215;
         this.currentValue707 = amount760;
         this.currentValue980 = amount761;
      }
   }

   private record CGX9upVJA0hI2PBi(float x, float y, float z, float size, float spin) {
      public float getZ2() {
         return this.z;
      }

      public float getY() {
         return this.y;
      }

      public float getX() {
         return this.x;
      }
   }

   private static final class CefWHVrCHHOwSLC4 {
      private final double currentValue312;
      private final double currentValue883;
      private final double currentValue68;
      private final long lastUpdateMillis4;
      private final int stateCounter48;
      private float currentValue93;
      private float currentValue65;
      private float currentValue598 = 1.0F;
      private long lastUpdateMillis31;
      private long lastUpdateMillis19;
      private static final long lastUpdateMillis49 = 100L;
      private static final long lastUpdateMillis43 = 200L;
      private static final long lastUpdateMillis44 = 50L;

      private void updateState19(long value216) {
         float amount762 = Mth.clamp((float)(value216 - this.lastUpdateMillis31) / (float)this.lastUpdateMillis19, 0.0F, 1.0F);
         this.currentValue93 = Mth.lerp(amount762, this.currentValue65, this.currentValue598);
         this.currentValue65 = this.currentValue93;
         this.currentValue598 = value216 - this.lastUpdateMillis4 < lastUpdateMillis43 ? 1.0F : 0.0F;
         this.lastUpdateMillis31 = value216;
         this.lastUpdateMillis19 = lastUpdateMillis44;
      }

      private CefWHVrCHHOwSLC4(double amount763, double amount764, double amount765, long value217, int value218) {
         this.lastUpdateMillis19 = lastUpdateMillis49;
         this.currentValue312 = amount763;
         this.currentValue883 = amount764;
         this.currentValue68 = amount765;
         this.lastUpdateMillis4 = value217;
         this.stateCounter48 = value218;
         this.lastUpdateMillis31 = value217;
      }
   }

   private static final class F0nIo1tGX54h6o8A {
      private final double currentValue1025;
      private final int stateCounter18;
      private final float currentValue124;
      private final float currentValue1162;
      private final float currentValue1023;
      private final float currentValue4;
      private final float currentValue374;
      private final float currentValue487;
      private final int stateCounter34;
      private final float currentValue91;
      private final long lastUpdateMillis51;
      private final long lastUpdateMillis42;
      private final float currentValue681;

      private F0nIo1tGX54h6o8A(
         double amount766,
         int value219,
         float amount767,
         float amount768,
         float amount769,
         float amount770,
         float amount771,
         float amount772,
         int value220,
         float amount773,
         long value221,
         long value222,
         float amount774
      ) {
         this.currentValue1025 = amount766;
         this.stateCounter18 = value219;
         this.currentValue124 = amount767;
         this.currentValue1162 = amount768;
         this.currentValue1023 = amount769;
         this.currentValue4 = amount770;
         this.currentValue374 = amount771;
         this.currentValue487 = amount772;
         this.stateCounter34 = value220;
         this.currentValue91 = amount773;
         this.lastUpdateMillis51 = value221;
         this.lastUpdateMillis42 = value222;
         this.currentValue681 = amount774;
      }
   }

   private static final class FDRJZV2OjTT35QNI {
      private final List<Vec3> list7;
      private final float currentValue40;
      private final float currentValue138;

      private FDRJZV2OjTT35QNI(List<Vec3> items24, float amount775, float amount776) {
         this.list7 = items24;
         this.currentValue40 = amount775;
         this.currentValue138 = amount776;
      }
   }

   private static final class SjzMjyMYX7ylg4dN {
      private final List<Vec3> list8;
      private final long lastUpdateMillis47;
      private final long lastUpdateMillis14;
      private final long lastUpdateMillis34;
      private final float currentValue841;
      private final float currentValue20;
      private final int stateCounter2;
      private final float currentValue914;

      private SjzMjyMYX7ylg4dN(List<Vec3> items25, long value223, long value224, long value225, float amount777, float amount778, int value226, float amount779) {
         this.list8 = items25;
         this.lastUpdateMillis47 = value223;
         this.lastUpdateMillis14 = value224;
         this.lastUpdateMillis34 = value225;
         this.currentValue841 = amount777;
         this.currentValue20 = amount778;
         this.stateCounter2 = value226;
         this.currentValue914 = amount779;
      }
   }

   private static final class vbgKRi4F34zTzWmv {
      private final List<TargetEspRenderer.FDRJZV2OjTT35QNI> list6;
      private final Vec3 vec32;
      private final Vec3 vec3;
      private final long lastUpdateMillis5;
      private final long lastUpdateMillis24;
      private final float currentValue158;
      private final int stateCounter38;
      private final double currentValue388;
      private final double currentValue98;
      private final float currentValue849;
      private final float currentValue607;

      private vbgKRi4F34zTzWmv(
         List<TargetEspRenderer.FDRJZV2OjTT35QNI> items26,
         Vec3 position236,
         Vec3 position237,
         long value227,
         long value228,
         float amount780,
         int value229,
         double amount781,
         double amount782,
         float amount783,
         float amount784
      ) {
         this.list6 = items26;
         this.vec32 = position236;
         this.vec3 = position237;
         this.lastUpdateMillis5 = value227;
         this.lastUpdateMillis24 = value228;
         this.currentValue158 = amount780;
         this.stateCounter38 = value229;
         this.currentValue388 = amount781;
         this.currentValue98 = amount782;
         this.currentValue849 = amount783;
         this.currentValue607 = amount784;
      }
   }
}
