package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.AromaVialItem;
import net.sorukoma.quasium.item.ModItemTags;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FlavorRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public FlavorRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    public static String vialRecipe(String flavor) {
        return "aroma_vial_" + flavor;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Aroma vial recipes";
    }

    private static Map<String, Ingredient> sources() {
        Map<String, Ingredient> sources = new LinkedHashMap<>();
        sources.put("mint", Ingredient.of(ModItemTags.ESSENCE_MINT));
        sources.put("berry", Ingredient.of(ModItemTags.ESSENCE_BERRY));
        sources.put("lemon", Ingredient.of(ModItemTags.ESSENCE_LEMON));
        sources.put("lime", Ingredient.of(ModItemTags.ESSENCE_LIME));
        sources.put("orange", Ingredient.of(ModItemTags.ESSENCE_ORANGE));
        sources.put("grapefruit", Ingredient.of(ModItemTags.ESSENCE_GRAPEFRUIT));
        sources.put("ice", Ingredient.of(ModItemTags.ESSENCE_ICE));
        sources.put("honey", Ingredient.of(ModItemTags.ESSENCE_HONEY));
        sources.put("nether", Ingredient.of(ModItemTags.ESSENCE_NETHER));
        sources.put("lavender", Ingredient.of(ModItemTags.ESSENCE_LAVENDER));
        sources.put("cocoa", Ingredient.of(ModItemTags.ESSENCE_COCOA));
        sources.put("apple", Ingredient.of(Items.APPLE));
        sources.put("watermelon", Ingredient.of(Items.MELON_SLICE));
        sources.put("grape", Ingredient.of(ModItemTags.ESSENCE_GRAPE));
        sources.put("cherry", Ingredient.of(Items.GLOW_BERRIES));
        sources.put("vanilla", Ingredient.of(Items.SUGAR));
        sources.put("coffee", Ingredient.of(Items.COCOA_BEANS));
        sources.put("cinnamon", Ingredient.of(Items.BLAZE_POWDER));
        sources.put("coconut", Ingredient.of(ModItemTags.ESSENCE_COCONUT));
        sources.put("rose", Ingredient.of(Items.PINK_PETALS));
        sources.put("anise", Ingredient.of(ModItemTags.ESSENCE_ANISE));
        return sources;
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            sources().forEach((flavor, source) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AromaVialItem.of(flavor))
                    .requires(Items.GLASS_BOTTLE)
                    .requires(source, 2)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output, id(vialRecipe(flavor))));
        }

        private static ResourceLocation id(String path) {
            return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
        }
    }
}
