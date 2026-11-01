package ess.ll.client.config;

import ess.ll.client.render.ParticleRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParticleAnimationConfig {
   private int lastTick;
   private Consumer<ParticleAnimationConfig> tickCallback;
   public String animation;
   public String tag;
   public boolean dead;
   private boolean appearing = true;
   private final String blendMode;
   private double life;
   public double x;
   public double y;
   public double z;
   public double velX;
   public double velY;
   public double velZ;
   private double alpha;
   private double scale;
   public double targetScale;
   public double rotX;
   public double rotY;
   public double rotZ;
   public double rotSpeedX;
   public double rotSpeedY;
   public double rotSpeedZ;
   private double age;
   private final boolean gravity;
   public InteractionHand hand;
   private Identifier texture;
   private final List<Identifier> frames;
   public PoseStack poseStack;
   private final Iterator<Identifier> frameIterator;
   private static final double INITIAL_ALPHA = 255.0;
   private static final double INITIAL_ALPHA_ANIMATED = 255.0;
   private static final float LIFE_DECAY = 0.1F;
   private static final float LIFE_DECAY_RATE = 30.0F;
   private static final double MAX_LIFE = 9999.0;
   private static final float AGE_STEP = 0.1F;
   private static final float AGE_RATE = 30.0F;
   private static final double MOVE_RATE_X = 30.0;
   private static final double MOVE_RATE_Y = 30.0;
   private static final double MOVE_RATE_Z = 30.0;
   private static final double DRAG_X = 0.9;
   private static final float DRAG_RATE_X = 30.0F;
   private static final double DRAG_Y = 0.9;
   private static final float DRAG_RATE_Y = 30.0F;
   private static final double DRAG_Z = 0.9;
   private static final float DRAG_RATE_Z = 30.0F;
   private static final double GRAVITY = 0.01;
   private static final double GRAVITY_RATE = 30.0;
   private static final double SPIN_RATE_X = 30.0;
   private static final double SPIN_RATE_Y = 30.0;
   private static final double SPIN_RATE_Z = 30.0;
   private static final String MODE_SCALE = "SCALE";
   private static final double SHRINK_SPEED = 0.03;
   private static final double SHRINK_RATE = 30.0;
   private static final double GROW_SPEED = 0.07;
   private static final double GROW_RATE = 30.0;
   private static final String MODE_OPACITY = "OPACITY";
   private static final float FADE_SPEED = 10.0F;
   private static final float FADE_RATE = 30.0F;
   private static final double FULL_ALPHA = 255.0;
   private static final String MODE_SPAWN = "SPAWN";
   private static final String MODE_KEYFRAME = "KEYFRAME";
   private static final String BLEND_ADDITIVE = "ADDITIVE";
   private static final double DEG_TO_RAD_X = Math.PI / 180.0;
   private static final double DEG_TO_RAD_Y = Math.PI / 180.0;
   private static final double DEG_TO_RAD_Z = Math.PI / 180.0;
   private static final double HALF_SIZE = 0.25;

   public ParticleAnimationConfig(
      boolean gravity,
      double x,
      double y,
      double z,
      double velX,
      double velY,
      double velZ,
      double rotX,
      double rotY,
      double rotZ,
      double rotSpeedX,
      double rotSpeedY,
      double rotSpeedZ,
      double targetScale,
      Identifier texture,
      String tag,
      InteractionHand hand,
      String animation,
      String blendMode,
      double life,
      double alpha,
      Consumer<ParticleAnimationConfig> tickCallback
   ) {
      this.alpha = INITIAL_ALPHA;
      this.targetScale = 1.0;
      this.frames = new ArrayList<>();
      this.gravity = gravity;
      this.animation = animation;
      this.blendMode = blendMode;
      this.tag = tag;
      this.hand = hand;
      this.tickCallback = tickCallback;
      this.alpha = alpha;
      this.x = x;
      this.y = y;
      this.z = z;
      this.velX = velX;
      this.velY = velY;
      this.velZ = velZ;
      this.rotX = rotX;
      this.rotY = rotY;
      this.rotZ = rotZ;
      this.rotSpeedX = rotSpeedX;
      this.rotSpeedY = rotSpeedY;
      this.rotSpeedZ = rotSpeedZ;
      this.targetScale = targetScale;
      this.life = life;
      this.texture = texture;
      this.frameIterator = this.frames.iterator();
   }

   public ParticleAnimationConfig(
      boolean gravity,
      double x,
      double y,
      double z,
      double velX,
      double velY,
      double velZ,
      double rotX,
      double rotY,
      double rotZ,
      double rotSpeedX,
      double rotSpeedY,
      double rotSpeedZ,
      double targetScale,
      Identifier texture,
      String tag,
      InteractionHand hand,
      String animation,
      String blendMode,
      double life,
      double alpha
   ) {
      this(gravity, x, y, z, velX, velY, velZ, rotX, rotY, rotZ, rotSpeedX, rotSpeedY, rotSpeedZ, targetScale, texture, tag, hand, animation, blendMode, life, alpha, null);
   }

   public ParticleAnimationConfig(
      boolean gravity,
      double x,
      double y,
      double z,
      double velX,
      double velY,
      double velZ,
      double rotX,
      double rotY,
      double rotZ,
      double rotSpeedX,
      double rotSpeedY,
      double rotSpeedZ,
      double targetScale,
      Identifier texture,
      String tag,
      InteractionHand hand,
      String animation,
      String blendMode,
      double life,
      double alpha,
      Consumer<ParticleAnimationConfig> tickCallback,
      PoseStack poseStack
   ) {
      this(gravity, x, y, z, velX, velY, velZ, rotX, rotY, rotZ, rotSpeedX, rotSpeedY, rotSpeedZ, targetScale, texture, tag, hand, animation, blendMode, life, alpha, tickCallback);
      this.poseStack = poseStack;
   }

   public ParticleAnimationConfig(
      boolean gravity,
      double x,
      double y,
      double z,
      double velX,
      double velY,
      double velZ,
      double rotX,
      double rotY,
      double rotZ,
      double rotSpeedX,
      double rotSpeedY,
      double rotSpeedZ,
      double targetScale,
      Identifier texture,
      List<Identifier> frames,
      String tag,
      InteractionHand hand,
      String animation,
      String blendMode,
      double life,
      double alpha,
      Consumer<ParticleAnimationConfig> tickCallback
   ) {
      this.alpha = INITIAL_ALPHA_ANIMATED;
      this.targetScale = 1.0;
      this.frames = frames;
      this.gravity = gravity;
      this.animation = animation;
      this.blendMode = blendMode;
      this.tag = tag;
      this.hand = hand;
      this.tickCallback = tickCallback;
      this.x = x;
      this.y = y;
      this.z = z;
      this.alpha = alpha;
      this.velX = velX;
      this.velY = velY;
      this.velZ = velZ;
      this.rotX = rotX;
      this.rotY = rotY;
      this.rotZ = rotZ;
      this.rotSpeedX = rotSpeedX;
      this.rotSpeedY = rotSpeedY;
      this.rotSpeedZ = rotSpeedZ;
      this.targetScale = targetScale;
      this.texture = texture;
      this.frameIterator = frames.iterator();
   }

   public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, AbstractClientPlayer player, float partialTick) {
      if (!this.dead) {
         poseStack.pushPose();
         poseStack.translate(this.x, this.y, this.z);
         float renderScale = (float)this.scale;
         poseStack.scale(renderScale, renderScale, renderScale);
         RenderType renderType = BLEND_ADDITIVE.equals(this.blendMode)
            ? ParticleRenderer.additive(this.texture)
            : ParticleRenderer.translucent(this.texture);
         Matrix4f matrix = poseStack.last().pose();
         Vector3f translation = new Vector3f();
         Vector3f scaleVector = new Vector3f();
         matrix.getTranslation(translation);
         matrix.getScale(scaleVector);
         Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
         matrix.identity().translate(translation).scale(scaleVector).rotate(camera.rotation());
         matrix.rotate(new Quaternionf().rotateX((float)(this.rotX * DEG_TO_RAD_X)));
         matrix.rotate(new Quaternionf().rotateY((float)(this.rotY * DEG_TO_RAD_Y)));
         matrix.rotate(new Quaternionf().rotateZ((float)(this.rotZ * DEG_TO_RAD_Z)));
         double halfSizeExact = HALF_SIZE;
         int alphaByte = Math.clamp((int)this.alpha, 0, 255);
         float halfSize = (float)halfSizeExact;
         submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            Matrix4f matrix2 = pose.pose();
            Vector3f corner = new Vector3f();
            matrix2.transformPosition(halfSize, halfSize, 0.0F, corner);
            consumer.addVertex(corner.x, corner.y, corner.z).setUv(1.0F, 0.0F).setColor(255, 255, 255, alphaByte);
            matrix2.transformPosition(-halfSize, halfSize, 0.0F, corner);
            consumer.addVertex(corner.x, corner.y, corner.z).setUv(0.0F, 0.0F).setColor(255, 255, 255, alphaByte);
            matrix2.transformPosition(-halfSize, -halfSize, 0.0F, corner);
            consumer.addVertex(corner.x, corner.y, corner.z).setUv(0.0F, 1.0F).setColor(255, 255, 255, alphaByte);
            matrix2.transformPosition(halfSize, -halfSize, 0.0F, corner);
            consumer.addVertex(corner.x, corner.y, corner.z).setUv(1.0F, 1.0F).setColor(255, 255, 255, alphaByte);
         });
         poseStack.popPose();
      }
   }

   public void tick() {
      if (!this.dead) {
         float frameDelta = ParticleRenderer.frameDelta;
         this.life = this.life - LIFE_DECAY * frameDelta * LIFE_DECAY_RATE;
         this.life = Math.clamp(this.life, 0.0, MAX_LIFE);
         this.age = this.age + AGE_STEP * frameDelta * AGE_RATE;
         this.x = this.x + this.velX * frameDelta * MOVE_RATE_X;
         this.y = this.y + this.velY * frameDelta * MOVE_RATE_Y;
         this.z = this.z + this.velZ * frameDelta * MOVE_RATE_Z;
         this.velX = this.velX * Math.pow(DRAG_X, frameDelta * DRAG_RATE_X);
         this.velY = this.velY * Math.pow(DRAG_Y, frameDelta * DRAG_RATE_Y);
         this.velZ = this.velZ * Math.pow(DRAG_Z, frameDelta * DRAG_RATE_Z);
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            if (this.gravity) {
               this.velY = this.velY - GRAVITY * frameDelta * GRAVITY_RATE;
            }

            this.rotX = this.rotX + this.rotSpeedX * frameDelta * SPIN_RATE_X;
            this.rotY = this.rotY + this.rotSpeedY * frameDelta * SPIN_RATE_Y;
            this.rotZ = this.rotZ + this.rotSpeedZ * frameDelta * SPIN_RATE_Z;
            if (!this.frames.isEmpty() && this.frameIterator.hasNext() && player.tickCount % 4 == 0 && player.tickCount != this.lastTick) {
               this.texture = this.frameIterator.next();
            }

            if (MODE_SCALE.equals(this.animation)) {
               if (!this.appearing && this.life == 0.0) {
                  this.scale = this.scale - SHRINK_SPEED * frameDelta * SHRINK_RATE;
                  if (this.scale <= 0.0) {
                     this.dead = true;
                  }
               } else {
                  this.scale = this.scale + GROW_SPEED * frameDelta * GROW_RATE;
                  if (this.scale >= this.targetScale) {
                     this.scale = this.targetScale;
                     this.appearing = false;
                  }
               }
            } else if (MODE_OPACITY.equals(this.animation)) {
               if (!this.appearing && this.life == 0.0) {
                  this.alpha = this.alpha - FADE_SPEED * frameDelta * FADE_RATE;
                  if (this.alpha <= 0.0) {
                     this.dead = true;
                     this.alpha = 0.0;
                  }
               } else {
                  this.scale = this.targetScale;
                  this.appearing = false;
                  this.alpha = FULL_ALPHA;
               }
            } else if (MODE_SPAWN.equals(this.animation)) {
               if (!this.appearing && this.life == 0.0) {
                  this.dead = true;
               } else {
                  this.scale = this.targetScale;
                  this.appearing = false;
               }
            } else if (MODE_KEYFRAME.equals(this.animation)) {
               if (!this.appearing && !this.frameIterator.hasNext()) {
                  this.dead = true;
               } else {
                  this.scale = this.targetScale;
                  this.appearing = false;
               }
            }

            if (this.tickCallback != null) {
               this.tickCallback.accept(this);
            }

            this.lastTick = player.tickCount;
         }
      }
   }
}
