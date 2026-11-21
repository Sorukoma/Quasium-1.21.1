package net.sorukoma.quasium.ritual.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.ritual.RitualRegistry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RitualLootProvider extends BlockLootSubProvider {
    public RitualLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    public static DataProvider tables(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        LootTableProvider tables = new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(RitualLootProvider::new, LootContextParamSets.BLOCK)), registries);
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return tables.run(cache);
            }

            @Override
            public String getName() {
                return "Ritual brazier loot";
            }
        };
    }

    @Override
    protected void generate() {
        dropSelf(RitualRegistry.RITUAL_BRAZIER.get());
        dropSelf(RitualRegistry.RITUAL_PEDESTAL.get());
        Block catcher = RitualRegistry.DREAM_CATCHER.get();
        add(catcher, LootTable.lootTable().withPool(applyExplosionCondition(catcher, LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(catcher).apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                        .include(ModDataComponents.ESSENCE_AMOUNT.get()))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(RitualRegistry.RITUAL_BRAZIER.get(), RitualRegistry.RITUAL_PEDESTAL.get(), RitualRegistry.DREAM_CATCHER.get());
    }
}
