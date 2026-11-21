package net.sorukoma.quasium.golem;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record GolemPipe(BlockPos hookah, long from, long to) {
    static final StreamCodec<ByteBuf, GolemPipe> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GolemPipe::hookah,
            ByteBufCodecs.VAR_LONG, GolemPipe::from,
            ByteBufCodecs.VAR_LONG, GolemPipe::to,
            GolemPipe::new);
    private static final float RAISE_TICKS = 6f;

    public float raise(long now, float partialTick) {
        return ramp(now - from, partialTick) - ramp(now - to, partialTick);
    }

    private static float ramp(long since, float partialTick) {
        return Mth.clamp((since + partialTick) / RAISE_TICKS, 0f, 1f);
    }
}
