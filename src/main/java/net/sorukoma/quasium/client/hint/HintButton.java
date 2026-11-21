package net.sorukoma.quasium.client.hint;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Locale;

final class HintButton extends AbstractButton {
    static final int SIZE = 20;
    private static final int IDLE_TOP = 0x60C0C0FF;
    private static final int IDLE_BOTTOM = 0x30C0C0FF;
    private static final int HOVER_TOP = 0xF0C0C0FF;
    private static final int HOVER_BOTTOM = 0xA0C0C0FF;
    private static final int CLICK_TOP = 0xFFFFFFFF;
    private static final int CLICK_BOTTOM = 0xDDFFFFFF;
    private static final float HOVER_SPEED = 0.6f;
    private static final float CLICK_SPEED = 0.15f;
    private static final float FLASH_SPEED = 0.1f;

    private final ResourceLocation icon;
    private final KeyMapping shortcut;
    private final int labelY;
    private final Runnable action;
    private boolean lit;
    private float flash;
    private float flashBefore;
    private int fromTop = IDLE_TOP;
    private int fromBottom = IDLE_BOTTOM;
    private float blend;
    private float blendBefore;
    private float blendSpeed = HOVER_SPEED;
    private boolean hoveredBefore;
    private int ticks;

    HintButton(int x, int y, String name, KeyMapping shortcut, int labelY, Runnable action) {
        super(x - 4, y - 4, SIZE + 8, SIZE + 8, Component.translatable("quasium.hint.button." + name));
        this.icon = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hint/" + name);
        this.shortcut = shortcut;
        this.labelY = labelY;
        this.action = action;
    }

    void light(boolean on) {
        lit = on;
    }

    void tick() {
        ticks++;
        flashBefore = flash;
        flash += ((lit ? 1f : 0f) - flash) * FLASH_SPEED;
        blendBefore = blend;
        blend -= blend * blendSpeed;
    }

    boolean matches(int key, int scan) {
        return visible && shortcut.matches(key, scan);
    }

    @Override
    public void onPress() {
        action.run();
        animateFrom(CLICK_TOP, CLICK_BOTTOM, CLICK_SPEED);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isHovered != hoveredBefore) {
            animateFrom(base(hoveredBefore, true, partialTick), base(hoveredBefore, false, partialTick), HOVER_SPEED);
            hoveredBefore = isHovered;
        }
        int x = getX() + 4;
        int y = getY() + 4;
        HintTheme.box(graphics, x, y, SIZE, SIZE, HintTheme.TRANSPARENT, border(true, partialTick), border(false, partialTick));
        graphics.blitSprite(icon, x + 2, y + 2, 16, 16);
        Font font = Minecraft.getInstance().font;
        String key = shortcut.getTranslatedKeyMessage().getString().toLowerCase(Locale.ROOT);
        graphics.drawCenteredString(font, key, x + SIZE / 2 + 8, y + SIZE - 6, HintTheme.TEXT_DARKER);
        if (isHovered) graphics.drawCenteredString(font, getMessage(), x + SIZE / 2, labelY, HintTheme.TEXT_DARKER);
    }

    private int base(boolean hovered, boolean top, float partialTick) {
        int state = hovered ? (top ? HOVER_TOP : HOVER_BOTTOM) : (top ? IDLE_TOP : IDLE_BOTTOM);
        return HintTheme.mix(state, top ? fromTop : fromBottom, Mth.lerp(partialTick, blendBefore, blend));
    }

    private int border(boolean top, float partialTick) {
        int color = base(isHovered, top, partialTick);
        float glowing = Mth.lerp(partialTick, flashBefore, flash);
        if (glowing <= 0.1f) return color;
        float pulse = (0.5f + 0.5f * Mth.sin((ticks + partialTick) / 10f)) * glowing;
        int glow = Math.min(255, (color >>> 24) + 150) << 24 | (top ? 0xFFFFFF : 0x9B9B9B);
        return HintTheme.mix(color, glow, pulse);
    }

    private void animateFrom(int top, int bottom, float speed) {
        fromTop = top;
        fromBottom = bottom;
        blend = blendBefore = 1f;
        blendSpeed = speed;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
