package ess.ll.client.render;

import ess.ll.client.config.ParticleAnimationConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

public class ParticleKeyframe {
    public static void add(List<ParticleAnimationConfig> items, boolean var1, double var2, double var4, double var6, double var8, double var10, double var12, double var14, double var16, double var18, double var20, double var22, double var24, double var26, Identifier identifier, String var29, InteractionHand interactionHand, String var31, String var32, double var33, double var35) {
        add(items, var1, var2, var4, var6, var8, var10, var12, var14, var16, var18, var20, var22, var24, var26, identifier, var29, interactionHand, var31, var32, var33, var35, null);
    }

    public static void add(List<ParticleAnimationConfig> items, boolean var1, double var2, double var4, double var6, double var8, double var10, double var12, double var14, double var16, double var18, double var20, double var22, double var24, double var26, Identifier identifier, String var29, InteractionHand interactionHand, String var31, String var32, double var33, double var35, Consumer<ParticleAnimationConfig> consumer) {
        items.add(new ParticleAnimationConfig(var1, var2, var4, var6, var8, var10, var12, var14, var16, var18, var20, var22, var24, var26, identifier, var29, interactionHand, var31, var32, var33, var35, consumer));
    }

    public static void addWithPose(List<ParticleAnimationConfig> items, boolean var1, double var2, double var4, double var6, double var8, double var10, double var12, double var14, double var16, double var18, double var20, double var22, double var24, double var26, Identifier identifier, String var29, InteractionHand interactionHand, String var31, String var32, double var33, double var35, Consumer<ParticleAnimationConfig> consumer, PoseStack poseStack) {
        items.add(new ParticleAnimationConfig(var1, var2, var4, var6, var8, var10, var12, var14, var16, var18, var20, var22, var24, var26, identifier, var29, interactionHand, var31, var32, var33, var35, consumer, poseStack));
    }

    public static void addUniqueKeyframe(List<ParticleAnimationConfig> items, boolean var1, double var2, double var4, double var6, double var8, double var10, double var12, double var14, double var16, double var18, double var20, double var22, double var24, double var26, Identifier identifier, List<Identifier> items2, String var30, InteractionHand interactionHand, String var32, String var33, double var34, double var36, Consumer<ParticleAnimationConfig> consumer) {
        for (ParticleAnimationConfig particleAnimationConfig : items) {
            if ("KEYFRAME".equals(particleAnimationConfig.animation) && particleAnimationConfig.hand == interactionHand) {
                return;
            }
        }
        items.add(new ParticleAnimationConfig(var1, var2, var4, var6, var8, var10, var12, var14, var16, var18, var20, var22, var24, var26, identifier, items2, var30, interactionHand, var32, var33, var34, var36, consumer));
    }
}
