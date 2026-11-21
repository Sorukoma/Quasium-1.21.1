package net.sorukoma.quasium.golem.program;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum Check implements StringRepresentable {
    LOAD_FULL("load_full", false, false),
    LOAD_EMPTY("load_empty", false, false),
    HAS_ITEM("has_item", true, true),
    NO_ROOM("no_room", true, true),
    HUNGRY("hungry", false, false),
    DAY("day", false, false),
    NIGHT("night", false, false),
    BLOCK_IS("block_is", true, true),
    EMPTY_AT("empty_at", true, false),
    SIGNAL("signal", true, false);

    public static final StringRepresentable.EnumCodec<Check> CODEC = StringRepresentable.fromEnum(Check::values);
    public static final StreamCodec<ByteBuf, Check> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(Check::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Check::ordinal);

    private final String id;
    private final boolean usesA;
    private final boolean usesFilter;

    Check(String id, boolean usesA, boolean usesFilter) {
        this.id = id;
        this.usesA = usesA;
        this.usesFilter = usesFilter;
    }

    public boolean usesA() {
        return usesA;
    }

    public boolean usesFilter() {
        return usesFilter;
    }

    public BlockGroup group() {
        return this == SIGNAL ? BlockGroup.REDSTONE : BlockGroup.ACTIONS;
    }

    public String key() {
        return "quasium.program.check." + id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
