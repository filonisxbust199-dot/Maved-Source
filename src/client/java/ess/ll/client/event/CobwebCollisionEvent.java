package ess.ll.client.event;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class CobwebCollisionEvent extends Event {
    public static final CobwebCollisionEvent INSTANCE = new CobwebCollisionEvent();
    private BlockPos pos;

    public void fire(BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof LocalPlayer) {
            this.pos = pos;
            super.dispatch();
            if (this.consumeCancelled()) {
                ci.cancel();
            }
        }
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
