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
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class DistillationCategory implements IRecipeCategory<NoteProfile> {
    public static final RecipeType<NoteProfile> RECIPE_TYPE = RecipeType.create(QuasiumMod.MOD_ID, "distillation", NoteProfile.class);
    private static final int WIDTH = 150;
    private static final int HEIGHT = 44;
    private static final int OUTPUT_X = 52;
    private static final int OUTPUT_STEP = 24;

    private final Component title;
    private final IDrawable icon;
    private final IDrawable arrow;

    public DistillationCategory(IGuiHelper guiHelper) {
        title = Component.translatable("quasium.jei.category.distillation");
        icon = guiHelper.createDrawableItemLike(ModBlocks.DISTILLATION_CUBE_ITEM.get());
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<NoteProfile> getRecipeType() {
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

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, NoteProfile profile, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 10).addItemStacks(profile.sourceStacks());
        List<ItemStack> outputs = profile.essenceStacks();
        for (int i = 0; i < outputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + i * OUTPUT_STEP, 10).addItemStack(outputs.get(i));
        }
    }

    @Override
    public void draw(NoteProfile profile, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 24, 10);
        int i = 0;
        for (Map.Entry<NoteType, Integer> note : profile.notes().entrySet()) {
            Component ml = Component.translatable("gui.quasium.ml", note.getValue());
            graphics.drawString(Minecraft.getInstance().font, ml, OUTPUT_X + i * OUTPUT_STEP, 30, 0xFF000000 | note.getKey().color(), true);
            i++;
        }
    }
}
