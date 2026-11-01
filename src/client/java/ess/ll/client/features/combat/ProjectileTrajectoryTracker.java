package ess.ll.client.features.combat;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.render.ProjectileHelper;
import ess.ll.client.render.ColorUtils;
import ess.ll.client.util.EnchantmentUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class ProjectileTrajectoryTracker implements MinecraftAccess {
    public static final ProjectileTrajectoryTracker INSTANCE = new ProjectileTrajectoryTracker();
    private final Map<Integer, ProjectileTrajectoryTracker.TrackedProjectile> trackedProjectiles = new HashMap<>();
    private final List<ProjectileTrajectoryTracker.TrajectoryPath> entityPaths = new ArrayList<>();
    private final List<ProjectileTrajectoryTracker.TrajectoryPath> heldPaths = new ArrayList<>();
    private long lastGameTime;
    private double lastEyeX;
    private double lastEyeY;
    private double lastEyeZ;
    private double lastVelocityX;
    private double lastVelocityY;
    private double lastVelocityZ;
    private float lastPitch;
    private float lastYaw;
    private ItemStack lastMainHandItem;
    private ItemStack lastOffhandItem;
    private long cachedWaterPos;
    private boolean cachedInWater;

    private ProjectileTrajectoryTracker.ShotParams createShot(ProjectileHelper projectileHelper, Player player, ItemStack stack, InteractionHand interactionHand, float var5) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        Vec3 eyePosition = player.getEyePosition(var5);
        Vec3 vector = eyePosition.add(0.0, -0.1F, 0.0);
        if (item instanceof EnderpearlItem && projectileHelper.isPreviewSelected("Эндер жемчуг")) {
            return new ProjectileTrajectoryTracker.ShotParams("Эндер жемчуг", stack, vector, player.getViewVector(var5).scale(1.5), 0.03, 0.99, 0.8, ColorUtils.Theme.animatedThemeGradient(20));
        }
        if (item instanceof SnowballItem && projectileHelper.isPreviewSelected("Снежок")) {
            return new ProjectileTrajectoryTracker.ShotParams("Снежок", stack, vector, player.getViewVector(var5).scale(1.5), 0.03, 0.99, 0.8, ColorUtils.Theme.animatedThemeGradient(90));
        }
        if (item instanceof WindChargeItem && projectileHelper.isPreviewSelected("Заряд ветра")) {
            return new ProjectileTrajectoryTracker.ShotParams("Заряд ветра", stack, vector, player.getViewVector(var5).scale(1.5), 0.0, 1.0, 1.0, ColorUtils.Theme.animatedThemeGradient(120));
        }
        if (item instanceof ThrowablePotionItem && projectileHelper.isPreviewSelected("Взрывное зелье")) {
            Vec3 vector2 = this.shootVector(player.getViewXRot(var5), player.getViewYRot(var5), -20.0F);
            return new ProjectileTrajectoryTracker.ShotParams("Взрывное зелье", stack, vector, vector2.scale(0.5), 0.05, 0.99, 0.8, ColorUtils.Theme.animatedThemeGradient(150));
        }
        if (projectileHelper.isPreviewSelected("Стрела") && (item instanceof BowItem || item instanceof CrossbowItem)) {
            double vector2;
            if (item instanceof BowItem) {
                if (!player.isUsingItem() || player.getUsedItemHand() != interactionHand) {
                    return null;
                }
                float sxxxx = this.bowPower(player, var5);
                if (sxxxx < 0.1F) {
                    return null;
                }
                vector2 = 3.0 * sxxxx;
            } else {
                if (!CrossbowItem.isCharged(stack)) {
                    return null;
                }
                vector2 = 3.15;
            }
            return new ProjectileTrajectoryTracker.ShotParams("Стрела", Items.ARROW.getDefaultInstance(), vector, player.getViewVector(var5).scale(vector2), 0.05, 0.99, 0.6, ColorUtils.Theme.animatedThemeGradient(210));
        } else if (item instanceof TridentItem && projectileHelper.isPreviewSelected("Трезубец")) {
            return player.isUsingItem() && player.getUsedItemHand() == interactionHand && player.getTicksUsingItem() >= 10 ? new ProjectileTrajectoryTracker.ShotParams("Трезубец", Items.TRIDENT.getDefaultInstance(), vector, player.getViewVector(var5).scale(2.5), 0.05, 0.99, 0.99, ColorUtils.Theme.animatedThemeGradient(280)) : null;
        } else if (projectileHelper.isPreviewSelected("Предмет")) {
            Vec3 vector2 = new Vec3(eyePosition.x, eyePosition.y - 0.3F, eyePosition.z);
            return new ProjectileTrajectoryTracker.ShotParams("Предмет", stack, vector2, this.shootDirection(player, var5), 0.04, 0.98, 0.9702, ColorUtils.Theme.animatedThemeGradient(330));
        } else {
            return null;
        }
    }

    private List<ProjectileTrajectoryTracker.ShotParams> createShots(ProjectileHelper projectileHelper, Player player, ItemStack stack, InteractionHand interactionHand, float var5) {
        ArrayList items = new ArrayList();
        ProjectileTrajectoryTracker.ShotParams var7 = this.createShot(projectileHelper, player, stack, interactionHand, var5);
        if (var7 == null) {
            return items;
        }
        if (stack.getItem() instanceof CrossbowItem && this.hasMultishot(stack)) {
            Vec3 vector = player.getViewVector(var5).normalize();
            Vec3 vector2 = this.viewUp(player, var5).normalize();
            Vec3 vector3 = vector.scale(3.15);
            items.add(new ProjectileTrajectoryTracker.ShotParams(var7.name, stack, var7.origin, this.rotateAroundAxis(vector3, vector2, -10.0F), var7.gravity, var7.drag, var7.waterDrag, var7.color));
            items.add(var7);
            items.add(new ProjectileTrajectoryTracker.ShotParams(var7.name, stack, var7.origin, this.rotateAroundAxis(vector3, vector2, 10.0F), var7.gravity, var7.drag, var7.waterDrag, var7.color));
        } else {
            items.add(var7);
        }
        return items;
    }

    private Vec3 shootVector(float var1, float var2, float var3) {
        float var4 = var2 * (float) (Math.PI / 180.0);
        float var5 = var1 * (float) (Math.PI / 180.0);
        float var6 = (var1 + var3) * (float) (Math.PI / 180.0);
        double var7 = -Mth.sin(var4) * Mth.cos(var5);
        double var9 = -Mth.sin(var6);
        double var11 = Mth.cos(var4) * Mth.cos(var5);
        return new Vec3(var7, var9, var11).normalize();
    }

    private Vec3 inheritedVelocity(Player player, Vec3 vector) {
        return new Vec3(vector.x, player.onGround() ? 0.0 : vector.y, vector.z);
    }

    private Vec3 viewUp(Player player, float var2) {
        float var3 = (float) Math.toRadians(-player.getViewYRot(var2));
        float var4 = (float) Math.toRadians(-player.getViewXRot(var2));
        return new Vec3(-Math.sin(var4) * Math.sin(var3), Math.cos(var4), -Math.sin(var4) * Math.cos(var3)).normalize();
    }

    private Vec3 rotateAroundAxis(Vec3 vector, Vec3 vector2, float var3) {
        double var4 = Math.toRadians(var3);
        Vec3 vector3 = vector2.normalize();
        double var7 = Math.cos(var4);
        double var9 = Math.sin(var4);
        return vector.scale(var7).add(vector3.cross(vector).scale(var9)).add(vector3.scale(vector3.dot(vector) * (1.0 - var7)));
    }

    public ProjectileTrajectoryTracker.TrajectoryPath simulate(Vec3 vector, Vec3 vector2, double var3, double var5, double var7, boolean var9, double var10, boolean var12, Entity entity) {
        return this.simulate(vector, vector2, var3, var5, var7, var9, var10, var12, entity, 300);
    }

    private ProjectileTrajectoryTracker.TrackedProjectile createTrackedProjectile(ProjectileHelper projectileHelper, Entity entity) {
        ProjectileTrajectoryTracker.TrackedProjectile var3 = new ProjectileTrajectoryTracker.TrackedProjectile();
        if (entity instanceof ThrownEnderpearl) {
            var3.name = "Эндер жемчуг";
            var3.gravity = 0.03;
            var3.drag = 0.99;
            var3.waterDrag = 0.8;
            var3.colorOffset = 20;
            var3.icon = Items.ENDER_PEARL.getDefaultInstance();
        } else if (entity instanceof Arrow) {
            var3.name = "Стрела";
            var3.gravity = 0.05;
            var3.drag = 0.99;
            var3.waterDrag = 0.6;
            var3.colorOffset = 210;
            var3.icon = Items.ARROW.getDefaultInstance();
        } else if (entity instanceof ThrownTrident) {
            var3.name = "Трезубец";
            var3.gravity = 0.05;
            var3.drag = 0.99;
            var3.waterDrag = 0.99;
            var3.colorOffset = 280;
            var3.icon = Items.TRIDENT.getDefaultInstance();
        } else {
            if (!(entity instanceof ItemEntity var4)) {
                return null;
            }
            var3.name = "Предмет";
            var3.gravity = 0.04;
            var3.drag = 0.98;
            var3.waterDrag = 0.9702;
            var3.colorOffset = 330;
            var3.icon = var4.getItem();
        }
        return !projectileHelper.isPredictSelected(var3.name) ? null : var3;
    }

    private float bowPower(Player player, float var2) {
        float var3 = (player.getTicksUsingItem() + var2) / 20.0F;
        var3 = (var3 * var3 + var3 * 2.0F) / 3.0F;
        return var3 > 1.0F ? 1.0F : var3;
    }

    private ProjectileTrajectoryTracker() {
        this.lastGameTime = Long.MIN_VALUE;
        this.cachedWaterPos = Long.MIN_VALUE;
    }

    public void update(ProjectileHelper projectileHelper) {
        if (mc.level != null && mc.player != null) {
            for (ProjectileTrajectoryTracker.TrackedProjectile var3 : this.trackedProjectiles.values()) {
                var3.seen = false;
            }
            for (Entity entity : mc.level.entitiesForRendering()) {
                ProjectileTrajectoryTracker.TrackedProjectile var4 = this.createTrackedProjectile(projectileHelper, entity);
                if (var4 != null) {
                    int id = entity.getId();
                    if (entity.getDeltaMovement().lengthSqr() <= 0.02) {
                        this.trackedProjectiles.remove(id);
                    } else {
                        ProjectileTrajectoryTracker.TrackedProjectile var6 = this.trackedProjectiles.get(id);
                        if (var6 == null || !var6.name.equals(var4.name)) {
                            var6 = var4;
                            this.trackedProjectiles.put(id, var6);
                        }
                        var6.position = entity.position();
                        var6.velocity = entity.getDeltaMovement();
                        var6.age = 0;
                        var6.seen = true;
                    }
                }
            }
            var iterator2 = this.trackedProjectiles.entrySet().iterator();
            while (iterator2.hasNext()) {
                ProjectileTrajectoryTracker.TrackedProjectile value = (ProjectileTrajectoryTracker.TrackedProjectile) ((Entry) iterator2.next()).getValue();
                if (!value.seen) {
                    if (!projectileHelper.isPredictSelected(value.name)) {
                        iterator2.remove();
                    } else {
                        value.age++;
                        if (value.age > 120) {
                            iterator2.remove();
                        }
                    }
                }
            }
            this.entityPaths.clear();
            for (ProjectileTrajectoryTracker.TrackedProjectile var12 : this.trackedProjectiles.values()) {
                ProjectileTrajectoryTracker.TrajectoryPath var13 = this.simulate(var12.position, var12.velocity, var12.gravity, var12.drag, var12.waterDrag, false, 0.0, false, mc.player);
                int var14 = var12.age;
                if (var14 > 0) {
                    if (var14 >= var13.points.size() - 1) {
                        continue;
                    }
                    var13.points.subList(0, var14).clear();
                    var13.flightTime = Math.max(0.0, var13.flightTime - var14 * 0.05);
                }
                if (var13.points.size() >= 2) {
                    var13.name = var12.name;
                    var13.color = ColorUtils.Theme.animatedThemeGradient(var12.colorOffset);
                    var13.icon = var12.icon;
                    this.entityPaths.add(var13);
                }
            }
        } else {
            this.trackedProjectiles.clear();
            this.entityPaths.clear();
        }
    }

    public boolean isInWater(Vec3 vector) {
        BlockPos blockPos = BlockPos.containing(vector.x, vector.y, vector.z);
        long var3 = blockPos.asLong();
        if (var3 == this.cachedWaterPos) {
            return this.cachedInWater;
        }
        this.cachedWaterPos = var3;
        this.cachedInWater = !mc.level.getFluidState(blockPos).isEmpty() || mc.level.getBlockState(blockPos).is(Blocks.WATER);
        return this.cachedInWater;
    }

    private void collectHeldPaths(List<ProjectileTrajectoryTracker.TrajectoryPath> items, ProjectileHelper projectileHelper, InteractionHand interactionHand, float var4, Vec3 vector) {
        ItemStack itemInHand = mc.player.getItemInHand(interactionHand);
        var var7 = this.createShots(projectileHelper, mc.player, itemInHand, interactionHand, var4);
        for (int var8 = 0; var8 < var7.size(); var8++) {
            ProjectileTrajectoryTracker.ShotParams var9 = (ProjectileTrajectoryTracker.ShotParams) var7.get(var8);
            boolean var10 = var9.icon.getItem() instanceof ThrowablePotionItem;
            Vec3 vector2 = var9.velocity.add(this.inheritedVelocity(mc.player, vector));
            ProjectileTrajectoryTracker.TrajectoryPath var12 = this.simulate(var9.origin, vector2, var9.gravity, var9.drag, var9.waterDrag, var10, var10 ? 0.125 : 0.0, true, mc.player);
            if (var12.pointCount() > 1) {
                var12.name = var9.name;
                var12.color = var9.color;
                var12.local = var10;
                var12.icon = var9.icon;
                items.add(var12);
            }
        }
    }

    private boolean hasMultishot(ItemStack stack) {
        return EnchantmentUtils.getLevel(stack, Enchantments.MULTISHOT) > 0;
    }

    public List<ProjectileTrajectoryTracker.TrajectoryPath> getEntityPaths() {
        return this.entityPaths;
    }

    public ProjectileTrajectoryTracker.TrajectoryPath simulate(Vec3 vector, Vec3 vector2, double var3, double var5, double var7, boolean var9, double var10, boolean var12, Entity entity, int var14) {
        ProjectileTrajectoryTracker.TrajectoryPath var15 = new ProjectileTrajectoryTracker.TrajectoryPath();
        Vec3 vector3 = vector;
        Vec3 vector4 = vector2;
        this.cachedWaterPos = Long.MIN_VALUE;
        var15.addPoint(vector3);
        for (int var18 = 0; var18 < var14; var18++) {
            Vec3 vector5 = vector3;
            if (var9) {
                double var20 = this.isInWater(vector5) ? var7 : var5;
                vector4 = vector4.add(0.0, -var3, 0.0).scale(var20);
            }
            Vec3 vector6 = vector5.add(vector4);
            BlockHitResult blockHitResult = mc.level.clip(new ClipContext(vector5, vector6, Block.COLLIDER, Fluid.NONE, entity));
            var15.flightTime = var15.flightTime + 0.05;
            Vec3 vector7 = blockHitResult.getType() == Type.BLOCK ? blockHitResult.getLocation() : null;
            double var23 = vector7 != null ? vector5.distanceToSqr(vector7) : Double.MAX_VALUE;
            ProjectileTrajectoryTracker.EntityHit var25 = var12 ? this.findEntityHit(entity, vector5, vector6, var10) : null;
            double var26 = var25 != null ? vector5.distanceToSqr(var25.hitPos()) : Double.MAX_VALUE;
            if (var25 != null && var26 <= var23) {
                vector3 = var25.hitPos();
                var15.addPoint(vector3);
                var15.hit = true;
                var15.impactPosition = vector3;
                var15.impactNormal = var25.normal();
                var15.impactEntityBox = var25.entityBox();
                var15.impactEntityId = var25.entityId();
                break;
            }
            if (vector7 != null) {
                vector3 = vector7;
                var15.addPoint(vector3);
                var15.hit = true;
                var15.impactPosition = vector3;
                var15.impactBlock = blockHitResult.getBlockPos();
                Direction direction = blockHitResult.getDirection();
                var15.impactNormal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
                break;
            }
            vector3 = vector6;
            var15.addPoint(vector3);
            if (!var9) {
                double var28 = this.isInWater(vector3) ? var7 : var5;
                vector4 = vector4.scale(var28).add(0.0, -var3, 0.0);
            }
            if (vector3.y < mc.level.getMinY() - 8) {
                break;
            }
        }
        return var15;
    }

    public List<ProjectileTrajectoryTracker.TrajectoryPath> getHeldPaths(ProjectileHelper projectileHelper, float var2, Vec3 vector) {
        if (mc.player != null && mc.level != null) {
            Vec3 eyePosition = mc.player.getEyePosition(var2);
            float viewXRot = mc.player.getViewXRot(var2);
            float viewYRot = mc.player.getViewYRot(var2);
            long gameTime = mc.level.getGameTime();
            ItemStack itemInHand = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack itemInHand2 = mc.player.getItemInHand(InteractionHand.OFF_HAND);
            if (gameTime == this.lastGameTime && itemInHand == this.lastMainHandItem && itemInHand2 == this.lastOffhandItem && eyePosition.x == this.lastEyeX && eyePosition.y == this.lastEyeY && eyePosition.z == this.lastEyeZ && viewXRot == this.lastPitch && viewYRot == this.lastYaw && vector.x == this.lastVelocityX && vector.y == this.lastVelocityY && vector.z == this.lastVelocityZ) {
                return this.heldPaths;
            }
            this.lastGameTime = gameTime;
            this.lastMainHandItem = itemInHand;
            this.lastOffhandItem = itemInHand2;
            this.lastEyeX = eyePosition.x;
            this.lastEyeY = eyePosition.y;
            this.lastEyeZ = eyePosition.z;
            this.lastPitch = viewXRot;
            this.lastYaw = viewYRot;
            this.lastVelocityX = vector.x;
            this.lastVelocityY = vector.y;
            this.lastVelocityZ = vector.z;
            this.heldPaths.clear();
            this.collectHeldPaths(this.heldPaths, projectileHelper, InteractionHand.MAIN_HAND, var2, vector);
            this.collectHeldPaths(this.heldPaths, projectileHelper, InteractionHand.OFF_HAND, var2, vector);
            return this.heldPaths;
        } else {
            this.heldPaths.clear();
            this.lastGameTime = Long.MIN_VALUE;
            this.lastMainHandItem = null;
            this.lastOffhandItem = null;
            return this.heldPaths;
        }
    }

    private boolean isHittableEntity(Entity entity) {
        return entity.isAlive() && !entity.isSpectator() && !(entity instanceof Projectile) && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb);
    }

    private Vec3 shootDirection(Player player, float var2) {
        float var3 = player.getViewXRot(var2) * (float) (Math.PI / 180.0);
        float var4 = player.getViewYRot(var2) * (float) (Math.PI / 180.0);
        float var5 = Mth.sin(var3);
        float var6 = Mth.cos(var3);
        float var7 = Mth.sin(var4);
        float var8 = Mth.cos(var4);
        float var9 = (float) Math.PI;
        float var10 = 0.01F;
        return new Vec3(-var7 * var6 * 0.3F + Math.cos(var9) * var10, -var5 * 0.3F + 0.1F, var8 * var6 * 0.3F + Math.sin(var9) * var10);
    }

    private Vec3 faceNormal(AABB bounds, Vec3 vector) {
        double var3 = 0.001;
        if (Math.abs(vector.x - bounds.minX) < var3) {
            return new Vec3((-1.0), 0.0, 0.0);
        } else if (Math.abs(vector.x - bounds.maxX) < var3) {
            return new Vec3(1.0, 0.0, 0.0);
        } else if (Math.abs(vector.y - bounds.minY) < var3) {
            return new Vec3(0.0, -1.0, 0.0);
        } else if (Math.abs(vector.y - bounds.maxY) < var3) {
            return new Vec3(0.0, 1.0, 0.0);
        } else if (Math.abs(vector.z - bounds.minZ) < var3) {
            return new Vec3(0.0, 0.0, -1.0);
        } else {
            return Math.abs(vector.z - bounds.maxZ) < var3 ? new Vec3(0.0, 0.0, 1.0) : null;
        }
    }

    private ProjectileTrajectoryTracker.EntityHit findEntityHit(Entity entity, Vec3 vector, Vec3 vector2, double var4) {
        AABB bounds = new AABB(vector, vector2).inflate(1.0 + var4);
        var entities = mc.level.getEntities(entity, bounds, this::isHittableEntity);
        Entity entity2 = null;
        Vec3 vector3 = null;
        AABB bounds2 = null;
        double var11 = Double.MAX_VALUE;
        for (Entity entity3 : entities) {
            AABB bounds3 = entity3.getBoundingBox().inflate(entity3.getPickRadius() + var4);
            var var16 = bounds3.clip(vector, vector2);
            if (!var16.isEmpty()) {
                double distanceSquared = vector.distanceToSqr((Vec3) var16.get());
                if (distanceSquared < var11) {
                    var11 = distanceSquared;
                    entity2 = entity3;
                    vector3 = (Vec3) var16.get();
                    bounds2 = bounds3;
                }
            }
        }
        return entity2 != null && vector3 != null ? new ProjectileTrajectoryTracker.EntityHit(vector3, bounds2, entity2.getId(), this.faceNormal(bounds2, vector3)) : null;
    }

    private static final class ShotParams {
        private final String name;
        private final ItemStack icon;
        private final Vec3 origin;
        private final Vec3 velocity;
        private final double gravity;
        private final double drag;
        private final double waterDrag;
        private final int color;

        private ShotParams(String var1, ItemStack stack, Vec3 vector, Vec3 vector2, double var5, double var7, double var9, int var11) {
            this.name = var1;
            this.icon = stack;
            this.origin = vector;
            this.velocity = vector2;
            this.gravity = var5;
            this.drag = var7;
            this.waterDrag = var9;
            this.color = var11;
        }
    }

    private record EntityHit(Vec3 hitPos, AABB entityBox, int entityId, Vec3 normal) {
    }

    private static final class TrackedProjectile {
        private String name;
        private double gravity;
        private double drag;
        private double waterDrag;
        private int colorOffset;
        private ItemStack icon;
        private Vec3 position = Vec3.ZERO;
        private Vec3 velocity = Vec3.ZERO;
        private int age;
        private boolean seen;
    }

    public static final class TrajectoryPath {
        public final List<Vec3> points = new ArrayList<>(64);
        public String name;
        public double flightTime;
        public boolean hit;
        public Vec3 impactPosition;
        public Vec3 impactNormal;
        public BlockPos impactBlock;
        public AABB impactEntityBox;
        public int impactEntityId = -1;
        public int color;
        public boolean local;
        public ItemStack icon;

        public int pointCount() {
            return this.points.size();
        }

        private void addPoint(Vec3 vector) {
            this.points.add(vector);
        }
    }
}
