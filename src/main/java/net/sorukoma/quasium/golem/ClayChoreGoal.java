package net.sorukoma.quasium.golem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

abstract class ClayChoreGoal extends Goal {
    private static final double BLOCK_REACH = 2.8;
    private static final double ENTITY_REACH = 2.0;
    private static final double FOLLOW = 1.5;
    private static final int RESCAN_TICKS = 40;
    private static final int SWING_TICKS = 12;

    protected final ClayGolemEntity golem;
    private final GolemJob job;
    private final ClayTrek trek;
    @Nullable
    private Chore chore;
    private Vec3 aimed = Vec3.ZERO;
    private int nextScan;
    private int pause;
    private int done;

    record Chore(BlockPos zone, BlockPos pos, @Nullable Entity entity, BooleanSupplier valid, BooleanSupplier act) {
        static Chore at(BlockPos zone, BlockPos pos, BooleanSupplier valid, BooleanSupplier act) {
            return new Chore(zone, pos.immutable(), null, valid, act);
        }

        static Chore on(BlockPos zone, Entity entity, BooleanSupplier valid, BooleanSupplier act) {
            return new Chore(zone, entity.blockPosition(), entity, () -> entity.isAlive() && valid.getAsBoolean(), act);
        }

        Vec3 spot() {
            return entity != null ? entity.position() : Vec3.atCenterOf(pos);
        }

        double reach() {
            return entity != null ? ENTITY_REACH : BLOCK_REACH;
        }
    }

    ClayChoreGoal(ClayGolemEntity golem, GolemJob job) {
        this.golem = golem;
        this.job = job;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Nullable
    protected abstract Chore next(List<BlockPos> zones);

    protected void noZone() {
        golem.waitFor("no_zone");
    }

    static Predicate<ItemStack> kept(ClayGolemEntity golem) {
        return switch (golem.job()) {
            case TOBACCO -> CuringGoal.kept(golem);
            case HERDER -> HerderGoal.kept(golem);
            default -> stack -> false;
        };
    }

    protected <T extends Entity> List<T> around(Class<T> type, BlockPos zone, Predicate<T> wanted) {
        return golem.level().getEntitiesOfClass(type, ZoneBox.area(golem.level(), zone),
                entity -> wanted.test(entity) && !golem.isUnreachable(entity.blockPosition()));
    }

    @Nullable
    protected <T extends Entity> T nearest(List<T> entities) {
        return entities.stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    }

    private boolean onDuty() {
        return golem.job() == job && !golem.savesTool() && golem.mayWork();
    }

    @Override
    public boolean canUse() {
        if (GolemPrograms.running(golem) || GolemSmoke.withOwner(golem)) return false;
        if (golem.tickCount < nextScan || !onDuty()) return false;
        nextScan = golem.tickCount + golem.paced(RESCAN_TICKS);
        List<BlockPos> zones = golem.marks(MarkType.ZONE);
        if (zones.isEmpty()) {
            noZone();
            return false;
        }
        chore = next(zones);
        return chore != null;
    }

    @Override
    public boolean canContinueToUse() {
        return chore != null && golem.job() == job && !golem.savesTool();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        done = 0;
        pause = 0;
        golem.note("job_start", chore.zone(), job.key());
        walk();
    }

    @Override
    public void stop() {
        chore = null;
        golem.getNavigation().stop();
        if (done > 0) golem.endHarvestTrip();
    }

    @Override
    public void tick() {
        if (chore == null) return;
        golem.getLookControl().setLookAt(chore.spot());
        if (pause > 0) {
            pause--;
        } else if (!chore.valid().getAsBoolean()) {
            advance();
        } else if (golem.distanceToSqr(chore.spot()) <= chore.reach() * chore.reach()) {
            golem.swing(InteractionHand.MAIN_HAND);
            pause = golem.paced(SWING_TICKS);
            done++;
            if (chore.act().getAsBoolean()) advance();
        } else if (trek.stuck()) {
            golem.markUnreachable(chore.pos());
            advance();
        } else if (chore.spot().distanceToSqr(aimed) > FOLLOW * FOLLOW) {
            walk();
        }
    }

    private void advance() {
        chore = onDuty() ? next(golem.marks(MarkType.ZONE)) : null;
        if (chore != null) walk();
    }

    private void walk() {
        aimed = chore.spot();
        trek.to(aimed);
    }
}
