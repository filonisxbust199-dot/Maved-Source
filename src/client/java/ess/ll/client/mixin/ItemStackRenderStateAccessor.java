package ess.ll.client.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Wardrobe weapon skins read the item's display transform from its first layer. */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("layers") ItemStackRenderState.LayerRenderState[] ess$layers();
    @Accessor("activeLayerCount") int ess$activeLayerCount();
    @Accessor("displayContext") ItemDisplayContext ess$displayContext();
}
