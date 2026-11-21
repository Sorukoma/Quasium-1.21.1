package net.sorukoma.quasium.mind.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientMagicData;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.table.ResearchTableMenu;
import net.sorukoma.quasium.progression.ConfigArgs;

@OnlyIn(Dist.CLIENT)
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/research_table.png");
    private static final int LABEL = 0x404040;
    private static final int ARROW_X = 55;
    private static final int ARROW_Y = 35;
    private static final int HINT_U = 176;
    private static final int TEXT_X = 116;
    private static final int BUTTON_Y = 33;
    private static final int BUTTON_W = 52;

    private Button write;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        write = addRenderableWidget(Button.builder(Component.translatable("quasium.research_table.write"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchTableMenu.WRITE_BUTTON))
                .bounds(leftPos + TEXT_X, topPos + BUTTON_Y, BUTTON_W, 20).build());
    }

    @Override
    protected void containerTick() {
        write.active = ClientMagicData.insight() >= ConfigArgs.synced(MindConfig.TREATISE_INSIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        hint(graphics, menu.getSlot(0), 0);
        hint(graphics, menu.getSlot(1), 16);
        GuiBars.arrow(graphics, leftPos + ARROW_X, topPos + ARROW_Y, menu.progress());
    }

    private void hint(GuiGraphics graphics, Slot slot, int v) {
        if (!slot.hasItem()) graphics.blit(TEXTURE, leftPos + slot.x, topPos + slot.y, HINT_U, v, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("quasium.research_table.insight", ClientMagicData.insight()), TEXT_X, 20, LABEL, false);
        graphics.drawString(font, Component.translatable("quasium.research_table.price", ConfigArgs.synced(MindConfig.TREATISE_INSIGHT)),
                TEXT_X, 56, LABEL, false);
        if (menu.queued() > 0) {
            graphics.drawString(font, Component.translatable("quasium.research_table.queued", menu.queued()), ARROW_X - 2, 56, LABEL, false);
        }
    }
}
