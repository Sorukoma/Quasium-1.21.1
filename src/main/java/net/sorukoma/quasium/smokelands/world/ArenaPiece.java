package net.sorukoma.quasium.smokelands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;
import net.sorukoma.quasium.smokelands.sultan.SultanThroneBlock;
import net.sorukoma.quasium.smokelands.sultan.Sultans;

public class ArenaPiece extends AshPiece {
    public static final int HEADROOM = 14;
    private static final int[] MOSAIC_RINGS = {8, 16, 24};
    private static final int PILLARS = 8;
    private static final int PILLAR_HEIGHT = 9;

    private final int radius;

    public ArenaPiece(BlockPos ground, int radius) {
        super(SmokelandsWorldgen.ARENA_PIECE.get(), new BoundingBox(ground.getX() - radius, ground.getY() - 1, ground.getZ() - radius,
                ground.getX() + radius, ground.getY() + HEADROOM, ground.getZ() + radius), Direction.NORTH);
        this.radius = radius;
    }

    public ArenaPiece(CompoundTag tag) {
        super(SmokelandsWorldgen.ARENA_PIECE.get(), tag);
        radius = tag.getInt("Radius");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Radius", radius);
    }

    public BlockPos throne() {
        return getWorldPos(radius, 1, radius).immutable();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        for (int x = 0; x <= 2 * radius; x++) {
            for (int z = 0; z <= 2 * radius; z++) {
                double distance = Math.hypot(x - radius, z - radius);
                if (distance > radius + 0.5) continue;
                floor(level, box, ground(x, z, distance), x, z);
                clearAbove(level, box, x, z, HEADROOM);
            }
        }
        pillars(level, box);
        dais(level, box);
        BlockPos throne = throne();
        if (box.isInside(throne)) level.scheduleTick(throne, Sultans.THRONE.get(), SultanThroneBlock.CHECK_TICKS);
    }

    private BlockState ground(int x, int z, double distance) {
        if (distance >= radius - 1) return state(SmokelandsBlocks.PETRIFIED_SMOKE);
        int sector = (int) Math.floor((Math.atan2(z - radius, x - radius) + Math.PI) / (Math.PI / 3));
        for (int ring : MOSAIC_RINGS) {
            if (Math.abs(distance - ring) < 0.6) return mosaic(sector + ring / 8);
        }
        return worn(x, 0, z, 12) ? state(SmokelandsBlocks.CRACKED_ASH_BRICKS) : state(SmokelandsBlocks.ASH_BRICKS);
    }

    private void pillars(WorldGenLevel level, BoundingBox box) {
        for (int index = 0; index < PILLARS; index++) {
            double angle = index * Math.PI * 2 / PILLARS;
            int x = radius + (int) Math.round(Math.cos(angle) * (radius - 4));
            int z = radius + (int) Math.round(Math.sin(angle) * (radius - 4));
            int height = worn(x, 1, z, 35) ? PILLAR_HEIGHT - 4 : PILLAR_HEIGHT;
            column(level, box, state(SmokelandsBlocks.PETRIFIED_SMOKE), x, z, height);
            placeBlock(level, mosaic(index), x, height + 1, z, box);
            if (height == PILLAR_HEIGHT) placeBlock(level, state(SmokelandsBlocks.SMOLDER), x, height + 2, z, box);
        }
    }

    private void dais(WorldGenLevel level, BoundingBox box) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                placeBlock(level, state(SmokelandsBlocks.PETRIFIED_SMOKE), radius + dx, 0, radius + dz, box);
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) placeBlock(level, state(SmokelandsBlocks.SMOLDER), radius + dx, 1, radius + dz, box);
            }
        }
        placeBlock(level, state(Sultans.THRONE), radius, 1, radius, box);
    }
}
