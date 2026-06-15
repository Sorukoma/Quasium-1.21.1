package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.component.ModDataComponents;
import net.minecraft.ChatFormatting;

import java.util.List;

public class TobaccoItem extends Item {
    private final String flavor;

    public TobaccoItem(String flavor, Properties pProperties) {
        super(pProperties);
        this.flavor = flavor;
    }

    public String getFlavor() {
        return flavor;
    }

    @Override
    public int getDefaultMaxStackSize() {
        return 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        if (pLevel.isClientSide()) {
            return InteractionResultHolder.success(itemstack);
        }

        Integer weight = itemstack.get(ModDataComponents.TOBACCO_WEIGHT.get());
        if (weight == null || weight <= 0) {
            pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.0F);
            return InteractionResultHolder.fail(itemstack);
        }

        // Уменьшаем массу пачки на 1
        int newWeight = weight - 1;
        itemstack.set(ModDataComponents.TOBACCO_WEIGHT.get(), newWeight);

        // Выдаем 1 грамм табака
        ItemStack oneGram = new ItemStack(ModItems.ONE_GRAM_TOBACCO.get());
        oneGram.set(ModDataComponents.TOBACCO_FLAVOR.get(), flavor);
        if (!pPlayer.getInventory().add(oneGram)) {
            pPlayer.drop(oneGram, false);
        }

        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                SoundEvents.CROP_BREAK, SoundSource.PLAYERS, 0.5F, 1.2F);

        return InteractionResultHolder.sidedSuccess(itemstack, false);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        if (pStack.has(ModDataComponents.TOBACCO_WEIGHT.get())) {
            int weight = pStack.get(ModDataComponents.TOBACCO_WEIGHT.get());
            pTooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_weight", weight).withStyle(ChatFormatting.GRAY));
        }
    }
}