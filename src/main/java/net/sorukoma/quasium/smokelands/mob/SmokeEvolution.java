package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ecology.SmokeDensity;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;

import java.util.Optional;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokeEvolution {
    public static final ResourceLocation DYMNIK = SmokelandsDimension.id("dymnik");
    public static final ResourceLocation GAREVSHCHIK = SmokelandsDimension.id("garevshchik");
    public static final String AGE = "QuasiumSmokeAge";

    private SmokeEvolution() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.tickCount % 20 != 0 || !(mob.level() instanceof ServerLevel level)) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (DYMNIK.equals(id) && thickSmoke(level, mob.blockPosition()) && aged(mob, SmokelandsConfig.DYMNIK_EVOLVE_SECONDS.get())) {
            evolve(level, mob, GAREVSHCHIK);
        } else if (GAREVSHCHIK.equals(id) && overTheSea(level, mob.blockPosition())
                && aged(mob, SmokelandsConfig.GAREVSHCHIK_EVOLVE_SECONDS.get())) {
            evolve(level, mob, SmokelandsEntities.MIST_PHANTOM.getId());
        }
    }

    public static boolean thickSmoke(ServerLevel level, BlockPos pos) {
        return SmokelandsDimension.contains(level) || SmokeDensity.get(level, pos) >= SmokelandsConfig.EVOLVE_DENSITY.get();
    }

    public static boolean overTheSea(ServerLevel level, BlockPos pos) {
        return SmokelandsDimension.contains(level) && level.getBiome(pos).is(SmokelandsDimension.SMOKE_SEA);
    }

    public static boolean aged(Mob mob, int seconds) {
        int age = mob.getPersistentData().getInt(AGE) + 1;
        mob.getPersistentData().putInt(AGE, age);
        return age >= seconds;
    }

    public static Optional<Mob> evolve(ServerLevel level, Mob mob, ResourceLocation into) {
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(into);
        if (type.isEmpty() || !(type.get().create(level) instanceof Mob next)) return Optional.empty();
        next.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(next, level, level.getCurrentDifficultyAt(next.blockPosition()), MobSpawnType.CONVERSION, null);
        if (mob.hasCustomName()) {
            next.setCustomName(mob.getCustomName());
            next.setCustomNameVisible(mob.isCustomNameVisible());
        }
        if (mob.isPersistenceRequired()) next.setPersistenceRequired();
        next.setTarget(mob.getTarget());
        level.addFreshEntity(next);
        EventHooks.onLivingConvert(mob, next);
        mob.discard();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, next.getX(), next.getY(0.5), next.getZ(), 24, 0.4, 0.5, 0.4, 0.02);
        level.playSound(null, next.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8f, 0.5f);
        return Optional.of(next);
    }
}
