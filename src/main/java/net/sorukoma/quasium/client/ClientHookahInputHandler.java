package net.sorukoma.quasium.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.PacketStopPuffing; // Эти пакеты теперь отправляются из HookahPipeItem
import net.sorukoma.quasium.network.PacketAttachPipeToServer;
import net.sorukoma.quasium.network.PacketDetachPipeToServer;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.common.util.TriState;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public class ClientHookahInputHandler {
    private static BlockPos connectedHookahPos = null;
    private static boolean connectedToBackpack = false;
    private static long lastInteractionTime = 0;
    private static final int INTERACTION_COOLDOWN_TICKS = 10; // Кулдаун в тиках (0.5 секунды)

    // Метод для установки подключенного кальяна (вызывается из PacketSyncHookahConnectionHandler)
    public static void setConnectedHookahPos(BlockPos pos) {
        connectedHookahPos = pos;
        if (pos != null) {
            connectedToBackpack = false; // Can't be connected to both at the same time
        }
    }

    // Метод для получения подключенного кальяна (используется HookahPipeItem)
    public static BlockPos getConnectedHookahPos() {
        return connectedHookahPos;
    }

    // Backpack hookah connection
    public static void setConnectedToBackpack(boolean connected) {
        connectedToBackpack = connected;
        if (connected) {
            connectedHookahPos = null; // Can't be connected to both at the same time
        }
    }

    public static boolean isConnectedToBackpack() {
        return connectedToBackpack;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        // Проверяем кулдаун
        if (minecraft.level.getGameTime() - lastInteractionTime < INTERACTION_COOLDOWN_TICKS) {
            event.setCanceled(true);
            event.setUseItem(TriState.TRUE);
            event.setUseBlock(TriState.TRUE);
            return;
        }

        // Обрабатываем только основную руку
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            BlockPos clickedBlockPos = event.getPos();
            BlockEntity blockEntity = minecraft.level.getBlockEntity(clickedBlockPos);

            boolean isHoldingPipe = event.getItemStack().getItem() == ModItems.HOOKAH_PIPE.get();
            boolean isShiftKeyDown = minecraft.player.isShiftKeyDown();
            boolean isHookahBlock = (blockEntity instanceof net.sorukoma.quasium.block.entity.HookahBlockEntity);

            // Сценарий 1: Прикрепление трубки (Shift + ПКМ с трубкой по кальяну)
            if (isShiftKeyDown && isHoldingPipe && isHookahBlock) {
                PacketDistributor.sendToServer(new PacketAttachPipeToServer(clickedBlockPos)); // Отправляем пакет на сервер
                minecraft.player.sendSystemMessage(Component.translatable("quasium.message.attempting_attach"));
                event.setCanceled(true); // Отменяем событие, чтобы серверный HookahBlock.useItemOn не обрабатывал его
                event.setUseItem(TriState.TRUE); // Указываем, что предмет был использован
                event.setUseBlock(TriState.TRUE); // Указываем, что блок был использован
                lastInteractionTime = minecraft.level.getGameTime(); // Обновляем время последней интеракции
                return;
            }

            // Сценарий 2: Открепление трубки (Shift + ПКМ ПУСТОЙ рукой по кальяну).
            // Важно: только пустая рука, иначе shift+ПКМ с шахтой/чашей/каллаудом
            // (сборка модульного кальяна) ошибочно трактовался бы как открепление.
            if (isShiftKeyDown && event.getItemStack().isEmpty() && isHookahBlock) {
                PacketDistributor.sendToServer(new PacketDetachPipeToServer(clickedBlockPos)); // Отправляем пакет на сервер
                minecraft.player.sendSystemMessage(Component.translatable("quasium.message.attempting_detach"));
                event.setCanceled(true); // Отменяем событие
                event.setUseItem(TriState.TRUE); // Указываем, что предмет был использован
                event.setUseBlock(TriState.TRUE); // Указываем, что блок был использован
                lastInteractionTime = minecraft.level.getGameTime(); // Обновляем время последней интеракции
                return;
            }
            // Сценарий 3: сборка/вода/GUI модульного кальяна. Ванильный конвейер НЕ вызывает
            // Block.useItemOn при shift+предмет, поэтому шлём собственный пакет на сервер
            // (как для трубки). Обрабатываем ЛКМ-жесты сборки и заливку воды.
            if (blockEntity instanceof net.sorukoma.quasium.block.entity.ModularHookahBlockEntity) {
                PacketDistributor.sendToServer(new net.sorukoma.quasium.network.PacketModularHookahInteract(
                        clickedBlockPos, event.getHand() == InteractionHand.MAIN_HAND));
                event.setCanceled(true);
                event.setUseItem(TriState.FALSE);
                event.setUseBlock(TriState.FALSE);
                lastInteractionTime = minecraft.level.getGameTime();
                return;
            }
        }
        // Для всех остальных сценариев, позволяем предмету/блоку обработать это.
        // Логика начала затяжки при клике по блоку (не кальяну) или по кальяну без Shift
        // теперь полностью в HookahPipeItem.java.
        event.setUseBlock(TriState.DEFAULT);
        event.setUseItem(TriState.DEFAULT);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        // Проверяем кулдаун
        if (minecraft.level.getGameTime() - lastInteractionTime < INTERACTION_COOLDOWN_TICKS) {
            return;
        }

        // Обрабатываем только основную руку
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            boolean isHoldingPipe = event.getItemStack().getItem() == ModItems.HOOKAH_PIPE.get();
            boolean isShiftKeyDown = minecraft.player.isShiftKeyDown();

            // Если игрок держит трубку и не нажимает Shift, и подключен к кальяну,
            // то это потенциальное начало затяжки по воздуху.
            // Логика начала затяжки теперь полностью в HookahPipeItem.java.
            // Здесь мы просто обновляем кулдаун и позволяем предмету обработать событие.
            if (!isShiftKeyDown && isHoldingPipe && connectedHookahPos != null) {
                lastInteractionTime = minecraft.level.getGameTime(); // Обновляем время последней интеракции
                // Не отменяем событие и не устанавливаем UseItem/UseBlock,
                // так как HookahPipeItem.use будет вызван автоматически.
                return;
            }
        }
        // Для всех остальных сценариев, просто позволяем событию пройти.
    }

    // Метод onMouseButtonReleased больше не нужен, т.к. releaseUsing в HookahPipeItem обрабатывает отпускание кнопки.
    // @SubscribeEvent
    // public static void onMouseButtonReleased(InputEvent.MouseButton.Post event) {
    //     Minecraft minecraft = Minecraft.getInstance();
    //     if (minecraft.player == null || minecraft.level == null) return;

    //     // Если отпущена правая кнопка мыши, и мы отслеживали затяжку
    //     if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == InputConstants.RELEASE && targetHookahPos != null) {
    //         PacketDistributor.sendToServer(new PacketStopPuffing(targetHookahPos));
    //         LOGGER.debug("Client: Stopped puffing at {}", targetHookahPos);
    //         targetHookahPos = null;
    //     }
    // }
}