package net.sorukoma.quasium.draft;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum PipeTier implements StringRepresentable {
    CLAY("clay", 32),
    COPPER("copper", 128),
    GLASS("glass", 256),
    QUASIUM("quasium", 512);

    public static final Codec<PipeTier> CODEC = StringRepresentable.fromEnum(PipeTier::values);
    public static final int DRAFT_PER_NOTE_ML = 8;

    private final String id;
    private final int draftPerSecond;

    PipeTier(String id, int draftPerSecond) {
        this.id = id;
        this.draftPerSecond = draftPerSecond;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public int draftPerSecond() {
        return draftPerSecond;
    }

    public int notesPerSecond() {
        return draftPerSecond / DRAFT_PER_NOTE_ML;
    }

    public static PipeTier weaker(PipeTier a, PipeTier b) {
        return a.draftPerSecond <= b.draftPerSecond ? a : b;
    }
}
