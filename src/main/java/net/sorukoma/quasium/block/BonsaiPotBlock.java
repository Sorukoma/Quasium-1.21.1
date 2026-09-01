package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sorukoma.quasium.block.entity.BonsaiPotBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.item.BonsaiSaplingItem;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;


public class BonsaiPotBlock extends BaseEntityBlock {
    public BonsaiPotBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
        if (!(level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity pot)) return ItemInteractionResult.FAIL;

        if (pot.getTreeType().isEmpty() && heldItem.getItem() instanceof BonsaiSaplingItem sapling) {
            pot.plant(sapling.treeType());
            if (!player.isCreative()) heldItem.shrink(1);
            return ItemInteractionResult.CONSUME;
        }
        if (!pot.getTreeType().isEmpty() && player.isShiftKeyDown()) {
            ItemStack sapling = new ItemStack(BonsaiPotBlock.getSapling(pot.getTreeType()));
            player.getInventory().placeItemBackInInventory(sapling);
            pot.clearTree();
            return ItemInteractionResult.CONSUME;
        }
        if (!pot.getTreeType().isEmpty() && pot.getGrowth() >= 100 && !pot.getOutput().getStackInSlot(0).isEmpty()) {
            ItemStack harvested = pot.getOutput().extractItem(0, 1, false);
            player.getInventory().placeItemBackInInventory(harvested);
            pot.harvest();
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity pot && !pot.getTreeType().isEmpty()) {
            if (player.isShiftKeyDown()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(BonsaiPotBlock.getSapling(pot.getTreeType())));
                pot.clearTree();
            } else {
                player.displayClientMessage(Component.translatable("quasium.message.bonsai.progress", pot.getTreeType(), pot.getGrowth()), true);
            }
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    public static net.minecraft.world.item.Item getFruit(String type) {
        return switch (type) {
            case "lemon" -> ModItems.LEMON.get();
            case "lime" -> ModItems.LIME.get();
            case "orange" -> ModItems.ORANGE.get();
            case "grapefruit" -> ModItems.GRAPEFRUIT.get();
            case "grape" -> ModItems.GRAPE.get();
            case "coconut" -> ModItems.COCONUT.get();
            case "cocoa" -> ModItems.COCOA_NIBS.get();
            case "lavender" -> ModItems.LAVENDER.get();
            default -> ModItems.LEMON.get();
        };
    }

    public static net.minecraft.world.item.Item getSapling(String type) {
        return switch (type) {
            case "lemon" -> ModItems.LEMON_SAPLING.get();
            case "lime" -> ModItems.LIME_SAPLING.get();
            case "orange" -> ModItems.ORANGE_SAPLING.get();
            case "grapefruit" -> ModItems.GRAPEFRUIT_SAPLING.get();
            case "grape" -> ModItems.GRAPE_SAPLING.get();
            case "coconut" -> ModItems.COCONUT_SAPLING.get();
            case "cocoa" -> ModItems.COCOA_CROP_SEEDS.get();
            case "lavender" -> ModItems.LAVENDER_SEEDS.get();
            default -> ModItems.LEMON_SAPLING.get();
        };
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BonsaiPotBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BONSAI_POT_BE.get(), BonsaiPotBlockEntity::tick);
    }
    @Override public MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(BonsaiPotBlock::new); }
}
