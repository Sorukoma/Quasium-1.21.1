package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.kiln.SmokeKilnBlockEntity;

public class SmokeKilnScreen extends AbstractContainerScreen<SmokeKilnMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/smoke_kiln.png");
    private static final int ARROW_X = 50;
    private static final int ARROW_Y = 34;
    private static final int HINT_BOTTLE_U = 176;
    private static final int HINT_TOOL_U = 192;

    public SmokeKilnScreen(SmokeKilnMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(ARROW_X, ARROW_Y, GuiBars.ARROW_W, GuiBars.ARROW_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.progress", menu.progressPercent()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.arrow(graphics, leftPos + ARROW_X, topPos + ARROW_Y, menu.progressPercent() / 100f);
        for (int index = 0; index < SmokeKilnBlockEntity.TOOL_SLOTS; index++) {
            hint(graphics, index, HINT_TOOL_U);
        }
        hint(graphics, SmokeKilnBlockEntity.ESSENCE_SLOT, HINT_BOTTLE_U);
    }

    private void hint(GuiGraphics graphics, int slotIndex, int u) {
        Slot slot = menu.getSlot(slotIndex);
        if (!slot.hasItem()) graphics.blit(TEXTURE, leftPos + slot.x, topPos + slot.y, u, 0, 16, 16);
    }
}
