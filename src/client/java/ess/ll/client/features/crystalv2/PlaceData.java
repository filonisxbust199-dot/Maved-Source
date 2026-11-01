package ess.ll.client.features.crystalv2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record PlaceData(
   BlockPos pos, Direction face, double damage, Rotation rotation, Action action, net.minecraft.world.entity.Entity entity, BlockPos checkPos
) {
}
