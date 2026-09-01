package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;

import java.util.List;

public class JerkyItem extends Item {
    public static final int SAFE_DRYING_PERCENT = 70;
    public static final int MAX_DRYING_PERCENT = 150;

    private final int dryingTime;

    public JerkyItem(int dryingTime, Properties properties) {
        super(properties);
        this.dryingTime = dryingTime;
    }

    public int getDryingTime() {
        return dryingTime;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        int dryingPercent = Mth.clamp(stack.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 100),
                0, MAX_DRYING_PERCENT);
        if (!level.isClientSide && entity instanceof Player player) {
            int kaifChange = calculateKaif(dryingPercent);
            if (kaifChange >= 0) {
                PlayerKaifData.addKaif(player, kaifChange);
            } else {
                PlayerKaifData.subtractKaif(player, -kaifChange);
            }
            player.displayClientMessage(Component.translatable("quasium.message.jerky.kaif",
                    dryingPercent, kaifChange >= 0 ? "+" + kaifChange : Integer.toString(kaifChange)), true);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    public static int calculateKaif(int dryingPercent) {
        if (dryingPercent < SAFE_DRYING_PERCENT) {
            return -Math.max(1, (SAFE_DRYING_PERCENT - dryingPercent + 9) / 10);
        }
        return Math.min(15, 1 + (dryingPercent - SAFE_DRYING_PERCENT) * 14 / 80);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponents.DRYING_PROGRESS.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int progress = Mth.clamp(stack.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 100),
                0, MAX_DRYING_PERCENT);
        return Math.round(13.0F * progress / MAX_DRYING_PERCENT);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int progress = Mth.clamp(stack.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 100),
                0, MAX_DRYING_PERCENT);
        return Mth.hsvToRgb(progress / (MAX_DRYING_PERCENT * 3.0F), 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int progress = stack.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 100);
        ChatFormatting color = progress < SAFE_DRYING_PERCENT ? ChatFormatting.RED
                : progress < 100 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        tooltip.add(Component.translatable("quasium.tooltip.jerky.drying", progress).withStyle(color));
        tooltip.add(Component.translatable("quasium.tooltip.jerky.kaif", calculateKaif(progress))
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
