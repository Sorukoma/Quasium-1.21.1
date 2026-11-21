package net.sorukoma.quasium.magic.page;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.magic.SmokeCasting;

import java.util.Optional;

public final class PagePackets {
    private PagePackets() {
    }

    public record Edit(boolean offhand, int index, SpellPage page) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(id("page_edit"));
        public static final StreamCodec<ByteBuf, Edit> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Edit::offhand,
                ByteBufCodecs.VAR_INT, Edit::index,
                SpellPage.STREAM_CODEC, Edit::page,
                Edit::new);

        @Override
        public Type<Edit> type() {
            return TYPE;
        }
    }

    public record Select(int index) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(id("page_select"));
        public static final StreamCodec<ByteBuf, Select> CODEC = ByteBufCodecs.VAR_INT.map(Select::new, Select::index);

        @Override
        public Type<Select> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(Edit.TYPE, Edit.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) edit(player, packet);
        }));
        registrar.playToServer(Select.TYPE, Select.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) select(player, packet.index());
        }));
    }

    public static void sendEdit(InteractionHand hand, int index, SpellPage page) {
        PacketDistributor.sendToServer(new Edit(hand == InteractionHand.OFF_HAND, index, page));
    }

    public static void sendSelect(int index) {
        PacketDistributor.sendToServer(new Select(index));
    }

    public static boolean edit(ServerPlayer player, Edit packet) {
        ItemStack stack = player.getItemInHand(packet.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof SpellTetradItem)) return false;
        TetradContents contents = SpellTetradItem.contents(stack);
        if (packet.index() < 0 || packet.index() >= contents.size()) return false;
        Optional<Component> refusal = SmokeCasting.holdsSmoke(player)
                ? Optional.of(Component.translatable("quasium.page.refused.breath"))
                : PageRules.check(player, packet.page(), contents.page(packet.index()));
        if (refusal.isPresent()) {
            player.displayClientMessage(refusal.get(), true);
            return false;
        }
        SpellTetradItem.store(stack, contents.with(packet.index(), packet.page()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1f);
        SmokecraftTrigger.fire(player, "page_written");
        return true;
    }

    public static boolean select(ServerPlayer player, int index) {
        ItemStack stack = Tetrads.find(player);
        if (stack.isEmpty()) return false;
        TetradContents contents = SpellTetradItem.contents(stack);
        if (index < 0 || index >= contents.size()) return false;
        SpellTetradItem.store(stack, contents.withActive(index));
        return true;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
