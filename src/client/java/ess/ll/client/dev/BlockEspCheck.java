package ess.ll.client.dev;

import baritone.api.BaritoneAPI;
import ess.ll.client.command.DotCommands;
import ess.ll.client.command.DotSuggestions;
import ess.ll.client.module.ClientModules;
import ess.ll.client.module.render.BlockESP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Explicitly opt-in integration check in the separate run-shots world. */
final class BlockEspCheck {
    private static Runnable restore;
    private static boolean enabled;
    static void tick(Minecraft mc,int tick) {
        var esp=BlockESP.INSTANCE;
        if(tick==5) {
            restore=esp.prepareLoad(esp.save()); enabled=esp.isEnabled();
            for(var module:ClientModules.all())if(module.enabled())module.setEnabled(false);
            DotCommands.execute(".blockesp clear");
            DotCommands.execute(".blockesp add diamond_ore #40DFFF");
            DotCommands.execute(".blockesp add chest 255 80 30");
            check(esp.entries().get("minecraft:diamond_ore")==0xff40dfff,"hex command");
            check(esp.entries().get("minecraft:chest")==0xffff501e,"RGB command");
            DotCommands.execute(".blockesp add stone white");
            DotCommands.execute(".blockesp remove stone");
            check(esp.entries().size()==2,"remove command");
            String suggestion=".blockesp add diamond_o";
            check(!DotSuggestions.complete(suggestion,suggestion.length()).isEmpty(),"registry completion");
            Runnable roundTrip=esp.prepareLoad(esp.save()); esp.clear(); roundTrip.run();
            check(esp.entries().size()==2,"persistence round trip");
            var server=mc.getSingleplayerServer();
            server.execute(()->{
                for(String command:List.of("gamemode creative @p","tp @p 0.5 151 0.5 0 10",
                    "fill -8 150 -8 8 150 12 stone","fill -8 151 -8 8 155 12 air",
                    "setblock -2 151 4 diamond_ore","setblock 2 151 4 chest",
                    "summon chest_minecart 0.5 151 4.5 {NoGravity:1b}","time set noon","weather clear"))
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);
            });
            DotCommands.execute(".blockesp on");
            check(BaritoneAPI.getProvider().getPrimaryBaritone()!=null,"Baritone provider");
        }
        if(tick==80) {
            try {
                var field=BlockESP.class.getDeclaredField("cache"); field.setAccessible(true);
                var cache=(Map<?,?>)field.get(esp);
                check(cache.values().stream().anyMatch(v->((List<?>)v).contains(new BlockPos(-2,151,4))),"loaded chunk scan");
            } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
            Screenshot.grab(mc.gameDirectory,"blockesp-check.png",mc.gameRenderer.mainRenderTarget(),1,
                message->System.out.println("[ESS BlockESP] "+message.getString()));
            check(BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("goto 0 151 8"),"Baritone goto command");
        }
        if(tick==180) {
            check(mc.player.getZ()>5,"Baritone path execution");
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            restore.run(); esp.setEnabled(enabled);
            System.out.println("[ESS BlockESP] PASS all checks"); mc.stop();
        }
    }
    private static void check(boolean ok,String name) {
        System.out.println("[ESS BlockESP] "+name+": "+(ok?"PASS":"FAIL"));
        if(!ok)throw new IllegalStateException(name);
    }
}
