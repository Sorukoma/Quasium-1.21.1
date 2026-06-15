package net.sorukoma.quasium.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.datagen.worldgen.ModConfiguredFeatures;
import net.sorukoma.quasium.datagen.worldgen.ModPlacedFeatures;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<net.minecraft.core.HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Worldgen: configured features + placed features
        RegistrySetBuilder worldgenBuilder = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
                .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap);

        DatapackBuiltinEntriesProvider worldgenProvider = new DatapackBuiltinEntriesProvider(
                packOutput, lookupProvider, worldgenBuilder, Set.of(QuasiumMod.MOD_ID));
        generator.addProvider(event.includeServer(), worldgenProvider);

        CompletableFuture<net.minecraft.core.HolderLookup.Provider> worldgenLookupProvider = worldgenProvider.getRegistryProvider();

        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new ModAdvancementProvider(packOutput, worldgenLookupProvider, event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new ModGlobalLootModifiersProvider(packOutput, worldgenLookupProvider));
    }
}