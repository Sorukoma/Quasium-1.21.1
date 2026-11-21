package net.sorukoma.quasium.smokelands;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.progression.ConfigArgs;

public final class DraftCycle {
    private DraftCycle() {
    }

    public static float phase(Level level, float partialTick) {
        int length = ConfigArgs.synced(SmokelandsConfig.DRAFT_CYCLE_SECONDS) * 20;
        return ((level.getGameTime() % length) + partialTick) / length;
    }

    public static float pulse(Level level, float partialTick) {
        return 0.5f - 0.5f * Mth.cos(phase(level, partialTick) * Mth.TWO_PI);
    }
}
