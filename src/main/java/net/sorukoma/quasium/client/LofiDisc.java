package net.sorukoma.quasium.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mixin.ChannelAccessor;
import net.sorukoma.quasium.mixin.SoundEngineAccessor;
import net.sorukoma.quasium.mixin.SoundManagerAccessor;
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class LofiDisc {
    static final float SILENT = 1.0E-4f;
    private static final double HEARING_BLOCKS = 64.0;
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> STEMS =
            List.of(ModSoundEvents.LOFI_KEYS, ModSoundEvents.LOFI_BASS, ModSoundEvents.LOFI_DRUMS, ModSoundEvents.LOFI_LEAD);
    private static final LofiLayers LAYERS = new LofiLayers();
    private static final List<SoundInstance> DISCS = new ArrayList<>();

    private LofiDisc() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance disc = event.getSound();
        if (disc == null || !disc.getLocation().equals(ModSoundEvents.MUSIC_DISC_LOFI_KAIF_BEATS.getId())) return;
        DISCS.add(disc);
        Minecraft minecraft = Minecraft.getInstance();
        SoundManager sounds = minecraft.getSoundManager();
        StemSync sync = new StemSync(STEMS.size());
        List<Stem> stems = new ArrayList<>();
        for (int stem = LofiLayers.KEYS; stem <= LofiLayers.LEAD; stem++) stems.add(new Stem(STEMS.get(stem).get(), stem, disc, sync));
        CompletableFuture.allOf(stems.stream().map(stem -> decoded(stem, sounds)).toArray(CompletableFuture[]::new))
                .whenCompleteAsync((buffers, failure) -> {
                    if (DISCS.contains(disc)) stems.forEach(sounds::play);
                }, minecraft);
    }

    @SubscribeEvent
    public static void onStemStarted(PlaySoundSourceEvent event) {
        if (event.getSound() instanceof Stem stem) stem.sync.started(stem.stem, ((ChannelAccessor) event.getChannel()).quasium$source());
    }

    private static CompletableFuture<SoundBuffer> decoded(Stem stem, SoundManager sounds) {
        SoundEngine engine = ((SoundManagerAccessor) sounds).quasium$soundEngine();
        stem.resolve(sounds);
        return ((SoundEngineAccessor) engine).quasium$soundBuffers().getCompleteBuffer(stem.getSound().getPath());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) return;
        DISCS.removeIf(disc -> !minecraft.getSoundManager().isActive(disc));
        LAYERS.tick(ClientZoneData.tier());
        if (LAYERS.shouldScratch()) scratch(minecraft);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DISCS.clear();
        LAYERS.reset();
    }

    public static boolean audible() {
        return nearest(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) != null;
    }

    private static SoundInstance nearest(Vec3 listener) {
        SoundInstance nearest = null;
        double best = HEARING_BLOCKS * HEARING_BLOCKS;
        for (SoundInstance disc : DISCS) {
            double distance = listener.distanceToSqr(disc.getX(), disc.getY(), disc.getZ());
            if (distance <= best) {
                best = distance;
                nearest = disc;
            }
        }
        return nearest;
    }

    private static void scratch(Minecraft minecraft) {
        SoundInstance disc = nearest(minecraft.gameRenderer.getMainCamera().getPosition());
        if (disc == null) return;
        minecraft.getSoundManager().play(new SimpleSoundInstance(ModSoundEvents.RECORD_SCRATCH.get(), SoundSource.RECORDS, 1f, 1f,
                SoundInstance.createUnseededRandom(), disc.getX(), disc.getY(), disc.getZ()));
    }

    private static final class Stem extends AbstractTickableSoundInstance {
        private final int stem;
        private final SoundInstance disc;
        private final StemSync sync;

        private Stem(SoundEvent sound, int stem, SoundInstance disc, StemSync sync) {
            super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.stem = stem;
            this.disc = disc;
            this.sync = sync;
            x = disc.getX();
            y = disc.getY();
            z = disc.getZ();
            relative = disc.isRelative();
            volume = layerVolume();
        }

        @Override
        public void tick() {
            if (!Minecraft.getInstance().getSoundManager().isActive(disc)) {
                stop();
                return;
            }
            volume = layerVolume();
        }

        private float layerVolume() {
            return Math.max(SILENT, LAYERS.volume(stem));
        }
    }
}
