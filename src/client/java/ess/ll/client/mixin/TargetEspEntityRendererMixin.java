package ess.ll.client.mixin;

import ess.ll.client.event.EntityOutlineColorEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class TargetEspEntityRendererMixin<T extends Entity,S extends EntityRenderState> {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",at=@At("RETURN"))
    private void ess$scanOutline(T entity,S state,float partialTick,CallbackInfo ci) {
        state.outlineColor=EntityOutlineColorEvent.INSTANCE.fire(entity,state.outlineColor);
    }
}
