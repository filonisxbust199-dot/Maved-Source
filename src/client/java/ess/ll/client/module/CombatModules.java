package ess.ll.client.module;

import ess.ll.client.render.HudRenderer;
import ess.ll.client.module.combat.CombatRuntime;

/**
 * Registers event-driven modules in the shared ClickGUI and config registry.
 */
public final class CombatModules {
    private CombatModules() {
    }

    static void register() {
        CombatRuntime.initialize();
        CombatHooks.register(CombatRuntime::tick, CombatRuntime::renderHud);
        var combat = ess.ll.client.util.ModuleManager.INSTANCE;
        add("Aura", combat.aura());
        add("NoFriendDamage", combat.noFriendDamage());
        combat.noFriendDamage().setEnabled(true);
        add("WebIgnore", "Player", combat.webIgnore());
        add("AutoTotem", combat.autoTotem());
        add("Criticals", combat.criticals());
        add("AutoExplosion", combat.autoExplosion());
        add("CrystalAura", combat.crystalAura());
        add("CrystalAuraV2", combat.crystalAuraV2());
        add("AntiCrystal", combat.antiCrystal());
        add("PearlTarget", combat.pearlTarget());
        add("ClickPearl", "Player", combat.clickPearl());
        add("AirStuck", "Movement", combat.airStuck());
        add("FreeCam", "Player", combat.freeCam());
        add("AntiBot", combat.antiBot());
        add("WebTrap", combat.webTrap());
        add("NoWeb", "Movement", combat.noWeb());
        add("AutoTool", "Player", combat.autoTool());
        add("SwingAnimation", "Render", combat.swingAnimations());
        add("ViewModel", "Render", combat.viewModel());
        add("Particles", "Render", combat.particles());
        add("Predictions", "Render", combat.predictions());
        add("TargetESP", "Render", combat.targetESP());
        add("EntityESP", "Render", combat.entityESP());
        add("BlockESP", "Render", ess.ll.client.module.render.BlockESP.INSTANCE);
        add("Removals", "Render", combat.removals());
        add("InventoryWalk", "Movement", combat.inventoryMove());
        add("Sprint", "Movement", combat.autoSprint());
        add("ElytraHelper", "Player", combat.elytraHelper());
        add("ElytraMotyaon", "Movement", combat.elytraMotyaon());
        add("ElytraBooster", "Movement", combat.elytraBooster());
        // TargetHud follows the Aura target while Aura is on.
        HudRenderer.targetSource = () -> {
            var target = ess.ll.client.module.combat.AttackAura.currentTarget();
            return combat.aura().isEnabled() && target != null && target.isAlive() && !target.isRemoved() ? target : null;
        };
    }

    private static void add(String name, ess.ll.client.module.ClientModule source) {
        add(name, "Combat", source);
    }

    private static void add(String name, String category, ess.ll.client.module.ClientModule source) {
        ClientModules.register(new ClientModules.Module(name, category, CombatSettings.adapt(source), source::isEnabled, () -> source.setEnabled(!source.isEnabled())));
    }
}
