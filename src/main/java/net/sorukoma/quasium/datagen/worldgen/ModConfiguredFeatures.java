package net.sorukoma.quasium.datagen.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> QUASIUM_ORE = createKey("quasium_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_DIKAR = createKey("patch_dikar");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_PESCHANIK = createKey("patch_peschanik");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_BOLOTNIK = createKey("patch_bolotnik");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_LEDYANOY = createKey("patch_ledyanoy");

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
