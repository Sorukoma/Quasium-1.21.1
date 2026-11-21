package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.kiln.NoteInfusion;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.List;

public class KilnEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 64;
    private static final int TEXT_Y = 28;
    private static final int TEXT_COLOR = 0xFF404040;

    private final NoteType note;

    public KilnEmiRecipe(NoteType note) {
        super(KilnEmiPlugin.CATEGORY, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/kiln/" + note.id()), WIDTH, HEIGHT);
        this.note = note;
        ItemStack tool = NoteInfusion.exampleTool(note);
        this.inputs = List.of(EmiStack.of(EssenceBottleItem.of(note.id(), ConfigArgs.synced(Config.KILN_ML_PER_ITEM))), EmiStack.of(tool));
        this.outputs = List.of(EmiStack.of(NoteInfusion.infused(tool, note)));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 4);
        widgets.addTexture(EmiTexture.PLUS, 22, 7);
        widgets.addSlot(inputs.get(1), 40, 4);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 63, 5);
        widgets.addSlot(outputs.get(0), 91, 0).large(true).recipeContext(this);
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(NoteInfusion.effect(note), WIDTH);
        for (int line = 0; line < lines.size(); line++) {
            widgets.addText(lines.get(line), 0, TEXT_Y + line * font.lineHeight, TEXT_COLOR, false);
        }
    }
}
