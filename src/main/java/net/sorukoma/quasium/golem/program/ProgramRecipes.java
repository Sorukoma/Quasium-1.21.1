package net.sorukoma.quasium.golem.program;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.sorukoma.quasium.QuasiumMod;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class ProgramRecipes {
    private static final ResourceLocation ANVIL = id("progression/obtained_anvil");
    private static final List<ResourceLocation> TAUGHT_BY_ANVIL = List.of(id("program_table"), id("place_marker"), id("program_card"));

    private ProgramRecipes() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) carryOver(player);
    }

    public static void carryOver(ServerPlayer player) {
        AdvancementHolder anvil = player.server.getAdvancements().get(ANVIL);
        if (anvil != null && player.getAdvancements().getOrStartProgress(anvil).isDone()) player.awardRecipesByKey(TAUGHT_BY_ANVIL);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
