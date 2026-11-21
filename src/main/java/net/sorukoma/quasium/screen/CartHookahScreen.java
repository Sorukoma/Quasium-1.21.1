package net.sorukoma.quasium.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.cart.CartHookahMenu;
import net.sorukoma.quasium.component.ModDataComponents;

public class CartHookahScreen extends AbstractHookahScreen<CartHookahMenu> {
    private static final int BOWL_SLOT = 1;

    public CartHookahScreen(CartHookahMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Reading reading() {
        return new Reading(menu.heat(), menu.tobacco(), menu.warmup(), menu.getSlot(BOWL_SLOT).getItem().get(ModDataComponents.HOOKAH_BOWL_CONTENTS.get()));
    }
}
