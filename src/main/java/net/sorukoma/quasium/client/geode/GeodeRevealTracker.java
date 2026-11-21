package net.sorukoma.quasium.client.geode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.geode.GeodeReveal;

import java.util.Map;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class GeodeRevealTracker {
    private static boolean revealed;

    private GeodeRevealTracker() {
    }

    @SubscribeEvent
    public static void disguiseCrust(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel deepslate = models.get(BlockModelShaper.stateToModelLocation(Blocks.DEEPSLATE.defaultBlockState()));
        for (BlockState state : SelectionRegistry.QUASIUM_CRUST.get().getStateDefinition().getPossibleStates()) {
            models.computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, crust) -> new QuasiumCrustModel(crust, deepslate));
        }
    }

    @SubscribeEvent
    public static void rerenderOnThreshold(ClientTickEvent.Post event) {
        boolean now = GeodeReveal.revealed(ClientKaifData.getKaifValue());
        if (now == revealed) return;
        revealed = now;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        SectionPos center = SectionPos.of(minecraft.player.blockPosition());
        int radius = minecraft.options.getEffectiveRenderDistance();
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                for (int y = minecraft.level.getMinSection(); y < minecraft.level.getMaxSection(); y++) {
                    minecraft.levelRenderer.setSectionDirty(x, y, z);
                }
            }
        }
    }
}
