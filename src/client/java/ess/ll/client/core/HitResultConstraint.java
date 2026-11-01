package ess.ll.client.core;

import ess.ll.client.event.HitResultEvent;
import net.minecraft.core.BlockPos;

public final class HitResultConstraint {
    private boolean active;
    private BlockPos pos;

    public void lockTo(BlockPos pos) {
        this.active = true;
        this.pos = pos;
    }

    public void apply(HitResultEvent hitResultEvent) {
        if (this.active && this.pos != null) {
            if (hitResultEvent.getType() == HitResultEvent.Type.ENTITY) {
                hitResultEvent.setCancelled(true);
            } else if (hitResultEvent.getType() == HitResultEvent.Type.BLOCK) {
                if (hitResultEvent.getPos() == null || !this.pos.equals(hitResultEvent.getPos())) {
                    hitResultEvent.setHandled(true);
                    hitResultEvent.setCancelled(true);
                }
            }
        }
    }

    public void clear() {
        this.active = false;
        this.pos = null;
    }
}
