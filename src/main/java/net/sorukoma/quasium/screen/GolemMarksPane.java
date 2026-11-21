package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.golem.GolemBindPacket;
import net.sorukoma.quasium.golem.GolemBindings;
import net.sorukoma.quasium.golem.GolemProfile;
import net.sorukoma.quasium.golem.MarkBlocks;
import net.sorukoma.quasium.golem.WorkerGolem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class GolemMarksPane extends GolemListPane {
    private static final int ROW = 20;
    private static final int BUTTON = 14;
    private static final int ICON = 16;
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x808080;
    private static final int FAR = 0xA03030;
    private static final Component BOUND = Component.translatable("quasium.golem.marks.bound");
    private static final Component NEARBY = Component.translatable("quasium.golem.marks.nearby");
    private static final Component NONE_BOUND = Component.translatable("quasium.golem.marks.none_bound");
    private static final Component NONE_NEARBY = Component.translatable("quasium.golem.marks.none_nearby", GolemBindings.markRadius());
    private static final Tooltip BIND = Tooltip.create(Component.translatable("quasium.golem.marks.bind"));
    private static final Tooltip UNBIND = Tooltip.create(Component.translatable("quasium.golem.marks.unbind"));

    private sealed interface Row permits Line, Mark {
    }

    private record Line(Component text, int color) implements Row {
    }

    private record Mark(GolemProfile.Spot spot, boolean bound) implements Row {
    }

    private final GolemMenu menu;
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private boolean shown;

    GolemMarksPane(GolemMenu menu) {
        super(ROW);
        this.menu = menu;
    }

    List<Button> createButtons() {
        buttons.clear();
        for (int slot = 0; slot < visibleRows(); slot++) {
            int pressed = slot;
            buttons.add(Button.builder(Component.empty(), button -> press(pressed)).size(BUTTON, BUTTON).build());
        }
        return buttons;
    }

    void show(boolean visible) {
        shown = visible;
        scrolled();
    }

    void rebuild(GolemProfile profile) {
        rows.clear();
        rows.add(new Line(BOUND, TEXT));
        if (profile.bound().isEmpty()) rows.add(new Line(NONE_BOUND, MUTED));
        profile.bound().forEach(spot -> rows.add(new Mark(spot, true)));
        rows.add(new Line(NEARBY, TEXT));
        if (profile.nearby().isEmpty()) rows.add(new Line(NONE_NEARBY, MUTED));
        profile.nearby().forEach(spot -> rows.add(new Mark(spot, false)));
        scrollTo(first);
    }

    @Override
    int rowCount() {
        return rows.size();
    }

    @Override
    void scrolled() {
        for (int slot = 0; slot < buttons.size(); slot++) {
            Button button = buttons.get(slot);
            Row row = first + slot < rows.size() ? rows.get(first + slot) : null;
            button.visible = shown && row instanceof Mark;
            if (!(row instanceof Mark mark)) continue;
            button.setPosition(left + LEFT + WIDTH - BUTTON, rowY(slot) + (ROW - BUTTON) / 2);
            button.setMessage(Component.literal(mark.bound() ? "-" : "+"));
            button.setTooltip(mark.bound() ? UNBIND : BIND);
        }
    }

    private void press(int slot) {
        if (first + slot < rows.size() && rows.get(first + slot) instanceof Mark mark) {
            PacketDistributor.sendToServer(new GolemBindPacket(menu.containerId, mark.spot().pos(), !mark.bound()));
        }
    }

    @Override
    void renderRow(GuiGraphics graphics, Font font, int row, int x, int y) {
        switch (rows.get(row)) {
            case Line line -> graphics.drawString(font, line.text(), x, y + (ROW - font.lineHeight) / 2 + 1, line.color(), false);
            case Mark mark -> {
                graphics.renderItem(new ItemStack(MarkBlocks.item(mark.spot().type())), x, y + 2);
                String where = font.plainSubstrByWidth(mark.spot().pos().toShortString(), WIDTH - ICON - BUTTON - 8);
                graphics.drawString(font, where, x + ICON + 4, y + 2, TEXT, false);
                int blocks = blocksAway(mark);
                if (blocks < 0) return;
                boolean far = blocks > GolemBindings.reach(mark.spot().type());
                graphics.drawString(font, Component.translatable(far ? "quasium.golem.marks.far" : "quasium.golem.marks.distance", blocks),
                        x + ICON + 4, y + 11, far ? FAR : MUTED, false);
            }
        }
    }

    private int blocksAway(Mark mark) {
        WorkerGolem golem = menu.golem();
        return golem == null ? -1 : (int) Math.round(Math.sqrt(golem.blockPosition().distSqr(mark.spot().pos())));
    }

    @Nullable
    Component hovered(double mouseX, double mouseY) {
        int slot = (int) Math.floor((mouseY - top - TOP) / ROW);
        if (mouseX < left + LEFT || mouseX >= left + LEFT + ICON || slot < 0 || slot >= visibleRows() || first + slot >= rows.size()) return null;
        return rows.get(first + slot) instanceof Mark mark ? MarkBlocks.item(mark.spot().type()).getDescription() : null;
    }
}
