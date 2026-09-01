package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.sorukoma.quasium.block.entity.DryingShelfBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.JerkyItem;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.recipe.DryingRecipes;
import org.jetbrains.annotations.Nullable;

public class DryingShelfBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Collision shapes per facing direction (matching the model elements)
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(2, 14, 12, 14, 16, 16),   // shelf board
            box(2, 10, 15, 4, 14, 16),     // left bracket
            box(12, 10, 15, 14, 14, 16)    // right bracket
    );
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            box(2, 14, 0, 14, 16, 4),
            box(12, 10, 0, 14, 14, 1),
            box(2, 10, 0, 4, 14, 1)
    );
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            box(12, 14, 2, 16, 16, 14),
            box(15, 10, 12, 16, 14, 14),
            box(15, 10, 2, 16, 14, 4)
    );
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            box(0, 14, 2, 4, 16, 14),
            box(0, 10, 12, 1, 14, 14),
            box(0, 10, 2, 1, 14, 4)
    );

    public DryingShelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShelfShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShelfShape(state);
    }

    private VoxelShape getShelfShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST  -> WEST_SHAPE;
            case EAST  -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis().isHorizontal() && canAttachTo(context.getLevel(), context.getClickedPos().relative(clickedFace.getOpposite()))) {
            return this.defaultBlockState().setValue(FACING, clickedFace);
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (canAttachTo(context.getLevel(), context.getClickedPos().relative(dir.getOpposite()))) {
                return this.defaultBlockState().setValue(FACING, dir);
            }
        }
        return null;
    }

    private boolean canAttachTo(Level level, BlockPos behindPos) {
        BlockState behindState = level.getBlockState(behindPos);
        return behindState.isSolidRender(level, behindPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof DryingShelfBlockEntity shelfEntity)) return ItemInteractionResult.FAIL;

        if (!heldItem.isEmpty() && (heldItem.getItem() instanceof JerkyItem
                || DryingRecipes.isValidInput(heldItem)
                || TobaccoVarieties.isLeaf(heldItem))) {
            ItemStack slotItem = shelfEntity.getItemHandler().getStackInSlot(0);
            if (slotItem.isEmpty()) {
                shelfEntity.getItemHandler().setStackInSlot(0, heldItem.copyWithCount(1));
                if (!player.isCreative()) heldItem.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof DryingShelfBlockEntity shelfEntity)) return InteractionResult.FAIL;

        ItemStack slotItem = shelfEntity.getItemHandler().getStackInSlot(0);
        if (!slotItem.isEmpty()) {
            if (player.isShiftKeyDown() && shelfEntity.getDryingTotalTime() > 0) {
                int progress;
                int secondsLeft;
                if (slotItem.getItem() instanceof JerkyItem) {
                    progress = slotItem.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 0);
                    int maxTicks = shelfEntity.getDryingTotalTime() * JerkyItem.MAX_DRYING_PERCENT / 100;
                    secondsLeft = Math.max(0, (maxTicks - shelfEntity.getDryingProgress() + 19) / 20);
                } else {
                    progress = Math.min(100, shelfEntity.getDryingProgress() * 100 / shelfEntity.getDryingTotalTime());
                    secondsLeft = Math.max(0, (shelfEntity.getDryingTotalTime() - shelfEntity.getDryingProgress() + 19) / 20);
                }
                player.displayClientMessage(Component.translatable(
                        "quasium.message.drying_shelf.progress", progress, secondsLeft), true);
                return InteractionResult.SUCCESS;
            }
            player.getInventory().placeItemBackInInventory(slotItem.copy());
            shelfEntity.getItemHandler().setStackInSlot(0, ItemStack.EMPTY);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof DryingShelfBlockEntity shelfEntity) {
                for (int i = 0; i < shelfEntity.getItemHandler().getSlots(); i++) {
                    ItemStack stack = shelfEntity.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        if (fromPos.equals(behind) && !level.getBlockState(behind).isSolidRender(level, behind)) {
            level.destroyBlock(pos, true);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingShelfBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.DRYING_SHELF_BE.get(), DryingShelfBlockEntity::tick);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(DryingShelfBlock::new);
    }
}