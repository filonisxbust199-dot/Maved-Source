package ess.ll.client.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TotemPopEvent extends Event {
    public static final TotemPopEvent INSTANCE = new TotemPopEvent();
    private Entity entity;
    private ItemStack totem;
    private float x;
    private float y;
    private float z;

    public void setZ(float z) {
        this.z = z;
    }

    public float getZ() {
        return this.z;
    }

    public void fire(Entity entity) {
        this.entity = entity;
        this.totem = findTotem(entity);
        this.x = (float) entity.getX();
        this.y = (float) entity.getY();
        this.z = (float) entity.getZ();
        super.dispatch();
    }

    private static ItemStack findTotem(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack mainHandItem = livingEntity.getMainHandItem();
            if (mainHandItem.is(Items.TOTEM_OF_UNDYING)) {
                return mainHandItem.copy();
            }
            ItemStack offhandItem = livingEntity.getOffhandItem();
            return offhandItem.is(Items.TOTEM_OF_UNDYING) ? offhandItem.copy() : ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getX() {
        return this.x;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public float getY() {
        return this.y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public ItemStack getTotem() {
        return this.totem;
    }

    public void setTotem(ItemStack stack) {
        this.totem = stack;
    }
}
