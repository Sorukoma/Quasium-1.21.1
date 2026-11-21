package net.sorukoma.quasium.mind;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.network.OreGlowPacket;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Insight {
    public static final String ENLIGHTENED_AGAIN_AT = "EnlightenedAgainAt";
    private static final Map<UUID, Integer> HIGH_SECONDS = new HashMap<>();
    private static final Map<UUID, Integer> PEAK_SECONDS = new HashMap<>();

    private Insight() {
    }

    public static int cap(Player player) {
        return Perk.ENLIGHTENED.ownedBy(player) ? ConfigArgs.synced(MindConfig.INSIGHT_CAP_ENLIGHTENED) : ConfigArgs.synced(Config.INSIGHT_CAP);
    }

    public static int dripKaif(Player player) {
        return Perk.ENLIGHTENED.ownedBy(player) ? MindConfig.INSIGHT_DRIP_KAIF_ENLIGHTENED.getAsInt() : MindConfig.INSIGHT_DRIP_KAIF.getAsInt();
    }

    static void tick(ServerPlayer player) {
        int kaif = PlayerKaifData.getKaifValue(player);
        if (count(HIGH_SECONDS, player, kaif >= dripKaif(player)) >= MindConfig.INSIGHT_DRIP_SECONDS.getAsInt()) {
            HIGH_SECONDS.remove(player.getUUID());
            if (PlayerMagicData.addInsight(player, 1) > 0) {
                player.displayClientMessage(Component.translatable("quasium.insight.drip", PlayerMagicData.getInsight(player))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        if (count(PEAK_SECONDS, player, kaif >= PlayerKaifData.MAX_KAIF) >= MindConfig.ENLIGHTENMENT_HOLD_SECONDS.getAsInt()) {
            PEAK_SECONDS.remove(player.getUUID());
            if (player.level().getGameTime() >= MindData.time(player, ENLIGHTENED_AGAIN_AT)) enlighten(player);
        }
    }

    private static int count(Map<UUID, Integer> seconds, ServerPlayer player, boolean holding) {
        if (!holding) {
            seconds.remove(player.getUUID());
            return 0;
        }
        return seconds.merge(player.getUUID(), 1, Integer::sum);
    }

    public static void enlighten(ServerPlayer player) {
        MindData.setTime(player, ENLIGHTENED_AGAIN_AT, player.level().getGameTime() + MindConfig.ENLIGHTENMENT_COOLDOWN_TICKS.getAsInt());
        List<BlockPos> ores = BlockFinder.near(player.serverLevel(), player.blockPosition(), MindConfig.ENLIGHTENMENT_RADIUS.getAsInt(),
                BlockFinder.matching(MindConfig.ENLIGHTENMENT_ORES.get()), OreGlowPacket.MAX_POSITIONS);
        PacketDistributor.sendToPlayer(player, new OreGlowPacket(ores, MindConfig.ENLIGHTENMENT_GLOW_SECONDS.getAsInt() * 20));
        PlayerMagicData.addInsight(player, MindConfig.ENLIGHTENMENT_INSIGHT.getAsInt());
        player.level().playSound(null, player.blockPosition(), MindRegistry.ENLIGHTENMENT.get(), SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.translatable("quasium.enlightenment.done", ores.size()).withStyle(ChatFormatting.GOLD), false);
        SmokecraftTrigger.fire(player, "enlightenment");
    }

    static void forget(UUID player) {
        HIGH_SECONDS.remove(player);
        PEAK_SECONDS.remove(player);
    }
}
