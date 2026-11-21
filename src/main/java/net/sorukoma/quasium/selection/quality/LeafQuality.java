package net.sorukoma.quasium.selection.quality;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.genetics.Genetics;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LeafQuality {
    private LeafQuality() {
    }

    public static int of(ItemStack stack) {
        return Genetics.clamp(stack.getOrDefault(SelectionRegistry.LEAF_QUALITY.get(), Genetics.MIN_STARS));
    }

    public static int atHarvest(Level level, BlockPos pos, BlockState state) {
        boolean exact = state.getBlock() instanceof TobaccoCropBlock crop && crop.isExactMature(level, pos);
        BlockState soil = level.getBlockState(pos.below());
        boolean wet = state.getValue(TobaccoCropBlock.WATERED)
                || soil.getBlock() instanceof FarmBlock && soil.getValue(FarmBlock.MOISTURE) == FarmBlock.MAX_MOISTURE;
        return QualityMath.atHarvest(exact, wet, workedHiveNear(level, pos));
    }

    public static void fermentedClean(ItemStack leaves) {
        leaves.set(SelectionRegistry.LEAF_QUALITY.get(), QualityMath.fermentedClean(of(leaves)));
    }

    public static int kaifPct(int stars) {
        return QualityMath.perStar(SelectionConfig.KAIF_MULTIPLIER_PCT.get(), stars);
    }

    public static int distillBonusMl(int stars) {
        return QualityMath.perStar(SelectionConfig.DISTILL_BONUS_ML.get(), stars);
    }

    public static Map<NoteType, Integer> noteYield(ItemStack stack, NoteProfile profile) {
        Map<NoteType, Integer> notes = new EnumMap<>(NoteType.class);
        int yieldMultiplier = TobaccoVarieties.isLeaf(stack) && TobaccoVarieties.PRIMORDIAL.equals(TobaccoVarieties.getVariety(stack)) ? 2 : 1;
        profile.notes().forEach((note, ml) -> notes.put(note, ml * yieldMultiplier));
        int bonus = distillBonusMl(of(stack));
        if (bonus <= 0) return notes;
        notes.entrySet().stream().max(Map.Entry.comparingByValue())
                .ifPresent(main -> notes.merge(main.getKey(), bonus, Integer::sum));
        return notes;
    }

    public static String stars(int value) {
        int filled = Math.max(0, Math.min(Genetics.MAX_STARS, value));
        return "\u2605".repeat(filled) + "\u2606".repeat(Genetics.MAX_STARS - filled);
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        if (!stack.has(SelectionRegistry.LEAF_QUALITY.get())) return;
        tooltip.add(Component.translatable("quasium.tooltip.leaf_quality", stars(of(stack))).withStyle(ChatFormatting.YELLOW));
    }

    private static boolean workedHiveNear(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return false;
        return server.getPoiManager().findAll(type -> type.is(PoiTypeTags.BEE_HOME),
                hive -> server.getBlockState(hive).getOptionalValue(BeehiveBlock.HONEY_LEVEL).orElse(0) > 0,
                pos, SelectionConfig.HIVE_RADIUS.get(), PoiManager.Occupancy.ANY).findAny().isPresent();
    }
}
