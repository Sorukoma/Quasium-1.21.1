package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ZoneBox {
    public static final int DEFAULT_HALF = 4;
    public static final int MAX_SPAN = 32;
    public static final int MAX_HEIGHT = 16;

    private ZoneBox() {
    }

    public static BoundingBox of(BlockGetter level, BlockPos mark) {
        BlockPos corner = level.getBlockEntity(mark) instanceof SmokeMarkBlockEntity entity ? entity.corner() : null;
        if (corner == null) {
            return new BoundingBox(mark.getX() - DEFAULT_HALF, mark.getY() - 1, mark.getZ() - DEFAULT_HALF,
                    mark.getX() + DEFAULT_HALF, mark.getY() + 1, mark.getZ() + DEFAULT_HALF);
        }
        return BoundingBox.fromCorners(mark, corner);
    }

    public static AABB area(BlockGetter level, BlockPos mark) {
        return AABB.of(of(level, mark));
    }

    public static boolean fits(BlockPos mark, BlockPos corner) {
        BoundingBox box = BoundingBox.fromCorners(mark, corner);
        return box.getXSpan() <= MAX_SPAN && box.getZSpan() <= MAX_SPAN && box.getYSpan() <= MAX_HEIGHT;
    }

    static boolean loaded(Level level, BoundingBox box) {
        return level.hasChunksAt(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    static Iterable<BlockPos> blocks(BoundingBox box) {
        return BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    static List<BlockEntity> blockEntities(Level level, BoundingBox box) {
        ChunkPos from = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
        ChunkPos to = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
        return ChunkPos.rangeClosed(from, to)
                .flatMap(chunk -> level.getChunk(chunk.x, chunk.z).getBlockEntities().values().stream())
                .filter(entity -> box.isInside(entity.getBlockPos()))
                .toList();
    }
}
