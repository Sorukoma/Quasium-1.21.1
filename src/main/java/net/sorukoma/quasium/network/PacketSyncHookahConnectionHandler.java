package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.client.RingTiming;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class PacketSyncHookahConnectionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handle(final PacketSyncHookahConnection payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;

            boolean wasConnected = ClientHookahInputHandler.getConnectedHookahPos() != null;
            ClientHookahInputHandler.setConnectedHookahPos(payload.connectedHookahPos().orElse(null));
            if (wasConnected && payload.connectedHookahPos().isEmpty()) RingTiming.stoppedByServer();
        });
    }
}
