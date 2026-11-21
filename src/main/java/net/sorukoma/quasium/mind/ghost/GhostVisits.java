package net.sorukoma.quasium.mind.ghost;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindRegistry;
import org.jetbrains.annotations.Nullable;

public final class GhostVisits {
    private static final String LAST_NIGHT = "GhostNight";
    private static final int DAY = 24000;
    private static final int TRIES = 12;

    private GhostVisits() {
    }

    public static long night(ServerLevel level) {
        return level.getDayTime() / DAY;
    }

    public static boolean due(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return level.isNight() && level.dimensionType().hasSkyLight()
                && PlayerKaifData.getKaifValue(player) <= MindConfig.GHOST_KAIF.getAsInt()
                && (!MindData.root(player).contains(LAST_NIGHT) || MindData.time(player, LAST_NIGHT) != night(level));
    }

    @Nullable
    public static HookahGhost visit(ServerPlayer player) {
        if (!due(player)) return null;
        BlockPos spot = spot(player);
        if (spot == null) return null;
        HookahGhost ghost = HookahGhost.visiting(player);
        ghost.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getRandom().nextFloat() * 360f, 0f);
        player.serverLevel().addFreshEntity(ghost);
        MindData.setTime(player, LAST_NIGHT, night(player.serverLevel()));
        player.level().playSound(null, spot, MindRegistry.GHOST_OFFER.get(), SoundSource.HOSTILE, 1f, 1f);
        player.displayClientMessage(Component.translatable("quasium.ghost.arrived").withStyle(ChatFormatting.DARK_AQUA), false);
        SmokecraftTrigger.fire(player, "ghost_visit");
        return ghost;
    }

    @Nullable
    private static BlockPos spot(ServerPlayer player) {
        RandomSource random = player.getRandom();
        int radius = MindConfig.GHOST_RADIUS.getAsInt();
        for (int attempt = 0; attempt < TRIES; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 2 + random.nextDouble() * Math.max(0, radius - 2);
            BlockPos spot = standable(player, BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(),
                    player.getZ() + Math.sin(angle) * distance));
            if (spot != null) return spot;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos spot = standable(player, player.blockPosition().relative(side, 2));
            if (spot != null) return spot;
        }
        return null;
    }

    @Nullable
    private static BlockPos standable(ServerPlayer player, BlockPos column) {
        ServerLevel level = player.serverLevel();
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos feet = column.above(dy);
            if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                    && level.isEmptyBlock(feet) && level.isEmptyBlock(feet.above())) return feet;
        }
        return null;
    }
}
