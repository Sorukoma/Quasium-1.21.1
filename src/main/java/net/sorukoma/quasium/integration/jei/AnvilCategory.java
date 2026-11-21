package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.mind.client.ResearchLocks;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.Nullable;

public class AnvilCategory implements IRecipeCategory<AnvilRecipes.AnvilRecipe> {
    public static final RecipeType<AnvilRecipes.AnvilRecipe> RECIPE_TYPE =
            RecipeType.create(QuasiumMod.MOD_ID, "anvil", AnvilRecipes.AnvilRecipe.class);
    private static final int WIDTH = 120;
    private static final int TEXT_HEIGHT = 60;
    private static final int HEIGHT = TEXT_HEIGHT + ResearchLocks.RESERVED_HEIGHT;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public AnvilCategory(IGuiHelper guiHelper) {
        this.title = Component.translatable("quasium.jei.category.anvil");
        this.icon = guiHelper.createDrawableItemLike(ModItems.NETHERITE_HAMMER.get());
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<AnvilRecipes.AnvilRecipe> getRecipeType() {
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
    @Nullable
    public IDrawable getIcon() {
        return icon;
    }

    @SuppressWarnings("removal")
    @Override
    @Nullable
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilRecipes.AnvilRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        int x = 0;
        for (int i = 0; i < recipe.inputs().size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, x, 5).addItemStacks(recipe.shownIn(i));
            x += 20;
        }
        // Fill remaining slots with empty markers
        for (int i = recipe.inputs().size(); i < 3; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, x, 5);
            x += 20;
        }

        // Hammer catalyst slot
        builder.addSlot(RecipeIngredientRole.CATALYST, x, 5)
                .addItemStack(new ItemStack(ModItems.NETHERITE_HAMMER.get()));

        // Output slot
        builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 20, 5)
                .addItemStack(new ItemStack(recipe.output().get()));
    }

    @Override
    public void draw(AnvilRecipes.AnvilRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        String hitsText = recipe.hitsRequired() + " hits";
        guiGraphics.drawString(Minecraft.getInstance().font, hitsText, 40, 38, 0xFF808080, false);

        String kaifText = recipe.kaifRule().getString();
        guiGraphics.drawString(Minecraft.getInstance().font, kaifText, 40, 48, 0xFF808080, false);
        if (recipe.research() != null) ResearchLocks.draw(guiGraphics, recipe.research(), 0, TEXT_HEIGHT + 2, WIDTH);
    }
}
