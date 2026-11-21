package net.sorukoma.quasium.tea;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.block.BlockShapes;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.block.LowTableBlock;
import net.sorukoma.quasium.block.entity.LowTableBlockEntity;
import net.sorukoma.quasium.util.WaterFilling;

import java.util.Map;

public class SamovarBlock extends BaseEntityBlock {
    public static final MapCodec<SamovarBlock> CODEC = simpleCodec(SamovarBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty ON_TABLE = BooleanProperty.create("on_table");
    public static final int LIT_LIGHT = 7;
    private static final double TABLE_DROP = (16 - LowTableBlock.TOP_PX) / 16.0;
    private static final VoxelShape BODY = Shapes.or(
            Block.box(5, 0, 5, 11, 2, 11),
            Block.box(3, 2, 4, 13, 11, 12),
            Block.box(4, 2, 3, 12, 11, 13),
            Block.box(4, 11, 4, 12, 13, 12),
            Block.box(6, 13, 6, 10, 16, 10),
            Block.box(7, 4, 1, 9, 6, 3),
            Block.box(1, 8, 7, 3, 10, 9),
            Block.box(13, 8, 7, 15, 10, 9));
    private static final Map<Direction, VoxelShape> SHAPES = BlockShapes.horizontal(Direction.NORTH, BODY);
    private static final Map<Direction, VoxelShape> TABLE_SHAPES = BlockShapes.horizontal(Direction.NORTH, BODY.move(0, -TABLE_DROP, 0));

    public SamovarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(ON_TABLE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, ON_TABLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ON_TABLE, isTable(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN ? state.setValue(ON_TABLE, isTable(neighborState)) : state;
    }

    private static boolean isTable(BlockState below) {
        return below.is(LoungeBlocks.LOW_TABLE.get());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (state.getValue(ON_TABLE) && level.getBlockEntity(pos.below()) instanceof LowTableBlockEntity table) table.spill();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(ON_TABLE) ? TABLE_SHAPES : SHAPES).get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SamovarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TeaBlocks.SAMOVAR_BE.get(), SamovarBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SamovarBlockEntity samovar)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(TeaBlocks.TEAPOT.get()) && TeapotItem.cups(stack) > 0) {
            if (player instanceof ServerPlayer server) TeaParty.tryStart(server, samovar, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        int waterMl = WaterFilling.mlIn(stack);
        if (waterMl == 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (samovar.addWater(waterMl)) WaterFilling.pourOut(stack, player, hand, level, pos);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SamovarBlockEntity samovar) player.openMenu(samovar, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SamovarBlockEntity samovar) samovar.drops();
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY() - (state.getValue(ON_TABLE) ? TABLE_DROP : 0);
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.WHITE_SMOKE, x + (random.nextDouble() - 0.5) * 0.15, y + 1.02, z + (random.nextDouble() - 0.5) * 0.15,
                0, 0.04, 0);
        if (random.nextInt(4) == 0) {
            Direction facing = state.getValue(FACING);
            level.addParticle(ParticleTypes.SMALL_FLAME, x + facing.getStepX() * 0.45, y + 0.2, z + facing.getStepZ() * 0.45, 0, 0, 0);
        }
    }
}
