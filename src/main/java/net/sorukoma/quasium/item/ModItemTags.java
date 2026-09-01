package net.sorukoma.quasium.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> JERKY_INPUTS = create("jerky_inputs");
    public static final TagKey<Item> RAW_BEEF = create("jerky_inputs/raw_beef");
    public static final TagKey<Item> RAW_PORK = create("jerky_inputs/raw_pork");
    public static final TagKey<Item> RAW_CHICKEN = create("jerky_inputs/raw_chicken");
    public static final TagKey<Item> RAW_MUTTON = create("jerky_inputs/raw_mutton");
    public static final TagKey<Item> RAW_RABBIT = create("jerky_inputs/raw_rabbit");
    public static final TagKey<Item> RAW_COD = create("jerky_inputs/raw_cod");
    public static final TagKey<Item> RAW_SALMON = create("jerky_inputs/raw_salmon");
    public static final TagKey<Item> ESSENCE_MINT = create("essence_ingredients/mint");
    public static final TagKey<Item> ESSENCE_BERRY = create("essence_ingredients/berry");
    public static final TagKey<Item> ESSENCE_LEMON = create("essence_ingredients/lemon");
    public static final TagKey<Item> ESSENCE_LIME = create("essence_ingredients/lime");
    public static final TagKey<Item> ESSENCE_ORANGE = create("essence_ingredients/orange");
    public static final TagKey<Item> ESSENCE_GRAPEFRUIT = create("essence_ingredients/grapefruit");
    public static final TagKey<Item> ESSENCE_GRAPE = create("essence_ingredients/grape");
    public static final TagKey<Item> ESSENCE_COCONUT = create("essence_ingredients/coconut");
    public static final TagKey<Item> ESSENCE_ANISE = create("essence_ingredients/anise");
    public static final TagKey<Item> ESSENCE_ICE = create("essence_ingredients/ice");
    public static final TagKey<Item> ESSENCE_HONEY = create("essence_ingredients/honey");
    public static final TagKey<Item> ESSENCE_NETHER = create("essence_ingredients/nether");
    public static final TagKey<Item> ESSENCE_LAVENDER = create("essence_ingredients/lavender");
    public static final TagKey<Item> ESSENCE_COCOA = create("essence_ingredients/cocoa");

    private static TagKey<Item> create(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("quasium", name));
    }

    private ModItemTags() {}
}
