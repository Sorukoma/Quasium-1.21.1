package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.PageLabels;
import net.sorukoma.quasium.mind.MindData;

public final class AccordJournal {
    public static final String KEY = "Accords";

    private AccordJournal() {
    }

    public static boolean knows(Player player, String accord) {
        return MindData.has(player, KEY, accord);
    }

    static void hear(ServerPlayer player, String accord) {
        if (!MindData.add(player, KEY, accord)) return;
        int insight = PageConfig.ACCORD_INSIGHT.getAsInt();
        PlayerMagicData.addInsight(player, insight);
        player.displayClientMessage(Component.translatable("quasium.accord.discovered", PageLabels.accord(accord), insight)
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        SmokecraftTrigger.fire(player, "accord:" + accord);
    }
}
