package ess.ll.client.render.targetfx.event;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.render.targetfx.api.StagedGeometryProvider;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
public class DeltaTrackerPoseStackEvent implements Event {
   private final DeltaTracker deltaTracker2;
   private final PoseStack poseStack2;
   private final StagedGeometryProvider stagedGeometryProvider2;
   private final Vec3 vec3;
   private final float currentValue;
   private final Matrix4f matrix4f4;
   private final Matrix4f matrix4f3;

   public float calculateValue() {
      return this.currentValue;
   }

   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker2;
   }

   public PoseStack getPoseStack() {
      return this.poseStack2;
   }

   public Matrix4f getMatrix4f() {
      return this.matrix4f3;
   }

   public DeltaTrackerPoseStackEvent(DeltaTracker deltaTracker, PoseStack poseStack, StagedGeometryProvider stagedGeometryProvider, Vec3 position, float amount, Matrix4f matrix4f, Matrix4f matrix4f2) {
      this.deltaTracker2 = deltaTracker;
      this.poseStack2 = poseStack;
      this.stagedGeometryProvider2 = stagedGeometryProvider;
      this.vec3 = position;
      this.currentValue = amount;
      this.matrix4f4 = matrix4f;
      this.matrix4f3 = matrix4f2;
   }

   public StagedGeometryProvider getStagedGeometryProvider() {
      return this.stagedGeometryProvider2;
   }

   public Matrix4f getMatrix4f2() {
      return this.matrix4f4;
   }

   public Vec3 getVec3() {
      return this.vec3;
   }
}
