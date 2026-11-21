package net.sorukoma.quasium.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler.ClientHookahData;

public class HookahScreen extends AbstractHookahScreen<HookahMenu> {
    public HookahScreen(HookahMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Reading reading() {
        ClientHookahData data = PacketHookahPuffDataClientHandler.getClientHookahData(menu.blockEntity.getBlockPos());
        return data == null ? Reading.EMPTY : new Reading(data.totalHeat(), data.totalTobaccoWeight(), data.warmup(), data.bowlContents());
    }
}
