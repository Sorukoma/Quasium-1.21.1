package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PneumaticHammerBlock extends BaseEntityBlock {
    public static final MapCodec<PneumaticHammerBlock> CODEC = simpleCodec(PneumaticHammerBlock::new);
    public static final BooleanProperty STRIKING = BooleanProperty.create("striking");
    private static final VoxelShape RAISED = shape(5);
    private static final VoxelShape STRUCK = shape(0);

    public PneumaticHammerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STRIKING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STRIKING);
    }

    private static VoxelShape shape(int headY) {
        return Shapes.or(Block.box(1, 9, 1, 15, 16, 15), Block.box(7, headY + 3, 7, 9, 9, 9), Block.box(4, headY, 4, 12, headY + 3, 12));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(STRIKING) ? STRUCK : RAISED;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumaticHammerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof PneumaticHammerBlockEntity hammer) {
            hammer.setOwner(player);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STRIKING)) level.setBlock(pos, state.setValue(STRIKING, false), Block.UPDATE_CLIENTS);
    }
}
