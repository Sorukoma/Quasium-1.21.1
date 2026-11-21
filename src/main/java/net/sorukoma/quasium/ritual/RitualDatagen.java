package net.sorukoma.quasium.ritual;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.sorukoma.quasium.ritual.datagen.RitualAdvancementProvider;
import net.sorukoma.quasium.ritual.datagen.RitualLootProvider;
import net.sorukoma.quasium.ritual.datagen.RitualRecipeProvider;

import java.util.concurrent.CompletableFuture;

public final class RitualDatagen {
    private RitualDatagen() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup) {
        generator.addProvider(event.includeServer(), new RitualRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), RitualLootProvider.tables(output, lookup));
        generator.addProvider(event.includeServer(), RitualAdvancementProvider.create(output, lookup, event.getExistingFileHelper()));
    }
}
