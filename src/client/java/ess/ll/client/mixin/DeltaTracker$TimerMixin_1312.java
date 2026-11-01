package ess.ll.client.mixin;

import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Timer.class)
public interface DeltaTracker$TimerMixin_1312 {
    @Accessor("deltaTickResidual")
    float tickDelta();

    @Final
    @Accessor("msPerTick")
    float tickTime();
}
