package ess.ll.client.mixin;

import ess.ll.client.module.render.CustomCrystalEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class CustomCrystalRemovalMixin {
    @Inject(method="removeEntity",at=@At("HEAD"))
    private void ess$crystalAfterimage(int id,Entity.RemovalReason reason,CallbackInfo ci) {
        Entity entity=((ClientLevel)(Object)this).getEntity(id);
        if(entity instanceof EndCrystal crystal) CustomCrystalEffects.onRemove(crystal,reason);
    }
}
