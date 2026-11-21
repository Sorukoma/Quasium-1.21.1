package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public record NoteRule(Optional<TagKey<Item>> tag, Optional<String> word, Optional<NoteTrait> trait,
                       Optional<TagKey<EntityType<?>>> mobTag, Optional<MobCategory> mobCategory,
                       int priority, boolean fallback, Map<NoteType, Integer> notes) {

    private static final Codec<NoteRule> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(NoteRule::tag),
            Codec.STRING.optionalFieldOf("word").forGetter(NoteRule::word),
            NoteTrait.CODEC.optionalFieldOf("trait").forGetter(NoteRule::trait),
            TagKey.codec(Registries.ENTITY_TYPE).optionalFieldOf("mob_tag").forGetter(NoteRule::mobTag),
            MobCategory.CODEC.optionalFieldOf("mob_category").forGetter(NoteRule::mobCategory),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(NoteRule::priority),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(NoteRule::fallback),
            Codec.unboundedMap(NoteType.CODEC, ExtraCodecs.POSITIVE_INT).fieldOf("notes").forGetter(NoteRule::notes)
    ).apply(instance, NoteRule::new));

    public static final Codec<NoteRule> CODEC = RAW_CODEC.validate(NoteRule::validate);
    public static final Codec<List<NoteRule>> FILE_CODEC = CODEC.listOf().fieldOf("rules").codec();

    public NoteRule {
        EnumMap<NoteType, Integer> ordered = new EnumMap<>(NoteType.class);
        ordered.putAll(notes);
        notes = Collections.unmodifiableMap(ordered);
    }

    private static DataResult<NoteRule> validate(NoteRule rule) {
        long matchers = Stream.of(rule.tag, rule.word, rule.trait, rule.mobTag, rule.mobCategory).filter(Optional::isPresent).count();
        if (matchers != 1) return DataResult.error(() -> "note rule needs exactly one of tag, word, trait, mob_tag, mob_category");
        if (rule.notes.isEmpty()) return DataResult.error(() -> "note rule has no notes");
        return DataResult.success(rule);
    }

    public boolean forMobs() {
        return mobTag.isPresent() || mobCategory.isPresent() || trait.filter(NoteTrait::forMobs).isPresent();
    }

    public boolean matches(Item item) {
        if (tag.isPresent()) return item.builtInRegistryHolder().is(tag.get());
        if (word.isPresent()) return words(item).contains(word.get());
        return trait.isPresent() && trait.get().matches(item);
    }

    public boolean matches(EntityType<?> type) {
        if (mobTag.isPresent()) return type.is(mobTag.get());
        if (mobCategory.isPresent()) return type.getCategory() == mobCategory.get();
        return trait.isPresent() && trait.get().matches(type);
    }

    private static List<String> words(Item item) {
        return Arrays.asList(BuiltInRegistries.ITEM.getKey(item).getPath().split("[_/]"));
    }
}
