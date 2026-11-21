package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientProgressionData;
import net.sorukoma.quasium.client.gui.ProgressionBookScreen;

import java.util.List;

public record ClaimedRewardsPacket(List<String> articles) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClaimedRewardsPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "claimed_rewards"));
    public static final StreamCodec<ByteBuf, ClaimedRewardsPacket> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(ClaimedRewardsPacket::new, ClaimedRewardsPacket::articles);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ClaimedRewardsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ClaimedRewardsPacket packet) {
        ClientProgressionData.setClaimed(packet.articles());
        if (Minecraft.getInstance().screen instanceof ProgressionBookScreen book) book.refreshReward();
    }
}
