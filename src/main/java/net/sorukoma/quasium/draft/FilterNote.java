package net.sorukoma.quasium.draft;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

public enum FilterNote implements StringRepresentable {
    NONE(null),
    MENTHOL(NoteType.MENTHOL),
    EMBER(NoteType.EMBER),
    NECTAR(NoteType.NECTAR),
    DEW(NoteType.DEW),
    RESIN(NoteType.RESIN),
    ASH(NoteType.ASH);

    @Nullable
    private final NoteType note;

    FilterNote(@Nullable NoteType note) {
        this.note = note;
    }

    @Nullable
    public NoteType note() {
        return note;
    }

    public FilterNote next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component describe() {
        return note == null
                ? Component.translatable("quasium.draft.filter.none")
                : Component.translatable("quasium.draft.filter.set", note.displayName());
    }

    public static FilterNote of(NoteType note) {
        for (FilterNote filter : values()) {
            if (filter.note == note) return filter;
        }
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return note == null ? "none" : note.id();
    }
}
