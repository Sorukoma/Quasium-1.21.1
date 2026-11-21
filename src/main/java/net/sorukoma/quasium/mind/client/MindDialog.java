package net.sorukoma.quasium.mind.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.QuasiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(Dist.CLIENT)
abstract class MindDialog extends Screen {
    static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mind/panel");
    private static final int WIDTH = 220;
    private static final int PAD = 10;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;
    private static final int ICON = 16;
    private static final int LABEL = 0x404040;

    private List<FormattedCharSequence> lines = List.of();
    private List<Choice> options = List.of();
    private int left;
    private int top;
    private int panelHeight;
    private int optionsTop;

    MindDialog(Component title) {
        super(title);
    }

    abstract Component body();

    abstract List<Choice> choices();

    ResourceLocation panel() {
        return PANEL;
    }

    int ink() {
        return LABEL;
    }

    @Override
    protected void init() {
        lines = font.split(body(), WIDTH - 2 * PAD);
        options = choices();
        int indent = options.stream().anyMatch(choice -> choice.icon() != null) ? ICON + GAP : 0;
        panelHeight = PAD + 12 + lines.size() * 10 + PAD + options.size() * (BUTTON_H + GAP) + PAD - GAP;
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        optionsTop = top + PAD + 12 + lines.size() * 10 + PAD;
        for (int index = 0; index < options.size(); index++) {
            Choice choice = options.get(index);
            addRenderableWidget(Button.builder(choice.label(), button -> {
                choice.action().run();
                onClose();
            }).bounds(left + PAD + indent, optionsTop + index * (BUTTON_H + GAP), WIDTH - 2 * PAD - indent, BUTTON_H).build());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(panel(), left, top, WIDTH, panelHeight);
        graphics.drawString(font, title, left + PAD, top + PAD, ink(), false);
        int y = top + PAD + 12;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, left + PAD, y, ink(), false);
            y += 10;
        }
        for (int index = 0; index < options.size(); index++) {
            ResourceLocation icon = options.get(index).icon();
            if (icon != null) graphics.blitSprite(icon, left + PAD, optionsTop + index * (BUTTON_H + GAP) + (BUTTON_H - ICON) / 2, ICON, ICON);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    record Choice(Component label, Runnable action, @Nullable ResourceLocation icon) {
    }
}
