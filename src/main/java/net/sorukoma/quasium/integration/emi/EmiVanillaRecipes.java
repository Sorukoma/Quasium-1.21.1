package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.sorukoma.quasium.QuasiumMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class EmiVanillaRecipes {
    private EmiVanillaRecipes() {
    }

    static void gate(EmiRegistry registry) {
        Set<EmiRecipeCategory> vanilla = Set.of(VanillaEmiRecipeCategories.CRAFTING, VanillaEmiRecipeCategories.SMELTING,
                VanillaEmiRecipeCategories.BLASTING, VanillaEmiRecipeCategories.STONECUTTING);
        registry.removeRecipes(recipe -> !(recipe instanceof GatedEmiRecipe) && recipe.getId() != null
                && QuasiumMod.MOD_ID.equals(recipe.getId().getNamespace()) && vanilla.contains(recipe.getCategory()));
        HolderLookup.Provider registries = Minecraft.getInstance().level.registryAccess();
        for (RecipeHolder<?> holder : registry.getRecipeManager().getRecipes()) {
            if (!QuasiumMod.MOD_ID.equals(holder.id().getNamespace())) continue;
            EmiRecipe recipe = wrap(holder, registries);
            if (recipe != null) registry.addRecipe(new GatedEmiRecipe(recipe, holder.id()));
        }
    }

    private static EmiRecipe wrap(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        EmiStack output = EmiStack.of(holder.value().getResultItem(registries));
        return switch (holder.value()) {
            case ShapedRecipe shaped -> new EmiCraftingRecipe(grid(shaped), output, holder.id(), false);
            case ShapelessRecipe shapeless -> new EmiCraftingRecipe(shapeless.getIngredients().stream().map(EmiIngredient::of).toList(),
                    output, holder.id(), true);
            case SmeltingRecipe smelting -> new Single(VanillaEmiRecipeCategories.SMELTING, holder.id(), smelting.getIngredients().get(0), output);
            case BlastingRecipe blasting -> new Single(VanillaEmiRecipeCategories.BLASTING, holder.id(), blasting.getIngredients().get(0), output);
            case StonecutterRecipe cutting -> new Single(VanillaEmiRecipeCategories.STONECUTTING, holder.id(), cutting.getIngredients().get(0), output);
            default -> null;
        };
    }

    private static List<EmiIngredient> grid(ShapedRecipe recipe) {
        List<EmiIngredient> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                boolean inside = row < recipe.getHeight() && col < recipe.getWidth();
                slots.add(inside ? EmiIngredient.of(recipe.getIngredients().get(row * recipe.getWidth() + col)) : EmiStack.EMPTY);
            }
        }
        return slots;
    }

    private static final class Single extends BasicEmiRecipe {
        Single(EmiRecipeCategory category, ResourceLocation id, Ingredient input, EmiStack output) {
            super(category, id, 82, 26);
            this.inputs = List.of(EmiIngredient.of(input));
            this.outputs = List.of(output);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(inputs.get(0), 0, 4);
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 24, 5);
            widgets.addSlot(outputs.get(0), 56, 0).large(true).recipeContext(this);
        }
    }
}
