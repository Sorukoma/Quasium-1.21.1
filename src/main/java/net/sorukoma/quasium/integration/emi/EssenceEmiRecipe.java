package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.recipe.EssenceRecipes;

import java.util.ArrayList;
import java.util.List;

public class EssenceEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 54;

    public EssenceEmiRecipe(EssenceRecipes.EssenceRecipe recipe, int index) {
        super(QuasiumEmiCategories.ESSENCE,
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/essence/" + index),
                WIDTH, HEIGHT);
        List<EmiIngredient> inputs = new ArrayList<>();
        inputs.add(EmiIngredient.of(EssenceRecipes.ingredientAlternatives(recipe).stream()
                .map(EmiStack::of)
                .toList()));
        inputs.add(EmiStack.of(recipe.bottle()));
        this.inputs = inputs;
        this.outputs = List.of(EmiStack.of(recipe.output()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 2, 18);
        widgets.addSlot(inputs.get(1), 24, 18);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 58, 20);
        widgets.addSlot(outputs.get(0), 92, 18).recipeContext(this);
        widgets.addText(Component.literal("×4"), 7, 38, 0xFF808080, false);
    }
}
