package ess.ll.client.mixin;

import net.minecraft.client.gui.components.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the accounts page turn the vanilla skin preview towards the mouse instead of by dragging. */
@Mixin(PlayerSkinWidget.class)
public interface SkinWidgetAccessor {
    @Accessor("rotationX") void ess$setRotationX(float value);
    @Accessor("rotationY") void ess$setRotationY(float value);
}
