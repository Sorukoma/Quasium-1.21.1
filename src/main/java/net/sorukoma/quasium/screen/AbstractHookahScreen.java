package net.sorukoma.quasium.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.hookah.HookahReport;
import net.sorukoma.quasium.util.HookahLogic;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class AbstractHookahScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    static final float FULL_HEAT = 4f;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/hookah_gui.png");
    private static final Component HEAT = Component.translatable("quasium.gui.hookah.heat_label");
    private static final Component TOBACCO = Component.translatable("quasium.gui.hookah.tobacco_label");
    private static final int LABEL = 0x404040;
    private static final int LEFT = 8;
    private static final int RIGHT = 74;
    private static final int BAR_W = RIGHT - LEFT;
    private static final int HEAT_Y = 18;
    private static final int TOBACCO_Y = 40;
    private static final int BAR_DROP = 11;
    private static final int ROW_H = BAR_DROP + GuiBars.THICKNESS;
    private static final int FLAME_X = 80;
    private static final int FLAME_Y = 36;
    private static final int MOOD_Y = 60;
    private static final int COOLER_SLOT = 3;
    private static final int MAX_GRAMS = HookahBlockEntity.MAX_TOBACCO_WEIGHT / HookahBlockEntity.TOBACCO_TICKS_PER_GRAM;

    protected AbstractHookahScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract Reading reading();

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        Reading reading = reading();
        if (isHovering(LEFT, HEAT_Y, BAR_W, ROW_H, mouseX, mouseY) || isHovering(FLAME_X, FLAME_Y, GuiBars.FLAME, GuiBars.FLAME, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, heatTooltip(reading, cooler()), mouseX, mouseY);
        } else if (isHovering(LEFT, TOBACCO_Y, BAR_W, ROW_H, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("quasium.gui.hookah.contents").append(": ").append(mix(reading)),
                    Component.translatable("quasium.gui.hookah.tobacco_duration", grams(reading.tobacco()), MAX_GRAMS,
                            secondsLeft(reading)).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        Reading reading = reading();
        float heat = reading.heat() / FULL_HEAT;
        heatBar(graphics, leftPos + LEFT, topPos + HEAT_Y + BAR_DROP, reading);
        GuiBars.bar(graphics, BossBarColor.GREEN, leftPos + LEFT, topPos + TOBACCO_Y + BAR_DROP, BAR_W,
                reading.tobacco() / (float) HookahBlockEntity.MAX_TOBACCO_WEIGHT);
        GuiBars.flame(graphics, leftPos + FLAME_X, topPos + FLAME_Y, heat);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Reading reading = reading();
        row(graphics, HEAT, Component.literal(heat(reading.heat())), HEAT_Y);
        row(graphics, TOBACCO, Component.translatable("quasium.gui.hookah.grams", grams(reading.tobacco())), TOBACCO_Y);
        HookahLogic.Mood mood = HookahLogic.mood(reading.heat(), reading.threshold(), cooler(), reading.warmup(), reading.tobacco());
        graphics.drawString(font, HookahReport.moodName(mood, reading.tobacco(), reading.warmup()), LEFT, MOOD_Y, moodColor(mood), false);
    }

    private boolean cooler() {
        return !menu.getSlot(COOLER_SLOT).getItem().isEmpty();
    }

    private void heatBar(GuiGraphics graphics, int x, int y, Reading reading) {
        int draw = px(HookahLogic.drawHeat(reading.threshold(), cooler()));
        int over = px(HookahLogic.overheatLine(reading.threshold()));
        int heat = px(reading.heat());
        band(graphics, BossBarColor.YELLOW, x, y, 0, draw, heat);
        band(graphics, BossBarColor.GREEN, x, y, draw, over, heat);
        band(graphics, BossBarColor.RED, x, y, over, BAR_W, heat);
    }

    private static void band(GuiGraphics graphics, BossBarColor color, int x, int y, int from, int to, int heat) {
        if (to - from < 2) return;
        GuiBars.track(graphics, color, x + from, y, to - from);
        GuiBars.fill(graphics, color, x, y, from, Math.min(heat, to));
    }

    private static int px(float heat) {
        return Math.round(Mth.clamp(heat / FULL_HEAT, 0f, 1f) * BAR_W);
    }

    private static int moodColor(HookahLogic.Mood mood) {
        return switch (mood) {
            case COLD -> LABEL;
            case WARMUP -> 0x8A6200;
            case PEAK -> 0x2F7D1F;
            case TAIL -> 0x6B4A2A;
            case OVERHEAT -> 0xB02020;
        };
    }

    private static int secondsLeft(Reading reading) {
        float perTick = HookahLogic.burnPerTick(reading.heat(), reading.threshold(), grams(reading.tobacco()));
        return Math.round(reading.tobacco() / perTick / HookahBlockEntity.TICKS_PER_SECOND);
    }

    private void row(GuiGraphics graphics, Component label, Component value, int y) {
        graphics.drawString(font, label, LEFT, y, LABEL, false);
        graphics.drawString(font, value, RIGHT - font.width(value), y, LABEL, false);
    }

    static List<Component> heatTooltip(float heat) {
        return List.of(Component.translatable("quasium.gui.hookah.heat", heat(heat)), needLine());
    }

    private static List<Component> heatTooltip(Reading reading, boolean cooler) {
        return List.of(Component.translatable("quasium.gui.hookah.heat", heat(reading.heat())),
                Component.translatable("quasium.gui.hookah.heat_band", heat(HookahLogic.drawHeat(reading.threshold(), cooler)),
                        heat(HookahLogic.overheatLine(reading.threshold()))).withStyle(ChatFormatting.DARK_GREEN),
                Component.translatable("quasium.gui.hookah.heat_over").withStyle(ChatFormatting.GRAY),
                needLine());
    }

    private static Component needLine() {
        return Component.translatable("quasium.gui.hookah.heat_need", heat(HookahLogic.HEAT_REGULAR),
                heat(HookahLogic.HEAT_SELF_MIXED), heat(HookahLogic.HEAT_PESCHANIK)).withStyle(ChatFormatting.GRAY);
    }

    private static Component mix(Reading reading) {
        HookahBowlContents contents = reading.contents();
        if (reading.tobacco() <= 0 || contents == null || contents.groups().isEmpty()) return Component.translatable("gui.quasium.empty");
        if (!contents.isCompatible()) return Component.translatable("quasium.tooltip.incompatible_mix").withStyle(ChatFormatting.RED);
        if (contents.groups().size() == 1) return groupName(contents.groups().get(0));
        MutableComponent mix = Component.empty();
        Map<Integer, Integer> percentages = contents.getGroupPercentages();
        for (int i = 0; i < contents.groups().size(); i++) {
            Integer percent = percentages.get(i);
            if (percent == null) continue;
            if (!mix.getSiblings().isEmpty()) mix.append(" / ");
            mix.append(percent + "% ").append(groupName(contents.groups().get(i)));
        }
        return mix;
    }

    private static Component groupName(BowlTobaccoGroup group) {
        return Component.translatable(group.isRegular() ? "quasium.flavor." + group.flavor() : "quasium.base." + group.base());
    }

    private static String heat(float heat) {
        return String.format(java.util.Locale.ROOT, "%.2f", heat);
    }

    private static int grams(int tobacco) {
        return tobacco / HookahBlockEntity.TOBACCO_TICKS_PER_GRAM;
    }

    protected record Reading(float heat, int tobacco, int warmup, @Nullable HookahBowlContents contents) {
        static final Reading EMPTY = new Reading(0f, 0, 0, null);

        float threshold() {
            return HookahLogic.heatThreshold(contents);
        }
    }
}
