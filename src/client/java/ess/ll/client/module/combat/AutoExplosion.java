package ess.ll.client.module.combat;

import ess.ll.client.core.HitResultConstraint;
import ess.ll.client.event.BlockPlaceEvent;
import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.WorldLoadEvent;
import ess.ll.client.features.combat.ExplosionDamageUtils;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.features.world.BlockPlacementManager;
import ess.ll.client.features.world.BlockPlacementTracker;
import ess.ll.client.features.world.WorldCollisionUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.RaycastUtils;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationChangeTracker;
import ess.ll.client.util.RotationManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class AutoExplosion extends ClientModule {
    private final ToggleSetting protectDroppedItems;
    private final ToggleSetting matrixBypass;
    private BlockPos clickedPos;
    private boolean placedThisClick;
    private int previousSlot;
    private int placeTick;
    private int explodeTick;
    private final HitResultConstraint hitConstraint;

    public boolean isMatrixBypass() {
        return this.matrixBypass.isEnabled();
    }

    @Subscribe
    private void onBlockPlace(BlockPlaceEvent event) {
        if (this.clickedPos == null) {
            if (mc.player != null && mc.level != null) {
                BlockPlaceContext context = event.getContext();
                if (context.getPlayer() instanceof LocalPlayer) {
                    BlockPos clickedPos = context.getClickedPos();
                    if (BlockPlacementTracker.isReserved(clickedPos)) {
                        return;
                    }
                    if (mc.level.getBlockState(clickedPos).getBlock() == Blocks.OBSIDIAN) {
                        this.clickedPos = clickedPos;
                        this.placedThisClick = false;
                        this.explodeTick = mc.player.tickCount;
                    }
                }
            }
        }
    }

    private boolean hasItemsNear(Vec3 vector) {
        return WorldCollisionUtils.hasEntityNear(ItemEntity.class, vector, 3.0);
    }

    private BlockHitResult targetedBlock(boolean var1) {
        if (!var1) {
            BlockHitResult blockHitResult = RaycastUtils.lookedAtBlock(Blocks.OBSIDIAN);
            return blockHitResult != null && blockHitResult.getBlockPos().equals(this.clickedPos) ? blockHitResult : null;
        }
        this.hitConstraint.lockTo(this.clickedPos);
        try {
            double var2 = mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(this.clickedPos)) + 1.0;
            return mc.player.pick(var2, 1.0F, false) instanceof BlockHitResult var5 && var5.getBlockPos().equals(this.clickedPos) ? var5 : null;
        } finally {
            this.hitConstraint.clear();
        }
    }

    @Override
    public void toggle() {
        this.restoreSlot();
        super.toggle();
    }

    @Override
    public void onDisable() {
        // ESS GUI/config toggles call setEnabled directly, not the source toggle method.
        this.restoreSlot();
        super.onDisable();
    }

    @Subscribe
    private void onHitResult(HitResultEvent event) {
        this.hitConstraint.apply(event);
    }

    @Subscribe
    private void onWorldLoad(WorldLoadEvent event) {
        this.restoreSlot();
    }

    private void restoreSlot() {
        if (this.previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(this.previousSlot);
        }
        this.clickedPos = null;
        this.placedThisClick = false;
        this.previousSlot = -1;
        this.placeTick = -1;
        this.explodeTick = -1;
    }

    public AutoExplosion() {
        super(ModuleCategory.Combat, ModuleSubCategory.Utility);
        this.protectDroppedItems = new ToggleSetting("Не взрывать ресурсы").withDefault(false).withContainer(this);
        this.matrixBypass = new ToggleSetting("Обход Matrix").withDefault(false).withContainer(this);
        this.clickedPos = null;
        this.placedThisClick = false;
        this.previousSlot = -1;
        this.placeTick = -1;
        this.explodeTick = -1;
        this.hitConstraint = new HitResultConstraint();
        this.setName("Auto Explosion");
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.clickedPos != null && mc.player != null && mc.level != null && mc.gameMode != null) {
            boolean enabled = this.matrixBypass.isEnabled();
            int var3 = enabled ? 14 : 6;
            if (mc.player.tickCount - this.explodeTick > var3) {
                this.restoreSlot();
            } else {
                BlockPos blockPos = this.clickedPos.above();
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof EndCrystal && entity.blockPosition().equals(blockPos)) {
                        if (this.protectDroppedItems.isEnabled() && this.hasItemsNear(entity.position())) {
                            return;
                        }
                        if (enabled) {
                            RotationManager.markAttack();
                            Vec3 vector = ExplosionDamageUtils.crystalAimPoint(entity, mc.player.getEyePosition(), 3.0F, true);
                            if (vector == null) {
                                return;
                            }
                            Vector2f vector2 = BlockPlacementManager.entityRotation(entity, vector, 3.0);
                            if (vector2 == null) {
                                return;
                            }
                            RotationManager.rotate(new Rotation(vector2.x, vector2.y), 999.0F, 1000, false);
                            if (BlockPlacementManager.isLookingAtEntity(entity, 3.0)) {
                                mc.gameMode.attack(mc.player, entity);
                                mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                                this.restoreSlot();
                            }
                            return;
                        }
                        Vec3 vector3 = Vec3.atCenterOf(blockPos).add(0.0, 0.5, 0.0);
                        Rotation rotation = Rotation.fromDirection(vector3.subtract(mc.player.getEyePosition()));
                        RotationManager.rotate(rotation, 999.0F, 1000, false);
                        if (RaycastUtils.isLookingAt(mc.player.getYRot(), mc.player.getXRot(), 3.0, entity)) {
                            mc.gameMode.attack(mc.player, entity);
                            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                            this.restoreSlot();
                        }
                        return;
                    }
                }
                if (this.placedThisClick) {
                    if (enabled) {
                        RotationManager.markAttack();
                    }
                } else {
                    int var9 = InventoryUtils.findItemIndex(Items.END_CRYSTAL, true);
                    if (var9 == -1 || mc.level.getBlockState(this.clickedPos).getBlock() != Blocks.OBSIDIAN) {
                        this.restoreSlot();
                    } else if (!this.protectDroppedItems.isEnabled() || !this.hasItemsNear(Vec3.atCenterOf(blockPos))) {
                        Rotation rotation2;
                        if (enabled) {
                            RotationManager.markAttack();
                            Vector2f vector4 = BlockPlacementManager.blockRotation(this.clickedPos, 4.5, true);
                            if (vector4 == null) {
                                return;
                            }
                            rotation2 = new Rotation(vector4.x, vector4.y);
                        } else {
                            rotation2 = Rotation.fromDirection(Vec3.atCenterOf(this.clickedPos).subtract(mc.player.getEyePosition()));
                        }
                        RotationManager.rotate(rotation2, 999.0F, 1000, false);
                        if (this.previousSlot == -1) {
                            this.previousSlot = mc.player.getInventory().getSelectedSlot();
                            mc.player.getInventory().setSelectedSlot(var9);
                            this.placeTick = mc.player.tickCount;
                        } else if (mc.player.tickCount > this.placeTick) {
                            BlockHitResult blockHitResult = this.targetedBlock(enabled);
                            if (blockHitResult != null) {
                                if (mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult).consumesAction()) {
                                    RotationChangeTracker.record();
                                    mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                                    this.placedThisClick = true;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.restoreSlot();
        }
    }
}
