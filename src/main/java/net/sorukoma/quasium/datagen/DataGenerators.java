package net.sorukoma.quasium.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.datagen.worldgen.ModConfiguredFeatures;
import net.sorukoma.quasium.datagen.worldgen.ModPlacedFeatures;

import java.util.List;
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
        generator.addProvider(event.includeServer(), new FlavorRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new LoungeRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new DraftRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new MarkRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new ModAdvancementProvider(packOutput, worldgenLookupProvider, event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new FirstContactItemTags(packOutput, lookupProvider, event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new ModGlobalLootModifiersProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(LoungeLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(GeneratorLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(MachineLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(ForgeLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(DraftLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(KilnLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(MarkLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(registries -> ModAdvancementProvider::bookGift,
                                LootContextParamSets.ADVANCEMENT_REWARD)),
                worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new MixingRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), MixingLootProvider.tables(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new GeneratorRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new MachineRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new ForgeRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new CartRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new WardenRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new KilnRecipeProvider(packOutput, worldgenLookupProvider));
        generator.addProvider(event.includeServer(), new TetradRecipeProvider(packOutput, worldgenLookupProvider));
        net.sorukoma.quasium.selection.SelectionDatagen.gather(event, generator, packOutput, worldgenLookupProvider);
        net.sorukoma.quasium.ecology.EcologyDatagen.gather(event, generator, packOutput, worldgenLookupProvider);
        net.sorukoma.quasium.mind.MindDatagen.gather(event, generator, packOutput, worldgenLookupProvider);
        net.sorukoma.quasium.ritual.RitualDatagen.gather(event, generator, packOutput, worldgenLookupProvider);
        net.sorukoma.quasium.smokelands.SmokelandsDatagen.gather(event, generator, packOutput, worldgenLookupProvider);
    }
}