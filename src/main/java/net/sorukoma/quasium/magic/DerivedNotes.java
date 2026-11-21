package net.sorukoma.quasium.magic;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.QuasiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class DerivedNotes {
    private static final String PREFIX = "derived/";
    public static final StreamCodec<ByteBuf, Map<ResourceLocation, NoteProfile>> STREAM_CODEC = StreamCodec.of(DerivedNotes::write, DerivedNotes::read);

    private record Index(Map<ResourceLocation, NoteProfile> profiles, Map<Item, Map.Entry<ResourceLocation, NoteProfile>> items,
                         Map<EntityType<?>, Map.Entry<ResourceLocation, NoteProfile>> mobs) {
    }

    private static volatile Index index = new Index(Map.of(), Map.of(), Map.of());

    private DerivedNotes() {
    }

    public static void set(Map<ResourceLocation, NoteProfile> derived) {
        Map<Item, Map.Entry<ResourceLocation, NoteProfile>> items = new HashMap<>();
        Map<EntityType<?>, Map.Entry<ResourceLocation, NoteProfile>> mobs = new HashMap<>();
        for (Map.Entry<ResourceLocation, NoteProfile> entry : derived.entrySet()) {
            Map.Entry<ResourceLocation, NoteProfile> keyed = Map.entry(entry.getKey(), entry.getValue());
            entry.getValue().item().ifPresent(item -> items.put(item, keyed));
            entry.getValue().entity().ifPresent(mob -> mobs.put(mob, keyed));
        }
        index = new Index(Collections.unmodifiableMap(new TreeMap<>(derived)), Map.copyOf(items), Map.copyOf(mobs));
    }

    public static Map<ResourceLocation, NoteProfile> all() {
        return index.profiles;
    }

    @Nullable
    public static Map.Entry<ResourceLocation, NoteProfile> forItem(Item item) {
        return index.items.get(item);
    }

    @Nullable
    public static Map.Entry<ResourceLocation, NoteProfile> forEntity(EntityType<?> type) {
        return index.mobs.get(type);
    }

    public static ResourceLocation idOf(Item item) {
        return derivedId("item", BuiltInRegistries.ITEM.getKey(item));
    }

    public static ResourceLocation idOf(EntityType<?> type) {
        return derivedId("entity", BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static boolean isDerived(String profileId) {
        return profileId.startsWith(QuasiumMod.MOD_ID + ":" + PREFIX);
    }

    private static ResourceLocation derivedId(String kind, ResourceLocation target) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, PREFIX + kind + "/" + target.getNamespace() + "/" + target.getPath());
    }

    private static void write(ByteBuf buf, Map<ResourceLocation, NoteProfile> profiles) {
        VarInt.write(buf, profiles.size());
        for (NoteProfile profile : profiles.values()) {
            buf.writeBoolean(profile.isEntity());
            ResourceLocation target = profile.entity().<ResourceLocation>map(BuiltInRegistries.ENTITY_TYPE::getKey)
                    .orElseGet(() -> BuiltInRegistries.ITEM.getKey(profile.item().orElse(Items.AIR)));
            ResourceLocation.STREAM_CODEC.encode(buf, target);
            for (NoteType note : NoteType.values()) buf.writeByte(profile.notes().getOrDefault(note, 0));
        }
    }

    private static Map<ResourceLocation, NoteProfile> read(ByteBuf buf) {
        int size = VarInt.read(buf);
        Map<ResourceLocation, NoteProfile> profiles = new HashMap<>();
        for (int i = 0; i < size; i++) {
            boolean mob = buf.readBoolean();
            ResourceLocation target = ResourceLocation.STREAM_CODEC.decode(buf);
            EnumMap<NoteType, Integer> notes = new EnumMap<>(NoteType.class);
            for (NoteType note : NoteType.values()) {
                int ml = buf.readUnsignedByte();
                if (ml > 0) notes.put(note, ml);
            }
            if (notes.isEmpty()) continue;
            if (mob) {
                BuiltInRegistries.ENTITY_TYPE.getOptional(target).ifPresent(type ->
                        profiles.put(idOf(type), new NoteProfile(Optional.empty(), Optional.empty(), Optional.of(type), notes)));
            } else {
                BuiltInRegistries.ITEM.getOptional(target).ifPresent(item ->
                        profiles.put(idOf(item), new NoteProfile(Optional.of(item), Optional.empty(), Optional.empty(), notes)));
            }
        }
        return profiles;
    }
}
