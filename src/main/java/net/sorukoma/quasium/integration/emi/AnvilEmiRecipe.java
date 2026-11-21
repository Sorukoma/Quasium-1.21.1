package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.recipe.AnvilRecipes;

import java.util.ArrayList;
import java.util.List;

public class AnvilEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 60;
    private final int inputCount;
    private final int hitsRequired;
    private final Component kaifRule;

    public AnvilEmiRecipe(AnvilRecipes.AnvilRecipe recipe, int index) {
        super(QuasiumEmiCategories.ANVIL,
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/anvil/" + index),
                WIDTH, HEIGHT);

        List<EmiIngredient> in = new ArrayList<>();
        for (int i = 0; i < recipe.inputs().size(); i++) {
            in.add(EmiIngredient.of(recipe.shownIn(i).stream().map(EmiStack::of).toList()));
        }
        this.inputs = in;
        this.inputCount = in.size();

        EmiStack hammer = EmiStack.of(ModItems.NETHERITE_HAMMER.get());
        this.catalysts = List.of(hammer);

        this.outputs = List.of(EmiStack.of(recipe.output().get()));
        this.hitsRequired = recipe.hitsRequired();
        this.kaifRule = recipe.kaifRule();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 0;
        for (int i = 0; i < inputCount; i++) {
            widgets.addSlot(inputs.get(i), x, 5);
            x += 20;
        }
        // Empty markers for the remaining input slots (anvil has 3)
        for (int i = inputCount; i < 3; i++) {
            widgets.addSlot(x, 5);
            x += 20;
        }

        // Hammer catalyst slot
        widgets.addSlot(catalysts.get(0), x, 5);

        // Output slot
        widgets.addSlot(outputs.get(0), WIDTH - 20, 5).recipeContext(this);

        widgets.addText(Component.literal(hitsRequired + " hits"), 40, 38, 0xFF808080, false);
        widgets.addText(kaifRule, 40, 48, 0xFF808080, false);
    }
}
