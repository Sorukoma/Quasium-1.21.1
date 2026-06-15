package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.data.PlayerKaifData;

@OnlyIn(Dist.CLIENT)
public class ClientKaifData {
    private static int clientKaifValue = 0;

    public static int getKaifValue() {
        return clientKaifValue;
    }

    public static void setKaifValue(int value) {
        clientKaifValue = Math.max(PlayerKaifData.MIN_KAIF, Math.min(PlayerKaifData.MAX_KAIF, value));
    }
}
