package net.sorukoma.quasium.smokelands.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.particle.ModParticles;

public class AncientFlaskBlock extends Block {
    public static final MapCodec<AncientFlaskBlock> CODEC = simpleCodec(AncientFlaskBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(4, 0, 4, 12, 8, 12), box(6, 8, 6, 10, 14, 10));
    private static final int SMOKE = 0x9A86C8;

    public AncientFlaskBlock(Properties properties) {
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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ModParticles.noteSmoke(SMOKE), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                (random.nextDouble() - 0.5) * 0.01, 0.015, (random.nextDouble() - 0.5) * 0.01);
    }
}
