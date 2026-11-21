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
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.tea.SamovarBrew;
import net.sorukoma.quasium.tea.TeaBlocks;

import java.util.List;

public class SamovarCategory implements IRecipeCategory<SamovarBrew> {
    public static final RecipeType<SamovarBrew> RECIPE_TYPE = RecipeType.create(QuasiumMod.MOD_ID, "samovar", SamovarBrew.class);
    private static final int WIDTH = 150;
    private static final int HEIGHT = 64;
    private static final int TEXT_Y = 44;
    private static final int TEXT_COLOR = 0xFF404040;

    private final Component title = Component.translatable("quasium.jei.category.samovar");
    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public SamovarCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(TeaBlocks.SAMOVAR_ITEM.get());
        plus = guiHelper.getRecipePlusSign();
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<SamovarBrew> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, SamovarBrew brew, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 5).setStandardSlotBackground().addItemStack(brew.leaves());
        builder.addSlot(RecipeIngredientRole.INPUT, 41, 5).setStandardSlotBackground().addItemStack(brew.emptyTeapot());
        builder.addSlot(RecipeIngredientRole.CATALYST, 1, 24).setStandardSlotBackground().addItemStack(brew.water());
        builder.addSlot(RecipeIngredientRole.CATALYST, 21, 24).setStandardSlotBackground().addItemStack(brew.fuel());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 5).setOutputSlotBackground().addItemStack(brew.fullTeapot());
    }

    @Override
    public void draw(SamovarBrew brew, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        plus.draw(graphics, 22, 7);
        arrow.draw(graphics, 63, 5);
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(brew.summary(), WIDTH);
        for (int line = 0; line < lines.size(); line++) {
            graphics.drawString(font, lines.get(line), 0, TEXT_Y + line * font.lineHeight, TEXT_COLOR, false);
        }
    }
}
