package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.block.entity.QuasiumAnvilBlockEntity;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuasiumAnvilBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = BlockShapes.horizontal(Direction.SOUTH, Shapes.or(
            Block.box(2, 0, 2, 14, 4, 14), Block.box(4, 4, 3, 12, 5, 13), Block.box(6, 5, 4, 10, 10, 12), Block.box(3, 10, 0, 13, 16, 16)));

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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
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

        if (heldItem.getItem() == ModItems.NETHERITE_HAMMER.get()) {
            AnvilRecipes.AnvilRecipe recipe = anvilEntity.getCurrentRecipe();
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            QuasiumAnvilBlockEntity.Strike strike = anvilEntity.strike(player, () -> heldItem.hurtAndBreak(1, player, slot),
                    result -> player.getInventory().placeItemBackInInventory(result));
            switch (strike) {
                case NO_RECIPE -> player.displayClientMessage(noRecipe(anvilEntity.getItemHandler()), true);
                case UNSTUDIED -> player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.unstudied",
                        Research.title(recipe.research().toString())), true);
                case LOW_KAIF -> player.displayClientMessage(recipe.refusal(), true);
                case HIT -> player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.hit_progress",
                        anvilEntity.getHitCount(), recipe.hitsRequired()), true);
                case CRAFTED -> player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.craft_complete"), true);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (insert(anvilEntity.getItemHandler(), heldItem, player)) anvilEntity.resetHitCount();
        return ItemInteractionResult.CONSUME;
    }

    private static Component noRecipe(ItemStackHandler slots) {
        List<Component> stacks = new ArrayList<>();
        for (int slot = 0; slot < slots.getSlots(); slot++) {
            ItemStack stack = slots.getStackInSlot(slot);
            if (!stack.isEmpty()) stacks.add(counted(stack, stack.getCount()));
        }
        if (stacks.isEmpty()) return Component.translatable("block.quasium.quasium_anvil.no_recipe");
        return Component.translatable("block.quasium.quasium_anvil.no_recipe_for", ComponentUtils.formatList(stacks, Component.literal(", ")));
    }

    private static Component counted(ItemStack stack, int count) {
        return Component.translatable("block.quasium.quasium_anvil.stacked", stack.getHoverName(), count);
    }

    private static boolean insert(ItemStackHandler slots, ItemStack held, Player player) {
        int top = slots.getSlots() - 1;
        while (top >= 0 && slots.getStackInSlot(top).isEmpty()) top--;
        ItemStack last = top < 0 ? ItemStack.EMPTY : slots.getStackInSlot(top);
        if (!last.isEmpty() && ItemStack.isSameItemSameComponents(last, held) && last.getCount() < last.getMaxStackSize()) {
            slots.setStackInSlot(top, last.copyWithCount(last.getCount() + 1));
            player.displayClientMessage(counted(last, last.getCount() + 1), true);
        } else if (top < slots.getSlots() - 1) {
            slots.setStackInSlot(top + 1, held.copyWithCount(1));
        } else {
            player.displayClientMessage(Component.translatable("block.quasium.quasium_anvil.full"), true);
            return false;
        }
        if (!player.isCreative()) held.shrink(1);
        return true;
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
