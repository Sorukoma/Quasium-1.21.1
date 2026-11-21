package net.sorukoma.quasium.cart;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;

public record CartPipePacket(int cartId, boolean pulling) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CartPipePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "cart_pipe"));
    public static final StreamCodec<ByteBuf, CartPipePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CartPipePacket::cartId,
            ByteBufCodecs.BOOL, CartPipePacket::pulling,
            CartPipePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(CartPipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level().getEntity(packet.cartId()) instanceof HookahMinecart cart) || !cart.isHeldBy(player)) {
                PacketDistributor.sendToPlayer(player, new CartHosePacket(-1));
            } else if (packet.pulling()) {
                cart.startPuffing(player);
            } else {
                cart.stopPuffing(player);
            }
        });
    }
}
