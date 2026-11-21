package net.sorukoma.quasium.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.selection.genetics.VarietyDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class TobaccoVarieties {
    public static final String DIKAR = "dikar";
    public static final String PESCHANIK = "peschanik";
    public static final String BOLOTNIK = "bolotnik";
    public static final String LEDYANOY = "ledyanoy";
    public static final String HYBRID = "hybrid";
    public static final String ECHO = "echo";
    public static final String ASHEN = "ashen";
    public static final String CHORUS = "chorus";
    public static final String PRIMORDIAL = "primordial";

    private static volatile Map<String, VarietyDefinition> definitions = Map.of();

    private TobaccoVarieties() {}

    public static void load(Map<ResourceLocation, VarietyDefinition> loaded) {
        Map<String, VarietyDefinition> byVariety = new TreeMap<>();
        loaded.forEach((id, definition) -> byVariety.put(id.getPath(), definition));
        replace(byVariety);
    }

    public static void replace(Map<String, VarietyDefinition> loaded) {
        definitions = Map.copyOf(loaded);
    }

    public static Map<String, VarietyDefinition> all() {
        return definitions;
    }

    @Nullable
    public static VarietyDefinition definition(String variety) {
        return definitions.get(variety);
    }

    public static List<String> wild() {
        return definitions.entrySet().stream().filter(entry -> entry.getValue().wild()).map(Map.Entry::getKey).sorted().toList();
    }

    public static boolean isLeaf(ItemStack stack) {
        return stack.getItem() instanceof LeavesItem;
    }

    public static String getVariety(ItemStack stack) {
        String stored = stack.get(ModDataComponents.TOBACCO_VARIETY.get());
        if (stored != null) return stored;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String suffix : List.of("_leaves", "_seeds")) {
            if (!path.endsWith(suffix)) continue;
            String variety = path.substring(0, path.length() - suffix.length());
            return variety.equals("hybrid_tobacco") ? HYBRID : variety;
        }
        return DIKAR;
    }

    public static int strength(String variety) {
        VarietyDefinition definition = definition(variety);
        return definition == null ? 3 : definition.strength();
    }

    public static int yield(String variety) {
        VarietyDefinition definition = definition(variety);
        return definition == null ? 2 : definition.yield();
    }

    public static int quality(String variety) {
        VarietyDefinition definition = definition(variety);
        return definition == null ? 3 : definition.quality();
    }

    public static TagKey<Block> soil(String variety) {
        VarietyDefinition definition = definition(variety);
        return definition == null ? VarietyDefinition.DEFAULT_SOIL : definition.soil();
    }

    public static int growthPct(String variety, Level level) {
        VarietyDefinition definition = definition(variety);
        return definition == null ? 100 : definition.growthPct(level);
    }
}
