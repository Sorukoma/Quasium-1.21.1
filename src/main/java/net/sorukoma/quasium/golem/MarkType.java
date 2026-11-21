package net.sorukoma.quasium.golem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum MarkType implements StringRepresentable {
    HARVEST("harvest", 0x9BD14B),
    FILL("fill", 0x5FB6E8),
    GUARD("guard", 0xD9534F),
    HEAT("heat", 0xFF9A3C),
    ZONE("zone", 0xB070E0),
    FEED("feed", 0xF2C94C),
    PROGRAM("program", 0x6FC3B2);

    public static final Codec<MarkType> CODEC = StringRepresentable.fromEnum(MarkType::values);
    public static final StreamCodec<ByteBuf, MarkType> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(MarkType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), MarkType::ordinal);

    private final String id;
    private final int color;

    MarkType(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
