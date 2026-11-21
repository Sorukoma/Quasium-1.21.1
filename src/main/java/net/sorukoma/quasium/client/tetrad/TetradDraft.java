package net.sorukoma.quasium.client.tetrad;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.client.ClientMagicData;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageCompiler;
import net.sorukoma.quasium.magic.page.PageEffect;
import net.sorukoma.quasium.magic.page.PageForm;
import net.sorukoma.quasium.magic.page.PageRules;
import net.sorukoma.quasium.magic.page.PageTop;
import net.sorukoma.quasium.magic.page.PageUnlocks;
import net.sorukoma.quasium.magic.page.SpellPage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class TetradDraft {
    private static final String SEAL_FORM = "seal";

    private SpellPage stored = SpellPage.EMPTY;
    private SpellPage page = SpellPage.EMPTY;

    void reset(SpellPage latest) {
        stored = latest;
        page = latest;
    }

    void follow(SpellPage latest) {
        if (latest.equals(stored)) return;
        if (page.equals(stored)) page = latest;
        stored = latest;
    }

    SpellPage page() {
        return page;
    }

    boolean dirty() {
        return !page.equals(stored);
    }

    void name(String name) {
        page = page.named(name);
    }

    void clear() {
        page = SpellPage.EMPTY;
    }

    void form(String form) {
        PageCatalog catalog = PageCatalog.get();
        List<String> tops = new ArrayList<>();
        for (String top : page.tops()) {
            PageTop definition = catalog.top(top);
            if (definition != null && definition.fits(form)) tops.add(top);
        }
        page = new SpellPage(page.name(), form, page.hearts(), tops, keptOverflow(tops), "");
    }

    void heart(int slot, @Nullable String effect) {
        page = new SpellPage(page.name(), page.form(), placed(page.hearts(), slot, effect), page.tops(), page.overflow(), page.seal());
    }

    void top(int slot, @Nullable String top) {
        List<String> tops = placed(page.tops(), slot, top);
        page = new SpellPage(page.name(), page.form(), page.hearts(), tops, keptOverflow(tops), page.seal());
    }

    void cycleOverflow() {
        List<String> options = overflowOptions();
        int next = (options.indexOf(page.overflow()) + 1) % options.size();
        page = new SpellPage(page.name(), page.form(), page.hearts(), page.tops(), options.get(next), page.seal());
    }

    List<String> overflowOptions() {
        List<String> options = new ArrayList<>(List.of(""));
        PageCatalog catalog = PageCatalog.get();
        for (String top : page.tops()) {
            PageTop definition = catalog.top(top);
            if (definition != null && !definition.scale().isEmpty() && !options.contains(top)) options.add(top);
        }
        return options;
    }

    Optional<CompiledPage> compiled() {
        return PageCompiler.compile(page, 0, null);
    }

    Optional<Component> problem(Player player) {
        if (dirty() && ClientMagicData.lungs() > 0f) return refused("breath");
        if (page.isEmpty()) {
            return page.hearts().isEmpty() && page.tops().isEmpty() ? Optional.empty()
                    : Optional.of(Component.translatable("quasium.tetrad.problem.base"));
        }
        PageForm form = PageCatalog.get().form(page.form());
        if (!page.isSeal() && page.hearts().isEmpty() && (form == null || form.shape() != SpellForm.SPIRIT)) return refused("empty");
        Optional<CompiledPage> compiled = compiled();
        if (compiled.isEmpty()) return refused("broken");
        if (compiled.get().volume() > PageRules.maxVolume(player)) return refused("volume");
        return Optional.empty();
    }

    @Nullable
    Component layerRefusal(Player player, String node) {
        if (page.isSeal()) return Component.translatable("quasium.tetrad.refusal.seal");
        return PageUnlocks.open(player, node) ? null : PartInfo.unlock(node);
    }

    @Nullable
    Component formRefusal(Player player, String id, PageForm form) {
        Component layer = layerRefusal(player, PageRules.BASE_NODE);
        if (layer != null) return layer;
        return open(player, form.unlock(), stored.form().equals(id)) ? null : PartInfo.unlock(form.unlock());
    }

    @Nullable
    Component effectRefusal(Player player, int slot, String id, PageEffect effect) {
        Component layer = layerRefusal(player, PageRules.HEART_NODE);
        if (layer != null) return layer;
        if (page.hearts().contains(id) && page.hearts().indexOf(id) != slot) return Component.translatable("quasium.tetrad.refusal.twice");
        return open(player, effect.unlock(), stored.hearts().contains(id)) ? null : PartInfo.unlock(effect.unlock());
    }

    @Nullable
    Component topRefusal(Player player, int slot, String id, PageTop top) {
        if (!top.fits(page.isSeal() ? SEAL_FORM : page.form())) return Component.translatable("quasium.tetrad.refusal.fits");
        List<String> others = new ArrayList<>(page.tops());
        if (slot < others.size()) others.remove(slot);
        if (Collections.frequency(others, id) >= top.max()) return Component.translatable("quasium.tetrad.refusal.max", top.max());
        return open(player, top.unlock(), stored.tops().contains(id)) ? null : PartInfo.unlock(top.unlock());
    }

    private String keptOverflow(List<String> tops) {
        return tops.contains(page.overflow()) ? page.overflow() : "";
    }

    private static List<String> placed(List<String> parts, int slot, @Nullable String part) {
        List<String> changed = new ArrayList<>(parts);
        if (part == null) {
            if (slot < changed.size()) changed.remove(slot);
            return changed;
        }
        if (slot < changed.size()) {
            changed.set(slot, part);
        } else {
            changed.add(part);
        }
        return changed;
    }

    private static boolean open(Player player, String unlock, boolean alreadyOnPage) {
        return alreadyOnPage || PageUnlocks.open(player, unlock);
    }

    private static Optional<Component> refused(String reason) {
        return Optional.of(Component.translatable("quasium.page.refused." + reason));
    }
}
