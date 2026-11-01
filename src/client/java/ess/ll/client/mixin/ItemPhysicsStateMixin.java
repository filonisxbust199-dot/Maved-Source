package ess.ll.client.mixin;

import ess.ll.client.module.render.ItemPhysics;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** ItemPhysics: the render state also carries whether the item rests on the ground and its entity id. */
@Mixin(ItemEntityRenderState.class)
public class ItemPhysicsStateMixin implements ItemPhysics.State {
    @Unique private boolean ess$ground;
    @Unique private int ess$id;

    @Override public boolean ess$ground() { return ess$ground; }
    @Override public int ess$id() { return ess$id; }
    @Override public void ess$set(boolean ground,int id) { ess$ground=ground; ess$id=id; }
}
