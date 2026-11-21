package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SpellForm implements StringRepresentable {
    BEAM("beam"),
    CONE("cone"),
    CLOUD("cloud"),
    PULSE("pulse"),
    RING("ring"),
    BOLT("bolt"),
    WALL("wall"),
    SELF("self"),
    SPIRIT("spirit");

    public static final Codec<SpellForm> CODEC = StringRepresentable.fromEnum(SpellForm::values);

    private final String id;

    SpellForm(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
