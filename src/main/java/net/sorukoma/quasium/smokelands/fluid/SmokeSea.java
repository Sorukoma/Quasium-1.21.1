package net.sorukoma.quasium.smokelands.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsKaif;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokeSea {
    private SmokeSea() {
    }

    public static boolean holds(Player player) {
        return SmokelandsKaif.of(player) >= ConfigArgs.synced(SmokelandsConfig.SEA_WALK_KAIF);
    }

    public static boolean isIn(Level level, BlockPos pos) {
        return level.getFluidState(pos).getFluidType() == SmokeFluids.TYPE.get();
    }

    public static boolean sinking(ServerPlayer player) {
        if (player.isSpectator() || holds(player)) return false;
        return isIn(player.level(), player.blockPosition()) || isIn(player.level(), BlockPos.containing(player.getEyePosition()));
    }

    public static boolean walking(ServerPlayer player) {
        return holds(player) && !player.isSpectator() && isIn(player.level(), player.blockPosition().below())
                && !isIn(player.level(), player.blockPosition());
    }

    public static void tick(ServerPlayer player) {
        if (sinking(player)) {
            PlayerKaifData.subtractKaif(player, SmokelandsConfig.SEA_SINK_KAIF_PER_SECOND.get());
        } else if (walking(player)) {
            SmokecraftTrigger.fire(player, "smoke_sea_walk");
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        for (ServerPlayer player : level.players()) tick(player);
    }
}
