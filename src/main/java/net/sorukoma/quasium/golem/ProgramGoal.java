package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.golem.ProgramHands.Result;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.ProgramTableBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

final class ProgramGoal extends Goal {
    private static final double STAND_REACH = 1.5;
    private static final double WORK_REACH = 2.8;
    private static final int GIVE_UP_TICKS = 600;
    private static final int SWING_TICKS = 12;

    private final ClayGolemEntity golem;
    private final ClayTrek trek;
    private final ProgramFlow flow;
    @Nullable
    private ProgramTask task;
    @Nullable
    private BlockPos source;
    private int walked;
    private int stay;
    private int pause;

    ProgramGoal(ClayGolemEntity golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        this.flow = new ProgramFlow(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return flow.awake() && GolemPrograms.running(golem) && golem.mayWork();
    }

    @Override
    public boolean canContinueToUse() {
        return flow.awake() && golem.mood().canWork() && GolemPrograms.running(golem);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        task = null;
        pause = 0;
        if (!flow.wake()) golem.note("program_start", GolemPrograms.table(golem));
    }

    @Override
    public void stop() {
        task = null;
        golem.getNavigation().stop();
        flow.settle();
        if (source != null && golem.level().isLoaded(source) && golem.level().getBlockEntity(source) instanceof ProgramTableBlockEntity table) {
            table.running(golem.getUUID(), -1);
        }
    }

    @Override
    public void tick() {
        if (pause > 0) {
            pause--;
            return;
        }
        ProgramTableBlockEntity table = GolemPrograms.program(golem);
        if (table == null) return;
        if (task == null) {
            source = table.getBlockPos();
            ProgramTask next = flow.next(table);
            if (next != null) begin(next);
        } else if (!table.getBlockPos().equals(source) || table.edits() != ProgramCursor.of(golem).edits) {
            task = null;
            golem.getNavigation().stop();
        } else if (task.type() == BlockKind.WAIT_SIGNAL) {
            listen();
        } else if (stay > 0) {
            stay--;
        } else if (stay == 0) {
            finish(Result.DONE);
        } else {
            walk();
        }
    }

    private void begin(ProgramTask next) {
        task = next;
        walked = 0;
        stay = -1;
        switch (next.type()) {
            case WAIT -> stay = seconds(next);
            case WAIT_SIGNAL -> golem.getNavigation().stop();
            default -> trek.to(next.spot());
        }
    }

    private void listen() {
        golem.getLookControl().setLookAt(task.spot());
        if (golem.tickCount % Config.PROGRAM_SIGNAL_CHECK_TICKS.getAsInt() != 0) return;
        if (ProgramChecks.powered(golem.level(), task.place())) finish(Result.DONE);
    }

    private void walk() {
        golem.getLookControl().setLookAt(task.spot());
        if (trek.arrived(task.type() == BlockKind.GO ? STAND_REACH : WORK_REACH)) {
            arrive();
        } else if (++walked >= golem.paced(GIVE_UP_TICKS)) {
            golem.note("program_unreachable", task.place());
            finish(Result.FAIL);
        } else {
            trek.stuck();
        }
    }

    private void arrive() {
        golem.getNavigation().stop();
        if (task.type() == BlockKind.GO) {
            stay = seconds(task);
            return;
        }
        Result result = ProgramHands.use(golem, task, false);
        if (result == Result.DONE) {
            golem.swing(InteractionHand.MAIN_HAND);
            pause = golem.paced(SWING_TICKS);
            flow.worked(task);
        }
        finish(result);
    }

    private void finish(Result result) {
        ProgramTask ended = task;
        task = null;
        flow.finished(ended, result);
    }

    private static int seconds(ProgramTask task) {
        return task.block().number() * 20;
    }
}
