package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record PageCatalog(Map<String, PageForm> forms, Map<String, PageEffect> effects, Map<String, PageTop> tops,
                          Map<String, Accord> accords, Map<String, EssenceNotes> essences) {
    public static final PageCatalog EMPTY = new PageCatalog(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

    public static final Codec<PageCatalog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PageForm.CODEC).fieldOf("forms").forGetter(PageCatalog::forms),
            Codec.unboundedMap(Codec.STRING, PageEffect.CODEC).fieldOf("effects").forGetter(PageCatalog::effects),
            Codec.unboundedMap(Codec.STRING, PageTop.CODEC).fieldOf("tops").forGetter(PageCatalog::tops),
            Codec.unboundedMap(Codec.STRING, Accord.CODEC).fieldOf("accords").forGetter(PageCatalog::accords),
            Codec.unboundedMap(Codec.STRING, EssenceNotes.CODEC).fieldOf("essences").forGetter(PageCatalog::essences)
    ).apply(instance, PageCatalog::new));

    private static volatile PageCatalog current = EMPTY;

    public static PageCatalog get() {
        return current;
    }

    public static void set(PageCatalog catalog) {
        current = catalog;
        PageSpells.clear();
    }

    public static void setForms(Map<ResourceLocation, PageForm> loaded) {
        PageCatalog old = current;
        set(new PageCatalog(byPath(loaded), old.effects, old.tops, old.accords, old.essences));
    }

    public static void setEffects(Map<ResourceLocation, PageEffect> loaded) {
        PageCatalog old = current;
        set(new PageCatalog(old.forms, byPath(loaded), old.tops, old.accords, old.essences));
    }

    public static void setTops(Map<ResourceLocation, PageTop> loaded) {
        PageCatalog old = current;
        set(new PageCatalog(old.forms, old.effects, byPath(loaded), old.accords, old.essences));
    }

    public static void setAccords(Map<ResourceLocation, Accord> loaded) {
        PageCatalog old = current;
        set(new PageCatalog(old.forms, old.effects, old.tops, byPath(loaded), old.essences));
    }

    public static void setEssences(Map<ResourceLocation, EssenceNotes> loaded) {
        PageCatalog old = current;
        set(new PageCatalog(old.forms, old.effects, old.tops, old.accords, byPath(loaded)));
    }

    private static <T> Map<String, T> byPath(Map<ResourceLocation, T> loaded) {
        Map<String, T> byPath = new TreeMap<>();
        loaded.forEach((id, value) -> byPath.put(id.getPath(), value));
        return Map.copyOf(byPath);
    }

    @Nullable
    public PageForm form(String id) {
        return forms.get(id);
    }

    @Nullable
    public PageEffect effect(String id) {
        return effects.get(id);
    }

    @Nullable
    public PageTop top(String id) {
        return tops.get(id);
    }

    @Nullable
    public String accordOf(NoteType a, NoteType b) {
        if (a == b) return null;
        for (Map.Entry<String, Accord> entry : accords.entrySet()) {
            if (entry.getValue().joins(a, b)) return entry.getKey();
        }
        return null;
    }

    public Set<NoteType> notesOf(String essence) {
        NoteType note = NoteType.byId(essence);
        if (note != null) return EnumSet.of(note);
        EssenceNotes compound = essences.get(essence);
        return compound == null || compound.notes().isEmpty() ? Set.of() : EnumSet.copyOf(compound.notes());
    }
}
