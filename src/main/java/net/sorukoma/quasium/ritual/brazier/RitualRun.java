package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.block.entity.NoteJarBlockEntity;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualMath;
import net.sorukoma.quasium.ritual.RitualRecipe;
import net.sorukoma.quasium.ritual.RitualRecipes;
import net.sorukoma.quasium.ritual.RitualRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RitualRun {
    public enum State { RUNNING, DONE, BROKEN }

    private static final int SECOND = RitualMath.TICKS_PER_SECOND;
    private static final int LOOP_TICKS = 8 * SECOND;
    private static final int JET_POINTS = 6;
    private static final int LEGACY_DRAFT_PER_PUFF = 80;

    private final ResourceLocation id;
    private final RitualRecipe recipe;
    private final List<BlockPos> pedestals;
    private final int required;
    private final RitualRhythm rhythm;
    private final Map<NoteType, Integer> drawn = new EnumMap<>(NoteType.class);
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private final float[] draftSeconds = new float[RitualConfig.DRAFT_WINDOW_SEC.getAsInt()];
    private RitualMath.Weights weights = RitualMath.Weights.current();
    private int ticks;
    private int secondsSeen;
    private float secondDraft;
    private double work;
    private float instability;
    private float drift;
    private float averageKaif;
    private Set<NoteType> dry = EnumSet.noneOf(NoteType.class);
    private boolean group;
    private List<ServerPlayer> participants = List.of();
    @Nullable
    private RitualBars bars;

    RitualRun(ResourceLocation id, RitualRecipe recipe, List<BlockPos> pedestals) {
        this.id = id;
        this.recipe = recipe;
        this.pedestals = List.copyOf(pedestals);
        this.required = recipe.work();
        this.rhythm = new RitualRhythm(recipe.pulseMs(), recipe.windowMs());
        this.instability = recipe.base();
    }

    public static Component displayName(ResourceLocation ritual) {
        return Component.translatable("ritual." + ritual.getNamespace() + "." + ritual.getPath());
    }

    public ResourceLocation id() {
        return id;
    }

    public RitualRecipe recipe() {
        return recipe;
    }

    public List<BlockPos> pedestals() {
        return pedestals;
    }

    public int ticks() {
        return ticks;
    }

    public double period() {
        return rhythm.period();
    }

    public float progress() {
        return (float) Math.min(1.0, work / required);
    }

    public float instability() {
        return instability;
    }

    public float drift() {
        return drift;
    }

    public float averageKaif() {
        return averageKaif;
    }

    public boolean group() {
        return group;
    }

    public boolean starved() {
        return !dry.isEmpty();
    }

    public Map<NoteType, Integer> drawn() {
        return Collections.unmodifiableMap(drawn);
    }

    public List<ServerPlayer> participants() {
        return participants;
    }

    public Set<UUID> puffers() {
        return lastHit.keySet();
    }

    public float draftRate() {
        int seconds = Math.min(secondsSeen, draftSeconds.length);
        if (seconds == 0) return 0f;
        float sum = 0f;
        for (int index = 0; index < seconds; index++) sum += draftSeconds[index];
        return sum / seconds;
    }

    public RitualRhythm.Beat press(ServerPlayer player, double runTick) {
        if (starved()) return RitualRhythm.Beat.PAUSED;
        double lag = RitualMath.ticks(RitualConfig.PRESS_LAG_MS.getAsInt());
        RitualRhythm.Beat beat = rhythm.press(player.getUUID(), Math.clamp(runTick, ticks - lag, ticks + 1.0));
        if (beat.kind() == RitualRhythm.Kind.HIT) {
            work = Math.min(required, work + multiplier(beat.streak()));
            instability = Math.max(0f, instability - weights.hitRelief());
            lastHit.put(player.getUUID(), ticks);
        } else if (beat.kind() == RitualRhythm.Kind.MISS) {
            instability += weights.missPenalty();
        }
        return beat;
    }

    public static float multiplier(int streak) {
        return RitualMath.streakMultiplier(streak, RitualConfig.STREAK_HITS.getAsInt(), RitualConfig.STREAK_MAX_PCT.getAsInt());
    }

    State tick(ServerLevel level, RitualBrazierBlockEntity brazier, float draft) {
        ticks++;
        secondDraft += draft;
        if (!starved()) {
            float efficiency = draft * SECOND / Math.max(1, recipe.rate());
            work = Math.min(required, work + RitualMath.networkWork(efficiency, RitualConfig.NETWORK_SPEED_PCT.getAsInt(),
                    RitualConfig.STREAK_MAX_PCT.getAsInt(), rhythm.period()));
        }
        if (ticks % SECOND == 0) second(level, brazier);
        while (rhythm.due(ticks, RitualConfig.PRESS_LAG_MS.getAsInt())) judge();
        if (instability >= RitualConfig.RITUAL_BREAK_THRESHOLD.getAsInt()) return State.BROKEN;
        return !starved() && RitualMath.reached(work, required) && notesDrawn() ? State.DONE : State.RUNNING;
    }

    private void judge() {
        if (starved()) {
            rhythm.skipNext();
            instability += RitualConfig.STARVE_PENALTY.get().floatValue();
            return;
        }
        boolean hit = rhythm.judgeNext();
        float coverage = RitualMath.draftFactor(draftRate(), recipe.rate());
        instability += drift + (hit ? 0f : RitualMath.emptyPulse(weights.emptyPenalty(), coverage));
    }

    private void second(ServerLevel level, RitualBrazierBlockEntity brazier) {
        draftSeconds[secondsSeen++ % draftSeconds.length] = secondDraft;
        secondDraft = 0f;
        int window = RitualConfig.GROUP_WINDOW_SEC.getAsInt() * SECOND;
        lastHit.values().removeIf(at -> ticks - at > window);
        boolean wasGroup = group;
        group = lastHit.size() >= RitualConfig.GROUP_PLAYERS.getAsInt();
        BlockPos center = brazier.getBlockPos();
        drawNotes(level, center, RitualSite.jars(level, center));
        participants = RitualSite.participants(level, center);
        averageKaif = RitualSite.averageKaif(participants);
        weights = RitualMath.Weights.current();
        drift = RitualMath.drift(recipe.base(), averageKaif, group, brazier.hasFilter(), weights);
        if (group && !wasGroup) RitualCues.groupFormed(this);
        if (starved()) RitualCues.starving(this, dry);
        if (bars == null) bars = new RitualBars(id);
        bars.update(participants, progress(), instability, group, starved());
        if (secondsSeen % (LOOP_TICKS / SECOND) == 1) {
            level.playSound(null, center, RitualRegistry.RITUAL_LOOP.get(), SoundSource.BLOCKS, 0.9f, 0.8f + 0.6f * progress());
        }
    }

    private void drawNotes(ServerLevel level, BlockPos center, List<NoteJarBlockEntity> jars) {
        Set<NoteType> lacking = EnumSet.noneOf(NoteType.class);
        float done = progress();
        for (Map.Entry<NoteType, Integer> need : recipe.notes().entrySet()) {
            NoteType note = need.getKey();
            int missing = (int) Math.ceil(done * need.getValue()) - drawn.getOrDefault(note, 0);
            for (NoteJarBlockEntity jar : jars) {
                if (missing <= 0) break;
                if (jar.note() != note) continue;
                int taken = jar.take(missing);
                if (taken <= 0) continue;
                missing -= taken;
                drawn.merge(note, taken, Integer::sum);
                jet(level, jar.getBlockPos(), center, note.color());
            }
            if (missing > 0) lacking.add(note);
        }
        dry = lacking;
    }

    private boolean notesDrawn() {
        return recipe.notes().entrySet().stream().allMatch(need -> drawn.getOrDefault(need.getKey(), 0) >= need.getValue());
    }

    private static void jet(ServerLevel level, BlockPos jar, BlockPos brazier, int color) {
        Vec3 from = Vec3.atCenterOf(jar).add(0, 0.3, 0);
        Vec3 to = Vec3.atCenterOf(brazier).add(0, 0.5, 0);
        for (int point = 0; point < JET_POINTS; point++) {
            Vec3 at = from.lerp(to, (point + level.random.nextFloat()) / JET_POINTS);
            level.sendParticles(ModParticles.noteSmoke(color), at.x, at.y + Math.sin(point * 0.9) * 0.15, at.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    void hideBars() {
        if (bars != null) bars.hide();
    }

    boolean reserves(BlockPos pedestal) {
        return pedestals.contains(pedestal);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Ritual", id.toString());
        tag.put("Pedestals", new LongArrayTag(pedestals.stream().mapToLong(BlockPos::asLong).toArray()));
        tag.putDouble("Work", work);
        tag.putFloat("Instability", instability);
        CompoundTag notes = new CompoundTag();
        drawn.forEach((note, ml) -> notes.putInt(note.id(), ml));
        tag.put("Drawn", notes);
        return tag;
    }

    @Nullable
    static RitualRun load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Ritual"));
        RitualRecipe recipe = RitualRecipes.get(id);
        if (recipe == null) return null;
        List<BlockPos> pedestals = Arrays.stream(tag.getLongArray("Pedestals")).mapToObj(BlockPos::of).toList();
        RitualRun run = new RitualRun(id, recipe, pedestals);
        double done = tag.contains("Work") ? tag.getDouble("Work") / run.required
                : tag.getFloat("Delivered") / ((double) recipe.puffs() * LEGACY_DRAFT_PER_PUFF);
        run.work = Math.clamp(done, 0.0, 1.0) * run.required;
        run.instability = tag.getFloat("Instability");
        CompoundTag notes = tag.getCompound("Drawn");
        for (String key : notes.getAllKeys()) {
            NoteType note = NoteType.byId(key);
            if (note != null && notes.contains(key, Tag.TAG_INT)) run.drawn.put(note, notes.getInt(key));
        }
        return run;
    }
}
