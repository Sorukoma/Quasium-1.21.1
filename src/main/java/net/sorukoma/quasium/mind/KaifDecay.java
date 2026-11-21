package net.sorukoma.quasium.mind;

import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.effect.ModEffects;
import net.sorukoma.quasium.mind.research.Perk;

public final class KaifDecay {
    public static final String CURSED_UNTIL = "CursedUntil";

    private KaifDecay() {
    }

    public static int percent(Player player) {
        if (player.hasEffect(ModEffects.SNACKED)) return 0;
        int percent = Perk.ZEN.ownedBy(player) ? 100 - MindConfig.ZEN_DECAY_PCT.getAsInt() : 100;
        return cursed(player) ? percent * MindConfig.GHOST_CURSE_MULTIPLIER.getAsInt() : percent;
    }

    public static boolean cursed(Player player) {
        return player.level().getGameTime() < MindData.time(player, CURSED_UNTIL);
    }
}
