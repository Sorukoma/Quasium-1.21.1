package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HookahBowlScreen extends AbstractContainerScreen<HookahBowlMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse("quasium:textures/gui/container/hookah_bowl.png");

    private static final int GRID_COLS = 5;
    private static final int GRID_ROWS = 4;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_X_START = 8;
    private static final int GRID_Y_START = 18;

    public HookahBowlScreen(HookahBowlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Основная текстура (vanilla generic_54 — double chest)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
