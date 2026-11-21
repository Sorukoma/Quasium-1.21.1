package net.sorukoma.quasium.mind.research;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.api.SmokeCastEvent;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindMath;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class CondensedFocus {
    private CondensedFocus() {
    }

    @SubscribeEvent
    public static void onCast(SmokeCastEvent event) {
        ServerPlayer player = event.player();
        if (event.essenceCost() <= 0 || !Perk.CONDENSATES.ownedBy(player)) return;
        float left = event.essenceCost() * (100 - MindConfig.CONDENSATES_PCT.getAsInt()) / 100f;
        event.setEssenceCost(MindMath.round(left, player.getRandom().nextFloat()));
    }
}
