package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.draft.SmokeBoilerBlockEntity;

public class SmokeBoilerScreen extends AbstractContainerScreen<SmokeBoilerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/smoke_boiler.png");
    private static final int TANK_X = 62;
    private static final int TANK_Y = 17;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;
    private static final int FLAME_X = SmokeBoilerMenu.FUEL_X;
    private static final int FLAME_Y = SmokeBoilerMenu.FUEL_Y - 17;

    public SmokeBoilerScreen(SmokeBoilerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.smoke_boiler.water",
                    menu.value(SmokeBoilerBlockEntity.DATA_WATER), menu.value(SmokeBoilerBlockEntity.DATA_WATER_CAPACITY)), mouseX, mouseY);
        } else if (isHovering(FLAME_X, FLAME_Y, GuiBars.FLAME, GuiBars.FLAME, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.quasium.fuel", GuiBars.percent(fuel())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.flame(graphics, leftPos + FLAME_X, topPos + FLAME_Y, fuel());
        GuiBars.water(graphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.value(SmokeBoilerBlockEntity.DATA_WATER) / (float) Math.max(1, menu.value(SmokeBoilerBlockEntity.DATA_WATER_CAPACITY)));
    }

    private float fuel() {
        return menu.value(SmokeBoilerBlockEntity.DATA_FUEL) / (float) Math.max(1, menu.value(SmokeBoilerBlockEntity.DATA_FUEL_MAX));
    }
}
