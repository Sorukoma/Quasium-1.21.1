package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.BookRewards;

public record ClaimBookRewardPacket(ResourceLocation article) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClaimBookRewardPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "claim_book_reward"));
    public static final StreamCodec<ByteBuf, ClaimBookRewardPacket> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(ClaimBookRewardPacket::new, ClaimBookRewardPacket::article);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ClaimBookRewardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
                BookRewards.claim(player, packet.article());
            }
        });
    }
}
