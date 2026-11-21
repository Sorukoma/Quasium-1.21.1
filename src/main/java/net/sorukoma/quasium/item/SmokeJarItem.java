package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.List;

public class SmokeJarItem extends BlockItem {
    public SmokeJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String flavor = stack.getOrDefault(ModDataComponents.TOBACCO_FLAVOR.get(), "");
        if (!flavor.isEmpty()) {
            tooltip.add(Component.translatable("quasium.smoke_jar.flavor", Component.translatable("quasium.flavor." + flavor))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("quasium.smoke_jar.hint", ConfigArgs.synced(Config.SMOKE_JAR_MINUTES)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
