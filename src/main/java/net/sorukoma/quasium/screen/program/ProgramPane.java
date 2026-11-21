package net.sorukoma.quasium.screen.program;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.sorukoma.quasium.golem.program.Program;
import net.sorukoma.quasium.golem.program.ProgramPath;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ProgramPane {
    public static final int LEFT = 174;
    public static final int TOP = 17;
    public static final int WIDTH = 239;
    public static final int HEIGHT = 191;
    private static final int TAIL = 16;
    private static final int RUNNING = 0xFFFFD84A;
    private static final Component EMPTY = Component.translatable("quasium.program.ui.empty");

    public record Hit(Placed placed, @Nullable Part part) {
    }

    public record Target(Spot spot, boolean valid) {
    }

    private final ScrollBox box = new ScrollBox(LEFT, TOP, WIDTH, HEIGHT);
    private final Font font;
    private BlockLayout.Result layout;
    @Nullable
    private Program laidOut;
    @Nullable
    private ProgramPath lifted;

    public ProgramPane(Font font) {
        this.font = font;
        layout = BlockLayout.program(Program.EMPTY, font);
    }

    public void place(int guiLeft, int guiTop) {
        box.place(guiLeft, guiTop);
    }

    public void update(Program program, @Nullable ProgramPath without) {
        if (program == laidOut && Objects.equals(without, lifted)) return;
        laidOut = program;
        lifted = without;
        Program shown = without == null ? program : program.remove(without).orElse(program);
        layout = BlockLayout.program(shown, font);
        box.content(layout.height() + TAIL);
    }

    public boolean contains(double mouseX, double mouseY) {
        return box.contains(mouseX, mouseY);
    }

    public void scrollBy(double amount) {
        box.scrollBy(amount);
    }

    public void scrollNearEdge(double mouseX, double mouseY, int zone, int speed) {
        if (!box.inColumn(mouseX)) return;
        if (mouseY < box.screenTop() + zone) box.scrollBy(-speed);
        else if (mouseY > box.screenBottom() - zone) box.scrollBy(speed);
    }

    public int screenX(Placed placed) {
        return box.originX() + placed.x();
    }

    public int screenY(Placed placed) {
        return box.originY() + placed.y();
    }

    @Nullable
    public Placed find(ProgramPath path) {
        for (Placed placed : layout.placed()) {
            if (placed.path().equals(path)) return placed;
        }
        return null;
    }

    @Nullable
    public Hit hit(double mouseX, double mouseY) {
        if (!box.contains(mouseX, mouseY)) return null;
        int x = (int) Math.floor(mouseX) - box.originX();
        int y = (int) Math.floor(mouseY) - box.originY();
        for (int index = layout.placed().size() - 1; index >= 0; index--) {
            Placed placed = layout.placed().get(index);
            Part part = placed.partAt(x, y);
            if (part != null || placed.onPlate(x, y)) return new Hit(placed, part);
        }
        return null;
    }

    @Nullable
    public Spot nearest(int ghostX, int ghostY) {
        int x = ghostX - box.originX();
        int y = ghostY - box.originY();
        Spot best = null;
        for (Spot spot : layout.spots()) {
            if (best == null || spot.distance(x, y) < best.distance(x, y)) best = spot;
        }
        return best;
    }

    public void render(GuiGraphics graphics, BlockPainter painter, @Nullable Hit hovered, long running, @Nullable Target target, boolean hint) {
        box.clip(graphics);
        int x = box.originX();
        int y = box.originY();
        for (int index = 0; index < layout.placed().size(); index++) {
            Placed placed = layout.placed().get(index);
            Part part = hovered != null && hovered.placed() == placed ? hovered.part() : null;
            painter.paint(graphics, placed, x, y, BlockPainter.Look.NORMAL, part, true);
            if (index < Long.SIZE && (running >>> index & 1) != 0) BlockPainter.highlight(graphics, placed, x, y, RUNNING);
        }
        if (target != null) BlockPainter.insertion(graphics, target.spot(), x, y, target.valid());
        graphics.disableScissor();
        box.bar(graphics);
        if (hint && layout.placed().isEmpty()) {
            graphics.drawWordWrap(font, EMPTY, box.originX() + 8, box.screenTop() + 40, box.innerWidth() - 16, 0xFFE6E6E6);
        }
    }
}
