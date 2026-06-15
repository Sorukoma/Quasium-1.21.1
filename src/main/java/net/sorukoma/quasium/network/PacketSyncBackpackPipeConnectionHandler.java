package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.client.ClientHookahInputHandler;

public class PacketSyncBackpackPipeConnectionHandler {
    public static void handle(final PacketSyncBackpackPipeConnection payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            ClientHookahInputHandler.setConnectedToBackpack(payload.connected());
        });
    }
}