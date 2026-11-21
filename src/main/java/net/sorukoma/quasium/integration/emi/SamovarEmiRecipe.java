package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.tea.SamovarBrew;
import net.sorukoma.quasium.tea.TeaBlocks;

import java.util.List;

public class SamovarEmiRecipe extends BasicEmiRecipe {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "samovar"), EmiStack.of(TeaBlocks.SAMOVAR_ITEM.get()));
    private static final int WIDTH = 150;
    private static final int HEIGHT = 64;
    private static final int TEXT_Y = 44;
    private static final int TEXT_COLOR = 0xFF404040;

    private final SamovarBrew brew;

    public SamovarEmiRecipe(SamovarBrew brew) {
        super(CATEGORY, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/samovar/teapot"), WIDTH, HEIGHT);
        this.brew = brew;
        inputs = List.of(EmiStack.of(brew.leaves()), EmiStack.of(brew.emptyTeapot()));
        catalysts = List.of(EmiStack.of(brew.water()), EmiStack.of(brew.fuel()));
        outputs = List.of(EmiStack.of(brew.fullTeapot()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 4);
        widgets.addTexture(EmiTexture.PLUS, 22, 7);
        widgets.addSlot(inputs.get(1), 40, 4);
        widgets.addSlot(catalysts.get(0), 0, 23).catalyst(true);
        widgets.addSlot(catalysts.get(1), 20, 23).catalyst(true);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 63, 5);
        widgets.addSlot(outputs.get(0), 91, 0).large(true).recipeContext(this);
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(brew.summary(), WIDTH);
        for (int line = 0; line < lines.size(); line++) {
            widgets.addText(lines.get(line), 0, TEXT_Y + line * font.lineHeight, TEXT_COLOR, false);
        }
    }
}
