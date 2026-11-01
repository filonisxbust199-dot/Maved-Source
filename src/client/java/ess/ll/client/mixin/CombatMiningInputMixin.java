package ess.ll.client.mixin;

import ess.ll.client.features.world.BlockBreakManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The original CrystalAura mining manager depends on these Minecraft input hooks.
 */
@Mixin(Minecraft.class)
public abstract class CombatMiningInputMixin {
    @Shadow
    private int missTime;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void ess$clearMiningMissDelay(CallbackInfo ci) {
        if (BlockBreakManager.INSTANCE.canMineTarget())
            this.missTime = 0;
    }

    @Inject(method = "pick(F)V", at = @At("HEAD"))
    private void ess$beginMiningPick(float delta, CallbackInfo ci) {
        BlockBreakManager.INSTANCE.lockHitResult();
    }

    @Inject(method = "pick(F)V", at = @At("RETURN"))
    private void ess$endMiningPick(float delta, CallbackInfo ci) {
        BlockBreakManager.INSTANCE.clearHitResultLock();
        BlockHitResult hit = BlockBreakManager.INSTANCE.miningHit();
        if (hit != null)
            ((Minecraft) (Object) this).hitResult = hit;
    }

    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"), index = 0)
    private boolean ess$continueAutoMining(boolean attackHeld) {
        return attackHeld || BlockBreakManager.INSTANCE.canMineTarget();
    }
}
