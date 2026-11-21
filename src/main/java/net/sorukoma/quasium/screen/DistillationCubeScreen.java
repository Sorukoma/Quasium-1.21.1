package net.sorukoma.quasium.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.DistillationCubeBlockEntity;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.magic.NoteType;

import java.util.List;

public class DistillationCubeScreen extends AbstractContainerScreen<DistillationCubeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/distillation_cube.png");
    private static final int TANK_X = 15;
    private static final int TANK_Y = 17;
    private static final int TANK_W = 12;
    private static final int TANK_H = 52;
    private static final int FLAME_X = 44;
    private static final int FLAME_Y = 36;
    private static final int ARROW_X = 68;
    private static final int ARROW_Y = 34;
    private static final int GAUGE_X = 101;
    private static final int GAUGE_STEP = 12;
    private static final int GAUGE_Y = 17;
    private static final int GAUGE_W = 8;
    private static final int GAUGE_H = 40;
    private static final int GLYPH_BOTTOM = 68;

    public DistillationCubeScreen(DistillationCubeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        NoteType note = gaugeAt(mouseX - leftPos, mouseY - topPos);
        if (note != null) {
            int capacity = value(DistillationCubeBlockEntity.DATA_NOTE_CAPACITY);
            graphics.renderComponentTooltip(font, List.of(note.displayName(),
                    Component.translatable("gui.quasium.distillation_cube.note_amount", menu.note(note), capacity).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.quasium.distillation_cube.scoop_hint").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        } else if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.distillation_cube.water",
                    value(DistillationCubeBlockEntity.DATA_WATER), value(DistillationCubeBlockEntity.DATA_WATER_CAPACITY)), mouseX, mouseY);
        } else if (isHovering(FLAME_X, FLAME_Y, GuiBars.FLAME, GuiBars.FLAME, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.fuel", GuiBars.percent(fuel())), mouseX, mouseY);
        } else if (isHovering(ARROW_X, ARROW_Y, GuiBars.ARROW_W, GuiBars.ARROW_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.progress", GuiBars.percent(progress())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.flame(graphics, leftPos + FLAME_X, topPos + FLAME_Y, fuel());
        GuiBars.arrow(graphics, leftPos + ARROW_X, topPos + ARROW_Y, progress());
        GuiBars.water(graphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                ratio(DistillationCubeBlockEntity.DATA_WATER, DistillationCubeBlockEntity.DATA_WATER_CAPACITY));
        float capacity = Math.max(1, value(DistillationCubeBlockEntity.DATA_NOTE_CAPACITY));
        for (NoteType note : NoteType.values()) {
            GuiBars.smoke(graphics, leftPos + GAUGE_X + note.ordinal() * GAUGE_STEP, topPos + GAUGE_Y, GAUGE_W, GAUGE_H,
                    menu.note(note) / capacity, note.color());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        NoteType note = gaugeAt((int) mouseX - leftPos, (int) mouseY - topPos);
        if (note != null && minecraft != null && minecraft.gameMode != null && !menu.getCarried().isEmpty()) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, note.ordinal());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int value(int index) {
        return menu.value(index);
    }

    private float ratio(int amount, int max) {
        return value(amount) / (float) Math.max(1, value(max));
    }

    private float fuel() {
        return ratio(DistillationCubeBlockEntity.DATA_FUEL, DistillationCubeBlockEntity.DATA_FUEL_MAX);
    }

    private float progress() {
        return ratio(DistillationCubeBlockEntity.DATA_PROGRESS, DistillationCubeBlockEntity.DATA_PROCESS_TICKS);
    }

    private static NoteType gaugeAt(int localX, int localY) {
        if (localY < GAUGE_Y || localY >= GLYPH_BOTTOM) return null;
        for (NoteType note : NoteType.values()) {
            int left = GAUGE_X + note.ordinal() * GAUGE_STEP;
            if (localX >= left && localX < left + GAUGE_W) return note;
        }
        return null;
    }
}
