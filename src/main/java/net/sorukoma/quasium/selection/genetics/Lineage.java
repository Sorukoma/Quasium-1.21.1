package net.sorukoma.quasium.selection.genetics;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.selection.SelectionRegistry;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public final class Lineage {
    private Lineage() {
    }

    public static List<String> of(ItemStack stack) {
        List<String> stored = stack.get(SelectionRegistry.TOBACCO_LINEAGE.get());
        return stored == null || stored.isEmpty() ? List.of(TobaccoVarieties.getVariety(stack)) : stored;
    }

    public static void apply(ItemStack stack, List<String> lineage) {
        if (isHybrid(lineage)) stack.set(SelectionRegistry.TOBACCO_LINEAGE.get(), List.copyOf(new TreeSet<>(lineage)));
    }

    public static List<String> merge(Collection<String> first, Collection<String> second) {
        TreeSet<String> merged = new TreeSet<>(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }

    public static boolean isHybrid(Collection<String> lineage) {
        return new TreeSet<>(lineage).size() >= 2;
    }

    public static String key(Collection<String> lineage) {
        return String.join("+", new TreeSet<>(lineage));
    }

    public static MutableComponent displayName(Collection<String> lineage) {
        MutableComponent name = Component.empty();
        for (String variety : new TreeSet<>(lineage)) {
            if (!name.getSiblings().isEmpty()) name.append(" x ");
            name.append(Component.translatable("quasium.variety." + variety));
        }
        return name;
    }
}
