package net.sorukoma.quasium.smokelands.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.client.geode.GeodeAmbience;

public class PetrifiedSmokeBlock extends Block {
    public PetrifiedSmokeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.dimension() == Level.OVERWORLD) GeodeAmbience.animate(level, pos, random);
    }
}
