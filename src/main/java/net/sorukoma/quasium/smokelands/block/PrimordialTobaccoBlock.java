package net.sorukoma.quasium.smokelands.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;

public class PrimordialTobaccoBlock extends BushBlock {
    public static final MapCodec<PrimordialTobaccoBlock> CODEC = simpleCodec(PrimordialTobaccoBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int RIPE = 3;
    public static final ResourceLocation LEAVES = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "primordial_leaves");
    public static final ResourceLocation SEEDS = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "primordial_seeds");
    private static final VoxelShape[] SHAPES = {box(4, 0, 4, 12, 5, 12), box(3, 0, 3, 13, 9, 13), box(2, 0, 2, 14, 12, 14),
            box(1, 0, 1, 15, 15, 15)};

    public PrimordialTobaccoBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(SmokelandsBlocks.NICOTINE_MOSS.get()) || state.is(SmokelandsBlocks.SOOT_MUD.get()) || state.is(BlockTags.DIRT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < RIPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < RIPE || BuiltInRegistries.ITEM.getOptional(LEAVES).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) harvest(state, level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void harvest(BlockState state, Level level, BlockPos pos, Player player) {
        BuiltInRegistries.ITEM.getOptional(LEAVES).ifPresent(leaves ->
                popResource(level, pos, new ItemStack(leaves, SmokelandsConfig.PRIMORDIAL_LEAVES_PER_HARVEST.get())));
        level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1f, 0.7f);
        if (player instanceof ServerPlayer serverPlayer) SmokecraftTrigger.fire(serverPlayer, "primordial_harvest");
    }
}
