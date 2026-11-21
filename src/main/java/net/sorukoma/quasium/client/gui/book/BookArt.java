package net.sorukoma.quasium.client.gui.book;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.QuasiumClientConfig;

public final class BookArt {
    public static final ResourceLocation LOCK = ResourceLocation.withDefaultNamespace("container/cartography_table/locked");
    public static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("widget/scroller");
    public static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "book/arrow");
    public static final ResourceLocation FLAME = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    public static final ResourceLocation PAGE_FORWARD = ResourceLocation.withDefaultNamespace("recipe_book/page_forward");
    public static final ResourceLocation PAGE_FORWARD_LIT = ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted");
    public static final ResourceLocation PAGE_BACKWARD = ResourceLocation.withDefaultNamespace("recipe_book/page_backward");
    public static final ResourceLocation PAGE_BACKWARD_LIT = ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted");
    public static final int LOCK_W = 10;
    public static final int LOCK_H = 14;
    public static final int FRAME_SIDE = 9;
    public static final int FRAME_TOP = 18;
    public static final int TAB_W = 32;
    public static final int TAB_H = 28;
    public static final int TAB_OVERLAP = 4;
    public static final int SLOT = 18;
    public static final int ARROW_W = 24;
    public static final int ARROW_H = 16;
    public static final int FLAME_SIZE = 14;

    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int[] FOLD = {0x0C000000, 0x16000000, 0x22000000, 0x2E000000, 0x22000000, 0x16000000, 0x0C000000};

    private BookArt() {
    }

    public static BookPalette palette() {
        return QuasiumClientConfig.DARK_BOOK.get() ? BookPalette.DARK : BookPalette.LIGHT;
    }

    public static void slot(GuiGraphics graphics, int x, int y, int size) {
        int right = x + size - 1;
        int bottom = y + size - 1;
        graphics.fill(x, y, x + size, y + size, SLOT_FILL);
        graphics.fill(x, y, right, y + 1, SLOT_DARK);
        graphics.fill(x, y, x + 1, bottom, SLOT_DARK);
        graphics.fill(x + 1, bottom, x + size, bottom + 1, SLOT_LIGHT);
        graphics.fill(right, y + 1, right + 1, bottom + 1, SLOT_LIGHT);
    }

    public static void shadeIcon(GuiGraphics graphics, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.fill(x, y, x + 16, y + 16, palette().lockedShade());
        graphics.pose().popPose();
    }

    public static void fold(GuiGraphics graphics, int centerX, int top, int bottom) {
        int left = centerX - FOLD.length / 2;
        for (int i = 0; i < FOLD.length; i++) {
            graphics.fill(left + i, top, left + i + 1, bottom, FOLD[i]);
        }
    }

    public static void tab(GuiGraphics graphics, int x, int y, boolean right, boolean first, boolean selected) {
        String name = "advancements/tab_" + (right ? "right_" : "left_") + (first ? "top" : "middle") + (selected ? "_selected" : "");
        graphics.blitSprite(ResourceLocation.withDefaultNamespace(name), x, y, TAB_W, TAB_H);
    }
}
