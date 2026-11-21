package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;

import java.util.EnumSet;

class WispFollowGoal extends Goal {
    private static final double CLOSE = 3.0;
    private static final double TELEPORT = 16.0;
    private final DraftWisp wisp;

    WispFollowGoal(DraftWisp wisp) {
        this.wisp = wisp;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player owner = wisp.owner();
        return owner != null && !owner.isSpectator() && wisp.distanceToSqr(owner) > CLOSE * CLOSE;
    }

    @Override
    public void tick() {
        Player owner = wisp.owner();
        if (owner == null) return;
        wisp.getLookControl().setLookAt(owner, 10f, wisp.getMaxHeadXRot());
        if (wisp.distanceToSqr(owner) > TELEPORT * TELEPORT) {
            wisp.moveTo(owner.getX(), owner.getEyeY() + 0.5, owner.getZ(), wisp.getYRot(), wisp.getXRot());
            wisp.getNavigation().stop();
            return;
        }
        wisp.getNavigation().moveTo(owner.getX(), owner.getEyeY() + 0.6, owner.getZ(), SmokelandsConfig.WISP_FOLLOW_SPEED.get());
    }
}
