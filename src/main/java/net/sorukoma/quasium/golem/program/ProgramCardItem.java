package net.sorukoma.quasium.golem.program;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ProgramCardItem extends Item {
    public ProgramCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !written(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.quasium.program_card.tooltip").withStyle(ChatFormatting.GRAY));
        Program program = written(stack);
        if (program.isEmpty()) {
            tooltip.add(Component.translatable("item.quasium.program_card.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.quasium.program_card.blocks", program.size()).withStyle(ChatFormatting.AQUA));
    }

    private static Program written(ItemStack stack) {
        return stack.getOrDefault(ProgramBlocks.PROGRAM.get(), Program.EMPTY);
    }
}
