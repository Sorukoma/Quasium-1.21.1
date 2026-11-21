package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.golem.GolemMood;
import net.sorukoma.quasium.golem.GolemProfile;
import net.sorukoma.quasium.golem.MarkBlocks;
import net.sorukoma.quasium.golem.MarkType;
import net.sorukoma.quasium.golem.WorkerGolem;
import org.jetbrains.annotations.Nullable;

public class GolemScreen extends AbstractContainerScreen<GolemMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/golem.png");
    private static final ResourceLocation LIST_PANEL = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "golem/list_panel");
    private static final BossBarColor[] BAR_COLORS = {BossBarColor.GREEN, BossBarColor.YELLOW, BossBarColor.RED, BossBarColor.PURPLE};
    private static final Component PET = Component.translatable("quasium.golem.pet");
    private static final Component FEEDER_HINT = Component.translatable("quasium.golem.feeder_hint");
    private static final Component[] TAB_NAMES = {Component.translatable("quasium.golem.tab.care"),
            Component.translatable("quasium.golem.tab.marks"), Component.translatable("quasium.golem.tab.journal"),
            Component.translatable("quasium.golem.tab.work")};
    private static final int CARE = 0;
    private static final int MARKS = 1;
    private static final int JOURNAL = 2;
    private static final int WORK = 3;
    private static final int TAB_W = 26;
    private static final int TAB_H = 32;
    private static final int TAB_STEP = 27;
    private static final int TAB_RISE = 28;
    private static final int LABEL = 0x404040;
    private static final int BOX_X = 8;
    private static final int BOX_Y = 18;
    private static final int BOX_W = 50;
    private static final int BOX_H = 70;
    private static final int TEXT_X = 64;
    private static final int TEXT_RIGHT = 168;
    private static final int BAR_Y = 29;
    private static final int BAR_W = TEXT_RIGHT - TEXT_X;
    private static final int HINT_U = 176;
    private static int lastTab = CARE;

    private final GolemMarksPane marks;
    private final GolemJournalPane journal = new GolemJournalPane();
    private final GolemWorkPane work;
    private GolemProfile shownProfile;
    private Button pet;
    private int tab = lastTab;

    public GolemScreen(GolemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = imageHeight - 94;
        marks = new GolemMarksPane(menu);
        work = new GolemWorkPane(menu);
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(topPos, TAB_RISE);
        int x = GolemMenu.FEEDER_X + 22;
        pet = addRenderableWidget(Button.builder(PET, button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GolemMenu.PET_BUTTON))
                .bounds(leftPos + x, topPos + GolemMenu.FEEDER_Y - 1, TEXT_RIGHT - x, 18).build());
        marks.createButtons().forEach(this::addRenderableWidget);
        marks.place(leftPos, topPos);
        journal.place(leftPos, topPos);
        work.buttons().forEach(this::addRenderableWidget);
        work.place(leftPos, topPos);
        shownProfile = null;
        select(tab);
        refreshPet();
    }

    private void select(int chosen) {
        tab = chosen;
        lastTab = chosen;
        menu.showTab(tab == CARE, tab == WORK);
        pet.visible = tab == CARE;
        marks.show(tab == MARKS);
        work.show(tab == WORK);
    }

    @Override
    protected void containerTick() {
        refreshPet();
        work.tick();
        if (menu.profile() != shownProfile) {
            shownProfile = menu.profile();
            marks.rebuild(shownProfile);
            journal.rebuild(shownProfile.journal(), font);
        }
    }

    @Nullable
    private GolemListPane pane() {
        return tab == MARKS ? marks : tab == JOURNAL ? journal : null;
    }

    private void refreshPet() {
        int wait = menu.petCooldown();
        pet.active = wait == 0;
        pet.setMessage(wait == 0 ? PET : Component.translatable("quasium.golem.pet_in", wait));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int hoveredTab = tabAt(mouseX, mouseY);
        Component markName = tab == MARKS ? marks.hovered(mouseX, mouseY) : null;
        if (hoveredTab >= 0) {
            graphics.renderTooltip(font, TAB_NAMES[hoveredTab], mouseX, mouseY);
        } else if (markName != null) {
            graphics.renderTooltip(font, markName, mouseX, mouseY);
        } else if (tab == CARE && !menu.status().isEmpty() && isHovering(BOX_X, BOX_Y, BOX_W, BOX_H, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, menu.status(), mouseX, mouseY);
        } else if (tab == CARE && hoveredSlot == menu.getSlot(0) && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(font, font.split(FEEDER_HINT, 160), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        for (int index = 0; index < TAB_NAMES.length; index++) {
            if (index != tab) renderTab(graphics, index);
        }
        if (tab == CARE || tab == WORK) graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        else graphics.blitSprite(LIST_PANEL, leftPos, topPos, imageWidth, imageHeight);
        renderTab(graphics, tab);
        if (pane() != null) {
            pane().render(graphics, font);
            return;
        }
        if (tab == CARE && !menu.getSlot(0).hasItem()) {
            graphics.blit(TEXTURE, leftPos + GolemMenu.FEEDER_X, topPos + GolemMenu.FEEDER_Y, HINT_U, 0, 16, 16);
        }
        if (tab == CARE) {
            GuiBars.bar(graphics, BAR_COLORS[menu.state() - 1], leftPos + TEXT_X, topPos + BAR_Y, BAR_W, menu.meter() / GolemMood.FULL);
        }
        WorkerGolem golem = menu.golem();
        if (golem != null) {
            int scale = Math.round(0.7f * Math.min(BOX_W / golem.getBbWidth(), BOX_H / golem.getBbHeight()));
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + BOX_X, topPos + BOX_Y, leftPos + BOX_X + BOX_W,
                    topPos + BOX_Y + BOX_H, scale, 0.0625f, mouseX, mouseY, golem);
        }
    }

    private void renderTab(GuiGraphics graphics, int index) {
        boolean selected = index == tab;
        String sprite = "container/creative_inventory/tab_top_" + (selected ? "selected_" : "unselected_") + (index + 1);
        int x = leftPos + index * TAB_STEP;
        int y = topPos - TAB_RISE;
        graphics.blitSprite(ResourceLocation.withDefaultNamespace(sprite), x, y, TAB_W, TAB_H);
        graphics.renderItem(tabIcon(index), x + 5, y + 9);
    }

    private static ItemStack tabIcon(int index) {
        return switch (index) {
            case CARE -> new ItemStack(Items.GOLDEN_APPLE);
            case MARKS -> new ItemStack(MarkBlocks.item(MarkType.HARVEST));
            case WORK -> new ItemStack(Items.IRON_AXE);
            default -> new ItemStack(Items.WRITABLE_BOOK);
        };
    }

    private int tabAt(double mouseX, double mouseY) {
        if (mouseY < topPos - TAB_RISE || mouseY >= topPos) return -1;
        int index = (int) Math.floor((mouseX - leftPos) / TAB_STEP);
        return index >= 0 && index < TAB_NAMES.length && mouseX - leftPos - index * TAB_STEP < TAB_W ? index : -1;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tab == WORK) {
            super.renderLabels(graphics, mouseX, mouseY);
            work.renderLabels(graphics, font);
            return;
        }
        if (tab != CARE) {
            graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
            return;
        }
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, WorkerGolem.moodName(menu.state()), TEXT_X, 18, LABEL, false);
        String percent = Math.round(menu.meter()) + "%";
        graphics.drawString(font, percent, TEXT_RIGHT - font.width(percent), 18, LABEL, false);
        graphics.drawString(font, Component.translatable("quasium.golem.works", menu.productivity()), TEXT_X, 38, LABEL, false);
        graphics.drawString(font, Component.translatable("quasium.golem.misses", menu.misses(), GolemMood.STREAK), TEXT_X, 49, LABEL, false);
        graphics.drawString(font, Component.translatable("quasium.golem.streak", menu.streak(), GolemMood.STREAK), TEXT_X, 60, LABEL, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int clicked = tabAt(mouseX, mouseY);
        if (clicked >= 0) {
            if (clicked != tab) {
                select(clicked);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            }
            return true;
        }
        if (pane() != null && pane().mouseClicked(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return pane() != null && pane().mouseDragged(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        marks.mouseReleased();
        journal.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return pane() != null && pane().mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
