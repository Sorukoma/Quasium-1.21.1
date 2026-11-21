package net.sorukoma.quasium.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Predicate;

public class ClayModuleItem extends Item {
    private final String module;
    private final Predicate<ClayGolemEntity> install;

    public ClayModuleItem(Properties properties, String module, Predicate<ClayGolemEntity> install) {
        super(properties);
        this.module = module;
        this.install = install;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ClayGolemEntity golem)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!install.test(golem)) {
            player.displayClientMessage(Component.translatable("quasium.clay_golem." + module + "_already"), true);
            return InteractionResult.CONSUME;
        }
        stack.consume(1, player);
        player.level().playSound(null, golem.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 1.2f);
        player.displayClientMessage(Component.translatable("quasium.clay_golem." + module + "_installed"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.quasium." + module + "_module.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
