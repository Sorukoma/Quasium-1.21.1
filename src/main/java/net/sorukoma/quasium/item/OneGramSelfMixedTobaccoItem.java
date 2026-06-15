package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;
import net.minecraft.ChatFormatting;

import java.util.List;

public class OneGramSelfMixedTobaccoItem extends Item {

    public OneGramSelfMixedTobaccoItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);

        // Основа (тип листьев)
        if (pStack.has(ModDataComponents.TOBACCO_BASE.get())) {
            String base = pStack.get(ModDataComponents.TOBACCO_BASE.get());
            pTooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_base", Component.translatable("quasium.base." + base)).withStyle(ChatFormatting.GOLD));
        }

        // Крепость
        if (pStack.has(ModDataComponents.TOBACCO_DAMAGE.get())) {
            float strength = pStack.get(ModDataComponents.TOBACCO_DAMAGE.get());
            pTooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_strength", String.format("%.1f", strength)).withStyle(ChatFormatting.RED));
        }

        // Эффекты
        if (pStack.has(ModDataComponents.SELF_MIXED_EFFECTS.get())) {
            List<MobEffectInstance> effects = pStack.get(ModDataComponents.SELF_MIXED_EFFECTS.get());
            for (MobEffectInstance effect : effects) {
                Component effectName = Component.translatable(effect.getDescriptionId());
                pTooltipComponents.add(Component.translatable("quasium.tooltip.potion_effect", effectName).withStyle(ChatFormatting.BLUE));
            }
        } else if (pStack.has(ModDataComponents.POTION_EFFECT.get())) {
            MobEffectInstance effect = pStack.get(ModDataComponents.POTION_EFFECT.get());
            Component effectName = Component.translatable(effect.getDescriptionId());
            pTooltipComponents.add(Component.translatable("quasium.tooltip.potion_effect", effectName).withStyle(ChatFormatting.BLUE));
        }

        pTooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_weight", 1).withStyle(ChatFormatting.GRAY));
        if (pStack.has(ModDataComponents.HEATED_MARKER.get()) && pStack.get(ModDataComponents.HEATED_MARKER.get())) {
            pTooltipComponents.add(Component.translatable("quasium.tooltip.heated_tobacco").withStyle(ChatFormatting.RED));
        }
    }
}
