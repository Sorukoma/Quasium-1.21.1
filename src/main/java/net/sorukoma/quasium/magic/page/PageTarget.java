package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum PageTarget implements StringRepresentable {
    FOE("foe"),
    ALLY("ally"),
    ANY("any"),
    SUPPORT("support"),
    WORLD("world");

    public static final Codec<PageTarget> CODEC = StringRepresentable.fromEnum(PageTarget::values);

    private final String id;

    PageTarget(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
