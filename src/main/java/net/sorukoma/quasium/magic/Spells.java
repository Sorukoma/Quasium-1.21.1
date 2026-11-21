package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageSpells;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class Spells {
    public static final Codec<Map<ResourceLocation, Spell>> MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Spell.CODEC);

    private static volatile Map<ResourceLocation, Spell> spells = Map.of();

    private Spells() {
    }

    public static void set(Map<ResourceLocation, Spell> loaded) {
        spells = Collections.unmodifiableMap(new TreeMap<>(loaded));
    }

    public static Map<ResourceLocation, Spell> all() {
        return spells;
    }

    @Nullable
    public static Spell get(@Nullable ResourceLocation id) {
        if (id == null) return null;
        Spell spell = spells.get(id);
        if (spell != null) return spell;
        CompiledPage page = PageSpells.get(id);
        return page == null ? null : page.spell();
    }

    @Nullable
    public static Spell registered(@Nullable ResourceLocation id) {
        return id == null ? null : spells.get(id);
    }

    @Nullable
    public static ResourceLocation match(List<String> essences) {
        return match(essences, Set.of());
    }

    @Nullable
    public static ResourceLocation match(List<String> essences, Set<ResourceLocation> knownHidden) {
        for (Map.Entry<ResourceLocation, Spell> entry : spells.entrySet()) {
            if (entry.getValue().hidden() && !knownHidden.contains(entry.getKey())) continue;
            if (MagicMath.matchAlternative(entry.getValue().inputs(), essences) >= 0) return entry.getKey();
        }
        return null;
    }

    public static List<FocusEssence> focusOf(ItemStack bowl) {
        return bowl.getOrDefault(ModDataComponents.BOWL_FOCUS.get(), List.of());
    }

    @Nullable
    public static ResourceLocation forBowl(ItemStack bowl, Set<ResourceLocation> knownHidden) {
        if (bowl.isEmpty()) return null;
        return match(FocusEssence.activeEssences(focusOf(bowl)), knownHidden);
    }
}
