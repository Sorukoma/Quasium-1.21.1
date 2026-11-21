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

import java.util.List;

public record GolemProfile(int containerId, List<Spot> bound, List<Spot> nearby, List<GolemJournal.Entry> journal)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GolemProfile> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "golem_profile"));
    public static final GolemProfile EMPTY = new GolemProfile(-1, List.of(), List.of(), List.of());

    public record Spot(BlockPos pos, MarkType type) {
        private static final StreamCodec<ByteBuf, Spot> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Spot::pos, MarkType.STREAM_CODEC, Spot::type, Spot::new);
    }

    public static final StreamCodec<ByteBuf, GolemProfile> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GolemProfile::containerId,
            Spot.STREAM_CODEC.apply(ByteBufCodecs.list()), GolemProfile::bound,
            Spot.STREAM_CODEC.apply(ByteBufCodecs.list()), GolemProfile::nearby,
            GolemJournal.Entry.STREAM_CODEC.apply(ByteBufCodecs.list(GolemJournal.SIZE)), GolemProfile::journal,
            GolemProfile::new);

    public static GolemProfile of(int containerId, WorkerGolem golem, Player viewer) {
        return new GolemProfile(containerId, GolemBindings.bound(golem), GolemBindings.bindable(golem, viewer), golem.journal().entries());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(GolemProfile profile, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof GolemMenu menu && menu.containerId == profile.containerId()) menu.receive(profile);
        });
    }
}
