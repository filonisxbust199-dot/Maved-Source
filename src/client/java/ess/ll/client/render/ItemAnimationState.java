package ess.ll.client.render;

import ess.ll.client.config.ParticleAnimationConfig;
import ess.ll.client.integration.MinecraftAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

public enum ItemAnimationState implements MinecraftAccess {
    INSTANCE;

    public float frameDelta;
    public float previousFrameTime;
    public float tickDelta;
    public Item previousMainHandItem = Items.AIR;
    public Item previousOffhandItem = Items.AIR;
    public float mainHandSwingProgress;
    public float offhandSwingProgress;
    public boolean mainHandSwingStarted;
    public boolean offhandSwingStarted;
    private boolean wasMainHandSwinging;
    private boolean wasOffhandSwinging;
    public boolean mainHandItemChanged;
    public boolean offhandItemChanged;
    public final Matrix4f mainHandPose = new Matrix4f();
    public final Matrix4f offhandPose = new Matrix4f();
    public final List<ParticleAnimationConfig> particles = new ArrayList<>();
    private final ItemAnimationContext mainHandContext = new ItemAnimationContext();
    private final ItemAnimationContext offhandContext = new ItemAnimationContext();

    ItemAnimationState() {
        this.mainHandContext.particles = this.particles;
        this.offhandContext.particles = this.particles;
    }

    public ItemAnimationContext contextForHand(boolean var1) {
        return var1 ? this.mainHandContext : this.offhandContext;
    }

    public void updateFrame() {
        float var1 = (float) (System.nanoTime() / 1.0E9);
        this.frameDelta = var1 - this.previousFrameTime;
        this.previousFrameTime = var1;
        if (mc.isPaused()) {
            this.frameDelta = 0.0F;
        } else {
            this.frameDelta = (float) Math.min(0.05, this.frameDelta);
        }
        LocalPlayer player2 = mc.player;
        boolean var3 = player2 != null && player2.isSwinging();
        InteractionHand interactionHand = player2 == null || player2.getCurrentSwing() == null ? null : player2.getCurrentSwing().hand();
        boolean var5 = var3 && interactionHand == InteractionHand.MAIN_HAND;
        boolean var6 = var3 && interactionHand == InteractionHand.OFF_HAND;
        this.mainHandSwingStarted = var5 && !this.wasMainHandSwinging;
        this.offhandSwingStarted = var6 && !this.wasOffhandSwinging;
        this.wasMainHandSwinging = var5;
        this.wasOffhandSwinging = var6;
    }

    public void updateContext(ItemAnimationContext itemAnimationContext, AbstractClientPlayer player, ItemStack stack, InteractionHand interactionHand, boolean var5, boolean var6, int var7, float var8, float var9, float var10, float var11, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int var14) {
        if (itemAnimationContext != null && player != null && stack != null) {
            itemAnimationContext.player = player;
            itemAnimationContext.stack = stack;
            itemAnimationContext.hand = interactionHand;
            itemAnimationContext.mainHand = var5;
            itemAnimationContext.rightHand = var6;
            itemAnimationContext.side = var7;
            itemAnimationContext.swingProgress = var8;
            itemAnimationContext.equipProgress = var9;
            itemAnimationContext.tickDelta = var10;
            itemAnimationContext.xRotation = var11;
            itemAnimationContext.poseStack = poseStack;
            itemAnimationContext.collector = submitNodeCollector;
            itemAnimationContext.packedLight = var14;
            itemAnimationContext.frameDelta = this.frameDelta;
            if (var5) {
                this.mainHandSwingProgress = var8;
            } else {
                this.offhandSwingProgress = var8;
            }
            itemAnimationContext.mainHandSwingProgress = this.mainHandSwingProgress;
            itemAnimationContext.offhandSwingProgress = this.offhandSwingProgress;
            Item item = stack.getItem();
            if (var5) {
                this.mainHandItemChanged = item != this.previousMainHandItem;
            } else {
                this.offhandItemChanged = item != this.previousOffhandItem;
            }
            itemAnimationContext.mainHandItemChanged = this.mainHandItemChanged;
            itemAnimationContext.offhandItemChanged = this.offhandItemChanged;
            itemAnimationContext.mainHandSwingStarted = this.mainHandSwingStarted;
            itemAnimationContext.offhandSwingStarted = this.offhandSwingStarted;
            itemAnimationContext.mainHandAttackStarted = this.mainHandSwingStarted;
            itemAnimationContext.offhandAttackStarted = this.offhandSwingStarted;
            boolean usingItem = player.isUsingItem();
            itemAnimationContext.usingItem = usingItem && player.getUsedItemHand() == interactionHand;
            MultiPlayerGameMode gameMode2 = mc.gameMode;
            itemAnimationContext.breakingBlock = gameMode2 != null && gameMode2.isDestroying();
        }
    }

    public void rememberItem(ItemAnimationContext itemAnimationContext) {
        Item item = itemAnimationContext.stack.getItem();
        if (itemAnimationContext.mainHand) {
            this.previousMainHandItem = item;
        } else {
            this.previousOffhandItem = item;
        }
    }
}
