package net.sorukoma.quasium.magic.page;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.Spell;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CompiledPage(ResourceLocation id, SpellPage page, Spell spell, List<String> hearts, Map<String, Integer> tops,
                           List<String> accords, Set<NoteType> notes, Map<NoteType, Integer> noteCost, int volume,
                           @Nullable ResourceLocation legacyId) {
    public boolean isSeal() {
        return legacyId != null;
    }

    public int topCount(String top) {
        return tops.getOrDefault(top, 0);
    }

    public float param(String key, float fallback) {
        return spell.param(key, fallback);
    }
}
