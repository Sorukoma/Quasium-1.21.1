package net.sorukoma.quasium.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class HookahBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3.5, 0, 3.5, 12.5, 5, 12.5), Block.box(4.5, 5, 4.5, 11.5, 7, 11.5),
            Block.box(6, 7, 6, 10, 15, 10), Block.box(3, 15, 3, 13, 17, 13), Block.box(6.5, 17, 6.5, 9.5, 18, 9.5));

    public HookahBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {

        if (pStack.is(ModItems.EMPTY_SMOKE_JAR.get()) || pStack.is(ModItems.KAIFOMETER.get())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;

        if (pLevel.isClientSide()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Серверная логика:
        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if (!(entity instanceof HookahBlockEntity hookahBlockEntity)) {
            LOGGER.error("Server: Hookah Block Entity not found at {}. Throwing IllegalStateException.", pPos);
            throw new IllegalStateException("Our Container provider is missing!");
        }

        // Если игрок держит HookahPipeItem
        if (pStack.is(ModItems.HOOKAH_PIPE.get())) {
            // Логика прикрепления/затяжки теперь полностью управляется ClientHookahInputHandler через пакеты.
            // Этот метод просто подтверждает, что взаимодействие с трубкой было обработано.
            return ItemInteractionResult.CONSUME;
        }

        // Для любого другого предмета — открываем GUI
        if (!pPlayer.isShiftKeyDown()) {
            triggerHookahAdvancement((ServerPlayer) pPlayer);
            PlayerTobaccoData.syncOnLogin((ServerPlayer) pPlayer);
            ((ServerPlayer) pPlayer).openMenu(hookahBlockEntity, pPos);
            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHit) {

        if (pLevel.isClientSide()) {
            return InteractionResult.PASS; // Пусть сервер обрабатывает
        }

        // Серверная логика:
        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if (!(entity instanceof HookahBlockEntity hookahBlockEntity)) {
            LOGGER.error("Server: Hookah Block Entity not found at {}. Throwing IllegalStateException.", pPos);
            throw new IllegalStateException("Our Container provider is missing!");
        }

        // Если Shift-клик без предмета, это теперь обрабатывается PacketDetachPipeToServer
        if (pPlayer.isShiftKeyDown()) {
            return InteractionResult.CONSUME;
        } else {
            // Сценарий: ПКМ без Shift без предмета (Открыть GUI)
            triggerHookahAdvancement((ServerPlayer) pPlayer);
            PlayerTobaccoData.syncOnLogin((ServerPlayer) pPlayer);
            ((ServerPlayer) pPlayer).openMenu(hookahBlockEntity, pPos);
            return InteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new HookahBlockEntity(pPos, pState);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            if (!pLevel.isClientSide()) {
                BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
                if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {
                    List<HookahConnectionEntity> attachedConnections = pLevel.getEntitiesOfClass(
                            HookahConnectionEntity.class,
                            new AABB(pPos).inflate(HookahConnectionEntity.PIPE_DETACH_DISTANCE_SQ + 1.0),
                            hookahConnection -> hookahConnection.getHookahBlockPos() != null &&
                                                hookahConnection.getHookahBlockPos().equals(pPos)
                    );
                    for (HookahConnectionEntity connection : attachedConnections) {
                        Player attachedPlayer = pLevel.getPlayerByUUID(connection.getAttachedPlayerUUID());
                        if (attachedPlayer != null) {
                            connection.stopPuffing(hookahBlockEntity, attachedPlayer);
                            attachedPlayer.displayClientMessage(Component.translatable("quasium.message.pipe_detached_broken"), true);
                        }
                        connection.discard();
                    }
                    hookahBlockEntity.drops();
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()) {
            return null;
        }
        return createTickerHelper(pBlockEntityType, ModBlockEntities.HOOKAH_BE.get(), HookahBlockEntity::tick);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new HookahBlock(properties));
    }

    private void triggerHookahAdvancement(ServerPlayer serverPlayer) {
        QuasiumModAdvancements.HOOKAH_OPENED.get().trigger(serverPlayer);
    }
}
