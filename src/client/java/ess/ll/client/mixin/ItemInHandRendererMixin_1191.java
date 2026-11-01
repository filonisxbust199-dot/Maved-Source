package ess.ll.client.mixin;

import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface ItemInHandRendererMixin_1191 {
    @Accessor("mainHandHeight")
    float getMainHandHeight();
}
