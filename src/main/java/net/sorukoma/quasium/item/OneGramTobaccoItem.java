package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;
import net.minecraft.ChatFormatting;

import java.util.List;

public class OneGramTobaccoItem extends Item {

    public OneGramTobaccoItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        if (pStack.has(ModDataComponents.TOBACCO_FLAVOR.get())) {
            String flavor = pStack.get(ModDataComponents.TOBACCO_FLAVOR.get());
            pTooltipComponents.add(Component.translatable("quasium.flavor." + flavor).withStyle(ChatFormatting.AQUA));
        }
        pTooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_weight", 1).withStyle(ChatFormatting.GRAY));
    }
}
