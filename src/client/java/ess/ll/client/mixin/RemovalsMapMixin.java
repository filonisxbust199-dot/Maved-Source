package ess.ll.client.mixin;

import ess.ll.client.event.VisualRemovalEvent;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public abstract class RemovalsMapMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void ess$lagMap(MapId id,MapItemSavedData data,MapRenderState state,CallbackInfo ci) {
        VisualRemovalEvent.INSTANCE.filterList(VisualRemovalEvent.Type.LAG_MACHINE,state.decorations);
    }
}
