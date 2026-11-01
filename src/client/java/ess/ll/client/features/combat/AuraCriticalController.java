package ess.ll.client.features.combat;

import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.render.geometry.PoseCriticalHitStrategy;
import net.minecraft.client.player.LocalPlayer;

public final class AuraCriticalController implements MinecraftAccess {
    private final AttackAura aura;
    private final CriticalHitStrategy[] strategies = new CriticalHitStrategy[] { new PoseCriticalHitStrategy(), new JumpCriticalHitStrategy() };
    private boolean jumpSuppressed;

    public void update() {
        for (CriticalHitStrategy criticalHitStrategy : this.strategies) {
            if (criticalHitStrategy.isApplicable(this.aura)) {
                criticalHitStrategy.update(this.aura);
            } else {
                criticalHitStrategy.reset();
            }
        }
    }

    private static boolean canTriggerFlight() {
        LocalPlayer player2 = mc.player;
        return player2 != null && player2.getAbilities().mayfly && player2.jumpTriggerTime > 0;
    }

    public void modifyInput(PlayerInputEvent event) {
        boolean jump = event.isJump();
        for (CriticalHitStrategy criticalHitStrategy : this.strategies) {
            if (criticalHitStrategy.isApplicable(this.aura)) {
                criticalHitStrategy.modifyInput(event);
            }
        }
        if (jump && !event.isJump()) {
            this.jumpSuppressed = true;
        } else if (this.jumpSuppressed && jump && canTriggerFlight()) {
            event.setJump(false);
        } else {
            this.jumpSuppressed = false;
        }
    }

    public int ticksUntilCritical() {
        for (CriticalHitStrategy criticalHitStrategy : this.strategies) {
            if (criticalHitStrategy.isApplicable(this.aura)) {
                int var5 = criticalHitStrategy.ticksUntilCritical(this.aura);
                if (var5 >= 0) {
                    return var5;
                }
            }
        }
        return -1;
    }

    public AuraCriticalController(AttackAura aura2) {
        this.aura = aura2;
    }

    public boolean shouldAllowFallbackAttack() {
        for (CriticalHitStrategy criticalHitStrategy : this.strategies) {
            if (criticalHitStrategy.isApplicable(this.aura) && criticalHitStrategy.shouldAllowFallbackAttack(this.aura)) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        this.jumpSuppressed = false;
        for (CriticalHitStrategy criticalHitStrategy : this.strategies) {
            criticalHitStrategy.reset();
        }
    }
}
