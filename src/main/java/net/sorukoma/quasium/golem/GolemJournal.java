package net.sorukoma.quasium.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public final class GolemJournal {
    public static final int SIZE = 20;
    private static final Codec<List<Entry>> LIST_CODEC = Entry.CODEC.listOf();

    public record Entry(long time, String key, List<String> args, int repeats) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("time").forGetter(Entry::time),
                Codec.STRING.fieldOf("key").forGetter(Entry::key),
                Codec.STRING.listOf().fieldOf("args").forGetter(Entry::args),
                Codec.INT.fieldOf("repeats").forGetter(Entry::repeats)).apply(instance, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Entry::time,
                ByteBufCodecs.STRING_UTF8, Entry::key,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Entry::args,
                ByteBufCodecs.VAR_INT, Entry::repeats,
                Entry::new);
    }

    private final Deque<Entry> entries = new ArrayDeque<>();
    @Nullable
    private String waiting;

    public void add(long time, String key, Object... args) {
        waiting = null;
        List<String> text = Arrays.stream(args).map(arg -> arg instanceof BlockPos pos ? pos.toShortString() : String.valueOf(arg)).toList();
        Entry newest = entries.peekFirst();
        if (newest != null && newest.key().equals(key) && newest.args().equals(text)) {
            entries.removeFirst();
            entries.addFirst(new Entry(time, key, text, newest.repeats() + 1));
            return;
        }
        entries.addFirst(new Entry(time, key, text, 1));
        if (entries.size() > SIZE) entries.removeLast();
    }

    void waitFor(long time, String key) {
        if (key.equals(waiting)) return;
        add(time, key);
        waiting = key;
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    void save(CompoundTag tag) {
        LIST_CODEC.encodeStart(NbtOps.INSTANCE, entries()).ifSuccess(list -> tag.put("Journal", list));
    }

    void load(CompoundTag tag) {
        entries.clear();
        if (!tag.contains("Journal")) return;
        LIST_CODEC.parse(NbtOps.INSTANCE, tag.get("Journal")).ifSuccess(list -> list.stream().limit(SIZE).forEach(entries::addLast));
    }
}
