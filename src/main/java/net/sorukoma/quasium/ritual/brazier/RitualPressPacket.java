package net.sorukoma.quasium.ritual.brazier;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;

public record RitualPressPacket(BlockPos brazier, double gameTime) implements CustomPacketPayload {
    public static final Type<RitualPressPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual_press"));
    public static final StreamCodec<ByteBuf, RitualPressPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RitualPressPacket::brazier, ByteBufCodecs.DOUBLE, RitualPressPacket::gameTime, RitualPressPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RitualPressPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.level().isLoaded(packet.brazier())) return;
            if (player.level().getBlockEntity(packet.brazier()) instanceof RitualBrazierBlockEntity brazier && brazier.beatsWith(player)) {
                brazier.pressAt(player, packet.gameTime());
            }
        });
    }
}
