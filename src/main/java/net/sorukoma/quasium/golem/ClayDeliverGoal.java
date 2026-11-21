package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

final class ClayDeliverGoal extends Goal {
    private static final double REACH = 2.0;
    private static final int RESCAN_TICKS = 20;

    private final ClayGolemEntity golem;
    private final ClayTrek trek;
    @Nullable
    private Drop drop;
    private int nextScan;

    private record Drop(BlockPos pos, boolean linked) {
        Vec3 spot() {
            return linked ? Vec3.atCenterOf(pos) : Vec3.atBottomCenterOf(pos);
        }

        int unload(ClayGolemEntity golem, boolean simulate) {
            return linked ? golem.unloadInto(pos, null, simulate) : golem.unloadAt(pos, simulate);
        }
    }

    ClayDeliverGoal(ClayGolemEntity golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (GolemPrograms.running(golem) || GolemSmoke.withOwner(golem)) return false;
        if (golem.tickCount < nextScan || !golem.hasCargo() || !golem.mayWork()) return false;
        nextScan = golem.tickCount + golem.paced(RESCAN_TICKS);
        if (golem.hasRoom() && (golem.choring() || ClayFields.findWork(golem) != null)) return false;
        drop = findDropOff();
        if (drop == null && !golem.hasRoom()) golem.waitFor(golem.marks(MarkType.FILL).isEmpty() ? "no_storage" : "storage_full");
        return drop != null;
    }

    @Override
    public boolean canContinueToUse() {
        return drop != null && golem.hasCargo();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        golem.note("deliver_start", drop.pos());
        trek.to(drop.spot());
    }

    @Override
    public void stop() {
        drop = null;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (drop == null) return;
        golem.getLookControl().setLookAt(Vec3.atCenterOf(drop.pos()));
        if (trek.arrived(REACH)) {
            int moved = drop.unload(golem, false);
            if (moved > 0) {
                golem.note("delivered", moved, drop.pos());
                golem.endDelivery();
            }
            drop = null;
        } else if (trek.stuck()) {
            golem.markUnreachable(drop.pos());
            drop = null;
        }
    }

    @Nullable
    private Drop findDropOff() {
        for (BlockPos fill : golem.marks(MarkType.FILL)) {
            for (Drop option : drops(fill)) {
                if (!golem.isUnreachable(option.pos()) && option.unload(golem, true) > 0) return option;
            }
        }
        return null;
    }

    private List<Drop> drops(BlockPos fill) {
        if (!(golem.level().getBlockEntity(fill) instanceof SmokeMarkBlockEntity mark) || !mark.hasLinks()) {
            return List.of(new Drop(fill, false));
        }
        return mark.liveLinks().stream().map(store -> new Drop(store, true)).toList();
    }
}
