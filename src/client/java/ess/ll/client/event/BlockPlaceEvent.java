package ess.ll.client.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class BlockPlaceEvent extends Event {
    public static final BlockPlaceEvent INSTANCE = new BlockPlaceEvent();
    private BlockPlaceContext context;

    public BlockPlaceContext getContext() {
        return this.context;
    }

    public void setContext(BlockPlaceContext blockPlaceContext) {
        this.context = blockPlaceContext;
    }

    public void fire(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<InteractionResult> cir) {
        if (((InteractionResult) cir.getReturnValue()).consumesAction()) {
            this.context = blockPlaceContext;
            super.dispatch();
        }
    }
}
