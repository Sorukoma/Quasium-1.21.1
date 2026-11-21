package net.sorukoma.quasium.mind.dream;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindMath;
import net.sorukoma.quasium.mind.MindPackets;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.mind.research.ResearchRules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class Dreams {
    public static final String INSPIRED_UNTIL = "InspiredUntil";
    public static final String NIGHTMARES = "nightmares";
    private static final String DREAM_AGAIN_AT = "DreamAgainAt";
    private static final int DAY = 24000;
    private static final int SUNSET = 12000;
    private static final Map<UUID, Integer> RESTLESS = new HashMap<>();
    private static final Set<UUID> LUCID = new HashSet<>();

    private Dreams() {
    }

    public static int[] weights(Player player) {
        boolean controlled = Perk.DREAM_CONTROL.ownedBy(player);
        int[] weights = new int[DreamOutcome.values().length];
        for (DreamOutcome outcome : DreamOutcome.values()) weights[outcome.ordinal()] = outcome.weight(controlled);
        return weights;
    }

    @SubscribeEvent
    public static void onTryToSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        RESTLESS.put(player.getUUID(), player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)));
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.wakeImmediately() || !(event.getEntity() instanceof ServerPlayer player) || !player.isSleepingLongEnough()) return;
        rested(player);
    }

    public static void rested(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now < MindData.time(player, DREAM_AGAIN_AT)) return;
        MindData.setTime(player, DREAM_AGAIN_AT, now + MindConfig.DREAM_COOLDOWN_TICKS.getAsInt());
        sleep(player);
    }

    public static Optional<DreamOutcome> sleep(ServerPlayer player) {
        if (PlayerKaifData.getKaifValue(player) <= MindConfig.NIGHTMARE_KAIF.getAsInt()) {
            nightmare(player);
            return Optional.empty();
        }
        Optional<DreamOutcome> outcome = DreamDraught.take(player) ? Optional.of(DreamOutcome.LUCID) : roll(player, player.getRandom());
        outcome.ifPresent(dream -> dream(player, dream));
        return outcome;
    }

    public static Optional<DreamOutcome> roll(Player player, RandomSource random) {
        if (PlayerKaifData.getKaifValue(player) < MindConfig.DREAM_KAIF.getAsInt()) return Optional.empty();
        int[] weights = weights(player);
        int total = MindMath.total(weights);
        if (total <= 0) return Optional.empty();
        return Optional.of(DreamOutcome.values()[MindMath.pick(weights, random.nextInt(total))]);
    }

    private static void nightmare(ServerPlayer player) {
        Integer restless = RESTLESS.remove(player.getUUID());
        if (restless != null) player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), restless);
        record(player, NIGHTMARES);
        int seconds = MindConfig.NIGHTMARE_HALLUCINATION_SECONDS.getAsInt();
        MindPackets.cue(player, "nightmare", seconds * 20);
        player.level().playSound(null, player.blockPosition(), MindRegistry.AWAKEN_NIGHTMARE.get(), SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.translatable("quasium.dream.nightmare.woke").withStyle(ChatFormatting.DARK_RED), false);
        SmokecraftTrigger.fire(player, "nightmare");
    }

    static void dream(ServerPlayer player, DreamOutcome outcome) {
        if (outcome == DreamOutcome.RECIPE && hiddenSpellsLeft(player).isEmpty()) outcome = DreamOutcome.INSIGHT;
        record(player, outcome.id());
        MindData.raise(player, ResearchRules.DREAMT);
        MindData.raise(player, ResearchRules.DREAMT + ":" + outcome.id());
        player.level().playSound(null, player.blockPosition(), outcome.sound(), SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.translatable("quasium.dream.woke", outcome.title()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        SmokecraftTrigger.fire(player, "dream");
        SmokecraftTrigger.fire(player, "dream:" + outcome.id());
        apply(player, outcome);
    }

    private static void apply(ServerPlayer player, DreamOutcome outcome) {
        switch (outcome) {
            case INSPIRATION -> MindData.setTime(player, INSPIRED_UNTIL, player.level().getGameTime() + untilSunset(player));
            case COMPASS -> DreamCompass.start(player);
            case RECIPE -> {
                List<ResourceLocation> left = hiddenSpellsLeft(player);
                ResourceLocation spell = left.get(player.getRandom().nextInt(left.size()));
                Research.learnSpell(player, spell);
                player.displayClientMessage(Component.translatable("quasium.research.spell", Spell.displayName(spell))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            case INSIGHT -> PlayerMagicData.addInsight(player, MindConfig.DREAM_INSIGHT.getAsInt());
            case LUCID -> {
                LUCID.add(player.getUUID());
                MindPackets.cue(player, "lucid", 0);
            }
        }
    }

    public static void chooseLucid(ServerPlayer player, String choice) {
        if (!LUCID.remove(player.getUUID())) return;
        for (DreamOutcome outcome : DreamOutcome.values()) {
            if (outcome != DreamOutcome.LUCID && outcome.id().equals(choice)) {
                SmokecraftTrigger.fire(player, "lucid_choice");
                dream(player, outcome);
                return;
            }
        }
    }

    public static boolean inspired(Player player) {
        return player.level().getGameTime() < MindData.time(player, INSPIRED_UNTIL);
    }

    public static int inspired(Player player, int essenceMl) {
        return inspired(player) ? Math.round(essenceMl * (100 + MindConfig.INSPIRATION_PCT.getAsInt()) / 100f) : essenceMl;
    }

    static long untilSunset(Player player) {
        long time = player.level().getDayTime() % DAY;
        return (SUNSET - time + DAY) % DAY;
    }

    private static List<ResourceLocation> hiddenSpellsLeft(ServerPlayer player) {
        Set<ResourceLocation> known = Research.knownSpells(player);
        return Spells.all().entrySet().stream().filter(entry -> entry.getValue().hidden() && !known.contains(entry.getKey()))
                .map(Map.Entry::getKey).toList();
    }

    private static void record(ServerPlayer player, String key) {
        CompoundTag journal = MindData.root(player).getCompound(MindData.DREAMS);
        journal.putInt(key, journal.getInt(key) + 1);
        MindData.root(player).put(MindData.DREAMS, journal);
        MindData.changed(player);
    }
}
