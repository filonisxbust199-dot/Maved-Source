package ess.ll.client.render;

import ess.ll.client.config.ParticleAnimationConfig;
import ess.ll.client.features.inventory.ItemFeatureContext;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ItemAnimationContext {
    public AbstractClientPlayer player;
    public ItemStack stack = ItemStack.EMPTY;
    public PoseStack poseStack;
    public SubmitNodeCollector collector;
    public InteractionHand hand;
    public boolean mainHand;
    public boolean rightHand;
    public int side;
    public float swingProgress;
    public float equipProgress;
    public float frameDelta;
    public float tickDelta;
    public float xRotation;
    public int packedLight;
    public float mainHandSwingProgress;
    public float offhandSwingProgress;
    public boolean mainHandSwingStarted;
    public boolean offhandSwingStarted;
    public boolean mainHandAttackStarted;
    public boolean offhandAttackStarted;
    public boolean mainHandItemChanged;
    public boolean offhandItemChanged;
    public boolean usingItem;
    public boolean breakingBlock;
    public List<ParticleAnimationConfig> particles;
    public ItemFeatureContext features;
}
