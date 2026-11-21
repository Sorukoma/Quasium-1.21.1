package net.sorukoma.quasium.smokelands.mob;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WispLightBlock extends Block {
    public static final MapCodec<WispLightBlock> CODEC = simpleCodec(WispLightBlock::new);
    private static final int ORPHAN_CHECK_TICKS = 100;
    private static final double WISP_REACH = 1.5;

    public WispLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LightBlock.LEVEL, 12));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LightBlock.LEVEL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, ORPHAN_CHECK_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getEntitiesOfClass(DraftWisp.class, new AABB(pos).inflate(WISP_REACH), DraftWisp::isTamed).isEmpty()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } else {
            level.scheduleTick(pos, this, ORPHAN_CHECK_TICKS);
        }
    }
}
