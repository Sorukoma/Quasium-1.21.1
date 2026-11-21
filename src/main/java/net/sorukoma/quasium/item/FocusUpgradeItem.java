package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.magic.FocusUpgrade;

import java.util.List;

public class FocusUpgradeItem extends Item {
    private final FocusUpgrade upgrade;

    public FocusUpgradeItem(FocusUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public FocusUpgrade upgrade() {
        return upgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("quasium.upgrade." + upgrade.id() + ".desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
