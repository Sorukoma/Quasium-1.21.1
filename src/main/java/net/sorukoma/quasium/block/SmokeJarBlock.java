package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.block.entity.SmokeJarBlockEntity;
import net.sorukoma.quasium.particle.ModParticles;

public class SmokeJarBlock extends BaseEntityBlock {
    public static final MapCodec<SmokeJarBlock> CODEC = simpleCodec(SmokeJarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 9, 11), Block.box(6, 9, 6, 10, 12, 10));

    public SmokeJarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokeJarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SMOKE_JAR_BE.get(), SmokeJarBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SmokeJarBlockEntity jar)) return;
        level.addParticle(ModParticles.noteSmoke(jar.color()), pos.getX() + 0.4 + random.nextDouble() * 0.2, pos.getY() + 0.78,
                pos.getZ() + 0.4 + random.nextDouble() * 0.2, 0.0, 0.03, 0.0);
    }
}
