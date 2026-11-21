package net.sorukoma.quasium.golem;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.sorukoma.quasium.QuasiumMod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class SmokeMarks {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuasiumMod.MOD_ID);
    public static final Supplier<AttachmentType<List<Long>>> MARKS = ATTACHMENTS.register("smoke_marks",
            () -> AttachmentType.<List<Long>>builder(() -> new ArrayList<>()).serialize(Codec.LONG.listOf()).build());
    static final Supplier<AttachmentType<ProgramCursor>> PROGRAM_CURSOR = ATTACHMENTS.register("program_cursor",
            () -> AttachmentType.builder(() -> new ProgramCursor()).serialize(ProgramCursor.CODEC, ProgramCursor::worthSaving).build());
    static final Supplier<AttachmentType<GolemSmoke>> SMOKE = ATTACHMENTS.register("golem_smoke",
            () -> AttachmentType.builder(() -> new GolemSmoke()).build());
    public static final Supplier<AttachmentType<GolemNap>> NAP = ATTACHMENTS.register("golem_nap",
            () -> AttachmentType.builder(() -> new GolemNap()).serialize(GolemNap.CODEC, GolemNap::worthSaving).build());
    public static final Supplier<AttachmentType<GolemPipe>> PIPE = ATTACHMENTS.register("golem_pipe",
            () -> AttachmentType.builder(() -> new GolemPipe(BlockPos.ZERO, 0L, 0L)).sync(GolemPipe.STREAM_CODEC).build());

    private SmokeMarks() {
    }

    public static int count(Level level, BlockPos pos) {
        return level.getChunkAt(pos).getData(MARKS).size();
    }

    static void add(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        List<Long> marks = new ArrayList<>(chunk.getData(MARKS));
        if (marks.contains(pos.asLong())) return;
        marks.add(pos.asLong());
        chunk.setData(MARKS, marks);
        chunk.setUnsaved(true);
    }

    static void remove(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        List<Long> marks = new ArrayList<>(chunk.getData(MARKS));
        if (!marks.remove(Long.valueOf(pos.asLong()))) return;
        chunk.setData(MARKS, marks);
        chunk.setUnsaved(true);
    }

    public static List<BlockPos> near(Level level, BlockPos center, int radius, MarkType type) {
        List<BlockPos> found = new ArrayList<>();
        ChunkPos from = new ChunkPos(center.offset(-radius, 0, -radius));
        ChunkPos to = new ChunkPos(center.offset(radius, 0, radius));
        double reach = (double) radius * radius;
        for (int x = from.x; x <= to.x; x++) {
            for (int z = from.z; z <= to.z; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (long packed : level.getChunk(x, z).getData(MARKS)) {
                    BlockPos mark = BlockPos.of(packed);
                    if (mark.distSqr(center) <= reach && level.getBlockState(mark).getBlock() instanceof SmokeMarkBlock block
                            && block.type() == type) {
                        found.add(mark);
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(mark -> mark.distSqr(center)));
        return found;
    }

    public static List<BlockPos> near(Level level, BlockPos center, int radius, MarkType type, UUID golem) {
        return near(level, center, radius, type).stream()
                .filter(mark -> level.getBlockEntity(mark) instanceof SmokeMarkBlockEntity entity && entity.golems().contains(golem)).toList();
    }

    static List<BlockPos> stores(Level level, BlockPos mark) {
        if (level.getBlockEntity(mark) instanceof SmokeMarkBlockEntity entity && entity.hasLinks()) return entity.liveLinks();
        return Arrays.stream(Direction.values()).map(mark::relative).toList();
    }

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}
