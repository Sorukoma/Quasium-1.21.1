package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualMath;
import net.sorukoma.quasium.smokelands.sultan.SultanEvents;
import net.sorukoma.quasium.smokelands.sultan.SultanVows;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RitualReport {
    private RitualReport() {
    }

    public static List<Component> of(ServerLevel level, RitualBrazierBlockEntity brazier) {
        return of(level, brazier, null);
    }

    public static List<Component> of(ServerLevel level, RitualBrazierBlockEntity brazier, @Nullable ServerPlayer asker) {
        RitualRun run = brazier.run();
        if (run == null) return checklist(level, brazier.getBlockPos(), RitualChecklist.of(level, brazier, asker));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("quasium.ritual.report.title").withStyle(ChatFormatting.GOLD));
        int missing = RitualSite.missingPlatformBlocks(level, brazier.getBlockPos());
        lines.add(Component.translatable("quasium.ritual.report.platform", RitualChecklist.PLATFORM - missing, RitualChecklist.PLATFORM)
                .withStyle(missing == 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        running(lines, run, brazier.hasFilter());
        if (SultanEvents.RESPAWN_RITUAL.equals(run.id())) lines.add(SultanVows.report(level, brazier.getBlockPos()));
        return lines;
    }

    public static List<Component> checklist(ServerLevel level, BlockPos brazier, RitualChecklist.Result result) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("quasium.ritual.check.title").withStyle(ChatFormatting.GOLD));
        for (RitualChecklist.Check check : result.checks()) {
            lines.add(Component.translatable(check.ok() ? "quasium.ritual.check.yes" : "quasium.ritual.check.no").append(" ").append(check.line())
                    .withStyle(check.ok() ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (result.ritual().filter(SultanEvents.RESPAWN_RITUAL::equals).isPresent()) lines.add(SultanVows.report(level, brazier));
        if (result.ready()) lines.add(Component.translatable("quasium.ritual.check.ready").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (result.hasCatalyst()) lines.add(Component.translatable("quasium.ritual.check.take").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static void running(List<Component> lines, RitualRun run, boolean filter) {
        lines.add(Component.translatable("quasium.ritual.report.running", RitualRun.displayName(run.id()), Math.round(run.progress() * 100)));
        lines.add(Component.translatable("quasium.ritual.report.instability", Math.round(run.instability()),
                RitualConfig.RITUAL_BREAK_THRESHOLD.getAsInt(), String.format(Locale.ROOT, "%.1f", run.drift())));
        lines.add(Component.translatable("quasium.ritual.report.rhythm", RitualMath.seconds(run.recipe().pulseMs()), run.recipe().windowMs()));
        lines.add(Component.translatable("quasium.ritual.report.draft", Math.round(run.draftRate()), run.recipe().rate()));
        lines.add(Component.translatable("quasium.ritual.report.kaif", Math.round(run.averageKaif()), run.participants().size()));
        if (run.group()) lines.add(Component.translatable("quasium.ritual.report.group", run.puffers().size()).withStyle(ChatFormatting.AQUA));
        if (filter) lines.add(Component.translatable("quasium.ritual.report.filter", RitualConfig.FILTER_BONUS.getAsInt()));
        if (run.starved()) lines.add(Component.translatable("quasium.ritual.report.starved").withStyle(ChatFormatting.RED));
    }
}
