package net.sorukoma.quasium.golem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;

public final class GolemMood {
    public static final int BEST = 1;
    public static final int WORST = 4;
    public static final int STREAK = 4;
    public static final float FULL = 100f;
    public static final float AUTO_EAT_BELOW = 25f;
    public static final float WASTE_LIMIT = 0.8f;
    private static final int STATE_STEP_PCT = 20;
    private static final int EXHAUSTED_TOP_PCT = 40;
    private static final float GOLDEN_OVERFLOW = 20f;
    private static final long TICKS_PER_MINUTE = 1200;

    public enum Food {
        APPLE, FRIES, GOLDEN, ENCHANTED;

        @Nullable
        public static Food of(ItemStack stack) {
            if (stack.is(Items.APPLE)) return APPLE;
            if (stack.is(ModItems.FRIES.get())) return FRIES;
            if (stack.is(Items.GOLDEN_APPLE)) return GOLDEN;
            if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return ENCHANTED;
            return null;
        }
    }

    private int state = BEST;
    private float meter = FULL;
    private int misses;
    private int streak;
    private boolean starving;
    private long hungryTicks;
    private long clock;
    private long creditedQuarter = -1;

    public static long periodTicks() {
        return Config.GOLEM_HUNGER_MINUTES.getAsInt() * TICKS_PER_MINUTE;
    }

    private static long quarterTicks() {
        return Math.max(1, periodTicks() / 4);
    }

    public int state() {
        return state;
    }

    public float meter() {
        return meter;
    }

    public int misses() {
        return misses;
    }

    public int streak() {
        return streak;
    }

    public int productivity() {
        if (state == WORST) return Math.max(1, Math.round(EXHAUSTED_TOP_PCT * meter / FULL));
        int ceiling = 100 - STATE_STEP_PCT * (state - BEST);
        if (meter >= AUTO_EAT_BELOW) return ceiling;
        return Math.max(1, Math.round(ceiling * (0.5f + 0.5f * meter / AUTO_EAT_BELOW)));
    }

    public boolean canWork() {
        return meter > 0f;
    }

    public boolean hungry() {
        return meter < AUTO_EAT_BELOW;
    }

    public void tick(long ticks, boolean paused) {
        clock += ticks;
        if (paused || ticks <= 0) return;
        long period = periodTicks();
        float drain = FULL * ticks / period;
        if (starving) {
            meter = Math.max(0f, meter - drain);
            hunger(ticks);
        } else if (meter > drain) {
            meter -= drain;
        } else {
            long late = Math.max(0, ticks - (long) (meter / FULL * period));
            runOut();
            hunger(late);
        }
    }

    public void spend(float pct) {
        if (pct <= 0f || starving) return;
        meter -= pct;
        if (meter <= 0f) runOut();
    }

    public void pet(float pct) {
        meter = Math.min(FULL, meter + pct);
    }

    public void set(int newState, float newMeter) {
        state = Mth.clamp(newState, BEST, WORST);
        meter = Mth.clamp(newMeter, 0f, FULL);
        misses = 0;
        streak = 0;
        starving = false;
        hungryTicks = 0;
    }

    public float wasted(Food food) {
        float room = FULL - meter;
        return switch (food) {
            case APPLE, FRIES -> {
                float value = Config.GOLEM_APPLE_PCT.getAsInt();
                yield 1f - Math.min(value, room) / value;
            }
            case GOLDEN -> state > BEST ? 0f : GOLDEN_OVERFLOW / (room + GOLDEN_OVERFLOW);
            case ENCHANTED -> {
                float value = FULL + GOLDEN_OVERFLOW * (WORST - BEST);
                yield 1f - (room + GOLDEN_OVERFLOW * (state - BEST)) / value;
            }
        };
    }

    public boolean overfeeds(Food food) {
        return wasted(food) > WASTE_LIMIT;
    }

    public boolean wantsFromFeeder(Food food) {
        return meter < AUTO_EAT_BELOW && (food == Food.GOLDEN || food == Food.ENCHANTED || wasted(food) == 0f);
    }

    public boolean wantsMeal(Food food) {
        return switch (food) {
            case APPLE, FRIES -> wasted(food) == 0f;
            case GOLDEN -> state > BEST;
            case ENCHANTED -> state > BEST + 1;
        };
    }

    public void feed(Food food) {
        boolean onTime = !starving;
        switch (food) {
            case APPLE, FRIES -> meter = Math.min(FULL, meter + Config.GOLEM_APPLE_PCT.getAsInt());
            case GOLDEN -> {
                state = Math.max(BEST, state - 1);
                meter = FULL;
            }
            case ENCHANTED -> {
                state = BEST;
                meter = FULL;
            }
        }
        starving = false;
        hungryTicks = 0;
        if (onTime) credit();
    }

    private void credit() {
        misses = 0;
        long quarter = clock / quarterTicks();
        if (quarter == creditedQuarter) return;
        creditedQuarter = quarter;
        if (++streak < STREAK) return;
        if (state == BEST) {
            streak = STREAK;
        } else {
            state--;
            streak = 0;
        }
    }

    private void runOut() {
        meter = 0f;
        starving = true;
        hungryTicks = 0;
        miss();
    }

    private void hunger(long ticks) {
        long quarter = quarterTicks();
        for (hungryTicks += ticks; hungryTicks >= quarter; hungryTicks -= quarter) miss();
    }

    private void miss() {
        streak = 0;
        if (++misses < STREAK) return;
        if (state == WORST) {
            misses = STREAK;
        } else {
            state++;
            misses = 0;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("State", state);
        tag.putFloat("Meter", meter);
        tag.putInt("Misses", misses);
        tag.putInt("Streak", streak);
        tag.putBoolean("Starving", starving);
        tag.putLong("Hungry", hungryTicks);
        tag.putLong("Clock", clock);
        tag.putLong("Credited", creditedQuarter);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.isEmpty()) return;
        state = Mth.clamp(tag.getInt("State"), BEST, WORST);
        meter = Mth.clamp(tag.getFloat("Meter"), 0f, FULL);
        misses = Mth.clamp(tag.getInt("Misses"), 0, STREAK);
        streak = Mth.clamp(tag.getInt("Streak"), 0, STREAK);
        starving = tag.getBoolean("Starving");
        hungryTicks = Math.max(0, tag.getLong("Hungry"));
        clock = Math.max(0, tag.getLong("Clock"));
        creditedQuarter = tag.getLong("Credited");
    }
}
