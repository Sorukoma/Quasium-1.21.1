package net.sorukoma.quasium.smokelands.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;

import java.util.Optional;

public class ArenaStructure extends Structure {
    public static final MapCodec<ArenaStructure> CODEC = simpleCodec(ArenaStructure::new);
    private static final int MAX_SLOPE = 14;

    public ArenaStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int radius = SmokelandsConfig.ARENA_RADIUS.get();
        return SmokelandsWorldgen.dryGround(context, x, z, radius, MAX_SLOPE).map(ground -> {
            BlockPos centre = new BlockPos(x, ground, z);
            return new GenerationStub(centre, builder -> builder.addPiece(new ArenaPiece(centre, radius)));
        });
    }

    @Override
    public StructureType<?> type() {
        return SmokelandsWorldgen.ARENA.get();
    }
}
