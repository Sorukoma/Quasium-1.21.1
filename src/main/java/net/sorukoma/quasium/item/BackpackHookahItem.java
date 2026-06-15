package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

public class BackpackHookahItem extends Item implements ICurioItem {

    public BackpackHookahItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("back_hookah_slot");
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // Player equipped the backpack hookah
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        Player player = slotContext.entity() instanceof Player p ? p : null;
        if (player != null && !player.level().isClientSide()) {
            // Drop contents and reset state when removed from Curios slot
            net.sorukoma.quasium.data.BackpackHookahState state =
                    net.sorukoma.quasium.data.BackpackHookahState.getState(player.getUUID());
            if (state != null) {
                if (state.isPuffing()) {
                    state.stopPuffing();
                }
                state.drops();
                state.resetState();
                net.sorukoma.quasium.data.BackpackHookahState.removeState(player.getUUID());
            }
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // Server-side ticking is handled by ServerTickHandler
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.quasium.backpack_hookah.tooltip"));
    }

    @Override
    public boolean makesPiglinsNeutral(SlotContext slotContext, ItemStack stack) {
        return false;
    }
}