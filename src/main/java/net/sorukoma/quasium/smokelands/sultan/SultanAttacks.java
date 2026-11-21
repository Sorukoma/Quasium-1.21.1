package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.magic.SmokeCasting;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;
import net.sorukoma.quasium.smokelands.mob.SmokeEvolution;

import java.util.List;
import java.util.Optional;

public final class SultanAttacks {
    public static final int CLOUD_COLOR = 0x7A9A5A;
    private static final float RING_SPREAD = 12f;
    private static final double PULL = 0.07;
    private static final int CLOUD_TICKS = 100;
    private static final double SUMMON_RADIUS = 6.0;
    private static final double BLAST_PUSH = 1.4;

    private SultanAttacks() {
    }

    static void perform(SultanOfSmoke sultan, SultanFight.Move move, ServerPlayer target, List<ServerPlayer> foes) {
        ServerLevel level = (ServerLevel) sultan.level();
        switch (move) {
            case RINGS -> rings(sultan, target);
            case INHALE_START -> sound(sultan, SmokelandsSounds.SULTAN_INHALE.get(), 1f);
            case INHALE_PULSE -> inhale(sultan, foes);
            case INHALE_END -> level.sendParticles(ParticleTypes.LARGE_SMOKE, sultan.getX(), sultan.getY(0.8), sultan.getZ(), 30, 0.6, 0.4, 0.6, 0.08);
            case CLOUD -> cloud(sultan, target.position(), foes);
            case SUMMON -> summon(sultan);
            case BURNOUT -> burnout(sultan, foes);
            case BLAST -> blast(sultan, foes);
            case STUN_END -> sound(sultan, SoundEvents.FIRECHARGE_USE, 0.6f);
        }
    }

    public static void rings(SultanOfSmoke sultan, ServerPlayer target) {
        Vec3 mouth = sultan.position().add(0, sultan.getBbHeight() * 0.7, 0);
        Vec3 aim = target.getEyePosition().subtract(mouth).normalize();
        int count = SmokelandsConfig.RINGS_PER_VOLLEY.get();
        double speed = SmokelandsConfig.RING_SPEED.get();
        float damage = SmokelandsConfig.RING_DAMAGE.get();
        for (int index = 0; index < count; index++) {
            float spread = (index - (count - 1) / 2f) * RING_SPREAD;
            SultanRing.launch(sultan, mouth, aim.yRot(spread * Mth.DEG_TO_RAD).scale(speed), damage);
        }
        sound(sultan, SmokelandsSounds.SULTAN_RING.get(), 1f);
    }

    public static void inhale(SultanOfSmoke sultan, List<ServerPlayer> players) {
        int lungs = SmokelandsConfig.INHALE_LUNGS_PER_SECOND.get();
        int kaif = SmokelandsConfig.INHALE_KAIF_PER_SECOND.get();
        ServerLevel level = (ServerLevel) sultan.level();
        for (ServerPlayer player : players) {
            SmokeCasting.drainLungs(player, lungs);
            PlayerKaifData.subtractKaif(player, kaif);
            Vec3 from = player.getEyePosition();
            Vec3 path = sultan.position().add(0, sultan.getBbHeight() * 0.6, 0).subtract(from);
            for (int step = 1; step <= 6; step++) {
                Vec3 at = from.add(path.scale(step / 7.0));
                level.sendParticles(ModParticles.noteSmoke(SultanRing.COLOR), at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }

    public static void pull(SultanOfSmoke sultan, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            Vec3 toward = sultan.position().subtract(player.position()).multiply(1, 0, 1);
            if (toward.lengthSqr() < 4) continue;
            player.setDeltaMovement(player.getDeltaMovement().add(toward.normalize().scale(PULL)));
            player.hurtMarked = true;
        }
    }

    public static AreaEffectCloud cloud(SultanOfSmoke sultan, Vec3 at, List<? extends LivingEntity> targets) {
        ServerLevel level = (ServerLevel) sultan.level();
        float radius = SmokelandsConfig.CLOUD_RADIUS.get();
        int poison = SmokelandsConfig.CLOUD_POISON_SECONDS.get() * 20;
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setOwner(sultan);
        cloud.setRadius(radius);
        cloud.setDuration(CLOUD_TICKS);
        cloud.setRadiusPerTick(0f);
        cloud.setParticle(ModParticles.noteSmoke(CLOUD_COLOR));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, poison));
        level.addFreshEntity(cloud);
        float damage = SmokelandsConfig.CLOUD_DAMAGE.get();
        for (LivingEntity target : targets) {
            if (target.position().distanceToSqr(at) > radius * radius) continue;
            target.hurt(level.damageSources().indirectMagic(cloud, sultan), damage);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, poison), sultan);
        }
        level.playSound(null, BlockPos.containing(at), SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE, 1f, 0.6f);
        return cloud;
    }

    public static void summon(SultanOfSmoke sultan) {
        ServerLevel level = (ServerLevel) sultan.level();
        Optional<EntityType<?>> dymnik = BuiltInRegistries.ENTITY_TYPE.getOptional(SmokeEvolution.DYMNIK);
        if (dymnik.isEmpty()) return;
        int count = SmokelandsConfig.SUMMON_COUNT.get();
        for (int index = 0; index < count; index++) {
            double angle = Mth.TWO_PI * index / count;
            int x = Mth.floor(sultan.home().getX() + Math.cos(angle) * SUMMON_RADIUS);
            int z = Mth.floor(sultan.home().getZ() + Math.sin(angle) * SUMMON_RADIUS);
            BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!(dymnik.get().create(level) instanceof Mob mob)) continue;
            mob.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(feet), MobSpawnType.MOB_SUMMONED, null);
            mob.setTarget(sultan.getTarget());
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY(0.5), mob.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private static void burnout(SultanOfSmoke sultan, List<ServerPlayer> foes) {
        sound(sultan, SmokelandsSounds.SULTAN_BURNOUT.get(), 1f);
        int windowSeconds = sultan.fight().rules().vulnerableTicks() / 20;
        for (ServerPlayer player : foes) {
            player.displayClientMessage(Component.translatable("quasium.sultan.burnout", windowSeconds), true);
        }
    }

    public static void blast(SultanOfSmoke sultan, List<? extends LivingEntity> targets) {
        ServerLevel level = (ServerLevel) sultan.level();
        double radius = SmokelandsConfig.BLAST_RADIUS.get();
        float damage = SmokelandsConfig.BLAST_DAMAGE.get();
        for (LivingEntity target : targets) {
            Vec3 away = target.position().subtract(sultan.position());
            if (away.lengthSqr() > radius * radius) continue;
            target.hurt(level.damageSources().explosion(sultan, sultan), damage);
            Vec3 push = away.multiply(1, 0, 1).normalize().scale(BLAST_PUSH);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.5, push.z));
            target.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, sultan.getX(), sultan.getY(0.6), sultan.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, sultan.getX(), sultan.getY(0.6), sultan.getZ(), 80, radius / 3, 1, radius / 3, 0.1);
        sound(sultan, SoundEvents.GENERIC_EXPLODE.value(), 1.4f);
    }

    static void stunned(SultanOfSmoke sultan, ServerPlayer player) {
        ServerLevel level = (ServerLevel) sultan.level();
        sound(sultan, SmokelandsSounds.SULTAN_STUN.get(), 1f);
        level.sendParticles(ParticleTypes.CLOUD, sultan.getX(), sultan.getY(0.95), sultan.getZ(), 40, 0.6, 0.2, 0.6, 0.05);
        SmokecraftTrigger.fire(player, "sultan_doused");
        for (ServerPlayer seen : sultan.arenaPlayers()) {
            seen.displayClientMessage(Component.translatable("quasium.sultan.stunned", SmokelandsConfig.STUN_SECONDS.get(),
                    SmokelandsConfig.STUN_DAMAGE_MULTIPLIER.get()), true);
        }
    }

    private static void sound(SultanOfSmoke sultan, SoundEvent sound, float volume) {
        sultan.level().playSound(null, sultan.blockPosition(), sound, SoundSource.HOSTILE, volume * 2f, 1f);
    }
}
