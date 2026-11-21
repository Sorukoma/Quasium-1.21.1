package net.sorukoma.quasium.mind.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ComaScreen extends Screen {
    private static final int FADE_TICKS = 60;
    private final int length;
    private int age;

    public ComaScreen(int lengthTicks) {
        super(Component.translatable("quasium.coma.title"));
        this.length = lengthTicks;
    }

    @Override
    public void tick() {
        if (++age > length + 40) onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float fade = Mth.clamp((age + partialTick) / FADE_TICKS, 0f, 1f);
        graphics.fill(0, 0, width, height, (int) (fade * 245) << 24);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (age < FADE_TICKS / 2) return;
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 12, 0x8A8A8A);
        int seconds = Math.max(0, (length - age) / 20);
        graphics.drawCenteredString(font, Component.translatable("quasium.coma.countdown", seconds), width / 2, height / 2 + 2, 0x5A5A5A);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
