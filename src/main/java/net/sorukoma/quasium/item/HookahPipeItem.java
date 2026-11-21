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
import net.sorukoma.quasium.cart.CartPipePacket;
import net.sorukoma.quasium.client.CartClient;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.magic.SmokeCasting;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class HookahPipeItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean holdsPipe(Player player) {
        return player.getMainHandItem().is(ModItems.HOOKAH_PIPE.get()) || player.getOffhandItem().is(ModItems.HOOKAH_PIPE.get());
    }

    public static boolean carriesPipe(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.HOOKAH_PIPE.get()))
                || player.containerMenu.getCarried().is(ModItems.HOOKAH_PIPE.get());
    }

    public static boolean coolingDown(Player player) {
        ItemStack pipe = player.getMainHandItem().is(ModItems.HOOKAH_PIPE.get()) ? player.getMainHandItem() : player.getOffhandItem();
        Long end = pipe.get(ModDataComponents.HOOKAH_PIPE_COOLDOWN_END.get());
        return end != null && player.level().getGameTime() < end || !SmokeCasting.breathReady(player);
    }

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

            if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK
                    && pLevel.getBlockEntity(blockHit.getBlockPos()) instanceof net.sorukoma.quasium.block.entity.HookahBlockEntity
                    && !blockHit.getBlockPos().equals(ClientHookahInputHandler.getConnectedHookahPos())) {
                return InteractionResultHolder.pass(itemstack);
            }

            if (!net.sorukoma.quasium.client.RingTiming.breathReady(pLevel)) {
                return InteractionResultHolder.fail(itemstack);
            }

            if (ClientHookahInputHandler.getConnectedHookahPos() != null) {
                pPlayer.startUsingItem(pUsedHand);
                PacketDistributor.sendToServer(new PacketStartPuffing(ClientHookahInputHandler.getConnectedHookahPos()));
                net.sorukoma.quasium.client.PuffSoundPlayer.start(pPlayer);
                net.sorukoma.quasium.client.RingTiming.pullStarted();
                return InteractionResultHolder.consume(itemstack);
            } else if (ClientHookahInputHandler.isConnectedToBackpack()) {
                pPlayer.startUsingItem(pUsedHand);
                PacketDistributor.sendToServer(new PacketStartPuffingBackpack());
                net.sorukoma.quasium.client.PuffSoundPlayer.start(pPlayer);
                net.sorukoma.quasium.client.RingTiming.pullStarted();
                return InteractionResultHolder.consume(itemstack);
            } else if (CartClient.connectedCart() >= 0) {
                pPlayer.startUsingItem(pUsedHand);
                PacketDistributor.sendToServer(new CartPipePacket(CartClient.connectedCart(), true));
                net.sorukoma.quasium.client.PuffSoundPlayer.start(pPlayer);
                net.sorukoma.quasium.client.RingTiming.pullStarted();
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
            net.sorukoma.quasium.client.PuffSoundPlayer.stop();
            net.sorukoma.quasium.client.RingTiming.pullReleased((Player) pLivingEntity);
            BlockPos connectedPos = ClientHookahInputHandler.getConnectedHookahPos();
            if (connectedPos != null) {
                PacketDistributor.sendToServer(new PacketStopPuffing(connectedPos));
            } else if (ClientHookahInputHandler.isConnectedToBackpack()) {
                PacketDistributor.sendToServer(new PacketStopPuffingBackpack());
            } else if (CartClient.connectedCart() >= 0) {
                PacketDistributor.sendToServer(new CartPipePacket(CartClient.connectedCart(), false));
            }
        }
    }
}