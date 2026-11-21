package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.cart.HookahCarts;

import java.util.concurrent.CompletableFuture;

public class CartRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public CartRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Hookah cart recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, HookahCarts.HOOKAH_MINECART_ITEM.get())
                    .requires(Items.MINECART)
                    .requires(ModBlocks.HOOKAH_BLOCK_ITEM.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
