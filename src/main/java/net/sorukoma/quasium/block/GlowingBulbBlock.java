package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GlowingBulbBlock extends Block {
    public static final MapCodec<GlowingBulbBlock> CODEC = simpleCodec(GlowingBulbBlock::new);
    public static final int LIGHT = 12;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 1, 11), Block.box(4, 1, 4, 12, 8, 12),
            Block.box(5, 8, 5, 11, 9, 11), Block.box(6, 9, 6, 10, 12, 10), Block.box(5, 12, 5, 11, 13, 11));

    public GlowingBulbBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
