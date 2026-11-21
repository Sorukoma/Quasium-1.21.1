package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.ritual.RitualConfig;

import java.util.Locale;
import java.util.Set;

final class RitualCues {
    private RitualCues() {
    }

    static void beat(ServerPlayer player, RitualRhythm.Beat beat) {
        Component message = switch (beat.kind()) {
            case HIT -> Component.translatable("quasium.ritual.beat.hit", beat.streak(),
                    String.format(Locale.ROOT, "%.1f", RitualRun.multiplier(beat.streak()))).withStyle(ChatFormatting.LIGHT_PURPLE);
            case MISS -> Component.translatable("quasium.ritual.beat.miss").withStyle(ChatFormatting.GOLD);
            case REPEAT, PAUSED -> null;
        };
        if (message != null) player.displayClientMessage(message, true);
    }

    static void starving(RitualRun run, Set<NoteType> dry) {
        Component notes = ComponentUtils.formatList(dry.stream().map(NoteType::displayName).toList(), Component.literal(", "));
        Component message = Component.translatable("quasium.ritual.starving", notes).withStyle(ChatFormatting.RED);
        run.participants().forEach(player -> player.displayClientMessage(message, true));
    }

    static void groupFormed(RitualRun run) {
        QuasiumMod.LOGGER.info("Ritual {}: group bonus, {} players on the beat, base pressure -{}", run.id(), run.puffers().size(),
                RitualConfig.GROUP_BONUS.getAsInt());
        Component message = Component.translatable("quasium.ritual.group", run.puffers().size(), RitualConfig.GROUP_BONUS.getAsInt())
                .withStyle(ChatFormatting.AQUA);
        run.participants().forEach(player -> player.sendSystemMessage(message));
    }
}
