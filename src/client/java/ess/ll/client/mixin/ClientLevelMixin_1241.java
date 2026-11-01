package ess.ll.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)
public interface ClientLevelMixin_1241 {
    @Accessor("clientLevelData")
    ClientLevelData clientWorldProperties();

    @Accessor("blockStatePredictionHandler")
    BlockStatePredictionHandler predictionHandler();
}
