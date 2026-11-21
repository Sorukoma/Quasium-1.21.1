package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.sorukoma.quasium.client.RecipeGate;
import net.sorukoma.quasium.recipe.DryingRecipes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JeiRecipeGate {
    private static final Map<RecipeType<?>, ResourceLocation> STATIONS = Map.of(
            DryingCategory.RECIPE_TYPE, RecipeGate.DRYING,
            FermentationCategory.RECIPE_TYPE, RecipeGate.FERMENTATION,
            SoakingCategory.RECIPE_TYPE, RecipeGate.SOAKING,
            AnvilCategory.RECIPE_TYPE, RecipeGate.ANVIL,
            EssenceCategory.RECIPE_TYPE, RecipeGate.ESSENCE,
            DistillationCategory.RECIPE_TYPE, RecipeGate.DISTILLATION,
            MixingCategory.RECIPE_TYPE, RecipeGate.MIXING,
            KilnCategory.RECIPE_TYPE, RecipeGate.KILN,
            SamovarCategory.RECIPE_TYPE, RecipeGate.SAMOVAR);

    private static IRecipeManager manager;
    private static Set<ResourceLocation> hidden = Set.of();

    private JeiRecipeGate() {
    }

    static void attach(IJeiRuntime runtime) {
        manager = runtime.getRecipeManager();
        hidden = Set.of();
        RecipeGate.listen(JeiRecipeGate::apply);
    }

    static void detach() {
        RecipeGate.listen(null);
        manager = null;
        hidden = Set.of();
    }

    private static void apply(Set<ResourceLocation> locked) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (manager == null || connection == null) return;
        Set<ResourceLocation> toHide = new HashSet<>(locked);
        toHide.removeAll(hidden);
        Set<ResourceLocation> toShow = new HashSet<>(hidden);
        toShow.removeAll(locked);
        toHide.forEach(id -> connection.getRecipeManager().byKey(id).ifPresent(holder -> toggle(holder, true)));
        toShow.forEach(id -> connection.getRecipeManager().byKey(id).ifPresent(holder -> toggle(holder, false)));
        STATIONS.forEach((type, station) -> {
            if (locked.contains(station)) manager.hideRecipeCategory(type);
            else manager.unhideRecipeCategory(type);
        });
        DryingRecipes.getRecipes().stream().filter(recipe -> recipe.gate() != null)
                .forEach(recipe -> change(DryingCategory.RECIPE_TYPE, recipe, locked.contains(recipe.gate())));
        hidden = Set.copyOf(locked);
    }

    @SuppressWarnings("unchecked")
    private static void toggle(RecipeHolder<?> holder, boolean hide) {
        switch (holder.value()) {
            case CraftingRecipe ignored -> change(RecipeTypes.CRAFTING, (RecipeHolder<CraftingRecipe>) holder, hide);
            case SmeltingRecipe ignored -> change(RecipeTypes.SMELTING, (RecipeHolder<SmeltingRecipe>) holder, hide);
            case BlastingRecipe ignored -> change(RecipeTypes.BLASTING, (RecipeHolder<BlastingRecipe>) holder, hide);
            case StonecutterRecipe ignored -> change(RecipeTypes.STONECUTTING, (RecipeHolder<StonecutterRecipe>) holder, hide);
            default -> {
            }
        }
    }

    private static <T> void change(RecipeType<T> type, T recipe, boolean hide) {
        if (hide) manager.hideRecipes(type, List.of(recipe));
        else manager.unhideRecipes(type, List.of(recipe));
    }
}
