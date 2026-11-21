package net.sorukoma.quasium.smokelands;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.sorukoma.quasium.smokelands.datagen.SmokelandsAdvancements;
import net.sorukoma.quasium.smokelands.datagen.SmokelandsBlockLoot;
import net.sorukoma.quasium.smokelands.datagen.SmokelandsChestLoot;
import net.sorukoma.quasium.smokelands.datagen.SmokelandsEntityLoot;
import net.sorukoma.quasium.smokelands.datagen.SmokelandsRecipes;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SmokelandsDatagen {
    private SmokelandsDatagen() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup) {
        generator.addProvider(event.includeServer(), named("Smokelands loot", new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(SmokelandsBlockLoot::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(SmokelandsEntityLoot::new, LootContextParamSets.ENTITY),
                        new LootTableProvider.SubProviderEntry(SmokelandsChestLoot::new, LootContextParamSets.CHEST)), lookup)));
        generator.addProvider(event.includeServer(), new SmokelandsRecipes(output, lookup));
        generator.addProvider(event.includeServer(), named("Smokelands advancements",
                new AdvancementProvider(output, lookup, event.getExistingFileHelper(), List.of(new SmokelandsAdvancements()))));
    }

    private static DataProvider named(String name, DataProvider provider) {
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return provider.run(cache);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
