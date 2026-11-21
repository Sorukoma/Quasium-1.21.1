package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.sorukoma.quasium.block.entity.FermentationBarrelBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.TobaccoVarieties;
import org.jetbrains.annotations.Nullable;

public class FermentationBarrelBlock extends BaseEntityBlock {

    public static final BooleanProperty FERMENTING = BooleanProperty.create("fermenting");

    public FermentationBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FERMENTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FERMENTING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof FermentationBarrelBlockEntity barrelEntity)) return ItemInteractionResult.FAIL;

        if (state.getValue(FERMENTING) && !heldItem.is(net.minecraft.world.item.Items.SUGAR)) {
            player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.fermenting"), true);
            return ItemInteractionResult.CONSUME;
        }

        boolean isLeaves = TobaccoVarieties.isLeaf(heldItem);
        boolean isFermentedLeaves = isLeaves && heldItem.has(ModDataComponents.FERMENTED.get());

        if (isFermentedLeaves) {
            player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.already_fermented"), true);
            return ItemInteractionResult.SUCCESS;
        }

        if (isLeaves) {
            int dryingStage = heldItem.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0);
            if (dryingStage < 2) {
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.needs_dried_leaves"), true);
                return ItemInteractionResult.CONSUME;
            }
            ItemStack slotItem = barrelEntity.getItemHandler().getStackInSlot(0);

            if (slotItem.isEmpty()) {
                int toInsert = Math.min(heldItem.getCount(), 16);
                barrelEntity.getItemHandler().setStackInSlot(0, heldItem.copyWithCount(toInsert));
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.added", toInsert, toInsert), true);
                if (!player.isCreative()) heldItem.shrink(toInsert);
                return ItemInteractionResult.CONSUME;
            } else if (FermentationBarrelBlockEntity.sameBatch(slotItem, heldItem)) {
                int canAdd = 16 - slotItem.getCount();
                if (canAdd <= 0) {
                    player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.full"), true);
                    return ItemInteractionResult.CONSUME;
                }
                int toInsert = Math.min(heldItem.getCount(), canAdd);
                ItemStack newSlot = slotItem.copy();
                newSlot.grow(toInsert);
                barrelEntity.getItemHandler().setStackInSlot(0, newSlot);
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.added", toInsert, slotItem.getCount() + toInsert), true);
                if (!player.isCreative()) heldItem.shrink(toInsert);
                return ItemInteractionResult.CONSUME;
            } else if (barrelEntity.isReady()) {
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.take_ready"), true);
                return ItemInteractionResult.CONSUME;
            } else if (heldItem.is(slotItem.getItem())) {
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.other_quality"), true);
                return ItemInteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.wrong_type"), true);
                return ItemInteractionResult.CONSUME;
            }
        }

        if (heldItem.is(net.minecraft.world.item.Items.SUGAR)) {
            if (state.getValue(FERMENTING)) {
                if (barrelEntity.addSugar()) {
                    if (!player.isCreative()) heldItem.shrink(1);
                    player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.sugar_accelerated"), true);
                } else {
                    player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.sugar_rejected"), true);
                }
                return ItemInteractionResult.CONSUME;
            }
            if (barrelEntity.addSugar()) {
                if (!player.isCreative()) heldItem.shrink(1);
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.added_sugar"), true);
            } else {
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.sugar_rejected"), true);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.getItem() == ModItems.NETHERITE_HAMMER.get()) {
            ItemStack slotItem = barrelEntity.getItemHandler().getStackInSlot(0);
            if (slotItem.isEmpty()) {
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.empty"), true);
                return ItemInteractionResult.CONSUME;
            }
            if (slotItem.has(ModDataComponents.FERMENTED.get())) {
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.already_fermented"), true);
                return ItemInteractionResult.CONSUME;
            }

            barrelEntity.startFermentation();
            level.setBlock(pos, state.setValue(FERMENTING, true), 3);
            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof FermentationBarrelBlockEntity barrelEntity)) return InteractionResult.FAIL;

        if (barrelEntity.isReady()) {
            boolean wasSpoiled = barrelEntity.isSpoiled();
            ItemStack result = barrelEntity.takeResult();
            player.getInventory().placeItemBackInInventory(result);
            if (state.getValue(FERMENTING)) {
                level.setBlock(pos, state.setValue(FERMENTING, false), 3);
            }
            player.displayClientMessage(Component.translatable(wasSpoiled
                    ? "block.quasium.fermentation_barrel.collected_spoiled"
                    : "block.quasium.fermentation_barrel.collected_ready"), true);
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(FERMENTING)) {
            int seconds = Math.max(0, (barrelEntity.getFermentationTotalTime() - barrelEntity.getFermentationProgress()) / 20);
            player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.progress", seconds), true);
            return InteractionResult.SUCCESS;
        }

        ItemStack slotItem = barrelEntity.getItemHandler().getStackInSlot(0);
        if (!slotItem.isEmpty()) {
            if (player.isShiftKeyDown()) {
                player.getInventory().placeItemBackInInventory(slotItem.copy());
                barrelEntity.getItemHandler().setStackInSlot(0, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
            if (!slotItem.has(ModDataComponents.FERMENTED.get())) {
                barrelEntity.startFermentation();
                level.setBlock(pos, state.setValue(FERMENTING, true), 3);
                player.displayClientMessage(Component.translatable("block.quasium.fermentation_barrel.started"), true);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof FermentationBarrelBlockEntity barrelEntity) {
                ItemStack stack = barrelEntity.getItemHandler().getStackInSlot(0);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
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
        return new FermentationBarrelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.FERMENTATION_BARREL_BE.get(), FermentationBarrelBlockEntity::tick);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(FermentationBarrelBlock::new);
    }
}
