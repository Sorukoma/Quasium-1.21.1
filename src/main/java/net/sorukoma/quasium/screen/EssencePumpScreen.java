package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.GuiBars;

public class EssencePumpScreen extends AbstractContainerScreen<EssencePumpMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/essence_pump.png");
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;

    public EssencePumpScreen(EssencePumpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(ARROW_X, ARROW_Y, GuiBars.ARROW_W, GuiBars.ARROW_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.progress", menu.progress()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.arrow(graphics, leftPos + ARROW_X, topPos + ARROW_Y, menu.progress() / 100f);
    }
}
