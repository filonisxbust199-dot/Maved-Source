package ess.ll.client.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public class HitResultEvent extends Event {
    public static final HitResultEvent INSTANCE = new HitResultEvent();
    private Entity entity;
    private BlockPos pos;
    private BlockState state;
    private HitResultEvent.Type type;
    private boolean handled;

    public void setState(BlockState blockState) {
        this.state = blockState;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockState getState() {
        return this.state;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public void setType(HitResultEvent.Type type) {
        this.type = type;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public boolean fireBlock(Entity entity, BlockPos pos, BlockState blockState) {
        if (!this.hasListeners()) {
            this.handled = false;
            return true;
        } else {
            this.setCancelled(false);
            this.handled = false;
            this.type = HitResultEvent.Type.BLOCK;
            this.entity = entity;
            this.pos = pos;
            this.state = blockState;
            super.dispatch();
            if (this.consumeCancelled()) {
                this.handled = false;
                return false;
            } else {
                this.handled = false;
                return true;
            }
        }
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public HitResultEvent.Type getType() {
        return this.type;
    }

    public boolean isHandled() {
        return this.handled;
    }

    public boolean fireEntity(Entity entity) {
        if (!this.hasListeners()) {
            return false;
        }
        this.setCancelled(false);
        this.handled = false;
        this.type = HitResultEvent.Type.ENTITY;
        this.entity = entity;
        super.dispatch();
        return this.consumeCancelled();
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    public enum Type {
        ENTITY, BLOCK
    }
}
