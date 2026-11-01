package ess.ll.client.features.world;

import ess.ll.client.integration.MinecraftAccess;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

public final class BlockPlacementTracker implements MinecraftAccess {
    private static final Long2IntOpenHashMap RESERVATION_EXPIRY = new Long2IntOpenHashMap();
    private static final Long2ObjectOpenHashMap<Object> RESERVATION_OWNERS = new Long2ObjectOpenHashMap();
    private static int lastPlacementTick = Integer.MIN_VALUE;
    private static final int NO_PLACEMENT_TICK = Integer.MIN_VALUE;
    private static final int NO_RESERVATION_TICK = Integer.MIN_VALUE;

    public static boolean isReservedByAnother(Object var0, BlockPos blockPos) {
        return isReserved(blockPos) && RESERVATION_OWNERS.get(blockPos.asLong()) != var0;
    }

    public static boolean wasPlacementRecent(int var0) {
        if (mc.player != null && lastPlacementTick != NO_PLACEMENT_TICK) {
            int var1 = mc.player.tickCount - lastPlacementTick;
            return var1 >= 0 && var1 <= var0;
        } else {
            return false;
        }
    }

    private BlockPlacementTracker() {
    }

    static {
        RESERVATION_EXPIRY.defaultReturnValue(Integer.MIN_VALUE);
    }

    public static void reserve(Object var0, BlockPos blockPos) {
        if (blockPos != null && mc.player != null) {
            if (RESERVATION_EXPIRY.size() > 64) {
                pruneExpired();
            }
            long var2 = blockPos.asLong();
            RESERVATION_EXPIRY.put(var2, mc.player.tickCount + 4);
            RESERVATION_OWNERS.put(var2, var0);
        }
    }

    public static boolean isReserved(BlockPos blockPos) {
        if (blockPos != null && mc.player != null) {
            long var1 = blockPos.asLong();
            int var3 = RESERVATION_EXPIRY.get(var1);
            if (var3 == NO_RESERVATION_TICK) {
                return false;
            } else if (mc.player.tickCount > var3) {
                RESERVATION_EXPIRY.remove(var1);
                RESERVATION_OWNERS.remove(var1);
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static void pruneExpired() {
        int tickCount2 = mc.player.tickCount;
        RESERVATION_EXPIRY.long2IntEntrySet().removeIf(var1 -> {
            if (tickCount2 <= var1.getIntValue()) {
                return false;
            }
            RESERVATION_OWNERS.remove(var1.getLongKey());
            return true;
        });
    }

    public static void markPlacement() {
        if (mc.player != null) {
            lastPlacementTick = mc.player.tickCount;
        }
    }
}
