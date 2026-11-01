package ess.ll.client.core;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.MovementUtils;
import net.minecraft.world.phys.shapes.VoxelShape;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ess.ll.client.mixin.EntityMixin_1159;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public class SimulatedPlayerInput implements MinecraftAccess {
   public final Player player;
   public final SimulatedPlayerInput.SimulatedInput input;
   public float speedOverride = Float.NaN;
   public Vec3 position;
   public Vec3 velocity;
   public AABB boundingBox;
   public EntityDimensions dimensions;
   public float yaw;
   public float pitch;
   public boolean sprinting;
   public float fallDistance;
   public int jumpDelay;
   public boolean jumping;
   public boolean fallFlying;
   public boolean onGround;
   public boolean horizontalCollision;
   public boolean verticalCollision;
   public boolean inWater;
   public boolean swimming;
   public boolean underWater;
   private final Object2DoubleMap<TagKey<Fluid>> fluidHeights;
   private final HashSet<TagKey<Fluid>> eyeFluids;
   private int ticks = 0;
   private boolean backedOffEdge = false;
   private static final double VOID_Y = -70.0;
   private static final double MIN_VEL_X = 0.003;
   private static final double MIN_VEL_Y = 0.003;
   private static final double MIN_VEL_Z = 0.003;
   private static final float STRAFE_SCALE = 0.98F;
   private static final float FORWARD_SCALE = 0.98F;
   private static final double SWIM_LOOK_DOWN = -0.2;
   private static final double SWIM_DOWN_RATE = 0.085;
   private static final double SWIM_UP_RATE = 0.06;
   private static final double SWIM_HEAD_OFFSET = 0.1;
   private static final double GRAVITY = 0.08;
   private static final double SLOW_FALL_GRAVITY = 0.01;
   private static final float WATER_SPRINT_DRAG = 0.9F;
   private static final float WATER_DRAG = 0.8F;
   private static final float WATER_SPEED = 0.02F;
   private static final float AIR_DEPTH_STRIDER_FACTOR = 0.5F;
   private static final float DEPTH_STRIDER_DRAG = 0.54600006F;
   private static final float DEPTH_STRIDER_DRAG_DIVISOR = 3.0F;
   private static final float DEPTH_STRIDER_SPEED_DIVISOR = 3.0F;
   private static final float DOLPHIN_DRAG = 0.96F;
   private static final double WATER_CLIMB_SPEED = 0.2;
   private static final double WATER_VERTICAL_DRAG = 0.8;
   private static final double WATER_EXIT_STEP = 0.6;
   private static final double WATER_EXIT_JUMP = 0.3;
   private static final float LAVA_SPEED = 0.02F;
   private static final double LAVA_DRAG_X = 0.5;
   private static final double LAVA_DRAG_Y = 0.8;
   private static final double LAVA_DRAG_Z = 0.5;
   private static final double LAVA_DEEP_DRAG = 0.5;
   private static final double LAVA_GRAVITY_DIVISOR = 4.0;
   private static final double LAVA_EXIT_STEP = 0.6;
   private static final double LAVA_EXIT_JUMP = 0.3;
   private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
   private static final double GLIDE_LOOK_DIVISOR = 0.4;
   private static final double GLIDE_GRAVITY_BASE = -1.0;
   private static final double GLIDE_GRAVITY_FACTOR = 0.75;
   private static final double GLIDE_LIFT = -0.1;
   private static final double GLIDE_CLIMB = 0.04;
   private static final double GLIDE_CLIMB_Y = 3.2;
   private static final double GLIDE_TURN_X = 0.1;
   private static final double GLIDE_TURN_Z = 0.1;
   private static final double GLIDE_DRAG_X = 0.99;
   private static final double GLIDE_DRAG_Y = 0.98;
   private static final double GLIDE_DRAG_Z = 0.99;
   private static final float GROUND_FRICTION_FACTOR = 0.91F;
   private static final float AIR_FRICTION = 0.91F;
   private static final double LEVITATION_SPEED = 0.05;
   private static final double LEVITATION_ACCEL = 0.2;
   private static final double UNLOADED_FALL = -0.1;
   private static final double VERTICAL_DRAG = 0.98F;
   private static final double FLYING_VERTICAL_DRAG = 0.6;
   private static final double CLIMB_SPEED = 0.2;
   private static final double MIN_INPUT_SQ = 1.0E-7;
   private static final float DEG_TO_RAD_SIN = (float) (Math.PI / 180.0);
   private static final float DEG_TO_RAD_COS = (float) (Math.PI / 180.0);
   private static final float GROUND_SPEED_FACTOR = 0.21600002F;
   private static final float AIR_SPEED = 0.02F;
   private static final float AIR_SPRINT_BONUS = 0.006F;
   private static final float DEFAULT_SPEED = 0.1F;
   private static final double MIN_MOVE_SQ = 1.0E-7;
   private static final double SPRINT_JUMP_BOOST_X = 0.2;
   private static final double SPRINT_JUMP_BOOST_Z = 0.2;
   private static final double CLIMB_MIN_X = -0.15F;
   private static final double CLIMB_MAX_X = 0.15F;
   private static final double CLIMB_MIN_Z = -0.15F;
   private static final double CLIMB_MAX_Z = 0.15F;
   private static final double CLIMB_MIN_Y = -0.15F;
   private static final double EDGE_STEP = 0.05;
   private static final double EDGE_PROBE_DOWN_X = -0.5;
   private static final double EDGE_PROBE_DOWN_Z = -0.5;
   private static final double EDGE_PROBE_DOWN_XZ = -0.5;
   private static final double EDGE_MAX_FALL = 0.5;
   private static final double EDGE_FALL_OFFSET = 0.5;
   private static final float JUMP_POWER = 0.42F;
   private static final float JUMP_BOOST_PER_LEVEL = 0.1F;
   private static final double FLUID_JUMP_SPEED = 0.04F;
   private static final double BELOW_OFFSET = 0.5000001;
   private static final double MIN_EYE_HEIGHT_FOR_FLUID_JUMP = 0.4;
   private static final double FLUID_JUMP_THRESHOLD = 0.4;
   private static final double WATER_FLOW_SCALE = 0.014;
   private static final float MIN_SPRINT_FORWARD = 0.8F;
   private static final double EYE_FLUID_OFFSET = 0.11111111F;
   private static final double FLUID_BOX_DEFLATE = 0.001;
   private static final double FLOW_DEPTH_THRESHOLD = 0.4;
   private static final double FLOW_MIN_VEL_X = 0.003;
   private static final double FLOW_MIN_VEL_Z = 0.003;
   private static final double MIN_FLOW = 0.0045;
   private static final double MIN_FLOW_SPEED = 0.0045;
   private static final double PI_YAW = Math.PI;
   private static final double HALF_TURN_YAW = 180.0;
   private static final double PI_PITCH = Math.PI;
   private static final double HALF_TURN_PITCH = 180.0;

   private void move(Vec3 movement) {
      Vec3 adjusted = movement;
      adjusted = this.backOffFromEdge(adjusted);
      Vec3 collided = this.collide(adjusted);
      if (collided.lengthSqr() > MIN_MOVE_SQ) {
         this.position = this.position.add(collided);
         this.boundingBox = this.dimensions.makeBoundingBox(this.position);
      }

      boolean collidedX = !Mth.equal(movement.x, collided.x);
      boolean collidedZ = !Mth.equal(movement.z, collided.z);
      this.horizontalCollision = collidedX || collidedZ;
      this.verticalCollision = movement.y != collided.y;
      this.onGround = this.verticalCollision && movement.y < 0.0;
      if (!this.isInWater()) {
         this.updateInWater();
      }

      if (this.onGround) {
         this.resetFallDistance();
      } else if (movement.y < 0.0) {
         this.fallDistance = this.fallDistance - (float)movement.y;
      }

      Vec3 oldVelocity = this.velocity;
      if (this.horizontalCollision || this.verticalCollision) {
         this.velocity = new Vec3(collidedX ? 0.0 : oldVelocity.x, this.verticalCollision ? 0.0 : oldVelocity.y, collidedZ ? 0.0 : oldVelocity.z);
      }
   }

   protected boolean isSneaking() {
      return this.input.keys.shift() || this.input.forceSneak;
   }

   private void moveRelative(float speed, Vec3 input) {
      Vec3 delta = this.inputVector(input, speed, this.yaw);
      this.velocity = this.velocity.add(delta);
   }

   public static SimulatedPlayerInput of(Player player, SimulatedPlayerInput.SimulatedInput input) {
      return new SimulatedPlayerInput(
         player,
         input,
         player.position(),
         player.position().subtract(new Vec3(player.xo, player.yo, player.zo)),
         player.getBoundingBox(),
         player.getYRot(),
         player.getXRot(),
         player.isSprinting(),
         (float)player.fallDistance,
         ((LivingEntityMixin_1248)(Object)player).getJumpDelay(),
         ((LivingEntityMixin_1248)(Object)player).isJumping_(),
         player.isFallFlying(),
         player.onGround(),
         player.horizontalCollision,
         player.verticalCollision,
         player.isInWater(),
         player.isSwimming(),
         player.isUnderWater(),
         fluidHeightsOf(player),
         eyeFluidsOf(player)
      );
   }

   private Vec3 backOffFromEdge(Vec3 movement) {
      if (movement.y <= 0.0 && this.isAboveGround()) {
         double dx = movement.x;
         double dz = movement.z;

         double step;
         for (step = EDGE_STEP;
            dx != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(dx, EDGE_PROBE_DOWN_X, 0.0));
            dx += dx > 0.0 ? -step : step
         ) {
            if (dx < step && dx >= -step) {
               dx = 0.0;
               break;
            }
         }

         while (dz != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(0.0, EDGE_PROBE_DOWN_Z, dz))) {
            if (dz < step && dz >= -step) {
               dz = 0.0;
               break;
            }

            dz += dz > 0.0 ? -step : step;
         }

         while (dx != 0.0 && dz != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(dx, EDGE_PROBE_DOWN_XZ, dz))) {
            dx = dx < step && dx >= -step ? 0.0 : (dx > 0.0 ? dx - step : dx + step);
            if (dz < step && dz >= -step) {
               dz = 0.0;
               break;
            }

            dz += dz > 0.0 ? -step : step;
         }

         if (movement.x != dx || movement.z != dz) {
            this.backedOffEdge = true;
         }

         if (this.isSneaking()) {
            movement = new Vec3(dx, movement.y, dz);
         }
      }

      return movement;
   }

   public SimulatedPlayerInput copy() {
      SimulatedPlayerInput simulatedPlayerInput = new SimulatedPlayerInput(
         this.player,
         this.input,
         this.position,
         this.velocity,
         this.boundingBox,
         this.yaw,
         this.pitch,
         this.sprinting,
         this.fallDistance,
         this.jumpDelay,
         this.jumping,
         this.fallFlying,
         this.onGround,
         this.horizontalCollision,
         this.verticalCollision,
         this.inWater,
         this.swimming,
         this.underWater,
         new Object2DoubleArrayMap(this.fluidHeights),
         new HashSet<>(this.eyeFluids)
      );
      simulatedPlayerInput.speedOverride = this.speedOverride;
      simulatedPlayerInput.dimensions = this.dimensions;
      simulatedPlayerInput.ticks = this.ticks;
      return simulatedPlayerInput;
   }

   private boolean isAboveGround() {
      return this.onGround
         || this.fallDistance < EDGE_MAX_FALL && !this.player.level().noCollision(this.player, this.boundingBox.move(0.0, this.fallDistance - EDGE_FALL_OFFSET, 0.0));
   }

   public BlockPos blockPosOf(Vec3 vec) {
      return new BlockPos(Mth.floor(vec.x), Mth.floor(vec.y), Mth.floor(vec.z));
   }

   public BlockState blockStateAt(BlockPos pos) {
      return this.player.level().getBlockState(pos);
   }

   private Vec3 viewVector(float pitch, float yaw) {
      float pitchRad = (float)(pitch * PI_YAW / HALF_TURN_YAW);
      float yawRad = (float)(-yaw * PI_PITCH / HALF_TURN_PITCH);
      float cosYaw = Mth.cos(yawRad);
      float sinYaw = Mth.sin(yawRad);
      float cosPitch = Mth.cos(pitchRad);
      float sinPitch = Mth.sin(pitchRad);
      return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
   }

   private void updateEyeFluids() {
      this.underWater = this.eyeFluids.contains(FluidTags.WATER);
      this.eyeFluids.clear();
      double minY = this.getEyeY() - EYE_FLUID_OFFSET;
      if (!(
         this.player.getVehicle() instanceof Boat boat && !boat.isUnderWater() && boat.getBoundingBox().maxY >= minY && boat.getBoundingBox().minY <= minY
      )) {
         BlockPos pos = BlockPos.containing(this.position.x, minY, this.position.z);
         FluidState fluidState = this.player.level().getFluidState(pos);
         double height = pos.getY() + fluidState.getHeight(this.player.level(), pos);
         if (height > minY) {
            this.eyeFluids.addAll(fluidState.typeHolder().tags().toList());
         }
      }
   }

   public static SimulatedPlayerInput ofClientPlayer(SimulatedPlayerInput.SimulatedInput input) {
      LocalPlayer player = mc.player;
      return new SimulatedPlayerInput(
         player,
         input,
         player.position(),
         player.getDeltaMovement(),
         player.getBoundingBox(),
         player.getYRot(),
         player.getXRot(),
         player.isSprinting(),
         (float)player.fallDistance,
         ((LivingEntityMixin_1248)(Object)player).getJumpDelay(),
         ((LivingEntityMixin_1248)(Object)player).isJumping_(),
         player.isFallFlying(),
         player.onGround(),
         player.horizontalCollision,
         player.verticalCollision,
         player.isInWater(),
         player.isSwimming(),
         player.isUnderWater(),
         fluidHeightsOf(player),
         eyeFluidsOf(player)
      );
   }

   private void resetFallDistance() {
      this.fallDistance = 0.0F;
   }

   private boolean shouldStopSprinting() {
      return this.input.forwardImpulse <= 0.0F || this.input.keys.shift() || this.horizontalCollision || this.isInWater() && !this.isUnderWater();
   }

   public void tick() {
      this.ticks++;
      this.backedOffEdge = false;
      if (!(this.position.y <= VOID_Y)) {
         this.input.update();
         this.updateInWater();
         this.updateEyeFluids();
         this.updateSprinting();
         this.updateSwimming();
         if (this.jumpDelay > 0) {
            this.jumpDelay--;
         }

         this.jumping = this.input.keys.jump();
         double velX = this.velocity.x;
         double velY = this.velocity.y;
         double velZ = this.velocity.z;
         if (Math.abs(this.velocity.x) < MIN_VEL_X) {
            velX = 0.0;
         }

         if (Math.abs(this.velocity.y) < MIN_VEL_Y) {
            velY = 0.0;
         }

         if (Math.abs(this.velocity.z) < MIN_VEL_Z) {
            velZ = 0.0;
         }

         if (this.onGround) {
            this.fallFlying = false;
         }

         this.velocity = new Vec3(velX, velY, velZ);
         if (this.jumping) {
            double fluidHeight = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean inFluid = this.isInWater() && fluidHeight > 0.0;
            double jumpThreshold = this.fluidJumpThreshold();
            if (!inFluid || this.onGround && !(fluidHeight > jumpThreshold)) {
               if (!this.isInLava() || this.onGround && !(fluidHeight > jumpThreshold)) {
                  if ((this.onGround || inFluid && fluidHeight <= jumpThreshold) && this.jumpDelay == 0) {
                     this.jumpFromGround();
                     this.jumpDelay = 10;
                  }
               } else {
                  this.jumpInFluid(FluidTags.LAVA);
               }
            } else {
               this.jumpInFluid(FluidTags.WATER);
            }
         }

         float strafe = this.input.strafeImpulse * STRAFE_SCALE;
         float forward = this.input.forwardImpulse * FORWARD_SCALE;
         float up = 0.0F;
         if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
         }

         this.travel(new Vec3(strafe, up, forward));
      }
   }

   public Vec3 getPlayerPosition() {
      return this.player.position();
   }

   private boolean isTrapdoorLadder(BlockPos pos, BlockState blockState) {
      if (!blockState.getValue(TrapDoorBlock.OPEN)) {
         return false;
      }

      BlockState blockState2 = this.player.level().getBlockState(pos.below());
      return blockState2.is(Blocks.LADDER) && blockState2.getValue(LadderBlock.FACING).equals(blockState.getValue(TrapDoorBlock.FACING));
   }

   private Vec3 getViewVector() {
      return this.viewVector(this.pitch, this.yaw);
   }

   private double getEyeY() {
      return this.position.y + this.player.getEyeHeight();
   }

   private Vec3 collide(Vec3 movement) {
      AABB aabb = this.dimensions.makeBoundingBox(this.position);
      List<VoxelShape> voxelShapes = Collections.emptyList();
      Vec3 collided;
      if (movement.lengthSqr() == 0.0) {
         collided = movement;
      } else {
         collided = Entity.collideBoundingBox(this.player, movement, aabb, this.player.level(), voxelShapes);
      }

      boolean collidedX = movement.x != collided.x;
      boolean collidedY = movement.y != collided.y;
      boolean collidedZ = movement.z != collided.z;
      boolean grounded = this.onGround || collidedY && movement.y < 0.0;
      if (this.player.maxUpStep() > 0.0F && grounded && (collidedX || collidedZ)) {
         Vec3 stepUp = Entity.collideBoundingBox(this.player, new Vec3(movement.x, this.player.maxUpStep(), movement.z), aabb, this.player.level(), voxelShapes);
         Vec3 stepUpOnly = Entity.collideBoundingBox(
            this.player, new Vec3(0.0, this.player.maxUpStep(), 0.0), aabb.expandTowards(movement.x, 0.0, movement.z), this.player.level(), voxelShapes
         );
         Vec3 stepAcross = Entity.collideBoundingBox(this.player, new Vec3(movement.x, 0.0, movement.z), aabb.move(stepUpOnly), this.player.level(), voxelShapes).add(stepUpOnly);
         if (stepUpOnly.y < this.player.maxUpStep() && stepAcross.horizontalDistanceSqr() > stepUp.horizontalDistanceSqr()) {
            stepUp = stepAcross;
         }

         if (stepUp.horizontalDistanceSqr() > collided.horizontalDistanceSqr()) {
            return stepUp.add(Entity.collideBoundingBox(this.player, new Vec3(0.0, -stepUp.y + movement.y, 0.0), aabb.move(stepUp), this.player.level(), voxelShapes));
         }
      }

      return collided;
   }

   public static SimulatedPlayerInput predict(Player player, int ticks) {
      SimulatedPlayerInput simulatedPlayerInput = of(player, SimulatedPlayerInput.SimulatedInput.guessInput(player));

      for (int i = 0; i < ticks; i++) {
         simulatedPlayerInput.tick();
      }

      return simulatedPlayerInput;
   }

   private float frictionSpeed(float friction) {
      return this.onGround ? this.getSpeed() * (GROUND_SPEED_FACTOR / (friction * friction * friction)) : this.airSpeed();
   }

   private Vec3 handleRelativeFriction(Vec3 input, float friction) {
      this.moveRelative(this.frictionSpeed(friction), input);
      this.velocity = this.handleOnClimbable(this.velocity);
      this.move(this.velocity);
      Vec3 result = this.velocity;
      BlockPos pos = this.blockPosOf(this.position);
      BlockState blockState = this.blockStateAt(pos);
      if ((this.horizontalCollision || this.jumping)
         && (this.onClimbable() || blockState != null && blockState.is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this.player))) {
         result = new Vec3(result.x, CLIMB_SPEED, result.z);
      }

      return result;
   }

   private float airSpeed() {
      float speed = AIR_SPEED;
      return this.input.keys.sprint() ? speed + AIR_SPRINT_BONUS : speed;
   }

   private Vec3 inputVector(Vec3 input, float speed, float yaw) {
      double lengthSq = input.lengthSqr();
      if (lengthSq < MIN_INPUT_SQ) {
         return Vec3.ZERO;
      }

      Vec3 scaled = (lengthSq > 1.0 ? input.normalize() : input).scale(speed);
      float sin = Mth.sin(yaw * DEG_TO_RAD_SIN);
      float cos = Mth.cos(yaw * DEG_TO_RAD_COS);
      return new Vec3(scaled.x * cos - scaled.z * sin, scaled.y, scaled.z * cos + scaled.x * sin);
   }

   private void updateInWater() {
      if (this.player.getVehicle() instanceof Boat) {
         Boat vehicle = (Boat)this.player.getVehicle();
         if (!vehicle.isUnderWater()) {
            this.inWater = false;
            return;
         }
      }

      if (this.updateFluidHeightAndFlow(FluidTags.WATER, WATER_FLOW_SCALE)) {
         this.resetFallDistance();
         this.inWater = true;
      } else {
         this.inWater = false;
      }
   }

   private void jumpInFluid(TagKey<Fluid> tagKey) {
      this.velocity = this.velocity.add(0.0, FLUID_JUMP_SPEED, 0.0);
   }

   public SimulatedPlayerInput copyState() {
      SimulatedPlayerInput simulatedPlayerInput = new SimulatedPlayerInput(
         this.player,
         this.input,
         this.position,
         this.velocity,
         this.boundingBox,
         this.yaw,
         this.pitch,
         this.sprinting,
         this.fallDistance,
         this.jumpDelay,
         this.jumping,
         this.fallFlying,
         this.onGround,
         this.horizontalCollision,
         this.verticalCollision,
         this.inWater,
         this.swimming,
         this.underWater,
         new Object2DoubleArrayMap(this.fluidHeights),
         new HashSet<>(this.eyeFluids)
      );
      simulatedPlayerInput.dimensions = this.dimensions;
      return simulatedPlayerInput;
   }

   public double getFluidHeight(TagKey<Fluid> tagKey) {
      return this.fluidHeights.getDouble(tagKey);
   }

   private void updateSprinting() {
      if (this.sprinting && this.shouldStopSprinting()) {
         this.sprinting = false;
      }

      if (!this.sprinting && this.canStartSprinting()) {
         this.sprinting = true;
      }
   }

   private boolean canStartSprinting() {
      return this.input.keys.sprint()
         && this.input.forwardImpulse > MIN_SPRINT_FORWARD
         && !this.input.keys.shift()
         && !this.horizontalCollision
         && !this.player.isUsingItem()
         && !this.player.isPassenger()
         && (!this.isInWater() || this.isUnderWater());
   }

   private static HashSet<TagKey<Fluid>> eyeFluidsOf(Player player) {
      HashSet hashSet = new HashSet();
      if (player.isEyeInFluid(FluidTags.WATER)) {
         hashSet.add(FluidTags.WATER);
      }

      if (player.isEyeInFluid(FluidTags.LAVA)) {
         hashSet.add(FluidTags.LAVA);
      }

      return hashSet;
   }

   private boolean isSprinting() {
      return this.sprinting;
   }

   private boolean isFree(AABB aabb) {
      return this.player.level().noCollision(this.player, aabb) && !this.player.level().containsAnyLiquid(aabb);
   }

   public boolean isInLava() {
      return this.fluidHeights.getDouble(FluidTags.LAVA) > 0.0;
   }

   private static Object2DoubleMap<TagKey<Fluid>> fluidHeightsOf(Player player) {
      Object2DoubleArrayMap object2DoubleArrayMap = new Object2DoubleArrayMap();
      double fluidHeight = player.getFluidHeight(FluidTags.WATER);
      if (fluidHeight > 0.0) {
         object2DoubleArrayMap.put(FluidTags.WATER, fluidHeight);
      }

      double fluidHeight2 = player.getFluidHeight(FluidTags.LAVA);
      if (fluidHeight2 > 0.0) {
         object2DoubleArrayMap.put(FluidTags.LAVA, fluidHeight2);
      }

      return object2DoubleArrayMap;
   }

   public static boolean willStopSprinting(int ticks) {
      if (mc.player != null && mc.player.input != null && ticks > 0) {
         SimulatedPlayerInput simulatedPlayerInput = ofClientPlayer(SimulatedPlayerInput.SimulatedInput.fromClientPlayer(mc.player.input.keyPresses));
         if (!simulatedPlayerInput.sprinting) {
            return true;
         }

         for (int i = 0; i < ticks; i++) {
            simulatedPlayerInput.tick();
            if (!simulatedPlayerInput.sprinting) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private BlockPos belowPos() {
      return BlockPos.containing(this.position.x, this.boundingBox.minY - BELOW_OFFSET, this.position.z);
   }

   public boolean hasEffect(Holder<MobEffect> holder) {
      MobEffectInstance effect = this.player.getEffect(holder);
      return effect != null && effect.getDuration() >= this.ticks;
   }

   private float getSpeed() {
      return Float.isNaN(this.speedOverride) ? DEFAULT_SPEED : this.speedOverride;
   }

   private boolean isFreeAt(double dx, double dy, double dz) {
      return this.isFree(this.boundingBox.move(dx, dy, dz));
   }

   private Vec3 handleOnClimbable(Vec3 velocity) {
      if (!this.onClimbable()) {
         return velocity;
      }

      this.resetFallDistance();
      double x = Mth.clamp(velocity.x, CLIMB_MIN_X, CLIMB_MAX_X);
      double z = Mth.clamp(velocity.z, CLIMB_MIN_Z, CLIMB_MAX_Z);
      double y = Math.max(velocity.y, CLIMB_MIN_Y);
      if (y < 0.0 && !this.blockStateAt(this.blockPosOf(this.position)).is(Blocks.SCAFFOLDING) && this.player.isSuppressingSlidingDownLadder()) {
         y = 0.0;
      }

      return new Vec3(x, y, z);
   }

   private void updateSwimming() {
      if (this.swimming) {
         this.swimming = this.isSprinting() && this.isInWater() && !this.player.isPassenger();
      } else {
         this.swimming = this.isSprinting()
            && this.isUnderWater()
            && !this.player.isPassenger()
            && this.player.level().getFluidState(this.blockPosOf(this.position)).is(FluidTags.WATER);
      }
   }

   public boolean isUnderWater() {
      return this.underWater && this.isInWater();
   }

   private float jumpFactor() {
      float factor = 0.0F;
      Block block = this.blockStateAt(this.blockPosOf(this.position)).getBlock();
      if (block != null) {
         factor = block.getJumpFactor();
      }

      float belowFactor = 0.0F;
      Block block2 = this.blockStateAt(this.belowPos()).getBlock();
      if (block2 != null) {
         belowFactor = block2.getJumpFactor();
      }

      return factor == 1.0F ? belowFactor : factor;
   }

   private void travel(Vec3 input) {
      if (this.swimming && !this.player.isPassenger()) {
         double lookY = this.getViewVector().y;
         double swimRate = lookY < SWIM_LOOK_DOWN ? SWIM_DOWN_RATE : SWIM_UP_RATE;
         BlockPos pos = new BlockPos(Mth.floor(this.position.x), Mth.floor(this.position.y + 1.0 - SWIM_HEAD_OFFSET), Mth.floor(this.position.z));
         if (lookY <= 0.0 || this.input.keys.jump() || !this.player.level().getBlockState(pos).getFluidState().isEmpty()) {
            this.velocity = this.velocity.add(0.0, (lookY - this.velocity.y) * swimRate, 0.0);
         }
      }

      double velY = this.velocity.y;
      double gravity = GRAVITY;
      boolean falling = this.velocity.y <= 0.0;
      if (this.velocity.y <= 0.0 && this.hasEffect(MobEffects.SLOW_FALLING)) {
         gravity = SLOW_FALL_GRAVITY;
         this.resetFallDistance();
      }

      if (this.isInWater() && this.player.isAffectedByFluids()) {
         double startY = this.position.y;
         float drag = this.isSprinting() ? WATER_SPRINT_DRAG : WATER_DRAG;
         float speed = WATER_SPEED;
         float efficiency = (float)this.attribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
         if (!this.onGround) {
            efficiency *= AIR_DEPTH_STRIDER_FACTOR;
         }

         if (efficiency > 0.0F) {
            drag += (DEPTH_STRIDER_DRAG - drag) * efficiency / DEPTH_STRIDER_DRAG_DIVISOR;
            speed += (this.getSpeed() - speed) * efficiency / DEPTH_STRIDER_SPEED_DIVISOR;
         }

         if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
            drag = DOLPHIN_DRAG;
         }

         this.moveRelative(speed, input);
         this.move(this.velocity);
         Vec3 vec2 = this.velocity;
         if (this.horizontalCollision && this.onClimbable()) {
            vec2 = new Vec3(vec2.x, WATER_CLIMB_SPEED, vec2.z);
         }

         this.velocity = vec2.multiply(drag, WATER_VERTICAL_DRAG, drag);
         Vec3 fluidFallingAdjustedMovement = this.player.getFluidFallingAdjustedMovement(gravity, falling, this.velocity);
         this.velocity = fluidFallingAdjustedMovement;
         if (this.horizontalCollision && this.isFreeAt(fluidFallingAdjustedMovement.x, fluidFallingAdjustedMovement.y + WATER_EXIT_STEP - this.position.y + startY, fluidFallingAdjustedMovement.z)) {
            this.velocity = new Vec3(fluidFallingAdjustedMovement.x, WATER_EXIT_JUMP, fluidFallingAdjustedMovement.z);
         }
      } else if (this.isInLava() && this.player.isAffectedByFluids()) {
         double lavaStartY = this.position.y;
         this.moveRelative(LAVA_SPEED, input);
         this.move(this.velocity);
         if (this.getFluidHeight(FluidTags.LAVA) <= this.fluidJumpThreshold()) {
            this.velocity = this.velocity.multiply(LAVA_DRAG_X, LAVA_DRAG_Y, LAVA_DRAG_Z);
            this.velocity = this.player.getFluidFallingAdjustedMovement(gravity, falling, this.velocity);
         } else {
            this.velocity = this.velocity.scale(LAVA_DEEP_DRAG);
         }

         if (!this.player.isNoGravity()) {
            this.velocity = this.velocity.add(0.0, -gravity / LAVA_GRAVITY_DIVISOR, 0.0);
         }

         if (this.horizontalCollision && this.isFreeAt(this.velocity.x, this.velocity.y + LAVA_EXIT_STEP - this.position.y + lavaStartY, this.velocity.z)) {
            this.velocity = new Vec3(this.velocity.x, LAVA_EXIT_JUMP, this.velocity.z);
         }
      } else if (this.fallFlying) {
         Vec3 motion = this.velocity;
         Vec3 look = this.getViewVector();
         float pitchRad = this.pitch * DEG_TO_RAD;
         double horizontalLook = Math.sqrt(look.x * look.x + look.z * look.z);
         double horizontalSpeed = this.velocity.horizontalDistance();
         double lookLength = look.length();
         float lift = Mth.cos(pitchRad);
         lift = (float)(lift * (lift * Math.min(1.0, lookLength / GLIDE_LOOK_DIVISOR)));
         motion = this.velocity.add(0.0, gravity * (GLIDE_GRAVITY_BASE + lift * GLIDE_GRAVITY_FACTOR), 0.0);
         if (motion.y < 0.0 && horizontalLook > 0.0) {
            double descent = motion.y * GLIDE_LIFT * lift;
            motion = motion.add(look.x * descent / horizontalLook, descent, look.z * descent / horizontalLook);
         }

         if (pitchRad < 0.0F && horizontalLook > 0.0) {
            double climb = horizontalSpeed * -Mth.sin(pitchRad) * GLIDE_CLIMB;
            motion = motion.add(-look.x * climb / horizontalLook, climb * GLIDE_CLIMB_Y, -look.z * climb / horizontalLook);
         }

         if (horizontalLook > 0.0) {
            motion = motion.add((look.x / horizontalLook * horizontalSpeed - motion.x) * GLIDE_TURN_X, 0.0, (look.z / horizontalLook * horizontalSpeed - motion.z) * GLIDE_TURN_Z);
         }

         this.velocity = motion.multiply(GLIDE_DRAG_X, GLIDE_DRAG_Y, GLIDE_DRAG_Z);
         this.move(this.velocity);
      } else {
         BlockPos below = this.belowPos();
         float friction = this.player.level().getBlockState(below).getBlock().getFriction();
         float horizontalDrag = this.onGround ? friction * GROUND_FRICTION_FACTOR : AIR_FRICTION;
         Vec3 moved = this.handleRelativeFriction(input, friction);
         double newY = moved.y;
         if (this.hasEffect(MobEffects.LEVITATION)) {
            MobEffectInstance mobEffectInstance = this.getEffect(MobEffects.LEVITATION);
            if (mobEffectInstance != null) {
               newY += (LEVITATION_SPEED * (mobEffectInstance.getAmplifier() + 1) - moved.y) * LEVITATION_ACCEL;
            }
         } else if (this.player.level().isClientSide() && !this.player.level().isLoaded(below)) {
            newY = this.position.y > this.player.level().getMaxY() ? UNLOADED_FALL : 0.0;
         } else if (!this.player.isNoGravity()) {
            newY -= gravity;
         }

         if (this.player.shouldDiscardFriction()) {
            this.velocity = new Vec3(moved.x, newY, moved.z);
         } else {
            this.velocity = new Vec3(moved.x * horizontalDrag, newY * VERTICAL_DRAG, moved.z * horizontalDrag);
         }
      }

      if (this.player.getAbilities().flying && !this.player.isPassenger()) {
         this.velocity = new Vec3(this.velocity.x, velY * FLYING_VERTICAL_DRAG, this.velocity.z);
         this.resetFallDistance();
      }
   }

   public static boolean sprintsAfterRelease(int ticks) {
      if (mc.player != null && mc.player.input != null) {
         SimulatedPlayerInput simulatedPlayerInput = ofClientPlayer(SimulatedPlayerInput.SimulatedInput.fromClientPlayer(mc.player.input.keyPresses));
         Input keys = simulatedPlayerInput.input.keys;
         simulatedPlayerInput.input.keys = new Input(false, false, false, false, keys.jump(), keys.shift(), false);

         for (int i = 0; i < ticks; i++) {
            simulatedPlayerInput.tick();
         }

         return simulatedPlayerInput.sprinting;
      } else {
         return false;
      }
   }

   private boolean isInWater() {
      return this.inWater;
   }

   public static SimulatedPlayerInput predictClient(int ticks) {
      SimulatedPlayerInput simulatedPlayerInput = ofClientPlayer(SimulatedPlayerInput.SimulatedInput.fromClientPlayer(mc.player.input.keyPresses));

      for (int i = 0; i < ticks; i++) {
         simulatedPlayerInput.tick();
      }

      return simulatedPlayerInput;
   }

   public void jumpFromGround() {
      this.velocity = this.velocity.add(0.0, this.jumpPower() - this.velocity.y, 0.0);
      if (this.isSprinting()) {
         float yawRad = (float)Math.toRadians(this.yaw);
         this.velocity = this.velocity.add(-Mth.sin(yawRad) * SPRINT_JUMP_BOOST_X, 0.0, Mth.cos(yawRad) * SPRINT_JUMP_BOOST_Z);
      }
   }

   public SimulatedPlayerInput(
      Player player,
      SimulatedPlayerInput.SimulatedInput simulatedInput,
      Vec3 position,
      Vec3 velocity,
      AABB boundingBox,
      float yaw,
      float pitch,
      boolean sprinting,
      float fallDistance,
      int jumpDelay,
      boolean jumping,
      boolean fallFlying,
      boolean onGround,
      boolean horizontalCollision,
      boolean verticalCollision,
      boolean inWater,
      boolean swimming,
      boolean underWater,
      Object2DoubleMap<TagKey<Fluid>> fluidHeights,
      HashSet<TagKey<Fluid>> eyeFluids
   ) {
      this.player = player;
      this.input = simulatedInput;
      this.position = position;
      this.velocity = velocity;
      this.boundingBox = boundingBox;
      this.yaw = yaw;
      this.pitch = pitch;
      this.sprinting = sprinting;
      this.fallDistance = fallDistance;
      this.jumpDelay = jumpDelay;
      this.jumping = jumping;
      this.fallFlying = fallFlying;
      this.onGround = onGround;
      this.horizontalCollision = horizontalCollision;
      this.verticalCollision = verticalCollision;
      this.inWater = inWater;
      this.swimming = swimming;
      this.underWater = underWater;
      this.fluidHeights = fluidHeights;
      this.eyeFluids = eyeFluids;
      this.dimensions = ((EntityMixin_1159)(Object)player).getDimensions_();
   }

   private MobEffectInstance getEffect(Holder<MobEffect> holder) {
      MobEffectInstance effect = this.player.getEffect(holder);
      return effect != null && effect.getDuration() >= this.ticks ? effect : null;
   }

   public void setDimensions(EntityDimensions entityDimensions) {
      this.dimensions = entityDimensions;
      this.boundingBox = entityDimensions.makeBoundingBox(this.position);
   }

   private boolean isInUnloadedArea() {
      AABB aabb = this.boundingBox.inflate(1.0);
      int minX = Mth.floor(aabb.minX);
      int maxX = Mth.ceil(aabb.maxX);
      int minZ = Mth.floor(aabb.minZ);
      int maxZ = Mth.ceil(aabb.maxZ);
      return !this.player.level().hasChunksAt(minX, minZ, maxX, maxZ);
   }

   private float jumpPower() {
      return JUMP_POWER * this.jumpFactor() + this.jumpBoost();
   }

   private double fluidJumpThreshold() {
      return this.player.getEyeHeight() < MIN_EYE_HEIGHT_FOR_FLUID_JUMP ? 0.0 : FLUID_JUMP_THRESHOLD;
   }

   public double attribute(Holder<Attribute> holder) {
      return this.player.getAttributes().getValue(holder);
   }

   private float jumpBoost() {
      if (this.hasEffect(MobEffects.JUMP_BOOST)) {
         MobEffectInstance mobEffectInstance = this.getEffect(MobEffects.JUMP_BOOST);
         return JUMP_BOOST_PER_LEVEL * (mobEffectInstance.getAmplifier() + 1);
      } else {
         return 0.0F;
      }
   }

   private boolean updateFluidHeightAndFlow(TagKey<Fluid> tagKey, double flowScale) {
      if (this.isInUnloadedArea()) {
         return false;
      }

      AABB aabb = this.boundingBox.deflate(FLUID_BOX_DEFLATE);
      int minX = Mth.floor(aabb.minX);
      int maxX = Mth.ceil(aabb.maxX);
      int minY = Mth.floor(aabb.minY);
      int maxY = Mth.ceil(aabb.maxY);
      int minZ = Mth.floor(aabb.minZ);
      int maxZ = Mth.ceil(aabb.maxZ);
      double maxHeight = 0.0;
      boolean pushable = true;
      boolean touching = false;
      Vec3 totalFlow = Vec3.ZERO;
      int flowCount = 0;
      MutableBlockPos pos = new MutableBlockPos();

      for (int i = minX; i < maxX; i++) {
         for (int j = minY; j < maxY; j++) {
            for (int k = minZ; k < maxZ; k++) {
               pos.set(i, j, k);
               FluidState fluidState = this.player.level().getFluidState(pos);
               if (fluidState.is(tagKey)) {
                  double height = j + fluidState.getHeight(this.player.level(), pos);
                  if (height >= aabb.minY) {
                     touching = true;
                     maxHeight = Math.max(height - aabb.minY, maxHeight);
                     if (pushable) {
                        Vec3 flow = fluidState.getFlow(this.player.level(), pos);
                        if (maxHeight < FLOW_DEPTH_THRESHOLD) {
                           flow = flow.scale(maxHeight);
                        }

                        totalFlow = totalFlow.add(flow);
                        flowCount++;
                     }
                  }
               }
            }
         }
      }

      if (totalFlow.length() > 0.0) {
         if (flowCount > 0) {
            totalFlow = totalFlow.scale(1.0 / flowCount);
         }

         totalFlow = totalFlow.scale(flowScale);
         if (Math.abs(this.velocity.x) < FLOW_MIN_VEL_X && Math.abs(this.velocity.z) < FLOW_MIN_VEL_Z && totalFlow.length() < MIN_FLOW) {
            totalFlow = totalFlow.normalize().scale(MIN_FLOW_SPEED);
         }

         this.velocity = this.velocity.add(totalFlow);
      }

      this.fluidHeights.put(tagKey, maxHeight);
      return touching;
   }

   public boolean onClimbable() {
      BlockPos pos = this.blockPosOf(this.position);
      BlockState blockState = this.blockStateAt(pos);
      return blockState.is(BlockTags.CLIMBABLE) ? true : blockState.getBlock() instanceof TrapDoorBlock && this.isTrapdoorLadder(pos, blockState);
   }

   public static class SimulatedInput extends ClientInput {
      public boolean forceSneak = false;
      public float forwardImpulse;
      public float strafeImpulse;
      public Input keys;
      private static final float BACKWARD = -1.0F;
      private static final float RIGHT = -1.0F;
      private static final float SNEAK_STRAFE = 0.3F;
      private static final float SNEAK_FORWARD = 0.3F;
      private static final double SPRINT_SPEED_SQ = 0.014641;
      private static final double MOVING_SPEED_SQ = 0.0025000000000000005;
      private static final float DIRECTION_TOLERANCE = 20.0F;

      public static SimulatedPlayerInput.SimulatedInput guessInput(Player player) {
         Vec3 vec = player.position().subtract(new Vec3(player.xo, player.yo, player.zo));
         double speedSq = vec.horizontalDistanceSqr();
         Input keys = new Input(false, false, false, false, !player.onGround(), player.isShiftKeyDown(), speedSq >= SPRINT_SPEED_SQ);
         if (speedSq > MOVING_SPEED_SQ) {
            double moveYaw = MovementUtils.relativeMoveYaw(vec, player.getYRot());
            double relativeYaw = Mth.wrapDegrees(moveYaw);
            keys = MovementUtils.inputForDirection(keys, relativeYaw, DIRECTION_TOLERANCE);
         }

         return new SimulatedPlayerInput.SimulatedInput(keys);
      }

      public static SimulatedPlayerInput.SimulatedInput fromClientPlayer(Input input) {
         return new SimulatedPlayerInput.SimulatedInput(input);
      }

      public void update() {
         if (this.keys.forward() != this.keys.backward()) {
            this.forwardImpulse = this.keys.forward() ? 1.0F : BACKWARD;
         } else {
            this.forwardImpulse = 0.0F;
         }

         if (this.keys.left() == this.keys.right()) {
            this.strafeImpulse = 0.0F;
         } else {
            this.strafeImpulse = this.keys.left() ? 1.0F : RIGHT;
         }

         if (this.keys.shift()) {
            this.strafeImpulse = this.strafeImpulse * SNEAK_STRAFE;
            this.forwardImpulse = this.forwardImpulse * SNEAK_FORWARD;
         }
      }

      @Override
      public String toString() {
         return "SimulatedPlayerInput(forwards={"
            + this.keys.forward()
            + "}, backwards={"
            + this.keys.backward()
            + "}, left={"
            + this.keys.left()
            + "}, right={"
            + this.keys.right()
            + "}, jumping={"
            + this.keys.jump()
            + "}, sprinting="
            + this.keys.sprint()
            + ", slowDown="
            + this.keys.shift()
            + ")";
      }

      public SimulatedInput(Input input) {
         this.keys = input;
      }
   }
}
