package ess.ll.client.render;

import ess.ll.client.core.EntityBurstEmitter;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.PlayerUtils;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public enum ClientParticleManager implements MinecraftAccess {
    INSTANCE;

    private final ClientParticleRenderer renderer = new ClientParticleRenderer();
    private final ArrayDeque<ClientParticleManager.RenderEntry> particles = new ArrayDeque<>();
    private final ArrayDeque<ClientParticleManager.RenderEntry> pendingParticles = new ArrayDeque<>();
    private final ArrayDeque<ParticleEmitter> emitters = new ArrayDeque<>();
    private final ArrayDeque<ParticleEmitter> pendingEmitters = new ArrayDeque<>();
    private int capacity = 16384;
    private boolean ticking;

    public void render() {
        this.renderer.registerListeners();
    }

    public void setCapacity(int var1) {
        this.capacity = Math.max(1, var1);
        this.enforceCapacity(0);
    }

    public int size() {
        return this.particles.size() + this.pendingParticles.size();
    }

    public boolean isEmpty() {
        return this.particles.isEmpty() && this.pendingParticles.isEmpty() && this.emitters.isEmpty() && this.pendingEmitters.isEmpty();
    }

    public Iterable<ClientParticleManager.RenderEntry> getParticles() {
        return this.particles;
    }

    public ClientParticle spawn(ClientParticle.SpawnOptions spawnOptions) {
        return this.spawn(spawnOptions, -1, 0.2F, 6);
    }

    public ClientParticle spawn(ClientParticle.SpawnOptions spawnOptions, int var2, float var3, int var4) {
        Objects.requireNonNull(spawnOptions, "args");
        ClientParticle clientParticle = new ClientParticle(spawnOptions);
        this.addParticle(clientParticle, var2, var3, var4);
        return clientParticle;
    }

    public boolean addParticle(ClientParticle clientParticle) {
        return this.addParticle(clientParticle, -1, 0.2F, 6);
    }

    public boolean addParticle(ClientParticle clientParticle, int var2, float var3, int var4) {
        if (ess.ll.client.module.misc.Optimization.noParticles()) {
            return false;
        }
        if (clientParticle != null && clientParticle.isAlive()) {
            ClientParticleManager.RenderEntry renderEntry = new ClientParticleManager.RenderEntry(clientParticle, var2, Math.max(0.01F, var3), resolveFadeDuration(clientParticle, var4));
            return this.enqueueParticle(renderEntry);
        } else {
            return false;
        }
    }

    public boolean addEmitter(ParticleEmitter particleEmitter) {
        if (ess.ll.client.module.misc.Optimization.noParticles()) {
            return false;
        }
        if (particleEmitter == null || !particleEmitter.isAlive()) {
            return false;
        } else if (this.ticking) {
            this.pendingEmitters.addLast(particleEmitter);
            return true;
        } else {
            this.emitters.addLast(particleEmitter);
            return true;
        }
    }

    public EntityBurstEmitter emitBurst(Entity entity, int var2) {
        EntityBurstEmitter entityBurstEmitter = new EntityBurstEmitter(entity, var2);
        this.addEmitter(entityBurstEmitter);
        return entityBurstEmitter;
    }

    public void clear() {
        this.particles.clear();
        this.pendingParticles.clear();
        this.emitters.clear();
        this.pendingEmitters.clear();
    }

    public void tick() {
        this.ticking = true;
        try {
            this.tickParticles();
            this.tickEmitters();
        } finally {
            this.ticking = false;
        }
        this.flushPendingParticles();
        this.flushPendingEmitters();
    }

    public void updateVisibility() {
        if (!this.particles.isEmpty()) {
            LocalPlayer player2 = mc.player;
            for (ClientParticleManager.RenderEntry renderEntry : this.particles) {
                ClientParticle particle2 = renderEntry.particle;
                renderEntry.visible = particle2.isAlive() && isWithinRenderDistance(player2, particle2) && PlayerUtils.isInView(particle2.getBounds());
            }
        }
    }

    private static boolean isWithinRenderDistance(Entity entity, ClientParticle clientParticle) {
        double maxRenderDistanceSquared = clientParticle.getMaxRenderDistanceSquared();
        return !(maxRenderDistanceSquared <= 0.0) && entity != null ? entity.distanceToSqr(clientParticle.getX(), clientParticle.getY(), clientParticle.getZ()) <= maxRenderDistanceSquared : true;
    }

    private boolean enqueueParticle(ClientParticleManager.RenderEntry renderEntry) {
        this.enforceCapacity(1);
        if (this.ticking) {
            this.pendingParticles.addLast(renderEntry);
            return true;
        } else {
            renderEntry.previousOpacity = renderEntry.opacity;
            renderEntry.opacity = calculateOpacity(renderEntry.particle, renderEntry.fadeDuration);
            this.particles.addLast(renderEntry);
            return true;
        }
    }

    private void tickParticles() {
        var iterator2 = this.particles.iterator();
        while (iterator2.hasNext()) {
            ClientParticleManager.RenderEntry renderEntry = (ClientParticleManager.RenderEntry) iterator2.next();
            ClientParticle particle2 = renderEntry.particle;
            particle2.tickSafely();
            if (!particle2.isAlive()) {
                iterator2.remove();
            } else {
                renderEntry.previousOpacity = renderEntry.opacity;
                renderEntry.opacity = calculateOpacity(particle2, renderEntry.fadeDuration);
            }
        }
    }

    private void tickEmitters() {
        var iterator2 = this.emitters.iterator();
        while (iterator2.hasNext()) {
            ParticleEmitter particleEmitter = (ParticleEmitter) iterator2.next();
            particleEmitter.tick(this);
            if (!particleEmitter.isAlive()) {
                iterator2.remove();
            }
        }
    }

    private void flushPendingParticles() {
        if (!this.pendingParticles.isEmpty()) {
            ClientParticleManager.RenderEntry renderEntry;
            while ((renderEntry = this.pendingParticles.pollFirst()) != null) {
                ClientParticle particle2 = renderEntry.particle;
                if (particle2.isAlive()) {
                    renderEntry.previousOpacity = renderEntry.opacity;
                    renderEntry.opacity = calculateOpacity(particle2, renderEntry.fadeDuration);
                    this.particles.addLast(renderEntry);
                }
            }
        }
    }

    private void flushPendingEmitters() {
        if (!this.pendingEmitters.isEmpty()) {
            ParticleEmitter particleEmitter;
            while ((particleEmitter = this.pendingEmitters.pollFirst()) != null) {
                if (particleEmitter.isAlive()) {
                    this.emitters.addLast(particleEmitter);
                }
            }
        }
    }

    private void enforceCapacity(int var1) {
        int var2 = this.particles.size() + this.pendingParticles.size() + var1 - this.capacity;
        while (var2 > 0) {
            ClientParticleManager.RenderEntry renderEntry = this.particles.pollFirst();
            if (renderEntry != null) {
                renderEntry.particle.remove();
                var2--;
            } else {
                ClientParticleManager.RenderEntry renderEntry2 = this.pendingParticles.pollFirst();
                if (renderEntry2 == null) {
                    break;
                }
                renderEntry2.particle.remove();
                var2--;
            }
        }
    }

    private static int resolveFadeDuration(ClientParticle clientParticle, int var1) {
        int var2 = Math.max(1, clientParticle.getLifetime());
        return var1 <= 0 ? Math.max(1, var2 / 2) : Math.max(1, Math.min(var1, var2));
    }

    private static float calculateOpacity(ClientParticle clientParticle, int var1) {
        int var2 = Math.max(1, clientParticle.getLifetime());
        float var3 = Math.min(var2, clientParticle.getAge());
        if (clientParticle.getInterpolationMode() == ParticleInterpolationMode.LINEAR) {
            return Math.clamp(1.0F - var3 / var2, 0.0F, 1.0F);
        }
        int var4 = Math.max(1, Math.min(var1, var2));
        float var5 = var2 - var4;
        float var6 = var3 > var5 ? (var2 - var3) / var4 : 1.0F;
        float var7 = Math.min(3.0F, var4);
        float var8 = var3 / Math.max(1.0F, var7);
        float var9 = Math.clamp(Math.min(var8, var6), 0.0F, 1.0F);
        return smoothstep(var9);
    }

    private static float smoothstep(float var0) {
        return var0 * var0 * (3.0F - 2.0F * var0);
    }

    public static final class RenderEntry {
        private final ClientParticle particle;
        private final int color;
        private final float size;
        private final int fadeDuration;
        private final float textureOffset = ThreadLocalRandom.current().nextFloat();
        private boolean visible = true;
        private float previousOpacity;
        private float opacity;

        public float getTextureOffset() {
            return this.textureOffset;
        }

        private RenderEntry(ClientParticle particle2, int color2, float size2, int fadeDuration2) {
            this.particle = particle2;
            this.color = color2;
            this.size = size2;
            this.fadeDuration = fadeDuration2;
        }

        public float interpolateOpacity(float var1) {
            return this.previousOpacity + (this.opacity - this.previousOpacity) * var1;
        }

        public boolean isVisible() {
            return this.visible;
        }

        public int getColor() {
            return this.color;
        }

        public ClientParticle getParticle() {
            return this.particle;
        }

        public float getSize() {
            return this.size;
        }
    }
}
