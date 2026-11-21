package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.sorukoma.quasium.draft.DraftBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class DraftRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public DraftRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Draft recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            pipe(output, DraftBlocks.CLAY_PIPE_ITEM.get(), 8, Items.BRICK, Items.CLAY_BALL);
            pipe(output, DraftBlocks.COPPER_PIPE_ITEM.get(), 4, Items.COPPER_INGOT, Items.COPPER_INGOT);
            pipe(output, DraftBlocks.GLASS_PIPE_ITEM.get(), 4, Items.GLASS, Items.COPPER_INGOT);
            pipe(output, DraftBlocks.QUASIUM_PIPE_ITEM.get(), 4, ModItems.QUASIUMINGOT.get(), ModItems.QUASIUMINGOT.get());
            ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, DraftBlocks.SMOKE_VALVE_ITEM.get())
                    .requires(DraftBlocks.COPPER_PIPE_ITEM.get())
                    .requires(Items.LEVER)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, DraftBlocks.NOTE_FILTER_ITEM.get())
                    .requires(DraftBlocks.COPPER_PIPE_ITEM.get())
                    .requires(Items.PAPER)
                    .requires(Items.STRING)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, DraftBlocks.SMOKE_SPLITTER_ITEM.get())
                    .requires(DraftBlocks.COPPER_PIPE_ITEM.get(), 3)
                    .requires(Items.COPPER_INGOT)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, DraftBlocks.SMOKE_SORTER_ITEM.get())
                    .requires(ModItems.HOOKAH_BOWL.get())
                    .requires(Items.GLASS_BOTTLE)
                    .requires(DraftBlocks.COPPER_PIPE_ITEM.get())
                    .requires(Items.CHARCOAL)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DraftBlocks.AUTO_DISTILLER_ITEM.get())
                    .pattern("CPC")
                    .pattern("GRG")
                    .pattern("CCC")
                    .define('C', Items.COPPER_INGOT)
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('G', Items.GLASS)
                    .define('R', Items.REDSTONE)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DraftBlocks.BELLOWS_ITEM.get())
                    .pattern("PPP")
                    .pattern("LCL")
                    .pattern("PPP")
                    .define('P', ItemTags.PLANKS)
                    .define('L', Items.LEATHER)
                    .define('C', Items.COPPER_INGOT)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }

        private void pipe(RecipeOutput output, ItemLike result, int count, ItemLike wall, ItemLike core) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count)
                    .pattern(" W ")
                    .pattern("WCW")
                    .pattern(" W ")
                    .define('W', wall)
                    .define('C', core)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
