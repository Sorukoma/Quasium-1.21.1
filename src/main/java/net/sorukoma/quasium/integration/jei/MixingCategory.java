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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.MixingBlocks;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.mind.client.ResearchLocks;
import net.sorukoma.quasium.recipe.MixingRecipe;

public class MixingCategory implements IRecipeCategory<MixingRecipe> {
    public static final RecipeType<MixingRecipe> RECIPE_TYPE = RecipeType.create(QuasiumMod.MOD_ID, "mixing", MixingRecipe.class);
    private static final int WIDTH = 118;
    private static final int TEXT_HEIGHT = 34;
    private static final int HEIGHT = TEXT_HEIGHT + ResearchLocks.RESERVED_HEIGHT;

    private final Component title = Component.translatable("quasium.jei.category.mixing");
    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public MixingCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(MixingBlocks.MIXING_TABLE_ITEM.get());
        plus = guiHelper.getRecipePlusSign();
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<MixingRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, MixingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 5).setStandardSlotBackground().addItemStack(recipe.bottle());
        builder.addSlot(RecipeIngredientRole.INPUT, 41, 5).setStandardSlotBackground().addItemStack(new ItemStack(recipe.catalyst()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 5).setOutputSlotBackground().addItemStack(new ItemStack(recipe.result()));
    }

    @Override
    public void draw(MixingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        plus.draw(graphics, 22, 7);
        arrow.draw(graphics, 63, 5);
        NoteType note = NoteType.byId(recipe.essence());
        int color = note == null ? 0xFFA0A0A0 : 0xFF000000 | note.color();
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.quasium.ml", recipe.ml()), 1, 25, color, true);
        recipe.research().ifPresent(node -> ResearchLocks.draw(graphics, node, 1, TEXT_HEIGHT + 2, WIDTH - 1));
    }
}
