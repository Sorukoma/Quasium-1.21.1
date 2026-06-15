package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.component.ModDataComponents;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;

public class PacketBackpackHookahPuffDataClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static ClientBackpackHookahData cachedData = null;

    public static void handle(final PacketBackpackHookahPuffData payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            cachedData = new ClientBackpackHookahData(
                    payload.totalHeat(),
                    payload.totalTobaccoWeight(),
                    payload.bowlContents()
            );
        });
    }

    @Nullable
    public static ClientBackpackHookahData getClientBackpackHookahData() {
        return cachedData;
    }

    public static void clearData() {
        cachedData = null;
    }

    public record ClientBackpackHookahData(
            float totalHeat,
            int totalTobaccoWeight,
            @Nullable ModDataComponents.HookahBowlContents bowlContents
    ) {}
}
