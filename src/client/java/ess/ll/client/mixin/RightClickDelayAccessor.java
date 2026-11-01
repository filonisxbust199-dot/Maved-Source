package ess.ll.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** NoDelay: the pause vanilla keeps between two uses of the right mouse button. */
@Mixin(Minecraft.class)
public interface RightClickDelayAccessor {
    @Accessor("rightClickDelay") void ess$setRightClickDelay(int ticks);
}
