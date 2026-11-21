package net.sorukoma.quasium.hookah;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.network.PacketStopHookahPuffSoundClient;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.sound.ModSoundEvents;
import net.sorukoma.quasium.util.HookahLogic;

public final class HookahSignals {
    private HookahSignals() {
    }

    public static void ready(ServerLevel level, Vec3 top) {
        level.playSound(null, top.x, top.y, top.z, ModSoundEvents.CALLOUD_HISS.get(), SoundSource.BLOCKS, 0.7f, 1.15f);
        level.sendParticles(ParticleTypes.SMOKE, top.x, top.y, top.z, 10, 0.12, 0.08, 0.12, 0.01);
        level.sendParticles(ParticleTypes.SMALL_FLAME, top.x, top.y, top.z, 3, 0.08, 0.02, 0.08, 0.0);
    }

    public static void unlit(ServerPlayer smoker, String reason) {
        smoker.displayClientMessage(Component.translatable("quasium.message." + reason), true);
    }

    public static void refuse(ServerPlayer smoker, String reason) {
        unlit(smoker, reason);
        PacketDistributor.sendToPlayer(smoker, new PacketStopHookahPuffSoundClient());
    }

    public static void peak(ServerPlayer smoker, Vec3 top) {
        peak(smoker.serverLevel(), top);
        smoker.displayClientMessage(Component.translatable("quasium.hookah.peak"), true);
    }

    public static void peak(ServerLevel level, Vec3 top) {
        level.playSound(null, top.x, top.y, top.z, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.x, top.y + 0.2, top.z, 6, 0.15, 0.1, 0.15, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, top.x, top.y, top.z, 5, 0.25, 0.15, 0.25, 0.0);
    }

    public static void tail(ServerPlayer smoker) {
        smoker.displayClientMessage(Component.translatable("quasium.hookah.tail", HookahLogic.TAIL_GRAMS), true);
    }

    public static void overheat(ServerPlayer smoker) {
        smoker.displayClientMessage(Component.translatable("quasium.hookah.overheat"), true);
    }

    public static void flavorChanged(ServerPlayer smoker, Vec3 top, String from, String to, int color) {
        smoker.serverLevel().sendParticles(ModParticles.noteSmoke(color), top.x, top.y + 0.2, top.z, 16, 0.2, 0.15, 0.2, 0.01);
        smoker.displayClientMessage(Component.translatable("quasium.hookah.flavor_changed",
                Component.translatable("quasium.flavor." + from), Component.translatable("quasium.flavor." + to)), true);
    }
}
