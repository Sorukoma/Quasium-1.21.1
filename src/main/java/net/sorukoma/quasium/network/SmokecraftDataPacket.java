package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.DerivedNotes;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteProfiles;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.magic.page.PageCatalog;

import java.util.Map;

public record SmokecraftDataPacket(Map<ResourceLocation, NoteProfile> profiles,
                                   Map<ResourceLocation, NoteProfile> derived,
                                   Map<ResourceLocation, Spell> spells,
                                   PageCatalog pages) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SmokecraftDataPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft_data"));
    public static final StreamCodec<ByteBuf, SmokecraftDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(NoteProfiles.MAP_CODEC), SmokecraftDataPacket::profiles,
            DerivedNotes.STREAM_CODEC, SmokecraftDataPacket::derived,
            ByteBufCodecs.fromCodec(Spells.MAP_CODEC), SmokecraftDataPacket::spells,
            ByteBufCodecs.fromCodec(PageCatalog.CODEC), SmokecraftDataPacket::pages,
            SmokecraftDataPacket::new);

    public static SmokecraftDataPacket current() {
        return new SmokecraftDataPacket(NoteProfiles.all(), DerivedNotes.all(), Spells.all(), PageCatalog.get());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(SmokecraftDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            NoteProfiles.set(packet.profiles);
            DerivedNotes.set(packet.derived);
            Spells.set(packet.spells);
            PageCatalog.set(packet.pages);
        });
    }
}
