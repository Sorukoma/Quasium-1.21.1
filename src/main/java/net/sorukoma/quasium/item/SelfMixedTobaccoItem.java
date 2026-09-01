package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.component.ModDataComponents;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;


public class SelfMixedTobaccoItem extends Item {
    public SelfMixedTobaccoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack selfMixedStack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(selfMixedStack);

        int weight = selfMixedStack.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0);
        if (weight <= 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.0f);
            return InteractionResultHolder.fail(selfMixedStack);
        }

        // Отщипываем 1г: создаём OneGramSelfMixedItem с теми же компонентами
        ItemStack oneGram = new ItemStack(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO.get());
        copySelfMixedComponents(selfMixedStack, oneGram);

        // Уменьшаем вес исходного предмета
        selfMixedStack.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight - 1);

        // Если вес стал 0 — уничтожаем предмет
        if (weight - 1 <= 0) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }

        // Даём 1г предмет в инвентарь (или дропаем если инвентарь полон)
        if (!player.getInventory().add(oneGram)) {
            player.drop(oneGram, false);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
        return InteractionResultHolder.sidedSuccess(selfMixedStack, level.isClientSide());
    }

    public static ItemStack createOneGram(ItemStack source) {
        ItemStack oneGram = new ItemStack(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO.get());
        copySelfMixedComponents(source, oneGram);
        return oneGram;
    }

    private static void copySelfMixedComponents(ItemStack source, ItemStack target) {
        if (source.has(ModDataComponents.TOBACCO_BASE.get())) {
            target.set(ModDataComponents.TOBACCO_BASE.get(), source.get(ModDataComponents.TOBACCO_BASE.get()));
        }
        if (source.has(ModDataComponents.TOBACCO_DAMAGE.get())) {
            target.set(ModDataComponents.TOBACCO_DAMAGE.get(), source.get(ModDataComponents.TOBACCO_DAMAGE.get()));
        }
        if (source.has(ModDataComponents.MOLASSES_AMOUNT.get())) {
            target.set(ModDataComponents.MOLASSES_AMOUNT.get(), source.get(ModDataComponents.MOLASSES_AMOUNT.get()));
        }
        if (source.has(ModDataComponents.ESSENCE_TYPE.get())) {
            target.set(ModDataComponents.ESSENCE_TYPE.get(), source.get(ModDataComponents.ESSENCE_TYPE.get()));
        }
        if (source.has(ModDataComponents.ESSENCE_AMOUNT.get())) {
            target.set(ModDataComponents.ESSENCE_AMOUNT.get(), source.get(ModDataComponents.ESSENCE_AMOUNT.get()));
        }
        if (source.has(ModDataComponents.TOXIC.get())) {
            target.set(ModDataComponents.TOXIC.get(), source.get(ModDataComponents.TOXIC.get()));
        }
        if (source.has(ModDataComponents.SELF_MIXED_EFFECTS.get())) {
            target.set(ModDataComponents.SELF_MIXED_EFFECTS.get(), source.get(ModDataComponents.SELF_MIXED_EFFECTS.get()));
        } else if (source.has(ModDataComponents.POTION_EFFECT.get())) {
            // Одиночный эффект → конвертируем в SELF_MIXED_EFFECTS list для унификации
            List<MobEffectInstance> effects = new ArrayList<>();
            effects.add(source.get(ModDataComponents.POTION_EFFECT.get()));
            target.set(ModDataComponents.SELF_MIXED_EFFECTS.get(), effects);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        // Основа (тип листьев)
        if (stack.has(ModDataComponents.TOBACCO_BASE.get())) {
            String base = stack.get(ModDataComponents.TOBACCO_BASE.get());
            tooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_base", Component.translatable("quasium.base." + base)).withStyle(ChatFormatting.GOLD));
        }

        // Масса
        if (stack.has(ModDataComponents.TOBACCO_WEIGHT.get())) {
            int weight = stack.get(ModDataComponents.TOBACCO_WEIGHT.get());
            tooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_weight", weight).withStyle(ChatFormatting.GRAY));
        }

        // Крепость
        if (stack.has(ModDataComponents.TOBACCO_DAMAGE.get())) {
            float strength = stack.get(ModDataComponents.TOBACCO_DAMAGE.get());
            tooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_strength", String.format("%.1f", strength)).withStyle(ChatFormatting.RED));
        }

        // Меласса
        if (stack.has(ModDataComponents.MOLASSES_AMOUNT.get())) {
            int ml = stack.get(ModDataComponents.MOLASSES_AMOUNT.get());
            tooltipComponents.add(Component.translatable("quasium.tooltip.molasses_ml", ml).withStyle(ChatFormatting.AQUA));
        }

        // Токсичность (мутация "toxic" гибрида) — предупреждение об уроне при курении
        if (stack.getOrDefault(ModDataComponents.TOXIC.get(), false)) {
            tooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_toxic").withStyle(ChatFormatting.DARK_GREEN));
        }

        // Эффекты зелья (список — для смешанных самозамесов)
        if (stack.has(ModDataComponents.SELF_MIXED_EFFECTS.get())) {
            List<MobEffectInstance> effects = stack.get(ModDataComponents.SELF_MIXED_EFFECTS.get());
            for (MobEffectInstance effect : effects) {
                Component effectName = Component.translatable(effect.getDescriptionId());
                tooltipComponents.add(Component.translatable("quasium.tooltip.potion_effect", effectName).withStyle(ChatFormatting.BLUE));
            }
        } else if (stack.has(ModDataComponents.POTION_EFFECT.get())) {
            // Одиночный эффект (для несмешанного самозамеса)
            MobEffectInstance effect = stack.get(ModDataComponents.POTION_EFFECT.get());
            Component effectName = Component.translatable(effect.getDescriptionId());
            tooltipComponents.add(Component.translatable("quasium.tooltip.potion_effect", effectName).withStyle(ChatFormatting.BLUE));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
