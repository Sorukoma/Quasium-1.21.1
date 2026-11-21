package net.sorukoma.quasium.mind.research;

import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.mind.MindData;

import java.util.Set;

public record MindView(Player player, int kaif, int insight, Set<String> researched, String school, Set<String> flags) {
    public static final String TEA = "tea";
    public static final String HOOKAH = "hookah";

    public static MindView of(Player player) {
        return new MindView(player, PlayerKaifData.getKaifValue(player), PlayerMagicData.getInsight(player),
                MindData.strings(player, MindData.RESEARCH), MindData.root(player).getString(MindData.SCHOOL),
                MindData.strings(player, MindData.FLAGS));
    }

    public boolean researched(String node) {
        return researched.contains(node);
    }

    public boolean flag(String flag) {
        return flags.contains(flag);
    }

    public boolean tea() {
        return TEA.equals(school);
    }

    public boolean hookah() {
        return HOOKAH.equals(school);
    }
}
