package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.Config;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class GolemBindings {
    private GolemBindings() {
    }

    public static int markRadius() {
        return Config.GOLEM_MARK_RADIUS.getAsInt();
    }

    static int shownRadius() {
        return markRadius() * 2;
    }

    public static int reach(MarkType type) {
        return type == MarkType.PROGRAM ? shownRadius() : markRadius();
    }

    static boolean refuse(Player player, @Nullable WorkerGolem golem, SmokeMarkBlockEntity mark) {
        if (golem == null) {
            player.displayClientMessage(Component.translatable("quasium.staff.golem_missing"), true);
            return true;
        }
        if (!golem.isOwnedBy(player)) return refuse(player, golem, mark, "quasium.golem.not_owner", "refused_owner");
        if (!mark.mayEdit(player)) return refuse(player, golem, mark, "quasium.staff.not_your_mark", "refused_mark");
        if (!golem.worksWith(mark.type())) return refuse(player, golem, mark, "quasium.staff.useless_pair", "refused_useless");
        return false;
    }

    private static boolean refuse(Player player, WorkerGolem golem, SmokeMarkBlockEntity mark, String message, String event) {
        player.displayClientMessage(Component.translatable(message, golem.getDisplayName()), true);
        golem.note(event, mark.getBlockPos());
        return true;
    }

    static boolean toggle(Player player, WorkerGolem golem, SmokeMarkBlockEntity mark) {
        boolean bound = mark.toggleGolem(player, golem.getUUID());
        golem.boundMarks.forget();
        golem.note(bound ? "bound" : "unbound", MarkBlocks.item(mark.type()).getDescriptionId(), mark.getBlockPos());
        if (bound && mark.type() == MarkType.PROGRAM) GolemPrograms.adopt(golem, mark);
        return bound;
    }

    public static boolean set(Player player, WorkerGolem golem, BlockPos pos, boolean bind) {
        Level level = golem.level();
        int reach = bind ? markRadius() : shownRadius();
        if (!level.isLoaded(pos) || pos.distSqr(golem.blockPosition()) > (double) reach * reach
                || !(level.getBlockEntity(pos) instanceof SmokeMarkBlockEntity mark)) return false;
        if (refuse(player, golem, mark) || mark.golems().contains(golem.getUUID()) == bind) return false;
        toggle(player, golem, mark);
        return true;
    }

    static void forgetAll(WorkerGolem golem) {
        for (MarkType type : MarkType.values()) {
            for (BlockPos pos : SmokeMarks.near(golem.level(), golem.blockPosition(), shownRadius(), type)) {
                if (golem.level().getBlockEntity(pos) instanceof SmokeMarkBlockEntity mark) mark.forget(golem.getUUID());
            }
        }
    }

    static List<GolemProfile.Spot> bound(WorkerGolem golem) {
        return spots(golem, shownRadius(), mark -> mark.golems().contains(golem.getUUID()));
    }

    static List<GolemProfile.Spot> bindable(WorkerGolem golem, Player player) {
        return spots(golem, markRadius(), mark -> mark.mayEdit(player) && !mark.golems().contains(golem.getUUID()));
    }

    private static List<GolemProfile.Spot> spots(WorkerGolem golem, int radius, Predicate<SmokeMarkBlockEntity> keep) {
        List<GolemProfile.Spot> found = new ArrayList<>();
        for (MarkType type : MarkType.values()) {
            if (!golem.worksWith(type)) continue;
            for (BlockPos pos : SmokeMarks.near(golem.level(), golem.blockPosition(), radius, type)) {
                if (golem.level().getBlockEntity(pos) instanceof SmokeMarkBlockEntity mark && keep.test(mark)) {
                    found.add(new GolemProfile.Spot(pos, type));
                }
            }
        }
        found.sort(Comparator.comparingDouble(spot -> spot.pos().distSqr(golem.blockPosition())));
        return found;
    }
}
