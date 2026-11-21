package net.sorukoma.quasium.smokelands.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;

import java.util.Optional;

public final class SmokelandsWorldgen {
    public static final ResourceKey<LootTable> RUINS_CHEST = ResourceKey.create(Registries.LOOT_TABLE, SmokelandsDimension.id("chests/smoke_ruins"));
    public static final ResourceKey<Structure> RUINED_PORTAL_KEY = ResourceKey.create(Registries.STRUCTURE, SmokelandsDimension.id("ruined_smoke_portal"));
    public static final TagKey<Structure> ON_SULTAN_MAPS = TagKey.create(Registries.STRUCTURE, SmokelandsDimension.id("on_sultan_maps"));
    public static final DeferredHolder<LootPoolEntryType, LootPoolEntryType> OPTIONAL_ITEM =
            SmokelandsRegistry.LOOT_ENTRIES.register("optional_item", () -> new LootPoolEntryType(OptionalItemEntry.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<RuinsStructure>> RUINS =
            SmokelandsRegistry.STRUCTURE_TYPES.register("smoke_ruins", () -> () -> RuinsStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<ArenaStructure>> ARENA =
            SmokelandsRegistry.STRUCTURE_TYPES.register("sultan_arena", () -> () -> ArenaStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<RuinedPortalStructure>> RUINED_PORTAL =
            SmokelandsRegistry.STRUCTURE_TYPES.register("ruined_smoke_portal", () -> () -> RuinedPortalStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> RUINS_PIECE =
            SmokelandsRegistry.STRUCTURE_PIECES.register("smoke_ruins", () -> (StructurePieceType.ContextlessType) RuinsPiece::new);
    public static final DeferredHolder<Feature<?>, SmokeGeodeFeature> SMOKE_GEODE =
            SmokelandsRegistry.FEATURES.register("smoke_geode", SmokeGeodeFeature::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ARENA_PIECE =
            SmokelandsRegistry.STRUCTURE_PIECES.register("sultan_arena", () -> (StructurePieceType.ContextlessType) ArenaPiece::new);

    private SmokelandsWorldgen() {
    }

    public static void load() {
    }

    static Optional<Integer> dryGround(Structure.GenerationContext context, int centreX, int centreZ, int reach, int maxSlope) {
        int[][] samples = {{0, 0}, {-reach, -reach}, {reach, -reach}, {-reach, reach}, {reach, reach}};
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int sum = 0;
        for (int[] offset : samples) {
            int x = centreX + offset[0];
            int z = centreZ + offset[1];
            int surface = height(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
            if (surface != height(context, x, z, Heightmap.Types.OCEAN_FLOOR_WG)) return Optional.empty();
            lowest = Math.min(lowest, surface);
            highest = Math.max(highest, surface);
            sum += surface;
        }
        return highest - lowest > maxSlope ? Optional.empty() : Optional.of(sum / samples.length);
    }

    private static int height(Structure.GenerationContext context, int x, int z, Heightmap.Types type) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, type, context.heightAccessor(), context.randomState());
    }
}
