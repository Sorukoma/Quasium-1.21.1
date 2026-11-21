package net.sorukoma.quasium.smokelands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;

public final class SmokelandsSounds {
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENCE = register("smokelands_ambience");
    public static final DeferredHolder<SoundEvent, SoundEvent> CINDER_STORM = register("cinder_storm");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_TRAVEL = register("portal_travel");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_TAME = register("wisp_tame");
    public static final DeferredHolder<SoundEvent, SoundEvent> SULTAN_THEME = register("sultan_theme");
    public static final DeferredHolder<SoundEvent, SoundEvent> SULTAN_INHALE = register("sultan_inhale");
    public static final DeferredHolder<SoundEvent, SoundEvent> SULTAN_RING = register("sultan_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> SULTAN_HURT = register("sultan_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SULTAN_STUN = register("sultan_stun");
    public static final DeferredHolder<SoundEvent, SoundEvent> SULTAN_BURNOUT = register("sultan_burnout");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_LOFI = register("music_disc.ash_lofi");

    private SmokelandsSounds() {
    }

    public static void load() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SmokelandsRegistry.SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name)));
    }
}
