package ess.ll.client.features.crystalv2;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Simulated implements V2Access {
    public final Player player;
    public final Input input;
    public Vec3 pos;
    public Vec3 velocity;
    public AABB boundingBox;
    public float yaw;
    public float pitch;
    public boolean sprinting;
    public float fallDistance;
    public int jumpingCooldown;
    public boolean isJumping;
    public boolean isFallFlying;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean touchingWater;
    public boolean isSwimming;
    public boolean submergedInWater;
    public final Reference2ObjectArrayMap<TagKey<Fluid>, Tracker> reference2ObjectArrayMap;
    public int simulatedTicks = 0;
    public int effectClockOffset;
    public int inferredLevitationLevel;
    public int itemUseRemainingTicks;
    public float itemUseMultiplier = 1.0F;
    public boolean clipLedged = false;
    public double movementSpeedOverride = Double.NaN;
    public static final double STEP_HEIGHT = 0.5;
    public static final double COBWEB_XZ = 0.25;
    public static final double COBWEB_Y = 0.05;
    public static final double HONEY_XZ = 0.4;
    public static final double HONEY_Y = 0.4;
    public static final double POWDER_SNOW_XZ = 0.9;
    public static final double POWDER_SNOW_Y = 1.5;
    public static final double SWEET_BERRY_XZ = 0.8;
    public static final double SWEET_BERRY_Y = 0.75;

    public Simulated(Player player, Input input, Vec3 pos, Vec3 velocity, AABB boundingBox, float yaw, float pitch, boolean sprinting, float fallDistance, int jumpingCooldown, boolean isJumping, boolean isFallFlying, boolean onGround, boolean horizontalCollision, boolean verticalCollision, boolean touchingWater, boolean isSwimming, boolean submergedInWater, Reference2ObjectArrayMap<TagKey<Fluid>, Tracker> reference2ObjectArrayMap) {
        this.player = player;
        this.input = input;
        this.pos = pos;
        this.velocity = velocity;
        this.boundingBox = boundingBox;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sprinting = sprinting;
        this.fallDistance = fallDistance;
        this.jumpingCooldown = jumpingCooldown;
        this.isJumping = isJumping;
        this.isFallFlying = isFallFlying;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.verticalCollision = verticalCollision;
        this.touchingWater = touchingWater;
        this.isSwimming = isSwimming;
        this.submergedInWater = submergedInWater;
        this.reference2ObjectArrayMap = reference2ObjectArrayMap;
    }

    public static Simulated simulateLocalPlayer(int ticks) {
        Simulated simulatedPlayer = fromClientPlayer(Input.fromClientPlayer(mc.player.input.keyPresses));
        for (int i = 0; i < ticks; i++) {
            simulatedPlayer.tick();
        }
        return simulatedPlayer;
    }

    public static Simulated simulateOtherPlayer(Player player, int ticks) {
        Simulated simulatedPlayer = fromOtherPlayer(player, Input.guessInput(player));
        for (int i = 0; i < ticks; i++) {
            simulatedPlayer.tick();
        }
        return simulatedPlayer;
    }

    public static List<Vec3> simulateOtherPlayerPath(Player player, int ticks) {
        Simulated simulatedPlayer = fromOtherPlayer(player, Input.guessInput(player));
        List<Vec3> path = new ArrayList<>(ticks);
        for (int i = 0; i < ticks; i++) {
            simulatedPlayer.tick();
            path.add(simulatedPlayer.pos);
        }
        return path;
    }

    /**
     * The two fluids the movement cares about, for an entity standing in them.
     */
    private static Reference2ObjectArrayMap<TagKey<Fluid>, Tracker> trackers(Player player) {
        Reference2ObjectArrayMap<TagKey<Fluid>, Tracker> copy = new Reference2ObjectArrayMap();
        for (TagKey<Fluid> fluid : java.util.List.of(FluidTags.WATER, FluidTags.LAVA)) {
            if (player.fluidInteraction.isInFluid(fluid)) {
                copy.put(fluid, new Tracker(player.fluidInteraction.getFluidHeight(fluid), player.fluidInteraction.isEyeInFluid(fluid)));
            }
        }
        return copy;
    }

    public static Simulated fromClientPlayer(Input input) {
        LocalPlayer player = mc.player;
        Reference2ObjectArrayMap<TagKey<Fluid>, Tracker> copy = trackers(player);
        return new Simulated(player, input, player.getPosition(1.0F), player.getDeltaMovement(), player.getBoundingBox(), player.getYRot(), player.getXRot(), player.isSprinting(), (float) player.fallDistance, player.noJumpDelay, player.isJumping(), player.isFallFlying(), player.onGround(), player.horizontalCollision, player.verticalCollision, player.isInWater(), player.isSwimming(), player.isUnderWater(), copy);
    }

    public static Simulated fromOtherPlayer(Player player, Input input) {
        PositionAndRotation interpolation = player.getInterpolation() == null ? null : player.getInterpolation().target();
        Vec3 renderedPosition = player.position();
        Vec3 serverPosition = interpolation == null ? renderedPosition : interpolation.position();
        Vec3 tickVelocity = renderedPosition.subtract(player.oldPosition()).scale(0.75);
        AABB serverBoundingBox = player.dimensions.makeBoundingBox(serverPosition);
        float serverYaw = interpolation == null ? player.getYRot() : interpolation.yRot();
        float serverPitch = interpolation == null ? player.getXRot() : interpolation.xRot();
        Reference2ObjectArrayMap<TagKey<Fluid>, Tracker> copy = trackers(player);
        Simulated simulatedPlayer = new Simulated(player, input, serverPosition, tickVelocity, serverBoundingBox, serverYaw, serverPitch, player.isSprinting(), (float) player.fallDistance, player.noJumpDelay, player.isJumping(), player.isFallFlying(), player.onGround(), player.horizontalCollision, player.verticalCollision, player.isInWater(), player.isSwimming(), player.isUnderWater(), copy);
        Vec3 initialMovement = simulatedPlayer.adjustMovementForCollisions(tickVelocity);
        simulatedPlayer.pos = serverPosition.add(initialMovement);
        simulatedPlayer.boundingBox = serverBoundingBox.move(initialMovement);
        return simulatedPlayer;
    }

    public void setMovementSpeedOverride(double movementSpeedOverride) {
        this.movementSpeedOverride = movementSpeedOverride;
    }

    public void setItemUseSlowdown(float multiplier, int remainingTicks) {
        this.itemUseMultiplier = Math.clamp(multiplier, 0.0F, 1.0F);
        this.itemUseRemainingTicks = Math.max(0, remainingTicks);
    }

    public void setInferredLevitationLevel(int level) {
        this.inferredLevitationLevel = Math.clamp(level, 0, 256);
    }

    public void setEffectClockOffset(int ticks) {
        this.effectClockOffset = Math.max(0, ticks);
    }

    public void tick() {
        this.simulatedTicks++;
        this.clipLedged = false;
        if (!(this.pos.y <= -70.0)) {
            this.input.update();
            this.checkWaterState();
            this.updateSubmergedInWaterState();
            this.updateSwimming();
            if (this.jumpingCooldown > 0) {
                this.jumpingCooldown--;
            }
            this.isJumping = this.input.playerInput.jump();
            double newX = this.velocity.x;
            double newY = this.velocity.y;
            double newZ = this.velocity.z;
            if (Math.abs(this.velocity.x) < 0.003) {
                newX = 0.0;
            }
            if (Math.abs(this.velocity.y) < 0.003) {
                newY = 0.0;
            }
            if (Math.abs(this.velocity.z) < 0.003) {
                newZ = 0.0;
            }
            if (this.onGround) {
                this.isFallFlying = false;
            }
            this.velocity = new Vec3(newX, newY, newZ);
            if (this.isJumping) {
                double fluidLevel = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
                boolean inWater = this.isTouchingWater() && fluidLevel > 0.0;
                double swimHeight = this.getSwimHeight();
                if (!inWater || this.onGround && !(fluidLevel > swimHeight)) {
                    if (!this.isInLava() || this.onGround && !(fluidLevel > swimHeight)) {
                        if ((this.onGround || inWater && fluidLevel <= swimHeight) && this.jumpingCooldown == 0) {
                            this.jump();
                            if (this.player.equals(mc.player)) {
                                this.jumpingCooldown = 10;
                            }
                        }
                    } else {
                        this.swimUpward(FluidTags.LAVA);
                    }
                } else {
                    this.swimUpward(FluidTags.WATER);
                }
            }
            float itemScale = this.itemUseRemainingTicks > Math.max(0, this.simulatedTicks - this.effectClockOffset) ? this.itemUseMultiplier : 1.0F;
            float sidewaysSpeed = this.input.movementSideways * 0.98F * itemScale;
            float forwardSpeed = this.input.movementForward * 0.98F * itemScale;
            float upwardsSpeed = 0.0F;
            if (this.hasStatusEffect(MobEffects.SLOW_FALLING) || this.hasStatusEffect(MobEffects.LEVITATION)) {
                this.onLanding();
            }
            this.travel(new Vec3(sidewaysSpeed, upwardsSpeed, forwardSpeed));
            this.applyBlockMovementEffects();
        }
    }

    public Vec3 applyFluidMovingSpeed(double gravity, boolean falling, Vec3 motion) {
        if (gravity != 0.0 && !this.isSprinting()) {
            double d;
            if (falling && Math.abs(motion.y - 0.005) >= 0.003 && Math.abs(motion.y - gravity / 16.0) < 0.003) {
                d = -0.003;
            } else {
                d = motion.y - gravity / 16.0;
            }
            return new Vec3(motion.x, d, motion.z);
        } else {
            return motion;
        }
    }

    public void travel(Vec3 movementInput) {
        if (this.isSwimming && !this.player.isPassenger()) {
            double g = this.getRotationVector().y;
            double h = g < -0.2 ? 0.085 : 0.06;
            BlockPos posAbove = new BlockPos(Mth.floor(this.pos.x), Mth.floor(this.pos.y + 1.0 - 0.1), Mth.floor(this.pos.z));
            if (g <= 0.0 || this.input.playerInput.jump() || !this.player.level().getBlockState(posAbove).getFluidState().isEmpty()) {
                this.velocity = this.velocity.add(0.0, (g - this.velocity.y) * h, 0.0);
            }
        }
        double beforeTravelVelocityY = this.velocity.y;
        double d = this.player.isNoGravity() ? 0.0 : this.getAttributeValue(Attributes.GRAVITY);
        boolean falling = this.velocity.y <= 0.0;
        if (this.velocity.y <= 0.0 && this.hasStatusEffect(MobEffects.SLOW_FALLING)) {
            d = Math.min(d, 0.01);
            this.onLanding();
        }
        if (this.isTouchingWater() && !this.player.getAbilities().flying) {
            double e = this.pos.y;
            float f = this.isSprinting() ? 0.9F : 0.8F;
            float g = 0.02F;
            float h = (float) this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
            if (!this.onGround) {
                h *= 0.5F;
            }
            if (h > 0.0F) {
                f += (0.54600006F - f) * h / 3.0F;
                g += (this.getMovementSpeed() - g) * h / 3.0F;
            }
            if (this.hasStatusEffect(MobEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }
            this.updateVelocity(g, movementInput);
            this.move(this.velocity);
            Vec3 tempVel = this.velocity;
            if (this.horizontalCollision && this.isClimbing()) {
                tempVel = new Vec3(tempVel.x, 0.2, tempVel.z);
            }
            this.velocity = tempVel.multiply(f, 0.8, f);
            Vec3 vec3d2 = this.applyFluidMovingSpeed(d, falling, this.velocity);
            this.velocity = vec3d2;
            if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - this.pos.y + e, vec3d2.z)) {
                this.velocity = new Vec3(vec3d2.x, 0.3, vec3d2.z);
            }
        } else if (this.isInLava() && !this.player.getAbilities().flying) {
            double e = this.pos.y;
            this.updateVelocity(0.02F, movementInput);
            this.move(this.velocity);
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
                this.velocity = this.velocity.multiply(0.5, 0.8, 0.5);
                this.velocity = this.applyFluidMovingSpeed(d, falling, this.velocity);
            } else {
                this.velocity = this.velocity.multiply(0.5, 0.5, 0.5);
            }
            if (!this.player.isNoGravity()) {
                this.velocity = this.velocity.add(0.0, -d / 4.0, 0.0);
            }
            if (this.horizontalCollision && this.doesNotCollide(this.velocity.x, this.velocity.y + 0.6 - this.pos.y + e, this.velocity.z)) {
                this.velocity = new Vec3(this.velocity.x, 0.3, this.velocity.z);
            }
        } else if (this.isFallFlying) {
            Vec3 e = this.velocity;
            if (e.y > -0.5) {
                this.fallDistance = 1.0F;
            }
            Vec3 vec3d3 = this.getRotationVector();
            float f = this.pitch * (float) (Math.PI / 180.0);
            double g = Math.sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z);
            double horizontalSpeed = this.velocity.horizontalDistance();
            double i = vec3d3.length();
            float j = Mth.cos(f);
            j = (float) (j * (j * Math.min(1.0, i / 0.4)));
            e = this.velocity.add(0.0, d * (-1.0 + j * 0.75), 0.0);
            if (e.y < 0.0 && g > 0.0) {
                double k = e.y * -0.1 * j;
                e = e.add(vec3d3.x * k / g, k, vec3d3.z * k / g);
            }
            if (f < 0.0F && g > 0.0) {
                double k = horizontalSpeed * -Mth.sin(f) * 0.04;
                e = e.add(-vec3d3.x * k / g, k * 3.2, -vec3d3.z * k / g);
            }
            if (g > 0.0) {
                e = e.add((vec3d3.x / g * horizontalSpeed - e.x) * 0.1, 0.0, (vec3d3.z / g * horizontalSpeed - e.z) * 0.1);
            }
            this.velocity = e.multiply(0.99, 0.98, 0.99);
            this.move(this.velocity);
        } else {
            BlockPos blockPos = this.getVelocityAffectingPos();
            float p = this.player.level().getBlockState(blockPos).getBlock().getFriction();
            float f = this.onGround ? p * 0.91F : 0.91F;
            Vec3 vec3d6 = this.applyMovementInput(movementInput, p);
            double q = vec3d6.y;
            MobEffectInstance levitation = this.getStatusEffect(MobEffects.LEVITATION);
            if (levitation == null && this.inferredLevitationLevel <= 0) {
                if (this.player.level().isClientSide() && !this.player.level().hasChunkAt(blockPos)) {
                    q = this.pos.y > this.player.level().getMinY() ? -0.1 : 0.0;
                } else if (!this.player.isNoGravity()) {
                    q -= d;
                }
            } else {
                int level = levitation != null ? levitation.getAmplifier() + 1 : this.inferredLevitationLevel;
                q += (0.05 * level - vec3d6.y) * 0.2;
            }
            this.velocity = new Vec3(vec3d6.x * f, q * 0.98F, vec3d6.z * f);
        }
        if (this.player.getAbilities().flying && !this.player.isPassenger()) {
            this.velocity = new Vec3(this.velocity.x, beforeTravelVelocityY * 0.6, this.velocity.z);
            this.onLanding();
        }
    }

    public Vec3 applyMovementInput(Vec3 movementInput, float slipperiness) {
        this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
        this.velocity = this.applyClimbingSpeed(this.velocity);
        this.move(this.velocity);
        Vec3 result = this.velocity;
        BlockPos posBlock = this.posToBlockPos(this.pos);
        BlockState state = this.getState(posBlock);
        if ((this.horizontalCollision || this.isJumping) && (this.isClimbing() || state != null && state.is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this.player))) {
            result = new Vec3(result.x, 0.2, result.z);
        }
        return result;
    }

    public void updateVelocity(float speed, Vec3 movementInput) {
        Vec3 vec = net.minecraft.world.entity.Entity.getInputVector(movementInput, speed, this.yaw);
        this.velocity = this.velocity.add(vec);
    }

    public float getMovementSpeed(float slipperiness) {
        return this.onGround ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getAirStrafingSpeed();
    }

    public float getAirStrafingSpeed() {
        float speed = 0.02F;
        return this.input.playerInput.sprint() ? speed + 0.006F : speed;
    }

    public float getMovementSpeed() {
        return Double.isNaN(this.movementSpeedOverride) ? 0.1F : (float) this.movementSpeedOverride;
    }

    public void move(Vec3 movement) {
        Vec3 modifiedMovement = movement;
        modifiedMovement = this.adjustMovementForSneaking(modifiedMovement);
        Vec3 adjustedMovement = this.adjustMovementForCollisions(modifiedMovement);
        if (adjustedMovement.lengthSqr() > 1.0E-7) {
            this.pos = this.pos.add(adjustedMovement);
            this.boundingBox = this.boundingBox.move(adjustedMovement);
        }
        boolean xCollision = !Mth.equal(movement.x, adjustedMovement.x);
        boolean zCollision = !Mth.equal(movement.z, adjustedMovement.z);
        this.horizontalCollision = xCollision || zCollision;
        this.verticalCollision = movement.y != adjustedMovement.y;
        this.onGround = this.verticalCollision && movement.y < 0.0;
        if (!this.isTouchingWater()) {
            this.checkWaterState();
        }
        if (this.onGround) {
            this.onLanding();
        } else if (movement.y < 0.0) {
            this.fallDistance = this.fallDistance - (float) movement.y;
        }
        Vec3 currentVel = this.velocity;
        if (this.horizontalCollision || this.verticalCollision) {
            this.velocity = new Vec3(xCollision ? 0.0 : currentVel.x, this.verticalCollision ? 0.0 : currentVel.y, zCollision ? 0.0 : currentVel.z);
        }
    }

    public void applyBlockMovementEffects() {
        AABB box = this.boundingBox.contract(0.001, 0.001, 0.001);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);
        double multiplierX = 1.0;
        double multiplierY = 1.0;
        double multiplierZ = 1.0;
        boolean resetFallDistance = false;
        MutableBlockPos mutable = new MutableBlockPos();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    mutable.set(x, y, z);
                    BlockState state = this.player.level().getBlockState(mutable);
                    if (!state.isAir()) {
                        net.minecraft.world.level.block.Block block = state.getBlock();
                        if (block instanceof WebBlock) {
                            multiplierX = Math.min(multiplierX, 0.25);
                            multiplierY = Math.min(multiplierY, 0.05);
                            multiplierZ = Math.min(multiplierZ, 0.25);
                        } else if (block instanceof HoneyBlock) {
                            multiplierX = Math.min(multiplierX, 0.4);
                            multiplierY = Math.min(multiplierY, 0.4);
                            multiplierZ = Math.min(multiplierZ, 0.4);
                        } else if (block instanceof PowderSnowBlock) {
                            multiplierX = Math.min(multiplierX, 0.9);
                            multiplierY = Math.min(multiplierY, 1.5);
                            multiplierZ = Math.min(multiplierZ, 0.9);
                            resetFallDistance = true;
                        } else if (block instanceof SweetBerryBushBlock) {
                            multiplierX = Math.min(multiplierX, 0.8);
                            multiplierY = Math.min(multiplierY, 0.75);
                            multiplierZ = Math.min(multiplierZ, 0.8);
                        }
                    }
                }
            }
        }
        if (resetFallDistance) {
            this.onLanding();
        }
        if (multiplierX != 1.0 || multiplierY != 1.0 || multiplierZ != 1.0) {
            this.velocity = this.velocity.multiply(multiplierX, multiplierY, multiplierZ);
        }
    }

    public Vec3 adjustMovementForCollisions(Vec3 movement) {
        AABB box = this.boundingBox;
        List<VoxelShape> collisionShapes = Collections.emptyList();
        Vec3 adjusted;
        if (movement.lengthSqr() == 0.0) {
            adjusted = movement;
        } else {
            adjusted = net.minecraft.world.entity.Entity.collideBoundingBox(this.player, movement, box, this.player.level(), collisionShapes);
        }
        boolean xCollide = movement.x != adjusted.x;
        boolean yCollide = movement.y != adjusted.y;
        boolean zCollide = movement.z != adjusted.z;
        boolean stepPossible = this.onGround || yCollide && movement.y < 0.0;
        if (this.player.maxUpStep() > 0.0F && stepPossible && (xCollide || zCollide)) {
            Vec3 stepAdjust = net.minecraft.world.entity.Entity.collideBoundingBox(this.player, new Vec3(movement.x, this.player.maxUpStep(), movement.z), box, this.player.level(), collisionShapes);
            Vec3 stepOffset = net.minecraft.world.entity.Entity.collideBoundingBox(this.player, new Vec3(0.0, this.player.maxUpStep(), 0.0), box.expandTowards(movement.x, 0.0, movement.z), this.player.level(), collisionShapes);
            Vec3 combined = net.minecraft.world.entity.Entity.collideBoundingBox(this.player, new Vec3(movement.x, 0.0, movement.z), box.move(stepOffset), this.player.level(), collisionShapes).add(stepOffset);
            if (stepOffset.y < this.player.maxUpStep() && combined.horizontalDistanceSqr() > stepAdjust.horizontalDistanceSqr()) {
                stepAdjust = combined;
            }
            if (stepAdjust.horizontalDistanceSqr() > adjusted.horizontalDistanceSqr()) {
                return stepAdjust.add(net.minecraft.world.entity.Entity.collideBoundingBox(this.player, new Vec3(0.0, -stepAdjust.y + movement.y, 0.0), box.move(stepAdjust), this.player.level(), collisionShapes));
            }
        }
        return adjusted;
    }

    public void onLanding() {
        this.fallDistance = 0.0F;
    }

    public void jump() {
        this.velocity = this.velocity.add(0.0, this.getJumpVelocity() - this.velocity.y, 0.0);
        if (this.isSprinting()) {
            float rad = (float) Math.toRadians(this.yaw);
            this.velocity = this.velocity.add(-Mth.sin(rad) * 0.2, 0.0, Mth.cos(rad) * 0.2);
        }
    }

    public Vec3 applyClimbingSpeed(Vec3 motion) {
        if (!this.isClimbing()) {
            return motion;
        }
        this.onLanding();
        double clampedX = Mth.clamp(motion.x, -0.15F, 0.15F);
        double clampedZ = Mth.clamp(motion.z, -0.15F, 0.15F);
        double clampedY = Math.max(motion.y, -0.15F);
        if (clampedY < 0.0 && !this.getState(this.posToBlockPos(this.pos)).is(Blocks.SCAFFOLDING) && this.player.isSuppressingSlidingDownLadder()) {
            clampedY = 0.0;
        }
        return new Vec3(clampedX, clampedY, clampedZ);
    }

    public boolean isClimbing() {
        BlockPos posBlock = this.posToBlockPos(this.pos);
        BlockState state = this.getState(posBlock);
        return state.is(BlockTags.CLIMBABLE) ? true : state.getBlock() instanceof TrapDoorBlock && this.canEnterTrapdoor(posBlock, state);
    }

    public boolean canEnterTrapdoor(BlockPos pos, BlockState state) {
        if (!(Boolean) state.getValue(TrapDoorBlock.OPEN)) {
            return false;
        }
        BlockState below = this.player.level().getBlockState(pos.relative(Direction.DOWN));
        return below.is(Blocks.LADDER) && ((Direction) below.getValue(LadderBlock.FACING)).equals(state.getValue(TrapDoorBlock.FACING));
    }

    public Vec3 adjustMovementForSneaking(Vec3 movement) {
        if (movement.y <= 0.0 && this.method_30263()) {
            double dx = movement.x;
            double dz = movement.z;
            double step;
            for (step = 0.05; dx != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(dx, -0.5, 0.0)); dx += dx > 0.0 ? -step : step) {
                if (dx < step && dx >= -step) {
                    dx = 0.0;
                    break;
                }
            }
            while (dz != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(0.0, -0.5, dz))) {
                if (dz < step && dz >= -step) {
                    dz = 0.0;
                    break;
                }
                dz += dz > 0.0 ? -step : step;
            }
            while (dx != 0.0 && dz != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(dx, -0.5, dz))) {
                dx = dx < step && dx >= -step ? 0.0 : (dx > 0.0 ? dx - step : dx + step);
                if (dz < step && dz >= -step) {
                    dz = 0.0;
                    break;
                }
                dz += dz > 0.0 ? -step : step;
            }
            if (movement.x != dx || movement.z != dz) {
                this.clipLedged = true;
            }
            if (this.shouldClipAtLedge()) {
                movement = new Vec3(dx, movement.y, dz);
            }
        }
        return movement;
    }

    public boolean shouldClipAtLedge() {
        return this.input.playerInput.shift() || this.input.forceSafeWalk;
    }

    public boolean method_30263() {
        return this.onGround || this.fallDistance < 0.5 && !this.player.level().noCollision(this.player, this.boundingBox.move(0.0, this.fallDistance - 0.5, 0.0));
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public float getJumpVelocity() {
        return 0.42F * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
    }

    public float getJumpBoostVelocityModifier() {
        if (this.hasStatusEffect(MobEffects.JUMP_BOOST)) {
            MobEffectInstance boost = this.getStatusEffect(MobEffects.JUMP_BOOST);
            return 0.1F * (boost.getAmplifier() + 1);
        } else {
            return 0.0F;
        }
    }

    public float getJumpVelocityMultiplier() {
        float multiplier1 = 0.0F;
        net.minecraft.world.level.block.Block block = this.getState(this.posToBlockPos(this.pos)).getBlock();
        if (block != null) {
            multiplier1 = block.getJumpFactor();
        }
        float multiplier2 = 0.0F;
        net.minecraft.world.level.block.Block block2 = this.getState(this.getVelocityAffectingPos()).getBlock();
        if (block2 != null) {
            multiplier2 = block2.getJumpFactor();
        }
        return multiplier1 == 1.0F ? multiplier2 : multiplier1;
    }

    public boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
        return this.doesNotCollide(this.boundingBox.move(offsetX, offsetY, offsetZ));
    }

    public boolean doesNotCollide(AABB box) {
        return this.player.level().noCollision(this.player, box) && !this.player.level().containsAnyLiquid(box);
    }

    public void swimUpward(TagKey<Fluid> fluidTag) {
        this.velocity = this.velocity.add(0.0, 0.04F, 0.0);
    }

    public BlockPos getVelocityAffectingPos() {
        return BlockPos.containing(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z);
    }

    public double getSwimHeight() {
        return this.player.getEyeY() < 0.4 ? 0.0 : 0.4;
    }

    public boolean isTouchingWater() {
        return this.touchingWater;
    }

    public boolean isInLava() {
        return this.getFluidHeight(FluidTags.LAVA) > 0.0;
    }

    public void checkWaterState() {
        if (this.player.getVehicle() instanceof Boat boat && !boat.isUnderWater()) {
            this.touchingWater = false;
        } else {
            if (this.updateMovementInFluid(FluidTags.WATER, 0.014)) {
                this.onLanding();
                this.touchingWater = true;
            } else {
                this.touchingWater = false;
            }
        }
    }

    public void updateSwimming() {
        if (this.isSwimming) {
            this.isSwimming = this.isSprinting() && this.isTouchingWater() && !this.player.isPassenger();
        } else {
            this.isSwimming = this.isSprinting() && this.isSubmergedInWater() && !this.player.isPassenger() && this.player.level().getFluidState(this.posToBlockPos(this.pos)).is(FluidTags.WATER);
        }
    }

    public void updateSubmergedInWaterState() {
        Tracker tracker = (Tracker) this.reference2ObjectArrayMap.get(FluidTags.WATER);
        this.submergedInWater = tracker != null && tracker.eyesInside();
        ObjectIterator eyeLevel = this.reference2ObjectArrayMap.entrySet().iterator();
        while (eyeLevel.hasNext()) {
            java.util.Map.Entry<TagKey<Fluid>, Tracker> entry = (java.util.Map.Entry<TagKey<Fluid>, Tracker>) eyeLevel.next();
            Tracker tracker2 = entry.getValue();
            entry.setValue(new Tracker(tracker2.height(), false));
        }
        double eyeLevelx = this.getEyeY() - 0.11111111F;
        if (!(this.player.getVehicle() instanceof Boat boat && !boat.isUnderWater() && boat.getBoundingBox().maxY >= eyeLevelx && boat.getBoundingBox().minY <= eyeLevelx)) {
            BlockPos posEye = BlockPos.containing(this.pos.x, eyeLevelx, this.pos.z);
            FluidState fluidState = this.player.level().getFluidState(posEye);
            double height = posEye.getY() + fluidState.getHeight(this.player.level(), posEye);
            if (height > eyeLevelx) {
                if (fluidState.is(FluidTags.WATER)) {
                    this.reference2ObjectArrayMap.compute(FluidTags.WATER, (var0, tracker3) -> new Tracker(tracker3 != null ? tracker3.height() : 0.0, true));
                }
                if (fluidState.is(FluidTags.LAVA)) {
                    this.reference2ObjectArrayMap.compute(FluidTags.LAVA, (var0, tracker3) -> new Tracker(tracker3 != null ? tracker3.height() : 0.0, true));
                }
            }
        }
    }

    public double getEyeY() {
        return this.pos.y + this.player.getEyeY();
    }

    public boolean isSubmergedInWater() {
        return this.submergedInWater && this.isTouchingWater();
    }

    public double getFluidHeight(TagKey<Fluid> tag) {
        Tracker tracker = (Tracker) this.reference2ObjectArrayMap.get(tag);
        return tracker != null ? tracker.height() : 0.0;
    }

    public boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
        if (this.isRegionUnloaded()) {
            return false;
        }
        AABB box = this.boundingBox.contract(0.001, 0.001, 0.001);
        int i = Mth.floor(box.minX);
        int j = Mth.ceil(box.maxX);
        int k = Mth.floor(box.minY);
        int l = Mth.ceil(box.maxY);
        int m = Mth.floor(box.minZ);
        int n = Mth.ceil(box.maxZ);
        double d = 0.0;
        boolean foundFluid = false;
        Vec3 fluidVelocity = Vec3.ZERO;
        int count = 0;
        MutableBlockPos mutable = new MutableBlockPos();
        for (int p = i; p < j; p++) {
            for (int q = k; q < l; q++) {
                for (int r = m; r < n; r++) {
                    mutable.set(p, q, r);
                    FluidState fluidState = this.player.level().getFluidState(mutable);
                    if (fluidState.is(tag)) {
                        double e = q + fluidState.getHeight(this.player.level(), mutable);
                        if (e >= box.minY) {
                            foundFluid = true;
                            d = Math.max(e - box.minY, d);
                            Vec3 vel = fluidState.getFlow(this.player.level(), mutable);
                            if (d < 0.4) {
                                vel = vel.multiply(d, d, d);
                            }
                            fluidVelocity = fluidVelocity.add(vel);
                            count++;
                        }
                    }
                }
            }
        }
        if (fluidVelocity.length() > 0.0) {
            if (count > 0) {
                fluidVelocity = fluidVelocity.multiply(1.0 / count, 1.0 / count, 1.0 / count);
            }
            fluidVelocity = fluidVelocity.multiply(speed, speed, speed);
            if (Math.abs(this.velocity.x) < 0.003 && Math.abs(this.velocity.z) < 0.003 && fluidVelocity.length() < 0.0045) {
                fluidVelocity = fluidVelocity.normalize().multiply(0.0045, 0.0045, 0.0045);
            }
            this.velocity = this.velocity.add(fluidVelocity);
        }
        Tracker old = (Tracker) this.reference2ObjectArrayMap.get(tag);
        this.reference2ObjectArrayMap.put(tag, new Tracker(d, old != null && old.eyesInside()));
        return foundFluid;
    }

    public boolean isRegionUnloaded() {
        AABB box = this.boundingBox.inflate(1.0, 1.0, 1.0);
        int i = Mth.floor(box.minX);
        int j = Mth.ceil(box.maxX);
        int k = Mth.floor(box.minZ);
        int l = Mth.ceil(box.maxZ);
        return !this.player.level().hasChunksAt(i, k, j, l);
    }

    public Vec3 getRotationVector() {
        return this.getRotationVector(this.pitch, this.yaw);
    }

    public Vec3 getRotationVector(float pitch, float yaw) {
        float f = (float) (pitch * Math.PI / 180.0);
        float g = (float) (-yaw * Math.PI / 180.0);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    public boolean hasStatusEffect(Holder<MobEffect> effect) {
        MobEffectInstance instance = this.player.getEffect(effect);
        return instance != null && instance.getDuration() >= Math.max(0, this.simulatedTicks - this.effectClockOffset);
    }

    public MobEffectInstance getStatusEffect(Holder<MobEffect> effect) {
        MobEffectInstance instance = this.player.getEffect(effect);
        return instance != null && instance.getDuration() >= Math.max(0, this.simulatedTicks - this.effectClockOffset) ? instance : null;
    }

    public double getAttributeValue(Holder<Attribute> attribute) {
        return this.player.getAttributes().getValue(attribute);
    }

    public BlockPos posToBlockPos(Vec3 pos) {
        return new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
    }

    public BlockState getState(BlockPos pos) {
        return this.player.level().getBlockState(pos);
    }
}
