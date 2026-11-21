package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.RecipeGate;
import net.sorukoma.quasium.ritual.RitualRecipes;
import net.sorukoma.quasium.ritual.RitualRegistry;

import java.util.Objects;

@JeiPlugin
public class RitualJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual");
    private static final ResourceLocation BRAZIER_RECIPE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual_brazier");
    private static final int CHECK_EVERY = 10;

    private static IRecipeManager manager;
    private static Boolean hidden;
    private static boolean listening;
    private static int ticks;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RitualCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RitualCategory.RECIPE_TYPE, RitualRecipes.all().entrySet().stream()
                .map(entry -> new RitualCategory.Display(entry.getKey(), entry.getValue())).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(RitualRegistry.RITUAL_BRAZIER_ITEM.get().getDefaultInstance(), RitualCategory.RECIPE_TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        manager = runtime.getRecipeManager();
        hidden = null;
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener(RitualJeiPlugin::onClientTick);
    }

    @Override
    public void onRuntimeUnavailable() {
        manager = null;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (manager == null || ++ticks % CHECK_EVERY != 0) return;
        boolean locked = RecipeGate.isLocked(BRAZIER_RECIPE);
        if (Objects.equals(hidden, locked)) return;
        if (locked) manager.hideRecipeCategory(RitualCategory.RECIPE_TYPE);
        else manager.unhideRecipeCategory(RitualCategory.RECIPE_TYPE);
        hidden = locked;
    }
}
