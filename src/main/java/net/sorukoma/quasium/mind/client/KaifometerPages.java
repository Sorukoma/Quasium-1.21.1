package net.sorukoma.quasium.mind.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.client.ClientMagicData;
import net.sorukoma.quasium.client.ClientTobaccoData;
import net.sorukoma.quasium.client.ClientZoneData;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.data.KaifAbilitiesHandler;
import net.sorukoma.quasium.data.PlayerFlavorPreferenceData;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.mind.Insight;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindIcons;
import net.sorukoma.quasium.mind.MindStats;
import net.sorukoma.quasium.mind.Perception;
import net.sorukoma.quasium.mind.Titles;
import net.sorukoma.quasium.mind.dream.DreamDraught;
import net.sorukoma.quasium.mind.dream.DreamOutcome;
import net.sorukoma.quasium.mind.dream.Dreams;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.progression.ConfigArgs;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
final class KaifometerPages {
    static final int LABEL = 0x404040;
    private static final int MUTED = 0x808080;
    private static final int GOOD = 0x2E6B2E;
    private static final int LINE = 10;
    private static final int ICON = 16;
    private static final int INDENT = ICON + 4;
    private static final int SLOT = 18;
    private static final float LOCKED = 0.8f;
    private static final float LOCKED_ALPHA = 0.5f;
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    private int scroll;
    private int overflow;
    private GuiGraphics graphics;
    private Font font;
    private int left;
    private int y;
    private int width;
    private int cursorX;
    private int cursorY;

    void reset() {
        scroll = 0;
    }

    boolean scroll(double amount) {
        if (overflow <= 0 || amount == 0) return false;
        scroll = Mth.clamp(scroll - (int) Math.signum(amount) * LINE * 2, 0, overflow);
        return true;
    }

    void render(String tab, GuiGraphics target, Font typeface, int x, int top, int areaWidth, int areaHeight, int mouseX, int mouseY) {
        graphics = target;
        font = typeface;
        left = x;
        width = areaWidth;
        cursorX = mouseX;
        cursorY = mouseY;
        y = top - scroll;
        graphics.enableScissor(x, top, x + areaWidth, top + areaHeight);
        switch (tab) {
            case "kaif" -> kaif();
            case "notes" -> KaifometerNotes.render(this);
            case "tolerance" -> tolerance();
            case "zone" -> zone();
            case "stats" -> stats();
            case "passport" -> passport();
            default -> dreams();
        }
        graphics.disableScissor();
        overflow = Math.max(0, y + scroll - top - areaHeight);
    }

    GuiGraphics graphics() {
        return graphics;
    }

    Font font() {
        return font;
    }

    int left() {
        return left;
    }

    int width() {
        return width;
    }

    int y() {
        return y;
    }

    void skip(int pixels) {
        y += pixels;
    }

    void text(Component text, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            graphics.drawString(font, line, left, y, color, false);
            y += LINE;
        }
    }

    private void key(String key, int color, Object... args) {
        text(Component.translatable("quasium.kaifometer." + key, args), color);
    }

    private void kaif() {
        int kaif = ClientKaifData.getKaifValue();
        KaifAbilitiesHandler.RankInfo rank = KaifAbilitiesHandler.getKaifRank(kaif);
        text(Component.translatable("quasium.kaifometer.kaif", kaif, Component.translatable(rank.translationKey())), rank.color() & 0xFFFFFF);
        GuiBars.bar(graphics, kaif < 0 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW, left, y + 1, width, (kaif + 100) / 200f);
        y += 9;
        List<Component> bonuses = new ArrayList<>();
        KaifAbilitiesHandler.getActiveAbilityNames(kaif, bonuses);
        key("bonuses", LABEL, bonuses.isEmpty() ? Component.translatable("quasium.kaifometer.none") : join(bonuses));
        int stage = ClientMind.data().getInt("Stage");
        key("overdose", stage > 0 ? 0x9A2A22 : LABEL, String.format(Locale.ROOT, "%.1f", ClientMind.data().getFloat("Grams")),
                ConfigArgs.synced(Config.OVERDOSE_WINDOW_MINUTES), number(ConfigArgs.synced(Config.OVERDOSE_THRESHOLD_GRAMS)),
                number(ConfigArgs.synced(MindConfig.OVERDOSE_STAGE2_GRAMS)), number(ConfigArgs.synced(MindConfig.OVERDOSE_STAGE3_GRAMS)));
        if (stage > 0) key("stage." + stage, 0x9A2A22);
        key("insight", LABEL, ClientMagicData.insight(), Insight.cap(Minecraft.getInstance().player));
        y += 3;
        key("perception", LABEL);
        perceive("perception.items", Perception.ITEMS, kaif >= ConfigArgs.synced(Config.SCAN_KAIF_ITEM), ConfigArgs.synced(Config.SCAN_KAIF_ITEM));
        perceive("perception.mobs", Perception.MOBS, kaif >= ConfigArgs.synced(Config.SCAN_KAIF_MOB), ConfigArgs.synced(Config.SCAN_KAIF_MOB));
        perceive("perception.ruins", Perception.RUINS, kaif >= ConfigArgs.synced(MindConfig.PERCEPTION_RUINS),
                ConfigArgs.synced(MindConfig.PERCEPTION_RUINS));
        perceive("perception.mirror", Perception.MIRROR, kaif <= ConfigArgs.synced(MindConfig.PERCEPTION_MIRROR),
                ConfigArgs.synced(MindConfig.PERCEPTION_MIRROR));
    }

    private void perceive(String key, String felt, boolean on, int threshold) {
        if (!ClientMind.flag(felt)) {
            key("perception.unknown", MUTED);
            return;
        }
        key(key, on ? GOOD : MUTED, threshold);
    }

    private void tolerance() {
        key("tolerance.rule", MUTED, ConfigArgs.synced(MindConfig.TOLERANCE_SESSION_GRAMS), ConfigArgs.synced(MindConfig.TOLERANCE_PER_SESSION),
                ConfigArgs.synced(MindConfig.TOLERANCE_DECAY_PER_HOUR));
        y += 2;
        CompoundTag tolerances = ClientMind.data().getCompound("Tolerance");
        int cap = Math.max(1, ConfigArgs.synced(MindConfig.TOLERANCE_CAP));
        int column = width / 2;
        int top = y;
        List<String> flavors = PlayerFlavorPreferenceData.FLAVORS;
        int rows = (flavors.size() + 1) / 2;
        for (int index = 0; index < flavors.size(); index++) {
            String flavor = flavors.get(index);
            int x = left + (index / rows) * column;
            int rowY = top + (index % rows) * 12;
            int value = tolerances.getInt(flavor);
            boolean tried = ClientTobaccoData.getTriedFlavors().contains(flavor);
            String name = font.plainSubstrByWidth(Component.translatable("quasium.flavor." + flavor).getString(), column - 50);
            graphics.drawString(font, name, x, rowY, tried ? LABEL : MUTED, false);
            GuiBars.bar(graphics, BossEvent.BossBarColor.PURPLE, x + column - 46, rowY + 2, 30, value / (float) cap);
            graphics.drawString(font, String.valueOf(value), x + column - 14, rowY, LABEL, false);
        }
        y = top + rows * 12;
    }

    private void zone() {
        int tier = ClientZoneData.tier();
        key("zone", LABEL, tier == 0 ? Component.translatable("quasium.kaifometer.zone.none") : Component.translatable("quasium.zone.tier." + tier));
        if (tier > 0) key("zone.effects." + tier, MUTED);
        y += 3;
        String school = ClientMind.school();
        key("school", LABEL, school.isEmpty() ? Component.translatable("quasium.kaifometer.none") : Component.translatable("quasium.research.school." + school));
        perks();
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        long cursed = ClientMind.data().getLong("CursedUntil") - now;
        if (cursed > 0) key("cursed", 0x9A2A22, cursed / 1200 + 1, ConfigArgs.synced(MindConfig.GHOST_CURSE_MULTIPLIER));
        if (ClientMind.data().getLong(Dreams.INSPIRED_UNTIL) > now) key("inspired", GOOD, ConfigArgs.synced(MindConfig.INSPIRATION_PCT));
        long enlightened = ClientMind.data().getLong(Insight.ENLIGHTENED_AGAIN_AT) - now;
        key(enlightened > 0 ? "enlightenment.wait" : "enlightenment.ready", MUTED, enlightened / 1200 + 1,
                ConfigArgs.synced(MindConfig.ENLIGHTENMENT_HOLD_SECONDS));
    }

    private void stats() {
        CompoundTag stats = ClientMind.data().getCompound("Stats");
        for (var stat : MindStats.ALL) {
            String name = stat.getId().getPath();
            int value = stats.getInt(name);
            Object shown = name.equals("grams_smoked") ? String.format(Locale.ROOT, "%.1f", value / 10f) : value;
            text(Component.translatable("stat.quasium." + name).append(": ").append(String.valueOf(shown)), LABEL);
            y += 2;
        }
    }

    private void passport() {
        int kaif = ClientKaifData.getKaifValue();
        key("name", LABEL, Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getGameProfile().getName());
        key("rank", LABEL, Component.translatable(KaifAbilitiesHandler.getKaifRank(kaif).translationKey()));
        String school = ClientMind.school();
        key("school", LABEL, school.isEmpty() ? Component.translatable("quasium.kaifometer.none") : Component.translatable("quasium.research.school." + school));
        key("researched", LABEL, ClientMind.strings(MindData.RESEARCH).size(), ClientMind.strings(MindData.PERKS).size());
        key("tried", LABEL, ClientMind.data().getInt("Tried"), PlayerFlavorPreferenceData.FLAVORS.size());
        y += 3;
        key("titles", LABEL);
        Set<String> earned = ClientMind.strings(MindData.TITLES);
        for (Titles title : Titles.values()) {
            boolean has = earned.contains(title.id());
            row(MindIcons.title(title), has, Component.translatable("quasium.title." + title.id()), has ? 0xB8860B : MUTED,
                    has ? null : Component.translatable("quasium.title." + title.id() + ".how", titleGoal(title)));
        }
    }

    private static Object titleGoal(Titles title) {
        return switch (title) {
            case SOMMELIER -> ConfigArgs.synced(MindConfig.TITLE_SOMMELIER_FLAVORS);
            case IRON_LUNGS -> ConfigArgs.synced(MindConfig.TITLE_IRON_LUNGS_PUFFS);
            default -> "";
        };
    }

    private void dreams() {
        CompoundTag journal = ClientMind.data().getCompound(MindData.DREAMS);
        key("dreams.rule", MUTED, ConfigArgs.synced(MindConfig.DREAM_KAIF), ConfigArgs.synced(MindConfig.NIGHTMARE_KAIF));
        if (ClientMind.data().getBoolean(DreamDraught.PENDING)) key("dreams.draught", GOOD);
        y += 2;
        for (DreamOutcome outcome : DreamOutcome.values()) {
            int count = journal.getInt(outcome.id());
            row(MindIcons.dream(outcome), count > 0, outcome.title().copy().append(" x" + count), count > 0 ? LABEL : MUTED,
                    Component.translatable("quasium.dream." + outcome.id() + ".what"));
        }
        int nightmares = journal.getInt(Dreams.NIGHTMARES);
        row(MindIcons.NIGHTMARE, nightmares > 0, Component.translatable("quasium.dream.nightmare").append(" x" + nightmares),
                nightmares > 0 ? 0x9A2A22 : MUTED, null);
    }

    private void perks() {
        Set<String> owned = ClientMind.strings(MindData.PERKS);
        Perk[] perks = Perk.values();
        key("perks", LABEL, Arrays.stream(perks).filter(perk -> owned.contains(perk.id())).count() + "/" + perks.length);
        int columns = Math.max(1, width / SLOT);
        for (int index = 0; index < perks.length; index++) {
            Perk perk = perks[index];
            boolean has = owned.contains(perk.id());
            int x = left + index % columns * SLOT;
            int top = y + index / columns * SLOT;
            graphics.blitSprite(SLOT_SPRITE, x, top, SLOT, SLOT);
            icon(MindIcons.perk(perk), x + 1, top + 1, has);
            if (hovered(x, top)) hint(Component.translatable("quasium.perk." + perk.id()).withStyle(has ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        }
        y += (perks.length + columns - 1) / columns * SLOT + 3;
    }

    private void row(ResourceLocation sprite, boolean lit, Component name, int color, @Nullable Component note) {
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(name, width - INDENT));
        int named = lines.size();
        if (note != null) lines.addAll(font.split(note, width - INDENT));
        int height = Math.max(ICON, lines.size() * LINE);
        icon(sprite, left, y + (height - ICON) / 2, lit);
        int top = y + (height - lines.size() * LINE) / 2 + 1;
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), left + INDENT, top + index * LINE, index < named ? color : MUTED, false);
        }
        y += height + 3;
    }

    private void icon(ResourceLocation sprite, int x, int top, boolean lit) {
        if (!lit) {
            RenderSystem.enableBlend();
            graphics.setColor(LOCKED, LOCKED, LOCKED, LOCKED_ALPHA);
        }
        graphics.blitSprite(sprite, x, top, ICON, ICON);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private boolean hovered(int x, int top) {
        return cursorX >= x && cursorX < x + SLOT && cursorY >= top && cursorY < top + SLOT && graphics.containsPointInScissor(cursorX, cursorY);
    }

    private static void hint(Component text) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) screen.setTooltipForNextRenderPass(text);
    }

    private static Component join(List<Component> parts) {
        return ComponentUtils.formatList(parts, Component.literal(", "));
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
