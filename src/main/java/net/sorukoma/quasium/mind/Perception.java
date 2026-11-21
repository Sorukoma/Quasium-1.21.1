package net.sorukoma.quasium.mind;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteProfiles;
import net.sorukoma.quasium.mind.research.ResearchRules;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class Perception {
    public static final String ITEMS = "perceived_items";
    public static final String MOBS = "perceived_mobs";
    public static final String RUINS = "perceived_ruins";
    public static final String MIRROR = ResearchRules.MIRROR_SEEN;

    private Perception() {
    }

    public static void scanned(ServerPlayer player, boolean mob) {
        MindData.raise(player, mob ? MOBS : ITEMS);
    }

    public static void sawRuins(ServerPlayer player) {
        MindData.raise(player, RUINS);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (String key : PlayerMagicData.getScanned(player)) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            NoteProfile profile = id == null ? null : NoteProfiles.get(id);
            if (profile != null) scanned(player, profile.isEntity());
        }
    }
}
