package ess.ll.client.mixin;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import java.util.UUID;

/** Boss bars on screen, so the Dynamic Island can move down under them. */
@Mixin(BossHealthOverlay.class)
public interface BossOverlayAccessor {
    @Accessor("events") Map<UUID,LerpingBossEvent> ess$events();
}
