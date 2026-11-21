package net.sorukoma.quasium.client.tetrad;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCompiler;
import net.sorukoma.quasium.magic.page.PagePackets;
import net.sorukoma.quasium.magic.page.SpellPage;
import net.sorukoma.quasium.magic.page.TetradContents;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PageWheelScreen extends Screen {
    private static final int SLOT = 26;
    private static final int HUB = 40;
    private static final int DEAD_ZONE = 16;
    private static final int SMALL_RADIUS = 60;
    private static final int LARGE_RADIUS = 76;
    private static final int TEXT_WIDTH = 160;
    private static final int MIN_TEXT_WIDTH = 70;
    private static final int SHADE = 0x60000000;
    private static final int WHITE = 0xFFFFFF;
    private static final int GRAY = 0xA0A0A0;

    private final TetradContents contents;
    private final boolean followsKey;
    private int hovered = -1;

    public PageWheelScreen(TetradContents contents, boolean followsKey) {
        super(Component.translatable("quasium.tetrad.wheel"));
        this.contents = contents;
        this.followsKey = followsKey;
    }

    @Override
    public void tick() {
        if (followsKey && !TetradKeys.flipHeld()) choose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        hovered = sectorAt(mouseX, mouseY);
        choose();
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (TetradKeys.FLIP.matches(keyCode, scanCode)) {
            choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, SHADE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        hovered = sectorAt(mouseX, mouseY);
        int centerX = width / 2;
        int centerY = height / 2;
        graphics.blitSprite(TetradArt.WHEEL_HUB, centerX - HUB / 2, centerY - HUB / 2, HUB, HUB);
        for (int index = 0; index < contents.size(); index++) slot(graphics, index);
        int shown = hovered >= 0 && !contents.page(hovered).isEmpty() ? hovered : contents.active();
        SpellPage page = contents.page(shown);
        if (page.isEmpty()) return;
        Component label = PageList.label(page);
        graphics.drawCenteredString(font, label, centerX, centerY - 6, WHITE);
        Optional<CompiledPage> compiled = PageCompiler.compile(page, 0, null);
        Set<NoteType> notes = compiled.map(CompiledPage::notes).orElse(Set.of());
        TetradArt.dots(graphics, notes, centerX - TetradArt.dotsWidth(notes) / 2, centerY + 5);
        if (compiled.isEmpty()) return;
        int textX = centerX + radius() + SLOT / 2 + 18;
        int textWidth = Math.min(TEXT_WIDTH, width - textX - 6);
        if (textWidth < MIN_TEXT_WIDTH) return;
        List<FormattedCharSequence> lines = font.split(PageFacts.describe(compiled.get()), textWidth);
        int y = centerY - lines.size() * 5;
        graphics.fill(textX - 3, y - 3, textX + textWidth + 3, y + lines.size() * 10 + 1, SHADE);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, textX, y, GRAY);
            y += 10;
        }
    }

    private void slot(GuiGraphics graphics, int index) {
        SpellPage page = contents.page(index);
        int x = slotX(index) - SLOT / 2;
        int y = slotY(index) - SLOT / 2;
        ResourceLocation frame = page.isEmpty() ? TetradArt.WHEEL_SLOT_EMPTY : index == hovered ? TetradArt.WHEEL_SLOT_HOVER : TetradArt.WHEEL_SLOT;
        graphics.blitSprite(frame, x, y, SLOT, SLOT);
        if (!page.isEmpty()) {
            ResourceLocation icon = page.isSeal() ? TetradArt.SEAL : TetradArt.form(page.form());
            graphics.blitSprite(icon, x + (SLOT - TetradArt.ICON) / 2, y + (SLOT - TetradArt.ICON) / 2, TetradArt.ICON, TetradArt.ICON);
        }
        if (index == contents.active() && !page.isEmpty()) graphics.blitSprite(TetradArt.RIBBON, x + SLOT - 6, y - 2, 5, 9);
        double angle = angleOf(index);
        int numberX = slotX(index) + (int) Math.round(Math.cos(angle) * (SLOT / 2 + 7));
        int numberY = slotY(index) + (int) Math.round(Math.sin(angle) * (SLOT / 2 + 7));
        graphics.drawCenteredString(font, String.valueOf(index + 1), numberX, numberY - 4, page.isEmpty() ? GRAY : WHITE);
    }

    private void choose() {
        if (hovered >= 0 && !contents.page(hovered).isEmpty() && hovered != contents.active()) PagePackets.sendSelect(hovered);
        onClose();
    }

    private int sectorAt(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) return -1;
        double turn = Math.atan2(dy, dx) + Math.PI / 2;
        double step = Math.PI * 2 / contents.size();
        return Math.floorMod((int) Math.round(Mth.positiveModulo(turn, Math.PI * 2) / step), contents.size());
    }

    private double angleOf(int index) {
        return -Math.PI / 2 + Math.PI * 2 * index / contents.size();
    }

    private int slotX(int index) {
        return width / 2 + (int) Math.round(Math.cos(angleOf(index)) * radius());
    }

    private int slotY(int index) {
        return height / 2 + (int) Math.round(Math.sin(angleOf(index)) * radius());
    }

    private int radius() {
        return contents.size() > 8 ? LARGE_RADIUS : SMALL_RADIUS;
    }
}
