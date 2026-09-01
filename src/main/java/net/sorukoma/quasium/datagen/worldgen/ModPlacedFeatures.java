package net.sorukoma.quasium.datagen.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.sorukoma.quasium.QuasiumMod;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> QUASIUM_ORE = createKey("quasium_ore");

    public static final ResourceKey<PlacedFeature> PATCH_DIKAR = createKey("patch_dikar");
    public static final ResourceKey<PlacedFeature> PATCH_PESCHANIK = createKey("patch_peschanik");
    public static final ResourceKey<PlacedFeature> PATCH_BOLOTNIK = createKey("patch_bolotnik");
    public static final ResourceKey<PlacedFeature> PATCH_LEDYANOY = createKey("patch_ledyanoy");

    /** Пятно кустов в среднем раз в N чанков. Чем больше — тем реже. */
    private static final int PATCH_RARITY = 12;

    private static ResourceKey<PlacedFeature> createKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name));
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder<ConfiguredFeature<?, ?>> oreFeature = configuredFeatures.getOrThrow(ModConfiguredFeatures.QUASIUM_ORE);

        // 4 veins per chunk, Y=-64 to Y=0 (deepslate level), uniform distribution
        context.register(QUASIUM_ORE, new PlacedFeature(oreFeature, List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(0)),
                BiomeFilter.biome()
        )));

        // Дикие сорта табака: редкие пятна кустов на поверхности своих биомов
        context.register(PATCH_DIKAR, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.PATCH_DIKAR), patchPlacement()));
        context.register(PATCH_PESCHANIK, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.PATCH_PESCHANIK), patchPlacement()));
        context.register(PATCH_BOLOTNIK, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.PATCH_BOLOTNIK), patchPlacement()));
        context.register(PATCH_LEDYANOY, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.PATCH_LEDYANOY), patchPlacement()));
    }

    private static List<PlacementModifier> patchPlacement() {
        return List.of(
                RarityFilter.onAverageOnceEvery(PATCH_RARITY),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome()
        );
    }
}