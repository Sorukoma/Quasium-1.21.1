package net.sorukoma.quasium.selection;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.sorukoma.quasium.selection.geode.GeodeLootProvider;
import net.sorukoma.quasium.selection.trade.HutLootProvider;
import net.sorukoma.quasium.selection.world.WorldLootProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SelectionDatagen {
    private SelectionDatagen() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup) {
        LootTableProvider tables = new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(GeodeLootProvider::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(registries -> new WorldLootProvider(), LootContextParamSets.CHEST),
                new LootTableProvider.SubProviderEntry(registries -> new HutLootProvider(), LootContextParamSets.CHEST)), lookup);
        generator.addProvider(event.includeServer(), new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return tables.run(cache);
            }

            @Override
            public String getName() {
                return "Selection loot tables";
            }
        });
    }
}
