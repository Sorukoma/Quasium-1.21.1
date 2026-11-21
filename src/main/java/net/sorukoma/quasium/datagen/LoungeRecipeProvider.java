package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.block.LoungeBlocks;

import java.util.concurrent.CompletableFuture;

public class LoungeRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public LoungeRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Lounge recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, LoungeBlocks.LOUNGE_CUSHION_ITEM.get(), 2)
                    .pattern(" S ")
                    .pattern("WWW")
                    .define('S', Items.STRING)
                    .define('W', ItemTags.WOOL)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, LoungeBlocks.LOW_TABLE_ITEM.get())
                    .pattern("PPP")
                    .pattern("S S")
                    .define('P', ItemTags.PLANKS)
                    .define('S', Items.STICK)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, LoungeBlocks.AROMA_LAMP_ITEM.get())
                    .pattern(" B ")
                    .pattern("CTC")
                    .pattern(" C ")
                    .define('B', Items.GLASS_BOTTLE)
                    .define('T', Items.TORCH)
                    .define('C', ItemTags.TERRACOTTA)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            bulb(output, LoungeBlocks.GLOWING_BULB_WHITE_ITEM, Items.WHITE_DYE);
            bulb(output, LoungeBlocks.GLOWING_BULB_BLACK_ITEM, Items.BLACK_DYE);
            bulb(output, LoungeBlocks.GLOWING_BULB_RED_ITEM, Items.RED_DYE);
            bulb(output, LoungeBlocks.GLOWING_BULB_BLUE_ITEM, Items.BLUE_DYE);
            bulb(output, LoungeBlocks.GLOWING_BULB_AMBER_ITEM, Items.ORANGE_DYE);
        }

        private static void bulb(RecipeOutput output, DeferredItem<?> bulb, Item dye) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, bulb.get())
                    .pattern(" D ")
                    .pattern("GLG")
                    .pattern(" G ")
                    .define('D', dye)
                    .define('G', Items.GLASS)
                    .define('L', Items.GLOWSTONE_DUST)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
