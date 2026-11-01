package ess.ll.client.mixin;

import ess.ll.client.bots.Bots;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bots: the player's main account and the bots get a mark in front of their name above the head. */
@Mixin(EntityRenderer.class)
public abstract class BotNameTagMixin {
    @Inject(method="getNameTag",at=@At("RETURN"),cancellable=true)
    private void ess$mark(Entity entity,CallbackInfoReturnable<Component> cir) {
        Component marked=Bots.nameTag(entity,cir.getReturnValue());
        if(marked!=cir.getReturnValue()) cir.setReturnValue(marked);
    }
}
