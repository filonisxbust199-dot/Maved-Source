package ess.ll.client.mixin;

import ess.ll.client.event.EntityVisibilityEvent;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(LevelExtractor.class)
public abstract class CombatVisibilityMixin {
    @ModifyReturnValue(method="isEntityVisible",at=@At("RETURN"))
    private boolean ess$antiBotVisibility(boolean original, @Local(argsOnly=true) Entity entity) {
        return EntityVisibilityEvent.INSTANCE.fire(entity, original);
    }
}
