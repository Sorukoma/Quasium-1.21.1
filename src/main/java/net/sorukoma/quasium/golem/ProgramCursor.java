package net.sorukoma.quasium.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.sorukoma.quasium.golem.program.Program;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramPath;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class ProgramCursor {
    private static final ProgramPath START = ProgramPath.top(0);
    static final Codec<ProgramCursor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("table").forGetter(cursor -> cursor.table),
            ProgramPath.CODEC.optionalFieldOf("path", START).forGetter(cursor -> cursor.path),
            Frame.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(cursor -> cursor.frames)
    ).apply(instance, ProgramCursor::new));

    @Nullable
    BlockPos table;
    int edits = -1;
    long searchAt;
    private ProgramPath path = START;
    private final List<Frame> frames = new ArrayList<>();

    static final class Frame {
        static final Codec<Frame> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("count").forGetter(frame -> frame.count),
                BlockPos.CODEC.optionalFieldOf("origin", BlockPos.ZERO).forGetter(frame -> frame.origin)
        ).apply(instance, Frame::new));

        int count;
        final BlockPos origin;
        boolean busy;
        boolean worked;
        @Nullable
        String missed;

        Frame(int count, BlockPos origin) {
            this.count = count;
            this.origin = origin.immutable();
        }

        Frame() {
            this(0, BlockPos.ZERO);
        }
    }

    ProgramCursor() {
    }

    private ProgramCursor(BlockPos table, ProgramPath path, List<Frame> frames) {
        this.table = table;
        this.path = path;
        this.frames.addAll(frames);
    }

    static ProgramCursor of(WorkerGolem golem) {
        return golem.getData(SmokeMarks.PROGRAM_CURSOR);
    }

    boolean worthSaving() {
        return table != null;
    }

    void restart(BlockPos at) {
        table = at.immutable();
        top();
    }

    void top() {
        path = START;
        frames.clear();
    }

    ProgramPath path() {
        return path;
    }

    int depth() {
        return frames.size();
    }

    List<Frame> frames() {
        return frames;
    }

    Frame frame(int depth) {
        return frames.get(depth - 1);
    }

    ProgramPath bracket(int depth) {
        return new ProgramPath(path.steps().subList(0, depth * 2 - 1));
    }

    boolean fits(Program program) {
        if (!path.valid() || frames.size() != path.enclosing()) return false;
        List<ProgramBlock> list = list(program);
        return list != null && path.index() <= list.size();
    }

    @Nullable
    List<ProgramBlock> list(Program program) {
        List<Integer> steps = path.steps();
        List<ProgramBlock> list = program.blocks();
        for (int at = 0; at < steps.size() - 1; at += 2) {
            int index = steps.get(at);
            int body = steps.get(at + 1);
            if (index >= list.size() || body >= list.get(index).kind().bodies()) return null;
            list = list.get(index).bodyOf(body);
        }
        return list;
    }

    void next() {
        path = after(path);
    }

    void enter(int body, Frame frame) {
        path = path.child(body, 0);
        frames.add(frame);
    }

    void loop() {
        path = bracket(depth()).child(0, 0);
    }

    void leave() {
        leave(depth());
    }

    void leave(int depth) {
        path = after(bracket(depth));
        frames.subList(depth - 1, frames.size()).clear();
    }

    private static ProgramPath after(ProgramPath at) {
        List<Integer> steps = new ArrayList<>(at.steps());
        steps.set(steps.size() - 1, at.index() + 1);
        return new ProgramPath(steps);
    }
}
