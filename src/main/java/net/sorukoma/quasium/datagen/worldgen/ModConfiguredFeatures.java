package net.sorukoma.quasium.datagen.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> QUASIUM_ORE = createKey("quasium_ore");

    private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name));
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Руда квазиума спавнится только в дипслейт-блоках
        List<OreConfiguration.TargetBlockState> targetStates = List.of(
                OreConfiguration.target(
                        new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                        ModBlocks.DEEPSLATE_QUASIUM_ORE.get().defaultBlockState()
                )
        );

        context.register(QUASIUM_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targetStates, 4)));
    }
}