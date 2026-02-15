package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.QuasiumMod;

public record PacketSpawnSmoke(BlockPos blockPos, Vec3 lookDirection) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "spawn_smoke");
    public static final StreamCodec<FriendlyByteBuf, PacketSpawnSmoke> STREAM_CODEC = CustomPacketPayload.codec(PacketSpawnSmoke::write, PacketSpawnSmoke::new);

    public PacketSpawnSmoke(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
        buffer.writeDouble(lookDirection.x);
        buffer.writeDouble(lookDirection.y);
        buffer.writeDouble(lookDirection.z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return new Type<>(ID);
    }
}
