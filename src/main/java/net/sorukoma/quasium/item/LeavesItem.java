package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.List;

public class LeavesItem extends Item {
    public LeavesItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.getOrDefault(ModDataComponents.SPOILED.get(), false)) {
            return Component.translatable(this.getDescriptionId() + ".spoiled");
        }
        if (stack.has(ModDataComponents.FERMENTED.get())) {
            return Component.translatable(this.getDescriptionId() + ".fermented");
        }
        int dryingStage = stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0);
        if (dryingStage == 2) return Component.translatable(this.getDescriptionId() + ".dried");
        if (dryingStage >= 3) return Component.translatable(this.getDescriptionId() + ".overdried");
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        int dryingStage = stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0);

        // Show drying progress only for leaves on the shelf (stage 1 = drying, stage 2 = done, stage 3 = overdried)
        if (dryingStage >= 1) {
            int percent = stack.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 0);

            // Stage indicator
            if (dryingStage == 1) {
                tooltipComponents.add(
                    Component.translatable("quasium.tooltip.leaves.drying_progress", percent)
                        .withStyle(ChatFormatting.YELLOW)
                );
            } else if (dryingStage == 2) {
                tooltipComponents.add(
                    Component.translatable("quasium.tooltip.leaves.dried", percent)
                        .withStyle(ChatFormatting.GREEN)
                );
                // Hint: strength bonus for well-dried leaves
                float strengthBonus = getStrengthBonus(percent);
                tooltipComponents.add(
                    Component.translatable("quasium.tooltip.leaves.strength_bonus",
                        String.format("%+.0f%%", (strengthBonus - 1.0f) * 100f))
                        .withStyle(ChatFormatting.GRAY)
                );
            } else if (dryingStage >= 3) {
                tooltipComponents.add(
                    Component.translatable("quasium.tooltip.leaves.overdried", percent)
                        .withStyle(ChatFormatting.RED)
                );
                tooltipComponents.add(
                    Component.translatable("quasium.tooltip.leaves.overdried_warning")
                        .withStyle(ChatFormatting.DARK_RED)
                );
            }
        }

        int strength = stack.getOrDefault(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), 0);
        int yield = stack.getOrDefault(ModDataComponents.TOBACCO_YIELD_STARS.get(), 0);
        int quality = stack.getOrDefault(ModDataComponents.TOBACCO_QUALITY_STARS.get(), 0);
        if (strength > 0) {
            tooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_stats",
                    stars(strength), stars(yield), stars(quality)).withStyle(ChatFormatting.GOLD));
        }

        String mutation = stack.getOrDefault(ModDataComponents.TOBACCO_MUTATION.get(), "");
        if (!mutation.isEmpty()) {
            tooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_mutation",
                    Component.translatable("quasium.mutation." + mutation)).withStyle(ChatFormatting.LIGHT_PURPLE));
            if ("toxic".equals(mutation)) {
                tooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_toxic").withStyle(ChatFormatting.DARK_GREEN));
            }
        }

        // Fermentation hints
        if (stack.has(ModDataComponents.FERMENTED.get())) {
            if (stack.getOrDefault(ModDataComponents.SPOILED.get(), false)) {
                tooltipComponents.add(
                    Component.translatable("quasium.tooltip.leaves.spoiled_hint")
                        .withStyle(ChatFormatting.DARK_RED)
                );
            } else {
                tooltipComponents.add(
                    Component.translatable("quasium.tooltip.leaves.fermented_hint")
                        .withStyle(ChatFormatting.AQUA)
                );
            }
        }
    }

    private static String stars(int value) {
        return "★".repeat(Math.max(0, value)) + "☆".repeat(Math.max(0, 5 - value));
    }

    /**
     * Returns a strength multiplier based on drying percent.
     * 100% = 1.0x (baseline), 100-140% scales smoothly up to 1.15x, 140%+ = overdried = 1.2x
     */
    public static float getStrengthBonus(int percent) {
        if (percent <= 0) return 0.7f;      // Undried = weaker
        if (percent < 70) return 0.85f;     // Partially dried
        if (percent < 100) return 0.95f;    // Almost dried
        if (percent <= 140) {
            // Scale from 1.0 at 100% to 1.15 at 140%
            float t = (percent - 100) / 40.0f;
            return 1.0f + t * 0.15f;
        }
        return 1.2f; // Overdried
    }
}
