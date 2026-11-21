package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.sound.ModSoundEvents;

@OnlyIn(Dist.CLIENT)
public final class ClientZoneData {
    public static final int NIRVANA = 3;
    private static final float DRONE_VOLUME = 0.2f;
    private static final float DRONE_FADE = 0.01f;
    private static int tier;
    private static int color;

    private ClientZoneData() {
    }

    public static int tier() {
        return tier;
    }

    public static int color() {
        return color;
    }

    public static void update(int newTier, int newColor) {
        int oldTier = tier;
        tier = newTier;
        color = newColor;
        Minecraft minecraft = Minecraft.getInstance();
        if (oldTier == 0 && newTier > 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSoundEvents.ZONE_ENTER.get(), 1f, 0.6f));
        } else if (oldTier > 0 && newTier == 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSoundEvents.ZONE_EXIT.get(), 1f, 0.6f));
        }
        if (newTier > oldTier && minecraft.player != null) {
            minecraft.gui.setOverlayMessage(Component.translatable("quasium.zone.entered",
                    Component.translatable("quasium.zone.tier." + newTier)).withColor(newColor == 0 ? 0xE0E0E0 : newColor), false);
        }
        if (oldTier < NIRVANA && newTier >= NIRVANA) {
            minecraft.getSoundManager().play(new NirvanaDrone());
        }
    }

    public static void reset() {
        tier = 0;
        color = 0;
    }

    private static final class NirvanaDrone extends AbstractTickableSoundInstance {
        private NirvanaDrone() {
            super(ModSoundEvents.ZONE_DRONE.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            volume = target();
            relative = true;
        }

        @Override
        public void tick() {
            if (tier < NIRVANA) {
                stop();
                return;
            }
            volume = Mth.approach(volume, target(), DRONE_FADE);
        }

        private static float target() {
            return LofiDisc.audible() ? LofiDisc.SILENT : DRONE_VOLUME;
        }
    }
}
