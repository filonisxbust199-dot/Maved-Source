package ess.ll.client.features.inventory;

public class ItemFeatureFlags {
    public boolean spearAnimation;
    public boolean fastChargeDisabled;
    public boolean slowChargeDisabled;
    public boolean thrustEnabled;

    public ItemFeatureFlags(boolean var1, boolean var2, boolean var3, boolean var4) {
        this.spearAnimation = var1;
        this.fastChargeDisabled = var2;
        this.slowChargeDisabled = var3;
        this.thrustEnabled = var4;
    }
}
