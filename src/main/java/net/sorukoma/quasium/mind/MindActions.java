package net.sorukoma.quasium.mind;

import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.mind.dream.Dreams;
import net.sorukoma.quasium.mind.ghost.GhostDeal;
import net.sorukoma.quasium.mind.research.Research;

public final class MindActions {
    private MindActions() {
    }

    static void handle(ServerPlayer player, String action, String argument) {
        switch (action) {
            case "research" -> Research.study(player, argument);
            case "school" -> Research.chooseSchool(player, argument);
            case "lucid" -> Dreams.chooseLucid(player, argument);
            case "ghost" -> GhostDeal.answer(player, argument);
            case "sync" -> MindSync.send(player);
            default -> {
            }
        }
    }
}
