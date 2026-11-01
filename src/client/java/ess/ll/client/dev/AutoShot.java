package ess.ll.client.dev;

import ess.ll.client.module.ClientModules;
import ess.ll.client.screen.ClickGuiScreen;
import ess.ll.client.screen.MainMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;

/**
 * Development-only visual check, enabled with -Dess.autoshot=true (gradle runAutoshot):
 * joins the quick-play world, captures the HUD, HUD edit mode, ClickGUI and every season, then quits.
 */
public final class AutoShot {
    private static final boolean ENABLED = Boolean.getBoolean("ess.autoshot");
    private static int ticks;
    private static ess.ll.client.render.MenuScene.Style menuBackground;
    private static int crystalCountBefore;
    private static final java.util.Set<Integer> benchCrystals = new java.util.HashSet<>();

    private AutoShot() {
    }

    private static int loadingTicks, loadingShots, botTicks;

    private static ess.ll.client.bots.Bot find(java.util.List<ess.ll.client.bots.Bot> list, String name) {
        for (var b : list) if (b.name.equals(name))
            return b;
        return null;
    }

    private static boolean reentered;
    private static int worldClock;

    /**
     * Dev: F1 (the vanilla HUD hidden flag is private in 26.3).
     */
    private static void hudHidden(Minecraft mc, boolean hidden) {
        try {
            var f = net.minecraft.client.gui.Hud.class.getDeclaredField("isHidden");
            f.setAccessible(true);
            f.setBoolean(mc.gui.hud, hidden);
        } catch (ReflectiveOperationException e) {
            System.out.println("[ESS hudperf] " + e);
        }
    }

    public static void tick(Minecraft mc) {
        // World loading screens: frames while the quick-play world is being prepared and loaded.
        if (ENABLED && Boolean.getBoolean("ess.autoshot.worldloadtest")) {
            var current = mc.gui.screen();
            // The first entry happens under the startup loading screen, so the world is left and entered again.
            boolean loading = current instanceof net.minecraft.client.gui.screens.LevelLoadingScreen || current instanceof net.minecraft.client.gui.screens.GenericMessageScreen;
            if (loading && reentered && mc.gui.overlay() == null && ++loadingTicks % 5 == 0 && loadingShots < 6)
                shot(mc, "ess-worldload-" + (++loadingShots) + ".png");
            if (mc.player != null) {
                ticks++;
                if (!reentered && ticks == 20)
                    mc.disconnectWithSavingScreen();
                if (reentered && ticks == 15)
                    mc.stop();
            } else if (ticks >= 20 && !reentered && mc.level == null) {
                // Leaving keeps the saving screen up until the caller picks the next one, as the pause menu does.
                reentered = true;
                ticks = 0;
                mc.createWorldOpenFlows().openWorld("New World", () -> {
                });
            }
            return;
        }
        if (!ENABLED || mc.player == null || mc.level == null)
            return;
        // Bots: two offline bots join (by default this world opened to LAN), get played one after another, then leave.
        if (Boolean.getBoolean("ess.autoshot.bottest")) {
            int t = ++botTicks;
            String server = System.getProperty("ess.autoshot.botserver", "");
            if (t == 20 && server.isEmpty() && mc.getSingleplayerServer() != null) {
                mc.getSingleplayerServer().publishServer(net.minecraft.server.MinecraftServer.MultiplayerScope.LAN, false, 25599);
                mc.getSingleplayerServer().setUsesAuthentication(false);
            }
            String address = server.isEmpty() ? "localhost:25599" : server;
            if (t == 40) {
                ess.ll.client.bots.Bots.add("ess_botA", address);
                ess.ll.client.bots.Bots.add("ess_botB", address);
            }
            var bots = ess.ll.client.bots.Bots.all();
            if (t % 40 == 0)
                for (var b : bots) {
                    System.out.println("[ESS autoshot] bot " + b.name + " " + b.state() + " " + b.reason() + " fg " + b.foreground() + " pos " + b.position() + " " + b.dimension() + " hp " + b.health());
                    var log = b.log();
                    for (String line : log.subList(Math.max(0, log.size() - 4), log.size())) System.out.println("[ESS autoshot]   | " + line);
                }
            ess.ll.client.bots.Bot a = find(bots, "ess_botA"), b = find(bots, "ess_botB");
            // Bot A goes far from the origin, where the chunk cache centre matters for the replay.
            if (t == 100 && mc.getSingleplayerServer() != null) {
                var host = mc.getSingleplayerServer();
                host.execute(() -> {
                    host.getCommands().performPrefixedCommand(host.createCommandSourceStack(), "gamemode creative ess_botA");
                    host.getCommands().performPrefixedCommand(host.createCommandSourceStack(), "tp ess_botA 900 120 -700");
                });
            }
            // A walks one chunk on after its chunks came in, so a newer chunk cache centre follows them.
            if (t == 170 && mc.getSingleplayerServer() != null) {
                var host = mc.getSingleplayerServer();
                host.execute(() -> host.getCommands().performPrefixedCommand(host.createCommandSourceStack(), "tp ess_botA 918 120 -700"));
            }
            if (t == 180) {
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Bots");
            }
            if (t == 205)
                shot(mc, "ess-bots-gui.png");
            if (t == 212)
                mc.gui.setScreen(null);
            if (t == 190 && b != null) {
                b.chat(".modules");
                b.chat("#help");
                b.chat("hello");
                for (String line : b.log()) System.out.println("[ESS autoshot] B| " + line);
            }
            if (t == 200 && b != null) {
                ess.ll.client.bots.Bots.dumpView(b, "screenshots/ess-bot-view.png");
                b.jump();
            }
            if (t == 220 && a != null) {
                System.out.println("[ESS autoshot] switch to A: " + ess.ll.client.bots.Bots.canSwitch(a));
                ess.ll.client.bots.Bots.switchTo(a);
            }
            if (t == 320) {
                System.out.println("[ESS autoshot] chunks " + (mc.level == null ? 0 : mc.level.getChunkSource().getLoadedChunksCount()) + " here " + (mc.level != null && mc.player != null && mc.level.hasChunk(mc.player.getBlockX() >> 4, mc.player.getBlockZ() >> 4)) + " screen " + mc.gui.screen());
                System.out.println("[ESS autoshot] playing as " + mc.getUser().getName() + " player " + (mc.player == null ? null : mc.player.getName().getString() + " " + mc.player.blockPosition()) + " screen " + mc.gui.screen());
                shot(mc, "ess-bot-as-a.png");
                if (a != null)
                    for (String line : a.log()) System.out.println("[ESS autoshot] A| " + line);
            }
            if (t == 340 && b != null) {
                System.out.println("[ESS autoshot] switch to B: " + ess.ll.client.bots.Bots.canSwitch(b));
                ess.ll.client.bots.Bots.switchTo(b);
            }
            if (t == 430 && b != null) {
                b.chat("#goto 0 100 0");
                b.chat("#stop");
                for (String line : b.log().subList(Math.max(0, b.log().size() - 4), b.log().size())) System.out.println("[ESS autoshot] B| " + line);
            }
            if (t == 440) {
                System.out.println("[ESS autoshot] playing as " + mc.getUser().getName() + " player " + (mc.player == null ? null : mc.player.getName().getString() + " " + mc.player.blockPosition()));
                shot(mc, "ess-bot-as-b.png");
                if (a != null)
                    ess.ll.client.bots.Bots.dumpView(a, "screenshots/ess-bot-view-a.png");
            }
            if (t == 400) {
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Bots");
            }
            if (t == 425)
                shot(mc, "ess-bots-gui-b.png");
            if (t == 432)
                mc.gui.setScreen(null);
            if (t == 460 && a != null)
                ess.ll.client.bots.Bots.switchTo(a);
            if (t == 540) {
                System.out.println("[ESS autoshot] back as " + mc.getUser().getName() + " player " + (mc.player == null ? null : mc.player.getName().getString()));
                shot(mc, "ess-bot-back-a.png");
            }
            if (t == 560)
                mc.stop();
            return;
        }
        var screen = mc.gui.screen();
        if (screen != null && !Boolean.getBoolean("ess.autoshot.emotetest") && !(screen instanceof ClickGuiScreen) && !(screen instanceof ess.ll.client.telegram.TelegramScreen) && !(screen instanceof ChatScreen) && !(screen instanceof MainMenuScreen) && !(screen instanceof ess.ll.client.screen.PauseMenuScreen) && !(screen instanceof ess.ll.client.screen.ServerMenuScreen) && !(screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen && Boolean.getBoolean("ess.autoshot.grieftest")) && !(screen instanceof ChatScreen && Boolean.getBoolean("ess.autoshot.newtest")) && !(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> && (Boolean.getBoolean("ess.autoshot.chesttest") || Boolean.getBoolean("ess.autoshot.auctest") || Boolean.getBoolean("ess.autoshot.dragtest") || Boolean.getBoolean("ess.autoshot.tgtest") || Boolean.getBoolean("ess.autoshot.scrolltest") || Boolean.getBoolean("ess.autoshot.headtest") || Boolean.getBoolean("ess.autoshot.zenithtest"))))
            return;
        ticks++;
        // Visual modules: Trails while walking, BlockOverlay gliding and breaking, Crosshair styles, ItemPhysics,
        // HitBubbles on a zombie (with a crit), Arrows to two bots standing behind and beside the player.
        if (Boolean.getBoolean("ess.autoshot.visualtest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            if (ticks == 20) {
                for (var m : new ClientModules.Module[] { ClientModules.TRAILS, ClientModules.HIT_BUBBLES, ClientModules.BLOCK_OVERLAY, ClientModules.CROSSHAIR, ClientModules.ITEM_PHYSICS, ClientModules.ARROWS }) m.setEnabled(true);
                run.accept("kill @e[type=!minecraft:player]");
                run.accept("gamemode survival " + me);
                run.accept("effect give " + me + " minecraft:resistance 600 4 true");
                run.accept("time set day");
                // An open stone floor high up, so nothing stands in the way.
                run.accept("tp " + me + " 0.5 201 0.5 0 0");
                run.accept("fill -16 200 -16 16 200 16 minecraft:smooth_stone");
                run.accept("fill -16 201 -16 16 206 16 minecraft:air");
                if (server != null) {
                    server.publishServer(net.minecraft.server.MinecraftServer.MultiplayerScope.LAN, false, 25599);
                    server.setUsesAuthentication(false);
                }
            }
            if (ticks == 30) {
                ess.ll.client.bots.Bots.add("ess_left", "localhost:25599");
                ess.ll.client.bots.Bots.add("ess_behind", "localhost:25599");
            }
            if (ticks == 60 || ticks == 120) {
                run.accept("tp ess_behind 0.5 201 -14");
                run.accept("tp ess_left -12 201 4");
            }
            // Trails: walking in a curve, seen from behind, with each look.
            String[] trails = { "Лента", "Шлейф", "Комета", "Частицы" };
            if (ticks == 40) {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                p.setXRot(25);
            }
            if (ticks >= 40 && ticks < 120) {
                int i = (ticks - 40) / 20;
                if ((ticks - 40) % 20 == 0)
                    ess.ll.client.module.render.Trails.MODE.select(trails[i]);
                mc.options.keyUp.setDown(true);
                mc.options.keySprint.setDown(true);
                p.setYRot(p.getYRot() + 5);
                if ((ticks - 40) % 20 == 16) {
                    shot(mc, "ess-vis-trail-" + i + ".png");
                    System.out.println("[ESS autoshot] trail " + trails[i] + " points " + ess.ll.client.module.render.Trails.points());
                }
            }
            if (ticks == 120) {
                mc.options.keyUp.setDown(false);
                mc.options.keySprint.setDown(false);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            // BlockOverlay: a stair and a full block next to each other, glide from one to the other, then breaking.
            if (ticks == 122) {
                run.accept("tp " + me + " 0.5 201 0.5 0 0");
                run.accept("setblock 1 201 2 minecraft:oak_stairs");
                run.accept("setblock -1 201 2 minecraft:stone");
            }
            if (ticks == 130)
                lookAt(p, 1.5, 201.4, 2.5);
            if (ticks == 140) {
                shot(mc, "ess-vis-outline-stair.png");
                System.out.println("[ESS autoshot] outline " + ess.ll.client.module.render.BlockOverlay.visibility() + " hit " + mc.hitResult);
            }
            if (ticks == 141)
                lookAt(p, -.5, 201.5, 2.5);
            if (ticks == 142)
                shot(mc, "ess-vis-outline-glide.png");
            if (ticks == 150)
                shot(mc, "ess-vis-outline-stone.png");
            if (ticks == 151)
                ess.ll.client.module.render.BlockOverlay.GLOW.toggle();
            if (ticks == 154) {
                shot(mc, "ess-vis-outline-noglow.png");
                ess.ll.client.module.render.BlockOverlay.GLOW.toggle();
            }
            mc.options.keyAttack.setDown(ticks >= 158 && ticks < 175);
            if (ticks == 172)
                shot(mc, "ess-vis-outline-break.png");
            // Crosshair: every style, then spread in a jump.
            String[] styles = { "Крест", "Точка", "Круг", "Уголки" };
            for (int i = 0; i < styles.length; i++) {
                if (ticks == 180 + i * 6) {
                    ess.ll.client.module.render.Crosshair.STYLE.select(styles[i]);
                    p.setYRot(180);
                    p.setXRot(-10);
                }
                if (ticks == 184 + i * 6)
                    shot(mc, "ess-vis-cross-" + i + ".png");
            }
            if (ticks == 205) {
                ess.ll.client.module.render.Crosshair.STYLE.select("Крест");
                p.jumpFromGround();
            }
            if (ticks == 209)
                shot(mc, "ess-vis-cross-jump.png");
            // ItemPhysics: items thrown up in front, tumbling and then lying.
            if (ticks == 215) {
                lookAt(p, .5, 201, -2.5);
                String[] items = { "diamond_sword", "diamond", "oak_planks", "golden_apple", "stone" };
                for (int i = 0; i < items.length; i++) run.accept("summon item " + (-.5 + i * .5) + " 202.5 -2.5 {Item:{id:\"minecraft:" + items[i] + "\",count:1},PickupDelay:400,Motion:[0.0,0.25,0.0]}");
            }
            if (ticks == 222)
                shot(mc, "ess-vis-items-air.png");
            if (ticks == 260)
                shot(mc, "ess-vis-items-ground.png");
            // HitBubbles: a still zombie in front; a plain hit, then a critical one from a jump.
            if (ticks == 265) {
                run.accept("kill @e[type=minecraft:item]");
                run.accept("summon husk 0.5 201 -3 {NoAI:1b,Health:200f,Attributes:[{id:\"minecraft:max_health\",base:200}]}");
                lookAt(p, .5, 202.2, -3);
            }
            if (ticks == 275 || ticks == 296) {
                var target = mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, p.getBoundingBox().inflate(6)).stream().findFirst().orElse(null);
                System.out.println("[ESS autoshot] zombie " + target + " crosshair " + mc.crosshairPickEntity + " fall " + p.fallDistance + " ground " + p.onGround());
                if (target != null) {
                    mc.gameMode.attack(p, target);
                }
            }
            if (ticks == 277)
                shot(mc, "ess-vis-hit.png");
            if (ticks == 282) {
                shot(mc, "ess-vis-hit-number.png");
                System.out.println("[ESS autoshot] hit fx " + ess.ll.client.module.render.HitBubbles.alive());
            }
            if (ticks == 289)
                p.jumpFromGround();
            if (ticks == 298)
                shot(mc, "ess-vis-crit.png");
            if (ticks == 303)
                shot(mc, "ess-vis-crit-number.png");
            // Arrows: one bot behind, one to the left; the second shot shows the one in view too.
            if (ticks == 310) {
                run.accept("kill @e[type=minecraft:husk]");
                run.accept("tp ess_behind 0.5 201 -14");
                run.accept("tp ess_left -12 201 4");
                run.accept("tp " + me + " 0.5 201 0.5 0 0");
            }
            if (ticks == 325) {
                shot(mc, "ess-vis-arrows.png");
                System.out.println("[ESS autoshot] arrows " + ess.ll.client.module.render.Arrows.shown() + " players " + mc.level.players().size());
            }
            if (ticks == 326)
                p.setYRot(150);
            if (ticks == 340)
                shot(mc, "ess-vis-arrows-turned.png");
            if (ticks == 345) {
                for (var b : List.copyOf(ess.ll.client.bots.Bots.all())) if (!b.main)
                    ess.ll.client.bots.Bots.remove(b);
                for (var m : new ClientModules.Module[] { ClientModules.TRAILS, ClientModules.HIT_BUBBLES, ClientModules.BLOCK_OVERLAY, ClientModules.CROSSHAIR, ClientModules.ITEM_PHYSICS, ClientModules.ARROWS }) m.setEnabled(false);
            }
            if (ticks == 350)
                mc.stop();
            return;
        }
        // ServerMenu: every tab of every server, with a bind waiting for a key.
        if (Boolean.getBoolean("ess.autoshot.servermenutest")) {
            var sm = ess.ll.client.module.misc.ServerMenu.class;
            if (ticks == 20) {
                ClientModules.SERVER_MENU.setEnabled(true);
                mc.player.getInventory().setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DRIED_KELP, 12));
                mc.gui.setScreen(new ess.ll.client.screen.ServerMenuScreen(null));
            }
            String[][] steps = { { "FunTime", "items" }, { "FunTime", "potions" }, { "FunTime", "other" }, { "HolyWorld", "items" }, { "ReallyWorld", "items" } };
            for (int i = 0; i < steps.length; i++) {
                int at = 30 + i * 20;
                if (ticks == at && mc.gui.screen() instanceof ess.ll.client.screen.ServerMenuScreen menu)
                    menu.showForTest(steps[i][0], steps[i][1]);
                if (ticks == at + 3)
                    shot(mc, "ess-sm-" + i + "-early.png");
                if (ticks == at + 14)
                    shot(mc, "ess-sm-" + i + ".png");
            }
            if (ticks == 140) {
                ess.ll.client.module.misc.ServerMenu.use(ess.ll.client.module.misc.ServerMenu.ENTRIES.get(3));
                ess.ll.client.module.misc.ServerMenu.use(ess.ll.client.module.misc.ServerMenu.ENTRIES.get(0));
                mc.gui.setScreen(new ChatScreen("", false));
            }
            if (ticks == 150)
                shot(mc, "ess-sm-chat.png");
            if (ticks == 155) {
                mc.gui.setScreen(null);
                ClientModules.SERVER_MENU.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // Predictions: the path and landing of a held pearl, then of a thrown one, and the ClickGUI logo.
        if (Boolean.getBoolean("ess.autoshot.predtest")) {
            if (ticks == 20) {
                for (var f : net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.class.getFields()) System.out.println("[ESS autoshot] stage " + f.getName() + " " + f.getGenericType().getTypeName());
                ClientModules.find("Predictions").setEnabled(true);
                mc.player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL, 16));
                mc.player.getInventory().setSelectedSlot(0);
                mc.player.setYRot(20);
                mc.player.setXRot(18);
            }
            if (ticks == 40)
                shot(mc, "ess-pred-aim.png");
            if (ticks == 41) {
                mc.player.setXRot(-12);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
            if (ticks == 44) {
                shot(mc, "ess-pred-arc.png");
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            if (ticks == 45) {
                mc.player.setXRot(-20);
            }
            if (ticks == 46)
                mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            if (ticks == 47) {
                mc.player.getInventory().setSelectedSlot(1);
                mc.player.setYRot(-40);
                mc.player.setXRot(-5);
            }
            if (ticks == 51)
                shot(mc, "ess-pred-flying.png");
            if (ticks == 70) {
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Combat");
            }
            if (ticks == 85)
                shot(mc, "ess-pred-gui.png");
            if (ticks == 90) {
                mc.gui.setScreen(null);
                ClientModules.find("Predictions").setEnabled(false);
                mc.stop();
            }
            return;
        }
        // NoDelay: the jump delay right after a jump, with the module off and on.
        if (Boolean.getBoolean("ess.autoshot.nodelaytest")) {
            var living = (ess.ll.client.mixin.LivingEntityMixin_1248) mc.player;
            mc.options.keyJump.setDown(ticks >= 20 && ticks < 23 || ticks >= 60 && ticks < 63);
            if (ticks == 22 || ticks == 62)
                System.out.println("[ESS autoshot] NoDelay " + ClientModules.NO_DELAY.enabled() + " jump delay " + living.getJumpDelay() + " ground " + mc.player.onGround());
            if (ticks == 40)
                ClientModules.NO_DELAY.setEnabled(true);
            if (ticks == 70) {
                ClientModules.NO_DELAY.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // JumpCircles: the player jumps and lands with each shape; the command help is shown in chat.
        if (Boolean.getBoolean("ess.autoshot.jumptest")) {
            var jc = ess.ll.client.module.render.JumpCircles.class;
            if (ticks == 20) {
                ClientModules.JUMP_CIRCLES.setEnabled(true);
                mc.player.setXRot(55);
            }
            String[] shapes = { "Кольцо", "Волны", "Звезда", "Шестигранник", "Цветок", "Сердце" };
            for (int i = 0; i < shapes.length; i++) {
                int at = 30 + i * 40;
                if (ticks == at) {
                    ess.ll.client.module.render.JumpCircles.SHAPE.select(shapes[i]);
                    mc.player.jumpFromGround();
                }
                if (ticks == at + 5) {
                    shot(mc, "ess-jump-" + i + "-up.png");
                    System.out.println("[ESS autoshot] jump " + i + " ground " + mc.player.onGround() + " y " + mc.player.getY() + " circles " + ess.ll.client.module.render.JumpCircles.count() + " on " + ClientModules.JUMP_CIRCLES.enabled());
                }
                if (ticks == at + 16)
                    shot(mc, "ess-jump-" + i + "-land.png");
            }
            if (ticks == 280) {
                ess.ll.client.command.DotCommands.execute(".help");
                ess.ll.client.command.DotCommands.execute(".help configs");
                ess.ll.client.command.DotCommands.execute(".cfg list");
                ess.ll.client.command.DotCommands.execute(".toggle");
                mc.gui.setScreen(new ChatScreen("", false));
            }
            if (ticks == 290)
                shot(mc, "ess-help.png");
            if (ticks == 300) {
                mc.gui.setScreen(null);
                ClientModules.JUMP_CIRCLES.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // Optimization: particles before and after, then the island tips shown one after another.
        if (Boolean.getBoolean("ess.autoshot.opttest")) {
            var particles = mc.particleEngine;
            if (ticks == 20) {
                ClientModules.find("Seasons").setEnabled(true);
                mc.options.renderDistance().set(16);
            }
            if (ticks >= 20 && ticks <= 200 && ticks % 2 == 0) {
                var p = mc.player.position();
                for (int i = 0; i < 40; i++) mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, p.x + Math.sin(i) * 2, p.y + 1 + i * .05, p.z + 3 + Math.cos(i) * 2, 0, .02, 0);
            }
            if (ticks == 40) {
                System.out.println("[ESS autoshot] particles before: " + particles.countParticles());
                shot(mc, "ess-opt-before.png");
            }
            if (ticks == 45)
                ClientModules.OPTIMIZATION.setEnabled(true);
            if (ticks == 70) {
                System.out.println("[ESS autoshot] particles after: " + particles.countParticles());
                shot(mc, "ess-opt-on.png");
            }
            if (ticks == 125)
                shot(mc, "ess-opt-tip1.png");
            if (ticks == 180)
                shot(mc, "ess-opt-tip2.png");
            if (ticks == 190) {
                ClientModules.OPTIMIZATION.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // AutoRwTikva: this world opened to LAN with test /homes, /home, /tpa and /tpaccept, three homes with a player
        // head each (and one home that does not match the prefix), a bot as the second account; the cycle runs to the end.
        if (Boolean.getBoolean("ess.autoshot.tikvatest")) {
            var server = mc.getSingleplayerServer();
            boolean botTeleports = Boolean.getBoolean("ess.autoshot.tikvabot");
            if (ticks == 20 && server != null) {
                server.publishServer(net.minecraft.server.MinecraftServer.MultiplayerScope.LAN, false, 25599);
                server.setUsesAuthentication(false);
                server.execute(() -> {
                    var run = (java.util.function.Consumer<String>) c -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    for (int i = 1; i <= 3; i++) TIKVA_HOMES.put("t" + i, new net.minecraft.core.BlockPos(40 * i, 200, 60));
                    TIKVA_HOMES.put("base", new net.minecraft.core.BlockPos(0, 200, 100));
                    for (var e : TIKVA_HOMES.entrySet()) {
                        var h = e.getValue();
                        run.accept("fill " + (h.getX() - 4) + " 199 " + (h.getZ() - 4) + " " + (h.getX() + 4) + " 199 " + (h.getZ() + 4) + " minecraft:stone");
                        run.accept("fill " + (h.getX() - 4) + " 200 " + (h.getZ() - 4) + " " + (h.getX() + 4) + " 204 " + (h.getZ() + 4) + " minecraft:air");
                        if (!e.getKey().equals("base"))
                            run.accept("setblock " + (h.getX() + 1) + " 200 " + (h.getZ() + 2) + " minecraft:player_head");
                    }
                    run.accept("gamemode survival @a");
                    var d = server.getCommands().getDispatcher();
                    d.register(net.minecraft.commands.Commands.literal("homes").executes(c -> {
                        c.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal("Ваши дома (4): t3, t1, base, t2"));
                        return 1;
                    }));
                    d.register(net.minecraft.commands.Commands.literal("home").then(net.minecraft.commands.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> {
                        var h = TIKVA_HOMES.get(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "name"));
                        if (h == null)
                            return 0;
                        run.accept("tp " + c.getSource().getTextName() + " " + (h.getX() + .5) + " " + h.getY() + " " + (h.getZ() + .5));
                        return 1;
                    })));
                    d.register(net.minecraft.commands.Commands.literal("tpa").then(net.minecraft.commands.Commands.argument("target", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> {
                        String target = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "target");
                        TIKVA_TPA.put(target, c.getSource().getTextName());
                        var to = server.getPlayerList().getPlayerByName(target);
                        if (to != null)
                            to.sendSystemMessage(net.minecraft.network.chat.Component.literal(c.getSource().getTextName() + " просит телепортироваться к вам"));
                        System.out.println("[ESS tikva] server: /tpa " + target + " from " + c.getSource().getTextName());
                        return 1;
                    })));
                    d.register(net.minecraft.commands.Commands.literal("tpaccept").executes(c -> {
                        String who = TIKVA_TPA.remove(c.getSource().getTextName());
                        if (who == null)
                            return 0;
                        run.accept("tp " + who + " " + c.getSource().getTextName());
                        System.out.println("[ESS tikva] server: " + who + " -> " + c.getSource().getTextName());
                        return 1;
                    }));
                    for (var pl : server.getPlayerList().getPlayers()) server.getCommands().sendCommands(pl);
                });
            }
            if (ticks == 40)
                ess.ll.client.bots.Bots.add("ess_tikva", "localhost:25599");
            if (ticks == 120) {
                if (botTeleports)
                    ess.ll.client.module.misc.AutoRwTikva.ROLE.select("Второй аккаунт");
                else
                    ess.ll.client.module.misc.AutoRwTikva.ROLE.select("Основной аккаунт");
                ess.ll.client.module.misc.AutoRwTikva.COUNT.set(0);
                ess.ll.client.module.misc.AutoRwTikva.TPA_DELAY.set(3);
                ess.ll.client.command.DotCommands.execute(".tikva home t");
                ClientModules.AUTO_RW_TIKVA.setEnabled(true);
            }
            if (ticks > 120 && ticks % 20 == 0 && server != null) {
                StringBuilder heads = new StringBuilder();
                for (int i = 1; i <= 3; i++) {
                    var h = TIKVA_HOMES.get("t" + i);
                    heads.append("t").append(i).append(server.overworld().getBlockState(h.offset(1, 0, 2)).is(net.minecraft.world.level.block.Blocks.PLAYER_HEAD) ? "=head " : "=broken ");
                }
                String bot = "";
                for (var b : ess.ll.client.bots.Bots.all()) bot += b.name + (b.foreground() ? "*" : "") + "@" + b.position() + " ";
                System.out.println("[ESS tikva] t=" + ticks + " " + ess.ll.client.module.misc.AutoRwTikva.stateForTest() + " | " + heads + "| " + bot);
            }
            if (ticks == 300)
                shot(mc, "ess-tikva.png");
            if (ticks > 130 && !ClientModules.AUTO_RW_TIKVA.enabled() && tikvaEnd < 0)
                tikvaEnd = ticks;
            if (tikvaEnd > 0 && ticks == tikvaEnd + 10 || ticks == 2400) {
                System.out.println("[ESS tikva] end at " + ticks);
                ClientModules.AUTO_RW_TIKVA.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // CrystalAura dig -> obsidian -> crystal: a fake player in a 1x1 stone pit two deep, no obsidian anywhere,
        // always at the same spot. Prints every block change around the pit, every new crystal and every pop.
        if (Boolean.getBoolean("ess.autoshot.obbytest")) {
            var server = mc.getSingleplayerServer();
            var p = mc.player;
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            if (ticks == 14)
                run.accept("tp @a 8.5 200 8.5 0 30");
            if (ticks == 20) {
                int x = 8, z = 8;
                obbyBase = new net.minecraft.core.BlockPos(x, 200, z + 3);
                // A blast-proof floor where crystals cannot go, a stone pit around the target, and a pickaxe that is not in hand.
                for (String c : new String[] { "gamemode survival @a", "difficulty normal", "time set noon", "clear @a", "kill @e[type=!minecraft:player]", "effect give @a instant_health 1 10 true", "effect give @a resistance 9999 4 true", "effect give @a haste 9999 1 true", "fill " + (x - 6) + " 199 " + (z - 4) + " " + (x + 6) + " 199 " + (z + 9) + " minecraft:crying_obsidian", "fill " + (x - 6) + " 200 " + (z - 4) + " " + (x + 6) + " 206 " + (z + 9) + " minecraft:air", "fill " + (x - 2) + " 199 " + (z + 1) + " " + (x + 2) + " 201 " + (z + 5) + " minecraft:stone", "fill " + x + " 200 " + (z + 3) + " " + x + " 201 " + (z + 3) + " minecraft:air", "item replace entity @a hotbar.0 with minecraft:diamond_pickaxe[enchantments={efficiency:5}]", "item replace entity @a hotbar.1 with minecraft:end_crystal 64", "item replace entity @a hotbar.2 with minecraft:obsidian 64", "tp @a " + x + ".5 200 " + z + ".5 0 30" }) run.accept(c);
            }
            if (ticks == 29 && Boolean.getBoolean("ess.autoshot.obbyweb"))
                run.accept("item replace entity @a hotbar.3 with minecraft:cobweb 64");
            if (ticks == 30) {
                p.setYRot(0);
                p.setXRot(30);
                p.getInventory().setSelectedSlot(1);
                // The fake goes into the pit: spawned where the player stands, so the player steps there first and back.
                double px = p.getX(), py = p.getY(), pz = p.getZ();
                p.setPos(obbyBase.getX() + .5, obbyBase.getY(), obbyBase.getZ() + .5);
                ess.ll.client.features.FakePlayers.add("Pit");
                p.setPos(px, py, pz);
            }
            if (ticks == 32) {
                var crystal = ClientModules.find(Boolean.getBoolean("ess.autoshot.obbyv2") ? "CrystalAuraV2" : "CrystalAura");
                for (var setting : crystal.settings) if (setting instanceof ess.ll.client.setting.Settings.BooleanSetting toggle && (setting.name().equals("Ставить обсидиан") || setting.name().equals("Авто подкоп") || setting.name().equals("Копать")) && !toggle.get())
                    toggle.toggle();
                if (Boolean.getBoolean("ess.autoshot.obbyweb"))
                    ClientModules.find("WebTrap").setEnabled(true);
                crystal.setEnabled(true);
                obbyStart = ticks;
            }
            // The player stays put: knockback from the blasts would carry them out of the arena.
            if (ticks > 32 && obbyBase != null) {
                p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                p.setPos(obbyBase.getX() + .5, obbyBase.getY(), obbyBase.getZ() - 2.5);
            }
            if (ticks > 32 && ticks <= 32 + 160 && obbyBase != null) {
                for (var pos : net.minecraft.core.BlockPos.betweenClosed(obbyBase.offset(-2, -1, -2), obbyBase.offset(2, 2, 2))) {
                    var state = mc.level.getBlockState(pos).getBlock();
                    var key = pos.immutable();
                    var before = obbyBlocks.put(key, state);
                    if (before != null && before != state)
                        System.out.println("[ESS obby] t=" + (ticks - obbyStart) + " " + key.subtract(obbyBase).toShortString() + " " + before.getName().getString() + " -> " + state.getName().getString());
                }
                for (var c : mc.level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, p.getBoundingBox().inflate(8))) if (obbyCrystals.add(c.getId()))
                    System.out.println("[ESS obby] t=" + (ticks - obbyStart) + " crystal " + c.blockPosition().subtract(obbyBase).toShortString());
                String state = ess.ll.client.features.FakePlayers.stateForTest();
                if (!state.equals(obbyState)) {
                    System.out.println("[ESS obby] t=" + (ticks - obbyStart) + " fake " + state);
                    obbyState = state;
                }
            }
            if (ticks == 32 + 60)
                shot(mc, "ess-obby.png");
            if (ticks == 32 + 161) {
                System.out.println("[ESS obby] result " + ess.ll.client.features.FakePlayers.stateForTest() + " crystals=" + obbyCrystals.size());
                ClientModules.find("CrystalAura").setEnabled(false);
                ClientModules.find("CrystalAuraV2").setEnabled(false);
                ClientModules.find("WebTrap").setEnabled(false);
                ess.ll.client.features.FakePlayers.remove("all");
                mc.stop();
            }
            return;
        }
        // Chat hints for dot commands: printed for a few inputs, then the popup with ".fakeplayer " typed.
        if (Boolean.getBoolean("ess.autoshot.hinttest")) {
            if (ticks == 30) {
                ess.ll.client.features.FakePlayers.add("Dummy");
                for (String text : new String[] { ".", ".fa", ".se", ".fakeplayer ", ".fakeplayer del ", ".sort ", ".tg ", ".emote s" }) {
                    var list = ess.ll.client.command.DotSuggestions.complete(text, text.length()).getList().stream().map(x -> x.getText()).toList();
                    System.out.println("[ESS hint] '" + text + "' -> " + (list.size() > 12 ? list.subList(0, 12) + "... (" + list.size() + ")" : list));
                }
            }
            if (ticks == 40)
                mc.gui.setScreen(new ChatScreen("", false));
            // Typed into the box, as a player would: the hints only open on input.
            if ((ticks == 42 || ticks == 58) && mc.gui.screen() instanceof ChatScreen chat)
                try {
                    var field = ChatScreen.class.getDeclaredField("input");
                    field.setAccessible(true);
                    var box = (net.minecraft.client.gui.components.EditBox) field.get(chat);
                    box.setValue("");
                    box.insertText(ticks == 42 ? ".fakeplayer " : ".se");
                } catch (ReflectiveOperationException e) {
                    System.out.println("[ESS hint] " + e);
                }
            if (ticks == 55)
                shot(mc, "ess-hint-fakeplayer.png");
            if (ticks == 70)
                shot(mc, "ess-hint-search.png");
            if (ticks == 75) {
                mc.gui.setScreen(null);
                ess.ll.client.features.FakePlayers.remove("all");
                mc.stop();
            }
            return;
        }
        // .fakeplayer: Aura and TargetESP on a fake player in diamond armour, then a crystal next to it, then removal.
        if (Boolean.getBoolean("ess.autoshot.fptest")) {
            var server = mc.getSingleplayerServer();
            var p = mc.player;
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            if (ticks == 20)
                for (String c : new String[] { "gamemode survival @a", "difficulty normal", "time set day", "clear @a", "effect give @a resistance 9999 4 true", "item replace entity @a hotbar.0 with minecraft:diamond_sword[enchantments={sharpness:5}]", "item replace entity @a armor.head with minecraft:diamond_helmet", "item replace entity @a armor.chest with minecraft:diamond_chestplate", "item replace entity @a armor.legs with minecraft:diamond_leggings", "item replace entity @a armor.feet with minecraft:diamond_boots" }) run.accept(c);
            // A clean stone platform high up, so terrain from other tests is out of the way.
            if (ticks == 22) {
                int x = p.getBlockX(), z = p.getBlockZ();
                run.accept("fill " + (x - 6) + " 199 " + (z - 6) + " " + (x + 6) + " 199 " + (z + 6) + " minecraft:stone");
                run.accept("fill " + (x - 6) + " 200 " + (z - 6) + " " + (x + 6) + " 206 " + (z + 6) + " minecraft:air");
                run.accept("tp @a " + x + ".5 200 " + z + ".5 0 10");
            }
            if (ticks == 30) {
                p.setYRot(0);
                p.setXRot(10);
                ess.ll.client.command.DotCommands.execute(".fakeplayer add Dummy");
            }
            if (ticks == 31)
                run.accept(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 0 15", p.getX(), p.getY(), p.getZ() - 3));
            if (ticks == 40) {
                var f = ess.ll.client.features.FakePlayers.firstForTest();
                System.out.println("[ESS fp] fake=" + f + " byId=" + mc.level.getEntity(f.getId()) + " removed=" + f.isRemoved() + " ticks=" + f.tickCount + " me=" + p.position() + " inRender=" + java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).anyMatch(e -> e == f));
            }
            if (ticks == 40) {
                System.out.println("[ESS fp] spawned " + ess.ll.client.features.FakePlayers.stateForTest());
                shot(mc, "ess-fp-spawn.png");
                ClientModules.find("TargetESP").setEnabled(true);
                ClientModules.find("Aura").setEnabled(true);
            }
            if (ticks > 40 && ticks <= 160 && ticks % 20 == 0)
                System.out.println("[ESS fp] t=" + ticks + " " + ess.ll.client.features.FakePlayers.stateForTest() + " target=" + ess.ll.client.module.combat.AttackAura.currentTarget());
            if (ticks == 100)
                shot(mc, "ess-fp-aura.png");
            // Aura waits for crits: jump now and then, as a player would.
            if (ticks > 40 && ticks < 160 && p.onGround())
                p.jumpFromGround();
            if (ticks == 161) {
                ClientModules.find("Aura").setEnabled(false);
                System.out.println("[ESS fp] before crystal " + ess.ll.client.features.FakePlayers.stateForTest());
            }
            var fake = ess.ll.client.features.FakePlayers.firstForTest();
            if (ticks == 165 && fake != null)
                run.accept(String.format(java.util.Locale.ROOT, "summon minecraft:end_crystal %.2f %.2f %.2f {ShowBottom:0b}", fake.getX() + 1.5, fake.getY(), fake.getZ()));
            if (ticks == 175 && fake != null)
                for (var e : mc.level.entitiesForRendering()) if (e instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal && crystal.distanceTo(fake) < 3) {
                    System.out.println("[ESS fp] attacking crystal at " + crystal.position() + " dist " + p.distanceTo(crystal));
                    mc.gameMode.attack(p, crystal);
                    break;
                }
            if (ticks == 178)
                shot(mc, "ess-fp-crystal.png");
            if (ticks == 185) {
                System.out.println("[ESS fp] after crystal " + ess.ll.client.features.FakePlayers.stateForTest());
                ess.ll.client.command.DotCommands.execute(".fakeplayer list");
            }
            if (ticks == 190)
                ess.ll.client.command.DotCommands.execute(".fakeplayer del");
            if (ticks == 195) {
                System.out.println("[ESS fp] after del " + ess.ll.client.features.FakePlayers.stateForTest() + " in level " + (fake != null && !fake.isRemoved()));
                shot(mc, "ess-fp-chat.png");
            }
            if (ticks == 200) {
                ClientModules.find("TargetESP").setEnabled(false);
                mc.stop();
            }
            return;
        }
        // Chams with TargetESP on a fake player: the effect must stay on top of the chams in every Chams mode.
        if (Boolean.getBoolean("ess.autoshot.chamsesptest")) {
            var server = mc.getSingleplayerServer();
            var p = mc.player;
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            if (ticks == 20)
                for (String c : new String[] { "gamemode survival @a", "time set day", "clear @a", "effect give @a resistance 9999 4 true", "item replace entity @a hotbar.0 with minecraft:diamond_sword" }) run.accept(c);
            if (ticks == 22) {
                int x = p.getBlockX(), z = p.getBlockZ();
                run.accept("fill " + (x - 6) + " 199 " + (z - 6) + " " + (x + 6) + " 199 " + (z + 6) + " minecraft:stone");
                run.accept("fill " + (x - 6) + " 200 " + (z - 6) + " " + (x + 6) + " 206 " + (z + 6) + " minecraft:air");
                run.accept("tp @a " + x + ".5 200 " + z + ".5 0 10");
            }
            if (ticks == 30) {
                p.setYRot(0);
                p.setXRot(10);
                ess.ll.client.command.DotCommands.execute(".fakeplayer add Dummy");
            }
            if (ticks == 31)
                run.accept(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 0 15", p.getX(), p.getY(), p.getZ() - 3));
            var esp = ess.ll.client.util.ModuleManager.INSTANCE.targetESP();
            java.util.function.Consumer<String> espMode = m -> {
                for (var st : esp.getSettings()) if (st instanceof ess.ll.client.setting.ModeSetting ms && ms.getModes().contains("Кристалики"))
                    ms.select(m);
            };
            if (ticks == 35) {
                var targets = ess.ll.client.module.render.Chams.TARGETS;
                if (!targets.selected("Игроки"))
                    targets.toggle(targets.options.indexOf("Игроки"));
                ess.ll.client.module.render.Chams.MODE.select("Flat");
                ClientModules.CHAMS.setEnabled(true);
                espMode.accept("Призраки 1");
                ClientModules.find("TargetESP").setEnabled(true);
                ClientModules.find("Aura").setEnabled(true);
            }
            String[][] steps = { { "Flat", "Призраки 1" }, { "Свечение", "Призраки 1" }, { "Блюр", "Призраки 1" }, { "Свечение", "Кристалики" }, { "Flat", "Кристалики" }, { "off", "Кристалики" } };
            for (int i = 0; i < steps.length; i++) {
                int at = 60 + i * 20;
                if (ticks == at - 12) {
                    if (steps[i][0].equals("off"))
                        ClientModules.CHAMS.setEnabled(false);
                    else
                        ess.ll.client.module.render.Chams.MODE.select(steps[i][0]);
                    espMode.accept(steps[i][1]);
                }
                if (ticks == at) {
                    System.out.println("[ESS chamsesp] " + steps[i][0] + "/" + steps[i][1] + " target=" + ess.ll.client.module.combat.AttackAura.target);
                    shot(mc, "ess-chamsesp-" + i + ".png");
                }
            }
            if (ticks == 60 + steps.length * 20) {
                ClientModules.find("Aura").setEnabled(false);
                ClientModules.find("TargetESP").setEnabled(false);
                ess.ll.client.features.FakePlayers.remove("all");
                mc.stop();
            }
            return;
        }
        // MotionBlur: the camera turns fast and steps sideways, with the blur on and then off, a shot of each.
        if (Boolean.getBoolean("ess.autoshot.motionblurtest")) {
            var p = mc.player;
            var server = mc.getSingleplayerServer();
            if (ticks == 20 && server != null)
                server.execute(() -> {
                    for (String c : new String[] { "gamemode creative @a", "time set day" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                });
            if (ticks == 30) {
                p.setXRot(15);
                ClientModules.MOTION_BLUR.setEnabled(true);
            }
            // A turn of 9 degrees each tick; the blur follows the camera between frames.
            if (ticks >= 40 && ticks < 60 || ticks >= 80 && ticks < 100)
                p.setYRot(p.getYRot() + 9);
            if (ticks == 55)
                shot(mc, "ess-motionblur-turn.png");
            if (ticks == 60)
                ClientModules.MOTION_BLUR.setEnabled(false);
            if (ticks == 95)
                shot(mc, "ess-motionblur-off.png");
            if (ticks == 100) {
                ClientModules.MOTION_BLUR.setEnabled(true);
                p.setYRot(0);
            }
            if (ticks >= 110 && ticks < 125)
                p.setPos(p.getX() + .6, p.getY(), p.getZ());
            if (ticks == 120)
                shot(mc, "ess-motionblur-strafe.png");
            if (ticks == 130)
                shot(mc, "ess-motionblur-still.png");
            if (ticks == 135) {
                ClientModules.MOTION_BLUR.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // CrystalAura placement mark: an obsidian in front of the camera fills from its centre, holds, then fades.
        if (Boolean.getBoolean("ess.autoshot.crystalfxtest")) {
            var module = ClientModules.find("CrystalAura");
            var aura = ess.ll.client.util.ModuleManager.INSTANCE.crystalAura();
            if (ticks == 30) {
                var server = mc.getSingleplayerServer();
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 120.5 200 57.5 0 20"));
            }
            if (ticks == 40) {
                mc.player.setYRot(0);
                mc.player.setXRot(20);
                module.setEnabled(true);
            }
            var base = mc.player.blockPosition().offset(0, 0, 3);
            if (ticks == 45)
                mc.level.setBlock(base, net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
            if (ticks == 50)
                aura.testMark(base);
            for (int f : new int[] { 1, 2, 4, 8, 14, 20 }) if (ticks == 50 + f)
                shot(mc, "ess-crystalfx-" + f + ".png");
            if (ticks == 90) {
                module.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // Pause menu over the world, with a playing track in the corner, in both themes.
        if (Boolean.getBoolean("ess.autoshot.pausetest")) {
            if (ticks == 40) {
                ess.ll.client.media.MediaBridge.fake("Midnight City", "M83", "Hurry Up, We're Dreaming", 9_000, 243_000);
                var lines = new java.util.ArrayList<ess.ll.client.media.Lyrics.Line>();
                lines.add(new ess.ll.client.media.Lyrics.Line(0, "Waiting for a ride in the dark"));
                ess.ll.client.media.Lyrics.put(ess.ll.client.media.MediaBridge.track(), lines);
                mc.pauseGame(false);
            }
            if (ticks == 75)
                shot(mc, "ess-pause.png");
            if (ticks == 76)
                MainMenuScreen.toggleLightForTest();
            if (ticks == 95)
                shot(mc, "ess-pause-light.png");
            if (ticks == 96) {
                MainMenuScreen.toggleLightForTest();
                // A click on "Статистика" through the normal screen input should open the stats screen.
                if (mc.gui.screen() instanceof ess.ll.client.screen.PauseMenuScreen pause) {
                    var at = pause.centerOf("stats");
                    System.out.println("[ESS autoshot] stats at " + (at == null ? "none" : at[0] + "," + at[1]));
                    if (at != null)
                        mc.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(at[0], at[1], new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                }
                System.out.println("[ESS autoshot] after click: " + (mc.gui.screen() == null ? "null" : mc.gui.screen().getClass().getSimpleName()));
                mc.stop();
            }
            return;
        }
        if (Boolean.getBoolean("ess.autoshot.blockesp")) {
            BlockEspCheck.tick(mc, ticks);
            return;
        }
        if (Boolean.getBoolean("ess.autoshot.portedmodules")) {
            PortedModulesCheck.tick(mc, ticks);
            return;
        }
        if (Boolean.getBoolean("ess.autoshot.crystalchain")) {
            CrystalChainCheck.tick(mc, ticks);
            return;
        }
        if (ticks == 10)
            mc.player.setXRot(0);
        if (ticks == 25) {
            for (var m : List.of(ClientModules.HUD, ClientModules.find("Sprint"), ClientModules.find("AutoJump"))) m.setEnabled(true);
            var server = mc.getSingleplayerServer();
            // Run as the integrated server so the check works in worlds without cheats.
            if (server != null)
                server.execute(() -> {
                    boolean crystal = Boolean.getBoolean("ess.autoshot.crystaltest");
                    for (String command : crystal ? List.of("time set noon", "weather clear", "effect clear @p", "effect give @p minecraft:resistance 90 5", "give @p minecraft:obsidian 64", "give @p minecraft:end_crystal 64", "execute as @p at @p run tp @p ~ 150 ~", "execute as @p at @p run fill ~-6 ~ ~-6 ~6 ~2 ~6 minecraft:air replace", "execute as @p at @p run kill @e[type=minecraft:end_crystal,distance=..8]", "execute as @p at @p run kill @e[type=minecraft:husk,distance=..8]", "execute as @p at @p run fill ~-6 ~-1 ~-6 ~6 ~-1 ~6 minecraft:stone replace", "execute as @p at @p run fill ~-2 ~ ~3 ~2 ~ ~5 minecraft:stone replace", "execute as @p at @p run setblock ~ ~ ~5 air", "execute as @p at @p run summon minecraft:husk ^ ^ ^5 {NoAI:1b,Health:20f}") : List.of("time set noon", "weather clear", "effect give @p minecraft:speed 300 1", "effect give @p minecraft:night_vision 90", "effect give @p minecraft:poison 20", "execute as @p at @p run summon minecraft:husk ^ ^0.3 ^3 {NoAI:1b,NoGravity:1b,Health:14f,equipment:{mainhand:{id:\"minecraft:iron_sword\"}," + "offhand:{id:\"minecraft:shield\"},head:{id:\"minecraft:diamond_helmet\"},chest:{id:\"minecraft:iron_chestplate\"},feet:{id:\"minecraft:golden_boots\"}}}")) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                    if (Boolean.getBoolean("ess.autoshot.targetesptest"))
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect give @e[type=minecraft:husk,limit=1,sort=nearest] minecraft:resistance 30 5");
                    // Benchmark: the husk soaks every explosion, so the crystals used in the window measure the cycle speed.
                    if (Boolean.getBoolean("ess.autoshot.crystalbench"))
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect give @e[type=minecraft:husk] minecraft:absorption 60 60 true");
                    if (Boolean.getBoolean("ess.autoshot.digtest"))
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "execute as @p at @p run setblock ~ ~ ~2 minecraft:stone");
                });
        }
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.modules")) {
            if (Boolean.getBoolean("ess.autoshot.swingtest"))
                ClientModules.find("CrystalAura").setEnabled(false);
            for (String name : List.of("AutoTool", "ClickPearl", "SwingAnimation", "ViewModel", "CrystalAura")) {
                if (Boolean.getBoolean("ess.autoshot.swingtest") && name.equals("CrystalAura"))
                    continue;
                ClientModules.find(name).setEnabled(true);
            }
            if (Boolean.getBoolean("ess.autoshot.swingtest")) {
                var swings = ess.ll.client.util.ModuleManager.INSTANCE.swingAnimations();
                swings.getSwingStyle().select(System.getProperty("ess.autoshot.swingmode", "Мод 3"));
                swings.getActiveHandMode().select("Всегда");
            }
        }
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.entityesptest")) {
            for (var setting : ClientModules.find("EntityESP").settings) if (setting.name().equals("Кого отображать") && setting instanceof ess.ll.client.setting.Settings.MultiBooleanSettings groups)
                for (String group : List.of("Мобы", "Себя")) if (!groups.selected(group))
                    groups.toggle(groups.options.indexOf(group));
            for (var setting : ClientModules.find("EntityESP").settings) if (setting.name().equals("Плашка") && setting instanceof ess.ll.client.setting.Settings.ModeSettings mode)
                mode.select(System.getProperty("ess.autoshot.espplate", "В строку"));
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            ClientModules.find("EntityESP").setEnabled(true);
            System.out.println("[ESS EntityESP] enabled with " + ClientModules.find("EntityESP").settings.size() + " settings");
        }
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.removalstest")) {
            ClientModules.find("Removals").setEnabled(true);
            System.out.println("[ESS Removals] enabled with " + ClientModules.find("Removals").settings.size() + " settings");
        }
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.elytratest")) {
            ClientModules.find("ElytraHelper").setEnabled(true);
            var server = mc.getSingleplayerServer();
            if (server != null)
                server.execute(() -> {
                    for (String command : List.of("item replace entity @p armor.chest with minecraft:elytra", "item replace entity @p hotbar.1 with minecraft:firework_rocket 16", "execute as @p at @p run tp @p ~ ~20 ~")) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                });
        }
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.guitest")) {
            var gui = new ClickGuiScreen();
            mc.gui.setScreen(gui);
            gui.expand(ClientModules.find("EntityESP"));
            gui.expand(ClientModules.find("ElytraHelper"));
        }
        // Sidebar style: collapsed cards, two opened by right click, the bind popup, then the window style.
        if (Boolean.getBoolean("ess.autoshot.sidebartest")) {
            if (ticks == 35) {
                ClientModules.GUI_STYLE.select("Сайдбар");
                ClientModules.find("AutoTotem").setEnabled(true);
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Combat");
            }
            if (ticks == 50)
                shot(mc, "ess-sidebar.png");
            if (ticks == 51 && mc.gui.screen() instanceof ClickGuiScreen gui) {
                gui.expand(ClientModules.find("Aura"));
                gui.expand(ClientModules.find("AutoTotem"));
            }
            if (ticks == 62)
                shot(mc, "ess-sidebar-open.png");
            if (ticks == 63 && mc.gui.screen() instanceof ClickGuiScreen gui)
                gui.showBind(ClientModules.find("Aura"));
            if (ticks == 72)
                shot(mc, "ess-sidebar-bind.png");
            if (ticks == 73 && mc.gui.screen() instanceof ClickGuiScreen gui) {
                gui.closeBind();
                ClientModules.GUI_STYLE.select("Окно");
            }
            if (ticks == 82)
                shot(mc, "ess-window-style.png");
            if (ticks == 83) {
                ClientModules.GUI_STYLE.select("Сайдбар");
                ClientModules.find("AutoTotem").setEnabled(false);
                mc.stop();
            }
        }
        // Themes tab, then a light theme with the bind popup open.
        // Telegram against a local fake Bot API (-Dess.telegram.api): the code goes to tg-code.txt for it to link with.
        if (Boolean.getBoolean("ess.autoshot.tgtest")) {
            var server = mc.getSingleplayerServer();
            if (ticks == 30) {
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "clear @a", "give @a minecraft:stone 100", "give @a minecraft:stone_bricks 5" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
                ess.ll.client.telegram.TelegramBot.unlink();
                ess.ll.client.telegram.TelegramBot.setToken("123456:test-token");
                if (!ClientModules.find("Sprint").enabled())
                    ClientModules.find("Sprint").setEnabled(true);
                ClientModules.TELEGRAM.setEnabled(true);
            }
            if (ticks == 60)
                try {
                    java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("tg-code.txt"), ess.ll.client.telegram.TelegramBot.code());
                } catch (Exception e) {
                    System.out.println("[ESS tg] " + e);
                }
            if (ticks % 100 == 0)
                System.out.println("[ESS tg] t=" + ticks + " state=" + ess.ll.client.telegram.TelegramBot.state() + " " + ess.ll.client.telegram.TelegramBot.error() + " linked=" + ess.ll.client.telegram.TelegramBot.linked() + " sprint=" + ClientModules.find("Sprint").enabled());
            if (ticks == 40)
                ess.ll.client.telegram.TelegramBot.openMenu();
            if (ticks == 55)
                shot(mc, "ess-tg-menu-code.png");
            if (ticks == 600 && mc.gui.screen() instanceof ess.ll.client.telegram.TelegramScreen menu)
                menu.typeForTest("7701234567:AAFxk2-pQ9");
            if (ticks == 610)
                shot(mc, "ess-tg-menu-linked.png");
            if (ticks == 700) {
                ClientModules.TELEGRAM.setEnabled(false);
                mc.stop();
            }
            return;
        }
        // ItemScroller through the real mouse path: press with Shift, move across inventory slots, release.
        if (Boolean.getBoolean("ess.autoshot.dragtest")) {
            var server = mc.getSingleplayerServer();
            var p = mc.player;
            if (ticks == 20 && server != null)
                server.execute(() -> {
                    for (String c : new String[] { "gamemode " + (Boolean.getBoolean("ess.autoshot.dragcreative") ? "creative" : "survival") + " @a", "clear @a", "item replace entity @a inventory.5 with minecraft:stone 7", "item replace entity @a inventory.6 with minecraft:sand 9", "item replace entity @a inventory.7 with minecraft:dirt 5", "item replace entity @a inventory.8 with minecraft:oak_log 3" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                });
            if (ticks == 20)
                ClientModules.ITEM_SCROLLER.setEnabled(true);
            if (ticks == 40)
                mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
            if (ticks == 45 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creative)
                try {
                    var select = creative.getClass().getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                    select.setAccessible(true);
                    select.invoke(creative, net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(net.minecraft.world.item.CreativeModeTabs.INVENTORY));
                } catch (ReflectiveOperationException e) {
                    System.out.println("[ESS drag] " + e);
                }
            if (ticks == 50)
                System.out.println("[ESS drag] screen=" + mc.gui.screen().getClass().getSimpleName() + " mode=" + mc.gameMode.getPlayerMode());
            if (ticks >= 50 && ticks <= 58 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> inv)
                try {
                    var lf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("leftPos");
                    lf.setAccessible(true);
                    var tf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("topPos");
                    tf.setAccessible(true);
                    double scale = mc.getWindow().getGuiScale();
                    long win = mc.getWindow().handle();
                    int[] cols = { 14, 15, 16, 17 };
                    int k = Math.min(3, (ticks - 50) / 2);
                    var sl = inv.getMenu().slots.stream().filter(x -> x.container instanceof net.minecraft.world.entity.player.Inventory && x.getContainerSlot() == cols[k]).findFirst().get();
                    double x = (lf.getInt(inv) + sl.x + 8) * scale, y = (tf.getInt(inv) + sl.y + 8) * scale;
                    var px = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
                    px.setAccessible(true);
                    var py = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
                    py.setAccessible(true);
                    double ox = px.getDouble(mc.mouseHandler), oy = py.getDouble(mc.mouseHandler);
                    mc.mouseHandler.onMove(win, x, y, x - ox, y - oy);
                    px.setDouble(mc.mouseHandler, x);
                    py.setDouble(mc.mouseHandler, y);
                    if (ticks == 50)
                        mc.mouseHandler.onButton(win, new net.minecraft.client.input.MouseButtonInfo(0, com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT), 1);
                    else
                        mc.mouseHandler.handleAccumulatedMovement();
                    if (ticks == 58)
                        mc.mouseHandler.onButton(win, new net.minecraft.client.input.MouseButtonInfo(0, com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT), 0);
                } catch (ReflectiveOperationException e) {
                    System.out.println("[ESS drag] " + e);
                }
            if (ticks == 64) {
                var sb = new StringBuilder();
                for (int i = 0; i < 36; i++) if (!p.getInventory().getItem(i).isEmpty())
                    sb.append(i).append(":").append(p.getInventory().getItem(i)).append(" ");
                System.out.println("[ESS drag] inventory after: " + sb + " active=" + mc.isWindowActive());
            }
            if (ticks == 70)
                mc.stop();
            return;
        }
        // AucHelper: a chest of lots with prices in their descriptions; the cheapest of each kind gets framed.
        if (Boolean.getBoolean("ess.autoshot.auctest")) {
            var server = mc.getSingleplayerServer();
            if (sorterBase == null)
                sorterBase = mc.player.blockPosition();
            var chest = sorterBase.offset(2, 0, 0);
            if (ticks == 30 && server != null)
                server.execute(() -> {
                    List<String> cmds = new java.util.ArrayList<>(List.of("gamemode creative @a", "setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " chest"));
                    String[] lots = { "totem_of_undying[lore=[\"Цена: 1 500$\"]]", "totem_of_undying[lore=[\"Продавец: Steve2009\",\"Цена: 2 000$\"]] 2", "totem_of_undying[lore=[\"Стоимость: 3к\"]]", "diamond_sword[enchantments={sharpness:5},lore=[\"Цена: 10 000$\"]]", "diamond_sword[enchantments={sharpness:5},lore=[\"Цена: 8.5к\"]]", "diamond_sword[lore=[\"Цена: 500$\"]]", "golden_apple[lore=[\"Цена: 1600$\"]] 16", "golden_apple[lore=[\"Цена: 960\"]] 8", "enchanted_golden_apple[lore=[\"Цена: 50к\"]]", "netherite_chestplate[enchantments={protection:4},lore=[\"Цена: 1,2кк\"]]", "netherite_chestplate[enchantments={protection:4},lore=[\"Цена: 900к\"]]", "dirt[lore=[\"Цена: 1$\"]]", "player_head[lore=[\"Цена: 5к\"]]" };
                    for (int i = 0; i < lots.length; i++) cmds.add("item replace block " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " container." + i + " with minecraft:" + lots[i]);
                    for (String c : cmds) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                });
            if (ticks == 40)
                ClientModules.AUC_HELPER.setEnabled(true);
            if (ticks == 45) {
                var c = net.minecraft.world.phys.Vec3.atCenterOf(chest);
                mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(c.add(0, .5, 0), net.minecraft.core.Direction.UP, chest, false));
            }
            if (ticks == 60 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> s)
                for (var slot : s.getMenu().slots) if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory) && slot.hasItem())
                    System.out.println("[ESS auc] " + slot.index + " " + slot.getItem().getCount() + "x" + slot.getItem().getItem() + " kind=" + ess.ll.client.module.misc.AucHelper.kindForTest(slot.getItem()) + " price=" + ess.ll.client.module.misc.AucHelper.priceForTest(slot.getItem()));
            if (ticks == 65)
                shot(mc, "ess-auc.png");
            if (ticks == 70)
                mc.stop();
            return;
        }
        // AutoSorter: a source chest with mixed items and three targets, one far enough to walk to.
        if (Boolean.getBoolean("ess.autoshot.sortertest")) {
            var server = mc.getSingleplayerServer();
            if (sorterBase == null)
                sorterBase = mc.player.blockPosition();
            var p = sorterBase;
            net.minecraft.core.BlockPos src = p.offset(2, 0, 0), a = p.offset(2, 0, 3), d = p.offset(-3, 0, 0), far = p.offset(9, 0, -2);
            if (ticks == 30 && server != null)
                server.execute(() -> {
                    List<String> cmds = new java.util.ArrayList<>(List.of("gamemode survival @a", "fill " + (p.getX() - 12) + " " + (p.getY() - 1) + " " + (p.getZ() - 12) + " " + (p.getX() + 12) + " " + (p.getY() - 1) + " " + (p.getZ() + 12) + " stone", "fill " + (p.getX() - 12) + " " + p.getY() + " " + (p.getZ() - 12) + " " + (p.getX() + 12) + " " + (p.getY() + 3) + " " + (p.getZ() + 12) + " air", "clear @a"));
                    for (var c : new net.minecraft.core.BlockPos[] { src, a, d, far }) cmds.add("setblock " + c.getX() + " " + c.getY() + " " + c.getZ() + " chest");
                    String[] fill = { "cobblestone 64", "dirt 32", "diamond 5", "oak_log 16", "cobblestone 20", "apple 3", "dirt 64", "oak_log 40" };
                    for (int i = 0; i < fill.length; i++) cmds.add("item replace block " + src.getX() + " " + src.getY() + " " + src.getZ() + " container." + (i * 3) + " with minecraft:" + fill[i]);
                    if (Boolean.getBoolean("ess.autoshot.sorterfull"))
                        for (int i = 0; i < 32; i++) cmds.add("give @a minecraft:stone_bricks 64");
                    for (String c : cmds) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                });
            if (ticks == 40) {
                ess.ll.client.module.misc.AutoSorter.devMark(src);
                ess.ll.client.module.misc.AutoSorter.devMark(a, "minecraft:cobblestone", "minecraft:dirt");
                ess.ll.client.module.misc.AutoSorter.devMark(d, "minecraft:diamond");
                ess.ll.client.module.misc.AutoSorter.devMark(far, "minecraft:oak_log");
            }
            if (ticks == 50)
                shot(mc, "ess-sorter-before.png");
            if (ticks == 55)
                ClientModules.AUTO_SORTER.setEnabled(true);
            if (ticks > 55 && ticks % 20 == 0)
                System.out.println("[ESS sorter] t=" + ticks + " stage=" + ess.ll.client.module.misc.AutoSorter.stage() + " on=" + ClientModules.AUTO_SORTER.enabled() + " at=" + mc.player.blockPosition());
            if (ticks > 60 && (!ClientModules.AUTO_SORTER.enabled() || ticks == 1500) && sorterDone < 0) {
                sorterDone = ticks;
                if (server != null)
                    server.execute(() -> {
                        for (var c : new net.minecraft.core.BlockPos[] { src, a, d, far }) {
                            StringBuilder line = new StringBuilder("[ESS sorter] chest " + c.toShortString() + ":");
                            if (server.overworld().getBlockEntity(c) instanceof net.minecraft.world.Container box)
                                for (int i = 0; i < box.getContainerSize(); i++) if (!box.getItem(i).isEmpty())
                                    line.append(" ").append(box.getItem(i).getCount()).append("x").append(box.getItem(i).getItem());
                            System.out.println(line);
                        }
                        StringBuilder inv = new StringBuilder("[ESS sorter] player:");
                        for (var pl : server.getPlayerList().getPlayers()) for (int i = 0; i < pl.getInventory().getContainerSize(); i++) if (!pl.getInventory().getItem(i).isEmpty())
                            inv.append(" ").append(pl.getInventory().getItem(i).getCount()).append("x").append(pl.getInventory().getItem(i).getItem());
                        System.out.println(inv);
                    });
            }
            if (sorterDone > 0 && ticks == sorterDone + 5)
                shot(mc, "ess-sorter-after.png");
            if (sorterDone > 0 && ticks == sorterDone + 10)
                mc.stop();
            return;
        }
        // StreamerMode: your nick, the grief number and the clan on the scoreboard, in chat and in the HUD.
        if (Boolean.getBoolean("ess.autoshot.streamertest")) {
            if (ticks == 30) {
                ClientModules.STREAMER_MODE.setEnabled(Boolean.parseBoolean(System.getProperty("ess.autoshot.streamer", "true")));
                String me = mc.getUser().getName();
                var server = mc.getSingleplayerServer();
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "scoreboard objectives add ess dummy \"Сервер\"", "scoreboard objectives setdisplay sidebar ess", "scoreboard players set a ess 3", "scoreboard players set b ess 2", "scoreboard players set c ess 1", "scoreboard players display name a ess [{\"text\":\"ГРИФ \",\"color\":\"red\"},{\"text\":\"#12\",\"color\":\"yellow\"}]", "scoreboard players display name b ess [{\"text\":\"Клан: \",\"color\":\"gray\"},{\"text\":\"Abcdef\",\"color\":\"green\"}]", "scoreboard players display name c ess [{\"text\":\"Ник: \",\"color\":\"gray\"},{\"text\":\"" + me + "\",\"color\":\"white\"}]", "tellraw @a {\"text\":\"<" + me + "> привет, мой клан: Abcdef\"}" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
            }
            if (ticks == 60)
                shot(mc, "ess-streamer.png");
            if (ticks == 61)
                mc.options.keyPlayerList.setDown(true);
            if (ticks == 70)
                shot(mc, "ess-streamer-tab.png");
            if (ticks == 71) {
                mc.options.keyPlayerList.setDown(false);
                mc.stop();
            }
            return;
        }
        // CS knives in first person: each knife comes out, rests, slashes and is inspected; then the CS tab of Cosmetics.
        if (Boolean.getBoolean("ess.autoshot.knifetest")) {
            String only = System.getProperty("ess.autoshot.knife", "");
            String[] knives = only.isEmpty() ? new String[] { "karambit", "butterfly", "m9", "daggers" } : only.split(",");
            String finish = System.getProperty("ess.autoshot.finish", "steel");
            if (ticks == 30) {
                ClientModules.WARDROBE.setEnabled(true);
                var server = mc.getSingleplayerServer();
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "gamemode creative @a", "time set noon", "item replace entity @a weapon.offhand with minecraft:air", "item replace entity @a weapon.mainhand with minecraft:diamond_sword" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
            }
            int at = ticks - 40, k = at / 150, t = at % 150;
            if (at >= 0 && k < knives.length) {
                String name = knives[k];
                if (t == 0)
                    ess.ll.client.wardrobe.Wardrobe.wear(ess.ll.client.wardrobe.Wardrobe.item("weapon_cs_" + name + "_" + finish));
                if (t == 5)
                    shot(mc, "ess-knife-" + name + "-draw.png");
                if (t == 34)
                    shot(mc, "ess-knife-" + name + "-idle.png");
                if (t == 36)
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                if (t == 41)
                    shot(mc, "ess-knife-" + name + "-swing.png");
                if (t == 46)
                    ess.ll.client.wardrobe.CsKnife.inspect();
                if (t == 55)
                    shot(mc, "ess-knife-" + name + "-inspect1.png");
                if (t == 66)
                    shot(mc, "ess-knife-" + name + "-inspect2.png");
                if (t == 78)
                    shot(mc, "ess-knife-" + name + "-inspect3.png");
            }
            if (at == knives.length * 150) {
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Cosmetics");
                gui.selectWardrobeCategory(ess.ll.client.wardrobe.Wardrobe.Category.WEAPON.ordinal());
                gui.showCsKnives();
            }
            if (at == knives.length * 150 + 20)
                shot(mc, "ess-knife-gui.png");
            if (at == knives.length * 150 + 25)
                mc.stop();
            return;
        }
        // Item skins in first person: a totem, a golden apple and a crystal with a worn skin each.
        if (Boolean.getBoolean("ess.autoshot.itemskintest")) {
            var W = ess.ll.client.wardrobe.Wardrobe.class;
            String hmi = Boolean.getBoolean("ess.autoshot.hmi") ? "-hmi" : "";
            if (ticks == 30) {
                if (!hmi.isEmpty()) {
                    var swings = ess.ll.client.util.ModuleManager.INSTANCE.swingAnimations();
                    for (var st : swings.getSettings()) if (st instanceof ess.ll.client.setting.ModeSetting ms && ms.getModes().contains("HoldMyItems"))
                        ms.select("HoldMyItems");
                    swings.setEnabled(true);
                }
                ClientModules.WARDROBE.setEnabled(true);
                for (var slot : new ess.ll.client.wardrobe.Wardrobe.Slot[] { ess.ll.client.wardrobe.Wardrobe.Slot.TOTEM, ess.ll.client.wardrobe.Wardrobe.Slot.GAPPLE, ess.ll.client.wardrobe.Wardrobe.Slot.CRYSTAL, ess.ll.client.wardrobe.Wardrobe.Slot.SWORD }) ess.ll.client.wardrobe.Wardrobe.wear(ess.ll.client.wardrobe.Wardrobe.items(slot).get(0));
                var server = mc.getSingleplayerServer();
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "gamemode creative @a", "item replace entity @a weapon.mainhand with minecraft:totem_of_undying" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
            }
            if (ticks == 50) {
                System.out.println("[ESS itemskin] worn totem=" + ess.ll.client.wardrobe.Wardrobe.worn(ess.ll.client.wardrobe.Wardrobe.Slot.TOTEM) + " enabled=" + ClientModules.WARDROBE.enabled() + " held=" + mc.player.getMainHandItem() + " hmi=" + ess.ll.client.integration.ItemInHandRendererHooks.enabled);
                shot(mc, "ess-itemskin-totem" + hmi + ".png");
            }
            if (ticks == 52) {
                var server = mc.getSingleplayerServer();
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "item replace entity @a weapon.mainhand with minecraft:golden_apple"));
            }
            if (ticks == 65)
                shot(mc, "ess-itemskin-gapple" + hmi + ".png");
            if (ticks == 67) {
                var server = mc.getSingleplayerServer();
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "item replace entity @a weapon.mainhand with minecraft:diamond_sword"));
            }
            if (ticks == 80)
                shot(mc, "ess-itemskin-sword" + hmi + ".png");
            if (ticks == 81) {
                var server = mc.getSingleplayerServer();
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "item replace entity @a weapon.mainhand with minecraft:end_crystal"));
            }
            if (ticks == 94)
                shot(mc, "ess-itemskin-crystal" + hmi + ".png");
            if (ticks == 95 && !hmi.isEmpty()) {
                ess.ll.client.util.ModuleManager.INSTANCE.swingAnimations().setEnabled(false);
                mc.stop();
            }
            // The Cosmetics preview of the totem slot, with an empty hand.
            if (ticks == 82 && hmi.isEmpty()) {
                mc.player.getInventory().setItem(mc.player.getInventory().getSelectedSlot(), net.minecraft.world.item.ItemStack.EMPTY);
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Cosmetics");
                gui.selectWardrobeCategory(ess.ll.client.wardrobe.Wardrobe.Category.ITEM.ordinal());
            }
            if (ticks == 100)
                shot(mc, "ess-itemskin-preview.png");
            if (ticks == 105)
                mc.stop();
            return;
        }
        // The ClickGUI: Combat with two cards open, then a Render module with a colour and its picker.
        if (Boolean.getBoolean("ess.autoshot.guitest")) {
            if (ticks == 30)
                ClientModules.BACKGROUND_STYLE.select("Блюр");
            if (ticks == 35) {
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.expand(ClientModules.find("CrystalAura"));
                gui.expand(ClientModules.find("Aura"));
            }
            if (ticks == 55)
                shot(mc, "ess-gui-combat.png");
            if (ticks == 56 && mc.gui.screen() instanceof ClickGuiScreen gui)
                gui.toggleSettings();
            if (ticks == 75)
                shot(mc, "ess-gui-settings.png");
            if (ticks == 80 && mc.gui.screen() instanceof ClickGuiScreen gui) {
                var module = ClientModules.find("BlockOverlay");
                gui.expand(module);
                for (var s : module.settings) if (s instanceof ess.ll.client.setting.Settings.ColorSetting color) {
                    gui.showPicker(color);
                    break;
                }
            }
            if (ticks == 95)
                shot(mc, "ess-gui-picker.png");
            if (ticks == 100)
                mc.stop();
            return;
        }
        if (Boolean.getBoolean("ess.autoshot.themetest")) {
            if (ticks == 30)
                ClientModules.BACKGROUND_STYLE.select("Блюр");
            if (ticks == 35) {
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Темы");
            }
            if (ticks == 50)
                shot(mc, "ess-themes.png");
            if (ticks == 51)
                ess.ll.client.render.Theme.apply(ess.ll.client.render.Theme.PRESETS.get(10));
            if (ticks == 60)
                shot(mc, "ess-themes-light.png");
            if (ticks == 61 && mc.gui.screen() instanceof ClickGuiScreen gui) {
                gui.show("Combat");
                gui.showBind(ClientModules.find("Aura"));
            }
            if (ticks == 72)
                shot(mc, "ess-gui-light.png");
            if (ticks == 73)
                ess.ll.client.render.Theme.reset();
            if (ticks == 74 && mc.gui.screen() instanceof ClickGuiScreen gui)
                gui.search("ра");
            if (ticks == 82)
                shot(mc, "ess-search.png");
            if (ticks == 83)
                mc.stop();
        }
        // DeathEffect: a zombie and a copy of the player shatter into cubes, then a second zombie turns into fireflies.
        if (Boolean.getBoolean("ess.autoshot.deathtest")) {
            var server = mc.getSingleplayerServer();
            Runnable zombie = () -> {
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "kill @e[type=minecraft:zombie]", "execute as @p at @p positioned ^-1.2 ^ ^4 run summon minecraft:zombie ~ ~ ~ {NoAI:1b,PersistenceRequired:1b}", "execute as @e[type=minecraft:zombie] at @s run tp @s ~ ~ ~ facing entity @p" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
            };
            Runnable kill = () -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:zombie]"));
            };
            if (ticks == 35) {
                ClientModules.DEATH_EFFECT.setEnabled(true);
                ess.ll.client.module.render.DeathEffect.MODE.select("Кубики");
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "time set noon", "weather clear", "gamerule doMobLoot false" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
                zombie.run();
            }
            if (ticks == 50)
                shot(mc, "ess-death-before.png");
            if (ticks == 54) {
                kill.run();
                var p = mc.player;
                double yaw = Math.toRadians(p.getYRot());
                double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
                ess.ll.client.module.render.DeathEffect.preview(p, p.getX() + fx * 4 - rx * 1.4, p.getY(), p.getZ() + fz * 4 - rz * 1.4, p.getYRot() + 180);
            }
            if (ticks == 56)
                shot(mc, "ess-death-cubes-0.png");
            if (ticks == 60)
                shot(mc, "ess-death-cubes-1.png");
            if (ticks == 72)
                shot(mc, "ess-death-cubes-2.png");
            if (ticks == 100) {
                ess.ll.client.module.render.DeathEffect.MODE.select("Светлячки");
                zombie.run();
            }
            if (ticks == 115)
                kill.run();
            if (ticks == 121)
                shot(mc, "ess-death-fireflies-0.png");
            if (ticks == 132)
                shot(mc, "ess-death-fireflies-1.png");
            // Cards and fields follow Непрозрачность as well.
            if (ticks == 136) {
                ClientModules.THEME_OPACITY.set(65);
                mc.gui.setScreen(new ClickGuiScreen());
            }
            if (ticks == 148)
                shot(mc, "ess-gui-opacity.png");
            if (ticks == 150) {
                ClientModules.THEME_OPACITY.set(92);
                ess.ll.client.module.render.DeathEffect.MODE.select("Кубики");
                mc.stop();
            }
            return;
        }
        // Loading screen: a resource reload, captured while the logo assembles, lights up and fades out.
        if (Boolean.getBoolean("ess.autoshot.loadingtest")) {
            if (ticks == 30)
                mc.reloadResourcePacks();
            if (ticks == 35)
                shot(mc, "ess-loading-1.png");
            if (ticks == 45)
                shot(mc, "ess-loading-2.png");
            if (ticks == 60)
                shot(mc, "ess-loading-3.png");
            if (ticks == 75)
                shot(mc, "ess-loading-4.png");
            if (ticks >= 62 && ticks <= 86 && ticks % 4 == 2)
                shot(mc, "ess-loading-glint-" + ticks + ".png");
            if (ticks == 110)
                mc.stop();
            return;
        }
        // Accounts page with a playing track, the server card, then the player opened with lyrics.
        if (Boolean.getBoolean("ess.autoshot.accountstest")) {
            if (ticks == 30) {
                var list = new net.minecraft.client.multiplayer.ServerList(mc);
                list.load();
                if (list.size() == 0) {
                    list.add(new net.minecraft.client.multiplayer.ServerData("Hypixel", "mc.hypixel.net", net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
                    list.add(new net.minecraft.client.multiplayer.ServerData("Локальный", "localhost", net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
                    list.save();
                }
                var alt = ess.ll.client.screen.AltAccounts.addOffline("ess_alt");
                if (alt != null) {
                    alt.favorite = true;
                    alt.serverName = "Hypixel";
                    alt.serverIp = "mc.hypixel.net";
                    alt.serverAt = System.currentTimeMillis() - 3_600_000;
                }
                ess.ll.client.screen.AltAccounts.addOffline("PvP_Test");
                ess.ll.client.media.MediaBridge.fake("Midnight City", "M83", "Hurry Up, We're Dreaming", 9_000, 243_000);
                var lines = new java.util.ArrayList<ess.ll.client.media.Lyrics.Line>();
                String[] text = { "Waiting in a car", "Waiting for a ride in the dark", "The night city grows", "Look and see her eyes, they glow", "Waiting in a car", "Waiting for the right time" };
                for (int i = 0; i < text.length; i++) lines.add(new ess.ll.client.media.Lyrics.Line(i * 3500L, text[i]));
                ess.ll.client.media.Lyrics.put(ess.ll.client.media.MediaBridge.track(), lines);
                // A second menu would read the worlds while the first one still holds their locks.
                if (!(mc.gui.screen() instanceof MainMenuScreen))
                    mc.gui.setScreen(new MainMenuScreen());
            }
            if (mc.gui.screen() instanceof MainMenuScreen menu) {
                if (ticks == 70)
                    shot(mc, "ess-accounts-home.png");
                if (ticks == 71)
                    menu.showPage(6);
                if (ticks == 100)
                    shot(mc, "ess-accounts.png");
                if (ticks == 101)
                    menu.selectAccount("ess_alt");
                if (ticks == 115)
                    shot(mc, "ess-accounts-alt.png");
                if (ticks == 116)
                    menu.showPage(2);
                if (ticks == 160)
                    shot(mc, "ess-accounts-multi.png");
                if (ticks == 161)
                    menu.showPage(1);
                if (ticks == 185)
                    shot(mc, "ess-accounts-single.png");
                if (ticks == 186) {
                    menu.showPage(0);
                    menu.openPlayer();
                }
                if (ticks == 210)
                    shot(mc, "ess-accounts-player.png");
                if (ticks == 211) {
                    menu.showPage(2);
                    menu.toggleTheme();
                }
                if (ticks == 245)
                    shot(mc, "ess-accounts-light.png");
                if (ticks == 246) {
                    menu.toggleTheme();
                    mc.stop();
                }
            }
            return;
        }
        // .way: a near point straight ahead, a far one, the HUD capsule turning as the camera turns; NoPush on.
        if (Boolean.getBoolean("ess.autoshot.waytest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            if (ticks == 20) {
                run.accept("kill @e[type=!minecraft:player]");
                run.accept("tp " + me + " 0.5 201 0.5 180 0");
                run.accept("time set day");
                ClientModules.NO_PUSH.setEnabled(true);
                ess.ll.client.command.DotCommands.execute(".way clear");
                ess.ll.client.command.DotCommands.execute(".way add 2000 -3000 База");
                ess.ll.client.command.DotCommands.execute(".way add 3 -12 Дом");
            }
            if (ticks == 40) {
                p.setYRot(180);
                p.setXRot(-8);
            }
            if (ticks == 59)
                System.out.println("[ESS waytest] ground at Дом " + mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 3, -12) + " chunk " + mc.level.hasChunk(0, -2) + " player y " + p.getY() + " pos " + p.position());
            if (ticks == 60) {
                shot(mc, "ess-way-near.png");
                System.out.println("[ESS waytest] active " + ess.ll.client.features.Waypoints.active() + " list " + ess.ll.client.features.Waypoints.here().size() + " nopush " + ClientModules.NO_PUSH.enabled());
            }
            if (ticks == 61) {
                ess.ll.client.command.DotCommands.execute(".way go База");
                p.setYRot(-146);
            }
            if (ticks == 80)
                shot(mc, "ess-way-far.png");
            if (ticks == 81)
                p.setYRot(30);
            if (ticks == 100)
                shot(mc, "ess-way-away.png");
            if (ticks == 101) {
                ess.ll.client.command.DotCommands.execute(".way list");
                ess.ll.client.command.DotCommands.execute(".way remove Дом");
                ess.ll.client.command.DotCommands.execute(".way remove База");
                System.out.println("[ESS waytest] after remove " + ess.ll.client.features.Waypoints.here().size());
            }
            if (ticks == 110)
                mc.stop();
            return;
        }
        // Emotes: a few emotes from the front, then the wheel and the picker.
        if (Boolean.getBoolean("ess.autoshot.emotetest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            String[] emotes = { "waving", "point", "backflip", "twerk", "club_penguin_dance", "air_kiss" };
            if (ticks == 20) {
                run.accept("kill @e[type=!minecraft:player]");
                run.accept("fill -8 199 -12 8 199 6 minecraft:stone");
                run.accept("fill -8 200 -12 8 206 6 minecraft:air");
                run.accept("tp " + me + " 0.5 200 0.5 0 0");
                run.accept("time set day");
                ClientModules.EMOTES.setEnabled(true);
                ess.ll.client.module.misc.EmotesModule.WHEEL.set(-1);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                for (String h : List.of("EntityESP", "HpAlert", "ShaderESP")) {
                    var m = ClientModules.find(h);
                    if (m != null)
                        m.setEnabled(false);
                }
            }
            int[] cards = Boolean.getBoolean("ess.autoshot.short") ? new int[] { 6 } : new int[] { 0, 3, 6, 9, 10, 11 };
            if (ticks == 40)
                mc.gui.setScreen(new ess.ll.client.screen.EmoteWheelScreen());
            if (ticks == 42) {
                var w = mc.getWindow();
                move(mc, w.getGuiScaledWidth() / 2f + 40, w.getGuiScaledHeight() / 2f - 60);
            }
            if (ticks == 50) {
                shot(mc, "ess-emote-wheel.png");
                System.out.println("[ESS emotetest] screen " + mc.gui.screen());
            }
            if (ticks == 52)
                mc.gui.setScreen(new ess.ll.client.screen.EmotePickerScreen(9));
            for (int i = 0; i < cards.length; i++) {
                int at = 55 + i * 40, card = cards[i];
                if (ticks == at) {
                    var w = mc.getWindow();
                    float px = (w.getGuiScaledWidth() - Math.min(560, w.getGuiScaledWidth() - 30)) / 2f, py = (w.getGuiScaledHeight() - Math.min(340, w.getGuiScaledHeight() - 30)) / 2f;
                    move(mc, px + 12 + 31 + (card % 5) * 68, py + 34 + 26 + 37 + (card / 5) * 80);
                }
                if (ticks == at + 14)
                    shot(mc, "ess-emote-" + i + "-a.png");
                if (ticks == at + 28)
                    shot(mc, "ess-emote-" + i + "-b.png");
            }
            int end = 55 + cards.length * 40;
            if (ticks == end)
                mc.stop();
            return;
        }
        // Resource reload (F3+T): the loading overlay draws through the UI pipeline while shaders are reloaded.
        if (Boolean.getBoolean("ess.autoshot.reloadtest")) {
            if (ticks == 40)
                mc.reloadResourcePacks();
            if (ticks == 44)
                shot(mc, "ess-reload-overlay.png");
            if (ticks > 44 && mc.gui.overlay() == null && ++worldClock == 40) {
                shot(mc, "ess-reload-after.png");
                mc.stop();
            }
            return;
        }
        // HUD cost: every HUD widget on, uncapped frames; with the HUD, with glass panels, and hidden (F1).
        if (Boolean.getBoolean("ess.autoshot.hudperf")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var hud = ClientModules.HUD_ELEMENTS;
            if (ticks == 20) {
                run.accept("gamemode creative " + mc.player.getName().getString());
                run.accept("effect give @p minecraft:speed 300 1");
                run.accept("effect give @p minecraft:night_vision 300");
                run.accept("effect give @p minecraft:regeneration 300");
                ClientModules.HUD.setEnabled(true);
                for (int i = 0; i < hud.options.size(); i++) if (!hud.selected(i))
                    hud.toggle(i);
                mc.options.framerateLimit().set(260);
                mc.options.enableVsync().set(false);
                ClientModules.BACKGROUND_STYLE.select("Обычный");
                hudHidden(mc, false);
            }
            if (ticks == 80)
                ess.ll.client.dev.HudProfile.reset();
            if (ticks == 180) {
                System.out.println("[ESS hudperf] " + ess.ll.client.dev.HudProfile.report("hud"));
                shot(mc, "ess-hudperf.png");
                ClientModules.BACKGROUND_STYLE.select("Блюр");
            }
            if (ticks == 200)
                ess.ll.client.dev.HudProfile.reset();
            if (ticks == 299)
                shot(mc, "ess-hud-glass.png");
            if (ticks == 300) {
                System.out.println("[ESS hudperf] " + ess.ll.client.dev.HudProfile.report("hud+glass"));
                ClientModules.BACKGROUND_STYLE.select("Обычный");
                hudHidden(mc, true);
            }
            if (ticks == 320)
                ess.ll.client.dev.HudProfile.reset();
            // The blur is the default; the saved config must not keep the test's "off".
            if (ticks == 420) {
                System.out.println("[ESS hudperf] " + ess.ll.client.dev.HudProfile.report("hidden"));
                hudHidden(mc, false);
                ClientModules.BACKGROUND_STYLE.select("Блюр");
                mc.stop();
            }
            return;
        }
        // WorldTweaks weather (Zenith): rain by day and night, clear sky, mist, snow (a resource reload paints frost on the
        // foliage, so the clock here stops while the loading overlay is up), stars; then the world fog and grey.
        if (Boolean.getBoolean("ess.autoshot.worldtest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            String me = mc.player.getName().getString();
            var wt = ClientModules.WORLD_TWEAKS;
            java.util.function.BiConsumer<String, Boolean> part = (name, on) -> {
                if (ess.ll.client.module.render.WorldTweaks.PARTS.selected(name) != on)
                    ess.ll.client.module.render.WorldTweaks.PARTS.toggle(ess.ll.client.module.render.WorldTweaks.PARTS.options.indexOf(name));
            };
            if (ticks < 20 || mc.gui.overlay() != null)
                return;
            int t = ++worldClock;
            if (t == 1) {
                run.accept("weather clear");
                run.accept("effect clear " + me);
                run.accept("gamemode spectator " + me);
                run.accept("tp " + me + " " + System.getProperty("ess.autoshot.worldpos", "400 140 400").replace('_', ' ') + " 0 22");
                wt.setEnabled(true);
                for (String name : ess.ll.client.module.render.WorldTweaks.PARTS.options) part.accept(name, name.equals("Погода"));
                ess.ll.client.module.render.WorldTweaks.WEATHER.select("Дождь");
                ess.ll.client.module.render.WorldTweaks.SKY.select("Погода");
                ess.ll.client.module.render.WorldTweaks.HOUR.set(12);
                ess.ll.client.module.render.WorldTweaks.WATER.select("Лужи");
                if (!ess.ll.client.module.render.WorldTweaks.STORM.get())
                    ess.ll.client.module.render.WorldTweaks.STORM.toggle();
            }
            if (t == 50) {
                System.out.println("[ESS worldtest] " + ess.ll.client.weather.Weather.debug());
                shot(mc, "ess-weather-rain.png");
                System.out.println("[ESS worldtest] rain " + mc.level.getRainLevel(1) + " thunder " + mc.level.getThunderLevel(1) + " time " + ess.ll.client.module.render.WorldTweaks.time());
            }
            if (t == 51)
                ess.ll.client.module.render.WorldTweaks.HOUR.set(21);
            if (t == 70)
                shot(mc, "ess-weather-rain-night.png");
            if (t == 71) {
                ess.ll.client.module.render.WorldTweaks.HOUR.set(12);
                ess.ll.client.module.render.WorldTweaks.SKY.select("Ясно");
            }
            if (t == 90)
                shot(mc, "ess-weather-clear.png");
            if (t == 91) {
                ess.ll.client.module.render.WorldTweaks.SKY.select("Погода");
                ess.ll.client.module.render.WorldTweaks.WEATHER.select("Туман");
            }
            if (t == 120)
                shot(mc, "ess-weather-mist.png");
            if (t == 121) {
                ess.ll.client.module.render.WorldTweaks.WEATHER.select("Снег");
                ess.ll.client.module.render.WorldTweaks.HOUR.set(10);
            }
            if (t == 180) {
                shot(mc, "ess-weather-snow.png");
                System.out.println("[ESS worldtest] snow " + ess.ll.client.weather.Weather.debug());
            }
            if (t == 181) {
                ess.ll.client.module.render.WorldTweaks.SKY.select("Звёзды");
                ess.ll.client.module.render.WorldTweaks.HOUR.set(0);
            }
            if (t == 200)
                shot(mc, "ess-weather-stars.png");
            if (t == 201) {
                part.accept("Погода", false);
                part.accept("Туман", true);
                part.accept("Время", true);
                ess.ll.client.module.render.WorldTweaks.WORLD_HOUR.set(18);
                ess.ll.client.module.render.WorldTweaks.FOG_DISTANCE.set(45);
            }
            if (t == 240)
                shot(mc, "ess-world-fog.png");
            if (t == 241) {
                part.accept("Туман", false);
                part.accept("Насыщенность", true);
                ess.ll.client.module.render.WorldTweaks.SATURATION.set(0);
            }
            if (t == 250)
                shot(mc, "ess-world-gray.png");
            if (t == 252) {
                ess.ll.client.module.render.WorldTweaks.SATURATION.set(1);
                part.accept("Насыщенность", false);
                part.accept("Время", false);
                part.accept("Погода", true);
                mc.stop();
            }
            return;
        }
        // Zenith ports: ShaderESP on a zombie and a cow, ShulkerPreview over a dropped shulker and in its tooltip,
        // then AutoBrewing brewing Speed in a stand next to the player and storing it in a chest.
        if (Boolean.getBoolean("ess.autoshot.zenithtest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            String box = "minecraft:purple_shulker_box[minecraft:container=[{slot:0,item:{id:\"minecraft:diamond\",count:12}},{slot:4,item:{id:\"minecraft:golden_apple\",count:3}},{slot:13,item:{id:\"minecraft:totem_of_undying\",count:1}},{slot:26,item:{id:\"minecraft:ender_pearl\",count:16}}]]";
            if (ticks == 20) {
                run.accept("kill @e[type=!minecraft:player]");
                run.accept("gamemode survival " + me);
                run.accept("fill -8 199 -12 8 199 6 minecraft:stone");
                run.accept("fill -8 200 -12 8 206 6 minecraft:air");
                run.accept("tp " + me + " 0.5 200 0.5 180 10");
                run.accept("time set day");
                run.accept("clear " + me);
                run.accept("summon zombie -1.5 200 -4 {NoAI:1b,Silent:1b}");
                run.accept("summon cow 2.5 200 -5 {NoAI:1b,Silent:1b}");
                run.accept("give " + me + " " + box);
                run.accept("setblock 2 200 1 minecraft:brewing_stand");
                run.accept("setblock -2 200 1 minecraft:chest");
                run.accept("give " + me + " minecraft:potion[minecraft:potion_contents={potion:\"minecraft:water\"}] 3");
                run.accept("give " + me + " minecraft:blaze_powder 2");
                run.accept("give " + me + " minecraft:nether_wart 1");
                run.accept("give " + me + " minecraft:sugar 1");
                run.accept("give " + me + " minecraft:glowstone_dust 1");
            }
            if (ticks == 24)
                run.accept("summon item 0 200.3 -3 {Item:{id:\"minecraft:purple_shulker_box\",count:1,components:{\"minecraft:container\":[{slot:0,item:{id:\"minecraft:diamond\",count:12}},{slot:4,item:{id:\"minecraft:golden_apple\",count:3}},{slot:13,item:{id:\"minecraft:totem_of_undying\",count:1}}]}},PickupDelay:32767,NoGravity:1b}");
            if (ticks == 30) {
                ClientModules.SHADER_ESP.setEnabled(true);
                ClientModules.find("EntityESP").setEnabled(false);
                ClientModules.find("HpAlert").setEnabled(false);
                run.accept("effect clear " + me);
                run.accept("effect give " + me + " minecraft:instant_health 1 5");
            }
            if (ticks == 50)
                System.out.println("[ESS zenithtest] esp chain " + mc.getShaderManager().getPostChain(ess.ll.client.module.render.ShaderESP.CHAIN, java.util.Set.of(net.minecraft.client.renderer.PostChain.MAIN_TARGET_ID, net.minecraft.resources.Identifier.withDefaultNamespace("entity_outline"))));
            if (ticks == 31) {
                p.setYRot(180);
                p.setXRot(12);
            }
            if (ticks == 60)
                shot(mc, "ess-zenith-esp.png");
            if (ticks == 61) {
                ess.ll.client.module.render.ShaderESP.COLOR_MODE.select("Здоровье");
                ess.ll.client.module.render.ShaderESP.FILL_OPACITY.set(.8f);
                ess.ll.client.module.render.ShaderESP.GLOW_STRENGTH.set(3);
                ess.ll.client.module.render.ShaderESP.GLOW_RADIUS.set(30);
                ClientModules.SHULKER_PREVIEW.setEnabled(true);
            }
            if (ticks == 70)
                shot(mc, "ess-zenith-esp-health.png");
            if (ticks == 71) {
                ClientModules.SHADER_ESP.setEnabled(false);
                mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
            }
            if (ticks == 74) {
                var w = mc.getWindow();
                int left = (w.getGuiScaledWidth() - 176) / 2, top = (w.getGuiScaledHeight() - 166) / 2;
                int slot = -1;
                for (int i = 0; i < 9; i++) if (ess.ll.client.module.misc.ShulkerPreview.isShulker(p.getInventory().getItem(i)))
                    slot = i;
                System.out.println("[ESS zenithtest] shulker in hotbar slot " + slot);
                move(mc, left + 8 + Math.max(0, slot) * 18 + 8, top + 142 + 8);
            }
            if (ticks == 80)
                shot(mc, "ess-zenith-shulker-tooltip.png");
            if (ticks == 81 && !Boolean.getBoolean("ess.autoshot.brew")) {
                mc.stop();
                return;
            }
            if (ticks == 81) {
                mc.gui.setScreen(null);
                p.setYRot(0);
                p.setXRot(30);
                ess.ll.client.module.player.AutoBrewing.POTION.select("Скорость");
                ClientModules.AUTO_BREWING.setEnabled(true);
            }
            if (ticks > 81 && ticks % 100 == 0)
                System.out.println("[ESS zenithtest] t=" + ticks + " brewing on=" + ClientModules.AUTO_BREWING.enabled() + " screen=" + (mc.gui.screen() == null ? null : mc.gui.screen().getClass().getSimpleName()));
            if (ticks > 100 && ticks < 100000 && (!ClientModules.AUTO_BREWING.enabled() || ticks == 2400)) {
                System.out.println("[ESS zenithtest] brewing finished at t=" + ticks);
                var level = server.overworld();
                server.execute(() -> {
                    var chest = level.getBlockEntity(new net.minecraft.core.BlockPos(-2, 200, 1));
                    if (chest instanceof net.minecraft.world.Container c)
                        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty())
                            System.out.println("[ESS zenithtest] chest " + i + ": " + c.getItem(i) + " " + c.getItem(i).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS));
                    var stand = level.getBlockEntity(new net.minecraft.core.BlockPos(2, 200, 1));
                    if (stand instanceof net.minecraft.world.Container c)
                        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty())
                            System.out.println("[ESS zenithtest] stand " + i + ": " + c.getItem(i));
                });
                shot(mc, "ess-zenith-brewing.png");
                ticks = 100000;
            }
            if (ticks == 100010)
                mc.stop();
            return;
        }
        // Player heads in the inventory: by nick (skins fetched from Mojang) and with a texture value as servers
        // hand them out, to see which ones draw wrong.
        if (Boolean.getBoolean("ess.autoshot.headtest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            if (ticks == 20) {
                run.accept("clear " + me);
                for (String nick : new String[] { "Notch", "jeb_", "Dinnerbone", "Technoblade", "Dream", "MHF_Chest" }) run.accept("give " + me + " minecraft:player_head[profile={name:\"" + nick + "\"}]");
                String tex = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGY0ZjQ0ZjRiOTdmNzc4YjI2N2Q3MjFiNjA1NWRiNWM3NGVkMTA5NWY3YWMwMzE3NmI0ODRhZjBjMmRjMWUyYiJ9fX0=";
                for (int i = 0; i < 4; i++) run.accept("give " + me + " minecraft:player_head[profile={properties:[{name:\"textures\",value:\"" + tex + "\"}]}]");
                run.accept("give " + me + " minecraft:skeleton_skull");
                run.accept("give " + me + " minecraft:player_head");
            }
            if (ticks == 25) {
                // A chest full of different heads, like a server menu.
                String[] nicks = { "Notch", "jeb_", "Dinnerbone", "Technoblade", "Dream", "Grumm", "Herobrine", "Marc", "Xisuma", "Mumbo", "Grian", "Etho", "Iskall85", "TangoTek", "ImpulseSV", "Keralis", "Docm77", "GoodTimesWithScar", "Hypixel", "CaptainSparklez", "Philza", "TommyInnit", "Tubbo", "Ranboo", "Wilbur", "Quackity", "Sapnap", "GeorgeNotFound", "BadBoyHalo", "Skeppy", "AntVenom", "SkyDoesMinecraft", "Vikkstar123", "Preston", "Fruitberries", "Purpled", "Punz", "Seapeekay", "Smajor1995", "Krinios", "Fundy", "Nihachu", "Eret", "Callahan", "HBomb94", "Antfrost", "Awesamdude", "Ponk", "Shubble", "Sneegsnag", "Joel", "Stressmonster101", "Pearlescentmoon", "Rendog" };
                StringBuilder items = new StringBuilder();
                for (int i = 0; i < 27; i++) items.append(i == 0 ? "" : ",").append("{Slot:").append(i).append("b,id:\"minecraft:player_head\",count:1,components:{\"minecraft:profile\":{name:\"").append(nicks[i]).append("\"}}}");
                run.accept("tp " + me + " 0.5 201 0.5 0 0");
                run.accept("setblock 0 201 2 minecraft:chest{Items:[" + items + "]}");
            }
            if (ticks == 120)
                mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
            if (ticks == 140)
                shot(mc, "ess-heads.png");
            if (ticks == 141) {
                mc.gui.setScreen(null);
                lookAt(p, .5, 201.5, 2.5);
            }
            if (ticks == 145)
                mc.gameMode.useItemOn(p, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.5, 201.5, 2), net.minecraft.core.Direction.NORTH, new net.minecraft.core.BlockPos(0, 201, 2), false));
            if (ticks == 230)
                shot(mc, "ess-heads-chest.png");
            if (ticks == 240)
                mc.stop();
            return;
        }
        // ItemScroller in the player's own inventory, through real screen events: the wheel over a hotbar stack
        // moves one item into the main inventory, Shift + left drag moves two main stacks to the hotbar.
        if (Boolean.getBoolean("ess.autoshot.scrolltest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            if (ticks == 20)
                ClientModules.ITEM_SCROLLER.setEnabled(true);
            // Own inventory: no container, so the wheel trades between the hotbar and the main inventory.
            if (ticks == 20) {
                run.accept("clear " + me);
                run.accept("give " + me + " minecraft:cobblestone 20");
                run.accept("give " + me + " minecraft:dirt 10");
                run.accept("item replace entity " + me + " inventory.5 with minecraft:stone 7");
                run.accept("item replace entity " + me + " inventory.6 with minecraft:sand 9");
            }
            if (ticks == 38) {
                var sb = new StringBuilder();
                for (int i = 0; i < 36; i++) if (!p.getInventory().getItem(i).isEmpty())
                    sb.append(i).append(":").append(p.getInventory().getItem(i)).append(" ");
                System.out.println("[ESS chesttest] inventory before: " + sb + " screen=" + mc.gui.screen() + " menu=" + p.containerMenu);
            }
            if (ticks == 40)
                mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
            if (ticks == 48 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv) {
                var hotbar = inv.getMenu().slots.stream().filter(sl -> sl.container instanceof net.minecraft.world.entity.player.Inventory && sl.getContainerSlot() < 9 && sl.hasItem()).findFirst().orElse(null);
                if (hotbar == null)
                    System.out.println("[ESS chesttest] inventory: nothing in the hotbar");
                else
                    try {
                        var lf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("leftPos");
                        lf.setAccessible(true);
                        var tf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("topPos");
                        tf.setAccessible(true);
                        boolean handled = inv.mouseScrolled(lf.getInt(inv) + hotbar.x + 8, tf.getInt(inv) + hotbar.y + 8, 0, -1);
                        System.out.println("[ESS chesttest] inventory wheel handled=" + handled + " hotbar stack now " + hotbar.getItem().getCount());
                    } catch (ReflectiveOperationException e) {
                        System.out.println("[ESS chesttest] " + e);
                    }
            }
            // Shift + left drag across two main-inventory stacks: each moves to the hotbar.
            if (ticks == 54 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv)
                try {
                    var lf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("leftPos");
                    lf.setAccessible(true);
                    var tf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("topPos");
                    tf.setAccessible(true);
                    var info = new net.minecraft.client.input.MouseButtonInfo(0, com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT);
                    for (int c : new int[] { 14, 15 }) {
                        var sl = inv.getMenu().slots.stream().filter(x -> x.container instanceof net.minecraft.world.entity.player.Inventory && x.getContainerSlot() == c).findFirst().get();
                        inv.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(lf.getInt(inv) + sl.x + 8, tf.getInt(inv) + sl.y + 8, info), 1, 0);
                    }
                    inv.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(0, 0, info));
                } catch (ReflectiveOperationException e) {
                    System.out.println("[ESS chesttest] " + e);
                }
            if (ticks == 62 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv) {
                int main = 0;
                for (var sl : inv.getMenu().slots) if (sl.container instanceof net.minecraft.world.entity.player.Inventory && sl.getContainerSlot() >= 9 && sl.getContainerSlot() < 36)
                    main += sl.getItem().getCount();
                System.out.println("[ESS chesttest] inventory main now " + main + " crafting " + inv.getMenu().getCraftSlots().getItems() + " (stone and sand in main slots 14/15 moved by shift drag: " + p.getInventory().getItem(14) + ", " + p.getInventory().getItem(15) + ")");
                shot(mc, "ess-scroll-inventory.png");
            }
            if (ticks == 70)
                mc.stop();
            return;
        }
        // BetterChest on a real chest: the bar, search with E typed into it, ItemScroller's wheel, then take, store
        // and throw. Counts of items in the chest and in the inventory are printed after each step.
        if (Boolean.getBoolean("ess.autoshot.chesttest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            if (ticks == 20) {
                run.accept("tp " + me + " 0.5 201 0.5 0 0");
                run.accept("fill -6 200 -6 6 200 6 minecraft:smooth_stone");
                run.accept("fill -6 201 -6 6 205 6 minecraft:air");
                run.accept("setblock 0 201 2 minecraft:chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:12},{Slot:4b,id:\"minecraft:diamond_sword\",count:1},{Slot:9b,id:\"minecraft:oak_log\",count:32},{Slot:13b,id:\"minecraft:diamond_block\",count:3},{Slot:20b,id:\"minecraft:apple\",count:5}]}");
                run.accept("clear " + me);
                run.accept("give " + me + " minecraft:cobblestone 40");
                run.accept("give " + me + " minecraft:torch 16");
                ClientModules.BETTER_CHEST.setEnabled(true);
                ClientModules.ITEM_SCROLLER.setEnabled(true);
            }
            if (ticks == 40) {
                lookAt(p, .5, 201.5, 2.5);
                mc.gameMode.useItemOn(p, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.5, 201.5, 2), net.minecraft.core.Direction.NORTH, new net.minecraft.core.BlockPos(0, 201, 2), false));
            }
            if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen chest) {
                java.util.function.IntSupplier inChest = () -> {
                    int n = 0;
                    for (var sl : chest.getMenu().slots) if (!(sl.container instanceof net.minecraft.world.entity.player.Inventory))
                        n += sl.getItem().getCount();
                    return n;
                };
                java.util.function.IntSupplier inInv = () -> {
                    int n = 0;
                    for (var sl : chest.getMenu().slots) if (sl.container instanceof net.minecraft.world.entity.player.Inventory)
                        n += sl.getItem().getCount();
                    return n;
                };
                if (ticks == 55) {
                    shot(mc, "ess-chest-bar.png");
                    System.out.println("[ESS chesttest] open chest=" + inChest.getAsInt() + " inv=" + inInv.getAsInt());
                }
                if (ticks == 56)
                    for (var child : chest.children()) if (child instanceof net.minecraft.client.gui.components.EditBox box) {
                        chest.setFocused(box);
                        box.setFocused(true);
                        box.insertText("diamond");
                        chest.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_E, 0, 0));
                    }
                if (ticks == 62) {
                    shot(mc, "ess-chest-search.png");
                    System.out.println("[ESS chesttest] after E still open=" + (mc.gui.screen() == chest));
                }
                if (ticks == 63) {
                    for (var child : chest.children()) if (child instanceof net.minecraft.client.gui.components.EditBox box) {
                        box.setValue("");
                        box.setFocused(false);
                        chest.setFocused(null);
                    }
                    // Through the screen, as a real wheel turn arrives: over the logs' slot (leftPos/topPos via the slot's screen spot).
                    var logs = chest.getMenu().slots.get(9);
                    int left = (chest.width - 176) / 2, top = (chest.height - 166 - 3 * 18 + 54) / 2;
                    double mx = 0, my = 0;
                    try {
                        var lf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("leftPos");
                        lf.setAccessible(true);
                        var tf = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("topPos");
                        tf.setAccessible(true);
                        mx = lf.getInt(chest) + logs.x + 8;
                        my = tf.getInt(chest) + logs.y + 8;
                    } catch (ReflectiveOperationException e) {
                        System.out.println("[ESS chesttest] no leftPos: " + e);
                    }
                    boolean handled = chest.mouseScrolled(mx, my, 0, -1);
                    System.out.println("[ESS chesttest] wheel event at " + mx + "," + my + " handled=" + handled + " enabled=" + ClientModules.ITEM_SCROLLER.enabled());
                }
                if (ticks == 68)
                    System.out.println("[ESS chesttest] wheel chest=" + inChest.getAsInt() + " inv=" + inInv.getAsInt() + " logs=" + chest.getMenu().slots.get(9).getItem().getCount());
                if (ticks == 70)
                    ess.ll.client.module.misc.BetterChest.runForTest(chest, 0);
                if (ticks == 85) {
                    shot(mc, "ess-chest-take.png");
                    System.out.println("[ESS chesttest] take chest=" + inChest.getAsInt() + " inv=" + inInv.getAsInt());
                }
                if (ticks == 86)
                    ess.ll.client.module.misc.BetterChest.runForTest(chest, 1);
                if (ticks == 100)
                    System.out.println("[ESS chesttest] store chest=" + inChest.getAsInt() + " inv=" + inInv.getAsInt());
                if (ticks == 101)
                    ess.ll.client.module.misc.BetterChest.runForTest(chest, 2);
                if (ticks == 115)
                    System.out.println("[ESS chesttest] throw chest=" + inChest.getAsInt() + " inv=" + inInv.getAsInt());
            }
            if (ticks == 120)
                mc.stop();
            return;
        }
        // New modules together: CrystalPosESP spots around a bot on obsidian, EntityESP plates with blur and glow
        // (a friend in green, a § team prefix), typing in chat, HpAlert at low health and BPS in the watermark.
        if (Boolean.getBoolean("ess.autoshot.newtest")) {
            var server = mc.getSingleplayerServer();
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            var p = mc.player;
            String me = p.getName().getString();
            if (ticks == 20) {
                run.accept("kill @e[type=!minecraft:player]");
                run.accept("gamemode survival " + me);
                run.accept("time set day");
                run.accept("tp " + me + " 0.5 201 0.5 0 0");
                run.accept("fill -16 200 -16 16 200 16 minecraft:smooth_stone");
                run.accept("fill -4 200 1 4 200 7 minecraft:obsidian");
                run.accept("fill -16 201 -16 16 206 16 minecraft:air");
                run.accept("team add vip");
                run.accept("team modify vip prefix \"§c● §6VIP §7\"");
                if (server != null) {
                    server.publishServer(net.minecraft.server.MinecraftServer.MultiplayerScope.LAN, false, 25599);
                    server.setUsesAuthentication(false);
                }
                ess.ll.client.command.ClientData.addFriend("ess_friend");
                for (var m : new ClientModules.Module[] { ClientModules.CRYSTAL_POS_ESP, ClientModules.find("EntityESP"), ClientModules.HP_ALERT }) m.setEnabled(true);
                ess.ll.client.module.render.HpAlert.MODE.select("Оба");
            }
            if (ticks == 30) {
                ess.ll.client.bots.Bots.add("ess_target", "localhost:25599");
                ess.ll.client.bots.Bots.add("ess_friend", "localhost:25599");
            }
            if (ticks == 60 || ticks == 100) {
                run.accept("tp ess_target 0.5 201 5.5");
                run.accept("tp ess_friend -3.5 201 6.5");
                run.accept("team join vip ess_target");
            }
            if (ticks == 105)
                lookAt(p, 0, 200.5, 4.5);
            if (ticks == 115) {
                shot(mc, "ess-new-crystal.png");
                System.out.println("[ESS newtest] crystal spots " + ess.ll.client.module.render.CrystalPosESP.shown());
            }
            if (ticks == 116)
                lookAt(p, -1.5, 202, 6);
            if (ticks == 120)
                shot(mc, "ess-new-esp-row.png");
            if (ticks == 121)
                for (var setting : ClientModules.find("EntityESP").settings) if (setting.name().equals("Плашка") && setting instanceof ess.ll.client.setting.Settings.ModeSettings mode)
                    mode.select("Классика");
            if (ticks == 125)
                shot(mc, "ess-new-esp-classic.png");
            if (ticks == 130)
                mc.gui.setScreen(new net.minecraft.client.gui.screens.ChatScreen("", false));
            String typed = "Привет, ESS typing!";
            if (ticks >= 135 && ticks < 135 + typed.length() && mc.gui.screen() != null)
                for (var child : mc.gui.screen().children()) if (child instanceof net.minecraft.client.gui.components.EditBox box)
                    box.insertText(typed.substring(ticks - 135, ticks - 134));
            if (ticks == 135 + typed.length())
                shot(mc, "ess-new-typing.png");
            if (ticks == 160) {
                mc.gui.setScreen(null);
                run.accept("damage " + me + " 15 minecraft:generic");
            }
            if (ticks == 175) {
                shot(mc, "ess-new-hp.png");
                System.out.println("[ESS newtest] hp " + p.getHealth() + " alert " + ess.ll.client.module.render.HpAlert.shown());
            }
            if (ticks == 180)
                mc.stop();
            return;
        }
        // GriefJoiner against a stand-in hub: the compass sits outside the hotbar, the mode menu has the crafting
        // table, griefs 1–88 are split 30/30/28 over three pages with arrows; the click on #47 is what passes.
        if (Boolean.getBoolean("ess.autoshot.grieftest")) {
            var player = mc.player;
            var server = mc.getSingleplayerServer();
            if (ticks == 25 && server != null)
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "item replace entity " + player.getName().getString() + " inventory.11 with compass"));
            if (ticks == 40) {
                ess.ll.client.module.misc.GriefJoiner.GRIEF.set(47);
                ess.ll.client.module.misc.GriefJoiner.DELAY.set(150);
                ClientModules.GRIEF_JOINER.setEnabled(true);
            }
            if (ticks == 55)
                griefMenu(mc, 0);
            var carried = player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                String name = carried.getHoverName().getString();
                player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                if (carried.is(net.minecraft.world.item.Items.CRAFTING_TABLE))
                    griefMenu(mc, 1);
                else if (carried.is(net.minecraft.world.item.Items.ARROW))
                    griefMenu(mc, griefPage + 1);
                else {
                    System.out.println("[ESS grieftest] clicked " + name + " on page " + griefPage + (name.equals("Гриф #47") ? " PASS" : " FAIL"));
                    if (ticks < 9000)
                        shot(mc, "ess-grief-page.png");
                    player.closeContainer();
                    griefDone = ticks;
                }
            }
            if (griefDone > 0 && ticks == griefDone + 25)
                shot(mc, "ess-grief-done.png");
            if (griefDone > 0 && ticks == griefDone + 30 || ticks == 600) {
                if (griefDone == 0)
                    System.out.println("[ESS grieftest] FAIL timeout");
                mc.stop();
            }
            return;
        }
        // Servers page with the ViaFabricPlus version button, then the picker it opens.
        if (Boolean.getBoolean("ess.autoshot.viatest")) {
            if (ticks == 30)
                mc.gui.setScreen(new MainMenuScreen());
            if (ticks == 40 && mc.gui.screen() instanceof MainMenuScreen menu)
                menu.showPage(2);
            if (ticks == 90)
                shot(mc, "ess-via-servers.png");
            // The picker pauses the world, so ticks stop: its shot is timed by the clock instead.
            if (ticks == 91 && mc.gui.screen() instanceof MainMenuScreen menu) {
                menu.openViaForTest();
                Thread.startVirtualThread(() -> {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException ignored) {
                    }
                    mc.execute(() -> shot(mc, "ess-via-picker.png"));
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }
                    mc.execute(mc::stop);
                });
            }
            return;
        }
        // Main menu backdrops: the «Фон меню» page, then the home page over every shader, and one in the light theme.
        if (Boolean.getBoolean("ess.autoshot.bgtest")) {
            var styles = ess.ll.client.render.MenuScene.Style.values();
            if (ticks == 30) {
                bgBefore = MainMenuScreen.backgroundForTest(styles[2]);
                mc.gui.setScreen(new MainMenuScreen());
            }
            if (mc.gui.screen() instanceof MainMenuScreen menu) {
                if (ticks == 31) {
                    menu.showPage(3);
                    menu.showCategory("Фон меню");
                }
                if (ticks == 60)
                    shot(mc, "ess-bg-options.png");
                if (ticks == 61)
                    menu.showPage(0);
                for (int i = 2; i < styles.length; i++) {
                    int at = 61 + (i - 2) * 30;
                    if (ticks == at)
                        MainMenuScreen.backgroundForTest(styles[i]);
                    if (ticks == at + 25) {
                        shot(mc, "ess-bg-" + styles[i].name().toLowerCase(java.util.Locale.ROOT) + ".png");
                        System.out.println("[ESS autoshot] bg " + styles[i] + " fps " + mc.getFps() + " scale " + ess.ll.client.render.MenuScene.scale());
                    }
                }
                int light = 61 + (styles.length - 2) * 30;
                if (ticks == light) {
                    MainMenuScreen.backgroundForTest(styles[2]);
                    menu.toggleTheme();
                }
                if (ticks == light + 25)
                    shot(mc, "ess-bg-light.png");
                if (ticks == light + 26) {
                    menu.toggleTheme();
                    MainMenuScreen.backgroundForTest(bgBefore);
                    mc.stop();
                }
            }
            return;
        }
        // Main menu: every page in the dark theme, then the home page in the light one.
        if (Boolean.getBoolean("ess.autoshot.menutest")) {
            if (ticks == 30) {
                // A couple of servers in the test profile so the list has rows to show.
                var list = new net.minecraft.client.multiplayer.ServerList(mc);
                list.load();
                if (list.size() == 0) {
                    list.add(new net.minecraft.client.multiplayer.ServerData("Hypixel", "mc.hypixel.net", net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
                    list.add(new net.minecraft.client.multiplayer.ServerData("Локальный", "localhost", net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
                    list.save();
                }
                mc.gui.setScreen(new MainMenuScreen());
            }
            if (mc.gui.screen() instanceof MainMenuScreen menu) {
                if (ticks == 60)
                    shot(mc, "ess-menu-home.png");
                if (ticks == 61) {
                    boolean hit = menu.clickForTest(.5f, .52f);
                    System.out.println("[ESS menu] click " + hit + " page " + menu.pageForTest());
                    menu.showPage(1);
                }
                if (ticks == 80)
                    shot(mc, "ess-menu-single.png");
                if (ticks == 81)
                    menu.showPage(2);
                if (ticks == 120)
                    shot(mc, "ess-menu-multi.png");
                if (ticks == 121)
                    menu.showPage(3);
                if (ticks == 140)
                    shot(mc, "ess-menu-options.png");
                if (ticks == 141)
                    menu.showCategory(2);
                if (ticks == 155)
                    shot(mc, "ess-menu-controls.png");
                if (ticks == 156)
                    menu.showServerForm();
                if (ticks == 175)
                    shot(mc, "ess-menu-server.png");
                if (ticks == 176)
                    menu.showWorldForm();
                if (ticks == 230)
                    shot(mc, "ess-menu-world.png");
                if (ticks == 231)
                    menu.showPage(6);
                if (ticks == 255)
                    shot(mc, "ess-menu-accounts.png");
                if (ticks == 256) {
                    menuBackground = MainMenuScreen.backgroundForTest(ess.ll.client.render.MenuScene.Style.AURORA);
                    menu.showPage(0);
                }
                if (ticks == 275)
                    shot(mc, "ess-menu-blur-home.png");
                if (ticks == 276)
                    menu.showPage(2);
                if (ticks == 295)
                    shot(mc, "ess-menu-blur-multi.png");
                if (ticks == 296) {
                    MainMenuScreen.backgroundForTest(menuBackground);
                    mc.stop();
                }
            }
            return;
        }
        // The last module going off: its row and the KeyBinds plate fade out, frame by frame.
        if (Boolean.getBoolean("ess.autoshot.fadetest")) {
            var server = mc.getSingleplayerServer();
            if (ticks == 20 && server != null)
                server.execute(() -> {
                    for (String c : new String[] { "time set day", "tp @a 120.5 200 57.5 0 15" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                });
            if (ticks == 30) {
                for (var m : ClientModules.all()) if (m != ClientModules.HUD && m != ClientModules.THEME && m != ClientModules.COSMETICS && m != ClientModules.CLICK_GUI && m.enabled())
                    m.setEnabled(false);
                ClientModules.find("Sprint").bind = 82;
                ClientModules.find("Sprint").setEnabled(true);
            }
            if (ticks == 60)
                shot(mc, "ess-fade-60.png");
            if (ticks == 62) {
                System.out.println("[ESS fade] off");
                ClientModules.find("Sprint").setEnabled(false);
            }
            if (ticks >= 63 && ticks <= 72)
                shot(mc, "ess-fade-" + ticks + ".png");
            if (ticks == 100) {
                shot(mc, "ess-fade-100.png");
                ClientModules.find("Sprint").bind = -1;
            }
            if (ticks == 101)
                mc.stop();
            return;
        }
        // KeyBinds and TargetHUD on their dark plates: both TargetHUD styles, then a bind turning off.
        if (Boolean.getBoolean("ess.autoshot.bindstest")) {
            var server = mc.getSingleplayerServer();
            var p = mc.player;
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            if (ticks == 20)
                for (String c : new String[] { "gamemode survival @a", "time set day", "clear @a", "effect give @a resistance 9999 4 true", "item replace entity @a hotbar.0 with minecraft:diamond_sword", "tp @a 120.5 200 57.5 0 15" }) run.accept(c);
            if (ticks == 30) {
                p.setYRot(0);
                p.setXRot(15);
                ess.ll.client.command.DotCommands.execute(".fakeplayer add Dummy");
            }
            if (ticks == 31)
                run.accept("tp @a 120.5 200 54.5 0 15");
            if (ticks == 35) {
                ClientModules.find("Sprint").setEnabled(true);
                ClientModules.find("Sprint").bind = 82;
                ClientModules.find("AutoJump").setEnabled(true);
                ClientModules.find("AutoJump").bind = 74;
                ClientModules.find("Aura").setEnabled(true);
                ClientModules.find("Aura").bind = 73;
                ClientModules.TARGET_STYLE.select("Круг");
            }
            if (ticks == 36 || ticks == 37 || ticks == 39)
                shot(mc, "ess-binds-in" + ticks + ".png");
            if (ticks == 60) {
                System.out.println("[ESS binds] target=" + ess.ll.client.module.combat.AttackAura.target);
                shot(mc, "ess-binds-ring.png");
            }
            if (ticks == 61)
                ClientModules.TARGET_STYLE.select("Полоска");
            if (ticks == 80)
                shot(mc, "ess-binds-bar.png");
            if (ticks == 81)
                ClientModules.find("AutoJump").setEnabled(false);
            if (ticks == 82)
                shot(mc, "ess-binds-leaving.png");
            if (ticks == 95)
                shot(mc, "ess-binds-after.png");
            if (ticks == 100) {
                for (String m : new String[] { "Sprint", "AutoJump", "Aura" }) ClientModules.find(m).bind = -1;
                ClientModules.find("Aura").setEnabled(false);
                ess.ll.client.features.FakePlayers.remove("all");
                mc.stop();
            }
            return;
        }
        // CrystalAuraV2 on a fake target on an obsidian floor, then AntiCrystal covering the same floor.
        if (Boolean.getBoolean("ess.autoshot.crystalv2test")) {
            var server = mc.getSingleplayerServer();
            var p = mc.player;
            java.util.function.Consumer<String> run = c -> {
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c));
            };
            if (ticks == 20)
                for (String c : new String[] { "gamemode survival @a", "time set day", "clear @a", "effect give @a resistance 9999 4 true", "fill 110 199 47 130 199 67 minecraft:obsidian", "fill 110 200 47 130 206 67 minecraft:air", "item replace entity @a hotbar.1 with minecraft:end_crystal 64", "item replace entity @a hotbar.2 with minecraft:obsidian 64", "tp @a 120.5 200 59.5 0 15" }) run.accept(c);
            if (ticks == 30) {
                p.setYRot(0);
                p.setXRot(15);
                ess.ll.client.command.DotCommands.execute(".fakeplayer add Dummy");
            }
            if (ticks == 31)
                run.accept("tp @a 120.5 200 55.5 0 15");
            // The player stays put: the blasts would carry them out of range.
            if (ticks > 36) {
                p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                p.setPos(120.5, 200, 55.5);
            }
            // With a second module named, the aura has to let WebTrap finish first.
            String webAura = System.getProperty("ess.autoshot.webaura");
            if (ticks == 21 && webAura != null)
                run.accept("item replace entity @a hotbar.3 with minecraft:cobweb 64");
            if (webAura != null && ticks > 36 && ticks % 8 == 0)
                System.out.println("[ESS web] t=" + ticks + " wait " + ess.ll.client.util.ModuleManager.INSTANCE.webTrap().crystalsWait());
            if (ticks == 36 && webAura != null) {
                p.getInventory().setSelectedSlot(1);
                for (var setting : ClientModules.find(webAura).settings) if (setting instanceof ess.ll.client.setting.Settings.BooleanSetting toggle && setting.name().equals("Ставить обсидиан") && !toggle.get())
                    toggle.toggle();
                if (!Boolean.getBoolean("ess.autoshot.webnone"))
                    ClientModules.find("WebTrap").setEnabled(true);
                ClientModules.find(webAura).setEnabled(true);
                System.out.println("[ESS web] on " + ClientModules.find("WebTrap").enabled() + " " + ClientModules.find(webAura).enabled());
            }
            if (webAura != null && ticks > 36 && ticks <= 90)
                for (var pos : net.minecraft.core.BlockPos.betweenClosed(119, 200, 58, 121, 201, 60)) {
                    var state = mc.level.getBlockState(pos).getBlock();
                    var key = pos.immutable();
                    var before = obbyBlocks.put(key, state);
                    if (before != null && before != state)
                        System.out.println("[ESS web] t=" + ticks + " " + key.toShortString() + " " + before.getName().getString() + " -> " + state.getName().getString());
                }
            if (webAura != null && ticks == 91) {
                ClientModules.find("WebTrap").setEnabled(false);
                ClientModules.find(webAura).setEnabled(false);
                ess.ll.client.features.FakePlayers.remove("all");
                mc.stop();
                return;
            }
            if (ticks == 36 && webAura == null) {
                ClientModules.find("CrystalAuraV2").setEnabled(true);
                System.out.println("[ESS v2] on " + ClientModules.find("CrystalAuraV2").enabled() + " old " + ClientModules.find("CrystalAura").enabled());
            }
            if (ticks > 36 && ticks <= 240) {
                for (var c : mc.level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, p.getBoundingBox().inflate(10))) if (obbyCrystals.add(c.getId()))
                    System.out.println("[ESS v2] t=" + ticks + " crystal " + c.blockPosition().toShortString());
                String state = ess.ll.client.features.FakePlayers.stateForTest();
                if (!state.equals(obbyState)) {
                    System.out.println("[ESS v2] t=" + ticks + " fake " + state);
                    obbyState = state;
                }
            }
            // The four box colours of the aura, away from the blasts.
            if (ticks == 33) {
                var demo = new ess.ll.client.features.crystalv2.V2Visuals();
                demo.add(new net.minecraft.core.BlockPos(117, 201, 60), 1, 0, 0);
                demo.add(new net.minecraft.core.BlockPos(119, 202, 60), .2f, .6f, 1);
                demo.add(new net.minecraft.core.BlockPos(121, 202, 60), .68f, .58f, 1);
                demo.add(new net.minecraft.core.BlockPos(123, 201, 60), 1, .95f, 0);
            }
            for (int t : new int[] { 34, 36, 38, 42, 46, 52, 60, 90 }) if (ticks == t)
                shot(mc, "ess-v2-" + t + ".png");
            if (ticks == 140) {
                ClientModules.find("CrystalAuraV2").setEnabled(false);
                run.accept("kill @e[type=minecraft:end_crystal]");
                run.accept("fill 110 200 47 130 206 67 minecraft:air");
            }
            if (ticks == 142)
                run.accept("effect clear @a");
            if (ticks == 145) {
                obbyBase = p.blockPosition();
                obbyBlocks.clear();
                ClientModules.find("AntiCrystal").setEnabled(true);
            }
            if (ticks >= 145 && ticks <= 240)
                for (var pos : net.minecraft.core.BlockPos.betweenClosed(obbyBase.offset(-6, 0, -6), obbyBase.offset(6, 0, 6))) {
                    var state = mc.level.getBlockState(pos).getBlock();
                    var key = pos.immutable();
                    var before = obbyBlocks.put(key, state);
                    if (before != null && before != state)
                        System.out.println("[ESS anti] t=" + ticks + " " + key.toShortString() + " " + before.getName().getString() + " -> " + state.getName().getString());
                }
            if (ticks == 150)
                shot(mc, "ess-anti-150.png");
            if (ticks == 240) {
                ClientModules.find("AntiCrystal").setEnabled(false);
                ess.ll.client.features.FakePlayers.remove("all");
                mc.stop();
            }
            return;
        }
        // Hands in every mode with a sword in hand, then CreamyKeys and AimingItems turning towards a dropped elytra.
        if (Boolean.getBoolean("ess.autoshot.handstest")) {
            var server = mc.getSingleplayerServer();
            var mode = (ess.ll.client.setting.Settings.ModeSettings) ess.ll.client.module.render.Hands.INSTANCE.settings[0];
            String[] modes = { "Заливка", "Шейдер", "Зеркало", "Шейдер + Зеркало", "Nothing" };
            if (ticks == 35) {
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "time set noon", "weather clear", "item replace entity @p weapon.mainhand with minecraft:diamond_sword" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
            }
            if (ticks == 45)
                shot(mc, "ess-hands-off.png");
            if (ticks == 46)
                ClientModules.HANDS.setEnabled(true);
            for (int i = 0; i < modes.length; i++) {
                if (ticks == 47 + i * 12)
                    mode.select(modes[i]);
                if (ticks == 57 + i * 12)
                    shot(mc, "ess-hands-" + i + ".png");
            }
            if (ticks == 110) {
                ClientModules.CREAMY_KEYS.setEnabled(true);
                ess.ll.client.module.misc.CreamyKeys.onKey(65, 1);
                ess.ll.client.module.misc.CreamyKeys.onMouse(0, 1);
                ClientModules.AIMING_ITEMS.setEnabled(true);
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "execute as @p at @p positioned ^4 ^ ^3 run summon minecraft:item ~ ~ ~ {Item:{id:\"minecraft:elytra\",count:1},PickupDelay:32767,Age:-32768}"));
            }
            if (ticks == 112)
                System.out.println("[handstest] " + String.format("aiming before: yaw %s", mc.player.getYRot()));
            if (ticks == 140) {
                System.out.println("[handstest] " + String.format("aiming after: yaw %s", mc.player.getYRot()));
                shot(mc, "ess-aiming.png");
            }
            if (ticks == 141) {
                System.out.println("[handstest] " + String.format("pvpsafe /hub=%s /spawn=%s active=%s", ess.ll.client.module.misc.PvpSafe.isExitCommand("/hub"), ess.ll.client.module.misc.PvpSafe.isExitCommand("/spawn"), ess.ll.client.module.misc.PvpSafe.isPvpProtectionActive()));
                ClientModules.HANDS.setEnabled(false);
                ClientModules.CREAMY_KEYS.setEnabled(false);
                ClientModules.AIMING_ITEMS.setEnabled(false);
            }
            if (ticks == 145)
                mc.stop();
            return;
        }
        // TargetHUD fireflies on the player preview: the idle drift, then the burst from a hit.
        if (Boolean.getBoolean("ess.autoshot.fireflytest")) {
            var server = mc.getSingleplayerServer();
            if (ticks == 35) {
                if (!ClientModules.HUD_ELEMENTS.selected("TargetHud"))
                    ClientModules.HUD_ELEMENTS.toggle(ClientModules.HUD_ELEMENTS.options.indexOf("TargetHud"));
                if (!ClientModules.TARGET_PARTICLES.get())
                    ClientModules.TARGET_PARTICLES.toggle();
                ClientModules.TARGET_PARTICLE_MODE.select("Светлячки");
                // Survival, so damage really takes health; with chat open the card previews the player itself.
                // Resistance V saved in the world by the crystal run would absorb every hit; health carries over
                // between runs, so it is topped up first.
                if (server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "gamemode survival @a", "effect clear @a", "effect give @a minecraft:instant_health 1 10" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
                mc.gui.setScreen(new ChatScreen("", false));
            }
            if (ticks == 96)
                shot(mc, "ess-fireflies.png");
            if (ticks == 100 && server != null)
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "damage @p 6 minecraft:magic"));
            if (ticks == 103)
                shot(mc, "ess-fireflies-burst.png");
            if (ticks == 113)
                shot(mc, "ess-fireflies-ring.png");
            if (ticks == 114) {
                ClientModules.TARGET_STYLE.select("Полоска");
                if (server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "damage @p 4 minecraft:magic"));
            }
            if (ticks == 118)
                shot(mc, "ess-fireflies-bar.png");
            if (ticks == 120) {
                ClientModules.TARGET_STYLE.select("Круг");
                mc.stop();
            }
            // Spawn protection would swallow early damage, so this run waits and skips the shared steps below.
            return;
        }
        // HUD widgets: binds and effects in the world, then every settings popup in chat.
        if (Boolean.getBoolean("ess.autoshot.hudtest")) {
            var server = mc.getSingleplayerServer();
            if (ticks == 30 && server != null)
                server.execute(() -> {
                    for (String c : new String[] { "kill @e[type=minecraft:husk]", "effect give @a minecraft:speed 95 1", "effect give @a minecraft:fire_resistance 420", "effect give @a minecraft:regeneration 8", "effect give @a minecraft:night_vision 200", "item replace entity @a armor.head with minecraft:diamond_helmet[damage=30]", "item replace entity @a armor.chest with minecraft:diamond_chestplate[damage=200]", "item replace entity @a armor.legs with minecraft:iron_leggings[damage=150]", "item replace entity @a armor.feet with minecraft:golden_boots[damage=80]" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                });
            if (ticks == 32) {
                var sprint = ClientModules.find("Sprint");
                sprint.bind = InputConstants.KEY_R;
                if (!sprint.enabled())
                    sprint.toggle();
                var jump = ClientModules.find("AutoJump");
                jump.bind = InputConstants.KEY_J;
                if (!jump.enabled())
                    jump.toggle();
                ClientModules.find("Gamma").bind = InputConstants.KEY_G;
                if (!ClientModules.HUD_ELEMENTS.selected("KeyBinds"))
                    ClientModules.HUD_ELEMENTS.toggle(ClientModules.HUD_ELEMENTS.options.indexOf("KeyBinds"));
                if (!ClientModules.HUD_ELEMENTS.selected("Player"))
                    ClientModules.HUD_ELEMENTS.toggle(ClientModules.HUD_ELEMENTS.options.indexOf("Player"));
                if (!ClientModules.HUD_ELEMENTS.selected("Notifications"))
                    ClientModules.HUD_ELEMENTS.toggle(ClientModules.HUD_ELEMENTS.options.indexOf("Notifications"));
                if (!ClientModules.HUD_ELEMENTS.selected("Armor"))
                    ClientModules.HUD_ELEMENTS.toggle(ClientModules.HUD_ELEMENTS.options.indexOf("Armor"));
                ess.ll.client.media.MediaBridge.fake("Midnight City", "M83", "Hurry Up, We're Dreaming", 2_000, 243_000);
                var lines = new java.util.ArrayList<ess.ll.client.media.Lyrics.Line>();
                String[] text = { "Waiting in a car", "Waiting for a ride in the dark", "The night city grows, the lights are calling out to me again tonight", "Look and see her eyes, they glow", "Waiting in a car", "Waiting for the right time" };
                for (int i = 0; i < text.length; i++) lines.add(new ess.ll.client.media.Lyrics.Line(i * 3500L, text[i]));
                ess.ll.client.media.Lyrics.put(ess.ll.client.media.MediaBridge.track(), lines);
            }
            // The player and notifications: a stack of lines, a boss bar pushing them down, lyrics off and on.
            if (Boolean.getBoolean("ess.autoshot.islandtest")) {
                if (ticks == 33 && server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect clear @a"));
                if (ticks == 34)
                    ClientModules.find("Gamma").toggle();
                if (ticks == 37)
                    ClientModules.find("AutoJump").toggle();
                if (ticks == 38)
                    ess.ll.client.render.HudNotifications.queue(ess.ll.client.render.Icons.Icon.CLOCK, 0xffe49a3a, "Шалкер", "Ещё на кулдауне");
                if (ticks >= 35 && ticks <= 46)
                    shot(mc, "ess-note-" + ticks + ".png");
                if (ticks == 47)
                    ClientModules.find("AutoJump").toggle();
                if (ticks == 48)
                    ess.ll.client.render.HudNotifications.queue(ess.ll.client.render.Icons.Icon.CHECK, 0, "Шалкер", "Использовано");
                if (ticks == 54)
                    shot(mc, "ess-note-stack.png");
                if (ticks == 60 && Boolean.getBoolean("ess.autoshot.reallyrics"))
                    ess.ll.client.media.MediaBridge.fake("Midnight City (Official Video)", "M83VEVO", "", 30_000, 243_000);
                if (Boolean.getBoolean("ess.autoshot.reallyrics") && ticks % 40 == 0 && ticks > 60)
                    System.out.println("[ESS lyrics] t=" + ticks + " lines " + ess.ll.client.media.Lyrics.of(ess.ll.client.media.MediaBridge.track()).size());
                if (ticks == 100 && server != null)
                    server.execute(() -> {
                        for (String c : new String[] { "bossbar add ess:test \"Тестовый босс\"", "bossbar set ess:test players @a" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    });
                if (ticks == 120)
                    shot(mc, "ess-player-lyrics.png");
                if (ticks == 121)
                    ClientModules.PLAYER_LYRICS.toggle();
                if (ticks == 135)
                    shot(mc, "ess-player-plain.png");
                if (ticks == 136)
                    ClientModules.PLAYER_LYRICS.toggle();
                if (ticks == 150 && server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "bossbar remove ess:test"));
                if (ticks == 200)
                    shot(mc, "ess-player-rest.png");
                if (ticks == 202) {
                    ClientModules.find("Gamma").toggle();
                    mc.stop();
                }
                return;
            }
            if (ticks == 52) {
                ClientModules.WM_NICK.toggle();
            }
            if (ticks == 60)
                shot(mc, "ess-hud2-world.png");
            if (ticks == 61)
                if (ticks == 66)
                    shot(mc, "ess-hud2-wm-segments.png");
            if (ticks == 67) {
                ClientModules.WM_NICK.toggle();
            }
            if (ticks == 62)
                mc.gui.setScreen(new ChatScreen("", false));
            String[] ids = { "keybinds", "potions", "watermark", "targethud" };
            for (int i = 0; i < ids.length; i++) {
                if (ticks == 72 + i * 12)
                    ess.ll.client.render.HudRenderer.popupPoint(ids[i], 0);
                if (ticks == 82 + i * 12)
                    shot(mc, "ess-hud2-popup-" + ids[i] + ".png");
            }
            // TargetHUD styles on the player preview: a hit mid-animation, then settled.
            int t0 = 72 + ids.length * 12;
            String[] styles = { "Полоска", "Круг" };
            for (int i = 0; i < styles.length; i++) {
                int at = t0 + i * 30;
                if (ticks == at) {
                    ess.ll.client.render.HudRenderer.finishEditing();
                    ClientModules.TARGET_STYLE.select(styles[i]);
                }
                if (ticks == at + 2 && server != null)
                    server.execute(() -> {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode survival @a");
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect clear @a");
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect give @a minecraft:instant_health 1 4");
                    });
                if (ticks == at + 6 && server != null)
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "damage @p 6 minecraft:generic"));
                if (ticks == at + 6)
                    return;
                if (ticks == at + 10)
                    shot(mc, "ess-hud2-target-" + i + "-hit.png");
                if (ticks == at + 25)
                    shot(mc, "ess-hud2-target-" + i + ".png");
            }
            if (ticks == t0 + styles.length * 30)
                mc.stop();
            return;
        }
        // Wardrobe: an outfit (or the items in ess.autoshot.wear), the wardrobe tab per category, then the player in
        // third person from the front and the back.
        if (Boolean.getBoolean("ess.autoshot.cosmetictest")) {
            var server = mc.getSingleplayerServer();
            if (ticks == 30 && server != null)
                server.execute(() -> {
                    for (String c : List.of("kill @e[type=!minecraft:player]", "fill -8 199 -12 8 199 6 minecraft:stone", "fill -8 200 -12 8 206 6 minecraft:air", "tp @p 0.5 200 0.5 0 0", "time set day", "effect clear @p", Boolean.getBoolean("ess.autoshot.sword") ? "item replace entity @p weapon.mainhand with minecraft:diamond_sword" : "item replace entity @p weapon.mainhand with minecraft:air", "setblock 2 200 1 minecraft:respawn_anchor[charges=2]", "summon minecraft:end_crystal -1.5 200 1.5 {ShowBottom:0b}")) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                    if (Boolean.getBoolean("ess.autoshot.cosmeticarmor"))
                        for (String c : new String[] { "item replace entity @a armor.head with minecraft:diamond_helmet", "item replace entity @a armor.chest with minecraft:diamond_chestplate", "item replace entity @a armor.feet with minecraft:diamond_boots" }) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                });
            if (ticks == 32) {
                for (String h : List.of("EntityESP", "HpAlert", "ShaderESP")) {
                    var m = ClientModules.find(h);
                    if (m != null)
                        m.setEnabled(false);
                }
                ess.ll.client.wardrobe.Wardrobe.clear();
                for (String id : System.getProperty("ess.autoshot.wear", "char_samurai_wanderer").split(",")) {
                    var item = ess.ll.client.wardrobe.Wardrobe.item(id.trim());
                    System.out.println("[ESS wardrobe] wear " + id + " -> " + item);
                    if (item != null)
                        ess.ll.client.wardrobe.Wardrobe.wear(item);
                }
                var gui = new ClickGuiScreen();
                mc.gui.setScreen(gui);
                gui.show("Cosmetics");
            }
            int categories = ess.ll.client.wardrobe.Wardrobe.Category.values().length + 1;
            for (int i = 0; i < categories; i++) {
                int at = 50 + i * 20;
                if (ticks == at - 19 && mc.gui.screen() instanceof ClickGuiScreen gui)
                    gui.selectWardrobeCategory(i);
                if (ticks == at)
                    shot(mc, "ess-wardrobe-tab-" + i + ".png");
            }
            int end = 50 + categories * 20;
            if (ticks == end) {
                mc.gui.setScreen(null);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            }
            if (ticks == end + 30)
                shot(mc, "ess-wardrobe-world-front.png");
            if (ticks == end + 31)
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            if (ticks == end + 50)
                shot(mc, "ess-wardrobe-world-back.png");
            if (ticks == end + 51)
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            if (ticks == end + 60)
                shot(mc, "ess-wardrobe-first-person.png");
            if (ticks == end + 61) {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                mc.player.setYRot(140);
            }
            if (ticks == end + 120)
                shot(mc, "ess-wardrobe-pet.png");
            if (ticks == end + 121) {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.stop();
            }
            return;
        }
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.inventorytest")) {
            var walk = ess.ll.client.util.ModuleManager.INSTANCE.inventoryMove();
            walk.setEnabled(false);
            walk.unusedOption.setValue(true);
            java.util.concurrent.atomic.AtomicBoolean executed = new java.util.concurrent.atomic.AtomicBoolean();
            ess.ll.client.core.TickTaskScheduler.runInInventory(() -> executed.set(true), 4, false);
            if (!executed.get() || walk.isMoving())
                throw new IllegalStateException("InventoryWalk slowdown leaked while disabled");
            System.out.println("[ESS InventoryWalk] disabled slowdown bypassed");
        }
        if (ticks == 45 && Boolean.getBoolean("ess.autoshot.inventorytest"))
            mc.stop();
        if (ticks == 80 && Boolean.getBoolean("ess.autoshot.guitest"))
            shot(mc, "ess-bind-settings.png");
        if (ticks == 82 && Boolean.getBoolean("ess.autoshot.guitest"))
            mc.stop();
        if (ticks == 49 && Boolean.getBoolean("ess.autoshot.elytratest")) {
            boolean started = mc.player.tryToStartFallFlying();
            if (started)
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            System.out.println("[ESS ElytraHelper] started=" + started + " flying=" + mc.player.isFallFlying());
        }
        if (ticks == 60 && Boolean.getBoolean("ess.autoshot.elytratest")) {
            int before = mc.player.getInventory().getItem(1).getCount();
            for (var setting : ess.ll.client.util.ModuleManager.INSTANCE.elytraHelper().getSettings()) if (setting instanceof ess.ll.client.setting.KeyBindSetting key && setting.getDisplayName().equals("Кнопка фейера"))
                key.getValue().setKey(com.mojang.blaze3d.platform.InputConstants.KEY_F);
            ess.ll.client.event.KeyInputEvent.INSTANCE.fire(com.mojang.blaze3d.platform.InputConstants.KEY_F, ess.ll.client.ui.InputAction.PRESS);
            System.out.println("[ESS ElytraHelper] before=" + before + " flying=" + mc.player.isFallFlying());
        }
        if (ticks == 70 && Boolean.getBoolean("ess.autoshot.elytratest")) {
            System.out.println("[ESS ElytraHelper] after=" + mc.player.getInventory().getItem(1).getCount() + " flying=" + mc.player.isFallFlying());
        }
        if (ticks == 75 && Boolean.getBoolean("ess.autoshot.elytratest"))
            mc.stop();
        if (ticks == 80 && Boolean.getBoolean("ess.autoshot.entityesptest"))
            shot(mc, "ess-entity-esp.png");
        if (ticks == 82 && Boolean.getBoolean("ess.autoshot.entityesptest"))
            mc.stop();
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.effecttest")) {
            ClientModules.find("Particles").setEnabled(true);
            ClientModules.find("Predictions").setEnabled(true);
            mc.player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));
            mc.player.getInventory().setSelectedSlot(0);
        }
        if (ticks == 35 && Boolean.getBoolean("ess.autoshot.targetesptest")) {
            ClientModules.find("CrystalAura").setEnabled(false);
            ClientModules.find("Sprint").setEnabled(false);
            for (var setting : ClientModules.find("Aura").settings) if (setting instanceof ess.ll.client.setting.Settings.MultiBooleanSettings multiple && multiple.options.contains("Мобы") && !multiple.selected("Мобы"))
                multiple.toggle(multiple.options.indexOf("Мобы"));
            ClientModules.find("AutoTotem").setEnabled(true);
            ClientModules.find("TargetESP").setEnabled(true);
            String selected = System.getProperty("ess.autoshot.targetespmode", "Призраки 1");
            for (var setting : ClientModules.find("TargetESP").settings) if (setting.name().equals("Тип") && setting instanceof ess.ll.client.setting.Settings.ModeSettings modes)
                modes.select(selected);
            String lightningType = System.getProperty("ess.autoshot.lightningtype");
            if (Boolean.getBoolean("ess.autoshot.lightning3"))
                lightningType = "Тип 3";
            if (lightningType != null)
                for (var setting : ClientModules.find("TargetESP").settings) if (setting.name().equals("Тип молний") && setting instanceof ess.ll.client.setting.Settings.ModeSettings modes)
                    modes.select(lightningType);
            if (Boolean.getBoolean("ess.autoshot.lightning3"))
                for (var setting : ClientModules.find("TargetESP").settings) if (setting.name().equals("Тип") && setting instanceof ess.ll.client.setting.Settings.ModeSettings modes)
                    modes.select("Молнии");
        }
        if (ticks == 70 && Boolean.getBoolean("ess.autoshot.targetesptest"))
            System.out.println("[ESS TargetESP] target=" + ess.ll.client.module.combat.AttackAura.currentTarget() + " mode=" + System.getProperty("ess.autoshot.targetespmode", "Призраки 1"));
        if (Boolean.getBoolean("ess.autoshot.targetespall")) {
            String[] modes = { "Призраки 1", "Призраки 2", "Призраки 3", "Квадрат", "Круг", "Сканирование", "Молнии", "Кристалики" };
            for (int i = 0; i < modes.length; i++) {
                int at = 44 + i * 8;
                if (ticks == at)
                    for (var setting : ClientModules.find("TargetESP").settings) if (setting.name().equals("Тип") && setting instanceof ess.ll.client.setting.Settings.ModeSettings values)
                        values.select(modes[i]);
                if (ticks == at + 5)
                    shot(mc, "ess-target-esp-" + i + ".png");
            }
        }
        if (ticks == 42 && Boolean.getBoolean("ess.autoshot.effecttest")) {
            var target = mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, mc.player.getBoundingBox().inflate(10), e -> e != mc.player).stream().findFirst().orElse(null);
            if (target == null)
                target = mc.player;
            ess.ll.client.event.AttackEntityEvent.INSTANCE.firePre(target, new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("effects-test", false));
        }
        if (ticks == 60 && Boolean.getBoolean("ess.autoshot.effecttest")) {
            int count = ess.ll.client.render.ClientParticleManager.INSTANCE.size();
            System.out.println("[ESS effects] particles=" + count);
            if (count == 0)
                throw new IllegalStateException("Particles did not spawn after attack");
        }
        // Every crystal that existed near the player during the window: one per placement cycle.
        if (ticks >= 38 && ticks <= 100 && Boolean.getBoolean("ess.autoshot.crystalbench"))
            for (var c : mc.level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, mc.player.getBoundingBox().inflate(8))) benchCrystals.add(c.getId());
        if (ticks == 100 && Boolean.getBoolean("ess.autoshot.crystalbench"))
            System.out.println("[ESS crystal] cycles=" + benchCrystals.size() + " in 62 ticks");
        if ((ticks == 38 || ticks == 100) && Boolean.getBoolean("ess.autoshot.crystalbench") && mc.getSingleplayerServer() != null) {
            // Damage taken by the husk on the server: absorption plus health, before and after the window.
            for (var entity : mc.getSingleplayerServer().overworld().getAllEntities()) if (entity instanceof net.minecraft.world.entity.monster.Monster husk && husk.distanceTo(mc.player) < 10)
                System.out.println("[ESS crystal] tick=" + ticks + " husk hp=" + (husk.getHealth() + husk.getAbsorptionAmount()) + " crystals=" + mc.level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, mc.player.getBoundingBox().inflate(8)).size());
        }
        if (ticks == 38 && Boolean.getBoolean("ess.autoshot.crystaltest")) {
            crystalCountBefore = ess.ll.client.features.inventory.InventoryUtils.countItems(net.minecraft.world.item.Items.END_CRYSTAL);
            var crystal = ClientModules.find("CrystalAura");
            for (var setting : crystal.settings) {
                if (setting.name().equals("Макс. урон себе") && setting instanceof ess.ll.client.setting.Settings.SliderSetting slider) {
                    crystalSelfBefore = slider.get();
                    slider.set(20);
                }
                if (setting.name().equals("Анти-суицид") && setting instanceof ess.ll.client.setting.Settings.BooleanSetting toggle) {
                    crystalSuicideBefore = toggle.get();
                    if (toggle.get())
                        toggle.toggle();
                }
            }
            if (!Boolean.getBoolean("ess.autoshot.digtest") && !Boolean.getBoolean("ess.autoshot.swingtest"))
                crystal.setEnabled(true);
        }
        if (ticks >= 40 && ticks <= 205 && Boolean.getBoolean("ess.autoshot.digtest")) {
            if (digPos == null)
                digPos = mc.player.blockPosition().offset(0, 0, 3);
            var pos = digPos;
            if (ticks == 40)
                System.out.println("[ESS crystal] dig initial player=" + mc.player.blockPosition() + " pos=" + pos + " block=" + mc.level.getBlockState(pos).getBlock());
            boolean requested = ess.ll.client.features.world.BlockBreakManager.INSTANCE.requestBreak(ess.ll.client.util.ModuleManager.INSTANCE.crystalAura(), pos);
            if (ticks % 10 == 0)
                System.out.println("[ESS crystal] dig tick=" + ticks + " requested=" + requested + " state=" + ess.ll.client.features.world.BlockBreakManager.INSTANCE.state() + " progress=" + ess.ll.client.features.world.BlockBreakManager.INSTANCE.breakProgress() + " hit=" + mc.hitResult + " active=" + ess.ll.client.features.world.BlockBreakManager.INSTANCE.canMineTarget());
        }
        if (ticks == 210 && Boolean.getBoolean("ess.autoshot.digtest")) {
            var pos = digPos;
            System.out.println("[ESS crystal] dig test block=" + mc.level.getBlockState(pos).getBlock());
            if (!mc.level.getBlockState(pos).isAir())
                throw new IllegalStateException("BlockBreakManager did not mine test block");
        }
        if (ticks == 45 && Boolean.getBoolean("ess.autoshot.modules"))
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        if (ticks == 48 && Boolean.getBoolean("ess.autoshot.swingtest"))
            shot(mc, "ess-swing-active.png");
        if (ticks == 80)
            shot(mc, "ess-hud.png");
        if (ticks == 80 && Boolean.getBoolean("ess.autoshot.targetesptest"))
            shot(mc, "ess-target-esp.png");
        if (ticks == 83 && Boolean.getBoolean("ess.autoshot.swingtest"))
            shot(mc, "ess-swing-idle-2.png");
        if (ticks == 85 && !Boolean.getBoolean("ess.autoshot.digtest") && !Boolean.getBoolean("ess.autoshot.targetesptest"))
            mc.gui.setScreen(new ChatScreen("", false));
        // Real mouse input through MouseHandler: grab the visible TargetHUD preview and move it.
        if (ticks == 92 && !Boolean.getBoolean("ess.autoshot.crystaltest") && !Boolean.getBoolean("ess.autoshot.swingtest") && !Boolean.getBoolean("ess.autoshot.targetesptest")) {
            drag = ess.ll.client.render.HudRenderer.center("targethud");
            move(mc, drag[0], drag[1]);
        }
        if (ticks == 94 && !Boolean.getBoolean("ess.autoshot.crystaltest") && !Boolean.getBoolean("ess.autoshot.swingtest") && !Boolean.getBoolean("ess.autoshot.targetesptest"))
            button(mc, true);
        if (ticks == 96 && !Boolean.getBoolean("ess.autoshot.crystaltest") && !Boolean.getBoolean("ess.autoshot.swingtest") && !Boolean.getBoolean("ess.autoshot.targetesptest"))
            move(mc, drag[0] + 90, drag[1] + 70);
        if (ticks == 98 && !Boolean.getBoolean("ess.autoshot.crystaltest") && !Boolean.getBoolean("ess.autoshot.swingtest") && !Boolean.getBoolean("ess.autoshot.targetesptest")) {
            button(mc, false);
            float[] moved = ess.ll.client.render.HudRenderer.center("targethud");
            System.out.println("[ESS autoshot] drag targethud " + java.util.Arrays.toString(drag) + " -> " + java.util.Arrays.toString(moved));
            if (Math.abs(moved[0] - drag[0]) < 20 || Math.abs(moved[1] - drag[1]) < 20)
                throw new IllegalStateException("TargetHUD did not drag in both axes");
        }
        if (ticks == 100)
            shot(mc, "ess-edit.png");
        if (ticks == 100 && Boolean.getBoolean("ess.autoshot.crystaltest") && !Boolean.getBoolean("ess.autoshot.digtest") && !Boolean.getBoolean("ess.autoshot.swingtest")) {
            int obsidian = 0;
            var center = mc.player.blockPosition();
            for (var p : net.minecraft.core.BlockPos.betweenClosed(center.offset(-6, -1, -6), center.offset(6, 1, 6))) if (mc.level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.OBSIDIAN))
                obsidian++;
            System.out.println("[ESS crystal] placed obsidian count=" + obsidian);
            System.out.println("[ESS crystal] crystals before=" + crystalCountBefore + " after=" + ess.ll.client.features.inventory.InventoryUtils.countItems(net.minecraft.world.item.Items.END_CRYSTAL));
            if (obsidian == 0)
                throw new IllegalStateException("CrystalAura did not place obsidian in world");
        }
        if (ticks == 101 && Boolean.getBoolean("ess.autoshot.settings")) {
            float[] center = ess.ll.client.render.HudRenderer.center("targethud");
            move(mc, center[0], center[1]);
            button(mc, InputConstants.MOUSE_BUTTON_RIGHT, true);
            button(mc, InputConstants.MOUSE_BUTTON_RIGHT, false);
        }
        if (ticks == 102 && Boolean.getBoolean("ess.autoshot.settings")) {
            boolean before = ClientModules.TARGET_PARTICLES.get();
            particlesBefore = before;
            float[] point = ess.ll.client.render.HudRenderer.popupPoint("targethud", 1);
            move(mc, point[0], point[1]);
            button(mc, InputConstants.MOUSE_BUTTON_LEFT, true);
            button(mc, InputConstants.MOUSE_BUTTON_LEFT, false);
            System.out.println("[ESS autoshot] TargetHUD settings LMB: particles " + before + " -> " + ClientModules.TARGET_PARTICLES.get());
            if (before == ClientModules.TARGET_PARTICLES.get())
                throw new IllegalStateException("TargetHUD setting did not receive LMB");
        }
        if (ticks == 103 && Boolean.getBoolean("ess.autoshot.settings"))
            shot(mc, "ess-target-settings.png");
        if (ticks == 104 && Boolean.getBoolean("ess.autoshot.settings")) {
            float[] point = ess.ll.client.render.HudRenderer.popupPoint("targethud", 1);
            move(mc, point[0], point[1]);
            button(mc, InputConstants.MOUSE_BUTTON_LEFT, true);
            button(mc, InputConstants.MOUSE_BUTTON_LEFT, false);
            if (ClientModules.TARGET_PARTICLES.get() != particlesBefore)
                throw new IllegalStateException("TargetHUD setting did not restore");
            float[] current = ess.ll.client.render.HudRenderer.center("targethud");
            move(mc, current[0], current[1]);
            button(mc, true);
            move(mc, drag[0], drag[1]);
            button(mc, false);
        }
        if (ticks == 104 && Boolean.getBoolean("ess.autoshot.crystaltest")) {
            var crystal = ClientModules.find("CrystalAura");
            for (var setting : crystal.settings) {
                if (setting.name().equals("Макс. урон себе") && setting instanceof ess.ll.client.setting.Settings.SliderSetting slider)
                    slider.set(crystalSelfBefore);
                if (setting.name().equals("Анти-суицид") && setting instanceof ess.ll.client.setting.Settings.BooleanSetting toggle && toggle.get() != crystalSuicideBefore)
                    toggle.toggle();
            }
        }
        if (ticks == (Boolean.getBoolean("ess.autoshot.digtest") ? 215 : 108) && Boolean.getBoolean("ess.autoshot.short"))
            mc.stop();
        if (ticks == 105 && !Boolean.getBoolean("ess.autoshot.digtest")) {
            mc.gui.setScreen(null);
            var gui = new ClickGuiScreen();
            mc.gui.setScreen(gui);
            gui.expand(ClientModules.ZOOM);
        }
        if (ticks == 130)
            shot(mc, "ess-gui.png");
        if (ticks == 135)
            mc.gui.setScreen(null);
        int end = 140;
        if (ticks == end + 45)
            mc.gui.setScreen(new MainMenuScreen());
        if (ticks == end + 85) {
            shot(mc, "ess-menu.png");
            System.out.println("[ESS autoshot] menu fps " + mc.getFps() + " scale " + ess.ll.client.render.MenuScene.scale() + " samples " + ess.ll.client.render.MenuScene.accumulated());
        }
        if (ticks == end + 88) {
            var w = mc.getWindow();
            move(mc, Math.max(26, w.getGuiScaledWidth() * .075f) + 60, w.getGuiScaledHeight() / 2f + 40);
        }
        if (ticks == end + 100)
            shot(mc, "ess-menu-hover.png");
        if (ticks == end + 200) {
            shot(mc, "ess-menu-final.png");
            System.out.println("[ESS autoshot] menu fps " + mc.getFps() + " samples " + ess.ll.client.render.MenuScene.accumulated());
        }
        if (ticks == end + 202)
            ess.ll.client.render.MenuScene.style = ess.ll.client.render.MenuScene.Style.FIR;
        if (ticks == end + 225)
            shot(mc, "ess-menu-fir.png");
        if (ticks == end + 230)
            mc.stop();
    }

    private static ess.ll.client.render.MenuScene.Style bgBefore;
    private static final java.util.Map<String, net.minecraft.core.BlockPos> TIKVA_HOMES = new java.util.HashMap<>();
    private static final java.util.Map<String, String> TIKVA_TPA = new java.util.concurrent.ConcurrentHashMap<>();
    private static int tikvaEnd = -1;
    private static net.minecraft.core.BlockPos obbyBase;
    private static int obbyStart;
    private static String obbyState = "";
    private static final java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.Block> obbyBlocks = new java.util.HashMap<>();
    private static final java.util.Set<Integer> obbyCrystals = new java.util.HashSet<>();
    private static float[] drag;
    private static boolean particlesBefore;
    private static float crystalSelfBefore;
    private static boolean crystalSuicideBefore;
    private static net.minecraft.core.BlockPos digPos;

    private static void move(Minecraft mc, float guiX, float guiY) {
        var w = mc.getWindow();
        double sx = (double) w.getScreenWidth() / w.getGuiScaledWidth(), sy = (double) w.getScreenHeight() / w.getGuiScaledHeight();
        mc.mouseHandler.onMove(w.handle(), guiX * sx, guiY * sy, 0, 0);
    }

    private static void button(Minecraft mc, boolean press) {
        button(mc, InputConstants.MOUSE_BUTTON_LEFT, press);
    }

    private static void button(Minecraft mc, int button, boolean press) {
        mc.mouseHandler.onButton(mc.getWindow().handle(), new net.minecraft.client.input.MouseButtonInfo(button, 0), press ? 1 : 0);
    }

    /**
     * Turns the player to look at a point.
     */
    private static void lookAt(net.minecraft.world.entity.player.Player p, double x, double y, double z) {
        var d = new net.minecraft.world.phys.Vec3(x, y, z).subtract(p.getEyePosition());
        p.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        p.setXRot((float) Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z))));
    }

    private static int griefPage, griefDone, griefMenuId = 100;

    private static net.minecraft.world.item.ItemStack named(net.minecraft.world.item.Item item, String name) {
        var stack = new net.minecraft.world.item.ItemStack(item);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
        return stack;
    }

    /**
     * Opens page 0 (the mode menu) or grief page 1–3 of the stand-in hub as a client-side chest.
     */
    private static void griefMenu(Minecraft mc, int page) {
        griefPage = page;
        var items = new net.minecraft.world.SimpleContainer(54);
        if (page == 0)
            items.setItem(22, named(net.minecraft.world.item.Items.CRAFTING_TABLE, "ГРИФ ВЫЖИВАНИЕ"));
        else {
            int from = (page - 1) * 30 + 1, to = Math.min(88, page * 30);
            for (int n = from; n <= to; n++) items.setItem(n - from + (n - from) / 6 * 3 + 1, named(net.minecraft.world.item.Items.GRASS_BLOCK, "Гриф #" + n));
            if (page > 1)
                items.setItem(45, named(net.minecraft.world.item.Items.ARROW, "← Назад"));
            if (page < 3)
                items.setItem(53, named(net.minecraft.world.item.Items.ARROW, "Далее →"));
        }
        var menu = net.minecraft.world.inventory.ChestMenu.sixRows(++griefMenuId, mc.player.getInventory(), items);
        mc.player.containerMenu = menu;
        mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.ContainerScreen(menu, mc.player.getInventory(), net.minecraft.network.chat.Component.literal(page == 0 ? "Режимы" : "Грифы · " + page + "/3")));
    }

    private static int sorterDone = -1;
    private static net.minecraft.core.BlockPos sorterBase;

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, message -> System.out.println("[ESS autoshot] " + message.getString()));
    }
}
