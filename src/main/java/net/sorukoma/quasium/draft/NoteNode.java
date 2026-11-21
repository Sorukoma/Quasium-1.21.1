package net.sorukoma.quasium.draft;

import net.sorukoma.quasium.magic.NoteType;

import java.util.Map;

public interface NoteNode {
    int PRIORITY_LAST = 0;
    int PRIORITY_NORMAL = 1;

    default int acceptNote(NoteType note, int ml, boolean simulate) {
        return 0;
    }

    default Map<NoteType, Integer> noteSupply() {
        return Map.of();
    }

    default int takeNote(NoteType note, int ml) {
        return 0;
    }

    default int notePriority() {
        return PRIORITY_NORMAL;
    }

    default boolean isStorage() {
        return false;
    }
}
