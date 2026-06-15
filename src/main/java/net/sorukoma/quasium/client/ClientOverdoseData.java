package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientOverdoseData {
    private static boolean overdosed = false;

    public static void setOverdosed(boolean value) {
        overdosed = value;
        if (!value) {
            OverdoseHallucinationHandler.clearHallucinations();
        }
    }

    public static boolean isOverdosed() {
        return overdosed;
    }

    public static void reset() {
        overdosed = false;
        OverdoseHallucinationHandler.clearHallucinations();
    }
}
