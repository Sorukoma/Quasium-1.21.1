package net.sorukoma.quasium.integration.jei;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import net.sorukoma.quasium.recipe.DryingRecipes;
import net.sorukoma.quasium.recipe.EssenceRecipes;
import net.sorukoma.quasium.recipe.FermentationRecipes;
import net.sorukoma.quasium.recipe.SoakingRecipes;

@JeiPlugin
public class QuasiumJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new DryingCategory(guiHelper),
                new FermentationCategory(guiHelper),
                new SoakingCategory(guiHelper),
                new AnvilCategory(guiHelper),
                new EssenceCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        DryingRecipes.init();
        registration.addRecipes(DryingCategory.RECIPE_TYPE, DryingRecipes.getRecipes());
        registration.addRecipes(FermentationCategory.RECIPE_TYPE, FermentationRecipes.createRecipes());
        registration.addRecipes(SoakingCategory.RECIPE_TYPE, SoakingRecipes.createRecipes());
        AnvilRecipes.init();
        registration.addRecipes(AnvilCategory.RECIPE_TYPE, AnvilRecipes.getRecipes());
        registration.addRecipes(EssenceCategory.RECIPE_TYPE, EssenceRecipes.createRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DRYING_SHELF_ITEM.get()), DryingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FERMENTATION_BARREL_ITEM.get()), FermentationCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.NETHERITE_HAMMER.get()), FermentationCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SOAKING_BOWL_ITEM.get()), SoakingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.STICK), SoakingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.QUASIUM_ANVIL_ITEM.get()), AnvilCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.NETHERITE_HAMMER.get()), AnvilCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ESSENCE_PRESS_ITEM.get()), EssenceCategory.RECIPE_TYPE);
    }
}