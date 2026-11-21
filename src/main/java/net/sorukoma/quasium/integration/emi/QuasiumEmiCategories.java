package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;

/**
 * EMI recipe categories for Quasium. Category display names are resolved by EMI from the
 * translation key {@code emi.category.<namespace>.<path>} (see lang files).
 */
public class QuasiumEmiCategories {

    public static final EmiRecipeCategory DRYING = new EmiRecipeCategory(
            id("drying"), EmiStack.of(ModBlocks.DRYING_SHELF_ITEM.get()));

    public static final EmiRecipeCategory FERMENTATION = new EmiRecipeCategory(
            id("fermentation"), EmiStack.of(ModBlocks.FERMENTATION_BARREL_ITEM.get()));

    public static final EmiRecipeCategory SOAKING = new EmiRecipeCategory(
            id("soaking"), EmiStack.of(ModBlocks.SOAKING_BOWL_ITEM.get()));

    public static final EmiRecipeCategory ANVIL = new EmiRecipeCategory(
            id("anvil"), EmiStack.of(ModItems.NETHERITE_HAMMER.get()));

    public static final EmiRecipeCategory ESSENCE = new EmiRecipeCategory(
            id("essence"), EmiStack.of(ModBlocks.ESSENCE_PRESS_ITEM.get()));

    public static final EmiRecipeCategory DISTILLATION = new EmiRecipeCategory(
            id("distillation"), EmiStack.of(ModBlocks.DISTILLATION_CUBE_ITEM.get()));

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }

    private QuasiumEmiCategories() {}
}
