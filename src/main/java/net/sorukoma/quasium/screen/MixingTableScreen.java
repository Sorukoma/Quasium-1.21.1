package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.magic.FocusEssence;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.mind.ClientMind;

import java.util.List;

public class MixingTableScreen extends AbstractContainerScreen<MixingTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/mixing_table.png");
    private static final int HINT_BOTTLE_U = 176;
    private static final int HINT_SHARD_U = 192;
    private static final int HINT_BOWL_U = 208;
    private static final int HINT_GEM_U = 224;
    private static final int FOCUS_LINE_Y = 60;
    private static final int ARROW_X = 103;
    private static final int ARROW_Y = 17;
    private static final int LABEL = 0x404040;

    public MixingTableScreen(MixingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiBars.arrow(graphics, leftPos + ARROW_X, topPos + ARROW_Y, 0f);
        hint(graphics, MixingTableMenu.ESSENCE, HINT_BOTTLE_U);
        hint(graphics, MixingTableMenu.CATALYST, HINT_SHARD_U);
        hint(graphics, MixingTableMenu.BOWL, HINT_BOWL_U);
        if (!menu.hasBowl()) return;
        hint(graphics, MixingTableMenu.UPGRADE, HINT_GEM_U);
        for (int i = 0; i < FocusEssence.MAX_SLOTS; i++) {
            hint(graphics, MixingTableMenu.FOCUS + i, HINT_BOTTLE_U);
        }
    }

    private void hint(GuiGraphics graphics, int slotIndex, int u) {
        Slot slot = menu.getSlot(slotIndex);
        if (!slot.hasItem()) graphics.blit(TEXTURE, leftPos + slot.x, topPos + slot.y, u, 0, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        List<String> essences = menu.focusEssences();
        ResourceLocation spell = Spells.match(essences, ClientMind.knownSpells());
        Component line;
        if (!menu.hasBowl()) {
            line = Component.translatable("gui.quasium.mixing.no_bowl");
        } else if (spell != null) {
            line = Component.translatable("gui.quasium.bowl.focus_spell", Spell.displayName(spell));
        } else if (essences.isEmpty()) {
            line = Component.translatable("gui.quasium.bowl.focus_empty");
        } else {
            line = Component.translatable("gui.quasium.mixing.no_spell");
        }
        graphics.drawString(font, line, (imageWidth - font.width(line)) / 2, FOCUS_LINE_Y, LABEL, false);
    }
}
