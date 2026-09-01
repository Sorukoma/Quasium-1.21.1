package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.block.entity.QuasiumAnvilBlockEntity;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import org.jetbrains.annotations.Nullable;

public class QuasiumAnvilBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public QuasiumAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        return this.defaultBlockState().setValue(FACING, direction.getClockWise());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            BlockEntity entity = level.getBlockEntity(pos);
            // Drop items from block entity before falling
            if (entity instanceof QuasiumAnvilBlockEntity anvilEntity) {
                for (int i = 0; i < anvilEntity.getItemHandler().getSlots(); i++) {
                    ItemStack stack = anvilEntity.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(level, pos, state);
            this.falling(fallingblockentity);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, this.getDelayAfterPlace());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (pos.below().equals(neighborPos)) {
            level.scheduleTick(pos, this, this.getDelayAfterPlace());
        }
    }

    protected void falling(FallingBlockEntity entity) {
        // Can be overridden for custom fall behavior (e.g., damage on landing)
    }

    protected int getDelayAfterPlace() {
        return 2;
    }

    protected static boolean isFree(BlockState state) {
        return state.isAir() || state.liquid() || state.is(Blocks.FIRE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof QuasiumAnvilBlockEntity anvilEntity)) return ItemInteractionResult.FAIL;

        // Hit with netherite hammer
        if (heldItem.getItem() == ModItems.NETHERITE_HAMMER.get()) {
            AnvilRecipes.AnvilRecipe recipe = anvilEntity.getCurrentRecipe();
            if (recipe == null) {
                player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.no_recipe"), true);
                return ItemInteractionResult.CONSUME;
            }

            int playerKaif = PlayerKaifData.getKaifValue(player);
            if (playerKaif < recipe.kaifLevel()) {
                player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.low_kaif", recipe.kaifLevel()), true);
                return ItemInteractionResult.CONSUME;
            }

            int currentHits = anvilEntity.getHitCount();
            if (currentHits >= recipe.hitsRequired()) {
                return ItemInteractionResult.CONSUME;
            }

            // Damage hammer
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            heldItem.hurtAndBreak(1, player, slot);
            // Play vanilla anvil sound
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);

            anvilEntity.incrementHitCount();

            int newHits = anvilEntity.getHitCount();
            if (newHits >= recipe.hitsRequired()) {
                // Craft complete
                ItemStack result = new ItemStack(recipe.output().get());
                for (int i = 0; i < anvilEntity.getItemHandler().getSlots(); i++) {
                    anvilEntity.getItemHandler().setStackInSlot(i, ItemStack.EMPTY);
                }
                player.getInventory().placeItemBackInInventory(result);
                anvilEntity.resetHitCount();
                player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.craft_complete"), true);
            } else {
                player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.hit_progress", newHits, recipe.hitsRequired()), true);
            }

            return ItemInteractionResult.CONSUME;
        }

        // Insert items into slots
        if (!heldItem.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ItemStack slotItem = anvilEntity.getItemHandler().getStackInSlot(i);
                if (slotItem.isEmpty()) {
                    anvilEntity.getItemHandler().setStackInSlot(i, heldItem.copyWithCount(1));
                    if (!player.isCreative()) heldItem.shrink(1);
                    anvilEntity.resetHitCount();
                    return ItemInteractionResult.CONSUME;
                }
            }
            player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.full"), true);
            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof QuasiumAnvilBlockEntity anvilEntity)) return InteractionResult.FAIL;

        // Extract items from slots (last filled first)
        for (int i = 2; i >= 0; i--) {
            ItemStack slotItem = anvilEntity.getItemHandler().getStackInSlot(i);
            if (!slotItem.isEmpty()) {
                player.getInventory().placeItemBackInInventory(slotItem.copy());
                anvilEntity.getItemHandler().setStackInSlot(i, ItemStack.EMPTY);
                anvilEntity.resetHitCount();
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof QuasiumAnvilBlockEntity anvilEntity) {
                for (int i = 0; i < anvilEntity.getItemHandler().getSlots(); i++) {
                    ItemStack stack = anvilEntity.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuasiumAnvilBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null; // No ticking needed — anvil responds to player interaction only
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(QuasiumAnvilBlock::new);
    }
}
