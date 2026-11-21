package net.sorukoma.quasium.tea.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.tea.SamovarBlockEntity;
import net.sorukoma.quasium.tea.SamovarMenu;

import java.util.Locale;

public class SamovarScreen extends AbstractContainerScreen<SamovarMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/samovar.png");
    private static final int TANK_X = 26;
    private static final int TANK_Y = 17;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;
    private static final int FLAME_X = SamovarMenu.FUEL_X;
    private static final int FLAME_Y = SamovarMenu.FUEL_Y - 17;
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int LACK_Y = ARROW_Y - 12;
    private static final int PARTY_Y = 60;
    private static final int LABEL_COLOR = 0x404040;

    public SamovarScreen(SamovarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.samovar.water",
                    menu.value(SamovarBlockEntity.DATA_WATER), menu.value(SamovarBlockEntity.DATA_WATER_CAPACITY)), mouseX, mouseY);
        } else if (isHovering(FLAME_X, FLAME_Y, GuiBars.FLAME, GuiBars.FLAME, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.fuel", GuiBars.percent(fuel())), mouseX, mouseY);
        } else if (isHovering(ARROW_X, ARROW_Y, GuiBars.ARROW_W, GuiBars.ARROW_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.samovar.brewing", GuiBars.percent(progress())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.water(graphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.value(SamovarBlockEntity.DATA_WATER) / (float) Math.max(1, menu.value(SamovarBlockEntity.DATA_WATER_CAPACITY)));
        GuiBars.flame(graphics, leftPos + FLAME_X, topPos + FLAME_Y, fuel());
        GuiBars.arrow(graphics, leftPos + ARROW_X, topPos + ARROW_Y, progress());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        SamovarBlockEntity.Lack lack = SamovarBlockEntity.Lack.byIndex(menu.value(SamovarBlockEntity.DATA_LACK));
        if (lack != SamovarBlockEntity.Lack.NOTHING) {
            graphics.drawString(font, Component.translatable("gui.quasium.samovar.lack." + lack.name().toLowerCase(Locale.ROOT)),
                    ARROW_X, LACK_Y, LABEL_COLOR, false);
        }
        int seconds = menu.value(SamovarBlockEntity.DATA_PARTY_SECONDS);
        if (seconds <= 0) return;
        graphics.drawString(font, Component.translatable("gui.quasium.samovar.party", seconds / 60, String.format(Locale.ROOT, "%02d", seconds % 60)),
                ARROW_X, PARTY_Y, LABEL_COLOR, false);
    }

    private float fuel() {
        return menu.value(SamovarBlockEntity.DATA_FUEL) / (float) Math.max(1, menu.value(SamovarBlockEntity.DATA_FUEL_MAX));
    }

    private float progress() {
        return menu.value(SamovarBlockEntity.DATA_PROGRESS) / (float) Math.max(1, menu.value(SamovarBlockEntity.DATA_PROGRESS_MAX));
    }
}
