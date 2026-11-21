package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.block.entity.TobaccoPoleBlockEntity;
import net.sorukoma.quasium.selection.genetics.HybridCollection;
import net.sorukoma.quasium.selection.genetics.Lineage;

public class TobaccoPoleBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 6, 14, 6),
            Block.box(10, 0, 4, 12, 14, 6),
            Block.box(4, 0, 10, 6, 14, 12),
            Block.box(10, 0, 10, 12, 14, 12));

    private final float successChance;
    private final float mutationChance;

    public TobaccoPoleBlock(float successChance, float mutationChance, BlockBehaviour.Properties properties) {
        super(properties);
        this.successChance = successChance;
        this.mutationChance = mutationChance;
    }

    public float getSuccessChance() { return successChance; }
    public float getMutationChance() { return mutationChance; }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TobaccoPoleBlockEntity pole) {
            boolean bred = !pole.holdsClone();
            ItemStack result = pole.takeResult();
            if (!result.isEmpty()) {
                if (bred && player instanceof ServerPlayer serverPlayer) HybridCollection.discover(serverPlayer, Lineage.of(result));
                player.getInventory().placeItemBackInInventory(result);
                return InteractionResult.SUCCESS;
            }
            if (pole.getTotalTime() > 0) {
                int seconds = Math.max(0, (pole.getTotalTime() - pole.getProgress()) / 20);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("quasium.message.tobacco_pole.progress", seconds), true);
                return InteractionResult.SUCCESS;
            }
            if (pole.sameParentsOnly()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("quasium.message.tobacco_pole.same_parents"), true);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                                    InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.SHEARS)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TobaccoPoleBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TOBACCO_POLE_BE.get(), TobaccoPoleBlockEntity::tick);
    }
    @Override public MapCodec<? extends TobaccoPoleBlock> codec() { return simpleCodec(props -> new TobaccoPoleBlock(successChance, mutationChance, props)); }
}
