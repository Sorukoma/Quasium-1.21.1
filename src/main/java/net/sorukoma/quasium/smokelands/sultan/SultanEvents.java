package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.api.RitualCompleteEvent;
import net.sorukoma.quasium.api.SmokeCastEvent;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.brazier.RitualPedestalBlockEntity;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SultanEvents {
    public static final ResourceLocation RESPAWN_RITUAL = SmokelandsDimension.id("sultan_respawn");
    private static final long SPLASH_TICKS = 40;
    private static final Map<UUID, Long> DEW_CASTS = new HashMap<>();

    private SultanEvents() {
    }

    public static boolean isDew(ItemStack stack) {
        return NoteType.DEW.id().equals(EssenceBottleItem.typeOf(stack))
                && EssenceBottleItem.amountOf(stack) >= ConfigArgs.synced(SmokelandsConfig.DOUSE_DEW_ML);
    }

    public static void pour(ServerPlayer player, ItemStack bottle) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1f, 1f);
        if (player.getAbilities().instabuild) return;
        int left = EssenceBottleItem.amountOf(bottle) - SmokelandsConfig.DOUSE_DEW_ML.get();
        if (left > 0) {
            bottle.set(ModDataComponents.ESSENCE_AMOUNT.get(), left);
            return;
        }
        bottle.shrink(1);
        ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
        if (!player.getInventory().add(empty)) player.drop(empty, false);
    }

    @SubscribeEvent
    public static void onCast(SmokeCastEvent event) {
        boolean dew = event.spell().inputs().stream().anyMatch(input -> input.containsKey(NoteType.DEW.id()));
        if (dew) DEW_CASTS.put(event.player().getUUID(), event.player().level().getGameTime());
    }

    @Nullable
    public static ServerPlayer dewSplasher(DamageSource source) {
        if (!source.is(CastContext.SMOKE_MAGIC) || !(source.getEntity() instanceof ServerPlayer player)) return null;
        Long castAt = DEW_CASTS.get(player.getUUID());
        return castAt != null && player.level().getGameTime() - castAt <= SPLASH_TICKS ? player : null;
    }

    @SubscribeEvent
    public static void onRitual(RitualCompleteEvent event) {
        if (!RESPAWN_RITUAL.equals(event.ritualId())) return;
        Component answer = callBack(event.level(), event.center(), event.level().getGameTime());
        event.participants().forEach(player -> player.displayClientMessage(answer, false));
    }

    public static Component callBack(ServerLevel level, BlockPos brazier, long now) {
        SultanArena arena = SultanArena.of(level);
        Map<SultanVows.Vow, RitualPedestalBlockEntity> vows = SultanVows.placed(level, brazier);
        SultanArena.Call call = arena.call(brazier, now, vows.keySet());
        if (call == SultanArena.Call.CALLED) SultanVows.burn(level, vows.values());
        return switch (call) {
            case NO_ARENA -> Component.translatable("quasium.sultan.ritual.no_arena");
            case ALIVE -> Component.translatable("quasium.sultan.ritual.alive");
            case TOO_EARLY -> Component.translatable("quasium.sultan.ritual.too_early", daysLeft(arena, brazier, now));
            case CALLED -> vows.isEmpty() ? Component.translatable("quasium.sultan.ritual.called")
                    : Component.translatable("quasium.sultan.ritual.called_vows", SultanVows.names(vows.keySet()));
        };
    }

    private static long daysLeft(SultanArena arena, BlockPos brazier, long now) {
        long ticks = arena.nearestThrone(brazier).map(throne -> arena.ticksUntilRespawn(throne, now)).orElse(0L);
        return (ticks + SultanArena.DAY_TICKS - 1) / SultanArena.DAY_TICKS;
    }
}
