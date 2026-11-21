package net.sorukoma.quasium.selection.quality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.SelectionRegistry;

import java.util.List;

public class SecretRecipeItem extends Item {
    public SecretRecipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && !stack.has(SelectionRegistry.SECRET_MIX.get())) {
            stack.set(SelectionRegistry.SECRET_MIX.get(), SecretMix.flavors(server.getSeed()));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<String> mix = stack.get(SelectionRegistry.SECRET_MIX.get());
        if (mix == null || mix.isEmpty()) return;
        Component flavors = ComponentUtils.formatList(mix, Component.literal(" + "), flavor -> Component.translatable("quasium.flavor." + flavor));
        tooltip.add(Component.translatable("quasium.tooltip.secret_recipe", flavors).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("quasium.tooltip.secret_recipe.bonus", ConfigArgs.synced(SelectionConfig.SECRET_MIX_KAIF_BONUS_PCT))
                .withStyle(ChatFormatting.GRAY));
    }
}
