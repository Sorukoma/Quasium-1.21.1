package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.recipe.DryingRecipes;

import java.util.List;

public class DryingEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 90;
    private static final int HEIGHT = 36;
    private final int dryingTime;

    public DryingEmiRecipe(DryingRecipes.DryingRecipe recipe, int index) {
        super(QuasiumEmiCategories.DRYING,
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/drying/" + index),
                WIDTH, HEIGHT);
        this.inputs = List.of(EmiStack.of(recipe.displayInput().get()));
        this.outputs = List.of(EmiStack.of(recipe.output().get()));
        this.dryingTime = recipe.dryingTime();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 9);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 26, 10);
        widgets.addSlot(outputs.get(0), 60, 9).recipeContext(this);

        int seconds = dryingTime / 20;
        widgets.addText(Component.literal(seconds + " s"), 30, 0, 0xFF808080, false);
    }
}
