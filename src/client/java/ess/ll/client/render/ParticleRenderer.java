package ess.ll.client.render;

import ess.ll.client.config.ParticleAnimationConfig;
import ess.ll.client.resource.ResourceLoader;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;

public class ParticleRenderer {
    public static float frameDelta = 0.016666668F;
    private static final DepthStencilState NO_DEPTH_TEST = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
    private static final RenderPipeline ADDITIVE_PIPELINE = createPipeline("pipeline/hmi_particle_additive", new BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO));
    private static final RenderPipeline TRANSLUCENT_PIPELINE = createPipeline("pipeline/hmi_particle_translucent", BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA);
    private static final Function<Identifier, RenderType> ADDITIVE_TYPES = Util.memoize(var0 -> RenderType.create("hmi_particle_additive", createRenderSetup(ADDITIVE_PIPELINE, var0)));
    private static final Function<Identifier, RenderType> TRANSLUCENT_TYPES = Util.memoize(var0 -> RenderType.create("hmi_particle_translucent", createRenderSetup(TRANSLUCENT_PIPELINE, var0)));

    private static RenderSetup createRenderSetup(RenderPipeline renderPipeline, Identifier identifier) {
        return RenderSetup.builder(renderPipeline).withTexture("Sampler0", identifier).createRenderSetup();
    }

    public static void renderAnimations(List<ParticleAnimationConfig> items, PoseStack poseStack2, SubmitNodeCollector submitNodeCollector, String var3, InteractionHand interactionHand, int var5, AbstractClientPlayer player, float var7) {
        if (ess.ll.client.module.misc.Optimization.noParticles()) {
            return;
        }
        var iterator2 = items.iterator();
        while (iterator2.hasNext()) {
            ParticleAnimationConfig particleAnimationConfig = (ParticleAnimationConfig) iterator2.next();
            if (particleAnimationConfig.dead) {
                iterator2.remove();
            } else if (var3.equals(particleAnimationConfig.tag) && particleAnimationConfig.hand == interactionHand) {
                if (particleAnimationConfig.poseStack != null) {
                    particleAnimationConfig.render(particleAnimationConfig.poseStack, submitNodeCollector, var5, player, var7);
                } else {
                    particleAnimationConfig.render(poseStack2, submitNodeCollector, var5, player, var7);
                }
                particleAnimationConfig.tick();
            }
        }
    }

    private static RenderPipeline createPipeline(String var0, BlendFunction blendFunction) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation(Identifier.fromNamespaceAndPath("Hex".toLowerCase(Locale.ROOT), var0)).withVertexShader(ResourceLoader.coreResource("particle")).withFragmentShader(ResourceLoader.coreResource("particle")).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).withColorTargetState(new ColorTargetState(blendFunction)).withDepthStencilState(NO_DEPTH_TEST).withCull(false).build());
    }

    public static RenderType translucent(Identifier identifier) {
        return TRANSLUCENT_TYPES.apply(identifier);
    }

    public static RenderType additive(Identifier identifier) {
        return ADDITIVE_TYPES.apply(identifier);
    }
}
