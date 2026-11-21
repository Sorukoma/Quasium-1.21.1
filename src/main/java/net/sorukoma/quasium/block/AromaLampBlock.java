package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
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
import net.sorukoma.quasium.block.entity.AromaLampBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

public class AromaLampBlock extends BaseEntityBlock {
    public static final MapCodec<AromaLampBlock> CODEC = simpleCodec(AromaLampBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int LIT_LIGHT = 10;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 6, 11), Block.box(4, 6, 4, 12, 8, 12));

    public AromaLampBlock(Properties properties) {
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
        return new AromaLampBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LoungeBlocks.AROMA_LAMP_BE.get(), AromaLampBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AromaLampBlockEntity lamp)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        String essence = EssenceBottleItem.typeOf(stack);
        if (!essence.isEmpty()) {
            if (!level.isClientSide) pourBottle(lamp, essence, stack, player, hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.FLINT_AND_STEEL) && lamp.canLight()) {
            if (!level.isClientSide) {
                lamp.setLit(true);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                level.playSound(null, pos, ModSoundEvents.LAMP_LIT.get(), SoundSource.BLOCKS, 1f, 1f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void pourBottle(AromaLampBlockEntity lamp, String essence, ItemStack bottle, Player player, InteractionHand hand) {
        int poured = lamp.pour(essence, EssenceBottleItem.amountOf(bottle));
        if (poured <= 0) return;
        int left = EssenceBottleItem.amountOf(bottle) - poured;
        if (left > 0) {
            bottle.set(ModDataComponents.ESSENCE_AMOUNT.get(), left);
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(bottle, player, new ItemStack(Items.GLASS_BOTTLE)));
        }
        player.level().playSound(null, lamp.getBlockPos(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8f, 1.1f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof AromaLampBlockEntity lamp)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isSecondaryUseActive() && lamp.isLit()) {
            lamp.setLit(false);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
        } else {
            player.displayClientMessage(describe(lamp), true);
        }
        return InteractionResult.CONSUME;
    }

    private static Component describe(AromaLampBlockEntity lamp) {
        if (lamp.amount() <= 0) return Component.translatable("block.quasium.aroma_lamp.empty");
        Component essenceName = EssenceBottleItem.of(lamp.essence(), lamp.amount()).getHoverName();
        return Component.translatable("block.quasium.aroma_lamp.contents", essenceName, lamp.amount(),
                AromaLampBlockEntity.capacity());
    }
}
