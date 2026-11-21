package net.sorukoma.quasium.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.client.ClientProgressionData;
import net.sorukoma.quasium.client.gui.book.BookArt;
import net.sorukoma.quasium.client.gui.book.BookHover;
import net.sorukoma.quasium.client.gui.book.BookResearch;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ProgressionCategory;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

final class BookTabs {
    private static final int PROGRESS_COLOR = 0xA0A0A0;

    private final Predicate<ProgressionArticle> unlocked;
    private int frameX;
    private int frameY;
    private int frameW;

    BookTabs(Predicate<ProgressionArticle> unlocked) {
        this.unlocked = unlocked;
    }

    void place(int frameX, int frameY, int frameW) {
        this.frameX = frameX;
        this.frameY = frameY;
        this.frameW = frameW;
    }

    void render(GuiGraphics graphics, BookHover hover, ProgressionCategory selected) {
        for (ProgressionCategory tab : ProgressionCategory.values()) {
            if (!BookResearch.tabVisible(tab)) continue;
            int x = x(tab);
            int y = y(tab);
            BookArt.tab(graphics, x, y, tab.research(), y == frameY, tab == selected);
            graphics.renderFakeItem(new ItemStack(BuiltInRegistries.ITEM.get(tab.icon())), x + (tab.research() ? 6 : 10), y + 6);
            hover.text(left(tab), y, BookArt.TAB_W - BookArt.TAB_OVERLAP, BookArt.TAB_H, tooltip(tab));
        }
    }

    Optional<ProgressionCategory> at(double mouseX, double mouseY) {
        for (ProgressionCategory tab : ProgressionCategory.values()) {
            if (BookResearch.tabVisible(tab) && mouseX >= left(tab) && mouseX < left(tab) + BookArt.TAB_W - BookArt.TAB_OVERLAP
                    && mouseY >= y(tab) && mouseY < y(tab) + BookArt.TAB_H) {
                return Optional.of(tab);
            }
        }
        return Optional.empty();
    }

    private int x(ProgressionCategory tab) {
        return tab.research() ? frameX + frameW - BookArt.TAB_OVERLAP : frameX - BookArt.TAB_W + BookArt.TAB_OVERLAP;
    }

    private int left(ProgressionCategory tab) {
        return x(tab) + (tab.research() ? BookArt.TAB_OVERLAP : 0);
    }

    private int y(ProgressionCategory tab) {
        return frameY + ProgressionCategory.side(tab.research()).indexOf(tab) * BookArt.TAB_H;
    }

    private Component tooltip(ProgressionCategory tab) {
        if (tab == ProgressionCategory.PATCH_NOTES) return Component.translatable(tab.translationKey());
        List<ProgressionArticle> inTab = ClientProgressionData.getArticles().values().stream()
                .filter(candidate -> ProgressionBookScreen.categoryOf(candidate) == tab && !candidate.isSecret() && BookResearch.visible(candidate))
                .toList();
        long open = inTab.stream().filter(unlocked).count();
        return Component.translatable(tab.translationKey()).append("\n")
                .append(Component.translatable("quasium.progression.category_progress", open, inTab.size()).withColor(PROGRESS_COLOR));
    }
}
