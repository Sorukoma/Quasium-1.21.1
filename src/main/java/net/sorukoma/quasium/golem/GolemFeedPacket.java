package net.sorukoma.quasium.golem;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;

public record GolemFeedPacket(int golemId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GolemFeedPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "golem_feed"));
    public static final StreamCodec<ByteBuf, GolemFeedPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(GolemFeedPacket::new, GolemFeedPacket::golemId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(GolemFeedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!player.isSpectator() && player.level().getEntity(packet.golemId()) instanceof WorkerGolem golem
                    && player.canInteractWithEntity(golem, 1.0)) {
                golem.feed(player, InteractionHand.MAIN_HAND, true);
            }
        });
    }
}
