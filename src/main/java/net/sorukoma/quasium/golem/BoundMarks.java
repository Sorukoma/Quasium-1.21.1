package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BoundMarks {
    private static final int RESCAN_TICKS = 20;

    private final Map<MarkType, Scan> scans = new EnumMap<>(MarkType.class);

    private record Scan(long at, List<BlockPos> marks) {
    }

    List<BlockPos> of(WorkerGolem golem, MarkType type) {
        Level level = golem.level();
        long now = level.getGameTime();
        Scan scan = scans.get(type);
        if (scan == null || now < scan.at() || now - scan.at() >= RESCAN_TICKS) {
            List<BlockPos> found = SmokeMarks.near(level, golem.blockPosition(), GolemBindings.markRadius(), type, golem.getUUID());
            scans.put(type, new Scan(now, found));
            return found;
        }
        return scan.marks().stream().filter(pos -> stillBound(level, pos, type, golem.getUUID())).toList();
    }

    void forget() {
        scans.clear();
    }

    private static boolean stillBound(Level level, BlockPos pos, MarkType type, UUID golem) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof SmokeMarkBlockEntity mark && mark.type() == type
                && mark.golems().contains(golem);
    }
}
