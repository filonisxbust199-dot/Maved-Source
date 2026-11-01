package ess.ll.client.core;

import ess.ll.client.render.ClientParticle;
import ess.ll.client.render.ClientParticleManager;
import ess.ll.client.render.EntityParticleEmitter;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.entity.Entity;

public class EntityBurstEmitter extends EntityParticleEmitter {
   private static final float ZERO_A = 0;
   private static final int ZERO_B = 0;
   private static final int ZERO_C = 0;
   private static final float ZERO_D = 0;
   private static final float ZERO_E = 0;
   private static final float MIN_SIZE = 0.2F;
   private static final float MAX_SIZE = 0.25F;
   private static final float GRAVITY = 0.225F;
   private static final float FRICTION = 0.75F;
   private static final float BRIGHT_RED_BASE = 0.6F;
   private static final float BRIGHT_RED_RANGE = 0.2F;
   private static final float BRIGHT_GREEN_BASE = 0.6F;
   private static final float BRIGHT_GREEN_RANGE = 0.3F;
   private static final float BRIGHT_BLUE_RANGE = 0.2F;
   private static final float DIM_RED_BASE = 0.1F;
   private static final float DIM_RED_RANGE = 0.2F;
   private static final float DIM_GREEN_BASE = 0.4F;
   private static final float DIM_GREEN_RANGE = 0.3F;
   private static final float DIM_BLUE_RANGE = 0.2F;
   private static final float RED_SCALE = 255.0F;
   private static final float GREEN_SCALE = 255.0F;
   private static final float BLUE_SCALE = 255.0F;
   private static final int OPAQUE = -16777216;

   @Override
   protected void spawnParticle(ClientParticleManager clientParticleManager, double x, double y, double z, double velX, double velY, double velZ) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int lifetime = 80 + random.nextInt(40);
      float size = random.nextFloat(MIN_SIZE, MAX_SIZE);
      int color = randomColor(random);
      clientParticleManager.spawn(
         new ClientParticle.SpawnOptions(x, y, z)
            .withVelocity(velX, velY, velZ)
            .withGravity(GRAVITY)
            .withFriction(FRICTION)
            .withLifetime(lifetime)
            .withCollisionEnabled(true)
            .withSize(size, size),
         color,
         size,
         Math.max(1, lifetime / 2)
      );
   }

   public EntityBurstEmitter(Entity entity, int count) {
      super(entity, count);
   }

   private static int randomColor(ThreadLocalRandom random) {
      float red;
      float green;
      float blue;
      if (random.nextInt(4) == 0) {
         red = BRIGHT_RED_BASE + random.nextFloat() * BRIGHT_RED_RANGE;
         green = BRIGHT_GREEN_BASE + random.nextFloat() * BRIGHT_GREEN_RANGE;
         blue = random.nextFloat() * BRIGHT_BLUE_RANGE;
      } else {
         red = DIM_RED_BASE + random.nextFloat() * DIM_RED_RANGE;
         green = DIM_GREEN_BASE + random.nextFloat() * DIM_GREEN_RANGE;
         blue = random.nextFloat() * DIM_BLUE_RANGE;
      }

      int redByte = Math.min(255, Math.max(0, (int)(red * RED_SCALE)));
      int greenByte = Math.min(255, Math.max(0, (int)(green * GREEN_SCALE)));
      int blueByte = Math.min(255, Math.max(0, (int)(blue * BLUE_SCALE)));
      return OPAQUE | redByte << 16 | greenByte << 8 | blueByte;
   }
}
