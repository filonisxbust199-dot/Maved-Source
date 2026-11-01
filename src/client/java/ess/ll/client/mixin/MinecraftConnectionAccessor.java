package ess.ll.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Bots: the game connection Minecraft ticks; switching accounts hands it the connection being played. */
@Mixin(Minecraft.class)
public interface MinecraftConnectionAccessor {
    @Accessor("pendingConnection") Connection ess$connection();
    @Accessor("pendingConnection") void ess$setConnection(Connection connection);
}
