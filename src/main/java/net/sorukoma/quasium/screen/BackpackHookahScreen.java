package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.network.PacketBackpackHookahPuffDataClientHandler;
import net.sorukoma.quasium.network.PacketBackpackHookahPuffDataClientHandler.ClientBackpackHookahData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BackpackHookahScreen extends AbstractContainerScreen<BackpackHookahMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/hookah_gui.png");

    private static final int TICKS_PER_SECOND = 20;
    private static final int BAR_WIDTH = 5;
    private static final int BAR_HEIGHT = 16;

    private final int maxTobaccoWeight = BackpackHookahState.MAX_TOBACCO_WEIGHT;

    private ClientBackpackHookahData hookahData;

    public BackpackHookahScreen(BackpackHookahMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        this.hookahData = PacketBackpackHookahPuffDataClientHandler.getClientBackpackHookahData();

        float totalHeat = hookahData != null ? hookahData.totalHeat() : 0f;
        int totalTobaccoWeight = hookahData != null ? hookahData.totalTobaccoWeight() : 0;

        // Heat bar (max visual = 4.0 for makhorka threshold display)
        int heatBarX = x + 80 + 20;
        int heatBarY = y + 17;
        guiGraphics.fill(heatBarX, heatBarY, heatBarX + BAR_WIDTH, heatBarY + BAR_HEIGHT, 0xFF8B8B8B);
        if (totalHeat > 0) {
            float maxVisualHeat = 4.0f;
            int filledHeight = (int) (Math.min(totalHeat / maxVisualHeat, 1.0f) * BAR_HEIGHT);
            guiGraphics.fill(heatBarX, heatBarY + BAR_HEIGHT - filledHeight, heatBarX + BAR_WIDTH, heatBarY + BAR_HEIGHT, 0xFFFFA500);
        }

        int contentBarX = x + 80 + 20;
        int contentBarY = y + 53;
        guiGraphics.fill(contentBarX, contentBarY, contentBarX + BAR_WIDTH, contentBarY + BAR_HEIGHT, 0xFF8B8B8B);
        if (totalTobaccoWeight > 0) {
            int filledHeight = (int) ((float) totalTobaccoWeight / maxTobaccoWeight * BAR_HEIGHT);
            guiGraphics.fill(contentBarX, contentBarY + BAR_HEIGHT - filledHeight, contentBarX + BAR_WIDTH, contentBarY + BAR_HEIGHT, 0xFF00FF00);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        renderBarTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderBarTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hookahData == null) return;

        int x = this.leftPos;
        int y = this.topPos;

        float totalHeat = hookahData.totalHeat();
        int totalTobaccoWeight = hookahData.totalTobaccoWeight();
        ModDataComponents.HookahBowlContents bowlContents = hookahData.bowlContents();

        int contentBarX = x + 80 + 20;
        int contentBarY = y + 53;
        if (mouseX >= contentBarX && mouseX < contentBarX + BAR_WIDTH
                && mouseY >= contentBarY && mouseY < contentBarY + BAR_HEIGHT) {
            List<Component> tooltipLines = new ArrayList<>();
            if (totalTobaccoWeight > 0) {
                Component tooltipText;
                if (bowlContents != null && !bowlContents.isCompatible()) {
                    tooltipText = Component.translatable("quasium.tooltip.incompatible_mix").withStyle(net.minecraft.ChatFormatting.RED);
                } else if (bowlContents != null && bowlContents.groups().size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    Map<Integer, Integer> groupPercentages = bowlContents.getGroupPercentages();
                    for (int i = 0; i < bowlContents.groups().size(); i++) {
                        ModDataComponents.BowlTobaccoGroup group = bowlContents.groups().get(i);
                        Integer pct = groupPercentages.get(i);
                        if (pct == null) continue;
                        if (!sb.isEmpty()) sb.append(" / ");
                        if (group.isRegular()) {
                            sb.append(pct).append("% ")
                              .append(Component.translatable("quasium.flavor." + group.flavor()).getString());
                        } else {
                            sb.append(pct).append("% ")
                              .append(Component.translatable("quasium.base." + group.base()).getString());
                        }
                    }
                    tooltipText = Component.literal(sb.toString());
                } else if (bowlContents != null && bowlContents.groups().size() == 1) {
                    ModDataComponents.BowlTobaccoGroup group = bowlContents.groups().get(0);
                    if (group.isRegular()) {
                        tooltipText = Component.translatable("quasium.flavor." + group.flavor());
                    } else {
                        tooltipText = Component.translatable("quasium.base." + group.base());
                    }
                } else {
                    tooltipText = Component.translatable("gui.quasium.empty");
                }
                int tobaccoGrams = totalTobaccoWeight / BackpackHookahState.TOBACCO_TICKS_PER_GRAM;
                float tobaccoSeconds = (float) totalTobaccoWeight / TICKS_PER_SECOND;
                tooltipLines.add(Component.translatable("quasium.gui.hookah.contents").append(": ").append(tooltipText));
                tooltipLines.add(Component.literal(String.format("%dg / %dg (~%.0fs)", tobaccoGrams,
                        BackpackHookahState.MAX_TOBACCO_WEIGHT / BackpackHookahState.TOBACCO_TICKS_PER_GRAM,
                        tobaccoSeconds)));
            } else {
                tooltipLines.add(Component.translatable("quasium.gui.hookah.contents").append(": ").append(Component.translatable("gui.quasium.empty")));
            }
            guiGraphics.renderTooltip(this.font, tooltipLines.stream().map(Component::getVisualOrderText).collect(Collectors.toList()), mouseX, mouseY);
        }

        int heatBarX = x + 80 + 20;
        int heatBarY = y + 17;
        if (mouseX >= heatBarX && mouseX < heatBarX + BAR_WIDTH
                && mouseY >= heatBarY && mouseY < heatBarY + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, List.of(Component.translatable("quasium.gui.hookah.heat", String.format("%.2f", totalHeat))).stream().map(Component::getVisualOrderText).collect(Collectors.toList()), mouseX, mouseY);
        }
    }
}
