package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.PlayerZones;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.ritual.RitualRecipe;
import net.sorukoma.quasium.ritual.RitualRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RitualChecklist {
    public static final int PLATFORM = 9;

    public record Check(boolean ok, Component line, @Nullable Component refusal) {
        static Check of(boolean ok, Component line, Component refusal) {
            return new Check(ok, line, ok ? null : refusal);
        }
    }

    public record Result(List<Check> checks, Optional<RitualRecipes.Match> match, Optional<ResourceLocation> ritual, boolean hasCatalyst) {
        public boolean ready() {
            return match.isPresent() && checks.stream().allMatch(Check::ok);
        }

        public Optional<Component> refusal() {
            return checks.stream().filter(check -> !check.ok()).map(Check::refusal).findFirst();
        }
    }

    private RitualChecklist() {
    }

    public static Result of(ServerLevel level, RitualBrazierBlockEntity brazier, @Nullable ServerPlayer asker) {
        BlockPos pos = brazier.getBlockPos();
        List<Check> checks = new ArrayList<>();
        int missing = RitualSite.missingPlatformBlocks(level, pos);
        checks.add(Check.of(missing == 0, Component.translatable("quasium.ritual.report.platform", PLATFORM - missing, PLATFORM),
                Component.translatable("quasium.ritual.need_platform", missing)));
        ItemStack catalyst = brazier.catalyst();
        if (catalyst.isEmpty()) {
            checks.add(Check.of(false, Component.translatable("quasium.ritual.check.catalyst_empty"),
                    Component.translatable("quasium.ritual.need_catalyst")));
            return new Result(checks, Optional.empty(), Optional.empty(), false);
        }
        List<RitualPedestalBlockEntity> pedestals = RitualSite.pedestals(level, pos);
        List<ItemStack> offered = RitualSite.offered(pedestals);
        Optional<Map.Entry<ResourceLocation, RitualRecipe>> target = RitualRecipes.target(catalyst, offered);
        if (target.isEmpty()) {
            checks.add(Check.of(false, Component.translatable("quasium.ritual.check.catalyst_wrong", catalyst.getHoverName()),
                    Component.translatable("quasium.ritual.no_ritual")));
            return new Result(checks, Optional.empty(), Optional.empty(), true);
        }
        ResourceLocation ritual = target.get().getKey();
        RitualRecipe recipe = target.get().getValue();
        checks.add(new Check(true, Component.translatable("quasium.ritual.check.catalyst", catalyst.getHoverName(), RitualRun.displayName(ritual)),
                null));
        pedestals(checks, recipe, pedestals, offered);
        research(checks, recipe, asker);
        notes(checks, recipe, RitualSite.stock(RitualSite.jars(level, pos)));
        participants(checks, recipe, RitualSite.participants(level, pos));
        return new Result(checks, RitualRecipes.find(catalyst, offered), Optional.of(ritual), true);
    }

    private static void pedestals(List<Check> checks, RitualRecipe recipe, List<RitualPedestalBlockEntity> pedestals, List<ItemStack> offered) {
        List<Integer> missing = recipe.missingPedestals(offered);
        Component refusal = Component.translatable("quasium.ritual.need_pedestals",
                ComponentUtils.formatList(missing.stream().map(slot -> nameOf(recipe.pedestals().get(slot))).toList(), Component.literal(", ")));
        for (int slot = 0; slot < recipe.pedestals().size(); slot++) {
            checks.add(Check.of(!missing.contains(slot), Component.translatable("quasium.ritual.check.pedestal", nameOf(recipe.pedestals().get(slot))),
                    refusal));
        }
        long free = pedestals.stream().filter(pedestal -> pedestal.item().isEmpty()).count();
        if (free < missing.size()) {
            checks.add(Check.of(false, Component.translatable("quasium.ritual.check.free_pedestals", free, missing.size()), refusal));
        }
    }

    private static void research(List<Check> checks, RitualRecipe recipe, @Nullable ServerPlayer asker) {
        recipe.research().ifPresent(node -> {
            Component title = Research.title(node.toString());
            boolean known = asker != null && MindData.has(asker, MindData.RESEARCH, node.toString());
            checks.add(Check.of(known, Component.translatable("quasium.ritual.check.research", title),
                    Component.translatable("quasium.ritual.need_research", title)));
        });
    }

    private static void notes(List<Check> checks, RitualRecipe recipe, Map<NoteType, Integer> stock) {
        recipe.notes().forEach((note, need) -> {
            int have = stock.getOrDefault(note, 0);
            checks.add(Check.of(have >= need, Component.translatable("quasium.ritual.report.note", note.displayName(), Math.min(have, need), need),
                    Component.translatable("quasium.ritual.need_notes", need - have, note.displayName())));
        });
    }

    private static void participants(List<Check> checks, RitualRecipe recipe, List<ServerPlayer> participants) {
        float kaif = RitualSite.averageKaif(participants);
        checks.add(Check.of(recipe.kaif().allows(kaif), kaifLine(recipe.kaif(), Math.round(kaif)), kaifGate(recipe.kaif(), Math.round(kaif))));
        if (!recipe.needsNirvana()) return;
        boolean inside = participants.stream().anyMatch(member -> PlayerZones.tier(member) >= PlayerZones.NIRVANA);
        checks.add(Check.of(inside, Component.translatable("quasium.ritual.check.nirvana"),
                Component.translatable("quasium.ritual.need_nirvana", recipe.tier())));
    }

    private static Component kaifLine(RitualRecipe.KaifGate gate, int kaif) {
        if (gate.min().isEmpty() && gate.orBelow().isEmpty()) return Component.translatable("quasium.ritual.check.kaif_any", kaif);
        if (gate.orBelow().isPresent()) {
            return Component.translatable("quasium.ritual.check.kaif_or_clear", kaif, gate.min().orElse(0), gate.orBelow().get());
        }
        return Component.translatable("quasium.ritual.check.kaif", kaif, gate.min().orElse(0));
    }

    private static Component kaifGate(RitualRecipe.KaifGate gate, int kaif) {
        if (gate.orBelow().isPresent()) {
            return Component.translatable("quasium.ritual.need_kaif_or_clear", gate.min().orElse(0), gate.orBelow().get(), kaif);
        }
        return Component.translatable("quasium.ritual.need_kaif", gate.min().orElse(0), kaif);
    }

    static Component nameOf(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? Component.literal("?") : options[0].getHoverName();
    }
}
