package net.sorukoma.quasium.client.gui.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class BookHover {
    private final int mouseX;
    private final int mouseY;
    private ItemStack stack = ItemStack.EMPTY;
    private Component text;

    public BookHover(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public boolean over(int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void item(int x, int y, ItemStack candidate) {
        if (!candidate.isEmpty() && over(x, y, 16, 16)) stack = candidate;
    }

    public void text(int x, int y, int width, int height, Component candidate) {
        if (over(x, y, width, height)) text = candidate;
    }

    public void render(GuiGraphics graphics, Font font) {
        if (!stack.isEmpty()) {
            graphics.renderTooltip(font, stack, mouseX, mouseY);
        } else if (text != null) {
            graphics.renderTooltip(font, font.split(text, 200), mouseX, mouseY);
        }
    }
}
