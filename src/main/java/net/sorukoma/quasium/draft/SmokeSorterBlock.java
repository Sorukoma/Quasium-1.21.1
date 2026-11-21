package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmokeSorterBlock extends BaseEntityBlock {
    public static final MapCodec<SmokeSorterBlock> CODEC = simpleCodec(SmokeSorterBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 7, 12), Block.box(7, 7, 7, 9, 12, 9), Block.box(5, 12, 5, 11, 15, 11));

    public SmokeSorterBlock(Properties properties) {
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
        return new SmokeSorterBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SmokeSorterBlockEntity sorter)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (BottlePouring.pour(stack, player, hand, sorter::fill) <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7f, 1.1f);
        report(player, sorter);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SmokeSorterBlockEntity sorter) report(player, sorter);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void report(Player player, SmokeSorterBlockEntity sorter) {
        player.displayClientMessage(Component.translatable("quasium.draft.sorter.tank", sorter.essenceMl(),
                SmokeSorterBlockEntity.tankCapacity()), true);
    }
}
