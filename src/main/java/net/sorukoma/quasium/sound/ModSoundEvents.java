package net.sorukoma.quasium.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public final class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, QuasiumMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HOOKAH_PUFF_FULL = register("hookah_puff_full");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAST_INHALE = register("cast_inhale");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAST_EXHALE_BEAM = register("cast_exhale_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAST_EXHALE_CONE = register("cast_exhale_cone");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAST_EXHALE_CLOUD = register("cast_exhale_cloud");
    public static final DeferredHolder<SoundEvent, SoundEvent> CALLOUD_HISS = register("calloud_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCANNER_PING = register("scanner_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUBE_BUBBLE = register("cube_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> COUGH = register("cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRY_COUGH = register("dry_cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOTE_JAR_CLINK = register("note_jar_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_RELEASE = register("ring_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_PERFECT = register("ring_perfect");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_LOOP = register("boiler_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELLOWS = register("bellows");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE_FLOW = register("pipe_flow");
    public static final DeferredHolder<SoundEvent, SoundEvent> PNEUMATIC_HIT = register("pneumatic_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_SHOT = register("cannon_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> VALVE_CLICK = register("valve_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAN_HUM = register("fan_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINECART_HOOKAH_RATTLE = register("minecart_hookah_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_COAL_STEP = register("golem_coal_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_CLAY_HURT = register("golem_clay_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARDEN_SHOT = register("warden_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> KILN_LOOP = register("kiln_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_PLACE = register("seal_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_READY = register("golem_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> COAL_GOLEM_IGNITE = register("coal_golem_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZONE_ENTER = register("zone_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZONE_EXIT = register("zone_exit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZONE_DRONE = register("zone_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMP_LIT = register("lamp_lit");
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLE_PLACE = register("table_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_LOFI_KAIF_BEATS = register("music_disc.lofi_kaif_beats");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOFI_KEYS = register("music_disc.lofi_kaif_beats.keys");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOFI_BASS = register("music_disc.lofi_kaif_beats.bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOFI_DRUMS = register("music_disc.lofi_kaif_beats.drums");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOFI_LEAD = register("music_disc.lofi_kaif_beats.lead");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECORD_SCRATCH = register("record_scratch");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEODE_HUM = register("geode_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLUSTER_BREAK = register("cluster_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGER_HOOKAH_MASTER_WORK = register("villager_hookah_master_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_BELL = register("caravan_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> APHID_SQUISH = register("aphid_squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_LEAF_MUFFLE = register("echo_leaf_muffle");

    private ModSoundEvents() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
