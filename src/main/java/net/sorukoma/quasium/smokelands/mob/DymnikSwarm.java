package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.storm.CinderStorm;
import net.sorukoma.quasium.smokelands.sultan.SultanArena;

import java.util.Optional;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class DymnikSwarm {
    private static final int CHECK_TICKS = 100;
    private static final double REACH = 40.0;
    private static final int PER_CHECK = 2;
    private static final int MIN_DISTANCE = 14;
    private static final int MAX_DISTANCE = 28;

    private DymnikSwarm() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SmokelandsDimension.contains(level)
                || level.getGameTime() % CHECK_TICKS != 0) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        SultanArena arenas = SultanArena.of(level);
        BuiltInRegistries.ENTITY_TYPE.getOptional(SmokeEvolution.DYMNIK).ifPresent(dymnik -> {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && arenas.nearestThrone(player.blockPosition()).isEmpty()) fill(level, dymnik, player);
            }
        });
    }

    public static int wanted(ServerLevel level) {
        int base = SmokelandsConfig.DYMNIKS_PER_PLAYER.get();
        return CinderStorm.raging(level) ? base * SmokelandsConfig.STORM_DYMNIK_MULTIPLIER.get() : base;
    }

    private static void fill(ServerLevel level, EntityType<?> dymnik, ServerPlayer player) {
        int near = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(REACH), mob -> mob.getType() == dymnik).size();
        int spawned = 0;
        while (near + spawned < wanted(level) && spawned < PER_CHECK) {
            if (spawnNear(level, dymnik, player).isEmpty()) return;
            spawned++;
        }
    }

    private static Optional<Mob> spawnNear(ServerLevel level, EntityType<?> dymnik, ServerPlayer player) {
        RandomSource random = level.getRandom();
        float angle = random.nextFloat() * Mth.TWO_PI;
        int distance = Mth.nextInt(random, MIN_DISTANCE, MAX_DISTANCE);
        int x = player.getBlockX() + Math.round(Mth.cos(angle) * distance);
        int z = player.getBlockZ() + Math.round(Mth.sin(angle) * distance);
        if (!level.hasChunk(x >> 4, z >> 4)) return Optional.empty();
        BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (!level.getFluidState(feet.below()).isEmpty() || !(dymnik.create(level) instanceof Mob mob)) return Optional.empty();
        mob.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        if (!level.noCollision(mob)) return Optional.empty();
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(feet), MobSpawnType.EVENT, null);
        level.addFreshEntity(mob);
        return Optional.of(mob);
    }
}
