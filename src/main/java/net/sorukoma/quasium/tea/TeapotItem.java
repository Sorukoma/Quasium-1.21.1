package net.sorukoma.quasium.tea;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.EcologyRegistry;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.List;

public class TeapotItem extends Item {
    private static final int TEA_COLOR = 0xC8823C;
    private static final int BAR_WIDTH = 13;

    public TeapotItem(Properties properties) {
        super(properties);
    }

    public static int cups(ItemStack teapot) {
        return teapot.getOrDefault(TeaBlocks.TEA_CUPS.get(), 0);
    }

    public static void setCups(ItemStack teapot, int cups) {
        if (cups > 0) {
            teapot.set(TeaBlocks.TEA_CUPS.get(), cups);
        } else {
            teapot.remove(TeaBlocks.TEA_CUPS.get());
        }
    }

    public static boolean isFull(ItemStack teapot) {
        return cups(teapot) >= ConfigArgs.synced(EcologyConfig.TEAPOT_CUPS);
    }

    private static boolean pour(Player player, ItemStack teapot) {
        int emptyCup = player.getInventory().findSlotMatchingItem(new ItemStack(TeaBlocks.TEA_CUP.get()));
        if (cups(teapot) <= 0 || emptyCup < 0) return false;
        player.getInventory().removeItem(emptyCup, 1);
        setCups(teapot, cups(teapot) - 1);
        ItemStack tea = new ItemStack(TeaBlocks.CUP_OF_TEA.get());
        if (!player.getInventory().add(tea)) player.drop(tea, false);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack teapot = player.getItemInHand(hand);
        if (cups(teapot) <= 0) return InteractionResultHolder.pass(teapot);
        if (level.isClientSide) return InteractionResultHolder.success(teapot);
        if (pour(player, teapot)) {
            level.playSound(null, player.blockPosition(), EcologyRegistry.TEA_POUR.get(), SoundSource.PLAYERS, 0.8f, 1f);
        } else {
            player.displayClientMessage(Component.translatable("item.quasium.teapot.need_cup"), true);
        }
        return InteractionResultHolder.consume(teapot);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return cups(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(BAR_WIDTH * Mth.clamp(cups(stack) / (float) ConfigArgs.synced(EcologyConfig.TEAPOT_CUPS), 0f, 1f));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return TEA_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int cups = cups(stack);
        tooltip.add(cups > 0 ? Component.translatable("item.quasium.teapot.cups", cups, ConfigArgs.synced(EcologyConfig.TEAPOT_CUPS))
                : Component.translatable("item.quasium.teapot.empty"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
