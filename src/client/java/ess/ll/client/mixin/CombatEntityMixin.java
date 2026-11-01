package ess.ll.client.mixin;
import ess.ll.client.integration.EntityExtensionAccess;
import ess.ll.client.util.EntityEyeDistanceUtils;
import ess.ll.client.util.TrailPoint;
import ess.ll.client.event.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.*;
import java.util.*;
@Mixin(Entity.class)
public abstract class CombatEntityMixin implements EntityExtensionAccess {
 @Unique private boolean ess$bot;
 @Unique private Vec2 ess$rotation=Vec2.ZERO;
 @Unique private Vec3 ess$position=Vec3.ZERO;
 @Unique private List<TrailPoint> ess$trail;
 public void setBotEntity(boolean value){ess$bot=value;} public boolean botEntity(){return ess$bot;}
 public Vec2 getVec2f(){return ess$rotation;} public void setVec2f(Vec2 v){ess$rotation=v;}
 public Vec3 getVec3d(){return ess$position;} public void setVec3d(Vec3 v){ess$position=v==null?Vec3.ZERO:v;}
 public List<TrailPoint> getTrailPoints(){if(ess$trail==null)ess$trail=new ArrayList<>();return ess$trail;}
 public double getDistanceEyePos(Entity other){return EntityEyeDistanceUtils.eyeDistanceToBox((Entity)(Object)this,other);}
 @ModifyReturnValue(method="getDeltaMovement",at=@At("RETURN"))
 private Vec3 ess$velocity(Vec3 value){return EntityVelocityEvent.INSTANCE.fire((Entity)(Object)this,value);}
 @ModifyExpressionValue(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;"))
 private Vec3 ess$move(Vec3 value){return EntityMoveEvent.INSTANCE.fire((Entity)(Object)this,value);}
 @Inject(method="pick",at=@At("RETURN"),cancellable=true)
 private void ess$raycast(double reach,float tickDelta,boolean fluids,CallbackInfoReturnable<HitResult> cir){
   RaycastEvent.INSTANCE.fireBlock((Entity)(Object)this,reach,tickDelta,fluids,cir);
 }
}
