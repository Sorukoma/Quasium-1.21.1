package net.sorukoma.quasium.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3; // Import Vec3
import net.minecraft.world.level.ClipContext; // Import ClipContext

import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.network.PacketStartPuffing;
import net.sorukoma.quasium.network.PacketStopPuffing;
import net.sorukoma.quasium.network.PacketStartPuffingBackpack;
import net.sorukoma.quasium.network.PacketStopPuffingBackpack;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.component.ModDataComponents;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class HookahPipeItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public HookahPipeItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);

        // Проверка кулдауна трубки
        if (pLevel.isClientSide()) {
            Long cooldownEnd = itemstack.get(ModDataComponents.HOOKAH_PIPE_COOLDOWN_END.get());
            if (cooldownEnd != null && pLevel.getGameTime() < cooldownEnd) {
                pPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("quasium.message.pipe_cooldown"), true);
                return InteractionResultHolder.fail(itemstack);
            }
        }

        // Если игрок уже затягивается, продолжаем затяжку
        if (pPlayer.isUsingItem()) {
            return InteractionResultHolder.consume(itemstack);
        }

        if (pLevel.isClientSide()) { // Проверки на стороне клиента
            Vec3 viewVector = pPlayer.getLookAngle();
            Vec3 eyePosition = pPlayer.getEyePosition();
            Vec3 endVector = eyePosition.add(viewVector.scale(pPlayer.blockInteractionRange()));
            HitResult hitResult = pLevel.clip(new ClipContext(eyePosition, endVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pPlayer));

            boolean isShiftKeyDown = pPlayer.isShiftKeyDown();
            boolean isLookingAtHookahBlock = false;

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                isLookingAtHookahBlock = pLevel.getBlockEntity(blockHitResult.getBlockPos())
                        instanceof net.sorukoma.quasium.block.entity.HookahBlockEntity;
            }

            // Если игрок держит трубку, смотрит на блок кальяна и нажимает Shift,
            // это попытка присоединить трубку. Затяжка НЕ должна начинаться.
            // ClientHookahInputHandler должен обработать фактический пакет присоединения.
            if (isLookingAtHookahBlock && isShiftKeyDown) {
                return InteractionResultHolder.pass(itemstack);
            }

            // Если игрок кликает ПКМ по блоку кальяна, держа трубку, и НЕ нажимает Shift,
            // то мы пропускаем использование предмета, чтобы блок кальяна мог обработать свой интерактив (например, открыть GUI).
            // В этом случае затяжка НЕ должна начинаться.
            if (isLookingAtHookahBlock && !isShiftKeyDown) {
                return InteractionResultHolder.pass(itemstack); // Позволяем блоку кальяна обработать свой интерактив
            }

            // Начинаем использовать предмет (затяжку) только если игрок подключен к кальяну
            // и не сработали предыдущие условия (т.е. не клик по кальяну без Shift и не Shift+ПКМ для присоединения)
            if (ClientHookahInputHandler.getConnectedHookahPos() != null) {
                pPlayer.startUsingItem(pUsedHand);
                PacketDistributor.sendToServer(new PacketStartPuffing(ClientHookahInputHandler.getConnectedHookahPos()));
                return InteractionResultHolder.consume(itemstack);
            } else if (ClientHookahInputHandler.isConnectedToBackpack()) {
                pPlayer.startUsingItem(pUsedHand);
                PacketDistributor.sendToServer(new PacketStartPuffingBackpack());
                return InteractionResultHolder.consume(itemstack);
            }
        }

        return InteractionResultHolder.pass(itemstack);
    }

    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
        return 72000; // Максимальное время использования предмета (3600 секунд)
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW; // Используем анимацию лука для визуального отображения удержания
    }

    @Override
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
        // Server maintains puffing state — no packet needed each tick
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int pTimeCharged) {
        if (pLevel.isClientSide() && pLivingEntity instanceof Player) {
            BlockPos connectedPos = ClientHookahInputHandler.getConnectedHookahPos();
            if (connectedPos != null) {
                PacketDistributor.sendToServer(new PacketStopPuffing(connectedPos));
            } else if (ClientHookahInputHandler.isConnectedToBackpack()) {
                PacketDistributor.sendToServer(new PacketStopPuffingBackpack());
            }
        }
    }
}