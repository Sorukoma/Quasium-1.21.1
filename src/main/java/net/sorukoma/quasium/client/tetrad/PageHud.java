package net.sorukoma.quasium.client.tetrad;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCompiler;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.SpellPage;
import net.sorukoma.quasium.magic.page.SpellTetradItem;
import net.sorukoma.quasium.magic.page.Tetrads;
import net.sorukoma.quasium.ritual.artifact.QuasiumHarness;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class PageHud {
    public static final String SPIRIT_MIX = "spirit_mix";
    private static final int ZONE_ALPHA = 0x90000000;
    private static final int EDGE = 2;

    private PageHud() {
    }

    @Nullable
    public static CompiledPage activePage(Player player) {
        ItemStack tetrad = Tetrads.find(player);
        if (tetrad.isEmpty()) return null;
        SpellPage page = SpellTetradItem.contents(tetrad).activePage();
        if (page.isEmpty()) return null;
        Optional<CompiledPage> compiled = PageCompiler.compile(page, 0, null);
        return compiled.orElse(null);
    }

    public static boolean ready(Player player) {
        boolean pipe = player.getMainHandItem().is(ModItems.HOOKAH_PIPE.get()) || player.getOffhandItem().is(ModItems.HOOKAH_PIPE.get());
        boolean backpack = ClientHookahInputHandler.isConnectedToBackpack() && !QuasiumHarness.backpack(player).isEmpty();
        return (pipe || backpack) && activePage(player) != null;
    }

    public static Component missing(Font font, String tokens, int right) {
        MutableComponent names = Component.empty();
        String[] ids = tokens.split(",");
        for (int i = 0; i < ids.length; i++) names.append(i == 0 ? "" : ", ").append(token(ids[i].trim()));
        MutableComponent line = Component.translatable("quasium.tetrad.hud.missing").append(" ").append(names);
        return font.width(line) <= right - EDGE ? line : names;
    }

    public static Component token(String id) {
        NoteType note = NoteType.byId(id);
        if (note != null) return note.displayName();
        if (SPIRIT_MIX.equals(id)) return Component.translatable("quasium.tetrad.hud.spirit_mix");
        return Component.literal(id);
    }

    public static void drawLabel(GuiGraphics graphics, Font font, Component label, Set<NoteType> notes, int right, int y, int color) {
        int dots = notes.isEmpty() ? 0 : TetradArt.dotsWidth(notes) + 3;
        int room = Math.max(1, right - dots - EDGE);
        FormattedCharSequence text = font.width(label) <= room ? label.getVisualOrderText() : font.split(label, room).getFirst();
        int textX = right - font.width(text);
        graphics.drawString(font, text, textX, y, color, true);
        if (!notes.isEmpty()) TetradArt.dots(graphics, notes, textX - dots, y + 2);
    }

    public static void overflowZone(GuiGraphics graphics, CompiledPage page, int x, int y, int length, float capacity, int volume) {
        if (volume >= capacity) return;
        int overflow = PageConfig.OVERFLOW_STEP_ML.getAsInt() * PageConfig.OVERFLOW_MAX_STEPS.getAsInt();
        int from = x + Math.round(length * volume / capacity);
        int to = x + Math.round(length * Math.min(capacity, volume + overflow) / capacity);
        String target = page.page().overflow();
        int color = target.isEmpty() ? page.spell().color() : TetradArt.topColor(target);
        graphics.fill(from, y, to, y + GuiBars.THICKNESS, ZONE_ALPHA | color);
    }
}
