package ess.ll.client;

import ess.ll.client.render.Avatar;
import ess.ll.client.render.GlassRenderer;
import ess.ll.client.render.HudRenderer;
import ess.ll.client.render.Icons;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.RenderDemoHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.screen.ClickGuiScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import ess.ll.client.command.*;
import ess.ll.client.module.ModuleRuntime;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public class EssBestClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ess.ll.client.render.UiBatch.initialize();
        MsdfFont.initialize();
        GlassRenderer.initialize();
        Icons.initialize();
        Avatar.initialize();
        ess.ll.client.render.MenuScene.initialize();
        ess.ll.client.module.render.ShaderSky.initialize();
        ess.ll.client.module.render.CustomCrystalEffects.initialize();
        ess.ll.client.module.render.HitGhost.initialize();
        ess.ll.client.module.render.Chams.initialize();
        ess.ll.client.module.render.MotionBlur.initialize();
        HudRenderer.initialize();
        ess.ll.client.module.render.DeathEffect.initialize();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "render_demo"), ess.ll.client.dev.HudProfile.wrap("render_demo", RenderDemoHud::render));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "module_hud"), ess.ll.client.dev.HudProfile.wrap("module_hud", HudRenderer::render));
        ess.ll.client.module.ClientModules.all();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "combat_hud"), ess.ll.client.dev.HudProfile.wrap("combat_hud", ess.ll.client.module.CombatHooks::renderHud));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "hit_bubbles"), ess.ll.client.dev.HudProfile.wrap("hit_bubbles", ess.ll.client.module.render.HitBubbles::renderHud));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "arrows"), ess.ll.client.dev.HudProfile.wrap("arrows", ess.ll.client.module.render.Arrows::render));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "crystal_pos"), ess.ll.client.dev.HudProfile.wrap("crystal_pos", ess.ll.client.module.render.CrystalPosESP::renderHud));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "waypoint_label"), ess.ll.client.dev.HudProfile.wrap("waypoint_label", ess.ll.client.render.WaypointMarker::renderHud));
        ess.ll.client.render.WaypointMarker.init();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "hp_alert"), ess.ll.client.dev.HudProfile.wrap("hp_alert", ess.ll.client.module.render.HpAlert::render));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ess-best", "shulker_preview"), ess.ll.client.dev.HudProfile.wrap("shulker_preview", ess.ll.client.module.misc.ShulkerPreview::renderHud));
        HudElementRegistry.replaceElement(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CROSSHAIR, vanilla -> (g, delta) -> {
            if(!ess.ll.client.module.render.Crosshair.render(g, delta)) vanilla.extractRenderState(g, delta);
        });
        ClientSendMessageEvents.ALLOW_CHAT.register(DotCommands::allowChat);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ClientData.initialize());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModuleRuntime.restore(client);
            try { ClientData.flush(); } catch(java.io.IOException e) { System.err.println("[ESS] Save failed: "+e.getMessage()); }
        });
        KeyMapping guiKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ess-best.gui",InputConstants.KEY_RSHIFT,KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Keys.tick(client);
            ModuleRuntime.tick(client);
            ess.ll.client.module.render.Hands.tick();
            ess.ll.client.module.misc.AimingItems.tick();
            ess.ll.client.module.misc.PvpSafe.tick();
            ess.ll.client.module.misc.Optimization.tick();
            ess.ll.client.module.misc.NoDelay.tick();
            ess.ll.client.module.misc.ServerMenu.tick(client);
            ess.ll.client.module.misc.GriefJoiner.tick(client);
            ess.ll.client.module.misc.BetterChest.tick(client);
            ess.ll.client.features.FakePlayers.tick();
            ess.ll.client.module.misc.AutoSorter.tick(client);
            ess.ll.client.module.misc.AutoRwTikva.tick(client);
            ess.ll.client.telegram.TelegramBot.tick(client);
            ess.ll.client.module.player.AutoBrewing.tick(client);
            ess.ll.client.module.misc.EmotesModule.tick(client);
            ess.ll.client.wardrobe.WardrobeHeld.tick(client);
            ess.ll.client.wardrobe.WardrobeWorld.tick(client);
            ess.ll.client.bots.Bots.tick(client);
            ess.ll.client.module.render.JumpCircles.tick(client);
            ess.ll.client.module.render.Trails.tick(client);
            ess.ll.client.module.render.HitBubbles.tick(client);
            ess.ll.client.module.render.HitGhost.tick(client);
            ess.ll.client.module.render.Chams.tick();
            ess.ll.client.module.render.CrystalPosESP.tick(client);
            ess.ll.client.module.CombatHooks.tick();
            ess.ll.client.render.WorldFx.tick(client);
            ess.ll.client.module.render.WorldTweaks.tick(client);
            ess.ll.client.module.render.DeathEffect.tick(client);
            ClientData.tick();
            ess.ll.client.dev.AutoShot.tick(client);
            while(guiKey.consumeClick()) {
                if(client.level!=null && client.gui.screen()==null) client.gui.setScreen(new ClickGuiScreen());
            }
        });
    }
}
