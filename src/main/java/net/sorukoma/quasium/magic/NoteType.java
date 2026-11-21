package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

public enum NoteType implements StringRepresentable {
    MENTHOL("menthol", 0x66FFCC),
    EMBER("ember", 0xFF6B2A),
    NECTAR("nectar", 0xFFAA22),
    DEW("dew", 0x99DDFF),
    RESIN("resin", 0x8A6D3B),
    ASH("ash", 0x777777);

    public static final Codec<NoteType> CODEC = StringRepresentable.fromEnum(NoteType::values);

    private final String id;
    private final int color;

    NoteType(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    public MutableComponent displayName() {
        return Component.translatable("quasium.note." + id).withColor(color);
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    @Nullable
    public static NoteType byId(String id) {
        for (NoteType note : values()) {
            if (note.id.equals(id)) return note;
        }
        return null;
    }

    public static boolean isNote(String essenceType) {
        return byId(essenceType) != null;
    }
}
