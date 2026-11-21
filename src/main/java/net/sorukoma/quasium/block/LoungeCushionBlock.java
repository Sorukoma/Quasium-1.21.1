package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.entity.SeatEntity;

public class LoungeCushionBlock extends Block {
    public static final MapCodec<LoungeCushionBlock> CODEC = simpleCodec(LoungeCushionBlock::new);
    private static final int HEIGHT_PX = 6;
    public static final double SEAT_HEIGHT = HEIGHT_PX / 16.0;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, HEIGHT_PX, 15);

    public LoungeCushionBlock(Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || SeatEntity.isOccupied(level, pos)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return SeatEntity.sit(player, pos) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
}
