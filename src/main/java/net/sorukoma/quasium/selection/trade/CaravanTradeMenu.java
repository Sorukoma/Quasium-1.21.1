package net.sorukoma.quasium.selection.trade;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.trading.Merchant;

public class CaravanTradeMenu extends MerchantMenu {
    private static final int INVENTORY_START = 3;
    private static final int INVENTORY_END = 39;

    public CaravanTradeMenu(int containerId, Inventory inventory, Merchant caravan) {
        super(containerId, inventory, caravan);
    }

    @Override
    public void tryMoveItems(int selected) {
        super.tryMoveItems(selected);
        if (selected < 0 || selected >= getOffers().size() || !(getOffers().get(selected) instanceof SelfMixOffer)) return;
        Slot payment = getSlot(0);
        if (payment.hasItem() || getSlot(1).hasItem()) return;
        slots.subList(INVENTORY_START, INVENTORY_END).stream().filter(slot -> SelfMixOffer.accepts(slot.getItem())).findFirst()
                .ifPresent(slot -> payment.set(slot.remove(slot.getItem().getCount())));
    }
}
