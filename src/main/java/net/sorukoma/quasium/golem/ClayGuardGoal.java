package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class ClayGuardGoal extends Goal {
    private static final double STAY_RADIUS = 3.0;
    private static final int RESCAN_TICKS = 40;

    private final ClayGolemEntity golem;
    private Vec3 post = Vec3.ZERO;
    private int nextScan;

    ClayGuardGoal(ClayGolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (GolemPrograms.running(golem)) return false;
        if (golem.tickCount < nextScan) return false;
        nextScan = golem.tickCount + RESCAN_TICKS;
        BlockPos mark = golem.boundGuardMark();
        if (mark == null) return false;
        post = Vec3.atBottomCenterOf(mark);
        return golem.distanceToSqr(post) > STAY_RADIUS * STAY_RADIUS;
    }

    @Override
    public boolean canContinueToUse() {
        return !golem.getNavigation().isDone();
    }

    @Override
    public void start() {
        golem.getNavigation().moveTo(post.x, post.y, post.z, 1.0);
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
    }
}
