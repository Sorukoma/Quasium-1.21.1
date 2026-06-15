package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;

import javax.annotation.Nullable;
import java.util.List;

public class NetheriteHammerItem extends Item {

    public NetheriteHammerItem(Properties properties) {
        super(properties.stacksTo(1).durability(2031).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.quasium.netherite_hammer.tooltip"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
