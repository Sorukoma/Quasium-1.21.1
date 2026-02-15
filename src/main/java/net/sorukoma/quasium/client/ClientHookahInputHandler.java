package net.sorukoma.quasium.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.PacketPuffDuration;
import net.sorukoma.quasium.network.PacketStartPuffing;
import net.sorukoma.quasium.network.PacketStopPuffing;
import net.sorukoma.quasium.network.PacketAttachPipe;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler.ClientHookahData;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.util.TriState;

import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public class ClientHookahInputHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static long rightClickStartTime = -1;
    private static BlockPos targetHookahPos = null;
    // private static boolean isAttachInProgress = false; // Откатываем это поле

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        LOGGER.debug("ClientHookahInputHandler.onRightClickBlock called. Player: {}, Hand: {}, Shift: {}, Item: {}, Block: {}",
                minecraft.player.getName().getString(), event.getHand(), minecraft.player.isShiftKeyDown(),
                event.getItemStack().getItem().getDescriptionId(), event.getPos());

        // Обрабатываем только основную руку
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            BlockPos blockPos = event.getPos();
            BlockEntity blockEntity = minecraft.level.getBlockEntity(blockPos);

            if (blockEntity != null && blockEntity.getBlockState().is(ModBlocks.HOOKAH_BLOCK.get())) {
                if (blockEntity instanceof net.sorukoma.quasium.block.entity.HookahBlockEntity hookahBlockEntity) {
                    // Получаем актуальные данные о кальяне с клиента
                    ClientHookahData hookahData = PacketHookahPuffDataClientHandler.getClientHookahData(blockPos);

                    boolean isHoldingPipe = event.getItemStack().getItem() == ModItems.HOOKAH_PIPE.get();
                    boolean isPipeAttached = hookahData != null && hookahData.attachedPlayerUUID() != null;
                    boolean isAttachedByThisPlayer = isPipeAttached && minecraft.player.getUUID().equals(hookahData.attachedPlayerUUID());

                    LOGGER.debug("ClientHookahInputHandler: isHoldingPipe={}, isPipeAttached={}, isAttachedByThisPlayer={}",
                            isHoldingPipe, isPipeAttached, isAttachedByThisPlayer);

                    // Сценарий 1: Прикрепление трубки (Shift + ПКМ с трубкой)
                    if (minecraft.player.isShiftKeyDown() && isHoldingPipe && !isPipeAttached) {
                        PacketDistributor.sendToServer(new PacketAttachPipe(blockPos)); // Отправляем пакет для прикрепления
                        minecraft.player.sendSystemMessage(Component.literal("Attempting to attach hookah pipe..."));
                        event.setCanceled(true);
                        event.setUseItem(TriState.TRUE); // Исправлено на TRUE
                        event.setUseBlock(TriState.TRUE); // Исправлено на TRUE
                        LOGGER.debug("ClientHookahInputHandler: Attach pipe scenario. Sent PacketAttachPipe. Set Canceled=true, UseItem=TRUE, UseBlock=TRUE.");
                        return; // Важно, чтобы не обрабатывалось дальше
                    }

                    // Сценарий 2: Затяжка (ПКМ без Shift с трубкой, когда прикреплена)
                    if (!minecraft.player.isShiftKeyDown() && isHoldingPipe && isAttachedByThisPlayer) {
                        rightClickStartTime = minecraft.level.getGameTime();
                        targetHookahPos = blockPos;
                        PacketDistributor.sendToServer(new PacketStartPuffing(targetHookahPos));
                        LOGGER.debug("Client: Started puffing at {} at game time {}", targetHookahPos, rightClickStartTime);
                        event.setCanceled(true);
                        event.setUseItem(TriState.TRUE); // Исправлено на TRUE
                        event.setUseBlock(TriState.TRUE); // Исправлено на TRUE
                        LOGGER.debug("ClientHookahInputHandler: Puffing scenario. Set Canceled=true, UseItem=TRUE, UseBlock=TRUE.");
                        return; // Важно, чтобы не обрабатывалось дальше
                    }
                    
                    // Сценарий 3: Открепление трубки (Shift + ПКМ без предмета, когда прикреплена этим игроком)
                    if (minecraft.player.isShiftKeyDown() && !isHoldingPipe && isAttachedByThisPlayer) {
                        // Здесь мы не отправляем отдельный пакет, так как HookahBlock.useWithoutItem на сервере
                        // уже содержит логику detachPipe. Мы просто позволяем событию пройти.
                        LOGGER.debug("ClientHookahInputHandler: Detach pipe scenario (Shift+PKM without item). Allowing event to propagate.");
                        event.setUseBlock(TriState.DEFAULT);
                        event.setUseItem(TriState.DEFAULT);
                        return;
                    }

                    // Для всех остальных сценариев, позволяем методам HookahBlock на сервере обработать это.
                    // Например: ПКМ с трубкой, но не прикреплена (должно сказать прикрепить),
                    // Shift+ПКМ с трубкой, но уже прикреплена (должно сказать, что уже прикреплена),
                    // ПКМ без трубки (открыть GUI).
                    event.setUseBlock(TriState.DEFAULT);
                    event.setUseItem(TriState.DEFAULT);
                    LOGGER.debug("ClientHookahInputHandler: Default scenario. Set UseItem=DEFAULT, UseBlock=DEFAULT.");
                }
            }
        }
    }

    // Откатываем onRightClickEmpty
    // @SubscribeEvent
    // public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
    //     Minecraft minecraft = Minecraft.getInstance();
    //     if (minecraft.player == null || minecraft.level == null) return;
    //
    //     LOGGER.debug("ClientHookahInputHandler.onRightClickEmpty called. Player: {}, Hand: {}, Shift: {}, Item: {}",
    //             minecraft.player.getName().getString(), event.getHand(), minecraft.player.isShiftKeyDown(),
    //             event.getItemStack().getItem().getDescriptionId());
    //
    //     if (event.getHand() == InteractionHand.MAIN_HAND) {
    //         // Сценарий затяжки при ПКМ в воздух
    //         if (handlePuffing(minecraft, minecraft.player.isShiftKeyDown(), event.getItemStack())) {
    //             // event.setCanceled(true); // RightClickEmpty не имеет setCanceled
    //             event.setUseItem(InteractionResult.SUCCESS); // Используем InteractionResult для RightClickEmpty
    //             LOGGER.debug("ClientHookahInputHandler: Puffing handled by helper from RightClickEmpty. Set UseItem=SUCCESS.");
    //             return;
    //         }
    //         // Если не затяжка, позволяем другим обработчикам взять на себя (например, съесть еду)
    //         event.setUseItem(InteractionResult.PASS); // Используем InteractionResult для RightClickEmpty
    //         LOGGER.debug("ClientHookahInputHandler: Default scenario for RightClickEmpty. Set UseItem=PASS.");
    //     }
    // }

    @SubscribeEvent
    public static void onMouseButtonReleased(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        // Если отпущена правая кнопка мыши, и мы отслеживали затяжку
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == InputConstants.RELEASE && rightClickStartTime != -1 && targetHookahPos != null) {
            long durationHeldTicks = minecraft.level.getGameTime() - rightClickStartTime;

            LOGGER.debug("Client: Calculated puff duration: {} ticks (Start: {}, End: {})", durationHeldTicks, rightClickStartTime, minecraft.level.getGameTime());

            if (durationHeldTicks > 0) {
                PacketDistributor.sendToServer(new PacketPuffDuration(targetHookahPos, (int) durationHeldTicks));
            }
            PacketDistributor.sendToServer(new PacketStopPuffing(targetHookahPos));

            rightClickStartTime = -1;
            targetHookahPos = null;
        }
    }

    // Откатываем этот метод
    // public static void resetAttachInProgress() {
    //     if (isAttachInProgress) {
    //         LOGGER.debug("ClientHookahInputHandler: resetAttachInProgress called. Setting isAttachInProgress to false.");
    //         isAttachInProgress = false;
    //     }
    // }
}