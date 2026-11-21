package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends SteppedRecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        super(pOutput, pRegistries);
    }

    private static ResourceLocation recipeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }

    private void addEssenceSeedRecipe(RecipeOutput output, Item result, String recipeName, net.minecraft.world.level.ItemLike... inputs) {
        ShapelessRecipeBuilder seeds = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 2);
        for (net.minecraft.world.level.ItemLike input : inputs) seeds.requires(input);
        seeds.unlockedBy(STEP, ON_STEP).save(output, recipeId(recipeName));
    }

    private void addBonsaiSaplingRecipe(RecipeOutput output, net.minecraft.world.level.ItemLike sapling,
                                        net.minecraft.world.level.ItemLike ingredient,
                                        net.minecraft.world.item.Item result, String recipeName) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result)
                .requires(sapling)
                .requires(ingredient)
                .unlockedBy(STEP, ON_STEP)
                .save(output, recipeId(recipeName));
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModBlocks.WOODEN_TOBACCO_POLE_ITEM.get())
                .requires(Items.STICK, 2).requires(Items.STRING)
                .unlockedBy(STEP, ON_STEP).save(pRecipeOutput, recipeId("wooden_tobacco_pole"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModBlocks.REINFORCED_TOBACCO_POLE_ITEM.get())
                .requires(ModBlocks.WOODEN_TOBACCO_POLE_ITEM.get()).requires(Items.IRON_INGOT)
                .unlockedBy(STEP, ON_STEP).save(pRecipeOutput, recipeId("reinforced_tobacco_pole"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModBlocks.GOLDEN_TOBACCO_POLE_ITEM.get())
                .requires(ModBlocks.REINFORCED_TOBACCO_POLE_ITEM.get()).requires(Items.GOLD_INGOT).requires(Items.DIAMOND)
                .unlockedBy(STEP, ON_STEP).save(pRecipeOutput, recipeId("golden_tobacco_pole"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModBlocks.DIAMOND_TOBACCO_POLE_ITEM.get())
                .requires(ModBlocks.GOLDEN_TOBACCO_POLE_ITEM.get()).requires(Items.DIAMOND).requires(Items.NETHERITE_INGOT)
                .unlockedBy(STEP, ON_STEP).save(pRecipeOutput, recipeId("diamond_tobacco_pole"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.FRIES.get(), 2)
                .requires(Items.POTATO, 2).requires(ModItems.TALLOW.get())
                .unlockedBy(STEP, ON_STEP).save(pRecipeOutput, recipeId("fries_from_potatoes_and_tallow"));

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAWQUASIUM.get()), RecipeCategory.MISC, ModItems.QUASIUMINGOT.get(), 0.7f, 100)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("quasiumingot_from_blasting_rawquasium"));

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAWQUASIUM.get()), RecipeCategory.MISC, ModItems.QUASIUMINGOT.get(), 0.7f, 200)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("quasiumingot_from_smelting_rawquasium"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BOTTLED_GHAST_ACID.get())
                .requires(Items.GHAST_TEAR)
                .requires(Ingredient.of(Items.POTION))
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("bottled_ghast_acid_from_ghast_tear"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MOLASSES_BOTTLE.get())
                .requires(ModItems.GLYCERIN_BOTTLE.get())
                .requires(Items.HONEY_BOTTLE)
                .requires(Items.SUGAR)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("molasses_bottle_from_glycerin"));

        addEssenceSeedRecipe(pRecipeOutput, ModItems.MINT_CROP_SEEDS.get(), "mint_seeds_from_fern", Items.FERN);
        addEssenceSeedRecipe(pRecipeOutput, ModItems.ANISE_SEEDS.get(), "anise_seeds_from_wheat_seeds", Items.WHEAT_SEEDS);
        addEssenceSeedRecipe(pRecipeOutput, ModItems.LAVENDER_SEEDS.get(), "lavender_seeds_from_lilac", Items.LILAC, Items.BONE_MEAL);
        addEssenceSeedRecipe(pRecipeOutput, ModItems.COCOA_CROP_SEEDS.get(), "cocoa_seeds_from_cocoa_beans", Items.COCOA_BEANS, Items.BONE_MEAL);

        addBonsaiSaplingRecipe(pRecipeOutput, Items.OAK_SAPLING, Items.YELLOW_DYE,
                ModItems.LEMON_SAPLING.get(), "lemon_sapling_from_oak_sapling");
        addBonsaiSaplingRecipe(pRecipeOutput, Items.OAK_SAPLING, Items.LIME_DYE,
                ModItems.LIME_SAPLING.get(), "lime_sapling_from_oak_sapling");
        addBonsaiSaplingRecipe(pRecipeOutput, Items.OAK_SAPLING, Items.ORANGE_DYE,
                ModItems.ORANGE_SAPLING.get(), "orange_sapling_from_oak_sapling");
        addBonsaiSaplingRecipe(pRecipeOutput, Items.OAK_SAPLING, Items.PINK_DYE,
                ModItems.GRAPEFRUIT_SAPLING.get(), "grapefruit_sapling_from_oak_sapling");
        addBonsaiSaplingRecipe(pRecipeOutput, Items.AZALEA, Items.SWEET_BERRIES,
                ModItems.GRAPE_SAPLING.get(), "grape_sapling_from_azalea");
        addBonsaiSaplingRecipe(pRecipeOutput, Items.JUNGLE_SAPLING, Items.COCOA_BEANS,
                ModItems.COCONUT_SAPLING.get(), "coconut_sapling_from_jungle_sapling");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.UNFIRED_HOOKAH_BOWL.get())
                .pattern("C C")
                .pattern(" C ")
                .define('C', Items.CLAY_BALL)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("unfired_hookah_bowl_from_clay"));

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.UNFIRED_HOOKAH_BOWL.get()),
                        RecipeCategory.MISC, ModItems.HOOKAH_BOWL.get(), 0.1f, 200)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("hookah_bowl_from_smelting_unfired_hookah_bowl"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.KAIFOMETER.get())
                .pattern("II ")
                .pattern("IRI")
                .pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("kaifometer_from_iron_and_redstone"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.UNDRIED_QUASIUM_BLOCK_ITEM.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.QUASIUMINGOT.get())
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("undried_quasium_block_from_quasium_ingots"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.QUASIUM_ANVIL_ITEM.get())
                .pattern("BBB")
                .pattern(" I ")
                .pattern("III")
                .define('B', ModBlocks.QUASIUM_BLOCK_ITEM.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("quasium_anvil_from_quasium_blocks"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.NETHERITE_HAMMER.get())
                .pattern("NN")
                .pattern("NS")
                .pattern(" S")
                .define('N', Items.NETHERITE_INGOT)
                .define('S', Items.STICK)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("netherite_hammer_from_netherite_ingot"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RAW_BRIQUETTE.get(), 3)
                .requires(Ingredient.of(Items.CHARCOAL, Items.COAL))
                .requires(Ingredient.of(Items.CHARCOAL, Items.COAL))
                .requires(Items.CLAY_BALL)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("raw_briquette_from_charcoal_and_clay"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModItems.RAW_BRIQUETTE.get()), RecipeCategory.MISC, ModItems.CHARCOAL_22.get(), 3)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("charcoal_22_from_briquette_stonecutting"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModItems.RAW_BRIQUETTE.get()), RecipeCategory.MISC, ModItems.CHARCOAL_25.get(), 2)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("charcoal_25_from_briquette_stonecutting"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ModItems.RAW_BRIQUETTE.get()), RecipeCategory.MISC, ModItems.CHARCOAL_27.get(), 1)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("charcoal_27_from_briquette_stonecutting"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CALLOUD.get())
                .pattern("ICI")
                .pattern(" C ")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("calloud_from_iron_and_copper"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HOOKAH_BLOCK_ITEM.get())
                .pattern(" I ")
                .pattern("CIC")
                .pattern("GGG")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("hookah_block"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.HOOKAH_PIPE.get())
                .pattern("  N")
                .pattern(" L ")
                .pattern("L  ")
                .define('N', Items.IRON_NUGGET)
                .define('L', Items.LEATHER)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("hookah_pipe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HEATER_ITEM.get())
                .pattern("BBB")
                .pattern("BCB")
                .pattern("III")
                .define('B', Items.BRICK)
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("heater_from_brick_copper_iron"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FERMENTATION_BARREL_ITEM.get())
                .pattern("PPP")
                .pattern("PIP")
                .pattern("PPP")
                .define('P', Ingredient.of(ItemTags.PLANKS))
                .define('I', Items.IRON_INGOT)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("fermentation_barrel_from_planks_and_iron"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DRYING_SHELF_ITEM.get())
                .pattern("PSP")
                .pattern("P P")
                .pattern("PSP")
                .define('P', Ingredient.of(ItemTags.PLANKS))
                .define('S', Items.STICK)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("drying_shelf_from_planks_and_sticks"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SOAKING_BOWL_ITEM.get())
                .pattern("B B")
                .pattern("BCB")
                .define('B', Items.BRICK)
                .define('C', Items.CLAY_BALL)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("soaking_bowl_from_brick_and_clay"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BONSAI_POT_ITEM.get())
                .pattern("B B")
                .pattern("BDB")
                .define('B', Items.BRICK)
                .define('D', Items.DIRT)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("bonsai_pot_from_bricks_and_dirt"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ESSENCE_PRESS_ITEM.get())
                .pattern("IPI")
                .pattern("SCS")
                .pattern("SSS")
                .define('I', Items.IRON_INGOT)
                .define('P', Items.PISTON)
                .define('S', Ingredient.of(ItemTags.PLANKS))
                .define('C', Items.CAULDRON)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("essence_press"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.HOOKAH_DIFFUSER.get())
                .pattern(" I ").pattern("III").pattern(" I ")
                .define('I', Items.IRON_NUGGET)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("hookah_diffuser"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.HOOKAH_COOLER.get())
                .pattern("ICI").pattern("CBC").pattern("ICI")
                .define('I', Items.IRON_NUGGET).define('C', Items.COPPER_INGOT).define('B', Items.BLUE_ICE)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("hookah_cooler"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.HOOKAH_ICE_CHAMBER.get())
                .pattern("GGG").pattern("GIG").pattern("GGG")
                .define('G', Items.GLASS).define('I', Items.PACKED_ICE)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("hookah_ice_chamber"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PROGRESSION_BOOK.get())
                .requires(Items.BOOK)
                .requires(ModItems.TOBACCO_SEEDS.get())
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("progression_book_from_book_and_tobacco_seeds"));

        addBulbRecipe(pRecipeOutput, ModBlocks.MODULAR_HOOKAH_WHITE_ITEM.get(), Items.WHITE_DYE, "white");
        addBulbRecipe(pRecipeOutput, ModBlocks.MODULAR_HOOKAH_BLACK_ITEM.get(), Items.BLACK_DYE, "black");
        addBulbRecipe(pRecipeOutput, ModBlocks.MODULAR_HOOKAH_RED_ITEM.get(), Items.RED_DYE, "red");
        addBulbRecipe(pRecipeOutput, ModBlocks.MODULAR_HOOKAH_BLUE_ITEM.get(), Items.BLUE_DYE, "blue");
        addBulbRecipe(pRecipeOutput, ModBlocks.MODULAR_HOOKAH_AMBER_ITEM.get(), Items.ORANGE_DYE, "amber");

        addShaftRecipe(pRecipeOutput, ModItems.MODULAR_SHAFT_WHITE.get(), Items.WHITE_DYE, "white");
        addShaftRecipe(pRecipeOutput, ModItems.MODULAR_SHAFT_BLACK.get(), Items.BLACK_DYE, "black");
        addShaftRecipe(pRecipeOutput, ModItems.MODULAR_SHAFT_RED.get(), Items.RED_DYE, "red");
        addShaftRecipe(pRecipeOutput, ModItems.MODULAR_SHAFT_BLUE.get(), Items.BLUE_DYE, "blue");
        addShaftRecipe(pRecipeOutput, ModItems.MODULAR_SHAFT_AMBER.get(), Items.ORANGE_DYE, "amber");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DISTILLATION_CUBE_ITEM.get())
                .pattern("CGC")
                .pattern("C C")
                .pattern("BFB")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS_BOTTLE)
                .define('B', Items.BRICKS)
                .define('F', Items.FURNACE)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("distillation_cube"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NOTE_JAR_ITEM.get(), 2)
                .pattern(" W ")
                .pattern("G G")
                .pattern("GGG")
                .define('W', ItemTags.WOODEN_SLABS)
                .define('G', Items.GLASS)
                .unlockedBy(STEP, ON_STEP)
                .save(pRecipeOutput, recipeId("note_jar"));
    }

    private void addBulbRecipe(RecipeOutput output, net.minecraft.world.level.ItemLike result,
                               net.minecraft.world.item.Item dye, String color) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("GDG")
                .pattern("G G")
                .pattern("NGN")
                .define('G', Items.GLASS)
                .define('D', dye)
                .define('N', Items.GOLD_NUGGET)
                .unlockedBy(STEP, ON_STEP)
                .save(output, recipeId("modular_hookah_" + color));
    }

    private void addShaftRecipe(RecipeOutput output, net.minecraft.world.level.ItemLike result,
                                net.minecraft.world.item.Item dye, String color) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("IDI")
                .pattern(" I ")
                .pattern("INI")
                .define('I', Items.IRON_NUGGET)
                .define('D', dye)
                .define('N', Items.GOLD_NUGGET)
                .unlockedBy(STEP, ON_STEP)
                .save(output, recipeId("modular_shaft_" + color));
    }
}
