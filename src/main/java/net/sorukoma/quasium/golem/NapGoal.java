package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

final class NapGoal extends Goal {
    private static final int BED_RADIUS = 24;
    private static final int RESCAN_TICKS = 40;
    private static final double REACH = 1.6;

    private final ClayGolemEntity golem;
    private final ClayTrek trek;
    @Nullable
    private BlockPos bed;
    private int nextScan;

    NapGoal(ClayGolemEntity golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Nullable
    private GolemNap nap() {
        return golem.getExistingData(SmokeMarks.NAP).orElse(null);
    }

    @Override
    public boolean canUse() {
        GolemNap nap = nap();
        if (nap == null) return false;
        if (nap.asleep) return true;
        if (!nap.wantsBed || golem.tickCount < nextScan) return false;
        nextScan = golem.tickCount + RESCAN_TICKS;
        bed = nearestBed();
        return bed != null;
    }

    @Override
    public boolean canContinueToUse() {
        GolemNap nap = nap();
        if (nap == null) return false;
        return nap.asleep || nap.wantsBed && bed != null && free(bed);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        if (bed == null) return;
        golem.note("nap_walk", bed);
        trek.to(Vec3.atCenterOf(bed));
    }

    @Override
    public void stop() {
        bed = null;
        golem.getNavigation().stop();
        if (golem.isSleeping()) golem.stopSleeping();
    }

    @Override
    public void tick() {
        GolemNap nap = nap();
        if (nap == null || nap.asleep || bed == null) {
            golem.getNavigation().stop();
            return;
        }
        if (trek.arrived(REACH)) {
            lieDown(nap, bed);
        } else if (trek.stuck()) {
            golem.markUnreachable(bed);
            bed = null;
        }
    }

    private void lieDown(GolemNap nap, BlockPos at) {
        golem.getNavigation().stop();
        golem.startSleeping(at);
        nap.asleep = true;
        nap.wantsBed = false;
        golem.note("nap_asleep", at);
    }

    @Nullable
    private BlockPos nearestBed() {
        PoiManager pois = ((ServerLevel) golem.level()).getPoiManager();
        return pois.findClosest(type -> type.is(PoiTypes.HOME), this::free, golem.blockPosition(), BED_RADIUS, PoiManager.Occupancy.ANY)
                .orElse(null);
    }

    private boolean free(BlockPos pos) {
        BlockState state = golem.level().getBlockState(pos);
        return state.getBlock() instanceof BedBlock && !state.getValue(BedBlock.OCCUPIED) && !golem.isUnreachable(pos);
    }
}
