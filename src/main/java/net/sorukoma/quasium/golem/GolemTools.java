package net.sorukoma.quasium.golem;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class GolemTools {
    private GolemTools() {
    }

    static boolean worn(WorkerGolem golem) {
        return worn(golem.tool());
    }

    static boolean worn(ItemStack tool) {
        return tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= Math.max(2, tool.getMaxDamage() / 10);
    }

    static void use(WorkerGolem golem) {
        ItemStack tool = golem.tool();
        if (!tool.isDamageableItem()) return;
        Item item = tool.getItem();
        tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
        if (golem.tool().isEmpty()) golem.note("tool_broke", item.getDescriptionId());
    }

    static InteractionResult swap(WorkerGolem golem, Player player, ItemStack held) {
        if (golem.level().isClientSide) return InteractionResult.SUCCESS;
        if (!golem.isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("quasium.golem.not_owner"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack old = golem.tool();
        golem.setItemSlot(EquipmentSlot.MAINHAND, held.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, old);
        golem.note("took_tool", held.getDescriptionId());
        golem.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 0.8f, 1.1f);
        player.displayClientMessage(Component.translatable("quasium.golem.took_tool", held.getHoverName(), golem.job().title()), true);
        return InteractionResult.CONSUME;
    }
}
