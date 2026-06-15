package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.List;

public class CharcoalItem extends Item {
    private final ModDataComponents.CharcoalSize size;

    public CharcoalItem(ModDataComponents.CharcoalSize size, Properties properties) {
        super(properties);
        this.size = size;
    }

    public ModDataComponents.CharcoalSize getSize() {
        return size;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ModDataComponents.CharcoalData data = stack.get(ModDataComponents.CHARCOAL_DATA.get());
        if (data != null && data.heatingProgress() > 0f) {
            tooltipComponents.add(Component.translatable("quasium.tooltip.charcoal_heated", String.format("%.1f", data.heatingProgress())));
        }
    }
}