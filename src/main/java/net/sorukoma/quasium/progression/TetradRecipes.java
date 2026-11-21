package net.sorukoma.quasium.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.sorukoma.quasium.QuasiumMod;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class TetradRecipes {
    public static final ResourceLocation TETRAD = id("spell_tetrad");
    private static final ResourceLocation FIRST_SCAN = id("smokecraft/first_scan");

    private TetradRecipes() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) carryOver(player);
    }

    public static void carryOver(ServerPlayer player) {
        AdvancementHolder scan = player.server.getAdvancements().get(FIRST_SCAN);
        if (scan != null && player.getAdvancements().getOrStartProgress(scan).isDone()) player.awardRecipesByKey(List.of(TETRAD));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
