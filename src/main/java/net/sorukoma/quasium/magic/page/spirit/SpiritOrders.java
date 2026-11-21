package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

record SpiritOrders(ResourceLocation spellId, float power, double speed, double turn, double search, int launchTicks, int lifeTicks,
                    int guardTicks, int tauntTicks, boolean heals, boolean silent, int color, @Nullable NoteType note) {
    byte face() {
        return (byte) (note == null ? 0 : note.ordinal() + 1);
    }
}
