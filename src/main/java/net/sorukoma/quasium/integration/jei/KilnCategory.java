package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.kiln.KilnBlocks;
import net.sorukoma.quasium.kiln.NoteInfusion;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.List;

public class KilnCategory implements IRecipeCategory<NoteType> {
    public static final RecipeType<NoteType> RECIPE_TYPE = RecipeType.create(QuasiumMod.MOD_ID, "kiln", NoteType.class);
    private static final int WIDTH = 150;
    private static final int HEIGHT = 64;
    private static final int TEXT_Y = 28;
    private static final int TEXT_COLOR = 0xFF404040;

    private final Component title = Component.translatable("quasium.jei.category.kiln");
    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public KilnCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(KilnBlocks.SMOKE_KILN_ITEM.get());
        plus = guiHelper.getRecipePlusSign();
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<NoteType> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, NoteType note, IFocusGroup focuses) {
        ItemStack tool = NoteInfusion.exampleTool(note);
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 5).setStandardSlotBackground()
                .addItemStack(EssenceBottleItem.of(note.id(), ConfigArgs.synced(Config.KILN_ML_PER_ITEM)));
        builder.addSlot(RecipeIngredientRole.INPUT, 41, 5).setStandardSlotBackground().addItemStack(tool);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 5).setOutputSlotBackground().addItemStack(NoteInfusion.infused(tool, note));
    }

    @Override
    public void draw(NoteType note, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        plus.draw(graphics, 22, 7);
        arrow.draw(graphics, 63, 5);
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(NoteInfusion.effect(note), WIDTH);
        for (int line = 0; line < lines.size(); line++) {
            graphics.drawString(font, lines.get(line), 0, TEXT_Y + line * font.lineHeight, TEXT_COLOR, false);
        }
    }
}
