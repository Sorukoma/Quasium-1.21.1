package net.sorukoma.quasium.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.TobaccoCropBlock;

public final class SproutGrowth {
    private SproutGrowth() {
    }

    public static boolean grow(ServerLevel level, BlockPos pos, float cropChance, float saplingChance) {
        BlockState state = level.getBlockState(pos);
        float roll = level.random.nextFloat();
        boolean grew = false;
        if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
            if (roll < cropChance) {
                if (crop instanceof TobaccoCropBlock tobacco) tobacco.growStage(level, pos, state);
                else level.setBlockAndUpdate(pos, crop.getStateForAge(crop.getAge(state) + 1));
                grew = true;
            }
        } else if (state.getBlock() instanceof SaplingBlock sapling && roll < saplingChance) {
            sapling.advanceTree(level, pos, state, level.random);
            grew = true;
        }
        if (grew) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        }
        return grew;
    }
}
