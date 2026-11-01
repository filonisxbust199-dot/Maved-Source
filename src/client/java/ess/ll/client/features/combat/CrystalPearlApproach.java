package ess.ll.client.features.combat;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.mixin.ClientLevelMixin_1241;
import ess.ll.client.module.ClientModule;
import ess.ll.client.setting.*;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * Bounded landing search and collision-checked pearl arcs, owned by CrystalAura.
 */
public final class CrystalPearlApproach implements MinecraftAccess {
    private final ClientModule owner;
    private final ToggleSetting enabled;
    private final ModeSetting mode;
    private final NumberSetting<Float> distance, minimum, health;
    private final NumberSetting<Integer> delay, drop;
    private long nextSearch, nextThrow;
    private boolean queued;
    private int generation;
    private Object world;

    private record Aim(float yaw, float pitch, Vec3 landing) {
    }

    public CrystalPearlApproach(ClientModule owner) {
        this.owner = owner;
        enabled = new ToggleSetting("Перл к цели").withDefault(false).withContainer(owner);
        mode = new ModeSetting("Позиция перла").addModes("Ниже, иначе ближе", "Только ниже", "Ближе").withDefault("Ниже, иначе ближе").withVisibility(enabled::isEnabled).withContainer(owner);
        distance = new NumberSetting<Float>("Дальность перла").withMin(4f).withMax(24f).withStep(1f).withDefault(16f).withVisibility(enabled::isEnabled).withContainer(owner);
        minimum = new NumberSetting<Float>("Перл от дистанции").withMin(2f).withMax(12f).withStep(.5f).withDefault(4f).withVisibility(enabled::isEnabled).withContainer(owner);
        delay = new NumberSetting<Integer>("Задержка перла (мс)").withMin(500).withMax(10000).withStep(100).withDefault(2000).withVisibility(enabled::isEnabled).withContainer(owner);
        drop = new NumberSetting<Integer>("Глубина под целью").withMin(1).withMax(6).withStep(1).withDefault(3).withVisibility(() -> enabled.isEnabled() && !mode.is("Ближе")).withContainer(owner);
        health = new NumberSetting<Float>("Минимум HP для перла").withMin(5f).withMax(20f).withStep(1f).withDefault(8f).withVisibility(enabled::isEnabled).withContainer(owner);
    }

    public void reset() {
        generation++;
        queued = false;
        nextSearch = nextThrow = 0;
        world = null;
    }

    public void tick(LivingEntity preferred) {
        if (world != mc.level) {
            reset();
            world = mc.level;
        }
        if (!enabled.isEnabled()) {
            if (queued)
                reset();
            return;
        }
        long now = System.currentTimeMillis();
        if (queued || now < nextThrow || now < nextSearch || mc.player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null || mc.player.isSpectator() || mc.player.isDeadOrDying() || mc.player.isPassenger() || mc.player.getHealth() + mc.player.getAbsorptionAmount() < health.getValue())
            return;
        nextSearch = now + 250;
        if (InventoryActionManager.INSTANCE.isBusy())
            return;
        var slot = InventoryUtils.findSlot(Items.ENDER_PEARL);
        if (slot == null || mc.player.getCooldowns().isOnCooldown(slot.getItem()))
            return;
        for (var entity : mc.level.entitiesForRendering()) if (entity instanceof ThrownEnderpearl pearl && pearl.getOwner() == mc.player && !pearl.isRemoved())
            return;
        LivingEntity target = valid(preferred) ? preferred : null;
        if (target == null)
            for (Player player : CombatTargetUtils.findEnemiesInRange(distance.getValue())) if (valid(player) && (target == null || mc.player.distanceToSqr(player) < mc.player.distanceToSqr(target)))
                target = player;
        if (target == null || mc.player.distanceTo(target) < minimum.getValue())
            return;
        Aim aim = find(target);
        if (aim == null)
            return;
        final LivingEntity selected = target;
        final int token = generation;
        final var level = mc.level;
        queued = true;
        nextThrow = now + delay.getValue();
        InventoryActionManager.INSTANCE.enqueueClick(() -> {
            if (token != generation)
                return;
            try {
                if (!owner.isEnabled() || !enabled.isEnabled() || mc.level != level || mc.player == null || mc.gui.screen() != null || !valid(selected) || mc.player.distanceTo(selected) < minimum.getValue() || mc.player.getHealth() + mc.player.getAbsorptionAmount() < health.getValue())
                    return;
                // Inventory slowdown may have delayed this action; solve again for the current origin/target.
                Aim fresh = find(selected);
                if (fresh != null)
                    throwPearl(fresh);
            } finally {
                queued = false;
            }
        }, 1, true);
    }

    private boolean valid(LivingEntity target) {
        return target != null && target != mc.player && target.isAlive() && !target.isRemoved() && target.level() == mc.level && mc.player.distanceTo(target) <= distance.getValue() && (!(target instanceof Player player) || CombatTargetUtils.isEnemy(player));
    }

    private Aim find(LivingEntity target) {
        List<Vec3> lower = new ArrayList<>(), near = new ArrayList<>();
        BlockPos origin = target.blockPosition();
        double current = mc.player.position().distanceTo(target.position());
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = -drop.getValue(); y <= 1; y++) {
            BlockPos feet = origin.offset(x, y, z);
            if (!mc.level.hasChunkAt(feet))
                continue;
            var floor = mc.level.getBlockState(feet.below()).getCollisionShape(mc.level, feet.below());
            if (floor.isEmpty())
                continue;
            Vec3 point = new Vec3(feet.getX() + .5, feet.getY() - 1 + floor.max(Direction.Axis.Y) + .01, feet.getZ() + .5);
            if (point.distanceTo(target.position()) > 3.75 || point.distanceTo(target.position()) > current - 1 || point.distanceTo(mc.player.position()) < 2 || !safe(point) || target.getBoundingBox().intersects(body(point)))
                continue;
            if (point.y < target.getY() - .5)
                lower.add(point);
            else
                near.add(point);
        }
        Comparator<Vec3> score = Comparator.comparingDouble(p -> p.distanceToSqr(target.position()) + .1 * p.distanceToSqr(mc.player.position()));
        lower.sort(score);
        near.sort(score);
        if (!mode.is("Ближе")) {
            Aim result = solve(lower);
            if (result != null || mode.is("Только ниже"))
                return result;
        } else {
            near.addAll(lower);
            near.sort(score);
        }
        return solve(near);
    }

    private static AABB body(Vec3 p) {
        return new AABB(p.x - .3, p.y, p.z - .3, p.x + .3, p.y + 1.8, p.z + .3);
    }

    private boolean safe(Vec3 p) {
        BlockPos feet = BlockPos.containing(p);
        for (BlockPos check : List.of(feet, feet.above(), feet.below())) {
            var state = mc.level.getBlockState(check);
            if (state.is(net.minecraft.tags.BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW))
                return false;
        }
        return mc.level.getFluidState(feet).isEmpty() && mc.level.getFluidState(feet.below()).isEmpty() && mc.level.noCollision(mc.player, body(p)) && !mc.level.getBlockState(feet.below()).getCollisionShape(mc.level, feet.below()).isEmpty();
    }

    private Aim solve(List<Vec3> candidates) {
        Vec3 eye = mc.player.getEyePosition().add(0, -.1, 0);
        for (Vec3 destination : candidates.stream().limit(8).toList()) {
            Vec3 delta = destination.subtract(eye);
            float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
            double horizontal = Math.hypot(delta.x, delta.z), speedSquared = 2.25;
            double discriminant = speedSquared * speedSquared - .03 * (.03 * horizontal * horizontal + 2 * delta.y * speedSquared);
            if (horizontal < .01 || discriminant < 0)
                continue;
            float ballistic = (float) -Math.toDegrees(Math.atan((speedSquared - Math.sqrt(discriminant)) / (.03 * horizontal)));
            // Try the analytic low arc first, then a small correction for discrete drag and inherited velocity.
            for (int offset : new int[] { 0, -1, 1, -2, 2, -3, 3, -4, 4 }) {
                float pitch = Math.clamp(ballistic + offset, -85, 85);
                Vec3 landing = trace(eye, yaw, pitch);
                if (landing != null && landing.distanceToSqr(destination) < .36 && safe(landing))
                    return new Aim(yaw, pitch, landing);
            }
        }
        return null;
    }

    private Vec3 trace(Vec3 pos, float yaw, float pitch) {
        Vec3 motion = Vec3.directionFromRotation(pitch, yaw).scale(1.5);
        Vec3 movement = mc.player.getDeltaMovement();
        motion = motion.add(movement.x, mc.player.onGround() ? 0 : movement.y, movement.z);
        for (int tick = 0; tick < 60; tick++) {
            // 26.3 applies gravity and inertia before moving, including the first tick.
            motion = motion.add(0, -.03, 0).scale((double) .99f);
            Vec3 end = pos.add(motion);
            if (!mc.level.hasChunkAt(BlockPos.containing(end)))
                return null;
            var hit = mc.level.clip(new ClipContext(pos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            Vec3 stop = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
            for (var entity : mc.level.getEntities(mc.player, new AABB(pos, stop).inflate(.3), e -> e.isPickable())) if (entity.getBoundingBox().inflate(.125).clip(pos, stop).isPresent())
                return null;
            if (hit.getType() != HitResult.Type.MISS)
                return hit.getDirection() == Direction.UP ? stop.add(0, .01, 0) : null;
            if (!mc.level.getFluidState(BlockPos.containing(end)).isEmpty())
                return null;
            pos = end;
        }
        return null;
    }

    private void throwPearl(Aim aim) {
        var slot = InventoryUtils.findSlot(Items.ENDER_PEARL);
        if (slot == null || mc.player.getCooldowns().isOnCooldown(slot.getItem()))
            return;
        int selected = mc.player.getInventory().getSelectedSlot();
        int hotbar = InventoryUtils.findItemIndex(Items.ENDER_PEARL, true);
        int inventory = InventoryUtils.findItemIndex(Items.ENDER_PEARL, false);
        boolean offhand = mc.player.getOffhandItem().is(Items.ENDER_PEARL), swapped = false;
        if (!offhand && hotbar < 0 && inventory < 0)
            return;
        try {
            if (!offhand && hotbar < 0) {
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, inventory, selected, ContainerInput.SWAP, mc.player);
                swapped = true;
            } else if (!offhand)
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbar));
            RotationManager.rotate(new Rotation(aim.yaw, aim.pitch), 360f, 100, false);
            try (var prediction = ((ClientLevelMixin_1241) mc.level).predictionHandler().startPredicting()) {
                mc.player.connection.send(new ServerboundUseItemPacket(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, prediction.currentSequence(), aim.yaw, aim.pitch));
            }
            nextThrow = System.currentTimeMillis() + delay.getValue();
        } finally {
            if (swapped)
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, inventory, selected, ContainerInput.SWAP, mc.player);
            if (!offhand)
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(selected));
        }
    }
}
