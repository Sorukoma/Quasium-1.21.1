package net.sorukoma.quasium.ritual.brazier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.ritual.RitualRecipes;
import net.sorukoma.quasium.ritual.RitualRegistry;
import org.jetbrains.annotations.Nullable;

public class RitualBrazierBlock extends BaseEntityBlock {
    public static final MapCodec<RitualBrazierBlock> CODEC = simpleCodec(RitualBrazierBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(box(4, 0, 4, 12, 2, 12), box(6, 2, 6, 10, 6, 10),
            box(2, 6, 2, 14, 8, 14), box(1, 8, 1, 15, 11, 15));

    public RitualBrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
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
        return new RitualBrazierBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return createTickerHelper(type, RitualRegistry.RITUAL_BRAZIER_BE.get(), RitualBrazierBlockEntity::clientTick);
        return createTickerHelper(type, RitualRegistry.RITUAL_BRAZIER_BE.get(), RitualBrazierBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity brazier)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModItems.HOOKAH_PIPE.get())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!handles(state, brazier, stack)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) return ItemInteractionResult.SUCCESS;
        if (stack.is(ModItems.KAIFOMETER.get())) {
            RitualReport.of(serverLevel, brazier, server).forEach(server::sendSystemMessage);
        } else if (brazier.run() != null) {
            server.displayClientMessage(Component.translatable("quasium.ritual.busy"), true);
        } else if (ignites(stack)) {
            if (RitualStart.ignite(serverLevel, brazier, server)) spendIgniter(stack, server, hand);
        } else if (stack.is(ModItems.FOCUS_UPGRADE_FILTER.get())) {
            brazier.setFilter(stack.consumeAndReturn(1, player));
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
        } else if (RitualRecipes.anyTakes(stack)) {
            brazier.setCatalyst(stack.consumeAndReturn(1, player));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.9f, 0.7f);
        } else {
            server.displayClientMessage(Component.translatable("quasium.ritual.not_catalyst", stack.getHoverName()), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    private static boolean handles(BlockState state, RitualBrazierBlockEntity brazier, ItemStack stack) {
        if (stack.is(ModItems.KAIFOMETER.get()) || state.getValue(LIT) || ignites(stack)) return true;
        if (stack.is(ModItems.FOCUS_UPGRADE_FILTER.get()) && !brazier.hasFilter()) return true;
        return brazier.catalyst().isEmpty();
    }

    private static boolean ignites(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    private static void spendIgniter(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        if (stack.is(Items.FIRE_CHARGE)) stack.consume(1, player);
        else stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity brazier)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;
        RitualReport.of(serverLevel, brazier, server).forEach(server::sendSystemMessage);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity brazier)) return;
        if (brazier.catalyst().isEmpty() && !brazier.hasFilter()) return;
        if (brazier.run() != null) {
            player.displayClientMessage(Component.translatable("quasium.ritual.busy"), true);
            return;
        }
        ItemStack taken;
        if (!brazier.catalyst().isEmpty()) {
            taken = brazier.catalyst();
            brazier.setCatalyst(ItemStack.EMPTY);
        } else {
            taken = brazier.filter();
            brazier.setFilter(ItemStack.EMPTY);
        }
        if (!player.addItem(taken)) player.drop(taken, false);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.9f, 0.7f);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity brazier) {
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), brazier.catalyst());
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), brazier.filter());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.7;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.7;
        level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.75, z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 0.9, z, 0, 0.04, 0);
        if (random.nextInt(4) == 0) level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 0.8f, false);
    }
}
