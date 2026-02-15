package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
// import net.sorukoma.quasium.client.ClientHookahInputHandler; // Откатываем импорт
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class PacketHookahPuffDataClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public record ClientHookahData(int puffsRemainingPotion, int puffsRemainingCharcoal, @Nullable UUID attachedPlayerUUID, @Nullable String attachedPlayerName) {}

    private static final Map<BlockPos, ClientHookahData> clientHookahStates = new HashMap<>();

    public static void handle(final PacketHookahPuffData payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            LOGGER.debug("Client: Received PacketHookahPuffData for block at {}. PuffsPotion: {}, CoalTime: {}, AttachedUUID: {}, AttachedName: {}",
                    payload.blockPos(), payload.puffsRemainingPotion(), payload.puffsRemainingCharcoal(), payload.attachedPlayerUUID(), payload.attachedPlayerName());

            clientHookahStates.put(payload.blockPos(), new ClientHookahData(
                payload.puffsRemainingPotion(),
                payload.puffsRemainingCharcoal(),
                payload.attachedPlayerUUID(),
                payload.attachedPlayerName()
            ));
            // ClientHookahInputHandler.resetAttachInProgress(); // Откатываем вызов
            LOGGER.debug("Client: ClientHookahInputHandler.resetAttachInProgress() called after PacketHookahPuffData.");
        });
    }

    @Nullable
    public static ClientHookahData getClientHookahData(BlockPos pos) {
        return clientHookahStates.get(pos);
    }

    // Откатываем этот метод
    // public static Map<BlockPos, ClientHookahData> getAllClientHookahStates() {
    //     return clientHookahStates;
    // }
}
