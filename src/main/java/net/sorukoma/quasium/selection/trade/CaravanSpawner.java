package net.sorukoma.quasium.selection.trade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.SelectionRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class CaravanSpawner {
    public static final int LLAMAS = 2;
    private static final int TICKS_PER_MINUTE = 1200;
    private static final int PLAYER_RADIUS = 48;
    private static final int LLAMA_RADIUS = 4;

    private CaravanSpawner() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (!CaravanDays.of(level).startsNewDay(level.getDayTime() / 24000L)) return;
        GameRules rules = level.getGameRules();
        if (!rules.getBoolean(GameRules.RULE_DO_TRADER_SPAWNING) || !rules.getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        if (onTheRoad(level)) return;
        if (level.random.nextInt(100) >= SelectionConfig.CARAVAN_DAILY_CHANCE_PCT.get()) return;
        List<ServerPlayer> players = level.players().stream().filter(player -> !player.isSpectator()).toList();
        if (players.isEmpty()) return;
        arrive(level, players.get(level.random.nextInt(players.size())).blockPosition(), PLAYER_RADIUS);
    }

    @Nullable
    public static SmokeCaravanTrader arrive(ServerLevel level, BlockPos around, int radius) {
        BlockPos spot = groundNear(level, around, radius);
        if (spot == null) return null;
        SmokeCaravanTrader caravan = SelectionRegistry.SMOKE_CARAVAN.get().spawn(level, spot, MobSpawnType.EVENT);
        if (caravan == null) return null;
        caravan.stayFor(SelectionConfig.CARAVAN_STAY_MINUTES.get() * TICKS_PER_MINUTE);
        caravan.setWanderTarget(around);
        caravan.restrictTo(around, 16);
        CaravanDays.of(level).onTheRoadUntil(caravan.departsAt());
        for (int llama = 0; llama < LLAMAS; llama++) {
            BlockPos llamaSpot = groundNear(level, caravan.blockPosition(), LLAMA_RADIUS);
            TraderLlama trader = llamaSpot == null ? null : EntityType.TRADER_LLAMA.spawn(level, llamaSpot, MobSpawnType.EVENT);
            if (trader == null) continue;
            trader.setLeashedTo(caravan, true);
            CaravanLlamas.join(trader, caravan.departsAt());
        }
        return caravan;
    }

    public static boolean onTheRoad(ServerLevel level) {
        return level.getGameTime() < CaravanDays.of(level).onTheRoadUntil;
    }

    @Nullable
    private static BlockPos groundNear(ServerLevel level, BlockPos around, int radius) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = around.getX() + level.random.nextInt(radius * 2) - radius;
            int z = around.getZ() + level.random.nextInt(radius * 2) - radius;
            BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
            if (SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, spot, EntityType.WANDERING_TRADER)) return spot;
        }
        return null;
    }

    private static final class CaravanDays extends SavedData {
        private long lastDay = -1;
        private long onTheRoadUntil;

        static CaravanDays of(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new Factory<>(CaravanDays::new, CaravanDays::load), "quasium_caravan");
        }

        private static CaravanDays load(CompoundTag tag, HolderLookup.Provider registries) {
            CaravanDays days = new CaravanDays();
            days.lastDay = tag.getLong("last_day");
            days.onTheRoadUntil = tag.getLong("on_the_road_until");
            return days;
        }

        void onTheRoadUntil(long gameTime) {
            onTheRoadUntil = gameTime;
            setDirty();
        }

        boolean startsNewDay(long day) {
            if (day == lastDay) return false;
            boolean first = lastDay < 0;
            lastDay = day;
            setDirty();
            return !first;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLong("last_day", lastDay);
            tag.putLong("on_the_road_until", onTheRoadUntil);
            return tag;
        }
    }
}
