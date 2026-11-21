package net.sorukoma.quasium.ecology;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SmokeDensity {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuasiumMod.MOD_ID);
    public static final Supplier<AttachmentType<Integer>> DENSITY = ATTACHMENTS.register("smoke_density",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());
    public static final Supplier<AttachmentType<ChunkSmog>> SMOG = ATTACHMENTS.register("smog",
            () -> AttachmentType.builder(() -> ChunkSmog.CLEAR).serialize(ChunkSmog.CODEC).build());

    private SmokeDensity() {
    }

    public static int get(Level level, BlockPos pos) {
        return level.getChunkAt(pos).getData(DENSITY);
    }

    public static int add(Level level, BlockPos pos, int amount) {
        LevelChunk chunk = level.getChunkAt(pos);
        setDensity(chunk, chunk.getData(DENSITY) + amount);
        return chunk.getData(DENSITY);
    }

    public static void addAround(Level level, BlockPos pos, int radiusChunks, int amount) {
        forChunksAround(level, pos, radiusChunks, chunk -> setDensity(chunk, chunk.getData(DENSITY) + amount));
    }

    public static int density(LevelChunk chunk) {
        return chunk.getData(DENSITY);
    }

    public static ChunkSmog smog(LevelChunk chunk) {
        return chunk.getData(SMOG);
    }

    public static void setDensity(LevelChunk chunk, int wanted) {
        int before = chunk.getData(DENSITY);
        int after = Math.max(0, Math.min(Config.DENSITY_MAX.getAsInt(), wanted));
        if (after == before) return;
        chunk.setData(DENSITY, after);
        chunk.setUnsaved(true);
        EcologyIndex.touch(chunk);
        int haze = EcologyConfig.HAZE_THRESHOLD.getAsInt();
        if ((before >= haze) != (after >= haze)) EcologyNetwork.sendChunk(chunk);
    }

    public static void setSmog(LevelChunk chunk, ChunkSmog smog) {
        ChunkSmog before = chunk.getData(SMOG);
        if (smog.equals(before)) return;
        chunk.setData(SMOG, smog);
        chunk.setUnsaved(true);
        EcologyIndex.touch(chunk);
        if (smog.level() != before.level()) EcologyNetwork.sendChunk(chunk);
    }

    public static int addSmog(LevelChunk chunk, int amount) {
        ChunkSmog smog = chunk.getData(SMOG);
        setSmog(chunk, smog.withLevel(smog.level() + amount));
        return chunk.getData(SMOG).level();
    }

    public static void forChunksAround(Level level, BlockPos pos, int radiusChunks, Consumer<LevelChunk> action) {
        ChunkPos center = new ChunkPos(pos);
        for (int x = -radiusChunks; x <= radiusChunks; x++) {
            for (int z = -radiusChunks; z <= radiusChunks; z++) {
                if (level.hasChunk(center.x + x, center.z + z)) action.accept(level.getChunk(center.x + x, center.z + z));
            }
        }
    }

    public static Component describe(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        int smog = chunk.getData(SMOG).level();
        return smog > 0 ? Component.translatable("quasium.scan.density_smog", chunk.getData(DENSITY), smog, SmogMath.MAX_LEVEL)
                : Component.translatable("quasium.scan.density", chunk.getData(DENSITY));
    }

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}
