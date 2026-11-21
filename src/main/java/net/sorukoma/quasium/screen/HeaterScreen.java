package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.HeaterBlockEntity;
import net.sorukoma.quasium.client.GuiBars;

public class HeaterScreen extends AbstractContainerScreen<HeaterMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/heater.png");
    private static final int FLAME_X = 152;
    private static final int FLAME_Y = 19;

    public HeaterScreen(HeaterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(FLAME_X, FLAME_Y, GuiBars.FLAME, GuiBars.FLAME, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.fuel", GuiBars.percent(fuel())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.flame(graphics, leftPos + FLAME_X, topPos + FLAME_Y, fuel());
    }

    private float fuel() {
        return menu.fuelTicks() / (float) HeaterBlockEntity.FUEL_BURN_TIME;
    }
}
