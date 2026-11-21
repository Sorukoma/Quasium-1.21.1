package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ShearerGoal extends ClayChoreGoal {
    ShearerGoal(ClayGolemEntity golem) {
        super(golem, GolemJob.SHEARER);
    }

    @Nullable
    @Override
    protected Chore next(List<BlockPos> zones) {
        if (!golem.hasRoom()) return null;
        for (BlockPos zone : zones) {
            Sheep sheep = nearest(around(Sheep.class, zone, Sheep::readyForShearing));
            if (sheep != null) return Chore.on(zone, sheep, () -> sheep.readyForShearing() && golem.hasRoom(), () -> shear(sheep));
        }
        golem.waitFor("no_sheep");
        return null;
    }

    private boolean shear(Sheep sheep) {
        golem.takeIn(sheep.onSheared(null, golem.tool(), golem.level(), sheep.blockPosition()), sheep.blockPosition());
        sheep.gameEvent(GameEvent.SHEAR, golem);
        GolemTools.use(golem);
        golem.note("sheared");
        return true;
    }
}
