package net.sorukoma.quasium.selection.trade;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Map;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class HutPools {
    public static final int WEIGHT = 2;
    public static final Map<String, String> HUT_BY_VILLAGE = Map.of(
            "plains", "oak", "savanna", "oak", "taiga", "spruce", "snowy", "spruce", "desert", "sandstone");

    private HutPools() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        HUT_BY_VILLAGE.forEach((village, wood) -> {
            StructureTemplatePool houses = pools.get(ResourceLocation.withDefaultNamespace("village/" + village + "/houses"));
            if (houses == null) return;
            StructurePoolElement hut = StructurePoolElement.single(hutTemplate(wood)).apply(StructureTemplatePool.Projection.RIGID);
            for (int copy = 0; copy < WEIGHT; copy++) houses.templates.add(hut);
        });
    }

    public static String hutTemplate(String wood) {
        return QuasiumMod.MOD_ID + ":village/hookah_hut_" + wood;
    }
}
