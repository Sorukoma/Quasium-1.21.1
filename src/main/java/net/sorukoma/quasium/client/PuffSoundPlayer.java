package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class PuffSoundPlayer {
    @Nullable
    private static SoundInstance current;

    private PuffSoundPlayer() {
    }

    public static void start(Player player) {
        stop();
        current = new EntityBoundSoundInstance(ModSoundEvents.HOOKAH_PUFF_FULL.get(), SoundSource.PLAYERS, 0.55f, 1f,
                player, player.getRandom().nextLong());
        Minecraft.getInstance().getSoundManager().play(current);
    }

    public static void stop() {
        if (current == null) return;
        Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }
}
