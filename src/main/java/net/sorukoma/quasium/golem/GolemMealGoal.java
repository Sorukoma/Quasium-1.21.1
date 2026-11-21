package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

final class GolemMealGoal extends Goal {
    private static final float FULL_ENOUGH = 75f;
    private static final long DAY = 24000L;
    private static final long SUNSET = 12000L;
    private static final double EAT_REACH = 2.0;
    private static final double QUEUE_REACH = 3.0;
    private static final int BITE_TICKS = 20;
    private static final int SEARCH_TICKS = 100;
    private static final int RETRY_TICKS = 1200;
    private static final int GIVE_UP_TICKS = 2400;

    private final WorkerGolem golem;
    private final ClayTrek trek;
    @Nullable
    private BlockPos feeder;
    private long servedDay = -1;
    private long nextSearch;
    private int ticks;
    private int bites;
    private int nextBite;
    private boolean done;

    GolemMealGoal(WorkerGolem golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Level level = golem.level();
        if (level.getGameTime() < nextSearch || !wantsMeal(level)) return false;
        nextSearch = level.getGameTime() + SEARCH_TICKS;
        List<BlockPos> feeders = golem.marks(MarkType.FEED);
        feeder = feeders.isEmpty() ? null : feeders.get(0);
        return feeder != null;
    }

    private boolean wantsMeal(Level level) {
        long day = level.getDayTime() / DAY;
        boolean evening = level.getDayTime() % DAY >= SUNSET && day != servedDay;
        if (evening) servedDay = day;
        GolemMood mood = golem.mood();
        GolemMood.Food own = GolemMood.Food.of(golem.feeder().getStackInSlot(0));
        boolean hungry = mood.hungry() && (own == null || !mood.wantsFromFeeder(own));
        return (evening || hungry) && mood.meter() < FULL_ENOUGH;
    }

    @Override
    public boolean canContinueToUse() {
        return !done && ticks < GIVE_UP_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        bites = 0;
        nextBite = 0;
        done = false;
        golem.note("meal_start", feeder);
        trek.to(Vec3.atCenterOf(feeder));
    }

    @Override
    public void stop() {
        GolemFeederBlockEntity trough = trough();
        if (trough != null) trough.leave(golem.getUUID());
        if (!done && ticks >= GIVE_UP_TICKS) golem.note("unreachable", feeder);
        feeder = null;
        nextSearch = golem.level().getGameTime() + RETRY_TICKS;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        GolemFeederBlockEntity trough = trough();
        if (trough == null) {
            done = true;
            return;
        }
        ticks++;
        golem.getLookControl().setLookAt(Vec3.atCenterOf(feeder));
        if (trek.arrived(QUEUE_REACH) && !trough.seat(golem.getUUID(), golem.level().getGameTime())) {
            golem.getNavigation().stop();
            golem.waitFor("meal_queue");
        } else if (!trek.arrived(EAT_REACH)) {
            if (trek.stuck()) ticks = GIVE_UP_TICKS;
        } else {
            golem.getNavigation().stop();
            if (--nextBite <= 0) bite(trough);
        }
    }

    @Nullable
    private GolemFeederBlockEntity trough() {
        Level level = golem.level();
        return level.isLoaded(feeder) && level.getBlockEntity(feeder) instanceof GolemFeederBlockEntity trough ? trough : null;
    }

    private void bite(GolemFeederBlockEntity trough) {
        nextBite = BITE_TICKS;
        GolemMood mood = golem.mood();
        int slot = pick(trough.food(), mood);
        if (mood.meter() < FULL_ENOUGH && slot >= 0) {
            ItemStack meal = trough.food().extractItem(slot, 1, false);
            int before = mood.state();
            golem.eat(GolemMood.Food.of(meal), meal);
            golem.note("meal_ate", meal.getDescriptionId());
            golem.afterMood(before);
            bites++;
            return;
        }
        done = true;
        if (bites > 0 || mood.meter() >= FULL_ENOUGH) golem.note("meal_done", Math.round(mood.meter()));
        else golem.note(trough.isEmpty() ? "feeder_empty" : "meal_nothing", feeder);
    }

    private static int pick(IItemHandler food, GolemMood mood) {
        GolemMood.Food[] kinds = GolemMood.Food.values();
        for (int kind = kinds.length - 1; kind >= 0; kind--) {
            if (!mood.wantsMeal(kinds[kind])) continue;
            for (int slot = 0; slot < food.getSlots(); slot++) {
                if (GolemMood.Food.of(food.getStackInSlot(slot)) == kinds[kind]) return slot;
            }
        }
        return -1;
    }
}
