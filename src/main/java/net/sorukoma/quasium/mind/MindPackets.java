package net.sorukoma.quasium.mind;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.client.MindCues;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class MindPackets {
    private MindPackets() {
    }

    public record Sync(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("mind_sync"));
        static final StreamCodec<ByteBuf, Sync> CODEC = ByteBufCodecs.COMPOUND_TAG.map(Sync::new, Sync::data);

        @Override
        public Type<Sync> type() {
            return TYPE;
        }
    }

    public record Action(String action, String argument) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id("mind_action"));
        static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Action::action, ByteBufCodecs.stringUtf8(256), Action::argument, Action::new);

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    public record Cue(String cue, int value) implements CustomPacketPayload {
        public static final Type<Cue> TYPE = new Type<>(id("mind_cue"));
        static final StreamCodec<ByteBuf, Cue> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Cue::cue, ByteBufCodecs.VAR_INT, Cue::value, Cue::new);

        @Override
        public Type<Cue> type() {
            return TYPE;
        }
    }

    public static void cue(ServerPlayer player, String cue, int value) {
        PacketDistributor.sendToPlayer(player, new Cue(cue, value));
    }

    public static void act(String action, String argument) {
        PacketDistributor.sendToServer(new Action(action, argument));
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(Sync.TYPE, Sync.CODEC, (packet, context) -> context.enqueueWork(() -> ClientMind.set(packet.data())));
        registrar.playToClient(Cue.TYPE, Cue.CODEC, MindPackets::onCue);
        registrar.playToServer(Action.TYPE, Action.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) MindActions.handle(player, packet.action(), packet.argument());
        }));
    }

    private static void onCue(Cue packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) MindCues.handle(packet.cue(), packet.value());
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
