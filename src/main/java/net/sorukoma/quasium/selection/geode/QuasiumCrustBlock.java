package net.sorukoma.quasium.selection.geode;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.client.geode.GeodeAmbience;

public class QuasiumCrustBlock extends Block {
    public QuasiumCrustBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        GeodeAmbience.animate(level, pos, random);
    }
}
