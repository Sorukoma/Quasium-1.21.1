package net.sorukoma.quasium.ritual.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

public class ArtifactItem extends Item {
    private final Supplier<Object[]> tooltipArgs;

    public ArtifactItem(Properties properties) {
        this(properties, () -> new Object[0]);
    }

    public ArtifactItem(Properties properties, Supplier<Object[]> tooltipArgs) {
        super(properties);
        this.tooltipArgs = tooltipArgs;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(BuiltInRegistries.ITEM.getKey(this).toLanguageKey("item") + ".tooltip", tooltipArgs.get())
                .withStyle(ChatFormatting.GRAY));
    }
}
