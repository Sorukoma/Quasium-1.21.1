package net.sorukoma.quasium.magic;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.block.LowTableBlock;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.entity.SmokeRingEntity;
import net.sorukoma.quasium.entity.SmokeTrickEntity;
import net.sorukoma.quasium.hookah.ExhaleSmoke;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokeTricks {
    public enum Trick {
        RING(true), GHOST(false), JELLYFISH(true), TORNADO(false);

        private final boolean timed;

        Trick(boolean timed) {
            this.timed = timed;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String id() {
            return "trick_" + key();
        }
    }

    private static final int MIN_PUFF_TICKS = 20;
    private static final float DOWN_PITCH = 45f;
    private static final float UP_PITCH = -45f;
    private static final double TABLE_SLACK = 0.5;
    private static final ResourceLocation TRICKSTER = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft/ring_combo_5");
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        int combo;
        long lastAt = Long.MIN_VALUE;
        long heldAt = Long.MIN_VALUE;
        @Nullable
        SmokeTrickEntity ghost;
    }

    private SmokeTricks() {
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    static void ring(ServerPlayer player, SmokeSource source, int offsetMs, int puffTicks) {
        if (puffTicks < MIN_PUFF_TICKS || !source.atPeak() || offsetMs > Config.RING_WINDOW_MS.getAsInt() / 2) {
            hint(player, Component.translatable(puffTicks < MIN_PUFF_TICKS ? "quasium.ring.no_smoke"
                    : source.atPeak() ? "quasium.ring.broken" : "quasium.ring.not_peak"));
            return;
        }
        boolean perfect = offsetMs <= Config.RING_PERFECT_MS.getAsInt();
        boolean down = player.getXRot() >= DOWN_PITCH;
        player.level().addFreshEntity(down ? SmokeTrickEntity.jellyfish(player, perfect) : SmokeRingEntity.trick(player, perfect));
        player.level().playSound(null, player.blockPosition(), (perfect ? ModSoundEvents.RING_PERFECT : ModSoundEvents.RING_RELEASE).get(),
                SoundSource.PLAYERS, 0.8f, 1f);
        performed(player, down ? Trick.JELLYFISH : Trick.RING);
    }

    static void exhale(ServerPlayer player, SmokeSource source, int puffTicks) {
        if (puffTicks < MIN_PUFF_TICKS || !source.atPeak() || Spells.forBowl(source.smokeBowl(), Research.knownSpells(player)) != null) return;
        Vec3 table = tableInSight(player);
        if (table == null && player.getXRot() > UP_PITCH) return;
        State state = state(player);
        state.heldAt = player.level().getGameTime();
        int color = ExhaleSmoke.color(player, source.burningRow());
        if (table != null) {
            player.level().addFreshEntity(SmokeTrickEntity.tornado(player, table, color));
            player.level().playSound(null, table.x, table.y, table.z, SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 0.6f, 1.3f);
            performed(player, Trick.TORNADO);
            return;
        }
        if (state.ghost != null) state.ghost.discard();
        state.ghost = SmokeTrickEntity.ghost(player, color);
        player.level().addFreshEntity(state.ghost);
    }

    static void puffStarted(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.ghost == null) return;
        SmokeTrickEntity ghost = state.ghost;
        state.ghost = null;
        if (!ghost.isAlive()) return;
        ghost.inhale(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 0.5f, 1.4f);
        performed(player, Trick.GHOST);
    }

    public static boolean holdsExhale(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.heldAt == player.level().getGameTime();
    }

    static void forget(UUID playerId) {
        STATES.remove(playerId);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) carryOverRing(player);
    }

    public static void carryOverRing(ServerPlayer player) {
        AdvancementHolder trickster = player.server.getAdvancements().get(TRICKSTER);
        if (trickster == null || MindData.flag(player, Trick.RING.id()) || !player.getAdvancements().getOrStartProgress(trickster).isDone()) return;
        MindData.raise(player, Trick.RING.id());
        SmokecraftTrigger.fire(player, Trick.RING.id());
    }

    private static void performed(ServerPlayer player, Trick trick) {
        MindData.raise(player, trick.id());
        SmokecraftTrigger.fire(player, trick.id());
        if (trick.timed) combo(player);
    }

    private static void combo(ServerPlayer player) {
        State state = state(player);
        long now = player.level().getGameTime();
        state.combo = now - state.lastAt <= Config.RING_COMBO_WINDOW_SEC.getAsInt() * 20L ? state.combo + 1 : 1;
        state.lastAt = now;
        if (state.combo >= 3) PlayerKaifData.addKaif(player, Config.RING_COMBO_KAIF.getAsInt());
        if (state.combo == 3) PlayerMagicData.addInsight(player, Config.RING_COMBO_INSIGHT.getAsInt());
        if (state.combo >= 2) hint(player, Component.translatable("quasium.ring.combo", state.combo));
        if (state.combo >= 5) SmokecraftTrigger.fire(player, "ring_combo_5");
    }

    @Nullable
    private static Vec3 tableInSight(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        double reach = Config.TRICK_TABLE_REACH.getAsInt();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach + 2)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !player.level().getBlockState(hit.getBlockPos()).is(LoungeBlocks.LOW_TABLE.get())) return null;
        Vec3 top = Vec3.atBottomCenterOf(hit.getBlockPos()).add(0, LowTableBlock.TOP_PX / 16.0, 0);
        return top.subtract(player.position()).horizontalDistance() <= reach + TABLE_SLACK ? top : null;
    }

    private static void hint(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }
}
