package net.sorukoma.quasium.datagen.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.sorukoma.quasium.QuasiumMod;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> QUASIUM_ORE = createKey("quasium_ore");

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
    }
}