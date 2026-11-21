package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

final class ClayHarvestGoal extends Goal {
    private static final double REACH = 1.8;
    private static final int RESCAN_TICKS = 20;
    private static final int SWING_TICKS = 8;

    private final ClayGolemEntity golem;
    private final ClayTrek trek;
    @Nullable
    private ClayFields.Work work;
    private BlockPos field = BlockPos.ZERO;
    private int carriedBefore;
    private int nextScan;
    private int pause;
    private int done;

    ClayHarvestGoal(ClayGolemEntity golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (GolemPrograms.running(golem) || GolemSmoke.withOwner(golem)) return false;
        if (golem.tickCount < nextScan || !golem.hasRoom() || !EventHooks.canEntityGrief(golem.level(), golem) || !golem.mayWork()) return false;
        nextScan = golem.tickCount + golem.paced(RESCAN_TICKS);
        work = ClayFields.findWork(golem);
        if (work == null && golem.job() == GolemJob.TOBACCO && ClayFields.fields(golem).isEmpty()) golem.waitFor("no_harvest_mark");
        return work != null;
    }

    @Override
    public boolean canContinueToUse() {
        return work != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        done = 0;
        pause = 0;
        field = work.mark();
        carriedBefore = golem.cargoCount();
        golem.note("harvest_start", field);
        trek.to(work.spot());
    }

    @Override
    public void stop() {
        work = null;
        golem.getNavigation().stop();
        if (done == 0) return;
        int gathered = golem.cargoCount() - carriedBefore;
        if (gathered > 0) golem.note("harvested", gathered, field);
        golem.endHarvestTrip();
    }

    @Override
    public void tick() {
        if (work == null) return;
        golem.getLookControl().setLookAt(work.spot());
        if (pause > 0) {
            pause--;
            return;
        }
        if (!ClayFields.stillThere(golem, work)) {
            next();
        } else if (trek.arrived(REACH)) {
            harvest(work);
            next();
        } else if (trek.stuck()) {
            golem.markUnreachable(work.pos());
            next();
        }
    }

    private void harvest(ClayFields.Work target) {
        if (target.item() != null) golem.pickUp(target.item());
        else golem.takeIn(ClayFields.reap((ServerLevel) golem.level(), target.pos(), golem), target.pos());
        golem.swing(InteractionHand.MAIN_HAND);
        pause = golem.paced(SWING_TICKS);
        done++;
    }

    private void next() {
        work = golem.hasRoom() ? ClayFields.findWork(golem) : null;
        if (work != null) trek.to(work.spot());
    }
}
