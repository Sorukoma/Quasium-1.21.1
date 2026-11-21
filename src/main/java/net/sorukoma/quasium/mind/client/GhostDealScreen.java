package net.sorukoma.quasium.mind.client;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindPackets;
import net.sorukoma.quasium.mind.ghost.GhostDeal;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GhostDealScreen extends MindDialog {
    public GhostDealScreen() {
        super(Component.translatable("entity.quasium.hookah_ghost"));
    }

    @Override
    Component body() {
        return Component.translatable("quasium.ghost.offer", ConfigArgs.synced(MindConfig.GHOST_KAIF_GAIN),
                ConfigArgs.synced(MindConfig.GHOST_TOLERANCE), ConfigArgs.synced(MindConfig.GHOST_CURSE_MULTIPLIER));
    }

    @Override
    List<Choice> choices() {
        return List.of(
                answer(Component.translatable("quasium.ghost.pay_emeralds", ConfigArgs.synced(MindConfig.GHOST_PRICE_EMERALDS)), GhostDeal.EMERALDS),
                answer(Component.translatable("quasium.ghost.pay_levels", ConfigArgs.synced(MindConfig.GHOST_PRICE_LEVELS)), GhostDeal.LEVELS),
                answer(Component.translatable("quasium.ghost.refuse"), GhostDeal.LEAVE));
    }

    private static Choice answer(Component label, String choice) {
        return new Choice(label, () -> MindPackets.act("ghost", choice), null);
    }
}
