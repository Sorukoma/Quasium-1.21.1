package net.sorukoma.quasium.smokelands.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record SmokePortalShape(BlockPos origin, Direction.Axis axis) {
    public static final int WIDTH = 2;
    public static final int HEIGHT = 3;

    public static Optional<SmokePortalShape> find(LevelAccessor level, BlockPos inside) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Direction right = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            for (int across = 0; across < WIDTH; across++) {
                for (int up = 0; up < HEIGHT; up++) {
                    SmokePortalShape shape = new SmokePortalShape(inside.relative(right, -across).below(up), axis);
                    if (shape.isFramed(level) && shape.isOpen(level)) return Optional.of(shape);
                }
            }
        }
        return Optional.empty();
    }

    public BlockPos at(int across, int up) {
        return origin.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE), across).above(up);
    }

    public boolean isFramed(LevelAccessor level) {
        for (int across = 0; across < WIDTH; across++) {
            if (!isFrame(level.getBlockState(at(across, -1))) || !isFrame(level.getBlockState(at(across, HEIGHT)))) return false;
        }
        for (int up = 0; up < HEIGHT; up++) {
            if (!isFrame(level.getBlockState(at(-1, up))) || !isFrame(level.getBlockState(at(WIDTH, up)))) return false;
        }
        return true;
    }

    private boolean isOpen(LevelAccessor level) {
        for (int across = 0; across < WIDTH; across++) {
            for (int up = 0; up < HEIGHT; up++) {
                BlockState state = level.getBlockState(at(across, up));
                if (!state.isAir() && !state.is(SmokePortals.PORTAL.get())) return false;
            }
        }
        return true;
    }

    public void fill(LevelAccessor level) {
        BlockState portal = SmokePortals.PORTAL.get().defaultBlockState().setValue(SmokePortalBlock.AXIS, axis);
        for (int across = 0; across < WIDTH; across++) {
            for (int up = 0; up < HEIGHT; up++) level.setBlock(at(across, up), portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    public static boolean isFrame(BlockState state) {
        return state.is(SmokePortals.FRAMES);
    }
}
