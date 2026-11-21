package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;

public class EmptySmokeJarItem extends Item {
    public EmptySmokeJarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof HookahBlockEntity hookah)) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        if (!hookah.canBottleSmoke()) {
            player.displayClientMessage(Component.translatable("quasium.smoke_jar.not_peak"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack filled = new ItemStack(ModBlocks.SMOKE_JAR_ITEM.get());
        filled.set(ModDataComponents.TOBACCO_FLAVOR.get(), hookah.zoneFlavor());
        player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(context.getItemInHand(), player, filled));
        context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 0.8f);
        SmokecraftTrigger.fire(player, "bottle_smoke");
        return InteractionResult.CONSUME;
    }
}
