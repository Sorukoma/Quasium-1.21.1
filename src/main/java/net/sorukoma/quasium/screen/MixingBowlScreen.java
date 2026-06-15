package net.sorukoma.quasium.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.network.PacketMixTobacco;
import net.neoforged.neoforge.network.PacketDistributor;

public class MixingBowlScreen extends AbstractContainerScreen<MixingBowlMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/mixing_bowl_gui.png");
    private EditBox percentage1EditBox;
    private EditBox percentage2EditBox;
    private Button mixButton;

    public MixingBowlScreen(MixingBowlMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Percentage 1 EditBox
        this.percentage1EditBox = new EditBox(this.font, x + 40, y + 45, 30, 15, Component.translatable("quasium.gui.percentage1"));
        this.percentage1EditBox.setMaxLength(3);
        this.percentage1EditBox.setValue("50");
        this.percentage1EditBox.setFilter(s -> s.matches("\\d*")); // Only allow digits
        this.addRenderableWidget(this.percentage1EditBox);

        // Percentage 2 EditBox
        this.percentage2EditBox = new EditBox(this.font, x + 110, y + 45, 30, 15, Component.translatable("quasium.gui.percentage2"));
        this.percentage2EditBox.setMaxLength(3);
        this.percentage2EditBox.setValue("50");
        this.percentage2EditBox.setFilter(s -> s.matches("\\d*")); // Only allow digits
        this.addRenderableWidget(this.percentage2EditBox);

        // Mix Button
        this.mixButton = Button.builder(Component.translatable("quasium.gui.mix_button"), (button) -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                try {
                    int percent1 = Integer.parseInt(percentage1EditBox.getValue());
                    int percent2 = Integer.parseInt(percentage2EditBox.getValue());
                    PacketDistributor.sendToServer(new PacketMixTobacco(percent1, percent2));
                } catch (NumberFormatException e) {
                    this.minecraft.player.sendSystemMessage(Component.translatable("quasium.message.invalid_percentage"));
                }
            }
        }).bounds(x + 60, y + 75, 60, 20).build();
        this.addRenderableWidget(this.mixButton);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pGuiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);

        // Draw labels for percentage fields
        pGuiGraphics.drawString(this.font, Component.translatable("quasium.gui.percentage"), this.leftPos + 40, this.topPos + 35, 0x404040, false);
        pGuiGraphics.drawString(this.font, Component.translatable("quasium.gui.percentage"), this.leftPos + 110, this.topPos + 35, 0x404040, false);
    }
}