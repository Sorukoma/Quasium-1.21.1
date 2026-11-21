package net.sorukoma.quasium.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class MixingRecipes {
    public static final Codec<Map<ResourceLocation, MixingRecipe>> MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, MixingRecipe.CODEC);

    private static volatile Map<ResourceLocation, MixingRecipe> recipes = Map.of();

    private MixingRecipes() {
    }

    public static void set(Map<ResourceLocation, MixingRecipe> loaded) {
        recipes = Collections.unmodifiableMap(new TreeMap<>(loaded));
    }

    public static Map<ResourceLocation, MixingRecipe> all() {
        return recipes;
    }

    @Nullable
    public static MixingRecipe find(ItemStack bottle, ItemStack catalyst) {
        for (MixingRecipe recipe : recipes.values()) {
            if (recipe.matches(bottle, catalyst)) return recipe;
        }
        return null;
    }

    public static boolean isCatalyst(ItemStack stack) {
        return recipes.values().stream().anyMatch(recipe -> stack.is(recipe.catalyst()));
    }
}
