package net.sorukoma.quasium.smokelands;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.sorukoma.quasium.QuasiumMod;

public final class SmokelandsDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, id("smokelands"));
    public static final ResourceLocation EFFECTS = id("smokelands");

    public static final ResourceKey<Biome> ASH_DUNES = biome("ash_dunes");
    public static final ResourceKey<Biome> SMOKE_SEA = biome("smoke_sea");
    public static final ResourceKey<Biome> COAL_MARSH = biome("coal_marsh");
    public static final ResourceKey<Biome> NICOTINE_JUNGLE = biome("nicotine_jungle");

    private SmokelandsDimension() {
    }

    public static boolean contains(Level level) {
        return level.dimension() == LEVEL;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, id(path));
    }
}
