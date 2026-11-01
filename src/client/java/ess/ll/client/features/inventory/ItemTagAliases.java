package ess.ll.client.features.inventory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ItemTagAliases {
    public static final TagKey<Item> PICKAXES = minecraftTag("pickaxes");
    public static final TagKey<Item> SWORDS = minecraftTag("swords");
    public static final TagKey<Item> AXES = minecraftTag("axes");
    public static final TagKey<Item> SHOVELS = minecraftTag("shovels");
    public static final TagKey<Item> HANGING_SIGNS = minecraftTag("hanging_signs");
    public static final TagKey<Item> SIGNS = minecraftTag("signs");
    public static final TagKey<Item> DOORS = minecraftTag("doors");
    public static final TagKey<Item> SKULLS = minecraftTag("skulls");
    public static final TagKey<Item> BUNDLES = minecraftTag("bundles");
    public static final TagKey<Item> BEDS = minecraftTag("beds");
    public static final TagKey<Item> COPPER_GOLEM_STATUES = minecraftTag("copper_golem_statues");
    public static final TagKey<Item> MUSIC_DISCS = commonTag("music_discs");
    public static final TagKey<Item> NUGGETS = commonTag("nuggets");

    private static TagKey<Item> minecraftTag(String var0) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", var0));
    }

    private ItemTagAliases() {
    }

    private static TagKey<Item> commonTag(String var0) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", var0));
    }
}
