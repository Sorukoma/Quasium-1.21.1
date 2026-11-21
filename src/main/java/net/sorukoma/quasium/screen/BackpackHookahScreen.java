package net.sorukoma.quasium.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.network.PacketBackpackHookahPuffDataClientHandler;
import net.sorukoma.quasium.network.PacketBackpackHookahPuffDataClientHandler.ClientBackpackHookahData;

public class BackpackHookahScreen extends AbstractHookahScreen<BackpackHookahMenu> {
    public BackpackHookahScreen(BackpackHookahMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Reading reading() {
        ClientBackpackHookahData data = PacketBackpackHookahPuffDataClientHandler.getClientBackpackHookahData();
        return data == null ? Reading.EMPTY : new Reading(data.totalHeat(), data.totalTobaccoWeight(), data.warmup(), data.bowlContents());
    }
}
