package net.sorukoma.quasium.smokelands.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.smokelands.fluid.SmokeFluids;

public class BuddingDreamQuartzBlock extends Block {
    public static final MapCodec<BuddingDreamQuartzBlock> CODEC = simpleCodec(BuddingDreamQuartzBlock::new);
    private static final Direction[] SIDES = Direction.values();

    public BuddingDreamQuartzBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) grow(level, pos, SIDES[random.nextInt(SIDES.length)]);
    }

    public static boolean grow(ServerLevel level, BlockPos budding, Direction side) {
        BlockPos at = budding.relative(side);
        BlockState there = level.getBlockState(at);
        Block next = null;
        if (there.isAir() || there.getFluidState().isSourceOfType(SmokeFluids.SOURCE.get())) {
            next = SmokelandsBlocks.DREAM_QUARTZ_BUD.get();
        } else if (there.is(SmokelandsBlocks.DREAM_QUARTZ_BUD.get()) && there.getValue(AmethystClusterBlock.FACING) == side) {
            next = SmokelandsBlocks.DREAM_QUARTZ_CLUSTER.get();
        }
        if (next == null) return false;
        level.setBlockAndUpdate(at, next.defaultBlockState().setValue(AmethystClusterBlock.FACING, side));
        return true;
    }
}
