package net.sorukoma.quasium.smokelands.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class RuinsStructure extends Structure {
    public static final MapCodec<RuinsStructure> CODEC = simpleCodec(RuinsStructure::new);
    private static final int REACH = 6;
    private static final int MAX_SLOPE = 5;
    private static final int MIDDLE_TO_CORNER = 7;
    private static final int NUDGE = 5;
    private static final int[][] NUDGES = {{0, 0}, {-NUDGE, 0}, {NUDGE, 0}, {0, -NUDGE}, {0, NUDGE},
            {-NUDGE, -NUDGE}, {NUDGE, -NUDGE}, {-NUDGE, NUDGE}, {NUDGE, NUDGE}};

    public RuinsStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        for (int[] nudge : NUDGES) {
            int x = chunk.getMiddleBlockX() + nudge[0];
            int z = chunk.getMiddleBlockZ() + nudge[1];
            Optional<Integer> ground = SmokelandsWorldgen.dryGround(context, x, z, REACH, MAX_SLOPE);
            if (ground.isPresent()) return Optional.of(veiledRuins(context, new BlockPos(x - MIDDLE_TO_CORNER, ground.get(), z - MIDDLE_TO_CORNER)));
        }
        return Optional.empty();
    }

    private static GenerationStub veiledRuins(GenerationContext context, BlockPos corner) {
        RuinsPiece.Kind kind = RuinsPiece.Kind.values()[context.random().nextInt(RuinsPiece.Kind.values().length)];
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        return new GenerationStub(corner, builder -> builder.addPiece(RuinsPiece.veiled(kind, corner, facing)));
    }

    @Override
    public StructureType<?> type() {
        return SmokelandsWorldgen.RUINS.get();
    }
}
