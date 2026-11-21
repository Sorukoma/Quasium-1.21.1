package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.item.EssenceBottleItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record NoteProfile(Optional<Item> item, Optional<TagKey<Item>> tag, Optional<EntityType<?>> entity,
                          Map<NoteType, Integer> notes) {

    private static final Codec<NoteProfile> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(NoteProfile::item),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(NoteProfile::tag),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("entity").forGetter(NoteProfile::entity),
            Codec.unboundedMap(NoteType.CODEC, ExtraCodecs.POSITIVE_INT).fieldOf("notes").forGetter(NoteProfile::notes)
    ).apply(instance, NoteProfile::new));

    public static final Codec<NoteProfile> CODEC = RAW_CODEC.validate(NoteProfile::validate);

    public NoteProfile {
        EnumMap<NoteType, Integer> ordered = new EnumMap<>(NoteType.class);
        ordered.putAll(notes);
        notes = Collections.unmodifiableMap(ordered);
    }

    private static DataResult<NoteProfile> validate(NoteProfile profile) {
        int targets = (profile.item.isPresent() ? 1 : 0) + (profile.tag.isPresent() ? 1 : 0) + (profile.entity.isPresent() ? 1 : 0);
        if (targets != 1) return DataResult.error(() -> "note profile needs exactly one of item, tag, entity");
        if (profile.notes.isEmpty()) return DataResult.error(() -> "note profile has no notes");
        return DataResult.success(profile);
    }

    public boolean matches(ItemStack stack) {
        if (item.isPresent()) return stack.is(item.get());
        return tag.isPresent() && stack.is(tag.get());
    }

    public boolean isEntity() {
        return entity.isPresent();
    }

    public List<ItemStack> sourceStacks() {
        if (item.isPresent()) return List.of(new ItemStack(item.get()));
        return tag.map(key -> Arrays.asList(Ingredient.of(key).getItems())).orElse(List.of());
    }

    public List<ItemStack> essenceStacks() {
        return notes.entrySet().stream().map(note -> EssenceBottleItem.of(note.getKey().id(), note.getValue())).toList();
    }

    public int totalMl() {
        return notes.values().stream().mapToInt(Integer::intValue).sum();
    }

    public MutableComponent describe() {
        MutableComponent line = Component.empty();
        boolean first = true;
        for (Map.Entry<NoteType, Integer> note : notes.entrySet()) {
            if (!first) line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            line.append(note.getKey().displayName()).append(Component.literal(" " + note.getValue()).withStyle(ChatFormatting.WHITE));
            first = false;
        }
        return line;
    }
}
