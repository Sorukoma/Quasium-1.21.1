package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents;

public class LeavesItem extends Item {
    public LeavesItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.has(ModDataComponents.FERMENTED.get())) {
            return Component.translatable(this.getDescriptionId() + ".fermented");
        }
        return super.getName(stack);
    }
}