package net.sorukoma.quasium.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> NOTE_SMOKE =
            PARTICLE_TYPES.register("note_smoke", () -> new ParticleType<ColorParticleOption>(false) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> APHID =
            PARTICLE_TYPES.register("aphid", () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static ColorParticleOption noteSmoke(int rgb) {
        return ColorParticleOption.create(NOTE_SMOKE.get(), 0xFF000000 | rgb);
    }

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
