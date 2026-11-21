package net.sorukoma.quasium.golem;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.screen.GolemMenu;

public record GolemBindPacket(int containerId, BlockPos mark, boolean bind) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GolemBindPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "golem_bind"));
    public static final StreamCodec<ByteBuf, GolemBindPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GolemBindPacket::containerId,
            BlockPos.STREAM_CODEC, GolemBindPacket::mark,
            ByteBufCodecs.BOOL, GolemBindPacket::bind,
            GolemBindPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(GolemBindPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof GolemMenu menu) || menu.containerId != packet.containerId()
                    || menu.golem() == null || !menu.stillValid(player)) return;
            GolemBindings.set(player, menu.golem(), packet.mark(), packet.bind());
            menu.resendProfile();
        });
    }
}
