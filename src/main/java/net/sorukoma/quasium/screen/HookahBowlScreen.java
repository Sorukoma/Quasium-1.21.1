package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.FocusEssence;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.mind.ClientMind;

import java.util.List;

public class HookahBowlScreen extends AbstractContainerScreen<HookahBowlMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/container/hookah_bowl.png");
    private static final ResourceLocation LOCK = ResourceLocation.withDefaultNamespace("container/cartography_table/locked");
    private static final Component LOCKED = Component.translatable("gui.quasium.bowl.focus_locked");
    private static final int LABEL = 0x404040;
    private static final int LOCK_X = HookahBowlMenu.FOCUS_X_START + FocusEssence.MAX_SLOTS * 18 + 4;
    private static final int LOCK_Y = HookahBowlMenu.FOCUS_Y + 1;
    private static final int LOCK_W = 10;
    private static final int LOCK_H = 14;

    public HookahBowlScreen(HookahBowlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.isHeated() && isHovering(LOCK_X, LOCK_Y, LOCK_W, LOCK_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(LOCKED, 160), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.isHeated()) graphics.blitSprite(LOCK, leftPos + LOCK_X, topPos + LOCK_Y, LOCK_W, LOCK_H);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        List<String> essences = FocusEssence.activeEssences(menu.currentFocus());
        ResourceLocation spell = Spells.match(essences, ClientMind.knownSpells());
        Component focus;
        if (spell != null) {
            focus = Component.translatable("gui.quasium.bowl.focus_spell", Spell.displayName(spell));
        } else if (essences.isEmpty()) {
            focus = Component.translatable("gui.quasium.bowl.focus_empty");
        } else {
            focus = Component.translatable("gui.quasium.mixing.no_spell");
        }
        graphics.drawString(font, focus, 8, HookahBowlMenu.FOCUS_Y - 11, LABEL, false);
    }
}
