package ess.ll.client.dev;

import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.features.world.BlockBreakManager;
import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings;
import ess.ll.client.util.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Opt-in integrated-server regression: mining -> acknowledgement -> obsidian -> crystal -> explosion.
 */
final class CrystalChainCheck {
    private static final boolean ANCHOR = Boolean.getBoolean("ess.autoshot.chainAnchor");
    private static final boolean ABOVE = Boolean.getBoolean("ess.autoshot.anchorAbove");
    private static final BlockPos BASE = new BlockPos(0, ANCHOR && !ABOVE ? 151 : 150, 2);
    private static volatile boolean serverCharged;
    private static final List<Runnable> RESTORE = new ArrayList<>();
    private static final boolean DROP = Boolean.getBoolean("ess.autoshot.chainDrop");
    private static int mined = -1, placed = -1, consumed = -1, confirmed = -1;
    private static int maxAwait;
    private static volatile boolean setupComplete;
    private static int armedTick = -1;

    static void tick(Minecraft mc, int tick) {
        if (tick == 5) {
            for (String name : List.of("CrystalAura", "Aura", "AutoTool", "Sprint", "AutoJump")) {
                var module = ClientModules.find(name);
                if (module == null)
                    continue;
                boolean enabled = module.enabled();
                RESTORE.add(() -> module.setEnabled(enabled));
                module.setEnabled(false);
            }
            mc.getSingleplayerServer().execute(() -> {
                for (String command : List.of("gamemode survival @p", "difficulty normal", "gamerule minecraft:block_drops false", "tp @p 0.5 151 0.5 0 0", "effect clear @p", "effect give @p minecraft:resistance infinite 5 true", "fill -7 149 -7 7 154 7 air", "fill -7 149 -7 7 150 7 stone", "kill @e[type=minecraft:husk]", "kill @e[type=minecraft:end_crystal]", "kill @e[type=minecraft:item]", "clear @p", "item replace entity @p hotbar.0 with minecraft:diamond_pickaxe", "item replace entity @p hotbar.1 with minecraft:obsidian 64", "item replace entity @p hotbar.2 with minecraft:end_crystal 64", "summon minecraft:husk 0.5 151 3.5 {NoAI:1b,Health:20f,PersistenceRequired:1b}")) {
                    mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(), command);
                }
                if (DROP) {
                    for (String command : List.of("setblock 0 150 2 obsidian", "summon minecraft:item 0.5 151.5 2.5 {NoGravity:1b,PickupDelay:32767s,Item:{id:\"minecraft:stone\",count:1}}")) mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(), command);
                }
                if (ANCHOR) {
                    for (String command : List.of("item replace entity @p hotbar.3 with minecraft:respawn_anchor 64", "item replace entity @p hotbar.4 with minecraft:glowstone 64")) mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(), command);
                }
                if (ABOVE) {
                    for (String command : List.of("setblock 0 151 3 stone", "tp @e[type=minecraft:husk] 0.5 152 3.5")) mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(), command);
                }
                setupComplete = true;
            });
        }
        boolean sceneReady = setupComplete && InventoryUtils.countItems(Items.END_CRYSTAL) == 64 && !mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, new net.minecraft.world.phys.AABB(BASE).inflate(3)).isEmpty() && (!DROP || !mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(BASE.above())).isEmpty());
        if (armedTick < 0 && tick >= 25 && sceneReady) {
            armedTick = tick;
            mc.player.setYRot(180);
            var module = ClientModules.find("CrystalAura");
            for (var setting : module.settings) {
                if (setting instanceof Settings.SliderSetting slider) {
                    float previous = slider.get();
                    RESTORE.add(() -> slider.set(previous));
                    switch (setting.name()) {
                        case "Макс. урон себе" ->
                            slider.set(20);
                        case "Мин. урон врагу" ->
                            slider.set(1);
                        case "Задержка в тиках" ->
                            slider.set(0);
                        case "Дальность взрыва", "Дальность постановки" ->
                            slider.set(5);
                    }
                } else if (setting instanceof Settings.BooleanSetting toggle) {
                    boolean previous = toggle.get();
                    RESTORE.add(() -> {
                        if (toggle.get() != previous)
                            toggle.toggle();
                    });
                    boolean desired = switch (setting.name()) {
                        case "Анти-суицид", "Легитный" ->
                            false;
                        case "Обход Matrix" ->
                            Boolean.getBoolean("ess.autoshot.chainMatrix");
                        case "Авто подкоп", "Ставить обсидиан" ->
                            true;
                        case "Ставить анчорки" ->
                            ANCHOR;
                        default ->
                            previous;
                    };
                    if (toggle.get() != desired)
                        toggle.toggle();
                }
            }
            module.setEnabled(true);
            ClientModules.find("AutoTool").setEnabled(true);
            // Limit candidate choice to one real stone block; all actions still go through vanilla and the server.
            Map<BlockPos, Integer> blacklist = field("blacklist");
            for (BlockPos pos : BlockPos.betweenClosed(-7, 145, -7, 7, 155, 7)) {
                if (!pos.equals(BASE))
                    blacklist.put(pos.immutable(), mc.player.tickCount + 10000);
            }
        }
        if (armedTick < 0) {
            if (tick > 400)
                throw new IllegalStateException("Crystal test arena did not synchronize");
            return;
        }
        if (tick == armedTick)
            return;
        if (ANCHOR && !ABOVE) {
            var state = mc.level.getBlockState(BASE);
            if (placed < 0 && state.is(Blocks.RESPAWN_ANCHOR)) {
                placed = tick;
                System.out.println("[ESS anchor] placed tick=" + tick + " charges=" + state.getValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE));
            }
            if (placed >= 0 && tick >= placed + 5 && tick % 5 == 0)
                mc.getSingleplayerServer().execute(() -> {
                    var serverState = mc.getSingleplayerServer().overworld().getBlockState(BASE);
                    serverCharged = !serverState.is(Blocks.RESPAWN_ANCHOR) && mc.getSingleplayerServer().overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, new net.minecraft.world.phys.AABB(BASE).inflate(6), entity -> entity.isAlive()).isEmpty();
                });
            if (serverCharged || tick > armedTick + 100) {
                boolean passed = serverCharged && InventoryUtils.countItems(Items.END_CRYSTAL) == 64 && InventoryUtils.countItems(Items.RESPAWN_ANCHOR) == 63 && InventoryUtils.countItems(Items.GLOWSTONE) == 60;
                System.out.println("[ESS anchor] " + (passed ? "PASS" : "FAIL") + " serverExploded=" + serverCharged);
                ClientModules.find("CrystalAura").setEnabled(false);
                for (Runnable restore : RESTORE) restore.run();
                mc.stop();
                if (!passed)
                    throw new IllegalStateException("Anchor placement/charging failed");
            }
            return;
        }
        maxAwait = Math.max(maxAwait, CrystalChainCheck.<Integer>field("awaitTicks"));
        if (DROP && tick == armedTick + 15)
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(), "kill @e[type=minecraft:item]"));
        var state = mc.level.getBlockState(BASE);
        if (mined < 0 && state.isAir())
            mined = tick;
        if (mined >= 0 && confirmed < 0 && !CrystalChainCheck.<Map<BlockPos, Integer>>field("pendingBreaks").containsKey(BASE))
            confirmed = tick;
        if (placed < 0 && state.is(Blocks.OBSIDIAN))
            placed = tick;
        if (consumed < 0 && InventoryUtils.countItems(Items.END_CRYSTAL) < 64)
            consumed = tick;
        if (tick % 10 == 0)
            System.out.println("[ESS chain] tick=" + tick + " block=" + state.getBlock() + " mining=" + BlockBreakManager.INSTANCE.state() + " progress=" + BlockBreakManager.INSTANCE.breakProgress() + " pending=" + field("pendingBreaks") + " mined=" + mined + " confirmed=" + confirmed + " obsidian=" + placed + " crystal=" + consumed + " blacklist=" + field("blacklist") + " plan=" + field("plannedBase"));
        if (consumed >= 0 && tick > consumed + 15 || tick >= armedTick + 155) {
            boolean targetAlive = false;
            for (var entity : mc.level.entitiesForRendering()) if (entity instanceof net.minecraft.world.entity.monster.zombie.Husk husk && husk.isAlive())
                targetAlive = true;
            boolean passed = !targetAlive && (!ABOVE || InventoryUtils.countItems(Items.RESPAWN_ANCHOR) == 64) && (DROP ? maxAwait >= 2 && consumed >= armedTick + 15 && consumed < armedTick + 35 : mined >= 0 && placed >= mined && consumed >= placed && placed - mined < 20);
            System.out.println("[ESS chain] " + (passed ? "PASS" : "FAIL") + " mined=" + mined + " confirmed=" + confirmed + " obsidian=" + placed + " crystal=" + consumed + " targetAlive=" + targetAlive + " maxAwait=" + maxAwait);
            ClientModules.find("CrystalAura").setEnabled(false);
            for (Runnable restore : RESTORE) restore.run();
            mc.stop();
            if (!passed)
                throw new IllegalStateException("CrystalAura mining/placement chain stalled; see [ESS chain]");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name) {
        try {
            var aura = ModuleManager.INSTANCE.crystalAura();
            var field = aura.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(aura);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
