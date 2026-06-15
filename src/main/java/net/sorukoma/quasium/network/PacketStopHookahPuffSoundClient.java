package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import org.jetbrains.annotations.Nullable;

public record PacketStopHookahPuffSoundClient(@Nullable BlockPos hookahPos, boolean isBackpack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketStopHookahPuffSoundClient> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "stop_hookah_puff_sound_client"));

    public static final StreamCodec<FriendlyByteBuf, PacketStopHookahPuffSoundClient> STREAM_CODEC =
            CustomPacketPayload.codec(PacketStopHookahPuffSoundClient::write, PacketStopHookahPuffSoundClient::read);

    private static PacketStopHookahPuffSoundClient read(FriendlyByteBuf buffer) {
        boolean isBackpack = buffer.readBoolean();
        BlockPos pos = isBackpack ? null : buffer.readBlockPos();
        return new PacketStopHookahPuffSoundClient(pos, isBackpack);
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isBackpack);
        if (!isBackpack && hookahPos != null) {
            buffer.writeBlockPos(hookahPos);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(PacketStopHookahPuffSoundClient payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.isBackpack()) {
                // Stopping puff sound for backpack hookah — the pipe stays connected,
                // only the puff action stops. Don't disconnect the pipe here.
            } else if (payload.hookahPos() != null) {
                if (Minecraft.getInstance().level != null) {
                    if (Minecraft.getInstance().level.getBlockEntity(payload.hookahPos()) instanceof HookahBlockEntity hookahBlockEntity) {
                        hookahBlockEntity.stopClientPuffSound();
                    }
                }
            }
        });
    }
}