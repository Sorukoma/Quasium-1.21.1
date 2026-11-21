package net.sorukoma.quasium.ritual.artifact;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.api.SmokeCastEvent;
import net.sorukoma.quasium.api.TobaccoConsumptionEvent;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;
import net.sorukoma.quasium.smokelands.sultan.SultanVows;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class ArtifactEvents {
    private static final ResourceLocation END_CITY = ResourceLocation.withDefaultNamespace("chests/end_city_treasure");
    private static final Map<UUID, Float> ETERNAL_REMAINDER = new HashMap<>();

    private ArtifactEvents() {
    }

    @SubscribeEvent
    public static void onCast(SmokeCastEvent event) {
        ServerPlayer player = event.player();
        boolean flask = carries(player, RitualRegistry.BOTTOMLESS_FLASK.get()) && !SultanVows.sealsFlask(player);
        boolean voidPipe = carries(player, RitualRegistry.VOID_PIPE.get());
        if (!flask && !voidPipe) return;
        if (voidPipe && bowlGrams(event.source().smokeBowl()) < RitualConfig.VOID_PIPE_LOW_BOWL_GRAMS.getAsInt()) {
            player.hurt(player.damageSources().magic(), RitualConfig.VOID_PIPE_HP_PER_CAST.getAsInt());
        }
        if (event.tobaccoUnits() <= 0) return;
        event.setTobaccoUnits(0);
        if (flask) event.setKaifCost(event.kaifCost() + RitualConfig.BOTTOMLESS_FLASK_KAIF_PER_CAST.getAsInt());
    }

    @SubscribeEvent
    public static void onTobacco(TobaccoConsumptionEvent event) {
        ServerPlayer player = event.player();
        if (!EternalCoalItem.feeds(player)) return;
        float extra = ETERNAL_REMAINDER.getOrDefault(player.getUUID(), 0f)
                + event.units() * (RitualConfig.ETERNAL_COAL_GRAMS_PCT.getAsInt() - 100) / 100f;
        int whole = (int) extra;
        ETERNAL_REMAINDER.put(player.getUUID(), extra - whole);
        event.setUnits(event.units() + whole);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ETERNAL_REMAINDER.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel level) DreamCatcherBlock.catchNight(level, level.players());
    }

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        if (!END_CITY.equals(event.getName())) return;
        event.getTable().addPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(RitualRegistry.VOID_PIPE.get()))
                .when(LootItemRandomChanceCondition.randomChance(RitualConfig.VOID_PIPE_END_CITY_PCT.getAsInt() / 100f))
                .build());
    }

    @SubscribeEvent
    public static void onEquip(LivingEquipmentChangeEvent event) {
        if (event.getSlot().isArmor() && isArtifact(event.getTo()) && !isArtifact(event.getFrom())) chime(event.getEntity());
    }

    @SubscribeEvent
    public static void onCurio(CurioChangeEvent event) {
        if (event.getTo().has(RitualRegistry.QUASIUM_HARNESS.get()) && !event.getFrom().has(RitualRegistry.QUASIUM_HARNESS.get())) {
            chime(event.getEntity());
        }
    }

    public static boolean carries(Player player, Item item) {
        return player.getInventory().contains(stack -> stack.is(item));
    }

    private static int bowlGrams(ItemStack bowl) {
        ModDataComponents.HookahBowlContents contents = bowl.get(ModDataComponents.HOOKAH_BOWL_CONTENTS.get());
        return contents == null ? 0 : contents.filledSlotCount();
    }

    private static boolean isArtifact(ItemStack stack) {
        return stack.has(RitualRegistry.SMOKE_CARAPACE.get()) || stack.has(RitualRegistry.SMOKESTEP.get())
                || stack.has(RitualRegistry.SMOKE_THREAD.get());
    }

    private static void chime(LivingEntity wearer) {
        if (wearer.level().isClientSide) return;
        wearer.level().playSound(null, wearer.blockPosition(), RitualRegistry.ARTIFACT_EQUIP.get(), SoundSource.PLAYERS, 0.8f, 1f);
    }
}
