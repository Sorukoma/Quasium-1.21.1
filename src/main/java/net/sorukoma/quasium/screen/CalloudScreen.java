package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.GuiBars;

public class CalloudScreen extends AbstractContainerScreen<CalloudMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/calloud.png");
    private static final int FLAME_X = 72;
    private static final int FLAME_Y = 55;

    public CalloudScreen(CalloudMenu menu, Inventory playerInventory, Component title) {
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
            graphics.renderComponentTooltip(font, AbstractHookahScreen.heatTooltip(menu.contents().totalHeat()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.flame(graphics, leftPos + FLAME_X, topPos + FLAME_Y, menu.contents().totalHeat() / AbstractHookahScreen.FULL_HEAT);
    }
}
