package net.sorukoma.quasium.ritual.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.datagen.SteppedRecipeProvider;
import net.sorukoma.quasium.draft.DraftBlocks;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.ritual.RitualRegistry;

import java.util.concurrent.CompletableFuture;

public class RitualRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public RitualRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Ritual brazier recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RitualRegistry.RITUAL_BRAZIER_ITEM.get())
                    .pattern("QCQ")
                    .pattern("QFQ")
                    .pattern("III")
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .define('C', ModItems.CALLOUD.get())
                    .define('F', Items.CAMPFIRE)
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RitualRegistry.RITUAL_PEDESTAL_ITEM.get())
                    .pattern("QQQ")
                    .pattern(" D ")
                    .pattern("DDD")
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .define('D', Items.POLISHED_DEEPSLATE)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, RitualRegistry.SIGNAL_PIPE.get())
                    .requires(DraftBlocks.COPPER_PIPE_ITEM.get(), 2)
                    .requires(Items.CAMPFIRE)
                    .requires(ModItems.QUASIUMINGOT.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
