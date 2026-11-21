package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.PageEffectMarker;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class HeartRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, QuasiumMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, QuasiumMod.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuasiumMod.MOD_ID);

    public static final ResourceKey<DamageType> STEAM_BURN = ResourceKey.create(Registries.DAMAGE_TYPE, id("steam_burn"));

    public static final DeferredHolder<MobEffect, MobEffect> STICKY = EFFECTS.register("sticky",
            () -> new PageEffectMarker(MobEffectCategory.HARMFUL, 0xE0A030)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, id("sticky_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> ROOTED = EFFECTS.register("rooted",
            () -> new HeartStatuses.Rooted("rooted", 0x6A5A3A, false));
    public static final DeferredHolder<MobEffect, MobEffect> AMBER = EFFECTS.register("amber",
            () -> new HeartStatuses.Rooted("amber", 0xFFB030, true));
    public static final DeferredHolder<MobEffect, MobEffect> CALMED = EFFECTS.register("calmed", HeartStatuses.Calm::new);
    public static final DeferredHolder<MobEffect, MobEffect> DISSOLVED = EFFECTS.register("dissolved",
            () -> new PageEffectMarker(MobEffectCategory.HARMFUL, 0x7FB8D8)
                    .addAttributeModifier(Attributes.ARMOR, id("dissolved_armor"), -1.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> SLIPPERY = EFFECTS.register("slippery", HeartStatuses.Slippery::new);
    public static final DeferredHolder<MobEffect, MobEffect> CARAMELIZED = EFFECTS.register("caramelized", HeartStatuses.Caramel::new);
    public static final DeferredHolder<MobEffect, MobEffect> SMOLDERING = EFFECTS.register("smoldering",
            () -> new PageEffectMarker(MobEffectCategory.HARMFUL, 0xA04020));
    public static final DeferredHolder<MobEffect, MobEffect> FOGGED = EFFECTS.register("fogged", HeartStatuses.Fog::new);

    public static final Supplier<AttachmentType<NoteMemory>> NOTE_MEMORY = ATTACHMENTS.register("note_memory",
            () -> AttachmentType.builder(() -> new NoteMemory()).build());

    private static final Map<NoteType, DeferredHolder<SoundEvent, SoundEvent>> TONES = registerTones();

    private HeartRegistry() {
    }

    public static SoundEvent tone(NoteType note) {
        return TONES.get(note).value();
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }

    private static Map<NoteType, DeferredHolder<SoundEvent, SoundEvent>> registerTones() {
        Map<NoteType, DeferredHolder<SoundEvent, SoundEvent>> tones = new EnumMap<>(NoteType.class);
        for (NoteType note : NoteType.values()) {
            String name = "accord_tone_" + note.id();
            tones.put(note, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name))));
        }
        return tones;
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
        SOUNDS.register(eventBus);
        ATTACHMENTS.register(eventBus);
    }
}
