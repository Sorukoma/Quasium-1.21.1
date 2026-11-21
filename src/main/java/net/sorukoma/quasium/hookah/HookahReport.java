package net.sorukoma.quasium.hookah;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.block.entity.ModularHookahBlockEntity;
import net.sorukoma.quasium.magic.KaifZones;
import net.sorukoma.quasium.util.HookahLogic;

import java.util.List;
import java.util.Locale;

public final class HookahReport {
    private HookahReport() {
    }

    public static List<Component> of(ServerLevel level, HookahBlockEntity hookah) {
        HookahLogic.Mood mood = hookah.mood();
        float threshold = hookah.heatThreshold();
        int smokers = mood == HookahLogic.Mood.COLD ? 0 : hookah.zone().smokers(level.getGameTime());
        int golems = mood == HookahLogic.Mood.COLD ? 0 : hookah.zone().golems(level.getGameTime());
        boolean modular = hookah instanceof ModularHookahBlockEntity;
        int furniture = KaifZones.countFurniture(level, hookah.getBlockPos(), Config.ZONE_RADIUS_T3.getAsInt());
        int tier = KaifZones.tierFor(smokers, modular, furniture);
        return List.of(
                Component.translatable("quasium.scan.hookah.state", moodName(mood, hookah.getTotalTobaccoWeight(), hookah.warmupPuffsRemaining())).withStyle(ChatFormatting.GOLD),
                Component.translatable("quasium.scan.hookah.heat", number(hookah.getTotalHeat()),
                        number(HookahLogic.drawHeat(threshold, hookah.hasCooler())), number(HookahLogic.overheatLine(threshold))),
                Component.translatable("quasium.scan.hookah.tobacco",
                        hookah.getTotalTobaccoWeight() / HookahBlockEntity.TOBACCO_TICKS_PER_GRAM),
                zoneLine(tier, smokers, golems),
                nextLine(mood, tier, smokers, furniture).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent moodName(HookahLogic.Mood mood, int tobacco, int warmupRemaining) {
        return switch (mood) {
            case COLD -> Component.translatable(tobacco > 0 ? "quasium.gui.hookah.mood.cold" : "quasium.gui.hookah.mood.empty");
            case WARMUP -> Component.translatable("quasium.gui.hookah.mood.warmup",
                    HookahLogic.WARMUP_PUFFS - warmupRemaining, HookahLogic.WARMUP_PUFFS);
            case PEAK -> Component.translatable("quasium.gui.hookah.mood.peak");
            case TAIL -> Component.translatable("quasium.gui.hookah.mood.tail");
            case OVERHEAT -> Component.translatable("quasium.gui.hookah.mood.overheat");
        };
    }

    private static Component zoneLine(int tier, int smokers, int golems) {
        if (tier == 0) return Component.translatable("quasium.scan.hookah.no_zone");
        Object who = golems == 0 ? smokers : Component.translatable(golems == 1 ? "quasium.scan.hookah.smokers_golem"
                : "quasium.scan.hookah.smokers_golems", smokers - golems, golems);
        return Component.translatable("quasium.scan.hookah.zone", Component.translatable("quasium.zone.tier." + tier),
                KaifZones.radiusFor(tier), who, Config.ZONE_SMOKER_MEMORY_SEC.getAsInt());
    }

    private static MutableComponent nextLine(HookahLogic.Mood mood, int tier, int smokers, int furniture) {
        if (mood == HookahLogic.Mood.COLD) return Component.translatable("quasium.scan.hookah.next_cold");
        return switch (tier) {
            case 0 -> Component.translatable("quasium.scan.hookah.next_smoke");
            case 1 -> Component.translatable("quasium.scan.hookah.next_haze");
            case 2 -> Component.translatable("quasium.scan.hookah.next_nirvana", smokers, KaifZones.NIRVANA_SMOKERS,
                    furniture, Config.NIRVANA_FURNITURE.getAsInt());
            default -> Component.translatable("quasium.scan.hookah.next_max");
        };
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
