package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.List;

public class MolassesBottleItem extends Item {
    public MolassesBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        int mass = stack.getOrDefault(ModDataComponents.MOLASSES_MASS.get(), 5);
        tooltipComponents.add(Component.translatable("quasium.tooltip.molasses_bottle_mass", mass));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}