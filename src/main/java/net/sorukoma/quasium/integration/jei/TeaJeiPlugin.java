package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.tea.SamovarBrew;
import net.sorukoma.quasium.tea.TeaBlocks;

import java.util.List;

@JeiPlugin
public class TeaJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "tea");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SamovarCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(SamovarCategory.RECIPE_TYPE, List.of(SamovarBrew.current()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(TeaBlocks.SAMOVAR_ITEM.get()), SamovarCategory.RECIPE_TYPE);
    }
}
