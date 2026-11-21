package net.sorukoma.quasium.smokelands.world;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokePortalUnlock {
    private static final int LOOK_EVERY = 20;
    private static final ResourceLocation DRIED_QUASIUM = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_anvil");
    private static final ResourceLocation SMOKE_PORTAL = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/smoke_portal");
    private static final String DRIED_QUASIUM_CRITERION = "has_quasium_block";

    private SmokePortalUnlock() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % LOOK_EVERY != 0 || player.isSpectator()) return;
        if (player.level().dimension() != Level.OVERWORLD) return;
        if (player.serverLevel().structureManager()
                .getStructureWithPieceAt(player.blockPosition(), structure -> structure.is(SmokelandsWorldgen.RUINED_PORTAL_KEY)).isValid()) {
            SmokecraftTrigger.fire(player, "ruined_portal");
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) carryOver(player);
    }

    public static void carryOver(ServerPlayer player) {
        AdvancementHolder dried = player.server.getAdvancements().get(DRIED_QUASIUM);
        AdvancementHolder portal = player.server.getAdvancements().get(SMOKE_PORTAL);
        if (dried == null || portal == null) return;
        PlayerAdvancements progress = player.getAdvancements();
        if (progress.getOrStartProgress(dried).isDone() && !progress.getOrStartProgress(portal).isDone()) progress.award(portal, DRIED_QUASIUM_CRITERION);
    }
}
