package net.sorukoma.quasium.tea;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.progression.ConfigArgs;

public class CupOfTeaItem extends Item {
    public CupOfTeaItem(Properties properties) {
        super(properties);
    }

    public static void drink(LivingEntity drinker) {
        drinker.removeEffect(MobEffects.CONFUSION);
        drinker.addEffect(new MobEffectInstance(TeaBlocks.VIGOR, EcologyConfig.VIGOR_SECONDS.getAsInt() * 20));
        if (drinker instanceof ServerPlayer player) SmokecraftTrigger.fire(player, "tea_cup");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        if (!level.isClientSide) drink(drinker);
        if (drinker instanceof Player player && player.getAbilities().instabuild) return stack;
        ItemStack cup = new ItemStack(TeaBlocks.TEA_CUP.get());
        stack.shrink(1);
        if (stack.isEmpty()) return cup;
        if (drinker instanceof Player player && !player.getInventory().add(cup)) player.drop(cup, false);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ConfigArgs.synced(EcologyConfig.TEA_DRINK_TICKS);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
