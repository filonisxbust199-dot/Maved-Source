package ess.ll.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ess.ll.client.integration.PlayerAnimationAccess;
import ess.ll.client.features.inventory.ItemFeatureVariables;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_1281 implements PlayerAnimationAccess {
    @Unique
    private boolean interactOffhand;

    @Unique
    private boolean interactMainHand;

    @Unique
    private boolean blockBreaking;

    @Unique
    private int offHandSwingTicks;

    @Unique
    private boolean offHandSwinging;

    @Unique
    private float offHandSwingProgress;

    @Unique
    private float lastOffHandSwingProgress;

    @Unique
    private int mainHandSwingTicks;

    @Unique
    private boolean mainHandSwinging;

    @Unique
    private float mainHandSwingProgress;

    @Unique
    private float lastMainHandSwingProgress;

    @Unique
    private int mainSwingCount;

    @Unique
    private boolean swingMHand;

    @Unique
    private boolean swingOHand;

    @Shadow
    public abstract ItemStack getMainHandItem();

    @Shadow
    public abstract ItemStack getOffhandItem();

    @Override
    public int hMI5_0$getSwingCount() {
        return this.mainSwingCount;
    }

    @Override
    public void hMI5_0$resetOffHandSwing(boolean interact) {
        this.offHandSwingTicks = 0;
        this.offHandSwinging = true;
        this.swingOHand = !this.swingOHand;
        this.interactOffhand = interact;
    }

    @Override
    public void hMI5_0$resetMainHandSwing(boolean interact) {
        this.mainHandSwingTicks = 0;
        this.mainHandSwinging = true;
        this.swingMHand = !this.swingMHand;
        this.mainSwingCount++;
        this.interactMainHand = interact;
        if (Minecraft.getInstance().gameMode.isDestroying()) {
            this.blockBreaking = true;
        }
    }

    @Override
    public float hMI5_0$getMainHandSwingProgress(float partialTick) {
        float delta = this.mainHandSwingProgress - this.lastMainHandSwingProgress;
        if (delta < 0.0F) {
            delta++;
        }
        return this.lastMainHandSwingProgress + delta * partialTick;
    }

    @Override
    public float hMI5_0$getOffHandSwingProgress(float partialTick) {
        float delta = this.offHandSwingProgress - this.lastOffHandSwingProgress;
        if (delta < 0.0F) {
            delta++;
        }
        return this.lastOffHandSwingProgress + delta * partialTick;
    }

    @Override
    public boolean hMI5_0$getMInteract() {
        return this.interactMainHand;
    }

    @Override
    public boolean hMI5_0$getOInteract() {
        return this.interactOffhand;
    }

    @Override
    public boolean hMI5_0$getBlockBreak() {
        return this.blockBreaking;
    }

    @Override
    public boolean hMI5_0$getMHandEvent() {
        return this.swingMHand;
    }

    @Override
    public boolean hMI5_0$getOHandEvent() {
        return this.swingOHand;
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void hmiTick(CallbackInfo ci) {
        this.lastOffHandSwingProgress = this.offHandSwingProgress;
        this.lastMainHandSwingProgress = this.mainHandSwingProgress;
        int mainSwingDuration = ItemFeatureVariables.INSTANCE.weaponChargeTicks.getOrDefault(this.getMainHandItem().getItem().toString(), 10);
        if (this.mainHandSwinging) {
            this.mainHandSwingTicks++;
            if (this.mainHandSwingTicks >= mainSwingDuration) {
                this.mainHandSwingTicks = 0;
                this.mainHandSwinging = false;
            }
        } else {
            if (this.interactMainHand) {
                this.interactMainHand = false;
            }
            if (this.blockBreaking) {
                this.blockBreaking = false;
            }
            this.mainHandSwingTicks = 0;
        }
        this.mainHandSwingProgress = (float) this.mainHandSwingTicks / mainSwingDuration;
        int offSwingDuration = ItemFeatureVariables.INSTANCE.weaponChargeTicks.getOrDefault(this.getOffhandItem().getItem().toString(), 10);
        if (this.offHandSwinging) {
            this.offHandSwingTicks++;
            if (this.offHandSwingTicks >= offSwingDuration) {
                this.offHandSwingTicks = 0;
                this.offHandSwinging = false;
            }
        } else {
            if (this.interactOffhand) {
                this.interactOffhand = false;
            }
            this.offHandSwingTicks = 0;
        }
        this.offHandSwingProgress = (float) this.offHandSwingTicks / offSwingDuration;
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"))
    private void hmiOnSwing(InteractionHand interactionHand, net.minecraft.world.item.component.SwingAnimation animation, boolean updateSelf, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (interactionHand == InteractionHand.OFF_HAND) {
            int swingDuration = ItemFeatureVariables.INSTANCE.weaponChargeTicks.getOrDefault(this.getOffhandItem().getItem().toString(), 10);
            if (!this.offHandSwinging || this.offHandSwingTicks >= swingDuration / 2 || this.offHandSwingTicks < 0) {
                this.offHandSwingTicks = -1;
                this.offHandSwinging = true;
            }
        } else {
            int mainSwingDuration = ItemFeatureVariables.INSTANCE.weaponChargeTicks.getOrDefault(this.getMainHandItem().getItem().toString(), 10);
            if (!this.mainHandSwinging || this.mainHandSwingTicks >= mainSwingDuration / 2 || this.mainHandSwingTicks < 0) {
                this.mainHandSwingTicks = -1;
                this.mainHandSwinging = true;
                if (Minecraft.getInstance().gameMode.isDestroying()) {
                    this.blockBreaking = true;
                }
            }
        }
    }
}
