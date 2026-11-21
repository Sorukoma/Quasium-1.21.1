package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class NoteProfiles {
    public static final Codec<Map<ResourceLocation, NoteProfile>> MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, NoteProfile.CODEC);

    private static volatile Map<ResourceLocation, NoteProfile> profiles = Map.of();

    private NoteProfiles() {
    }

    public static void set(Map<ResourceLocation, NoteProfile> loaded) {
        profiles = Collections.unmodifiableMap(new TreeMap<>(loaded));
    }

    public static Map<ResourceLocation, NoteProfile> all() {
        return profiles;
    }

    public static Map<ResourceLocation, NoteProfile> known() {
        Map<ResourceLocation, NoteProfile> known = new TreeMap<>(DerivedNotes.all());
        known.putAll(profiles);
        return known;
    }

    @Nullable
    public static NoteProfile get(ResourceLocation id) {
        NoteProfile explicit = profiles.get(id);
        return explicit != null ? explicit : DerivedNotes.all().get(id);
    }

    @Nullable
    public static Map.Entry<ResourceLocation, NoteProfile> forStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Map.Entry<ResourceLocation, NoteProfile> explicit = explicitFor(stack);
        return explicit != null ? explicit : DerivedNotes.forItem(stack.getItem());
    }

    @Nullable
    public static Map.Entry<ResourceLocation, NoteProfile> explicitFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Map.Entry<ResourceLocation, NoteProfile> tagMatch = null;
        for (Map.Entry<ResourceLocation, NoteProfile> entry : profiles.entrySet()) {
            NoteProfile profile = entry.getValue();
            if (profile.item().isPresent() && stack.is(profile.item().get())) return entry;
            if (tagMatch == null && profile.tag().isPresent() && stack.is(profile.tag().get())) tagMatch = entry;
        }
        return tagMatch;
    }

    @Nullable
    public static Map.Entry<ResourceLocation, NoteProfile> forEntity(EntityType<?> type) {
        Map.Entry<ResourceLocation, NoteProfile> explicit = explicitFor(type);
        return explicit != null ? explicit : DerivedNotes.forEntity(type);
    }

    @Nullable
    public static Map.Entry<ResourceLocation, NoteProfile> explicitFor(EntityType<?> type) {
        for (Map.Entry<ResourceLocation, NoteProfile> entry : profiles.entrySet()) {
            if (entry.getValue().entity().filter(type::equals).isPresent()) return entry;
        }
        return null;
    }
}
