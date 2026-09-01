package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class PacketSyncHookahConnectionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handle(final PacketSyncHookahConnection payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;

            payload.connectedHookahPos().ifPresentOrElse(
                    pos -> {
                        ClientHookahInputHandler.setConnectedHookahPos(pos);
                    },
                    () -> {
                        ClientHookahInputHandler.setConnectedHookahPos(null);
                    }
            );
        });
    }
}
