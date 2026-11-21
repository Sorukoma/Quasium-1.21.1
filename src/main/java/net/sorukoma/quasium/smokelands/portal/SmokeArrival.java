package net.sorukoma.quasium.smokelands.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;

import java.util.Comparator;
import java.util.Optional;

public final class SmokeArrival {
    private static final int SEARCH_RADIUS = 32;

    private SmokeArrival() {
    }

    public static BlockPos arrive(ServerLevel level, BlockPos target) {
        return nearestPortal(level, target).orElseGet(() -> build(level, target));
    }

    static Optional<BlockPos> nearestPortal(ServerLevel level, BlockPos target) {
        PoiManager poi = level.getPoiManager();
        poi.ensureLoadedAndValid(level, target, SEARCH_RADIUS);
        return poi.getInSquare(type -> type.is(SmokePortals.POI_KEY), target, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(pos -> level.getBlockState(pos).is(SmokePortals.PORTAL.get()))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(target)))
                .map(pos -> bottomOf(level, pos));
    }

    private static BlockPos bottomOf(ServerLevel level, BlockPos pos) {
        BlockPos bottom = pos;
        while (level.getBlockState(bottom.below()).is(SmokePortals.PORTAL.get())) bottom = bottom.below();
        return bottom;
    }

    static BlockPos build(ServerLevel level, BlockPos target) {
        level.getChunk(target.getX() >> 4, target.getZ() >> 4);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        int top = level.getMinBuildHeight() + level.getLogicalHeight() - SmokePortalShape.HEIGHT - 3;
        BlockPos origin = new BlockPos(target.getX(), Mth.clamp(surface, level.getMinBuildHeight() + 2, top), target.getZ());
        SmokePortalShape shape = new SmokePortalShape(origin, Direction.Axis.X);
        BlockState frame = SmokelandsBlocks.PETRIFIED_SMOKE.get().defaultBlockState();
        for (int across = -1; across <= SmokePortalShape.WIDTH; across++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos ground = shape.at(across, -1).relative(Direction.SOUTH, side);
                if (!level.getBlockState(ground).isSolid()) level.setBlockAndUpdate(ground, frame);
            }
            for (int up = 0; up < SmokePortalShape.HEIGHT; up++) {
                level.setBlockAndUpdate(shape.at(across, up).south(), Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(shape.at(across, -1), frame);
            level.setBlockAndUpdate(shape.at(across, SmokePortalShape.HEIGHT), frame);
        }
        for (int up = 0; up < SmokePortalShape.HEIGHT; up++) {
            level.setBlockAndUpdate(shape.at(-1, up), frame);
            level.setBlockAndUpdate(shape.at(SmokePortalShape.WIDTH, up), frame);
        }
        shape.fill(level);
        return origin.south();
    }
}
