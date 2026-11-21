package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.golem.ProgramCursor.Frame;
import net.sorukoma.quasium.golem.ProgramHands.Result;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.MarkedPlace;
import net.sorukoma.quasium.golem.program.Program;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramPath;
import net.sorukoma.quasium.golem.program.ProgramTableBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ProgramFlow {
    private static final int FAIL_TICKS = 40;
    private static final int IDLE_TICKS = 100;

    private final ClayGolemEntity golem;
    private final ProgramAreas areas = new ProgramAreas();
    private ProgramTableBlockEntity table;
    private Program program = Program.EMPTY;
    private ProgramCursor cursor;
    private boolean lapBusy;
    private boolean rested;
    private long restUntil;
    private int done;
    private int scanLeft;

    private record Target(BlockPos pos, Direction face, boolean inArea) {
    }

    ProgramFlow(ClayGolemEntity golem) {
        this.golem = golem;
    }

    boolean awake() {
        return golem.level().getGameTime() >= restUntil;
    }

    boolean wake() {
        boolean was = rested;
        rested = false;
        return was;
    }

    @Nullable
    ProgramTask next(ProgramTableBlockEntity table) {
        follow(table);
        int budget = Config.PROGRAM_LOGIC_PER_TICK.getAsInt();
        scanLeft = Config.PROGRAM_AREA_SCAN_PER_TICK.getAsInt();
        for (int spent = 0; spent < budget && scanLeft > 0 && awake(); spent++) {
            List<ProgramBlock> list = cursor.list(program);
            int index = cursor.path().index();
            if (index >= list.size()) {
                closeBody();
                continue;
            }
            table.running(golem.getUUID(), preorder(program, cursor.path()));
            ProgramTask task = visit(list.get(index));
            if (task != null) {
                busy();
                return task;
            }
        }
        return null;
    }

    private void follow(ProgramTableBlockEntity table) {
        this.table = table;
        program = table.program();
        cursor = ProgramCursor.of(golem);
        if (cursor.edits != table.edits()) {
            if (cursor.edits >= 0) cursor.restart(table.getBlockPos());
            cursor.edits = table.edits();
        }
        if (!cursor.fits(program)) cursor.restart(table.getBlockPos());
    }

    @Nullable
    private ProgramTask visit(ProgramBlock block) {
        if (block.kind().bodies() == 0) return block.kind() == BlockKind.WAIT_SIGNAL ? listen(block) : act(block);
        switch (block.kind()) {
            case FOR_EACH -> enterArea(block);
            case REPEAT, FOREVER -> cursor.enter(0, new Frame());
            default -> branch(block);
        }
        return null;
    }

    @Nullable
    private ProgramTask act(ProgramBlock block) {
        Target where = block.usesA() ? target(block) : here();
        if (where == null) {
            cursor.next();
            return null;
        }
        if (where.inArea() && opensAreaBody()) return sweep(block);
        ProgramTask task = new ProgramTask(block, where.pos(), where.face(), where.inArea());
        return outcome(task, tried(task));
    }

    private boolean opensAreaBody() {
        return cursor.path().index() == 0 && areaDepth() == cursor.depth();
    }

    @Nullable
    private ProgramTask sweep(ProgramBlock action) {
        int depth = cursor.depth();
        ProgramBlock area = program.at(cursor.bracket(depth));
        Frame frame = cursor.frame(depth);
        List<BlockPos> blocks = areas.order(area, frame.origin);
        Direction face = area.a().orElseThrow().face();
        for (; scanLeft > 0; scanLeft--) {
            if (frame.count >= blocks.size()) {
                closeArea(frame);
                return null;
            }
            ProgramTask task = new ProgramTask(action, blocks.get(frame.count), face, true);
            Result result = tried(task);
            if (result != Result.SKIP && result != Result.MISSED) return outcome(task, result);
            if (result == Result.MISSED) missed(task);
            frame.count++;
        }
        return null;
    }

    private Result tried(ProgramTask task) {
        if (golem.level().isLoaded(task.place())) return ProgramHands.use(golem, task, true);
        golem.note("program_unreachable", task.place());
        return Result.FAIL;
    }

    @Nullable
    private ProgramTask outcome(ProgramTask task, Result result) {
        if (result == Result.DONE) return task;
        finished(task, result);
        return null;
    }

    @Nullable
    private ProgramTask listen(ProgramBlock block) {
        Target where = target(block);
        if (where == null || ProgramChecks.powered(golem.level(), where.pos())) {
            cursor.next();
            return null;
        }
        return new ProgramTask(block, where.pos(), where.face(), where.inArea());
    }

    private void branch(ProgramBlock block) {
        Target where = checked(block);
        if (where == null) cursor.next();
        else if (holds(block, where)) cursor.enter(0, new Frame());
        else if (block.kind() == BlockKind.IF_ELSE) cursor.enter(1, new Frame());
        else cursor.next();
    }

    @Nullable
    private Target checked(ProgramBlock block) {
        return block.check().usesA() ? target(block) : here();
    }

    private boolean holds(ProgramBlock block, Target where) {
        return ProgramChecks.holds(golem, block.check(), where.pos(), where.face(), block.filter());
    }

    private void enterArea(ProgramBlock block) {
        MarkedPlace from = block.a().filter(table::reaches).orElse(null);
        MarkedPlace to = block.b().filter(table::reaches).orElse(null);
        if (from == null || to == null) {
            cursor.next();
        } else if (block.body().isEmpty()) {
            skip("program_area_empty");
        } else if (!ZoneBox.fits(from.where().pos(), to.where().pos())) {
            skip("program_area_big", ZoneBox.MAX_SPAN, ZoneBox.MAX_HEIGHT, ZoneBox.MAX_SPAN);
        } else if (!loaded(block)) {
            skip("program_unreachable", from.where().pos());
        } else {
            cursor.enter(0, new Frame(0, golem.blockPosition()));
        }
    }

    private boolean loaded(ProgramBlock area) {
        BoundingBox box = BoundingBox.fromCorners(area.a().orElseThrow().where().pos(), area.b().orElseThrow().where().pos());
        return ZoneBox.loaded(golem.level(), box);
    }

    private void closeBody() {
        int depth = cursor.depth();
        if (depth == 0) {
            lap();
            return;
        }
        ProgramBlock bracket = program.at(cursor.bracket(depth));
        Frame frame = cursor.frame(depth);
        switch (bracket.kind()) {
            case REPEAT -> {
                settle();
                if (++frame.count < bracket.number()) cursor.loop();
                else cursor.leave();
            }
            case FOREVER -> {
                pass(frame);
                cursor.loop();
            }
            case WHILE -> {
                pass(frame);
                Target where = checked(bracket);
                if (where != null && holds(bracket, where)) cursor.loop();
                else cursor.leave();
            }
            case FOR_EACH -> nextAreaBlock(bracket, frame);
            default -> cursor.leave();
        }
    }

    private void nextAreaBlock(ProgramBlock area, Frame frame) {
        if (++frame.count >= areas.order(area, frame.origin).size()) {
            closeArea(frame);
        } else if (!loaded(area)) {
            golem.note("program_unreachable", area.a().orElseThrow().where().pos());
            busy();
            rest(FAIL_TICKS);
            cursor.leave();
        } else {
            cursor.loop();
        }
    }

    private void closeArea(Frame frame) {
        if (frame.missed != null && !frame.worked) golem.note("program_no_tool", frame.missed);
        cursor.leave();
    }

    private void lap() {
        settle();
        if (!lapBusy) rest(IDLE_TICKS);
        lapBusy = false;
        cursor.top();
    }

    private void pass(Frame frame) {
        settle();
        if (!frame.busy) rest(IDLE_TICKS);
        frame.busy = false;
    }

    void finished(ProgramTask task, Result result) {
        if (result == Result.MISSED) missed(task);
        if (result == Result.FAIL || result == Result.STOP) {
            busy();
            rest(FAIL_TICKS);
        }
        if (result != Result.STOP || !leaveArea()) cursor.next();
    }

    void worked(ProgramTask task) {
        done++;
        Frame area = task.inArea() ? areaFrame() : null;
        if (area != null) area.worked = true;
    }

    private void missed(ProgramTask task) {
        Frame area = areaFrame();
        if (area != null) area.missed = golem.level().getBlockState(task.place()).getBlock().getDescriptionId();
    }

    private boolean leaveArea() {
        int depth = areaDepth();
        if (depth == 0) return false;
        cursor.leave(depth);
        return true;
    }

    @Nullable
    private Frame areaFrame() {
        int depth = areaDepth();
        return depth == 0 ? null : cursor.frame(depth);
    }

    private int areaDepth() {
        for (int depth = cursor.depth(); depth > 0; depth--) {
            if (program.at(cursor.bracket(depth)).kind() == BlockKind.FOR_EACH) return depth;
        }
        return 0;
    }

    @Nullable
    private Target target(ProgramBlock block) {
        MarkedPlace place = block.a().orElse(null);
        if (place != null) return table.reaches(place) ? new Target(place.where().pos(), place.face(), false) : null;
        int depth = areaDepth();
        if (depth == 0) return null;
        ProgramBlock area = program.at(cursor.bracket(depth));
        Frame frame = cursor.frame(depth);
        List<BlockPos> blocks = areas.order(area, frame.origin);
        if (frame.count >= blocks.size()) return null;
        return new Target(blocks.get(frame.count), area.a().orElseThrow().face(), true);
    }

    private Target here() {
        return new Target(golem.blockPosition(), Direction.UP, false);
    }

    private void skip(String event, Object... args) {
        golem.note(event, args);
        busy();
        rest(FAIL_TICKS);
        cursor.next();
    }

    private void busy() {
        lapBusy = true;
        for (Frame frame : cursor.frames()) frame.busy = true;
    }

    private void rest(int ticks) {
        restUntil = golem.level().getGameTime() + ticks;
        rested = true;
    }

    void settle() {
        if (done > 0) golem.endHarvestTrip();
        done = 0;
    }

    static int preorder(Program program, ProgramPath path) {
        List<ProgramBlock> list = program.blocks();
        List<Integer> steps = path.steps();
        int index = 0;
        for (int at = 0; ; at += 2) {
            int position = steps.get(at);
            for (int before = 0; before < position; before++) index += list.get(before).size();
            if (at == steps.size() - 1) return index;
            ProgramBlock bracket = list.get(position);
            int body = steps.get(at + 1);
            index++;
            if (body == 1) {
                for (ProgramBlock inner : bracket.body()) index += inner.size();
            }
            list = bracket.bodyOf(body);
        }
    }
}
