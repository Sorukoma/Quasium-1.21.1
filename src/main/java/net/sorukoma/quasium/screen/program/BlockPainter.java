package net.sorukoma.quasium.screen.program;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.golem.program.BlockGroup;
import net.sorukoma.quasium.golem.program.MarkedPlace;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramBlocks;
import net.sorukoma.quasium.screen.ProgramTableMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static net.sorukoma.quasium.screen.program.BlockLayout.ARM;
import static net.sorukoma.quasium.screen.program.BlockLayout.ROW;

public final class BlockPainter {
    private static final int[] GROUP_COLORS = {0xFF3B8579, 0xFFB0742E, 0xFF8A4E77, 0xFFA63F35};
    private static final int LOCKED = 0xFF7A7A7A;
    private static final int PILL = 0xFFEDE6D3;
    private static final int INK = 0xFF35302A;
    private static final int FAR = 0xFFC8392E;
    private static final int HOVER = 0x50FFFFFF;
    private static final int DOOMED = 0x90B02020;
    private static final int MOUTH_HINT = 0x60FFFFFF;
    private static final float ICON = 0.75f;

    public enum Look {
        NORMAL, LOCKED, DOOMED
    }

    private record Colors(int base, int light, int dark, int socket) {
        static Colors of(int base) {
            return new Colors(base, FastColor.ARGB32.lerp(0.35f, base, 0xFFFFFFFF), FastColor.ARGB32.lerp(0.45f, base, 0xFF000000),
                    FastColor.ARGB32.lerp(0.3f, base, 0xFF000000));
        }
    }

    private final Font font;
    private final ProgramTableMenu menu;

    public BlockPainter(Font font, ProgramTableMenu menu) {
        this.font = font;
        this.menu = menu;
    }

    public static int groupColor(BlockGroup group) {
        return GROUP_COLORS[group.ordinal()];
    }

    public void paint(GuiGraphics graphics, Placed placed, int originX, int originY, Look look, @Nullable Part hovered, boolean mouthHints) {
        Colors colors = Colors.of(look == Look.LOCKED ? LOCKED : groupColor(placed.block().kind().group()));
        int x = originX + placed.x();
        int y = originY + placed.y();
        plate(graphics, placed, x, y, colors);
        if (mouthHints) mouthHints(graphics, placed, x, originY);
        for (Part part : placed.parts()) {
            int partX = originX + part.x();
            int partY = originY + part.y();
            part(graphics, placed.block(), part, partX, partY, colors, look == Look.LOCKED);
            if (part == hovered) graphics.fill(partX, partY, partX + part.width(), partY + Part.HEIGHT, HOVER);
        }
        if (look == Look.DOOMED) graphics.fill(x, y, x + placed.width(), y + placed.height(), DOOMED);
    }

    private void plate(GuiGraphics graphics, Placed placed, int x, int y, Colors colors) {
        int right = x + placed.width();
        int bottom = y + placed.height();
        graphics.fill(x, y, right, y + ROW, colors.base);
        graphics.fill(x, y, right, y + 1, colors.light);
        graphics.fill(x, y, x + 1, bottom, colors.light);
        graphics.fill(x + 6, y, x + 14, y + 1, colors.dark);
        graphics.fill(x + 6, bottom, x + 14, bottom + 1, colors.dark);
        if (placed.mouths().isEmpty()) {
            graphics.fill(right - 1, y + 1, right, bottom, colors.dark);
            graphics.fill(x + 1, bottom - 1, right, bottom, colors.dark);
            return;
        }
        graphics.fill(right - 1, y + 1, right, y + ROW, colors.dark);
        graphics.fill(x + ARM, y + ROW - 1, right - 1, y + ROW, colors.dark);
        int shift = y - placed.y();
        for (int index = 0; index < placed.mouths().size(); index++) {
            Placed.Mouth mouth = placed.mouths().get(index);
            graphics.fill(x + 1, mouth.top() + shift, x + ARM, mouth.bottom() + shift, colors.base);
            graphics.fill(x + ARM - 1, mouth.top() + shift, x + ARM, mouth.bottom() + shift, colors.dark);
            int barBottom = index + 1 < placed.mouths().size() ? placed.mouths().get(index + 1).top() + shift : bottom;
            bar(graphics, x, mouth.bottom() + shift, right, barBottom, colors);
        }
        graphics.fill(x + 1, bottom - 1, right, bottom, colors.dark);
    }

    private static void bar(GuiGraphics graphics, int x, int top, int right, int bottom, Colors colors) {
        graphics.fill(x + 1, top, right, bottom, colors.base);
        graphics.fill(x + ARM, top, right, top + 1, colors.light);
        graphics.fill(x + ARM, bottom - 1, right - 1, bottom, colors.dark);
        graphics.fill(right - 1, top + 1, right, bottom, colors.dark);
    }

    private static void mouthHints(GuiGraphics graphics, Placed placed, int x, int originY) {
        for (int body = 0; body < placed.mouths().size(); body++) {
            if (!placed.block().bodyOf(body).isEmpty()) continue;
            Placed.Mouth mouth = placed.mouths().get(body);
            int top = originY + mouth.top() + 2;
            int left = x + ARM + 2;
            graphics.renderOutline(left, top, 40, mouth.bottom() - mouth.top() - 4, MOUTH_HINT);
        }
    }

    private void part(GuiGraphics graphics, ProgramBlock block, Part part, int x, int y, Colors colors, boolean locked) {
        switch (part.kind()) {
            case TEXT -> graphics.drawString(font, part.text(), x, y + 2, locked ? 0xFFD8D8D8 : 0xFFFFFFFF, true);
            case PILL_TEXT -> graphics.drawString(font, part.text(), x, y + 2, INK, false);
            case NUMBER -> {
                graphics.fill(x + 1, y, x + part.width() - 1, y + Part.HEIGHT, PILL);
                graphics.fill(x, y + 1, x + part.width(), y + Part.HEIGHT - 1, PILL);
                graphics.drawString(font, part.text(), x + (part.width() - font.width(part.text())) / 2 + 1, y + 2, INK, false);
            }
            case CHECK -> hexagon(graphics, x, y, part.width());
            case SOCKET -> socket(graphics, block.place(part.corner()), part, x, y, colors);
            case FILTER -> filter(graphics, block.filter(), x, y, colors);
        }
    }

    private static void hexagon(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x + 2, y, x + width - 2, y + Part.HEIGHT, PILL);
        graphics.fill(x + 1, y + 2, x + width - 1, y + Part.HEIGHT - 2, PILL);
        graphics.fill(x, y + 4, x + width, y + Part.HEIGHT - 4, PILL);
    }

    private void socket(GuiGraphics graphics, Optional<MarkedPlace> place, Part part, int x, int y, Colors colors) {
        Level level = Minecraft.getInstance().level;
        boolean far = place.isPresent() && level != null && menu.far(level, place.get());
        well(graphics, x, y, part.width(), far ? FAR : colors.socket, colors);
        if (place.isPresent() && level != null) {
            icon(graphics, placeIcon(level, place.get()), x, y);
            return;
        }
        int ink = part.width() > Part.HEIGHT ? 0xFFFFFFFF : colors.light;
        graphics.drawString(font, part.text(), x + (part.width() - font.width(part.text())) / 2 + 1, y + 2, ink, false);
    }

    private void filter(GuiGraphics graphics, ItemStack filter, int x, int y, Colors colors) {
        well(graphics, x, y, Part.HEIGHT, colors.socket, colors);
        if (!filter.isEmpty()) {
            icon(graphics, filter, x, y);
            return;
        }
        for (int row = 0; row < 4; row++) graphics.fill(x + 2 + row, y + 3 + row, x + 10 - row, y + 4 + row, colors.light);
        graphics.fill(x + 5, y + 7, x + 7, y + 10, colors.light);
    }

    private static void well(GuiGraphics graphics, int x, int y, int width, int fill, Colors colors) {
        graphics.fill(x, y, x + width, y + Part.HEIGHT, fill);
        graphics.fill(x, y, x + width, y + 1, colors.dark);
        graphics.fill(x, y, x + 1, y + Part.HEIGHT, colors.dark);
    }

    private static void icon(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(ICON, ICON, 1f);
        graphics.renderFakeItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private static ItemStack placeIcon(Level level, MarkedPlace place) {
        if (place.where().dimension() == level.dimension() && level.isLoaded(place.where().pos())) {
            Item item = level.getBlockState(place.where().pos()).getBlock().asItem();
            if (item != Items.AIR) return new ItemStack(item);
        }
        return new ItemStack(ProgramBlocks.PLACE_MARKER.get());
    }

    public static void highlight(GuiGraphics graphics, Placed placed, int originX, int originY, int color) {
        int x = originX + placed.x() - 1;
        int y = originY + placed.y() - 1;
        graphics.renderOutline(x, y, placed.width() + 2, ROW + 2, color);
    }

    public static void insertion(GuiGraphics graphics, Spot spot, int originX, int originY, boolean valid) {
        int x = originX + spot.x();
        int y = originY + spot.y();
        int color = valid ? 0xFFFFFFFF : 0xFFE0453A;
        if (spot.mouth()) {
            graphics.fill(x + 2, y + 2, x + 42, y + BlockLayout.MOUTH - 2, valid ? 0xA0FFFFFF : 0xA0E0453A);
            return;
        }
        graphics.fill(x, y - 1, x + 48, y + 1, color);
        graphics.fill(x, y + 1, x + 48, y + 2, 0x80000000);
    }
}
