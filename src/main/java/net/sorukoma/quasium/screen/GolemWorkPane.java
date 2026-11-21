package net.sorukoma.quasium.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.sorukoma.quasium.golem.CalloudGolemEntity;
import net.sorukoma.quasium.golem.ClayGolemEntity;
import net.sorukoma.quasium.golem.GolemJob;
import net.sorukoma.quasium.golem.GolemProfile;
import net.sorukoma.quasium.golem.MarkType;
import net.sorukoma.quasium.golem.WorkerGolem;
import net.sorukoma.quasium.golem.ZoneBox;

import java.util.List;

final class GolemWorkPane {
    private static final int LABEL = 0x404040;
    private static final int DIM = 0x707070;
    private static final int TEXT_X = 64;
    private static final int TEXT_RIGHT = 168;
    private static final int SMALL = 12;
    private static final int WIDE_X = GolemMenu.FEEDER_X + 22;

    private final GolemMenu menu;
    private final Button less;
    private final Button more;
    private final Button refill;
    private final Button toggle;
    private boolean shown;

    GolemWorkPane(GolemMenu menu) {
        this.menu = menu;
        less = Button.builder(Component.literal("-"), button -> press(GolemMenu.HERD_LESS)).size(SMALL, SMALL).build();
        more = Button.builder(Component.literal("+"), button -> press(GolemMenu.HERD_MORE)).size(SMALL, SMALL).build();
        refill = Button.builder(Component.translatable("quasium.golem.refill_now"), button -> press(GolemMenu.REFILL_NOW))
                .size(TEXT_RIGHT - WIDE_X, 18).build();
        toggle = Button.builder(Component.empty(), button -> press(GolemMenu.REFILL_TOGGLE)).size(TEXT_RIGHT - TEXT_X, 18).build();
    }

    List<Button> buttons() {
        return List.of(less, more, refill, toggle);
    }

    void place(int left, int top) {
        more.setPosition(left + TEXT_RIGHT - SMALL, top + 50);
        less.setPosition(left + TEXT_RIGHT - 2 * SMALL - 2, top + 50);
        refill.setPosition(left + WIDE_X, top + GolemMenu.FEEDER_Y - 1);
        toggle.setPosition(left + TEXT_X, top + 50);
    }

    void show(boolean visible) {
        shown = visible;
        tick();
    }

    void tick() {
        WorkerGolem golem = menu.golem();
        boolean herder = golem instanceof ClayGolemEntity && golem.job() == GolemJob.HERDER;
        boolean calloud = golem instanceof CalloudGolemEntity;
        less.visible = more.visible = shown && herder;
        refill.visible = toggle.visible = shown && calloud;
        refill.active = !menu.refillPaused();
        toggle.setMessage(Component.translatable(menu.refillPaused() ? "quasium.golem.refill_off" : "quasium.golem.refill_on"));
    }

    void renderLabels(GuiGraphics graphics, Font font) {
        WorkerGolem golem = menu.golem();
        if (golem == null) return;
        if (golem instanceof CalloudGolemEntity) {
            graphics.drawString(font, Component.translatable("quasium.golem.refill_title"), TEXT_X, 18, LABEL, false);
            int y = 29;
            for (FormattedCharSequence line : font.split(Component.translatable("quasium.golem.refill_hint"), TEXT_RIGHT - TEXT_X)) {
                graphics.drawString(font, line, TEXT_X, y, DIM, false);
                y += font.lineHeight;
            }
            return;
        }
        graphics.drawString(font, golem.job().title(), TEXT_X, 18, LABEL, false);
        Component tool = golem.tool().isEmpty() ? Component.translatable("quasium.golem.no_tool") : golem.tool().getHoverName();
        graphics.drawString(font, font.plainSubstrByWidth(tool.getString(), TEXT_RIGHT - TEXT_X), TEXT_X, 29, DIM, false);
        graphics.drawString(font, zoneLine(golem), TEXT_X, 40, LABEL, false);
        if (golem.job() == GolemJob.HERDER) {
            graphics.drawString(font, Component.translatable("quasium.golem.herd", menu.herdLimit()), TEXT_X, 52, LABEL, false);
        }
    }

    private Component zoneLine(WorkerGolem golem) {
        for (GolemProfile.Spot spot : menu.profile().bound()) {
            if (spot.type() != MarkType.ZONE) continue;
            BoundingBox box = ZoneBox.of(golem.level(), spot.pos());
            return Component.translatable("quasium.golem.zone", box.getXSpan(), box.getYSpan(), box.getZSpan());
        }
        return Component.translatable("quasium.golem.no_zone");
    }

    private void press(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
}
