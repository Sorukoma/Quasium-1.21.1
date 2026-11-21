package net.sorukoma.quasium.ritual;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class RitualRecipes {
    public static final Codec<Map<ResourceLocation, RitualRecipe>> MAP_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, RitualRecipe.CODEC);

    public record Match(ResourceLocation id, RitualRecipe recipe, int[] pedestals) {
    }

    private static volatile Map<ResourceLocation, RitualRecipe> all = Map.of();

    private RitualRecipes() {
    }

    public static void set(Map<ResourceLocation, RitualRecipe> loaded) {
        all = Map.copyOf(loaded);
    }

    public static Map<ResourceLocation, RitualRecipe> all() {
        return all;
    }

    @Nullable
    public static RitualRecipe get(@Nullable ResourceLocation id) {
        return id == null ? null : all.get(id);
    }

    public static boolean anyTakes(ItemStack catalyst) {
        return all.values().stream().anyMatch(recipe -> recipe.takesCatalyst(catalyst));
    }

    public static Optional<Match> find(ItemStack catalyst, List<ItemStack> pedestalItems) {
        return all.entrySet().stream()
                .filter(entry -> entry.getValue().takesCatalyst(catalyst))
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, RitualRecipe> entry) -> -entry.getValue().pedestals().size())
                        .thenComparing(Map.Entry::getKey))
                .flatMap(entry -> {
                    int[] picked = entry.getValue().matchPedestals(pedestalItems);
                    return picked == null ? Stream.empty() : Stream.of(new Match(entry.getKey(), entry.getValue(), picked));
                })
                .findFirst();
    }

    public static Optional<Map.Entry<ResourceLocation, RitualRecipe>> target(ItemStack catalyst, List<ItemStack> pedestalItems) {
        Optional<Match> match = find(catalyst, pedestalItems);
        if (match.isPresent()) return Optional.of(Map.entry(match.get().id(), match.get().recipe()));
        return all.entrySet().stream().filter(entry -> entry.getValue().takesCatalyst(catalyst))
                .min(Comparator.comparingInt((Map.Entry<ResourceLocation, RitualRecipe> entry) -> entry.getValue().missingPedestals(pedestalItems).size())
                        .thenComparingInt(entry -> entry.getValue().pedestals().size())
                        .thenComparing(Map.Entry::getKey));
    }
}
