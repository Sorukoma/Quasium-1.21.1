package net.sorukoma.quasium.tea;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.ecology.EcologyConfig;

public class TeaBushBlock extends CropBlock {
    public static final MapCodec<TeaBushBlock> CODEC = simpleCodec(TeaBushBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int MAX_AGE = 3;
    public static final int PICKED_AGE = 1;
    private static final VoxelShape[] SHAPES = {
            Block.box(4, 0, 4, 12, 5, 12),
            Block.box(3, 0, 3, 13, 8, 13),
            Block.box(2, 0, 2, 14, 11, 14),
            Block.box(1, 0, 1, 15, 13, 15)
    };

    public TeaBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TeaBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return TeaBlocks.TEA_SEEDS.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isMaxAge(state)) return InteractionResult.PASS;
        if (!level.isClientSide) pick(level, pos, state, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void pick(Level level, BlockPos pos, BlockState state, Player player) {
        popResource(level, pos, new ItemStack(TeaBlocks.TEA_LEAF.get(), EcologyConfig.TEA_LEAVES_PER_HARVEST.getAsInt()));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1f, 0.8f + level.random.nextFloat() * 0.4f);
        BlockState picked = state.setValue(AGE, PICKED_AGE);
        level.setBlock(pos, picked, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
    }
}
