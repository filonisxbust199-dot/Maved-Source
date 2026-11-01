package ess.ll.client.mixin;

import ess.ll.client.weather.SnowChanges;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WorldTweaks snow: a placed block grows snow again, a broken one crumbles the snow it carried. */
@Mixin(ClientLevel.class)
public abstract class WeatherBlockChangeMixin {
    @Inject(method="sendBlockUpdated",at=@At("RETURN"))
    private void ess$snowBlockChanged(BlockPos pos,BlockState old,BlockState now,int flags,CallbackInfo ci) {
        SnowChanges.onBlockChanged((ClientLevel)(Object)this,pos,old,now);
    }
}
