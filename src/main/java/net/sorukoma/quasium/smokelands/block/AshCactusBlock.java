package net.sorukoma.quasium.smokelands.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AshCactusBlock extends CactusBlock {
    public AshCactusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(side)).isSolid() || level.getFluidState(pos.relative(side)).is(FluidTags.LAVA)) return false;
        }
        BlockState below = level.getBlockState(pos.below());
        return (below.is(this) || below.is(BlockTags.SAND)) && !level.getBlockState(pos.above()).liquid();
    }
}
