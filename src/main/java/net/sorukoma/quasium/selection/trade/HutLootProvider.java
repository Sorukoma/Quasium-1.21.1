package net.sorukoma.quasium.selection.trade;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.genetics.Genetics;
import net.sorukoma.quasium.selection.world.WorldLootProvider;

import java.util.List;
import java.util.function.BiConsumer;

public final class HutLootProvider implements LootTableSubProvider {
    public static final ResourceKey<LootTable> HUT_CHEST =
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "chests/hookah_hut"));
    public static final List<String> HYBRID_LINEAGE = List.of(TobaccoVarieties.DIKAR, TobaccoVarieties.PESCHANIK);
    public static final Genetics.Stars HYBRID_STARS = new Genetics.Stars(4, 3, 3);
    public static final float BULB_CHANCE = 0.1f;

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        LootPool.Builder seeds = LootPool.lootPool();
        for (Item wild : List.of(ModItems.DIKAR_SEEDS.get(), ModItems.PESCHANIK_SEEDS.get(), ModItems.BOLOTNIK_SEEDS.get(), ModItems.LEDYANOY_SEEDS.get())) {
            seeds.add(LootItem.lootTableItem(wild).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))));
        }
        LootPool.Builder hybrid = LootPool.lootPool().add(LootItem.lootTableItem(ModItems.HYBRID_TOBACCO_SEEDS.get())
                .apply(SetComponentsFunction.setComponent(ModDataComponents.TOBACCO_VARIETY.get(), TobaccoVarieties.HYBRID))
                .apply(SetComponentsFunction.setComponent(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), HYBRID_STARS.strength()))
                .apply(SetComponentsFunction.setComponent(ModDataComponents.TOBACCO_YIELD_STARS.get(), HYBRID_STARS.yield()))
                .apply(SetComponentsFunction.setComponent(ModDataComponents.TOBACCO_QUALITY_STARS.get(), HYBRID_STARS.quality()))
                .apply(SetComponentsFunction.setComponent(SelectionRegistry.TOBACCO_LINEAGE.get(), HYBRID_LINEAGE)));
        LootPool.Builder bulb = WorldLootProvider.rareBulbs().when(LootItemRandomChanceCondition.randomChance(BULB_CHANCE));
        output.accept(HUT_CHEST, LootTable.lootTable().withPool(WorldLootProvider.noteBottles()).withPool(seeds).withPool(hybrid).withPool(bulb));
    }
}
