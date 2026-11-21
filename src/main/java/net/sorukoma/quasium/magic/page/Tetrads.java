package net.sorukoma.quasium.magic.page;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.magic.FocusEssence;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.Spell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Tetrads {
    private Tetrads() {
    }

    public static ItemStack find(Player player) {
        if (player.getOffhandItem().getItem() instanceof SpellTetradItem) return player.getOffhandItem();
        if (player.getMainHandItem().getItem() instanceof SpellTetradItem) return player.getMainHandItem();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SpellTetradItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static Map<NoteType, Integer> available(List<FocusEssence> focus) {
        PageCatalog catalog = PageCatalog.get();
        EnumMap<NoteType, Integer> available = new EnumMap<>(NoteType.class);
        for (FocusEssence bottle : focus) {
            if (bottle.isEmpty()) continue;
            catalog.notesOf(bottle.essence()).forEach(note -> available.merge(note, bottle.ml(), Integer::sum));
        }
        return available;
    }

    public static Set<NoteType> missing(CompiledPage page, List<FocusEssence> focus) {
        if (page.isSeal()) return castsSeal(page, focus) ? Set.of() : EnumSet.copyOf(sealNotes(page));
        Map<NoteType, Integer> available = available(focus);
        Set<NoteType> missing = EnumSet.noneOf(NoteType.class);
        page.noteCost().forEach((note, cost) -> {
            if (available.getOrDefault(note, 0) < cost) missing.add(note);
        });
        return missing;
    }

    private static boolean castsSeal(CompiledPage page, List<FocusEssence> focus) {
        Spell legacy = page.spell();
        return MagicMath.matchAlternative(legacy.inputs(), FocusEssence.activeEssences(focus)) >= 0;
    }

    private static Set<NoteType> sealNotes(CompiledPage page) {
        Set<NoteType> notes = EnumSet.noneOf(NoteType.class);
        PageCatalog catalog = PageCatalog.get();
        if (!page.spell().inputs().isEmpty()) page.spell().inputs().getFirst().keySet().forEach(essence -> notes.addAll(catalog.notesOf(essence)));
        return notes.isEmpty() ? EnumSet.allOf(NoteType.class) : notes;
    }

    public static List<FocusEssence> drain(List<FocusEssence> focus, Map<NoteType, Integer> cost, int multiplier) {
        List<FocusEssence> bottles = new ArrayList<>(focus);
        PageCatalog catalog = PageCatalog.get();
        cost.forEach((note, amount) -> {
            int left = amount * multiplier;
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < bottles.size(); i++) {
                if (!bottles.get(i).isEmpty() && catalog.notesOf(bottles.get(i).essence()).contains(note)) order.add(i);
            }
            order.sort(Comparator.comparingInt(i -> catalog.notesOf(bottles.get(i).essence()).size()));
            for (int index : order) {
                if (left <= 0) break;
                FocusEssence bottle = bottles.get(index);
                int taken = Math.min(left, bottle.ml());
                bottles.set(index, new FocusEssence(bottle.essence(), bottle.ml() - taken));
                left -= taken;
            }
        });
        return bottles;
    }
}
