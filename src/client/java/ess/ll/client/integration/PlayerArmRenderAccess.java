package ess.ll.client.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public interface PlayerArmRenderAccess {
   void hmi$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, float equip, float swing, HumanoidArm humanoidArm);

   void hmi$renderMap(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, ItemStack stack);
}
