package ess.ll.client.features.crystalv2;

import net.minecraft.client.Minecraft;

/** The game instance and the tracked packet state for the CrystalAuraV2 classes. */
public interface V2Access {
   Minecraft mc = Minecraft.getInstance();
   V2Packets pc = V2Packets.INSTANCE;
}
