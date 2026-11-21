package net.sorukoma.quasium.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.tetrad.PageHud;
import net.sorukoma.quasium.data.KaifAbilitiesHandler;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCasting;
import net.sorukoma.quasium.magic.page.PageLabels;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class ContextBar {
    private static final Set<ResourceLocation> REPLACED = Set.of(VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL);
    private static final Component LUNGS = Component.translatable("quasium.hud.lungs");
    private static final int LENGTH = 182;
    private static final int GAP = 4;
    private static final int TEXT = 0xFFFFFF;
    private static final int GOLD = 0xFFFFD27A;
    private static final int CHOKE = 0xFFE04040;

    private ContextBar() {
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "context_bar"),
                ContextBar::render);
    }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (REPLACED.contains(event.getName()) && shown(Minecraft.getInstance().player)) event.setCanceled(true);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!shown(player) || minecraft.options.hideGui) return;
        int x = graphics.guiWidth() / 2 - LENGTH / 2;
        int y = graphics.guiHeight() - 29;
        if (breathing() || PageHud.ready(player)) {
            lungs(graphics, minecraft.font, player, x, y);
        } else {
            kaif(graphics, minecraft.font, x, y);
        }
    }

    private static boolean shown(LocalPlayer player) {
        return player != null && !player.isSpectator() && player.jumpableVehicle() == null
                && (breathing() || PageHud.ready(player) || player.getMainHandItem().is(ModItems.KAIFOMETER.get())
                || player.getOffhandItem().is(ModItems.KAIFOMETER.get()));
    }

    private static boolean breathing() {
        return ClientMagicData.spell() != null || ClientMagicData.lungs() > 0f || !ClientMagicData.missingNotes().isEmpty();
    }

    private static void lungs(GuiGraphics graphics, Font font, Player player, int x, int y) {
        ResourceLocation spell = ClientMagicData.spell();
        float capacity = ClientMagicData.capacity();
        float lungs = ClientMagicData.lungs();
        CompiledPage page = PageHud.activePage(player);
        boolean choking = ClientMagicData.coughTicks() > 0 && ClientMagicData.holdTicks() > ClientMagicData.coughTicks() * 3 / 4;
        boolean blink = choking && (player.tickCount / 3) % 2 == 0;
        boolean ready = spell != null && lungs >= ClientMagicData.cost();
        GuiBars.bar(graphics, blink ? BossBarColor.RED : ready ? BossBarColor.PURPLE : BossBarColor.WHITE, x, y, LENGTH, lungs / capacity);
        int volume = spell != null && ClientMagicData.cost() > 0 ? ClientMagicData.cost() : page == null ? 0 : page.volume();
        if (page != null && volume > 0) PageHud.overflowZone(graphics, page, x, y, LENGTH, capacity, volume);
        if (volume > 0) mark(graphics, x + Math.round(Math.min(1f, volume / capacity) * LENGTH), y);
        label(graphics, font, spell, page, x - GAP, y - 3);
        String amount = Math.round(lungs) + "/" + ClientMagicData.capacity();
        int right = labelRight(graphics, font, amount, x, y, TEXT);
        if (ClientMagicData.charge() > 1.001f) {
            graphics.drawString(font, String.format("x%.2f", ClientMagicData.charge()), right + GAP, y - 3, blink ? CHOKE : GOLD, true);
        }
    }

    private static void label(GuiGraphics graphics, Font font, @Nullable ResourceLocation spell, @Nullable CompiledPage page, int right, int y) {
        String missing = ClientMagicData.missingNotes();
        if (!missing.isEmpty()) {
            PageHud.drawLabel(graphics, font, PageHud.missing(font, missing, right), Set.of(), right, y, CHOKE);
            return;
        }
        if (page != null) {
            PageHud.drawLabel(graphics, font, PageLabels.of(PageCasting.label(page.page())), page.notes(), right, y, TEXT);
            return;
        }
        String label = ClientMagicData.pageLabel();
        Component text = !label.isEmpty() ? PageLabels.of(label) : spell == null ? LUNGS : Spell.displayName(spell);
        PageHud.drawLabel(graphics, font, text, Set.of(), right, y, TEXT);
    }

    private static void kaif(GuiGraphics graphics, Font font, int x, int y) {
        int kaif = ClientKaifData.getKaifValue();
        KaifAbilitiesHandler.RankInfo rank = KaifAbilitiesHandler.getKaifRank(kaif);
        int center = LENGTH / 2;
        int reach = Math.round(Math.abs(kaif) / (float) PlayerKaifData.MAX_KAIF * center);
        GuiBars.track(graphics, BossBarColor.WHITE, x, y, LENGTH);
        GuiBars.tint(graphics, rank.color());
        GuiBars.fill(graphics, BossBarColor.WHITE, x, y, kaif < 0 ? center - reach : center, kaif < 0 ? center : center + reach);
        graphics.setColor(1f, 1f, 1f, 1f);
        mark(graphics, x + center, y);
        labelLeft(graphics, font, Component.translatable(rank.translationKey()), x, y, rank.color());
        labelRight(graphics, font, kaif + "/" + PlayerKaifData.MAX_KAIF, x, y, TEXT);
    }

    private static void mark(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y - 1, x + 1, y + GuiBars.THICKNESS + 1, GOLD);
    }

    private static void labelLeft(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x - GAP - font.width(text), y - 3, color, true);
    }

    private static int labelRight(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        int left = x + LENGTH + GAP;
        graphics.drawString(font, text, left, y - 3, color, true);
        return left + font.width(text);
    }
}
