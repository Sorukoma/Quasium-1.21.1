package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

abstract class GolemErrandGoal extends Goal {
    private static final double REACH = 2.5;
    private static final int GIVE_UP_TICKS = 400;
    private static final int REPATH_TICKS = 20;

    protected final CalloudGolemEntity golem;
    private final int searchTicks;
    private final String errand;
    @Nullable
    private BlockPos target;
    @Nullable
    private BlockPos next;
    private int ticks;
    private int nextSearch;

    GolemErrandGoal(CalloudGolemEntity golem, int searchTicks, String errand) {
        this.golem = golem;
        this.searchTicks = searchTicks;
        this.errand = errand;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    protected abstract boolean wanted();

    @Nullable
    protected abstract BlockPos find();

    protected abstract void arrive(BlockPos pos);

    protected void then(BlockPos pos) {
        next = pos;
    }

    @Nullable
    protected IItemHandler store(BlockPos pos) {
        return golem.level().isLoaded(pos) ? golem.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) : null;
    }

    @Override
    public boolean canUse() {
        if (--nextSearch > 0 || !wanted() || !golem.mayWork()) return false;
        nextSearch = reducedTickDelay(golem.paced(searchTicks));
        target = find();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && ticks < golem.paced(GIVE_UP_TICKS) && wanted();
    }

    @Override
    public void start() {
        ticks = 0;
        golem.note(errand + "_start", target);
    }

    @Override
    public void stop() {
        if (target != null && ticks >= golem.paced(GIVE_UP_TICKS)) golem.note("unreachable", target);
        target = null;
        next = null;
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) return;
        golem.getLookControl().setLookAt(Vec3.atCenterOf(target));
        if (target.closerToCenterThan(golem.position(), REACH)) {
            arrive(target);
            target = next;
            next = null;
            ticks = 0;
        } else if (ticks++ % REPATH_TICKS == 0) {
            golem.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        }
    }
}
