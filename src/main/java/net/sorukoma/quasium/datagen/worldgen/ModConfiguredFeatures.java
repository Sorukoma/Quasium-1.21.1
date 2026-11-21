package net.sorukoma.quasium.datagen.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.selection.SelectionRegistry;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> QUASIUM_ORE = createKey("quasium_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_DIKAR = createKey("patch_dikar");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_PESCHANIK = createKey("patch_peschanik");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_BOLOTNIK = createKey("patch_bolotnik");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_LEDYANOY = createKey("patch_ledyanoy");
    public static final ResourceKey<ConfiguredFeature<?, ?>> QUASIUM_GEODE = createKey("quasium_geode");

    /** Сколько попыток посадки внутри одного пятна — определяет плотность куста в пятне. */
    private static final int PATCH_TRIES = 8;

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

        // Дикие сорта табака: пятна кустов на подходящей почве в своих биомах
        context.register(PATCH_DIKAR, patch(ModBlocks.DIKAR_CROP.get(), BlockTags.DIRT));
        context.register(PATCH_PESCHANIK, patch(ModBlocks.PESCHANIK_CROP.get(), BlockTags.SAND));
        context.register(PATCH_BOLOTNIK, patch(ModBlocks.BOLOTNIK_CROP.get(), BlockTags.DIRT));
        context.register(PATCH_LEDYANOY, patch(ModBlocks.LEDYANOY_CROP.get(), BlockTags.DIRT));

        context.register(QUASIUM_GEODE, new ConfiguredFeature<>(SelectionRegistry.QUASIUM_GEODE.get(), quasiumGeode()));
    }

    private static GeodeConfiguration quasiumGeode() {
        BlockStateProvider crust = BlockStateProvider.simple(SelectionRegistry.QUASIUM_CRUST.get());
        return new GeodeConfiguration(
                new GeodeBlockSettings(BlockStateProvider.simple(Blocks.AIR), crust, BlockStateProvider.simple(SelectionRegistry.BUDDING_QUASIUM.get()),
                        BlockStateProvider.simple(Blocks.CALCITE), crust,
                        SelectionRegistry.BUD_STAGES.stream().map(stage -> stage.get().defaultBlockState()).toList(),
                        BlockTags.FEATURES_CANNOT_REPLACE, BlockTags.GEODE_INVALID_BLOCKS),
                new GeodeLayerSettings(2.0, 2.5, 3.6, 5.0),
                new GeodeCrackSettings(0.0, 2.0, 2),
                0.35, 0.083, true,
                UniformInt.of(4, 6), ConstantInt.of(4), UniformInt.of(1, 2),
                -16, 16, 0.02, 1);
    }

    private static ConfiguredFeature<RandomPatchConfiguration, ?> patch(Block crop, TagKey<Block> groundTag) {
        // Куст ставится только в воздух и только над блоком нужного тега — проверка выполняется
        // на этапе генерации чанка, где обращение к соседям легально.
        BlockPredicate predicate = BlockPredicate.allOf(
                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesTag(Direction.DOWN.getNormal(), groundTag)
        );

        return new ConfiguredFeature<>(Feature.RANDOM_PATCH, new RandomPatchConfiguration(
                PATCH_TRIES, 7, 3,
                PlacementUtils.filtered(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(crop.defaultBlockState())),
                        predicate
                )
        ));
    }
}
