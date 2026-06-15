package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Map;

public class MixedTobaccoItem extends Item {

    public MixedTobaccoItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public int getDefaultMaxStackSize() {
        return 1;
    }

    private String getFlavorDisplayName(String flavor) {
        return Component.translatable("quasium.flavor." + flavor).getString();
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);

        if (pStack.has(ModDataComponents.TOBACCO_WEIGHT.get())) {
            int weight = pStack.get(ModDataComponents.TOBACCO_WEIGHT.get());
            pTooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_weight", weight).withStyle(ChatFormatting.GRAY));
        }

        if (pStack.has(ModDataComponents.TOBACCO_MIX.get())) {
            ModDataComponents.TobaccoMixData mixData = pStack.get(ModDataComponents.TOBACCO_MIX.get());
            net.minecraft.network.chat.MutableComponent combined = Component.literal("");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : mixData.flavors().entrySet()) {
                if (!first) combined.append(Component.literal(" / "));
                first = false;
                combined.append(Component.literal(entry.getValue() + "% ").append(Component.translatable("quasium.flavor." + entry.getKey())));
            }
            pTooltipComponents.add(Component.translatable("quasium.tooltip.mixed_tobacco_multi", combined).withStyle(ChatFormatting.AQUA));
        }
    }
}