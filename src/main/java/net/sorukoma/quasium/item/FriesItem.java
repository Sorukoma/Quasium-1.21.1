package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.effect.ModEffects;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.List;

public class FriesItem extends Item {
    private static final float MUNCHIES_SATURATION = 0.6f;

    public FriesItem(Properties properties) {
        super(properties);
    }

    public static boolean munchies(Player player) {
        return PlayerKaifData.getKaifValue(player) >= Config.FRIES_MUNCHIES_MIN_KAIF.getAsInt();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player && munchies(player)) {
            int seconds = Config.FRIES_KAIF_HOLD_SECONDS.getAsInt();
            player.getFoodData().eat(Config.FRIES_MUNCHIES_EXTRA_FOOD.getAsInt(), MUNCHIES_SATURATION);
            player.addEffect(new MobEffectInstance(ModEffects.SNACKED, seconds * 20, 0, false, false, true));
            player.displayClientMessage(Component.translatable("quasium.message.fries.munchies", seconds), true);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("quasium.tooltip.fries", ConfigArgs.synced(Config.FRIES_MUNCHIES_MIN_KAIF),
                ConfigArgs.synced(Config.FRIES_KAIF_HOLD_SECONDS)).withStyle(ChatFormatting.GRAY));
    }
}
