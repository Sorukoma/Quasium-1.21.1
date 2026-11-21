package net.sorukoma.quasium.smokelands.world;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;

abstract class AshPiece extends StructurePiece {
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final NoteType[] NOTES = NoteType.values();

    protected AshPiece(StructurePieceType type, BoundingBox box, Direction facing) {
        super(type, 0, box);
        setOrientation(facing);
    }

    protected AshPiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
    }

    protected boolean worn(int x, int y, int z, int pct) {
        long seed = Mth.getSeed(getWorldX(x, z), getWorldY(y), getWorldZ(x, z));
        return Math.floorMod(seed >> 17, 100) < pct;
    }

    protected static BlockState state(DeferredBlock<?> block) {
        return block.get().defaultBlockState();
    }

    protected static BlockState mosaic(int index) {
        return state(SmokelandsBlocks.MOSAICS.get(NOTES[Math.floorMod(index, NOTES.length)]));
    }

    protected void brick(WorldGenLevel level, BoundingBox box, int x, int y, int z, int crackedPct) {
        placeBlock(level, worn(x, y, z, crackedPct) ? state(SmokelandsBlocks.CRACKED_ASH_BRICKS) : state(SmokelandsBlocks.ASH_BRICKS),
                x, y, z, box);
    }

    protected void floor(WorldGenLevel level, BoundingBox box, BlockState top, int x, int z) {
        placeBlock(level, top, x, 0, z, box);
        fillColumnDown(level, state(SmokelandsBlocks.ASH_STONE), x, -1, z, box);
    }

    protected void clearAbove(WorldGenLevel level, BoundingBox box, int x, int z, int height) {
        for (int y = 1; y <= height; y++) placeBlock(level, AIR, x, y, z, box);
    }

    protected void column(WorldGenLevel level, BoundingBox box, BlockState block, int x, int z, int height) {
        for (int y = 1; y <= height; y++) placeBlock(level, block, x, y, z, box);
    }
}
