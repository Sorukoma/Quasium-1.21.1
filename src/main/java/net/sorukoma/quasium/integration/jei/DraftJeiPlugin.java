package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.draft.DraftCatalog;

@JeiPlugin
public class DraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "draft");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (DraftCatalog.Entry entry : DraftCatalog.registered()) {
            registration.addIngredientInfo(new ItemStack(entry.item()), VanillaTypes.ITEM_STACK, entry.lines().toArray(Component[]::new));
        }
    }
}
