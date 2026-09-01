package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.QuasiumMod;

/**
 * Спавн дыма от головы курящего игрока.
 * Пакет несёт ID сущности курящего (smokerEntityId), чтобы КАЖДЫЙ клиент,
 * получивший пакет, нашёл именно этого игрока и заспавнил дым у его головы,
 * а не у локального игрока. Направление взгляда передаётся с сервера, т.к.
 * на других клиентах оно может быть не синхронизировано мгновенно.
 *
 * particleColor — RGB int, цвет DUST-оверлея по типу табака (см. SmokeColorUtil).
 * particleCount — плотность дыма (число частиц за спавн); растёт с количеством мелассы.
 */
public record PacketSpawnSmoke(int smokerEntityId, Vec3 lookDirection, int particleColor, int particleCount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSpawnSmoke> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "spawn_smoke"));
    public static final StreamCodec<FriendlyByteBuf, PacketSpawnSmoke> STREAM_CODEC = CustomPacketPayload.codec(PacketSpawnSmoke::write, PacketSpawnSmoke::new);

    public PacketSpawnSmoke(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readInt(), buffer.readVarInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(smokerEntityId);
        buffer.writeDouble(lookDirection.x);
        buffer.writeDouble(lookDirection.y);
        buffer.writeDouble(lookDirection.z);
        buffer.writeInt(particleColor);
        buffer.writeVarInt(particleCount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
