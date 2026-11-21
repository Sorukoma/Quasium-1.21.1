package net.sorukoma.quasium.client.tetrad;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;

import java.util.Collection;
import java.util.Map;

public final class TetradArt {
    public static final ResourceLocation PANEL = sprite("panel");
    public static final ResourceLocation INSET = sprite("inset");
    public static final ResourceLocation SLOT = sprite("slot");
    public static final ResourceLocation SLOT_HOVER = sprite("slot_hover");
    public static final ResourceLocation SLOT_PICKED = sprite("slot_picked");
    public static final ResourceLocation SLOT_LOCKED = sprite("slot_locked");
    public static final ResourceLocation ROW = sprite("row");
    public static final ResourceLocation RIBBON = sprite("ribbon");
    public static final ResourceLocation REMOVE = sprite("remove");
    public static final ResourceLocation LOCK = sprite("lock");
    public static final ResourceLocation SEAL = sprite("seal");
    public static final ResourceLocation WHEEL_SLOT = sprite("wheel_slot");
    public static final ResourceLocation WHEEL_SLOT_HOVER = sprite("wheel_slot_hover");
    public static final ResourceLocation WHEEL_SLOT_EMPTY = sprite("wheel_slot_empty");
    public static final ResourceLocation WHEEL_HUB = sprite("wheel_hub");
    public static final int ICON = 16;
    public static final int CELL = 20;
    public static final int INK = 0x404040;
    public static final int DIM = 0x7A7A7A;
    public static final int WARN = 0xA8232A;
    public static final int OVERFLOW = 0xD9A441;
    private static final Map<String, Integer> TOP_COLORS = Map.of("power", 0xE05A3A, "breadth", 0x5AA6E0, "reach", 0x7FD0A0,
            "length", 0xC9A0FF);

    private TetradArt() {
    }

    public static ResourceLocation form(String id) {
        return sprite("form/" + id);
    }

    public static ResourceLocation effect(String id) {
        return sprite("effect/" + id);
    }

    public static ResourceLocation top(String id) {
        return sprite("top/" + id);
    }

    public static int topColor(String top) {
        return TOP_COLORS.getOrDefault(top, OVERFLOW);
    }

    public static void dots(GuiGraphics graphics, Collection<NoteType> notes, int x, int y) {
        int left = x;
        for (NoteType note : notes) {
            graphics.fill(left, y, left + 4, y + 4, 0xFF000000);
            graphics.fill(left + 1, y + 1, left + 3, y + 3, 0xFF000000 | note.color());
            left += 5;
        }
    }

    public static int dotsWidth(Collection<NoteType> notes) {
        return notes.isEmpty() ? 0 : notes.size() * 5 - 1;
    }

    private static ResourceLocation sprite(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "tetrad/" + path);
    }
}
