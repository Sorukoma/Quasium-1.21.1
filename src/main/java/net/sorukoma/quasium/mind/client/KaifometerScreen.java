package net.sorukoma.quasium.mind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.mind.MindPackets;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class KaifometerScreen extends Screen {
    static final int WIDTH = 196;
    static final int HEIGHT = 172;
    private static final int TAB_W = 26;
    private static final int TAB_H = 32;
    private static final int TAB_STEP = 27;
    private static final int TAB_RISE = 28;
    private static final int SYNC_TICKS = 40;
    private static final List<String> TABS = List.of("kaif", "notes", "tolerance", "zone", "stats", "passport", "dreams");
    private static int lastTab;

    private final KaifometerPages pages = new KaifometerPages();
    private int tab = lastTab;
    private int left;
    private int top;
    private int age;

    public KaifometerScreen() {
        super(Component.translatable("item.quasium.kaifometer"));
    }

    public static void open() {
        MindPackets.act("sync", "");
        Minecraft.getInstance().setScreen(new KaifometerScreen());
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = Math.max(TAB_RISE, (height - HEIGHT + TAB_RISE) / 2);
        pages.reset();
    }

    @Override
    public void tick() {
        if (++age % SYNC_TICKS == 0) MindPackets.act("sync", "");
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        for (int index = 0; index < TABS.size(); index++) {
            if (index != tab) renderTab(graphics, index);
        }
        graphics.blitSprite(MindDialog.PANEL, left, top, WIDTH, HEIGHT);
        renderTab(graphics, tab);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, Component.translatable("quasium.kaifometer.tab." + TABS.get(tab)), left + 8, top + 6, KaifometerPages.LABEL, false);
        pages.render(TABS.get(tab), graphics, font, left + 8, top + 18, WIDTH - 16, HEIGHT - 26, mouseX, mouseY);
        int hovered = tabAt(mouseX, mouseY);
        if (hovered >= 0) graphics.renderTooltip(font, Component.translatable("quasium.kaifometer.tab." + TABS.get(hovered)), mouseX, mouseY);
    }

    private void renderTab(GuiGraphics graphics, int index) {
        String sprite = "container/creative_inventory/tab_top_" + (index == tab ? "selected_" : "unselected_") + (index + 1);
        int x = left + index * TAB_STEP;
        int y = top - TAB_RISE;
        graphics.blitSprite(ResourceLocation.withDefaultNamespace(sprite), x, y, TAB_W, TAB_H);
        graphics.renderItem(icon(index), x + 5, y + 9);
    }

    private static ItemStack icon(int index) {
        return switch (index) {
            case 0 -> new ItemStack(ModItems.KAIFOMETER.get());
            case 1 -> new ItemStack(ModBlocks.NOTE_JAR_ITEM.get());
            case 2 -> new ItemStack(ModItems.MINT_TOBACCO.get());
            case 3 -> new ItemStack(LoungeBlocks.LOUNGE_CUSHION_ITEM.get());
            case 4 -> new ItemStack(Items.WRITABLE_BOOK);
            case 5 -> new ItemStack(Items.NAME_TAG);
            default -> new ItemStack(Items.RED_BED);
        };
    }

    private int tabAt(double mouseX, double mouseY) {
        if (mouseY < top - TAB_RISE || mouseY >= top) return -1;
        int index = Mth.floor((mouseX - left) / TAB_STEP);
        return index >= 0 && index < TABS.size() && mouseX - left - index * TAB_STEP < TAB_W ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int clicked = tabAt(mouseX, mouseY);
        if (clicked < 0 || clicked == tab) return super.mouseClicked(mouseX, mouseY, button);
        tab = clicked;
        lastTab = clicked;
        pages.reset();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return pages.scroll(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
