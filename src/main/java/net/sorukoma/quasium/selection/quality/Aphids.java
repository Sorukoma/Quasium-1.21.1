package net.sorukoma.quasium.selection.quality;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.block.entity.TobaccoCropBlockEntity;
import net.sorukoma.quasium.draft.BottlePouring;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.sound.ModSoundEvents;

public final class Aphids {
    public static final String CURE_EVENT = "aphid_cure";

    private Aphids() {
    }

    public static BlockState dailyRoll(ServerLevel level, BlockPos pos, BlockState state, TobaccoCropBlockEntity crop, RandomSource random) {
        if (!crop.startAphidDay(level.getDayTime() / 24000L) || state.getValue(TobaccoCropBlock.APHIDS)) return state;
        double chance = QualityMath.aphidChance(SelectionConfig.APHID_DAILY_CHANCE_PCT.get(), flowersNear(level, pos),
                SelectionConfig.APHID_FLOWER_REDUCTION_PCT.get());
        if (random.nextDouble() >= chance) return state;
        BlockState infested = state.setValue(TobaccoCropBlock.APHIDS, true);
        level.setBlock(pos, infested, 2);
        return infested;
    }

    public static int growthPct(boolean infested, int basePct) {
        return QualityMath.growthPct(infested, basePct, SelectionConfig.APHID_GROWTH_PCT.get());
    }

    public static boolean fumigate(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!state.getValue(TobaccoCropBlock.APHIDS) || !NoteType.EMBER.id().equals(EssenceBottleItem.typeOf(held))) return false;
        if (level.isClientSide) return true;
        int cost = SelectionConfig.APHID_CURE_EMBER_ML.get();
        if (BottlePouring.pour(held, player, hand, ml -> ml >= cost ? cost : 0) <= 0) return false;
        level.setBlock(pos, state.setValue(TobaccoCropBlock.APHIDS, false), 2);
        level.playSound(null, pos, ModSoundEvents.APHID_SQUISH.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 12, 0.25, 0.2, 0.25, 0.01);
        }
        player.displayClientMessage(Component.translatable("quasium.message.tobacco.aphids_cured"), true);
        if (player instanceof ServerPlayer serverPlayer) SmokecraftTrigger.fire(serverPlayer, CURE_EVENT);
        return true;
    }

    private static boolean flowersNear(Level level, BlockPos pos) {
        int radius = SelectionConfig.APHID_FLOWER_RADIUS.get();
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            if (level.getBlockState(near).is(BlockTags.FLOWERS)) return true;
        }
        return false;
    }
}
