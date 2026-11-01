package ess.ll.client.features.inventory;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ItemFeatureContext {
    public boolean flag1;
    public float value1;
    public ItemStack stack;
    public AbstractClientPlayer player;
    public InteractionHand hand;
    public boolean flag2;
    public float value2;
    public float value3;
    public float value4;
    public float value5;
    public boolean flag3;
    public boolean flag4;
    public boolean flag5;
    public boolean flag6;
    public boolean flag7;
    public boolean flag8;

    public ItemFeatureContext(boolean var1, float var2, AbstractClientPlayer player, InteractionHand interactionHand, boolean var5, float var6, float var7, float var8, float var9, boolean var10, boolean var11, boolean var12, boolean var13, boolean var14, boolean var15, ItemStack stack) {
        this.flag1 = var1;
        this.value1 = var2;
        this.player = player;
        this.hand = interactionHand;
        this.flag2 = var5;
        this.value2 = var6;
        this.value3 = var7;
        this.value4 = var8;
        this.value5 = var9;
        this.flag3 = var10;
        this.flag4 = var11;
        this.flag5 = var12;
        this.flag6 = var13;
        this.flag7 = var14;
        this.flag8 = var15;
        this.stack = stack;
    }

    public void copyFrom(ItemFeatureContext itemFeatureContext) {
        if (itemFeatureContext != null) {
            this.flag1 = itemFeatureContext.flag1;
            this.value1 = itemFeatureContext.value1;
            this.player = itemFeatureContext.player;
            this.hand = itemFeatureContext.hand;
            this.flag2 = itemFeatureContext.flag2;
            this.value2 = itemFeatureContext.value2;
            this.value3 = itemFeatureContext.value3;
            this.value4 = itemFeatureContext.value4;
            this.value5 = itemFeatureContext.value5;
            this.flag3 = itemFeatureContext.flag3;
            this.flag4 = itemFeatureContext.flag4;
            this.flag5 = itemFeatureContext.flag5;
            this.flag6 = itemFeatureContext.flag6;
            this.flag7 = itemFeatureContext.flag7;
            this.flag8 = itemFeatureContext.flag8;
            this.stack = itemFeatureContext.stack;
        }
    }
}
