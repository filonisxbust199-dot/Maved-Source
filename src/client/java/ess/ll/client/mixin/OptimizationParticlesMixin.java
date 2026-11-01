package ess.ll.client.mixin;

import ess.ll.client.module.misc.Optimization;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optimization "Все частицы": no vanilla particle is created, and the ones alive when it turns on are dropped. */
@Mixin(ParticleEngine.class)
public abstract class OptimizationParticlesMixin {
    @Shadow public abstract void clearParticles();

    @Inject(method="createParticle",at=@At("HEAD"),cancellable=true)
    private void ess$create(ParticleOptions options,double x,double y,double z,double dx,double dy,double dz,CallbackInfoReturnable<Particle> cir) {
        if(Optimization.noParticles()) cir.setReturnValue(null);
    }

    @Inject(method="add",at=@At("HEAD"),cancellable=true)
    private void ess$add(Particle particle,CallbackInfo ci) {
        if(Optimization.noParticles()) ci.cancel();
    }

    @Inject(method="createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",at=@At("HEAD"),cancellable=true)
    private void ess$emitter(Entity entity,ParticleOptions options,int lifetime,CallbackInfo ci) {
        if(Optimization.noParticles()) ci.cancel();
    }

    @Inject(method="createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",at=@At("HEAD"),cancellable=true)
    private void ess$emitterDefault(Entity entity,ParticleOptions options,CallbackInfo ci) {
        if(Optimization.noParticles()) ci.cancel();
    }

    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void ess$tick(CallbackInfo ci) {
        if(Optimization.noParticles()) { clearParticles(); ci.cancel(); }
    }
}
