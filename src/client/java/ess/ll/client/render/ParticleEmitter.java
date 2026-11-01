package ess.ll.client.render;

public abstract class ParticleEmitter {
    private boolean removed;

    protected abstract void emit(ClientParticleManager clientParticleManager);

    public final boolean isAlive() {
        return !this.removed;
    }

    public final void tick(ClientParticleManager clientParticleManager) {
        if (!this.removed && clientParticleManager != null) {
            try {
                this.emit(clientParticleManager);
            } catch (Throwable throwable) {
                this.remove();
            }
        }
    }

    public final void remove() {
        this.removed = true;
    }
}
