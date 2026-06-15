package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.component.ModDataComponents;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class PacketHookahPuffDataClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public record ClientHookahData(float totalHeat, int totalTobaccoWeight, @Nullable ModDataComponents.HookahBowlContents bowlContents) {}

    private static final Map<BlockPos, ClientHookahData> clientHookahStates = new HashMap<>();

    public static void handle(final PacketHookahPuffData payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            clientHookahStates.put(payload.blockPos(), new ClientHookahData(
                payload.totalHeat(),
                payload.totalTobaccoWeight(),
                payload.bowlContents()
            ));
        });
    }

    @Nullable
    public static ClientHookahData getClientHookahData(BlockPos pos) {
        return clientHookahStates.get(pos);
    }

    public static void clearAllData() {
        clientHookahStates.clear();
    }

    public static void removeData(BlockPos pos) {
        clientHookahStates.remove(pos);
    }
}
