package net.sorukoma.quasium.cart;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.CartClient;

public record CartHosePacket(int cartId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CartHosePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "cart_hose"));
    public static final StreamCodec<ByteBuf, CartHosePacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CartHosePacket::new, CartHosePacket::cartId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(CartHosePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> handleClient(packet));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(CartHosePacket packet) {
        CartClient.setConnectedCart(packet.cartId());
    }
}
