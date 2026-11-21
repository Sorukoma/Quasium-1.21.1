package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.loot.modifier.AddItemLootModifier;
import net.sorukoma.quasium.loot.modifier.WildSeedsLootModifier;
import net.sorukoma.quasium.mind.BestiaryLootModifier;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifiersProvider extends GlobalLootModifierProvider {
    private static final float TOBACCO_SEEDS_CHANCE = 0.1F;
    private static final float CHEST_SEEDS_CHANCE = 0.2F;

    public ModGlobalLootModifiersProvider(net.minecraft.data.PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, QuasiumMod.MOD_ID);
    }

    @Override
    protected void start() {
        add("bestiary", new BestiaryLootModifier(new LootItemCondition[0]));
        addGrassSeeds("tobacco_seeds_from_short_grass", "blocks/short_grass");
        addGrassSeeds("tobacco_seeds_from_tall_grass", "blocks/tall_grass");
        addChestSeeds("ashen_seeds_in_nether_bridge", "chests/nether_bridge", ModItems.ASHEN_SEEDS.get());
        addChestSeeds("chorus_seeds_in_end_city", "chests/end_city_treasure", ModItems.CHORUS_SEEDS.get());
    }

    private void addGrassSeeds(String name, String grass) {
        add(name, new WildSeedsLootModifier(
                new LootItemCondition[]{
                        LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace(grass)).build(),
                        LootItemRandomChanceCondition.randomChance(TOBACCO_SEEDS_CHANCE).build()
                },
                ModItems.TOBACCO_SEEDS.get()));
    }

    private void addChestSeeds(String name, String chest, Item seeds) {
        add(name, new AddItemLootModifier(
                new LootItemCondition[]{
                        LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace(chest)).build(),
                        LootItemRandomChanceCondition.randomChance(CHEST_SEEDS_CHANCE).build()
                },
                new ItemStack(seeds)));
    }
}
