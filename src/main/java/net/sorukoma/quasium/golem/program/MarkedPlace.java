package net.sorukoma.quasium.golem.program;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.StreamCodec;

public record MarkedPlace(GlobalPos where, Direction face) {
    public static final Codec<MarkedPlace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("where").forGetter(MarkedPlace::where),
            Direction.CODEC.fieldOf("face").forGetter(MarkedPlace::face)).apply(instance, MarkedPlace::new));
    public static final StreamCodec<ByteBuf, MarkedPlace> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, MarkedPlace::where, Direction.STREAM_CODEC, MarkedPlace::face, MarkedPlace::new);
}
