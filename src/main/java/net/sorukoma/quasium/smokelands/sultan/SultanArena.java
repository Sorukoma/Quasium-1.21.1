package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SultanArena extends SavedData {
    public enum Call { NO_ARENA, ALIVE, TOO_EARLY, CALLED }

    public static final long DAY_TICKS = 24000L;
    private static final String NAME = "quasium_sultan_arenas";
    private static final Factory<SultanArena> FACTORY = new Factory<>(SultanArena::new, SultanArena::load);

    private final Map<BlockPos, Entry> arenas = new HashMap<>();

    private static final class Entry {
        long defeatedAt = -1;
        boolean called;
        int vows;
        @Nullable
        UUID sultan;
    }

    public static SultanArena of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public Optional<SultanOfSmoke> wake(ServerLevel level, BlockPos throne) {
        double reach = SmokelandsConfig.WAKE_DISTANCE.get();
        boolean watched = level.getNearestPlayer(throne.getX() + 0.5, throne.getY() + 1, throne.getZ() + 0.5, reach,
                entity -> entity instanceof Player player && !player.isSpectator() && !player.isCreative()) != null;
        return watched ? rise(level, throne) : Optional.empty();
    }

    public Optional<SultanOfSmoke> rise(ServerLevel level, BlockPos throne) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !entitiesLoadedAround(level, throne)) return Optional.empty();
        Entry entry = arenas.computeIfAbsent(throne.immutable(), pos -> new Entry());
        Entity known = entry.sultan == null ? null : level.getEntity(entry.sultan);
        if (known != null && known.isAlive()) return Optional.empty();
        if (entry.defeatedAt >= 0 && !entry.called) return Optional.empty();
        SultanOfSmoke sultan = Sultans.SULTAN.get().create(level);
        if (sultan == null) return Optional.empty();
        sultan.settleAt(throne);
        sultan.vow(SultanVows.of(entry.vows));
        level.addFreshEntity(sultan);
        entry.sultan = sultan.getUUID();
        entry.called = false;
        entry.defeatedAt = -1;
        setDirty();
        level.playSound(null, throne, SmokelandsSounds.SULTAN_INHALE.get(), SoundSource.HOSTILE, 3f, 0.6f);
        sultan.arenaPlayers().forEach(player -> player.displayClientMessage(Component.translatable("quasium.sultan.rises"), true));
        return Optional.of(sultan);
    }

    public static boolean entitiesLoadedAround(ServerLevel level, BlockPos throne) {
        ChunkPos centre = new ChunkPos(throne);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!level.areEntitiesLoaded(ChunkPos.asLong(centre.x + x, centre.z + z))) return false;
            }
        }
        return true;
    }

    public void defeated(BlockPos home, long now) {
        Entry entry = arenas.computeIfAbsent(home.immutable(), pos -> new Entry());
        entry.defeatedAt = now;
        entry.called = false;
        entry.vows = 0;
        entry.sultan = null;
        setDirty();
    }

    public void forget(BlockPos home) {
        Entry entry = arenas.get(home);
        if (entry == null) return;
        entry.sultan = null;
        setDirty();
    }

    public Optional<BlockPos> nearestThrone(BlockPos pos) {
        double radius = SmokelandsConfig.ARENA_RADIUS.get();
        return arenas.keySet().stream().filter(throne -> throne.distSqr(pos) <= radius * radius)
                .min(Comparator.comparingDouble(throne -> throne.distSqr(pos)));
    }

    public long ticksUntilRespawn(BlockPos throne, long now) {
        Entry entry = arenas.get(throne);
        if (entry == null || entry.defeatedAt < 0) return 0;
        return Math.max(0, entry.defeatedAt + SmokelandsConfig.RESPAWN_DAYS.get() * DAY_TICKS - now);
    }

    public boolean defeated(BlockPos throne) {
        Entry entry = arenas.get(throne);
        return entry != null && entry.defeatedAt >= 0;
    }

    public Call call(BlockPos ritual, long now, Set<SultanVows.Vow> vows) {
        Optional<BlockPos> throne = nearestThrone(ritual);
        if (throne.isEmpty()) return Call.NO_ARENA;
        Entry entry = arenas.get(throne.get());
        if (entry.defeatedAt < 0) return Call.ALIVE;
        if (ticksUntilRespawn(throne.get(), now) > 0) return Call.TOO_EARLY;
        entry.called = true;
        entry.vows |= SultanVows.mask(vows);
        setDirty();
        return Call.CALLED;
    }

    private static SultanArena load(CompoundTag tag, HolderLookup.Provider registries) {
        SultanArena state = new SultanArena();
        for (Tag element : tag.getList("arenas", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) element;
            NbtUtils.readBlockPos(saved, "throne").ifPresent(throne -> {
                Entry entry = new Entry();
                entry.defeatedAt = saved.getLong("defeatedAt");
                entry.called = saved.getBoolean("called");
                entry.vows = saved.getInt("vows");
                if (saved.hasUUID("sultan")) entry.sultan = saved.getUUID("sultan");
                state.arenas.put(throne, entry);
            });
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        arenas.forEach((throne, entry) -> {
            CompoundTag saved = new CompoundTag();
            saved.put("throne", NbtUtils.writeBlockPos(throne));
            saved.putLong("defeatedAt", entry.defeatedAt);
            saved.putBoolean("called", entry.called);
            saved.putInt("vows", entry.vows);
            if (entry.sultan != null) saved.putUUID("sultan", entry.sultan);
            list.add(saved);
        });
        tag.put("arenas", list);
        return tag;
    }
}
