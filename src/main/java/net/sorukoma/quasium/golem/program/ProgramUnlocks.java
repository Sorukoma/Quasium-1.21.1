package net.sorukoma.quasium.golem.program;

import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.mind.research.Research;

public final class ProgramUnlocks {
    private ProgramUnlocks() {
    }

    public static boolean mayUse(Player player, BlockGroup group) {
        return group.research() == null || player.getAbilities().instabuild || Research.studied(player, group.research());
    }

    public static boolean mayUse(Player player, BlockKind kind) {
        return mayUse(player, kind.group());
    }

    public static boolean mayUse(Player player, Check check) {
        return mayUse(player, check.group());
    }
}
