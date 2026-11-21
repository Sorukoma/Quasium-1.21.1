package net.sorukoma.quasium.smokelands.storm;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;

public final class CinderStorm extends SavedData {
    private static final String NAME = "quasium_cinder_storm";
    private static final int HELD_TICKS = 2;
    private int calm;
    private int storm;

    private static final Factory<CinderStorm> FACTORY = new Factory<>(CinderStorm::new, CinderStorm::load);

    public static CinderStorm of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static boolean raging(ServerLevel level) {
        return SmokelandsDimension.contains(level) && of(level).storm > 0;
    }

    private static CinderStorm load(CompoundTag tag, HolderLookup.Provider registries) {
        CinderStorm state = new CinderStorm();
        state.calm = tag.getInt("calm");
        state.storm = tag.getInt("storm");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("calm", calm);
        tag.putInt("storm", storm);
        return tag;
    }

    public boolean active() {
        return storm > 0;
    }

    public int stormTicks() {
        return storm;
    }

    public int calmTicks() {
        return calm;
    }

    public void tick(ServerLevel level) {
        if (storm > 0) {
            if (--storm == 0) end(level);
        } else {
            if (calm <= 0) calm = rollCalm(level.getRandom());
            if (--calm == 0) start(level);
        }
    }

    public void keepRaging(ServerLevel level) {
        if (storm == 0) start(level);
        storm = Math.max(storm, HELD_TICKS);
    }

    public void start(ServerLevel level) {
        storm = SmokelandsConfig.STORM_SECONDS.get() * 20;
        calm = 0;
        setDirty();
        sync(level);
        level.players().forEach(player -> player.displayClientMessage(Component.translatable("quasium.smokelands.storm_rises"), true));
    }

    private void end(ServerLevel level) {
        calm = rollCalm(level.getRandom());
        setDirty();
        sync(level);
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) SmokecraftTrigger.fire(player, "storm_survived");
        }
    }

    private static int rollCalm(RandomSource random) {
        int min = SmokelandsConfig.STORM_CALM_MIN_SECONDS.get() * 20;
        int max = Math.max(min, SmokelandsConfig.STORM_CALM_MAX_SECONDS.get() * 20);
        return min + random.nextInt(max - min + 1);
    }

    private void sync(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, new StormStatePacket(active()));
    }
}
