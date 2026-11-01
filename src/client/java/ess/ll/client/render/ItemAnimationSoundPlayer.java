package ess.ll.client.render;

import ess.ll.client.integration.MinecraftAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ItemAnimationSoundPlayer implements MinecraftAccess {
    public static void play(String var0, float var1) {
        if (mc.player != null) {
            mc.player.playSound(SoundEvent.createVariableRangeEvent(Identifier.parse(var0)), var1, 1.0F);
        }
    }
}
