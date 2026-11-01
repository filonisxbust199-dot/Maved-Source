package ess.ll.client.mixin;

import ess.ll.client.bots.BotCamera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bots: in the bot camera pass the bot's own body is left out, as the player's is in first person. */
@Mixin(EntityRenderDispatcher.class)
public abstract class BotEyesMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private <E extends Entity> void ess$hideEyes(E entity,Frustum frustum,double camX,double camY,double camZ,float partial,CallbackInfoReturnable<Boolean> cir) {
        if(BotCamera.isEyes(entity)) cir.setReturnValue(false);
    }
}
