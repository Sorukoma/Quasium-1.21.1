package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.List;

public class EssenceBottleItem extends Item {
    public EssenceBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        String type = stack.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "unknown");
        return Component.translatable("item.quasium.essence_bottle." + type);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("quasium.tooltip.essence_amount",
                stack.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), 0)));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
