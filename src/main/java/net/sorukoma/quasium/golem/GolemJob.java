package net.sorukoma.quasium.golem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;
import net.sorukoma.quasium.item.ModItems;

import java.util.Locale;

public enum GolemJob {
    TOBACCO, LUMBERJACK, HERDER, SHEARER, FISHER;

    public static GolemJob of(ItemStack tool) {
        if (tool.canPerformAction(ItemAbilities.AXE_DIG)) return LUMBERJACK;
        if (tool.canPerformAction(ItemAbilities.SWORD_DIG)) return HERDER;
        if (tool.canPerformAction(ItemAbilities.SHEARS_HARVEST)) return SHEARER;
        if (tool.canPerformAction(ItemAbilities.FISHING_ROD_CAST) || tool.is(Tags.Items.TOOLS_FISHING_ROD)) return FISHER;
        return TOBACCO;
    }

    public static boolean isTool(ItemStack stack) {
        if (stack.is(ModItems.HOOKAH_PIPE.get()) || of(stack) != TOBACCO) return true;
        return stack.isDamageableItem() && !(stack.getItem() instanceof ArmorItem);
    }

    public String key() {
        return "quasium.golem.job." + name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable(key());
    }
}
