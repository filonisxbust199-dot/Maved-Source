package ess.ll.client.integration;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
/** Integration with modules outside this port. All are absent/disabled in ESS. */
public final class OptionalModules {
 public static class Absent { public boolean isEnabled(){return false;} public boolean isActive(){return false;} }
 public record Value<T>(T value) { public T getValue(){return value;} public boolean isEnabled(){return Boolean.TRUE.equals(value);} public boolean is(String text){return text.equals(value);} }
 public static class ElytraTarget extends Absent {
  public final Value<Float> fallFlyingOffset=new Value<>(0f),unusedFloat=new Value<>(0f);
  public final Value<Boolean> aimAtPredicted=new Value<>(false),unusedFlag=new Value<>(false);
  public final Value<Integer> unusedInt=new Value<>(0);
  public final Value<String> unusedMode=new Value<>(""),unusedTarget=new Value<>("");
  public Vec3 predictedPosition;
 }
 public static class MaceHelper extends Absent { public boolean isAttacking(){return false;} }
 public static class AutoAnchor extends Absent {}
 public static class InventoryMove extends Absent {
  public final Value<Boolean> unusedOption=new Value<>(false);
  public boolean isMoving(){return false;} public int delayTicks(int ticks){return ticks;}
 }
}
