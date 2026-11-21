package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;
import net.sorukoma.quasium.item.CharcoalItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class FetchCoalGoal extends GolemErrandGoal {
    FetchCoalGoal(CalloudGolemEntity golem) {
        super(golem, 60, "fetch");
    }

    @Override
    protected boolean wanted() {
        return !golem.hasCoal();
    }

    @Nullable
    @Override
    protected BlockPos find() {
        List<BlockPos> fills = golem.marks(MarkType.FILL);
        if (fills.isEmpty()) golem.waitFor("no_coal_mark");
        for (BlockPos mark : fills) {
            for (BlockPos store : SmokeMarks.stores(golem.level(), mark)) {
                if (holdsCoal(store(store))) return store;
            }
        }
        return null;
    }

    @Override
    protected void arrive(BlockPos pos) {
        IItemHandler store = store(pos);
        if (store == null) return;
        int carried = golem.coalCount();
        for (int slot = 0; slot < store.getSlots() && golem.coalRoom() > 0; slot++) {
            if (store.getStackInSlot(slot).getItem() instanceof CharcoalItem) golem.addCoal(store.extractItem(slot, golem.coalRoom(), false));
        }
        if (golem.coalCount() <= carried) return;
        golem.note("fetched", golem.coalCount() - carried, pos);
        golem.jobDone();
    }

    private static boolean holdsCoal(@Nullable IItemHandler store) {
        if (store == null) return false;
        for (int slot = 0; slot < store.getSlots(); slot++) {
            if (store.getStackInSlot(slot).getItem() instanceof CharcoalItem) return true;
        }
        return false;
    }
}
