package net.sorukoma.quasium.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.ModItemTags;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class BookGift {
    public static final ResourceLocation ADVANCEMENT = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/book_gift");
    public static final String CONTACT = "mod_contact";
    private static final ResourceLocation ROOT = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "root");

    private BookGift() {
    }

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        if (fromMod(event.getLevel().getBlockState(event.getPos()))) touch(event.getEntity());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (fromMod(event.getState())) touch(event.getPlayer());
    }

    @SubscribeEvent
    public static void onEntityClick(PlayerInteractEvent.EntityInteract event) {
        if (fromMod(event.getTarget())) touch(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityHit(AttackEntityEvent event) {
        if (fromMod(event.getTarget())) touch(event.getEntity());
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity() instanceof Player player && (fromMod(source.getEntity()) || fromMod(source.getDirectEntity()))) touch(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) giveVeteran(player);
    }

    public static void giveVeteran(ServerPlayer player) {
        AdvancementHolder gift = ungiven(player);
        if (gift == null) return;
        if (!earnedAnyModAdvancement(player, gift) && !player.getInventory().hasAnyMatching(stack -> stack.is(ModItemTags.FIRST_CONTACT))) return;
        PlayerAdvancements progress = player.getAdvancements();
        for (String criterion : progress.getOrStartProgress(gift).getRemainingCriteria()) progress.award(gift, criterion);
    }

    @Nullable
    private static AdvancementHolder ungiven(ServerPlayer player) {
        AdvancementHolder gift = player.server.getAdvancements().get(ADVANCEMENT);
        return gift == null || player.getAdvancements().getOrStartProgress(gift).isDone() ? null : gift;
    }

    private static boolean earnedAnyModAdvancement(ServerPlayer player, AdvancementHolder gift) {
        return player.server.getAdvancements().getAllAdvancements().stream()
                .filter(holder -> holder != gift && earnedByPlaying(holder.id()))
                .anyMatch(holder -> player.getAdvancements().getOrStartProgress(holder).isDone());
    }

    private static boolean earnedByPlaying(ResourceLocation id) {
        return id.getNamespace().equals(QuasiumMod.MOD_ID) && !id.equals(ROOT) && !id.getPath().startsWith("recipes/");
    }

    private static void touch(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player instanceof FakePlayer || ungiven(serverPlayer) == null) return;
        MinecraftServer server = serverPlayer.server;
        server.tell(new TickTask(server.getTickCount(), () -> {
            if (!serverPlayer.hasDisconnected()) SmokecraftTrigger.fire(serverPlayer, CONTACT);
        }));
    }

    private static boolean fromMod(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(QuasiumMod.MOD_ID);
    }

    private static boolean fromMod(@Nullable Entity entity) {
        return entity != null && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(QuasiumMod.MOD_ID);
    }
}
