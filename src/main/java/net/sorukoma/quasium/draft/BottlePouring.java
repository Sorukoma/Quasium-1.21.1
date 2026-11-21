package net.sorukoma.quasium.draft;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;

import java.util.function.IntUnaryOperator;

public final class BottlePouring {
    private BottlePouring() {
    }

    public static int pour(ItemStack bottle, Player player, InteractionHand hand, IntUnaryOperator fill) {
        int ml = EssenceBottleItem.amountOf(bottle);
        if (EssenceBottleItem.typeOf(bottle).isBlank() || ml <= 0) return 0;
        int poured = fill.applyAsInt(ml);
        if (poured <= 0) return 0;
        if (poured < ml) {
            bottle.set(ModDataComponents.ESSENCE_AMOUNT.get(), ml - poured);
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(bottle, player, new ItemStack(Items.GLASS_BOTTLE)));
        }
        return poured;
    }
}
