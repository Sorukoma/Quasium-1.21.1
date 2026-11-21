package net.sorukoma.quasium.mind;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TooltipItem extends Item {
    private final String tooltip;

    public TooltipItem(Properties properties, String tooltip) {
        super(properties);
        this.tooltip = tooltip;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
    }
}
