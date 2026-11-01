package ess.ll.client.integration;

import ess.ll.client.util.TrailPoint;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface EntityExtensionAccess {
   void setBotEntity(boolean bot);

   List<TrailPoint> getTrailPoints();

   Vec2 getVec2f();

   void setVec3d(Vec3 vec);

   boolean botEntity();

   Vec3 getVec3d();

   double getDistanceEyePos(Entity entity);

   void setVec2f(Vec2 vec2);
}
