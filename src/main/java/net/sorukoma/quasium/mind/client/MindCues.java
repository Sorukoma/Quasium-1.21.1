package net.sorukoma.quasium.mind.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.client.OverdoseHallucinationHandler;

@OnlyIn(Dist.CLIENT)
public final class MindCues {
    private MindCues() {
    }

    public static void handle(String cue, int value) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (cue) {
            case "coma" -> {
                if (value > 0) minecraft.setScreen(new ComaScreen(value));
                else if (minecraft.screen instanceof ComaScreen) minecraft.setScreen(null);
            }
            case "nightmare" -> OverdoseHallucinationHandler.haunt(value);
            case "lucid" -> minecraft.setScreen(new LucidDreamScreen());
            case "ghost" -> minecraft.setScreen(new GhostDealScreen());
            default -> {
            }
        }
    }
}
