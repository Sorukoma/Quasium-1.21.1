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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.golem.MarkBlocks;
import net.sorukoma.quasium.golem.program.ProgramBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class MarkRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public MarkRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Smoke mark recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            mark(output, MarkBlocks.HARVEST_MARK_ITEM.get(), Items.LIME_DYE);
            mark(output, MarkBlocks.FILL_MARK_ITEM.get(), Items.LIGHT_BLUE_DYE);
            mark(output, MarkBlocks.GUARD_MARK_ITEM.get(), Items.RED_DYE);
            mark(output, MarkBlocks.HEAT_MARK_ITEM.get(), Items.ORANGE_DYE);
            mark(output, MarkBlocks.ZONE_MARK_ITEM.get(), Items.PURPLE_DYE);
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MarkBlocks.GOLEM_STAFF.get())
                    .pattern("  Q")
                    .pattern(" S ")
                    .pattern("S  ")
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .define('S', Items.STICK)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MarkBlocks.GOLEM_FEEDER_ITEM.get())
                    .pattern("PAP")
                    .pattern("PPP")
                    .define('P', ItemTags.PLANKS)
                    .define('A', Items.APPLE)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProgramBlocks.PROGRAM_TABLE_ITEM.get())
                    .pattern("PRP")
                    .pattern("#Q#")
                    .pattern("# #")
                    .define('P', Items.PAPER)
                    .define('R', Items.REDSTONE)
                    .define('#', ItemTags.PLANKS)
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ProgramBlocks.PLACE_MARKER.get())
                    .pattern("D")
                    .pattern("P")
                    .pattern("S")
                    .define('D', Items.CYAN_DYE)
                    .define('P', Items.PAPER)
                    .define('S', Items.STICK)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ProgramBlocks.PROGRAM_CARD.get())
                    .requires(Items.PAPER)
                    .requires(Items.CLAY_BALL)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }

        private void mark(RecipeOutput output, Item result, Item dye) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 4)
                    .requires(Items.PAPER)
                    .requires(Items.CHARCOAL)
                    .requires(dye)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
