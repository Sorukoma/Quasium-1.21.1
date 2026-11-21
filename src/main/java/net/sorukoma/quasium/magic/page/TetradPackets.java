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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.SmokeCasting;

import java.util.function.BiConsumer;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class TetradPackets {
    private TetradPackets() {
    }

    public record Flip(int step) implements CustomPacketPayload {
        public static final Type<Flip> TYPE = new Type<>(id("page_flip"));
        public static final StreamCodec<ByteBuf, Flip> CODEC = ByteBufCodecs.VAR_INT.map(Flip::new, Flip::step);

        @Override
        public Type<Flip> type() {
            return TYPE;
        }
    }

    public record Tear(boolean offhand, int index) implements CustomPacketPayload {
        public static final Type<Tear> TYPE = new Type<>(id("page_tear"));
        public static final StreamCodec<ByteBuf, Tear> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Tear::offhand,
                ByteBufCodecs.VAR_INT, Tear::index,
                Tear::new);

        @Override
        public Type<Tear> type() {
            return TYPE;
        }
    }

    public record Paste(boolean offhand, int index, int slot) implements CustomPacketPayload {
        public static final Type<Paste> TYPE = new Type<>(id("page_paste"));
        public static final StreamCodec<ByteBuf, Paste> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Paste::offhand,
                ByteBufCodecs.VAR_INT, Paste::index,
                ByteBufCodecs.VAR_INT, Paste::slot,
                Paste::new);

        @Override
        public Type<Paste> type() {
            return TYPE;
        }
    }

    public record CopyOver(boolean offhand) implements CustomPacketPayload {
        public static final Type<CopyOver> TYPE = new Type<>(id("page_copy_over"));
        public static final StreamCodec<ByteBuf, CopyOver> CODEC = ByteBufCodecs.BOOL.map(CopyOver::new, CopyOver::offhand);

        @Override
        public Type<CopyOver> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(Flip.TYPE, Flip.CODEC, onServer((player, packet) -> flip(player, packet.step())));
        registrar.playToServer(Tear.TYPE, Tear.CODEC, onServer((player, packet) -> tear(player, hand(packet.offhand()), packet.index())));
        registrar.playToServer(Paste.TYPE, Paste.CODEC,
                onServer((player, packet) -> paste(player, hand(packet.offhand()), packet.index(), packet.slot())));
        registrar.playToServer(CopyOver.TYPE, CopyOver.CODEC, onServer((player, packet) -> copyOver(player, hand(packet.offhand()))));
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> onServer(BiConsumer<ServerPlayer, T> handler) {
        return (T packet, IPayloadContext context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handler.accept(player, packet);
        });
    }

    public static void sendFlip(int step) {
        PacketDistributor.sendToServer(new Flip(step));
    }

    public static void sendTear(InteractionHand hand, int index) {
        PacketDistributor.sendToServer(new Tear(hand == InteractionHand.OFF_HAND, index));
    }

    public static void sendPaste(InteractionHand hand, int index, int slot) {
        PacketDistributor.sendToServer(new Paste(hand == InteractionHand.OFF_HAND, index, slot));
    }

    public static void sendCopyOver(InteractionHand hand) {
        PacketDistributor.sendToServer(new CopyOver(hand == InteractionHand.OFF_HAND));
    }

    public static boolean flip(ServerPlayer player, int step) {
        ItemStack stack = Tetrads.find(player);
        if (stack.isEmpty() || step == 0) return false;
        TetradContents contents = SpellTetradItem.contents(stack);
        int next = contents.nextFilled(contents.active(), Integer.signum(step));
        if (contents.page(next).isEmpty()) return false;
        SpellTetradItem.store(stack, contents.withActive(next));
        player.displayClientMessage(Component.translatable("quasium.tetrad.flipped", next + 1,
                PageLabels.of(PageCasting.label(contents.page(next)))), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.4f, 1.3f);
        return true;
    }

    public static boolean tear(ServerPlayer player, InteractionHand hand, int index) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SpellTetradItem) || refusedForBreath(player)) return false;
        TetradContents contents = SpellTetradItem.contents(stack);
        SpellPage page = contents.page(index);
        if (page.isEmpty()) return false;
        ItemStack sheet = new ItemStack(PageRegistry.TORN_PAGE.get());
        sheet.set(PageRegistry.PAGE.get(), page);
        SpellTetradItem.store(stack, contents.with(index, SpellPage.EMPTY));
        if (!player.getInventory().add(sheet)) player.drop(sheet, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9f, 0.6f);
        return true;
    }

    public static boolean paste(ServerPlayer player, InteractionHand hand, int index, int slot) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SpellTetradItem) || slot < 0 || slot >= player.getInventory().getContainerSize()) return false;
        ItemStack sheet = player.getInventory().getItem(slot);
        SpellPage page = sheet.is(PageRegistry.TORN_PAGE.get()) ? sheet.getOrDefault(PageRegistry.PAGE.get(), SpellPage.EMPTY) : SpellPage.EMPTY;
        TetradContents contents = SpellTetradItem.contents(stack);
        if (page.isEmpty() || index < 0 || index >= contents.size() || !contents.page(index).isEmpty()) return false;
        if (refusedForBreath(player)) return false;
        SpellTetradItem.store(stack, contents.with(index, page));
        sheet.shrink(1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9f, 1f);
        return true;
    }

    public static int copyOver(ServerPlayer player, InteractionHand hand) {
        ItemStack large = player.getItemInHand(hand);
        if (!(large.getItem() instanceof SpellTetradItem tetrad) || tetrad.pages() <= PageRegistry.SMALL_PAGES) return 0;
        ItemStack small = smallTetrad(player);
        if (small.isEmpty()) return 0;
        TetradContents target = SpellTetradItem.contents(large);
        int copied = 0;
        for (SpellPage page : SpellTetradItem.contents(small).pages()) {
            int free = firstFree(target);
            if (free < 0) break;
            if (page.isEmpty() || target.pages().contains(page)) continue;
            target = target.with(free, page);
            copied++;
        }
        if (copied == 0) return 0;
        SpellTetradItem.store(large, target);
        player.displayClientMessage(Component.translatable("quasium.tetrad.copied", copied), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9f, 1.1f);
        return copied;
    }

    public static ItemStack smallTetrad(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SpellTetradItem tetrad && tetrad.pages() == PageRegistry.SMALL_PAGES) return stack;
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() instanceof SpellTetradItem tetrad && tetrad.pages() == PageRegistry.SMALL_PAGES ? offhand : ItemStack.EMPTY;
    }

    public static int firstFree(TetradContents contents) {
        for (int i = 0; i < contents.size(); i++) {
            if (contents.page(i).isEmpty()) return i;
        }
        return -1;
    }

    private static boolean refusedForBreath(ServerPlayer player) {
        if (!SmokeCasting.holdsSmoke(player)) return false;
        player.displayClientMessage(Component.translatable("quasium.page.refused.breath"), true);
        return true;
    }

    private static InteractionHand hand(boolean offhand) {
        return offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
