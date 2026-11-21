package net.sorukoma.quasium.magic;

import net.minecraft.Util;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.api.SmokeCastEvent;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.SmokeAura;
import net.sorukoma.quasium.ecology.SmokeDensity;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.mind.MindStats;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCasting;
import net.sorukoma.quasium.magic.page.PageSpells;
import net.sorukoma.quasium.network.MagicStatePacket;
import net.sorukoma.quasium.network.PacketStopHookahPuffSoundClient;
import net.sorukoma.quasium.ritual.artifact.QuasiumHarness;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SmokeCasting {
    public enum PuffResult { NORMAL, HOLD, COUGH }

    private static final int SYNC_INTERVAL_TICKS = 2;
    private static final int RING_MARK_TTL_TICKS = 5;
    private static final int BREATH_TRUST_TICKS = 3;
    private static final Map<UUID, CastState> STATES = new HashMap<>();

    private static final class CastState {
        float lungs;
        boolean fromFlask;
        int holdTicks;
        int puffTicks;
        long pulledAtMs;
        long breathAt;
        boolean released;
        @Nullable
        ResourceLocation spellId;
        long lockUntil;
        int ticksSinceSync;
        @Nullable
        Integer ringClaimMs;
        @Nullable
        Long ringElapsedMs;
        long ringMarkedAt;
        final Map<ResourceLocation, Long> readyAt = new HashMap<>();
    }

    private SmokeCasting() {
    }

    private static CastState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new CastState());
    }

    public static int capacity(ServerPlayer player, ItemStack bowl) {
        int bonus = FocusUpgrade.CONDENSER.on(bowl) ? Perk.focusBonus(player, Config.UPGRADE_CONDENSER_ML.getAsInt()) : 0;
        return Math.round((Config.LUNG_CAPACITY_MB.getAsInt() + bonus) * Perk.lungMultiplier(player));
    }

    public static int lungCost(ServerPlayer player, Spell spell) {
        return MagicMath.withDiscount(spell.lungs(), PlayerZones.castDiscountPct(player));
    }

    public static int tobaccoUnits(ServerPlayer player, Spell spell, ItemStack bowl) {
        return tobaccoUnits(player, spell.grams(), bowl);
    }

    public static int tobaccoUnits(ServerPlayer player, float baseGrams, ItemStack bowl) {
        float grams = baseGrams * (100 - PlayerZones.castDiscountPct(player)) / 100f;
        if (FocusUpgrade.COOLER.on(bowl)) grams *= Math.max(0, 100 - Perk.focusBonus(player, Config.UPGRADE_COOLER_PCT.getAsInt())) / 100f;
        grams *= AromaEffects.gramsMultiplier(player);
        return Math.round(grams * SmokeSource.UNITS_PER_GRAM);
    }

    public static PuffResult onPuffTick(ServerPlayer player, SmokeSource source, float heatEfficiency) {
        drew(player);
        CastState state = state(player);
        ItemStack bowl = source.smokeBowl();
        ResourceLocation spellId = PageCasting.resolve(player, bowl, Research.knownSpells(player));
        Spell spell = Spells.get(spellId);
        if (spell == null) {
            if (state.lungs > 0f || state.spellId != null) {
                clear(player, state);
            } else if (PageCasting.hides(player) && ++state.ticksSinceSync >= SYNC_INTERVAL_TICKS) {
                sync(player, state, capacity(player, bowl));
            }
            return PuffResult.NORMAL;
        }
        if (player.level().getGameTime() < state.lockUntil) return PuffResult.NORMAL;
        if (!spellId.equals(state.spellId)) {
            state.spellId = spellId;
            state.holdTicks = 0;
        }
        int capacity = QuasiumHarness.lungs(player, source, capacity(player, bowl));
        if (state.lungs <= 0f) {
            player.level().playSound(null, player.blockPosition(), ModSoundEvents.CAST_INHALE.get(), SoundSource.PLAYERS, 0.6f, 1f);
            if (source.useFlaskCharge()) {
                state.lungs = Math.min(capacity, lungCost(player, spell));
                state.fromFlask = true;
            }
        }
        state.lungs = MagicMath.fillLungs(state.lungs, capacity, Config.PUFF_FILL_MB.getAsInt(), heatEfficiency);
        if (state.lungs >= lungCost(player, spell)) {
            state.holdTicks++;
            if (!PlayerZones.holdsWithoutCough(player) && MagicMath.coughs(state.holdTicks, Config.HOLD_COUGH_AFTER_SEC.getAsInt())) {
                cough(player, state);
                return PuffResult.COUGH;
            }
        }
        if (++state.ticksSinceSync >= SYNC_INTERVAL_TICKS) sync(player, state, capacity);
        return state.lungs >= capacity ? PuffResult.HOLD : PuffResult.NORMAL;
    }

    public static void drew(ServerPlayer player) {
        if (state(player).puffTicks++ == 0) SmokeTricks.puffStarted(player);
    }

    public static void pullStarted(ServerPlayer player) {
        CastState state = state(player);
        state.pulledAtMs = Util.getMillis();
        state.puffTicks = 0;
        state.released = false;
    }

    public static boolean endPull(ServerPlayer player) {
        CastState state = state(player);
        boolean counted = state.puffTicks >= ticks(Config.PULL_MIN_MS);
        if (!state.released) PacketDistributor.sendToPlayer(player, new PacketStopHookahPuffSoundClient());
        state.released = false;
        state.puffTicks = 0;
        state.breathAt = player.level().getGameTime() + ticks(Config.BREATH_RECOVERY_MS);
        boolean shown = PageCasting.dropShown(player);
        if (state.lungs > 0f || state.spellId != null || shown) clear(player, state);
        return counted;
    }

    public static boolean holdsSmoke(Player player) {
        CastState state = STATES.get(player.getUUID());
        return state != null && state.lungs > 0f;
    }

    public static boolean breathReady(Player player) {
        CastState state = STATES.get(player.getUUID());
        return state == null || player.level().getGameTime() >= state.breathAt - BREATH_TRUST_TICKS;
    }

    private static int ticks(ModConfigSpec.IntValue ms) {
        return ms.get() * MagicMath.TICKS_PER_SECOND / 1000;
    }

    public static void markRing(ServerPlayer player, int claimedMs) {
        mark(player, claimedMs, null);
    }

    public static void markRing(ServerPlayer player, int claimedMs, long serverElapsedMs) {
        mark(player, claimedMs, serverElapsedMs);
    }

    private static void mark(ServerPlayer player, int claimedMs, @Nullable Long serverElapsedMs) {
        CastState state = state(player);
        state.ringClaimMs = claimedMs;
        state.ringElapsedMs = serverElapsedMs;
        state.ringMarkedAt = player.level().getGameTime();
    }

    @Nullable
    private static Integer ringOffset(ServerPlayer player, CastState state, long now) {
        Integer claimed = state.ringClaimMs;
        state.ringClaimMs = null;
        if (claimed == null || now - state.ringMarkedAt > RING_MARK_TTL_TICKS) return null;
        long elapsed = state.ringElapsedMs != null ? state.ringElapsedMs : Util.getMillis() - state.pulledAtMs;
        return Perk.ringOffset(player, MagicMath.checkedRingOffset(claimed, elapsed));
    }

    public static void release(ServerPlayer player, SmokeSource source) {
        CastState state = STATES.get(player.getUUID());
        if (state == null) return;
        state.released = true;
        long now = player.level().getGameTime();
        Integer ringOffset = ringOffset(player, state, now);
        int puffTicks = state.puffTicks;
        ResourceLocation spellId = state.spellId;
        Spell spell = Spells.get(spellId);
        boolean ringSpell = spell != null && spell.form() == SpellForm.RING;
        if (ringOffset != null && !ringSpell) {
            clear(player, state);
            SmokeTricks.ring(player, source, ringOffset, puffTicks);
            return;
        }
        if (spell == null) {
            SmokeTricks.exhale(player, source, puffTicks);
            return;
        }
        if (state.lungs <= 0f) return;
        float lungs = state.lungs;
        float power = chargeOf(player, state);
        clear(player, state);
        if (ringOffset != null) {
            if (ringOffset > Config.RING_WINDOW_MS.getAsInt() / 2) {
                hint(player, Component.translatable("quasium.ring.broken"));
                return;
            }
            if (ringOffset <= Config.RING_PERFECT_MS.getAsInt()) {
                power *= Config.RING_PERFECT_POWER_PCT.getAsInt() / 100f;
                player.level().playSound(null, player.blockPosition(), ModSoundEvents.RING_PERFECT.get(), SoundSource.PLAYERS, 0.8f, 1f);
            }
        }
        tryCast(player, source, state, spellId, spell, lungs, power, now);
    }

    private static void tryCast(ServerPlayer player, SmokeSource source, CastState state, ResourceLocation spellId, Spell spell,
                                float lungs, float power, long now) {
        ItemStack bowl = source.smokeBowl();
        FocusUpgrade upgrade = FocusUpgrade.of(bowl);
        CompiledPage page = PageSpells.get(spellId);
        if (page != null) {
            page = PageCasting.boosted(page, lungs, upgrade);
            spellId = page.id();
            spell = page.spell();
        }
        long readyAt = page != null ? PageCasting.readyAt(state.readyAt, page) : state.readyAt.getOrDefault(spellId, 0L);
        if (now < readyAt) {
            hint(player, Component.translatable("quasium.cast.cooldown", String.format("%.1f", (readyAt - now) / 20f)));
            return;
        }
        int lungCost = lungCost(player, spell);
        if (lungs < lungCost) {
            hint(player, Component.translatable("quasium.cast.misfire", Math.round(lungs), lungCost));
            return;
        }
        if (PlayerKaifData.getKaifValue(player) < spell.kaif()) {
            hint(player, Component.translatable("quasium.cast.need_kaif", spell.kaif()));
            return;
        }
        boolean fromFlask = state.fromFlask;
        state.fromFlask = false;
        int essenceCost = Config.ESSENCE_PER_CAST.getAsInt() * (fromFlask ? Config.FLASK_CHARGE_ESSENCE_MULTIPLIER.getAsInt() : 1);
        float grams = page != null ? PageCasting.grams(page, lungs) : spell.grams();
        SmokeCastEvent cast = NeoForge.EVENT_BUS.post(new SmokeCastEvent(player, spellId, spell, source, fromFlask,
                fromFlask ? 0 : tobaccoUnits(player, grams, bowl), essenceCost));
        if (cast.isCanceled()) {
            if (cast.refusal() != null) hint(player, cast.refusal());
            return;
        }
        if (cast.tobaccoUnits() > 0 && !source.burnForCast(player, cast.tobaccoUnits())) {
            hint(player, Component.translatable("quasium.cast.no_tobacco", String.format(java.util.Locale.ROOT, "%.1f", grams)));
            return;
        }
        if (cast.kaifCost() > 0) PlayerKaifData.subtractKaif(player, cast.kaifCost());
        int selfGrams = Math.round(spell.param("self_grams", 0f) * SmokeSource.UNITS_PER_GRAM);
        if (selfGrams > 0) PlayerOverdoseData.recordConsumption(player, selfGrams);
        if (page != null) {
            PageCasting.drain(bowl, page, cast.essenceCost());
            PageCasting.startCooldowns(state.readyAt, page, now, harnessRatio(player, source, spell));
        } else {
            bowl.set(ModDataComponents.BOWL_FOCUS.get(), FocusEssence.drained(Spells.focusOf(bowl), cast.essenceCost()));
            state.readyAt.put(spellId, now + QuasiumHarness.cooldown(player, source, spell));
        }
        PlayerKaifData.addKaif(player, Config.KAIF_PER_CAST.getAsInt());
        MindStats.award(player, MindRegistry.CASTS, 1);
        if (page != null) {
            PageCasting.announce(player, page);
            SpellCaster.cast(player, spellId, spell, PageCasting.power(page, power), page.isSeal() ? upgrade : null);
        } else {
            SmokecraftTrigger.fire(player, "cast");
            SmokecraftTrigger.fire(player, "cast:" + spellId.getPath());
            SpellCaster.cast(player, spellId, spell, power, upgrade);
        }
        if (upgrade != FocusUpgrade.FILTER) SmokeDensity.add(player.level(), player.blockPosition(), Config.DENSITY_PER_CAST.getAsInt());
    }

    private static float harnessRatio(ServerPlayer player, SmokeSource source, Spell spell) {
        int base = spell.cooldownTicks();
        return base <= 0 ? 1f : QuasiumHarness.cooldown(player, source, spell) / (float) base;
    }

    public static void setCooldown(ServerPlayer player, ResourceLocation spellId, int ticks) {
        state(player).readyAt.put(spellId, player.level().getGameTime() + ticks);
    }

    public static float drainLungs(ServerPlayer player, float ml) {
        CastState state = STATES.get(player.getUUID());
        if (state == null || state.lungs <= 0f) return 0f;
        float taken = Math.min(ml, state.lungs);
        state.lungs -= taken;
        if (state.lungs <= 0f) {
            clear(player, state);
        } else {
            sync(player, state, capacity(player, ItemStack.EMPTY));
        }
        return taken;
    }

    public static void forget(UUID playerId) {
        STATES.remove(playerId);
        PageCasting.forget(playerId);
        SmokeTricks.forget(playerId);
    }

    private static float chargeOf(ServerPlayer player, CastState state) {
        int thickAir = SmokeAura.bonusPct(player.level(), player.blockPosition(), EcologyConfig.BONUS_HOLD_PCT);
        return MagicMath.holdMultiplier(state.holdTicks, Config.HOLD_CHARGE_PER_SEC_PCT.getAsInt() * (100 + thickAir) / 100,
                Config.HOLD_MAX_MULTIPLIER_PCT.getAsInt());
    }

    private static void cough(ServerPlayer player, CastState state) {
        state.lockUntil = player.level().getGameTime() + Config.COUGH_LOCK_SEC.getAsInt() * 20L;
        clear(player, state);
        PlayerKaifData.subtractKaif(player, Config.COUGH_KAIF_LOSS.getAsInt());
        player.level().playSound(null, player.blockPosition(), ModSoundEvents.COUGH.get(), SoundSource.PLAYERS,
                1f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        if (player.level() instanceof ServerLevel level) {
            Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.4));
            level.sendParticles(ParticleTypes.POOF, mouth.x, mouth.y, mouth.z, 8, 0.15, 0.1, 0.15, 0.02);
        }
        hint(player, Component.translatable("quasium.cast.cough"));
    }

    private static void clear(ServerPlayer player, CastState state) {
        state.lungs = 0f;
        state.fromFlask = false;
        state.holdTicks = 0;
        state.spellId = null;
        sync(player, state, capacity(player, ItemStack.EMPTY));
    }

    private static void sync(ServerPlayer player, CastState state, int capacity) {
        state.ticksSinceSync = 0;
        Spell spell = Spells.get(state.spellId);
        PacketDistributor.sendToPlayer(player, new MagicStatePacket(state.lungs, capacity,
                state.spellId == null ? "" : state.spellId.toString(), spell == null ? 0 : lungCost(player, spell),
                chargeOf(player, state), state.holdTicks,
                PlayerZones.holdsWithoutCough(player) ? 0 : Config.HOLD_COUGH_AFTER_SEC.getAsInt() * 20,
                PageCasting.shownLabel(player), PageCasting.shownMissing(player)));
    }

    private static void hint(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }
}
