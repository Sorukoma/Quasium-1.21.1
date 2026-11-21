package net.sorukoma.quasium.golem.program;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public record Program(List<ProgramBlock> blocks) {
    public static final Program EMPTY = new Program(List.of());
    public static final Codec<Program> CODEC = ProgramBlock.CODEC.listOf().xmap(Program::new, Program::blocks);
    public static final StreamCodec<RegistryFriendlyByteBuf, Program> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public Program {
        blocks = List.copyOf(blocks);
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public int size() {
        return blocks.stream().mapToInt(ProgramBlock::size).sum();
    }

    public int nesting() {
        return blocks.stream().mapToInt(ProgramBlock::nesting).max().orElse(0);
    }

    @Nullable
    public ProgramBlock at(ProgramPath path) {
        if (!path.valid()) return null;
        List<ProgramBlock> list = blocks;
        List<Integer> steps = path.steps();
        for (int at = 0; ; at += 2) {
            int index = steps.get(at);
            if (index >= list.size()) return null;
            ProgramBlock block = list.get(index);
            if (at == steps.size() - 1) return block;
            int body = steps.get(at + 1);
            if (body >= block.kind().bodies()) return null;
            list = block.bodyOf(body);
        }
    }

    public Optional<Program> insert(ProgramPath spot, ProgramBlock block) {
        return edit(spot, list -> {
            if (spot.index() > list.size()) return null;
            List<ProgramBlock> grown = new ArrayList<>(list);
            grown.add(spot.index(), block);
            return grown;
        });
    }

    public Optional<Program> remove(ProgramPath path) {
        return edit(path, list -> {
            if (path.index() >= list.size()) return null;
            List<ProgramBlock> shrunk = new ArrayList<>(list);
            shrunk.remove(path.index());
            return shrunk;
        });
    }

    public Optional<Program> replace(ProgramPath path, UnaryOperator<ProgramBlock> change) {
        return edit(path, list -> {
            if (path.index() >= list.size()) return null;
            List<ProgramBlock> changed = new ArrayList<>(list);
            changed.set(path.index(), change.apply(list.get(path.index())));
            return changed;
        });
    }

    public Optional<Program> move(ProgramPath from, ProgramPath spot) {
        ProgramBlock moving = at(from);
        if (moving == null || !spot.valid() || spot.inside(from)) return Optional.empty();
        ProgramPath target = spot.afterRemoving(from);
        return remove(from).flatMap(without -> without.insert(target, moving));
    }

    public List<ProgramPath> paths() {
        List<ProgramPath> paths = new ArrayList<>();
        collect(blocks, List.of(), paths);
        return paths;
    }

    private static void collect(List<ProgramBlock> list, List<Integer> prefix, List<ProgramPath> paths) {
        for (int index = 0; index < list.size(); index++) {
            List<Integer> steps = new ArrayList<>(prefix);
            steps.add(index);
            paths.add(new ProgramPath(steps));
            ProgramBlock block = list.get(index);
            for (int body = 0; body < block.kind().bodies(); body++) {
                List<Integer> inner = new ArrayList<>(steps);
                inner.add(body);
                collect(block.bodyOf(body), inner, paths);
            }
        }
    }

    private Optional<Program> edit(ProgramPath path, UnaryOperator<List<ProgramBlock>> change) {
        if (!path.valid()) return Optional.empty();
        List<ProgramBlock> edited = editList(blocks, path.steps(), 0, change);
        return Optional.ofNullable(edited).map(Program::new);
    }

    @Nullable
    private static List<ProgramBlock> editList(List<ProgramBlock> list, List<Integer> steps, int at, UnaryOperator<List<ProgramBlock>> change) {
        if (at == steps.size() - 1) return change.apply(list);
        int index = steps.get(at);
        int body = steps.get(at + 1);
        if (index >= list.size() || body >= list.get(index).kind().bodies()) return null;
        ProgramBlock block = list.get(index);
        List<ProgramBlock> inner = editList(block.bodyOf(body), steps, at + 2, change);
        if (inner == null) return null;
        List<ProgramBlock> changed = new ArrayList<>(list);
        changed.set(index, block.withBody(body, inner));
        return changed;
    }
}
