package net.sorukoma.quasium.screen;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.program.ProgramEdit;

public record ProgramEditPacket(int containerId, ProgramEdit edit) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ProgramEditPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "program_edit"));
    public static final StreamCodec<ByteBuf, ProgramEditPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ProgramEditPacket::containerId, ProgramEdit.STREAM_CODEC, ProgramEditPacket::edit, ProgramEditPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ProgramEditPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> packet.apply(context.player()));
    }

    public boolean apply(Player player) {
        return !player.isSpectator() && player.containerMenu instanceof ProgramTableMenu menu && menu.containerId == containerId
                && menu.stillValid(player) && menu.edit(player, edit);
    }
}
