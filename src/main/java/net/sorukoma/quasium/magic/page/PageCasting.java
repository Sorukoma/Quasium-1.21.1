package net.sorukoma.quasium.magic.page;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.magic.FocusEssence;
import net.sorukoma.quasium.magic.FocusUpgrade;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.magic.page.spirit.SpiritMix;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PageCasting {
    private static final Map<UUID, Shown> SHOWN = new HashMap<>();

    private record Shown(String label, String missing) {
    }

    private PageCasting() {
    }

    @Nullable
    public static ResourceLocation resolve(ServerPlayer player, ItemStack bowl, Set<ResourceLocation> knownHidden) {
        ItemStack tetrad = Tetrads.find(player);
        SpellPage page = tetrad.isEmpty() ? SpellPage.EMPTY : SpellTetradItem.contents(tetrad).activePage();
        if (page.isEmpty()) {
            SHOWN.remove(player.getUUID());
            return Spells.forBowl(bowl, knownHidden);
        }
        Optional<CompiledPage> compiled = PageCompiler.compile(page, 0, FocusUpgrade.of(bowl));
        if (compiled.isEmpty()) {
            SHOWN.put(player.getUUID(), new Shown(label(page), ""));
            return null;
        }
        List<String> missing = new ArrayList<>(Tetrads.missing(compiled.get(), Spells.focusOf(bowl)).stream().map(NoteType::id).toList());
        if (compiled.get().spell().form() == SpellForm.SPIRIT && !SpiritMix.inhaled(player, bowl)) missing.add(SpiritMix.TOKEN);
        SHOWN.put(player.getUUID(), new Shown(label(page), String.join(",", missing)));
        return missing.isEmpty() ? compiled.get().id() : null;
    }

    public static String label(SpellPage page) {
        if (!page.name().isBlank()) return page.name();
        if (page.isSeal()) return "@" + page.seal();
        return "#" + page.form();
    }

    public static String shownLabel(ServerPlayer player) {
        Shown shown = SHOWN.get(player.getUUID());
        return shown == null ? "" : shown.label();
    }

    public static String shownMissing(ServerPlayer player) {
        Shown shown = SHOWN.get(player.getUUID());
        return shown == null ? "" : shown.missing();
    }

    public static boolean hides(ServerPlayer player) {
        return !shownMissing(player).isEmpty();
    }

    public static boolean dropShown(ServerPlayer player) {
        return SHOWN.remove(player.getUUID()) != null;
    }

    public static void forget(UUID playerId) {
        SHOWN.remove(playerId);
    }

    public static CompiledPage boosted(CompiledPage base, float lungs, @Nullable FocusUpgrade upgrade) {
        int step = PageMath.overflowStep(lungs, base.volume(), PageConfig.OVERFLOW_STEP_ML.getAsInt(), PageConfig.OVERFLOW_MAX_STEPS.getAsInt());
        if (step == 0) return base;
        return PageCompiler.compile(base.page(), step, base.isSeal() ? null : upgrade).orElse(base);
    }

    public static ResourceLocation effectKey(String effect) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "page_effect/" + effect);
    }

    public static long readyAt(Map<ResourceLocation, Long> readyAt, CompiledPage page) {
        if (page.isSeal()) return readyAt.getOrDefault(page.legacyId(), 0L);
        long latest = 0L;
        for (String heart : page.hearts()) latest = Math.max(latest, readyAt.getOrDefault(effectKey(heart), 0L));
        return latest;
    }

    public static void startCooldowns(Map<ResourceLocation, Long> readyAt, CompiledPage page, long now, float ratio) {
        if (page.isSeal()) {
            readyAt.put(page.legacyId(), now + Math.round(page.spell().cooldownTicks() * ratio));
            return;
        }
        PageCatalog catalog = PageCatalog.get();
        for (String heart : page.hearts()) {
            PageEffect effect = catalog.effect(heart);
            if (effect != null) readyAt.put(effectKey(heart), now + Math.round(effect.cooldownTicks() * ratio));
        }
    }

    public static float grams(CompiledPage page, float lungs) {
        if (page.isSeal()) return page.spell().grams();
        return PageMath.grams(lungs, PageConfig.GRAMS_PER_ML.get().floatValue(), page.param("extra_grams", 0f));
    }

    public static void drain(ItemStack bowl, CompiledPage page, int multiplier) {
        if (multiplier <= 0) return;
        if (page.isSeal()) {
            bowl.set(ModDataComponents.BOWL_FOCUS.get(), FocusEssence.drained(Spells.focusOf(bowl), multiplier));
            return;
        }
        bowl.set(ModDataComponents.BOWL_FOCUS.get(), Tetrads.drain(Spells.focusOf(bowl), page.noteCost(), multiplier));
    }

    public static float power(CompiledPage page, float power) {
        return PageMath.capPower(power * page.param(PageCompiler.POWER, 1f), PageConfig.POWER_CAP_PCT.getAsInt() / 100f);
    }

    public static void announce(ServerPlayer player, CompiledPage page) {
        SmokecraftTrigger.fire(player, "cast");
        if (page.isSeal()) {
            SmokecraftTrigger.fire(player, "cast:" + page.legacyId().getPath());
            return;
        }
        SmokecraftTrigger.fire(player, "cast:page");
        SmokecraftTrigger.fire(player, "cast_form:" + page.page().form());
        page.accords().forEach(accord -> SmokecraftTrigger.fire(player, "cast_accord:" + accord));
    }
}
