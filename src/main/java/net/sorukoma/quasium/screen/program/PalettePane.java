package net.sorukoma.quasium.screen.program;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.golem.program.BlockGroup;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramUnlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PalettePane {
    public static final int LEFT = 7;
    public static final int TOP = 17;
    public static final int WIDTH = 162;
    public static final int HEIGHT = 123;
    private static final int HEADER = 12;
    private static final int ENTRY_GAP = 3;
    private static final int GROUP_GAP = 3;
    private static final int CHIP = 6;

    public record Entry(BlockKind kind, Placed placed, int top) {
    }

    private record Header(BlockGroup group, int top) {
    }

    private final ScrollBox box = new ScrollBox(LEFT, TOP, WIDTH, HEIGHT);
    private final List<Entry> entries = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();
    private final Font font;

    public PalettePane(Font font) {
        this.font = font;
        int y = 0;
        for (BlockGroup group : BlockGroup.values()) {
            headers.add(new Header(group, y));
            y += HEADER;
            for (BlockKind kind : BlockKind.values()) {
                if (kind.group() != group) continue;
                BlockLayout.Result layout = BlockLayout.single(ProgramBlock.of(kind), font, true);
                entries.add(new Entry(kind, layout.placed().getFirst(), y));
                y += layout.height() + ENTRY_GAP;
            }
            y += GROUP_GAP;
        }
        box.content(y - ENTRY_GAP - GROUP_GAP);
    }

    public void place(int guiLeft, int guiTop) {
        box.place(guiLeft, guiTop);
    }

    public boolean contains(double mouseX, double mouseY) {
        return box.contains(mouseX, mouseY);
    }

    public void scrollBy(double amount) {
        box.scrollBy(amount);
    }

    public int screenX(Entry entry) {
        return box.originX() + entry.placed().x();
    }

    public int screenY(Entry entry) {
        return box.originY() + entry.top() + entry.placed().y();
    }

    @Nullable
    public Entry entry(BlockKind kind) {
        return entries.stream().filter(entry -> entry.kind == kind).findFirst().orElse(null);
    }

    @Nullable
    public Entry hit(double mouseX, double mouseY) {
        if (!box.contains(mouseX, mouseY)) return null;
        int x = (int) Math.floor(mouseX) - box.originX();
        int y = (int) Math.floor(mouseY) - box.originY();
        for (Entry entry : entries) {
            if (entry.placed().onPlate(x, y - entry.top()) || entry.placed().partAt(x, y - entry.top()) != null) return entry;
        }
        return null;
    }

    public void render(GuiGraphics graphics, BlockPainter painter, Player player, @Nullable Entry hovered) {
        box.clip(graphics);
        int x = box.originX();
        int y = box.originY();
        for (Header header : headers) {
            boolean open = ProgramUnlocks.mayUse(player, header.group);
            int chipY = y + header.top + 2;
            graphics.fill(x, chipY, x + CHIP, chipY + CHIP, open ? BlockPainter.groupColor(header.group) : 0xFF7A7A7A);
            graphics.drawString(font, I18n.get(header.group.key()), x + CHIP + 3, y + header.top + 1, open ? 0xFFFFFFFF : 0xFFC8C8C8, true);
        }
        for (Entry entry : entries) {
            BlockPainter.Look look = ProgramUnlocks.mayUse(player, entry.kind) ? BlockPainter.Look.NORMAL : BlockPainter.Look.LOCKED;
            painter.paint(graphics, entry.placed, x, y + entry.top, look, null, false);
            if (entry == hovered) BlockPainter.highlight(graphics, entry.placed, x, y + entry.top, 0xA0FFFFFF);
        }
        graphics.disableScissor();
        box.bar(graphics);
    }
}
