package net.sorukoma.quasium.golem;

import net.minecraft.world.entity.ai.goal.Goal;

final class BleatGoal extends Goal {
    private static final int CHECK_TICKS = 20;
    private static final int ODDS = 900;

    private final ClayGolemEntity golem;

    BleatGoal(ClayGolemEntity golem) {
        this.golem = golem;
    }

    @Override
    public boolean canUse() {
        return golem.tickCount % CHECK_TICKS == 0 && GolemNap.sleeper(golem) && golem.getRandom().nextInt(ODDS) == 0 && GolemNap.heard(golem);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        GolemNap.say(golem, "quasium.golem.ashulbek.bleat");
    }
}
