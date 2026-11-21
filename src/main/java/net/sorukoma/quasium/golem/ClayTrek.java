package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

final class ClayTrek {
    private static final int CHECK_TICKS = 20;
    private static final int STUCK_CHECKS = 4;
    private static final double MOVED_SQR = 0.25 * 0.25;

    private final WorkerGolem golem;
    private Vec3 spot = Vec3.ZERO;
    private Vec3 stand = Vec3.ZERO;
    private Vec3 last = Vec3.ZERO;
    private int stalls;

    ClayTrek(WorkerGolem golem) {
        this.golem = golem;
    }

    void to(Vec3 target) {
        spot = target;
        stand = beside(target);
        last = golem.position();
        stalls = 0;
        golem.getNavigation().moveTo(stand.x, stand.y, stand.z, 1.0);
    }

    boolean arrived(double reach) {
        return golem.distanceToSqr(spot) <= reach * reach;
    }

    boolean stuck() {
        if (golem.tickCount % CHECK_TICKS != 0) return false;
        stalls = golem.position().distanceToSqr(last) > MOVED_SQR ? 0 : stalls + 1;
        last = golem.position();
        if (golem.getNavigation().isDone()) golem.getNavigation().moveTo(stand.x, stand.y, stand.z, 1.0);
        return stalls >= STUCK_CHECKS;
    }

    private Vec3 beside(Vec3 target) {
        BlockPos crop = BlockPos.containing(target);
        BlockState state = golem.level().getBlockState(crop);
        if (!(state.getBlock() instanceof CropBlock block) || !block.isMaxAge(state)) return target;
        Vec3 best = target;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos near = crop.relative(side);
            Vec3 option = Vec3.atBottomCenterOf(near);
            if (free(near) && (best == target || golem.distanceToSqr(option) < golem.distanceToSqr(best))) best = option;
        }
        return best;
    }

    private boolean free(BlockPos pos) {
        PathType type = WalkNodeEvaluator.getPathTypeStatic(golem, pos);
        return (type == PathType.WALKABLE || type == PathType.WATER_BORDER) && !isCrop(pos);
    }

    private boolean isCrop(BlockPos pos) {
        return golem.level().getBlockState(pos).getBlock() instanceof CropBlock;
    }
}
