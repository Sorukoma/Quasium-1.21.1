package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.sorukoma.quasium.golem.program.ProgramBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProgramAreas {
    private static final int KEPT_ORDERS = 4;

    private final Map<Box, List<BlockPos>> orders = new HashMap<>();

    private record Box(BlockPos from, BlockPos to, BlockPos origin) {
    }

    List<BlockPos> order(ProgramBlock area, BlockPos origin) {
        if (area.a().isEmpty() || area.b().isEmpty()) return List.of();
        Box box = new Box(area.a().get().where().pos(), area.b().get().where().pos(), origin);
        List<BlockPos> known = orders.get(box);
        if (known != null) return known;
        if (orders.size() >= KEPT_ORDERS) orders.clear();
        List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos pos : ZoneBox.blocks(BoundingBox.fromCorners(box.from(), box.to()))) blocks.add(pos.immutable());
        blocks.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        orders.put(box, blocks);
        return blocks;
    }
}
