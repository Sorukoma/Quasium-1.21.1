package net.sorukoma.quasium.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult; // Import for ItemInteractionResult
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class HookahBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();

    public HookahBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public HookahBlock() {
        this(BlockBehaviour.Properties.ofFullCopy(Blocks.BREWING_STAND)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        LOGGER.debug("HookahBlock.useItemOn called. ClientSide: {}, Player: {}, Hand: {}, Shift: {}, Item: {}", pLevel.isClientSide(), pPlayer.getName().getString(), pHand, pPlayer.isShiftKeyDown(), pStack.getItem().getDescriptionId());

        if (pLevel.isClientSide()) {
            // Client-side logic for puffing:
            // If player is holding the pipe, is attached, and not Shift-clicking,
            // we assume the client will send a puff request packet.
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if (entity instanceof HookahBlockEntity hookahBlockEntity) {
                if (!pPlayer.isShiftKeyDown() && pStack.getItem() == ModItems.HOOKAH_PIPE.get() &&
                    hookahBlockEntity.isPipeAttached() && pPlayer.getUUID().equals(hookahBlockEntity.getAttachedPlayerUUID())) {
                    LOGGER.debug("Client: Player is attached and holding pipe, not Shift-clicking. Returning SUCCESS for puffing intent.");
                    return ItemInteractionResult.SUCCESS; // Indicate client handled interaction
                }
            }
            LOGGER.debug("Client: Not puffing scenario. Returning PASS_TO_DEFAULT_BLOCK_INTERACTION.");
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // Let server handle other cases
        }

        // Server-side logic:
        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if (!(entity instanceof HookahBlockEntity hookahBlockEntity)) {
            LOGGER.error("Server: Hookah Block Entity not found at {}. Throwing IllegalStateException.", pPos);
            throw new IllegalStateException("Our Container provider is missing!");
        }

        if (pPlayer.isShiftKeyDown()) {
            LOGGER.debug("Server: Shift-clicking with item detected.");
            // Shift+ПКМ с предметом: Привязать трубку
            if (pStack.getItem() == ModItems.HOOKAH_PIPE.get()) {
                LOGGER.debug("Server: Holding Hookah Pipe.");
                if (!hookahBlockEntity.isPipeAttached()) {
                    hookahBlockEntity.attachPipe(pPlayer.getUUID(), pPlayer.getName().getString());
                    pPlayer.sendSystemMessage(Component.literal("You attached the hookah pipe."));
                    LOGGER.debug("Server: Pipe attached. Returning SUCCESS.");
                    return ItemInteractionResult.SUCCESS;
                } else {
                    if (hookahBlockEntity.getAttachedPlayerUUID().equals(pPlayer.getUUID())) {
                        pPlayer.sendSystemMessage(Component.literal("Your pipe is already attached to this hookah."));
                        LOGGER.debug("Server: Pipe already attached by this player. Returning FAIL.");
                    } else {
                        pPlayer.sendSystemMessage(Component.literal("This hookah already has a pipe attached by " + hookahBlockEntity.getAttachedPlayerName() + "."));
                        LOGGER.debug("Server: Pipe attached by another player. Returning FAIL.");
                    }
                    return ItemInteractionResult.FAIL; // Indicate no change
                }
            }
        } else {
            LOGGER.debug("Server: Regular Right-clicking with item detected.");
            // Обычный ПКМ с предметом (не Shift)
            if (pStack.getItem() == ModItems.HOOKAH_PIPE.get()) {
                if (hookahBlockEntity.isPipeAttached() && pPlayer.getUUID().equals(hookahBlockEntity.getAttachedPlayerUUID())) {
                    LOGGER.debug("Server: Player is attached and holding pipe. Assuming client will handle puffing. Returning SUCCESS.");
                    // Server-side: if client sends a puff packet, it will call processPuff.
                    // Here, we just acknowledge the interaction.
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        LOGGER.debug("Server: useItemOn not handled by custom logic. Returning PASS_TO_DEFAULT_BLOCK_INTERACTION.");
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // Let default block interaction handle it (e.g., open GUI if not pipe)
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHit) {
        LOGGER.debug("HookahBlock.useWithoutItem called. ClientSide: {}, Player: {}, Shift: {}", pLevel.isClientSide(), pPlayer.getName().getString(), pPlayer.isShiftKeyDown());

        if (pLevel.isClientSide()) {
            LOGGER.debug("Client: useWithoutItem. Returning PASS.");
            return InteractionResult.PASS; // Let server handle
        }

        // Server-side logic:
        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if (!(entity instanceof HookahBlockEntity hookahBlockEntity)) {
            LOGGER.error("Server: Hookah Block Entity not found at {}. Throwing IllegalStateException.", pPos);
            throw new IllegalStateException("Our Container provider is missing!");
        }

        if (pPlayer.isShiftKeyDown()) {
            LOGGER.debug("Server: Shift-clicking without item detected (detach pipe scenario).");
            // Shift+ПКМ без предмета: Отвязать трубку
            if (hookahBlockEntity.isPipeAttached()) {
                if (hookahBlockEntity.getAttachedPlayerUUID().equals(pPlayer.getUUID())) {
                    hookahBlockEntity.detachPipe(pPlayer.getUUID());
                    pPlayer.sendSystemMessage(Component.literal("You detached the hookah pipe."));
                    LOGGER.debug("Server: Pipe detached. Returning SUCCESS.");
                    return InteractionResult.SUCCESS;
                } else {
                    pPlayer.sendSystemMessage(Component.literal("This hookah pipe is attached by " + hookahBlockEntity.getAttachedPlayerName() + "."));
                    LOGGER.debug("Server: Pipe attached by another player. Cannot detach. Returning FAIL.");
                }
            } else {
                pPlayer.sendSystemMessage(Component.literal("No hookah pipe is attached to this hookah. Hold a hookah pipe and Shift+Right Click to attach it."));
                LOGGER.debug("Server: No pipe attached. Returning FAIL.");
            }
            return InteractionResult.FAIL; // Indicate no change or failed attempt
        } else {
            LOGGER.debug("Server: Regular Right-clicking without item detected (open GUI scenario).");
            // Обычный ПКМ без предмета: Открыть GUI
            ((ServerPlayer) pPlayer).openMenu(hookahBlockEntity, pPos);
            LOGGER.debug("Server: Opening GUI. Returning CONSUME.");
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
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {
                // If pipe is attached, drop it when hookah is broken
                if (hookahBlockEntity.isPipeAttached()) {
                    if (pLevel instanceof ServerLevel serverLevel) {
                        Player attachedPlayer = serverLevel.getPlayerByUUID(hookahBlockEntity.getAttachedPlayerUUID());
                        if (attachedPlayer != null) {
                            attachedPlayer.sendSystemMessage(Component.literal("Your hookah pipe detached because the hookah was broken!"));
                            // Pipe is not consumed on attach, so it's not dropped here.
                        } else {
                            // If player is offline, drop the pipe on the ground
                            Containers.dropItemStack(pLevel, pPos.getX(), pPos.getY(), pPos.getZ(), new ItemStack(ModItems.HOOKAH_PIPE.get()));
                        }
                    }
                }
                hookahBlockEntity.drops();
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
}