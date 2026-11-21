package net.sorukoma.quasium.selection.geode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.sorukoma.quasium.selection.SelectionRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BuddingQuasiumBlock extends QuasiumCrustBlock {
    private static final int GROWTH_CHANCE = 5;
    private static final Direction[] DIRECTIONS = Direction.values();

    public BuddingQuasiumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROWTH_CHANCE) != 0) return;
        Direction face = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        BlockPos target = pos.relative(face);
        BlockState current = level.getBlockState(target);
        Block next = nextStage(current, face);
        if (next == null) return;
        level.setBlockAndUpdate(target, next.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, face)
                .setValue(AmethystClusterBlock.WATERLOGGED, current.getFluidState().getType() == Fluids.WATER));
    }

    @Nullable
    private static Block nextStage(BlockState current, Direction face) {
        List<DeferredBlock<Block>> stages = SelectionRegistry.BUD_STAGES;
        if (BuddingAmethystBlock.canClusterGrowAtState(current)) return stages.getFirst().get();
        for (int stage = 0; stage < stages.size() - 1; stage++) {
            if (current.is(stages.get(stage)) && current.getValue(AmethystClusterBlock.FACING) == face) return stages.get(stage + 1).get();
        }
        return null;
    }
}
