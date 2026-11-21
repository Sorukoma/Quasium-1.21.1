package net.sorukoma.quasium.client.gui.book;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record BookPalette(ResourceLocation frame, ResourceLocation toggle, ResourceLocation toggleLit, int label, int title, int body,
                          int muted, int accent, int rule, int rowSelected, int rowHover, int marker, int lockedShade, int scrollTrack,
                          int met, int unmet) {
    static final BookPalette LIGHT = new BookPalette(sprite("frame"), sprite("theme_dark"), sprite("theme_dark_lit"),
            0xFF404040, 0xFF1E1E1E, 0xFF363636, 0xFF8C8273, 0xFF7A4A1C, 0x30000000, 0x338B5A2B, 0x1A8B5A2B, 0xFF8B5A2B,
            0x99FFFAEE, 0x1A000000, 0xFF2E6B2E, 0xFF9A2A22);
    static final BookPalette DARK = new BookPalette(sprite("frame_dark"), sprite("theme_light"), sprite("theme_light_lit"),
            0xFFD8CDB8, 0xFFF2E8D5, 0xFFDDD3C2, 0xFF9C9384, 0xFFE0A35C, 0x30FFFFFF, 0x40E0A35C, 0x20E0A35C, 0xFFE0A35C,
            0x992A2521, 0x1AFFFFFF, 0xFF8FD18F, 0xFFF08A7A);

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "book/" + name);
    }
}
