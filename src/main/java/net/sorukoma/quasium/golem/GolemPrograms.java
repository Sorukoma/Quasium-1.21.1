package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.golem.program.ProgramTableBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GolemPrograms {
    private static final int SEARCH_TICKS = 20;

    private GolemPrograms() {
    }

    @Nullable
    public static BlockPos table(WorkerGolem golem) {
        ProgramTableBlockEntity program = program(golem);
        return program == null ? null : program.getBlockPos();
    }

    public static boolean running(WorkerGolem golem) {
        if (!(golem instanceof ClayGolemEntity)) return false;
        ProgramTableBlockEntity program = program(golem);
        return program != null && !program.program().isEmpty();
    }

    @Nullable
    static ProgramTableBlockEntity program(WorkerGolem golem) {
        ProgramCursor cursor = ProgramCursor.of(golem);
        ProgramTableBlockEntity known = cursor.table == null ? null : boundAt(golem, cursor.table);
        if (known != null) return sees(golem, known.getBlockPos()) ? known : null;
        long now = golem.level().getGameTime();
        if (now < cursor.searchAt) return null;
        cursor.searchAt = now + SEARCH_TICKS;
        for (BlockPos pos : bound(golem)) {
            ProgramTableBlockEntity found = boundAt(golem, pos);
            if (found == null) continue;
            if (cursor.table != null) {
                unbind(golem, found);
                continue;
            }
            cursor.restart(pos);
            return found;
        }
        return null;
    }

    static void adopt(WorkerGolem golem, SmokeMarkBlockEntity table) {
        ProgramCursor cursor = ProgramCursor.of(golem);
        List<BlockPos> old = new ArrayList<>(bound(golem));
        if (cursor.table != null) old.add(cursor.table);
        for (BlockPos pos : old) {
            ProgramTableBlockEntity other = pos.equals(table.getBlockPos()) ? null : boundAt(golem, pos);
            if (other != null) unbind(golem, other);
        }
        cursor.restart(table.getBlockPos());
    }

    private static void unbind(WorkerGolem golem, SmokeMarkBlockEntity table) {
        table.forget(golem.getUUID());
        if (table instanceof ProgramTableBlockEntity program) program.running(golem.getUUID(), -1);
        golem.note("unbound", MarkBlocks.item(MarkType.PROGRAM).getDescriptionId(), table.getBlockPos());
    }

    private static boolean sees(WorkerGolem golem, BlockPos table) {
        int reach = GolemBindings.reach(MarkType.PROGRAM);
        return table.distSqr(golem.blockPosition()) <= (double) reach * reach;
    }

    private static List<BlockPos> bound(WorkerGolem golem) {
        return SmokeMarks.near(golem.level(), golem.blockPosition(), GolemBindings.reach(MarkType.PROGRAM), MarkType.PROGRAM, golem.getUUID());
    }

    @Nullable
    private static ProgramTableBlockEntity boundAt(WorkerGolem golem, BlockPos pos) {
        Level level = golem.level();
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ProgramTableBlockEntity program
                && program.golems().contains(golem.getUUID()) ? program : null;
    }
}
