package net.sorukoma.quasium.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler.ClientHookahData; // Import the new record
import net.sorukoma.quasium.block.entity.HookahBlockEntity; // Импортируем HookahBlockEntity для доступа к константам
import java.util.UUID; // Import UUID

public class HookahScreen extends AbstractContainerScreen<HookahMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/hookah_gui.png");

    // Константы для максимального количества затяжек (получаем из HookahBlockEntity)
    private final int MAX_PUFFS_PER_POTION;
    private final int COAL_BURN_TIME_TICKS;
    private static final int TICKS_PER_SECOND = 20; // Из HookahBlockEntity

    public HookahScreen(HookahMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        // Получаем константы из BlockEntity
        this.MAX_PUFFS_PER_POTION = menu.blockEntity.getMaxPuffsPerPotion();
        this.COAL_BURN_TIME_TICKS = menu.blockEntity.getCoalBurnTimeTicks();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY += 2;
        this.titleLabelY -= 10;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        ClientHookahData hookahData = PacketHookahPuffDataClientHandler.getClientHookahData(menu.blockEntity.getBlockPos());
        int puffsRemainingPotion = 0;
        int currentCoalBurnTime = 0;
        UUID attachedPlayerUUID = null;
        String attachedPlayerName = null;

        if (hookahData != null) {
            puffsRemainingPotion = hookahData.puffsRemainingPotion();
            currentCoalBurnTime = hookahData.puffsRemainingCharcoal();
            attachedPlayerUUID = hookahData.attachedPlayerUUID();
            attachedPlayerName = hookahData.attachedPlayerName();
        }


        // Отрисовка шкалы для зелья
        int potionBarX = x + 80 + 20;
        int potionBarY = y + 53;
        int barWidth = 5;
        int barHeight = 16;

        pGuiGraphics.fill(potionBarX, potionBarY, potionBarX + barWidth, potionBarY + barHeight, 0xFF8B8B8B);
        if (puffsRemainingPotion > 0) {
            int filledHeight = (int) ((float) puffsRemainingPotion / MAX_PUFFS_PER_POTION * barHeight);
            pGuiGraphics.fill(potionBarX, potionBarY + barHeight - filledHeight, potionBarX + barWidth, potionBarY + barHeight, 0xFF00FF00);
        }

        // Отрисовка шкалы для углей
        int charcoalBarX = x + 80 + 20;
        int charcoalBarY = y + 17;
        pGuiGraphics.fill(charcoalBarX, charcoalBarY, charcoalBarX + barWidth, charcoalBarY + barHeight, 0xFF8B8B8B);
        if (currentCoalBurnTime > 0) {
            int filledHeight = (int) ((float) currentCoalBurnTime / COAL_BURN_TIME_TICKS * barHeight);
            pGuiGraphics.fill(charcoalBarX, charcoalBarY + barHeight - filledHeight, charcoalBarX + barWidth, charcoalBarY + barHeight, 0xFFFFA500);
        }

        // Отображение имени прикрепленного игрока
        if (attachedPlayerName != null) {
            Component attachedPlayerComponent = Component.literal("Attached: " + attachedPlayerName);
            pGuiGraphics.drawString(this.font, attachedPlayerComponent, x + imageWidth / 2 - this.font.width(attachedPlayerComponent) / 2, y - 20, 0xFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        ClientHookahData hookahData = PacketHookahPuffDataClientHandler.getClientHookahData(menu.blockEntity.getBlockPos());
        int puffsRemainingPotion = 0;
        int currentCoalBurnTime = 0;
        // No need for attachedPlayerUUID/Name here for tooltips, but keeping the structure consistent
        // UUID attachedPlayerUUID = null;
        // String attachedPlayerName = null;

        if (hookahData != null) {
            puffsRemainingPotion = hookahData.puffsRemainingPotion();
            currentCoalBurnTime = hookahData.puffsRemainingCharcoal();
            // attachedPlayerUUID = hookahData.attachedPlayerUUID();
            // attachedPlayerName = hookahData.attachedPlayerName();
        }

        // Шкала зелья
        int potionBarX = x + 80 + 20;
        int potionBarY = y + 53;
        int barWidth = 5;
        int barHeight = 16;
        if (pMouseX >= potionBarX && pMouseX < potionBarX + barWidth && pMouseY >= potionBarY && pMouseY < potionBarY + barHeight) {
            float potionSeconds = (float) puffsRemainingPotion / TICKS_PER_SECOND;
            pGuiGraphics.renderTooltip(this.font, Component.literal(String.format("Potion: %.1fs / %.1fs", potionSeconds, (float)MAX_PUFFS_PER_POTION / TICKS_PER_SECOND)), pMouseX, pMouseY);
        }

        // Шкала углей
        int charcoalBarX = x + 80 + 20;
        int charcoalBarY = y + 17;
        if (pMouseX >= charcoalBarX && pMouseX < charcoalBarX + barWidth && pMouseY >= charcoalBarY && pMouseY < charcoalBarY + barHeight) {
            float charcoalSeconds = (float) currentCoalBurnTime / TICKS_PER_SECOND;
            pGuiGraphics.renderTooltip(this.font, Component.literal(String.format("Charcoal: %.1fs / %.1fs", charcoalSeconds, (float)COAL_BURN_TIME_TICKS / TICKS_PER_SECOND)), pMouseX, pMouseY);
        }
    }
}
