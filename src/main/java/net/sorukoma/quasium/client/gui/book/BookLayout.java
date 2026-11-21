package net.sorukoma.quasium.client.gui.book;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class BookLayout {
    private BookLayout() {
    }

    public static <T> List<List<T>> paginate(List<T> pieces, ToIntFunction<T> height, Predicate<T> isGap, int firstPage, int otherPages) {
        List<List<T>> pages = new ArrayList<>();
        List<T> page = new ArrayList<>();
        int used = 0;
        for (T piece : pieces) {
            boolean gap = isGap.test(piece);
            if (gap && page.isEmpty()) continue;
            int limit = pages.isEmpty() ? firstPage : otherPages;
            int size = height.applyAsInt(piece);
            if (used + size > limit && !page.isEmpty()) {
                pages.add(page);
                page = new ArrayList<>();
                used = 0;
                if (gap) continue;
            }
            page.add(piece);
            used += size;
        }
        while (!page.isEmpty() && isGap.test(page.get(page.size() - 1))) page.remove(page.size() - 1);
        if (!page.isEmpty() || pages.isEmpty()) pages.add(page);
        return pages;
    }
}
