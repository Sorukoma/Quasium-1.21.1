package net.sorukoma.quasium.client.gui.book;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import net.sorukoma.quasium.ritual.RitualRecipe;

import java.util.List;

final class BookRecipes {
    static final int ANVIL_WIDTH = 136;
    static final int ANVIL_HEIGHT = 40;
    private static final int OUTPUT = 26;
    private static final int RITUAL_HEAD = 30;
    private static final int RITUAL_HEAD_WIDTH = 80;
    private static final int RITUAL_ROW = 7;

    private BookRecipes() {
    }

    static boolean supports(Recipe<?> recipe) {
        return recipe instanceof CraftingRecipe || recipe instanceof AbstractCookingRecipe || recipe instanceof SingleItemRecipe;
    }

    static int width(Recipe<?> recipe) {
        return recipe instanceof CraftingRecipe ? 116 : 82;
    }

    static int height(Recipe<?> recipe) {
        if (recipe instanceof CraftingRecipe) return 58;
        return recipe instanceof AbstractCookingRecipe ? 40 : 30;
    }

    static void draw(GuiGraphics graphics, Font font, RecipeHolder<?> holder, int x, int y, BookHover hover) {
        Recipe<?> recipe = holder.value();
        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        if (recipe instanceof CraftingRecipe crafting) {
            grid(graphics, font, crafting, x, y + 2, hover);
            arrow(graphics, x + 60, y + 21);
            output(graphics, font, result, x + 90, y + 16, hover);
        } else if (recipe instanceof AbstractCookingRecipe) {
            input(graphics, font, cycle(List.of(recipe.getIngredients().get(0).getItems())), x, y + 2, hover);
            graphics.blitSprite(BookArt.FLAME, x + 2, y + 22, BookArt.FLAME_SIZE, BookArt.FLAME_SIZE);
            arrow(graphics, x + 24, y + 11);
            output(graphics, font, result, x + 54, y + 6, hover);
        } else {
            input(graphics, font, cycle(List.of(recipe.getIngredients().get(0).getItems())), x, y + 6, hover);
            arrow(graphics, x + 24, y + 7);
            output(graphics, font, result, x + 54, y + 2, hover);
        }
    }

    static void drawAnvil(GuiGraphics graphics, Font font, AnvilRecipes.AnvilRecipe recipe, int x, int y, BookHover hover) {
        for (int slot = 0; slot < 3; slot++) {
            int slotX = x + slot * 20;
            BookArt.slot(graphics, slotX, y + 4, BookArt.SLOT);
            if (slot < recipe.inputs().size()) item(graphics, font, cycle(recipe.shownIn(slot)), slotX + 1, y + 5, hover);
        }
        item(graphics, font, new ItemStack(ModItems.NETHERITE_HAMMER.get()), x + 62, y + 5, hover);
        arrow(graphics, x + 82, y + 5);
        output(graphics, font, new ItemStack(recipe.output().get()), x + 110, y, hover);
        graphics.drawString(font, recipe.bookInfo(), x, y + 30, BookArt.palette().muted(), false);
    }

    static int ritualHeight(RitualRecipe recipe) {
        return RITUAL_HEAD + (rows(recipe.pedestals().size()) + rows(recipe.notes().size())) * BookArt.SLOT + 2;
    }

    static void drawRitual(GuiGraphics graphics, Font font, RitualRecipe recipe, int x, int y, int width, BookHover hover) {
        int left = x + (width - RITUAL_HEAD_WIDTH) / 2;
        ItemStack catalyst = cycle(List.of(recipe.catalyst().getItems()));
        input(graphics, font, catalyst, left, y + 4, hover);
        arrow(graphics, left + 24, y + 5);
        output(graphics, font, recipe.previewFor(catalyst), left + 54, y, hover);
        int pedestalsEnd = slotRows(graphics, font, recipe.pedestals().stream().map(ingredient -> List.of(ingredient.getItems())).toList(),
                x, y + RITUAL_HEAD, width, hover);
        slotRows(graphics, font, recipe.notes().entrySet().stream()
                .map(note -> List.of(EssenceBottleItem.of(note.getKey().id(), note.getValue()))).toList(), x, pedestalsEnd, width, hover);
    }

    private static int slotRows(GuiGraphics graphics, Font font, List<List<ItemStack>> slots, int x, int y, int width, BookHover hover) {
        for (int index = 0; index < slots.size(); index++) {
            int row = index / RITUAL_ROW;
            int inRow = Math.min(RITUAL_ROW, slots.size() - row * RITUAL_ROW);
            int slotX = x + (width - inRow * BookArt.SLOT) / 2 + index % RITUAL_ROW * BookArt.SLOT;
            int slotY = y + row * BookArt.SLOT;
            BookArt.slot(graphics, slotX, slotY, BookArt.SLOT);
            item(graphics, font, cycle(slots.get(index)), slotX + 1, slotY + 1, hover);
        }
        return y + rows(slots.size()) * BookArt.SLOT;
    }

    private static int rows(int slots) {
        return (slots + RITUAL_ROW - 1) / RITUAL_ROW;
    }

    static void item(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, BookHover hover) {
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        hover.item(x, y, stack);
    }

    private static void grid(GuiGraphics graphics, Font font, CraftingRecipe recipe, int x, int y, BookHover hover) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + col * BookArt.SLOT;
                int slotY = y + row * BookArt.SLOT;
                BookArt.slot(graphics, slotX, slotY, BookArt.SLOT);
                int index = row * width + col;
                if (col < width && index < ingredients.size()) {
                    item(graphics, font, cycle(List.of(ingredients.get(index).getItems())), slotX + 1, slotY + 1, hover);
                }
            }
        }
    }

    private static void input(GuiGraphics graphics, Font font, ItemStack shown, int x, int y, BookHover hover) {
        BookArt.slot(graphics, x, y, BookArt.SLOT);
        item(graphics, font, shown, x + 1, y + 1, hover);
    }

    private static void output(GuiGraphics graphics, Font font, ItemStack result, int x, int y, BookHover hover) {
        BookArt.slot(graphics, x, y, OUTPUT);
        item(graphics, font, result, x + 5, y + 5, hover);
    }

    private static void arrow(GuiGraphics graphics, int x, int y) {
        graphics.blitSprite(BookArt.ARROW, x, y, BookArt.ARROW_W, BookArt.ARROW_H);
    }

    private static ItemStack cycle(List<ItemStack> options) {
        return options.isEmpty() ? ItemStack.EMPTY : options.get((int) (Util.getMillis() / 1000L % options.size()));
    }
}
