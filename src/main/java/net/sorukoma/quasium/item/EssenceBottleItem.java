package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.List;

public class EssenceBottleItem extends Item {
    public EssenceBottleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(String essenceType, int ml) {
        ItemStack bottle = new ItemStack(ModItems.ESSENCE_BOTTLE.get());
        bottle.set(ModDataComponents.ESSENCE_TYPE.get(), essenceType);
        bottle.set(ModDataComponents.ESSENCE_AMOUNT.get(), ml);
        return bottle;
    }

    public static String typeOf(ItemStack stack) {
        return stack.is(ModItems.ESSENCE_BOTTLE.get()) ? stack.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "") : "";
    }

    public static int amountOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), 0);
    }

    public static ItemStack drained(ItemStack bottle, int ml) {
        int left = amountOf(bottle) - ml;
        if (left <= 0) return new ItemStack(Items.GLASS_BOTTLE);
        ItemStack rest = bottle.copy();
        rest.set(ModDataComponents.ESSENCE_AMOUNT.get(), left);
        return rest;
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
