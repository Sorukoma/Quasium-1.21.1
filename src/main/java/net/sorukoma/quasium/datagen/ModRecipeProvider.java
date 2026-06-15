package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        super(pOutput, pRegistries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        // Blasting: Raw Quasium → Quasium Ingot
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAWQUASIUM.get()), RecipeCategory.MISC, ModItems.QUASIUMINGOT.get(), 0.7f, 100)
                .unlockedBy("has_raw_quasium", has(ModItems.RAWQUASIUM.get()))
                .save(pRecipeOutput, "quasiumingot_from_blasting_rawquasium");

        // Shapeless crafting: Ghast Tear + Water Bottle → Bottled Ghast Acid
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BOTTLED_GHAST_ACID.get())
                .requires(Items.GHAST_TEAR)
                .requires(Ingredient.of(Items.POTION))
                .unlockedBy("has_ghast_tear", has(Items.GHAST_TEAR))
                .save(pRecipeOutput, "bottled_ghast_acid_from_ghast_tear");

        // Shapeless crafting: Glycerin Bottle + Honey Bottle + Sugar → Molasses Bottle
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MOLASSES_BOTTLE.get())
                .requires(ModItems.GLYCERIN_BOTTLE.get())
                .requires(Items.HONEY_BOTTLE)
                .requires(Items.SUGAR)
                .unlockedBy("has_glycerin", has(ModItems.GLYCERIN_BOTTLE.get()))
                .save(pRecipeOutput, "molasses_bottle_from_glycerin");

        // Shaped crafting: Kaifometer (shield pattern with redstone center)
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.KAIFOMETER.get())
                .pattern("II ")
                .pattern("IRI")
                .pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(pRecipeOutput, "kaifometer_from_iron_and_redstone");

        // Shaped crafting: Undried Quasium Block (9 quasium ingots)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.UNDRIED_QUASIUM_BLOCK_ITEM.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.QUASIUMINGOT.get())
                .unlockedBy("has_quasium_ingot", has(ModItems.QUASIUMINGOT.get()))
                .save(pRecipeOutput, "undried_quasium_block_from_quasium_ingots");

        // Shaped crafting: Quasium Anvil (3 dried quasium blocks + 3 iron ingots, vanilla anvil pattern)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.QUASIUM_ANVIL_ITEM.get())
                .pattern("BBB")
                .pattern(" I ")
                .pattern("III")
                .define('B', ModBlocks.QUASIUM_BLOCK_ITEM.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_quasium_block", has(ModBlocks.QUASIUM_BLOCK_ITEM.get()))
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pRecipeOutput, "quasium_anvil_from_quasium_blocks");

        // Shaped crafting: Netherite Hammer (netherite ingot + stick pattern)
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.NETHERITE_HAMMER.get())
                .pattern("NN")
                .pattern("NS")
                .pattern(" S")
                .define('N', Items.NETHERITE_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                .save(pRecipeOutput, "netherite_hammer_from_netherite_ingot");

        // ─── Charcoal, Calloud & Heater Recipes ───

        // Уголь 22мм: 1 coal → 4 charcoal_22
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CHARCOAL_22.get(), 4)
                .requires(Items.CHARCOAL)
                .unlockedBy("has_charcoal", has(Items.CHARCOAL))
                .save(pRecipeOutput, "charcoal_22_from_charcoal");

        // Уголь 25мм: 2 coal → 3 charcoal_25
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CHARCOAL_25.get(), 3)
                .requires(Items.CHARCOAL)
                .requires(Items.CHARCOAL)
                .unlockedBy("has_charcoal", has(Items.CHARCOAL))
                .save(pRecipeOutput, "charcoal_25_from_charcoal");

        // Уголь 27мм: 3 coal → 2 charcoal_27
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CHARCOAL_27.get(), 2)
                .requires(Items.CHARCOAL)
                .requires(Items.CHARCOAL)
                .requires(Items.CHARCOAL)
                .unlockedBy("has_charcoal", has(Items.CHARCOAL))
                .save(pRecipeOutput, "charcoal_27_from_charcoal");

        // Каллауд: iron + copper (heat management device)
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CALLOUD.get())
                .pattern("ICI")
                .pattern(" C ")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(pRecipeOutput, "calloud_from_iron_and_copper");

        // Печка для кальяна: iron + copper + bricks
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HEATER_ITEM.get())
                .pattern("BBB")
                .pattern("BCB")
                .pattern("III")
                .define('B', Items.BRICK)
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_brick", has(Items.BRICK))
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pRecipeOutput, "heater_from_brick_copper_iron");
    }
}