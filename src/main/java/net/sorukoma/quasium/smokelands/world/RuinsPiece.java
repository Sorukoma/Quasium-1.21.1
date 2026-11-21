package net.sorukoma.quasium.smokelands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;

public class RuinsPiece extends AshPiece {
    public enum Kind {
        LOUNGE(9, 7, 9), COLONNADE(11, 8, 11), HALL(13, 7, 7);

        final int width;
        final int height;
        final int depth;

        Kind(int width, int height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }

    private final Kind kind;
    private boolean veiled;

    public RuinsPiece(Kind kind, BlockPos ground, Direction facing) {
        super(SmokelandsWorldgen.RUINS_PIECE.get(), makeBoundingBox(ground.getX(), ground.getY() - 1, ground.getZ(), facing,
                kind.width, kind.height, kind.depth), facing);
        this.kind = kind;
    }

    public RuinsPiece(CompoundTag tag) {
        super(SmokelandsWorldgen.RUINS_PIECE.get(), tag);
        kind = Kind.values()[Mth.clamp(tag.getInt("Kind"), 0, Kind.values().length - 1)];
        veiled = tag.getBoolean("Veiled");
    }

    public static RuinsPiece veiled(Kind kind, BlockPos ground, Direction facing) {
        RuinsPiece piece = new RuinsPiece(kind, ground, facing);
        piece.veiled = true;
        return piece;
    }

    public boolean veiled() {
        return veiled;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Kind", kind.ordinal());
        tag.putBoolean("Veiled", veiled);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        if (!veiled) build(level, box, random);
    }

    public void reveal(WorldGenLevel level, RandomSource random) {
        veiled = false;
        build(level, getBoundingBox(), random);
    }

    @Override
    protected boolean canBeReplaced(LevelReader level, int x, int y, int z, BoundingBox box) {
        return level.getBlockEntity(getWorldPos(x, y, z)) == null;
    }

    private void build(WorldGenLevel level, BoundingBox box, RandomSource random) {
        switch (kind) {
            case LOUNGE -> lounge(level, box, random);
            case COLONNADE -> colonnade(level, box, random);
            case HALL -> hall(level, box, random);
        }
    }

    private void lounge(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int last = kind.width - 1;
        for (int x = 0; x <= last; x++) {
            for (int z = 0; z <= last; z++) {
                int ring = Math.max(Math.abs(x - 4), Math.abs(z - 4));
                floor(level, box, ring == 2 ? mosaic(x + z) : state(SmokelandsBlocks.ASH_BRICKS), x, z);
                clearAbove(level, box, x, z, kind.height - 1);
                boolean edge = x == 0 || z == 0 || x == last || z == last;
                if (!edge) continue;
                int standing = 4 - Math.floorMod(Mth.getSeed(getWorldX(x, z), 0, getWorldZ(x, z)) >> 20, 4);
                for (int y = 1; y <= standing; y++) brick(level, box, x, y, z, 30);
            }
        }
        for (int corner : new int[]{0, last}) {
            column(level, box, state(SmokelandsBlocks.PETRIFIED_SMOKE), corner, 0, 5);
            column(level, box, state(SmokelandsBlocks.PETRIFIED_SMOKE), corner, last, worn(corner, 5, last, 50) ? 3 : 5);
        }
        clearAbove(level, box, 4, 0, 2);
        dust(level, box, 1, last - 1, 1, last - 1);
        flask(level, box, 1, 1);
        flask(level, box, last - 1, last - 1);
        createChest(level, box, random, 4, 1, 4, SmokelandsWorldgen.RUINS_CHEST);
    }

    private void colonnade(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int centre = kind.width / 2;
        for (int x = 0; x < kind.width; x++) {
            for (int z = 0; z < kind.depth; z++) {
                double distance = Math.hypot(x - centre, z - centre);
                if (distance > centre + 0.5) continue;
                boolean dais = Math.abs(x - centre) <= 1 && Math.abs(z - centre) <= 1;
                int sector = (int) Math.floor((Math.atan2(z - centre, x - centre) + Math.PI) / (Math.PI / 3));
                floor(level, box, dais ? state(SmokelandsBlocks.PETRIFIED_SMOKE)
                        : Math.abs(distance - 3) < 0.6 ? mosaic(sector) : state(SmokelandsBlocks.ASH_BRICKS), x, z);
                clearAbove(level, box, x, z, kind.height - 1);
            }
        }
        for (int index = 0; index < 8; index++) {
            double angle = index * Math.PI / 4;
            int x = centre + (int) Math.round(Math.cos(angle) * 4.5);
            int z = centre + (int) Math.round(Math.sin(angle) * 4.5);
            int height = 2 + Math.floorMod(Mth.getSeed(getWorldX(x, z), 1, getWorldZ(x, z)) >> 18, 5);
            column(level, box, state(SmokelandsBlocks.PETRIFIED_SMOKE), x, z, height);
            if (height >= 5) placeBlock(level, mosaic(index), x, height + 1, z, box);
        }
        dust(level, box, 1, kind.width - 2, 1, kind.depth - 2);
        flask(level, box, centre + 2, centre);
        createChest(level, box, random, centre, 1, centre, SmokelandsWorldgen.RUINS_CHEST);
    }

    private void hall(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int lastX = kind.width - 1;
        int lastZ = kind.depth - 1;
        for (int x = 0; x <= lastX; x++) {
            for (int z = 0; z <= lastZ; z++) {
                floor(level, box, state(SmokelandsBlocks.ASH_BRICKS), x, z);
                clearAbove(level, box, x, z, kind.height - 1);
                boolean wall = z == 0 || z == lastZ || x == 0 || x == lastX;
                for (int y = 1; y <= 4 && wall; y++) {
                    if (y == 2 && (z == 0 || z == lastZ) && x % 2 == 1) {
                        placeBlock(level, mosaic(x / 2), x, y, z, box);
                    } else if (!worn(x, y, z, y * 9)) {
                        brick(level, box, x, y, z, 35);
                    }
                }
                if (!worn(x, 5, z, 45)) brick(level, box, x, 5, z, 50);
            }
        }
        clearAbove(level, box, 0, lastZ / 2, 2);
        dust(level, box, 1, lastX - 1, 1, lastZ - 1);
        flask(level, box, 2, 1);
        flask(level, box, 2, lastZ - 1);
        flask(level, box, lastX - 2, 1);
        createChest(level, box, random, lastX - 1, 1, lastZ / 2, SmokelandsWorldgen.RUINS_CHEST);
    }

    private void dust(WorldGenLevel level, BoundingBox box, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (worn(x, 1, z, 14)) placeBlock(level, state(SmokelandsBlocks.ASH_SAND), x, 1, z, box);
            }
        }
    }

    private void flask(WorldGenLevel level, BoundingBox box, int x, int z) {
        if (!worn(x, 1, z, 35)) placeBlock(level, state(SmokelandsBlocks.ANCIENT_FLASK), x, 1, z, box);
    }
}
