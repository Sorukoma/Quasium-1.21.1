package net.sorukoma.quasium.client.gui.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import net.sorukoma.quasium.ritual.RitualRecipe;

import java.util.List;

public sealed interface BookPiece {
    int LINE = 10;
    int ICON_STEP = 20;

    int height();

    void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover);

    record Line(FormattedCharSequence text, int color) implements BookPiece {
        public int height() {
            return LINE;
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover) {
            graphics.drawString(font, text, x, y, color, false);
        }
    }

    record Gap() implements BookPiece {
        public int height() {
            return 5;
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover) {
        }
    }

    record Items(List<ItemStack> stacks) implements BookPiece {
        public int height() {
            return 20;
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover) {
            int left = x + (width - stacks.size() * ICON_STEP + 4) / 2;
            for (int i = 0; i < stacks.size(); i++) {
                BookRecipes.item(graphics, font, stacks.get(i), left + i * ICON_STEP, y + 2, hover);
            }
        }
    }

    record Recipe(RecipeHolder<?> holder) implements BookPiece {
        public int height() {
            return BookRecipes.height(holder.value());
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover) {
            BookRecipes.draw(graphics, font, holder, x + (width - BookRecipes.width(holder.value())) / 2, y, hover);
        }
    }

    record Anvil(AnvilRecipes.AnvilRecipe recipe) implements BookPiece {
        public int height() {
            return BookRecipes.ANVIL_HEIGHT;
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover) {
            BookRecipes.drawAnvil(graphics, font, recipe, x + (width - BookRecipes.ANVIL_WIDTH) / 2, y, hover);
        }
    }

    record Ritual(RitualRecipe recipe) implements BookPiece {
        public int height() {
            return BookRecipes.ritualHeight(recipe);
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover) {
            BookRecipes.drawRitual(graphics, font, recipe, x, y, width, hover);
        }
    }

    record Notice(Component text) implements BookPiece {
        public int height() {
            return 18;
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int width, BookHover hover) {
            graphics.blitSprite(BookArt.LOCK, x, y + 1, BookArt.LOCK_W, BookArt.LOCK_H);
            graphics.drawString(font, text, x + BookArt.LOCK_W + 5, y + 4, BookArt.palette().accent(), false);
        }
    }
}
