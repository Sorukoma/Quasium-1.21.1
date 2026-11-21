package net.sorukoma.quasium.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class BlockShapes {
    private BlockShapes() {
    }

    public static Map<Direction, VoxelShape> horizontal(Direction modelFacing, VoxelShape shape) {
        Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
        Direction facing = modelFacing;
        for (int turn = 0; turn < 4; turn++) {
            byFacing.put(facing, shape);
            shape = clockwise(shape);
            facing = facing.getClockWise();
        }
        return byFacing;
    }

    private static VoxelShape clockwise(VoxelShape shape) {
        VoxelShape turned = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            turned = Shapes.or(turned, Shapes.box(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX));
        }
        return turned;
    }
}
