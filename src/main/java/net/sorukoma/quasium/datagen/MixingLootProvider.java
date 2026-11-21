package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.sorukoma.quasium.block.MixingBlocks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MixingLootProvider extends BlockLootSubProvider {
    public MixingLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    public static DataProvider tables(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        LootTableProvider tables = new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(MixingLootProvider::new, LootContextParamSets.BLOCK)), registries);
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return tables.run(cache);
            }

            @Override
            public String getName() {
                return "Mixing table loot";
            }
        };
    }

    @Override
    protected void generate() {
        dropSelf(MixingBlocks.MIXING_TABLE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(MixingBlocks.MIXING_TABLE.get());
    }
}
