package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.block.BlockShapes;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SmokeCannonBlock extends BaseEntityBlock {
    public static final MapCodec<SmokeCannonBlock> CODEC = simpleCodec(SmokeCannonBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = BlockShapes.horizontal(Direction.NORTH, Shapes.or(
            Block.box(1, 0, 1, 15, 6, 15), Block.box(4, 6, 4, 12, 13, 12), Block.box(6, 8, 0, 10, 12, 4)));

    public SmokeCannonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokeCannonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ForgeBlocks.SMOKE_CANNON_BE.get(), SmokeCannonBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof SmokeCannonBlockEntity cannon) {
            cannon.setOwner(player.getUUID());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        String essence = EssenceBottleItem.typeOf(stack);
        if (essence.isBlank() || !(level.getBlockEntity(pos) instanceof SmokeCannonBlockEntity cannon)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (NoteType.byId(essence) != NoteType.ASH) {
            player.displayClientMessage(Component.translatable("quasium.draft.cannon.ash_only"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (BottlePouring.pour(stack, player, hand, cannon::fillAsh) > 0) {
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7f, 0.9f);
        }
        report(player, cannon);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SmokeCannonBlockEntity cannon) report(player, cannon);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void report(Player player, SmokeCannonBlockEntity cannon) {
        player.displayClientMessage(Component.translatable("quasium.draft.cannon.tank", cannon.ashMl(),
                SmokeCannonBlockEntity.tankCapacity(), cannon.chargePercent()), true);
    }
}
