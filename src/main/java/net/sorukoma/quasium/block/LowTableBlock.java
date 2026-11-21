package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.sorukoma.quasium.block.entity.LowTableBlockEntity;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.sound.ModSoundEvents;
import net.sorukoma.quasium.tea.TeaBlocks;
import org.jetbrains.annotations.Nullable;

public class LowTableBlock extends BaseEntityBlock {
    public static final MapCodec<LowTableBlock> CODEC = simpleCodec(LowTableBlock::new);
    public static final int TOP_PX = 9;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 7, 0, 16, TOP_PX, 16),
            Block.box(1, 0, 1, 3, 7, 3),
            Block.box(13, 0, 1, 15, 7, 3),
            Block.box(1, 0, 13, 3, 7, 15),
            Block.box(13, 0, 13, 15, 7, 15));

    public LowTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LowTableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.HOOKAH_PIPE.get()) || stack.is(TeaBlocks.SAMOVAR_ITEM.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (carriesSamovar(level, pos)) return level.getBlockState(pos.above()).useItemOn(stack, level, player, hand, hit.withPosition(pos.above()));
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof LowTableBlockEntity table)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (table.isFull()) return ItemInteractionResult.CONSUME;
        if (!level.isClientSide && table.place(stack.copyWithCount(1))) {
            stack.consume(1, player);
            level.playSound(null, pos, ModSoundEvents.TABLE_PLACE.get(), SoundSource.BLOCKS, 0.8f,
                    0.9f + level.random.nextFloat() * 0.2f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (carriesSamovar(level, pos)) return level.getBlockState(pos.above()).useWithoutItem(level, player, hit.withPosition(pos.above()));
        if (!(level.getBlockEntity(pos) instanceof LowTableBlockEntity table) || table.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemHandlerHelper.giveItemToPlayer(player, table.takeLast(), player.getInventory().selected);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean carriesSamovar(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(TeaBlocks.SAMOVAR.get());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LowTableBlockEntity table) {
            Containers.dropContents(level, pos, table.items());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
