package ess.ll.client.dev;

import ess.ll.client.module.ClientModules;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.setting.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

final class PortedModulesCheck {
    private static final List<Runnable> restore = new ArrayList<>();
    private static int start = -1;
    private static Vec3 playerPos, cameraPos;
    private static final FrameCounter frameCounter = new FrameCounter();
    private static int cameraStartFrame, cameraWaitTicks;

    private static final class FrameCounter {
        int frames;

        @ess.ll.client.event.Subscribe
        public void onFrame(ess.ll.client.event.FrameUpdateEvent event) {
            frames++;
        }
    }

    static void tick(Minecraft mc, int tick) {
        if (tick == 5) {
            for (String name : List.of("CrystalAura", "Aura", "Sprint", "AutoJump", "AirStuck", "FreeCam", "AntiBot", "ClickPearl")) {
                var module = ClientModules.find(name);
                boolean before = module.enabled();
                restore.add(() -> module.setEnabled(before));
                module.setEnabled(false);
            }
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer();
                for (String command : List.of("gamemode survival @p", "item replace entity @p hotbar.5 with minecraft:ender_pearl 16")) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                }
            });
        }
        if (start < 0 && tick >= 25 && InventoryUtils.countItems(Items.ENDER_PEARL) == 16)
            start = tick;
        if (start < 0) {
            if (tick > 400)
                throw new IllegalStateException("Pearl inventory did not synchronize");
            return;
        }
        int stage = tick - start;
        if (stage == 1) {
            ClientModules.find("AirStuck").setEnabled(true);
            playerPos = mc.player.position();
            mc.player.setDeltaMovement(.2, .2, .2);
        }
        if (stage == 6) {
            check(mc.player.position().distanceTo(playerPos) < .01, "AirStuck movement freeze");
            ClientModules.find("AirStuck").setEnabled(false);
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (stage == 10) {
            ess.ll.client.event.EventBus.register(frameCounter);
            cameraStartFrame = frameCounter.frames;
            ClientModules.find("FreeCam").setEnabled(true);
            playerPos = mc.player.position();
            cameraPos = mc.gameRenderer.mainCamera().position();
            mc.options.keyUp.setDown(true);
        }
        if (stage == 16) {
            // Camera movement runs on rendered frames, which may lag behind game ticks.
            if (frameCounter.frames - cameraStartFrame < 6 && cameraWaitTicks++ < 200) {
                mc.options.keyUp.setDown(true);
                start++;
                return;
            }
            ess.ll.client.event.EventBus.unregister(frameCounter);
            System.out.println("[ESS ports] FreeCam frames=" + (frameCounter.frames - cameraStartFrame)
                    + ", cameraDistance=" + mc.gameRenderer.mainCamera().position().distanceTo(cameraPos));
            mc.options.keyUp.setDown(false);
            check(mc.gameRenderer.mainCamera().position().distanceTo(cameraPos) > .3, "FreeCam camera movement");
            check(mc.player.position().distanceTo(playerPos) < .01, "FreeCam player freeze");
            ClientModules.find("FreeCam").setEnabled(false);
        }
        if (stage == 20)
            ClientModules.find("AntiBot").setEnabled(true);
        if (stage == 23) {
            var anti = ModuleManager.INSTANCE.antiBot();
            var fake = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TestBot"));
            check(!anti.isBot(mc.player) && anti.isBot(fake), "AntiBot classification");
            ClientModules.find("AntiBot").setEnabled(false);
            for (var setting : ClientModules.find("ClickPearl").settings) if (setting instanceof Settings.KeybindSetting bind) {
                int previous = bind.get();
                restore.add(() -> bind.set(previous));
                bind.set(1002);
            }
            ClientModules.find("ClickPearl").setEnabled(true);
            mc.player.setXRot(-89);
            ess.ll.client.core.FreeLookController.setPitch(-89);
        }
        if (stage == 25) {
            check(mc.gui.screen() == null, "ClickPearl input reaches gameplay");
            var button = new net.minecraft.client.input.MouseButtonInfo(2, 0);
            mc.mouseHandler.onButton(mc.getWindow().handle(), button, 1);
            mc.mouseHandler.onButton(mc.getWindow().handle(), button, 0);
        }
        if (stage == 65) {
            System.out.println("[ESS ports] remaining pearls=" + InventoryUtils.countItems(Items.ENDER_PEARL));
            check(InventoryUtils.countItems(Items.ENDER_PEARL) == 15, "ClickPearl middle-button quick click");
            ClientModules.find("ClickPearl").setEnabled(false);
            for (Runnable action : restore) action.run();
            System.out.println("[ESS ports] PASS all checks");
            mc.stop();
        }
    }

    private static void check(boolean result, String name) {
        System.out.println("[ESS ports] " + name + ": " + (result ? "PASS" : "FAIL"));
        if (!result)
            throw new IllegalStateException(name);
    }
}
