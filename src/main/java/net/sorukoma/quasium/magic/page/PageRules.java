package net.sorukoma.quasium.magic.page;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.artifact.QuasiumHarness;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PageRules {
    public static final String HEART_NODE = "research:quasium:research_smoke_heart";
    public static final String BASE_NODE = "research:quasium:research_smoke_bases";

    private PageRules() {
    }

    public static int maxVolume(Player player) {
        float lungs = (Config.LUNG_CAPACITY_MB.getAsInt() + Config.UPGRADE_CONDENSER_ML.getAsInt()) * Perk.lungMultiplier(player);
        if (QuasiumHarness.worn(player)) lungs = lungs * RitualConfig.HARNESS_LUNG_PCT.getAsInt() / 100f;
        return Math.round(lungs);
    }

    public static Optional<Component> check(ServerPlayer player, SpellPage page, SpellPage previous) {
        if (page.name().length() > SpellPage.MAX_NAME || page.name().chars().anyMatch(Character::isISOControl)) {
            return refuse("name");
        }
        if (page.isEmpty()) return Optional.empty();
        if (page.isSeal()) return checkSeal(player, page, previous);
        PageCatalog catalog = PageCatalog.get();
        PageForm form = catalog.form(page.form());
        boolean sameForm = previous.form().equals(page.form());
        if (form == null || !open(player, form.unlock(), sameForm) || !open(player, BASE_NODE, sameForm)) return refuse("form");
        if (!open(player, HEART_NODE, previous.hearts().equals(page.hearts()))) return refuse("heart");
        if (page.hearts().isEmpty() && form.shape() != SpellForm.SPIRIT) return refuse("empty");
        Set<String> seen = new HashSet<>();
        for (String heart : page.hearts()) {
            PageEffect effect = catalog.effect(heart);
            if (effect == null || !seen.add(heart) || !open(player, effect.unlock(), previous.hearts().contains(heart))) {
                return refuse("heart");
            }
        }
        Optional<Component> tops = checkTops(player, page, previous, page.form());
        if (tops.isPresent()) return tops;
        Optional<CompiledPage> compiled = PageCompiler.compile(page, 0, null);
        if (compiled.isEmpty()) return refuse("broken");
        if (compiled.get().volume() > maxVolume(player)) return refuse("volume");
        return Optional.empty();
    }

    private static Optional<Component> checkSeal(ServerPlayer player, SpellPage page, SpellPage previous) {
        ResourceLocation id = ResourceLocation.tryParse(page.seal());
        Spell legacy = id == null ? null : Spells.registered(id);
        if (legacy == null) return refuse("seal");
        boolean kept = previous.seal().equals(page.seal());
        if (!kept && legacy.hidden() && !Research.knownSpells(player).contains(id)) return refuse("seal");
        return checkTops(player, page, previous, "seal");
    }

    private static Optional<Component> checkTops(ServerPlayer player, SpellPage page, SpellPage previous, String form) {
        PageCatalog catalog = PageCatalog.get();
        Map<String, Integer> counts = PageCompiler.countTops(page.tops(), form, catalog);
        if (counts == null) return refuse("top");
        for (String top : counts.keySet()) {
            if (!open(player, catalog.top(top).unlock(), previous.tops().contains(top))) return refuse("top");
        }
        if (!page.overflow().isEmpty() && !counts.containsKey(page.overflow())) return refuse("overflow");
        return Optional.empty();
    }

    private static boolean open(Player player, String unlock, boolean alreadyOnPage) {
        return alreadyOnPage || PageUnlocks.open(player, unlock);
    }

    private static Optional<Component> refuse(String reason) {
        return Optional.of(Component.translatable("quasium.page.refused." + reason));
    }

    public static List<String> refusalReasons() {
        return List.of("name", "form", "empty", "heart", "top", "overflow", "broken", "volume", "seal", "breath");
    }
}
