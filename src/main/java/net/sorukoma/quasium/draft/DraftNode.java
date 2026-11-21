package net.sorukoma.quasium.draft;

import net.minecraft.core.Direction;

public interface DraftNode {
    default int draftOutput(Direction side) {
        return 0;
    }

    default int draftDemand(Direction side) {
        return 0;
    }

    default void draftSupplied(Direction side, float efficiency) {
    }
}
