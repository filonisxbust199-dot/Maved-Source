package ess.ll.client.render;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.entity.Entity;

public abstract class EntityParticleEmitter extends ParticleEmitter {
    protected final Entity entity;
    private final int lifetime;
    private final int particlesPerTick;
    private int age;

    protected EntityParticleEmitter(Entity entity2, int var2, int var3) {
        this.entity = entity2;
        this.lifetime = Math.max(1, var2);
        this.particlesPerTick = Math.max(1, var3);
    }

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    protected void emit(ClientParticleManager clientParticleManager) {
        if (this.entity != null && this.entity.level() != null) {
            ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
            for (int var3 = 0; var3 < this.particlesPerTick; var3++) {
                double var4 = threadLocalRandom.nextDouble() * 2.0 - 1.0;
                double var6 = threadLocalRandom.nextDouble() * 2.0 - 1.0;
                double var8 = threadLocalRandom.nextDouble() * 2.0 - 1.0;
                if (!(var4 * var4 + var6 * var6 + var8 * var8 > 1.0)) {
                    double x = this.entity.getX(var4 / 4.0);
                    double y = this.entity.getY(0.5 + var6 / 4.0);
                    double z = this.entity.getZ(var8 / 4.0);
                    this.spawnParticle(clientParticleManager, x, y, z, var4, var6 + 0.2, var8);
                }
            }
            this.age++;
            if (this.age >= this.lifetime) {
                this.remove();
            }
        } else {
            this.remove();
        }
    }

    protected abstract void spawnParticle(ClientParticleManager clientParticleManager, double var2, double var4, double var6, double var8, double var10, double var12);

    public int getAge() {
        return this.age;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    protected EntityParticleEmitter(Entity entity, int var2) {
        this(entity, var2, 16);
    }
}
