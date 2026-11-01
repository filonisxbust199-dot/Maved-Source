package ess.ll.client.features.combat;

import ess.ll.client.util.ElapsedTimer;

public class AuraAttackTiming {
    private long cooldownMillis = 0L;
    private final ElapsedTimer timer = new ElapsedTimer();

    public long getCooldownMillis() {
        return this.cooldownMillis;
    }

    public void startCooldown(long cooldownMillis2) {
        this.timer.reset();
        this.cooldownMillis = cooldownMillis2;
    }

    public ElapsedTimer getTimer() {
        return this.timer;
    }

    public boolean hasElapsed(long var1) {
        return this.timer.hasElapsed(var1);
    }

    public boolean isReady() {
        return this.hasElapsed(this.cooldownMillis);
    }
}
